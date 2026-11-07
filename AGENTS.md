# AGENTS.md — Instruções para agentes de código

Projeto: **Arquitetura Debaixo do Capô** (`arquitetura-debaixo-do-capo`).
Método: **Spec Driven Development (SDD)** com harness de verificação e revisão por tarefa.

## Ordem de leitura (obrigatória antes de qualquer tarefa)
1. `docs/specs/00-constitution.md` (princípios, stack, convenções, premissas)
2. O arquivo da tarefa em `docs/specs/tasks/T-xxx.md` (ou o item em `docs/specs/06-tasks.md`)
3. Apenas as seções de spec que a tarefa referencia (`02-requirements`, `03-design-contracts`, `04-quality-standards`)
4. `docs/specs/05-harness.md` (como verificar, como será revisado, armadilhas conhecidas)

## Regras inegociáveis
- **Hierarquia de autoridade:** constituição > requisitos > design/contratos > tarefa. Conflito ou lacuna: **pare**, registre em `docs/specs/open-questions.md` e aguarde decisão. Nunca improvise.
- Altere **somente** os caminhos listados em `allowed_paths` da tarefa.
- Não adicione dependência, endpoint, coluna, tópico, chave de configuração ou módulo que não esteja na spec.
- Teste primeiro: escreva os testes dos critérios de aceite (AC) e veja-os falhar antes de implementar.
- Todo teste referencia o requisito que cobre: JUnit `@Tag("RF-xxx")` / `@Tag("RNF-xxx")`; Vitest com o ID no nome: `it('[RF-xxx] ...')`.
- Invariantes do domínio: dinheiro em `BigDecimal` criado de texto (nunca `double`); tempo pelo `Clock` injetado; IDs por gerador injetado; estoque nunca negativo na versão correta; versões de um experimento comparadas de forma justa (`00 §2 P3`); a versão "ruim" nunca é alcançável fora do Laboratório.
- Pragmatismo (`00 §2 P2`): nenhuma interface ou camada sem justificativa escrita na spec ou num ADR.
- Código em inglês; documentação em português, no padrão de explicação DP-05 (`docs/produto/decisoes.md`).
- Não registre em log: tokens, segredos, dados pessoais.
- **Palco × bastidores (DP-22):** o repositório é público. Mensagens literais do autor, contexto pessoal, críticas da descoberta, registro de horas e ideias de produto ficam em `.bastidores/` (ignorada pelo git, com repositório privado próprio). Nos arquivos públicos, só texto revisado e neutro.
- Uma tarefa = um branch `task/T-xxx-slug` = um PR. Commits em Conventional Commits.
- **Git é do autor:** nunca execute commit, push, checkout ou pull. Entregue os comandos prontos em PowerShell, com a explicação do que cada um faz.

## Definição de pronto de uma tarefa
`scripts/verify-task.sh T-xxx` verde **e** relatório de revisão independente `docs/specs/reviews/T-xxx.md` com veredito PASS **e** aprovação humana.

**README vivo:** se a tarefa muda como subir ou usar a aplicação, o mesmo PR atualiza o `README.md`, e os comandos afetados são executados de novo em bash e em PowerShell, numa stack isolada; a saída real vai para a evidência. Se não muda, o checklist diz "N/A" e o porquê.

## Comandos
Disponíveis depois da T-001 (build) e da T-003 (harness):
- `./mvnw -q verify` — gate completo local do backend: formato, lint, testes, arquitetura, cobertura (detalhes na T-002)
- `scripts/sdd-lint-task.sh T-xxx` — a tarefa está pronta para executar? (rode antes de começar)
- `scripts/sdd-context.sh T-xxx` — o contexto exato da tarefa num só texto (leia isso em vez das specs inteiras)
- `BASE=<branch base> scripts/verify-task.sh [--rapido] T-xxx` — gate da tarefa (contrato, escopo, dependências, rastreabilidade, padrões, artefatos, build); `--rapido` para iterar, sem ele no fim
- `scripts/sdd-status.sh` — estado do backlog e próxima tarefa elegível
- `scripts/check-traceability.sh [--write]` — requisitos obrigatórios sem teste; `--write` regenera `docs/specs/traceability.md`
- `scripts/test/run-tests.sh` — testes dos scripts do harness
