# Evaldo — Machado

## 🔥 Visão Geral

Este repositório contém o projeto **Evaldo**, um robô Robocode desenvolvido para ser altamente competitivo combinando Wave Surfing, Anti-Gravity, Wall Smoothing e mira híbrida (linear + estatística). O objetivo é fornecer um bot robusto, adaptativo e difícil de acertar por adversários humanos ou bots.

---

## 🧠 Personalidade do Evaldo 

Evaldo **Machado** é um caçador calculista e implacável. Sua "personalidade" pode ser descrita assim:

* **Focado e Obsessivo**: assim que detecta um alvo preferencial, concentra todos os seus recursos (radar, movimento e canhão) nele até a derrota.
* **Frio e Calculista**: toma decisões baseadas em estatísticas e em simulações de ondas — raramente age por impulso.
* **Imprevisível quando precisa**: mistura movimentos aleatórios curtos (zig-zag) para quebrar padrões e evitar ser facilmente previsto.
* **Defensivo quando necessário**: prioriza sobrevivência com wall smoothing absoluto e anti-gravidade; se pressionado, recua e reposiciona.
* **Economizador de recursos**: gerencia energia ao atirar — dispara mais forte quando a chance de acerto é alta.

Tom geral: **caçador silencioso** — paciente, adaptativo e letal.

---

## ▶️ Passo a passo: Como rodar o Evaldo no Robocode

Siga este passo a passo se você usa a interface do Robocode (recomendado):

1. **Instale o Robocode**

   * Baixe e instale Robocode (versão compatível com seu JDK). Consulte `docs/COMO_RODAR.md` para links e requisitos.

2. **Copie os arquivos**

   * Copie a pasta `bots/` (ou apenas o arquivo `EvaldoWaveSurfGodMode.java` / `EvaldoWaveSurfMaxPlus.java` / `EvaldoWaveSurfMax.java`) para a pasta do Robocode:

     * `C:\robocode\robots\` (Windows) ou `~/robocode/robots/` (Linux/Mac).

3. **Crie/edite o .properties (se necessário)**

   * Para garantir que o robô compile como `STANDARD`, crie um arquivo `EvaldoWaveSurfGodMode.properties` (se não houver) dentro de `bots/` com conteúdo mínimo:

     ```text
     robot.classname=Robos.EvaldoWaveSurfGodMode
     robot.name=EvaldoWaveSurfGodMode
     robot.author=Seu Nome
     robot.type=STANDARD
     ```

4. **Abra o Robocode**

   * No menu, vá em *Robots → Editors* para ver o código, ou *Robots → Load robots from disk*.

5. **Compile**

   * No Robocode: *Compiler → Compile all* (ou botão "Compile" no editor). Veja se não há erros no console de compilação.

6. **Crie uma batalha**

   * *Battle → New Battle* → escolha o mapa (battlefield) e o tamanho.
   * Adicione o `Evaldo...` na lista de bots participantes. Adicione adversários (por exemplo: `sample.SpinBot`, `sample.RamFire`, ou outros bots do diretório `robots/`).

7. **Ajustes (opcionais)**

   * Para testar 1 vs 1, marque uma batalha com apenas 2 robôs.
   * Você pode ajustar o *rounds* (número de rodadas) e *gun heat* nas configurações, mas geralmente as defaults são suficientes.

8. **Execute a batalha**

   * Clique em *Start battle*. Observe no console se há mensagens de erro.

9. **Análise de resultado**

   * Após a batalha, confira o painel de estatísticas do Robocode e os logs na pasta `data/` (se configurado para salvar logs).

---

## ▶️ Passo a passo: Como compilar/rodar via terminal

Se preferir compilar manualmente com `javac`:

1. Abra terminal na pasta do robocode (`C:/robocode/robots/Robos`)
2. Compile com o classpath apontando para `libs/robocode.jar` (ajuste caminho):

   ```bash
   javac -cp "C:\robocode\libs\robocode.jar" Robos/*.java
   ```
3. Se não houver erros, os `.class` serão gerados; abra Robocode e carregue-os com *Robots → Load robots from disk*.

---

## 🛠 Dicas de testes e tuning

* **Testes unitários**: faça batalhas 1v1 contra bots conhecidos para medir eficácia de mira (acertos/tiros).
* **Ajuste energia**: se Evaldo estiver gastando muita energia, reduza potência de tiro em `doGun*()` conforme distância.
* **Logs**: habilite logs simples (salve estatísticas em `data/`) para analisar padrões de evasão e acerto.
* **Campos pequenos vs grandes**: a força de anti-gravidade e margens de wall smoothing podem precisar de ajuste quando o battlefield muda.

---

## 👥 Acadêmicos

* Kauan Amélio Cipriani
* Guilherme Depiné Neto
* Maria Cecilia Schneider de Oliveira
* Vitor Hugo Konzen

---
