---
id: T-xxx
title: <título>
depends_on: [T-...]
covers: [RF-001, RNF-004]
evidence_only: []                # IDs de covers provados por evidência, sem marcação em teste
allowed_paths:
  - <glob de arquivos que podem ser criados/alterados>
  - docs/specs/tasks/T-xxx.md
allowed_new_dependencies: []     # coordenadas permitidas; vazio = nenhuma
---
## Objetivo
<Uma frase do que entrega e qual risco obrigatório prova.>

## Entradas
- `02-requirements`: <RF/RNF e AC exatos>.
- `03-design-contracts`: <seções>.
- `00-constitution`: <princípios e premissas>.
- `04-quality-standards`: <seções, risco obrigatório>.
- ADRs: <...>. Tarefas anteriores reutilizadas: <classes/funções>.
- `open-questions`: <Q-nnn decididas que esta tarefa aplica>.

## Decisões da tarefa
<Tudo que o implementador precisaria decidir, decidido aqui: camadas e classes, algoritmo numerado, entrada, saída, erros (tabela status/type/quando), precedência dos erros, segurança, contrato, logs e métricas. Teste: dois implementadores produziriam o mesmo comportamento observável? Se algo não está aqui nem na spec, é lacuna → Q-nnn.>

## Latitude do implementador
Lista fechada; fora dela, qualquer escolha é decisão (→ Q-nnn). Cada escolha feita vai para a evidência.
- Nomes de métodos privados, variáveis locais e helpers de teste.
- <outro item explícito, se houver>.

## Saídas
- <caminhos entre crases, ex. `src/.../Arquivo.ext`>
- `docs/specs/reviews/T-xxx.evidence.md`

## Critérios de aceite
- **AC-001.1** <do 02, aplicado a esta tarefa>
- **AC-Txxx.1** <específico da tarefa, verificável>

## Testes exigidos
| AC | Teste (sugestão de nome) | Tag |
|---|---|---|
| AC-001.1 | <NomeDoTeste> | `RF-001` |
| AC-Txxx.2 | evidência: <comando e resultado esperado> | — |
(Uma linha por AC: `sdd-lint-task.sh` reprova AC sem linha aqui.)

Risco obrigatório desta tarefa: <qual, e a forma exigida>.

## Não-objetivos
- <o que fica para outra tarefa (T-yyy)>

## Checklist de pronto
- [ ] verify verde  - [ ] evidências registradas  - [ ] README atualizado e comandos executados nos shells suportados (ou N/A: <porquê>)  - [ ] revisão independente PASS  - [ ] aprovação humana
