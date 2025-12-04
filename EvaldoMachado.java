package Robos;

import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.util.*;

/**
 * EvaldoGodTier
 * - Hunter / Sniper focus: persegue um inimigo específico até ele morrer
 * - God Mode evasivo (wave-surfing + anti-wall)
 * - Tiro preciso com guess-factor weighting
 *
 */
public class EvaldoMachado extends AdvancedRobot {

    // guess-factor bins
    private static final int BINS = 47;
    private final double[] surfStats = new double[BINS];

    // waves and tracking
    private final List<EnemyWave> enemyWaves = new ArrayList<>();
    private Random rand = new Random();

    // target tracking (hunter/sniper)
    private String hunterTarget = null;
    private double targetDistance, targetAbsBearing, targetHeading, targetVelocity, targetEnergy;
    private double lastEnemyEnergy = 100;

    // movement control
    private int moveDirection = 1;
    private long lastChange = 0;

    public void run() {
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setColors(Color.BLACK, Color.RED, Color.YELLOW);

        // stay aggressive from the start
        while (true) {
            // if we have a target, keep radar lock, else scan full
            if (hunterTarget != null) {
                // lock radar on target by sweeping small arcs
                setTurnRadarRightRadians(Utils.normalRelativeAngle(targetAbsBearing - getRadarHeadingRadians()) * 2);
            } else {
                setTurnRadarRight(360);
            }
            // always execute loop ops
            execute();
        }
    }

    // When we scan ANY robot: decide if we should hunt it (hunter behavior)
    public void onScannedRobot(ScannedRobotEvent e) {
        String name = e.getName();

        double absBearing = Math.toRadians(getHeading()) + Math.toRadians(e.getBearing());
        double dist = e.getDistance();

        // If no hunter target, pick the first scanned robot and lock on it
        if (hunterTarget == null) {
            hunterTarget = name;
        }

        // If this scan is our chosen target, update target snapshot
        if (name.equals(hunterTarget)) {
            targetDistance = dist;
            targetAbsBearing = absBearing;
            targetHeading = Math.toRadians(e.getHeading());
            targetVelocity = e.getVelocity();
            targetEnergy = e.getEnergy();

            // radar lock
            double rturn = Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians());
            setTurnRadarRightRadians(rturn * 2);

            // detect enemy shot (energy drop) -> create wave
            double energyDrop = lastEnemyEnergy - targetEnergy;
            if (energyDrop > 0.0 && energyDrop <= 3.0) {
                EnemyWave w = new EnemyWave();
                w.fireTime = getTime();
                w.fireX = getX() + Math.sin(absBearing) * dist;
                w.fireY = getY() + Math.cos(absBearing) * dist;
                w.bulletVelocity = bulletVelocity(energyDrop);
                w.directAngle = absBearing;
                w.lastDistanceAtFire = dist;
                enemyWaves.add(w);
            }
            lastEnemyEnergy = targetEnergy;

            // movement + firing
            purgeOldWaves();
            doSurfingGod();
            doHunterGun(e);
            execute();
        } else {
            // if this scan is NOT the hunter and the hunter died, switch
            // if hunter not alive (we may detect robot death elsewhere) keep scanning
            // Keep scanning sweep for new target
        }
    }

    // If our target dies, switch to next available (or null)
    public void onRobotDeath(RobotDeathEvent e) {
        if (hunterTarget != null && hunterTarget.equals(e.getName())) {
            hunterTarget = null;        // stop hunting; next scanned robot will be chosen
            enemyWaves.clear();         // reset waves
            lastEnemyEnergy = 100;
        }
    }

    // onHitByBullet logs wave hits
    public void onHitByBullet(HitByBulletEvent e) {
        EnemyWave w = getClosestSurfableWave(e.getBullet().getPower(), e.getBullet().getVelocity());
        if (w != null) {
            logHit(w, getX(), getY());
            enemyWaves.remove(w);
        }
        // evasive jitter
        moveDirection *= -1;
        setAhead(100 * moveDirection);
    }

    public void onHitWall(HitWallEvent e) {
        // bounce off wall and change direction
        moveDirection *= -1;
        setBack(80);
    }

    // ---------- HUNTER SNIPER GUN ----------
    private void doHunterGun(ScannedRobotEvent e) {
        // compute firing power adaptively: stronger when close and when we have energy
        double power = Math.min(3, Math.max(0.6, 500 / Math.max(1, e.getDistance())));
        power = Math.min(power, getEnergy() - 0.1); // never drop to 0 energy
        if (power < 0.1) return;

        // linear prediction
        double absBearing = Math.toRadians(getHeading()) + Math.toRadians(e.getBearing());
        double enemyX = getX() + Math.sin(absBearing) * e.getDistance();
        double enemyY = getY() + Math.cos(absBearing) * e.getDistance();

        double bulletSpeed = bulletVelocity(power);
        double predictedX = enemyX, predictedY = enemyY;
        double enemyHeadingRad = Math.toRadians(e.getHeading());
        double v = e.getVelocity();
        double t = 0;
        while ((++t) * bulletSpeed < Point2D.distance(getX(), getY(), predictedX, predictedY) && t < 100) {
            predictedX += Math.sin(enemyHeadingRad) * v;
            predictedY += Math.cos(enemyHeadingRad) * v;
            if (!inField(predictedX, predictedY)) break;
        }

        // apply guess-factor bias from surfStats to adjust aim slightly towards likely enemy position
        double aim = Math.atan2(predictedX - getX(), predictedY - getY());
        double gfAdjust = guessFactorAdjustment(e, aim, bulletSpeed);
        double finalAim = aim + gfAdjust;

        double gunTurn = Utils.normalRelativeAngle(finalAim - getGunHeadingRadians());
        setTurnGunRightRadians(gunTurn);

        // fire more aggressively (sniper) when gun nearly aligned or target is close
        if ((Math.abs(getGunTurnRemaining()) < 0.35 && getGunHeat() == 0) || targetDistance < 120) {
            setFire(power);
        }
    }

    // guess-factor small tweak: returns a small radians offset
    private double guessFactorAdjustment(ScannedRobotEvent e, double aim, double bulletSpeed) {
        // choose index from predicted lateral offset and apply small weighted correction
        double absBearing = Math.toRadians(getHeading()) + Math.toRadians(e.getBearing());
        double rel = Utils.normalRelativeAngle(aim - absBearing); // relative angle
        double maxEscape = Math.asin(8.0 / Math.max(0.0001, e.getDistance()));
        double factor = rel / maxEscape;
        factor = Math.max(-1, Math.min(1, factor));
        int index = (int)Math.round(((factor + 1) / 2) * (BINS - 1));
        // find the direction of highest weight nearby
        double weight = surfStats[index];
        // small adjustment proportional to weight magnitude and sign of factor
        return (factor >= 0 ? 1 : -1) * Math.min(0.12, 0.0008 * weight);
    }

    // ---------- GOD SURFING (evade, never stick to walls) ----------
    private void doSurfingGod() {
        EnemyWave surfWave = getClosestWave();
        if (surfWave == null) {
            antiGravityGod();
            return;
        }

        // jitter direction randomly occasionally to be unpredictable
        if (getTime() - lastChange > 1) {
            if (rand.nextDouble() < 0.45) moveDirection *= -1;
            lastChange = getTime();
        }

        double dangerLeft = checkDanger(surfWave, -1);
        double dangerRight = checkDanger(surfWave, 1);
        int best = (dangerLeft < dangerRight) ? -1 : 1;
        // mix with random to avoid pattern
        if (rand.nextDouble() < 0.2) best = moveDirection;

        moveDirection = best;
        setMoveForDirectionGod(best);
    }

    private void setMoveForDirectionGod(int direction) {
        // strafe perpendicular to enemy bearing with wall-smoothing
        double angleToEnemy = Math.toDegrees(Math.atan2(getX() - enemyX(), getY() - enemyY()));
        double desired = Utils.normalRelativeAngleDegrees(angleToEnemy + direction * 90 + (rand.nextDouble() * 20 - 10));
        desired = wallSmoothAbsolute(desired, direction);
        setTurnRight(desired);
        setAhead(180 * direction);
    }

    // wall-smoothing absolute: if too close to any wall, enforce a strong steer away
    private double wallSmoothAbsolute(double angleDeg, int direction) {
        double angle = Math.toRadians(angleDeg);
        double testDist = 220;
        double testX = getX() + Math.sin(angle) * testDist;
        double testY = getY() + Math.cos(angle) * testDist;

        double margin = 500; // strict margin
        if (testX < margin) angleDeg += 45 * direction;
        if (testX > getBattleFieldWidth() - margin) angleDeg -= 45 * direction;
        if (testY < margin) angleDeg += 45 * direction;
        if (testY > getBattleFieldHeight() - margin) angleDeg -= 45 * direction;

        return Utils.normalRelativeAngleDegrees(angleDeg);
    }

    private void antiGravityGod() {
        // combine repulsion from walls and slight attraction to center but keep far from walls
        double xForce = 0, yForce = 0;
        double cx = getBattleFieldWidth() / 2.0, cy = getBattleFieldHeight() / 2.0;
        double dx = cx - getX(), dy = cy - getY();
        double dsq = Math.max(1, dx * dx + dy * dy);
        xForce += dx / dsq * -7000;
        yForce += dy / dsq * -7000;

        // very strong wall repulsion with high exponent so we don't stick
        double wallPower = -90000.0;
        xForce += wallPower / Math.pow(Math.max(1.0, getX()), 4);
        xForce -= wallPower / Math.pow(Math.max(1.0, getBattleFieldWidth() - getX()), 4);
        yForce += wallPower / Math.pow(Math.max(1.0, getY()), 4);
        yForce -= wallPower / Math.pow(Math.max(1.0, getBattleFieldHeight() - getY()), 4);

        double angle = Math.atan2(xForce, yForce);
        setTurnRightRadians(Utils.normalRelativeAngle(angle - getHeadingRadians()));
        setAhead(220); // fast push away from walls
    }

    // ---------- Danger simulation ----------
    private double checkDanger(EnemyWave w, int direction) {
        double myX = getX(), myY = getY();
        double heading = Math.toRadians(getHeading());
        double time = 0, danger = 0;
        while (time < 200) {
            double angle = absoluteBearing(w.fireX, w.fireY, myX, myY);
            double lateral = Utils.normalRelativeAngle(angle - w.directAngle);
            int idx = indexFrom(lateral, w.lastDistanceAtFire);
            if (idx >= 0 && idx < BINS) danger += surfStats[idx];

            myX += Math.sin(heading) * 9 * direction;
            myY += Math.cos(heading) * 9 * direction;
            time += 1;
            double dist = Point2D.distance(w.fireX, w.fireY, myX, myY);
            if (dist <= w.distanceTraveled + time * w.bulletVelocity) break;
            if (!inField(myX, myY)) { danger += 10000; break; }
        }
        return danger;
    }

    // ---------- Waves housekeeping ----------
    private EnemyWave getClosestWave() {
        EnemyWave best = null;
        double min = Double.POSITIVE_INFINITY;
        for (EnemyWave w : enemyWaves) {
            double d = Point2D.distance(w.fireX, w.fireY, getX(), getY()) - w.distanceTraveled;
            if (d > 0 && d < min) { min = d; best = w; }
        }
        return best;
    }

    private EnemyWave getClosestSurfableWave(double bulletPower, double bulletV) {
        if (enemyWaves.isEmpty()) return null;
        EnemyWave best = null;
        double bestDiff = Double.POSITIVE_INFINITY;
        for (EnemyWave w : enemyWaves) {
            double diff = Math.abs(w.bulletVelocity - bulletV);
            if (diff < bestDiff) { bestDiff = diff; best = w; }
        }
        return best;
    }

    private void logHit(EnemyWave w, double hx, double hy) {
        double abs = absoluteBearing(w.fireX, w.fireY, hx, hy);
        double offset = Utils.normalRelativeAngle(abs - w.originBearing);
        int idx = indexFrom(offset, w.lastDistanceAtFire);
        if (idx >= 0 && idx < BINS) surfStats[idx] += 1;
    }

    private void purgeOldWaves() {
        Iterator<EnemyWave> it = enemyWaves.iterator();
        while (it.hasNext()) {
            EnemyWave w = it.next();
            w.distanceTraveled = (getTime() - w.fireTime) * w.bulletVelocity;
            if (w.distanceTraveled > Math.max(getBattleFieldWidth(), getBattleFieldHeight()) * 3) it.remove();
        }
    }

    // ---------- helpers ----------
    private double enemyX() { return getX() + Math.sin(targetAbsBearing) * targetDistance; }
    private double enemyY() { return getY() + Math.cos(targetAbsBearing) * targetDistance; }
    private boolean inField(double x, double y) { return x > 160 && x < getBattleFieldWidth() - 160 && y > 160 && y < getBattleFieldHeight() - 160; }
    private double bulletVelocity(double power) { return 20 - 3 * power; }
    private double absoluteBearing(double x1, double y1, double x2, double y2) { return Math.atan2(y2 - y1, x2 - x1); }
    private int indexFrom(double offset, double dist) {
        double maxEscape = Math.asin(8.0 / Math.max(0.0001, dist));
        double factor = offset / maxEscape;
        factor = Math.max(-1, Math.min(1, factor));
        return (int)Math.round(((factor + 1) / 2) * (BINS - 1));
    }

    // ---------- inner classes ----------
    private class EnemyWave {
        long fireTime;
        double fireX, fireY;
        double originBearing;
        double directAngle;
        double bulletVelocity;
        double lastDistanceAtFire;
        double distanceTraveled;
        EnemyWave() { this.lastDistanceAtFire = targetDistance; }
    }

    private static class Point2D {
        static double distance(double x1, double y1, double x2, double y2) { return Math.hypot(x1 - x2, y1 - y2); }
    }
}
