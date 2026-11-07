---
id: SPEC-06
status: draft
---
# 06 · Backlog de Tarefas

Cada tarefa vira `docs/specs/tasks/T-xxx.md` (modelo em `docs/specs/templates/tarefa.md`). A Fase 0 está listada; cada tarefa é detalhada **antes** de iniciar, com o mesmo rigor, para não congelar decisões cedo demais.

Ordem: *walking skeleton* (fundação e harness) → fatias verticais → transversais → documentação final.
Numeração por fase (T-001.., T-010.., T-020..) deixa espaço para tarefas novas sem renumerar.
A coluna "Cobre" aponta os requisitos de `02-requirements.md`; o `/sdd-planejar` confirma e completa ao detalhar cada tarefa.

| ID | Tarefa | Depende de | Cobre | Resultado verificável |
|---|---|---|---|---|
| **Fase 0 — Fundação e harness** |||||
| T-001 | Bootstrap do repositório: Maven Wrapper, projeto Spring Boot vazio com Java 25, versões pinadas, `.editorconfig`, `LICENSE` (MIT, código) e `LICENSE-docs` (CC BY-NC-SA 4.0, documentação), README mínimo, matriz de versões, registro de horas por tipo de trabalho nos bastidores (H-01) | — | RNF-010; Q-001, Q-004 | `mvnw verify` verde em clone limpo, em bash e PowerShell; A3 confirmada com fonte |
| T-002 | Gates de qualidade: formatação, análise estática, cobertura, teste de arquitetura (ArchUnit) + CI no GitHub Actions, com detector de segredos e varredura de dependências e imagens | T-001 | RNF-004, RNF-005, AC-RNF-009.3 | CI verde; uma violação proposital de cada tipo falha o build |
| T-003 | Harness: `verify-task.sh`, `check-traceability.sh`, `sdd-lint-task.sh`, `sdd-context.sh`, `sdd-status.sh` (skill `/sdd-harness`), com a marcação `# covers:` para testes de script | T-002 | `05 §4` (gate), `02 §3` (rastreabilidade) | Scripts testados com cenários de sucesso e de falha, rodando no Git Bash |
| T-004 | Infra local: Docker Compose com perfis, PostgreSQL e pilha de observabilidade com imagens pinadas e limites de memória, `.env.example`, `scripts/compose-lint.sh`; confirmar as dependências de D-15 e D-16 | T-001 | AC-RNF-001.2, AC-RNF-009.1, RNF-007 (base) | `docker compose up` saudável (health checks verdes) em bash e PowerShell |
| T-005 | Esqueleto da arquitetura: ADRs das decisões já tomadas (D-01 a D-17, Q-005 a Q-011 e as escolhas de stack de `00 §3`), C4 níveis 1 e 2, tabela RNF → mecanismo | T-001 | documentação obrigatória | Cada decisão D-xx em exatamente um ADR; diagramas renderizam |
| T-006 | Site de documentação: Docusaurus com abas bash/PowerShell sincronizadas, glossário, modelo de página de estudo no padrão DP-05 (10 pontos, "Testado com", "Depende da versão", perguntas de verificação), `scripts/doc-lint.sh` | T-001 | AC-010.1, AC-010.2, AC-010.3, AC-010.5, RNF-006 | O site sobe localmente; a página-modelo passa no checklist DP-05 |
| T-007 | Spike H-05: subir o cenário de dados distribuídos mais pesado previsto e medir a memória (DP-16: nas 2 primeiras semanas) | T-004 | H-05 | Medição registrada com data e ambiente; H-05 marcada validada ou refutada em `docs/produto/hipoteses.md` |
| T-008 | Segurança e apresentação do repositório: `SECURITY.md`, `CONTRIBUTING.md`, código de conduta, modelos de issue e de PR, `CODEOWNERS`, `.github/dependabot.yml`, README de apresentação (o que é, por que, como rodar, roteiro, licenças) e um checklist das configurações que só o autor faz no GitHub (proteção da `main`, varredura de segredos com bloqueio no push, CodeQL, Dependabot, relato privado de vulnerabilidade, permissões do Actions, descrição, topics, imagem social, GitHub Pages) | T-001 | DP-06, DP-22 | Arquivos presentes; checklist executado pelo autor e registrado na evidência, com capturas das configurações |
| **Fase 1 — MVP: carga de dados, laboratório e fatia 1 reduzida (F1, F2, F3, F5, F6)** |||||
| T-010 | Esquema do catálogo, do estoque e dos pedidos (Flyway) e carga determinística dos dados de referência, orquestrada pelo `lab` | T-002, T-004 | RF-001 | Testes de RF-001 verdes com PostgreSQL real (10.000 produtos) |
| T-011 | Descritores de experimento: JSON Schema, carga e validação na inicialização, `GET /experiments` e `GET /experiments/{id}`, contrato OpenAPI inicial | T-002 | RF-002, RNF-002 (AC-RNF-002.2) | Testes de RF-002 verdes; descritor inválido impede a inicialização |
| T-012 | Execução e consulta de execuções: validação, precedência dos erros, `413`, trava por advisory lock, `lab_run`, CORS, erros sem stack trace, `RUNNING` órfãs | T-010, T-011 | RF-003, RF-004, RNF-003, RNF-009 (AC-RNF-009.2, AC-RNF-009.4) | Testes com `test-blocking` e `test-failing` verdes |
| T-013 | Experimento `list-intersection` (versões ruim, média e boa, método de medição D-06, `Stopwatch`) | T-012 | RF-005, RNF-002 (AC-RNF-002.1) | Checksums iguais nas três versões; AC-005.3 com relógio falso |
| T-014 | Experimento `n-plus-one` (entidades JPA do catálogo, batch fetching, projeção, contagem pelo Hibernate) | T-012 | RF-006 | Contagens exatas 51 / 3 / 1 com PostgreSQL real |
| T-015 | Telemetria das execuções: spans `lab.run`/`lab.variant`, métricas, logs JSON por OTLP, SQL como spans (D-15, D-16) | T-012, T-004 | RF-009 | Testes de AC-009.1 a AC-009.3 verdes; evidência de AC-009.4 no Grafana |
| T-016 | Tela genérica do laboratório (React, Vite, Recharts) | T-011, T-012 | RF-008 | Testes Vitest de RF-008 verdes, inclusive com o experimento fictício |
| T-017 | Páginas de estudo de `list-intersection` e `n-plus-one`, teste de consistência descritor ↔ página | T-006, T-013, T-014 | RF-010 (AC-010.4), RNF-006 (AC-RNF-006.2) | `doc-lint.sh` verde; revisão do conteúdo no padrão DP-05 |
| T-018 | Aplicação e front-end em containers no perfil `mvp`; README em até 3 comandos; medições de memória e da carga de 1 milhão | T-015, T-016 | RNF-001 (AC-RNF-001.1), RNF-007, RNF-008, RNF-009 (AC-RNF-009.5) | Evidência nos dois shells num clone limpo; `docker stats` e tempo de carga registrados |
| **Fase 2 — MVP: experimento "últimas unidades" da flash sale (F4, F5, F6)** |||||
| T-020 | Estoque (domínio hexagonal) e pedido da flash sale: versão correta com `UPDATE` condicional | T-012 | RF-007 (AC-007.1, AC-007.3, AC-007.4, AC-007.5) | 20 de 20 repetições com 200 compradores e `oversold` = 0 |
| T-021 | Versão ingênua (`RaceWindow`, barreira de teste) e o experimento `flash-sale-last-units` no laboratório | T-020 | RF-007 (AC-007.2, AC-007.6, AC-007.7) | 20 de 20 repetições com `oversold` > 0 |
| T-022 | Página de estudo da flash sale | T-006, T-021 | RF-010 (AC-010.4), RNF-006 | `doc-lint.sh` verde; revisão do conteúdo |
| **Fase 3 — MVP: publicação (F7) e avaliação da H-01** |||||
| T-030 | Post 1 (fatia 1 reduzida), com medições citando `runId` | T-017, T-018 | RNF-011 | Link e data em `docs/produto/publicacoes.md` |
| T-031 | Post 2 (flash sale) e avaliação da H-01 com o registro de horas | T-022, T-030 | RNF-011, RNF-010 | H-01 marcada validada ou refutada; decisão sobre a meta (DP-13) registrada |
| **Depois do MVP — fatias do roadmap (DP-13), cada uma com especificação própria** |||||

## Marcos
- **M0** (T-001..T-008): repositório com gates, harness, infra local, site de documentação, segurança e apresentação, e H-05 testada.
- **M1** (fim da Fase 1): primeiro experimento rodando na tela genérica, com página de estudo e post 1.
- **M2** (fim da Fase 3): MVP completo e H-01 avaliada; meta de até 10 semanas desde o início da implementação [meta, DP-17].

## Corte de escopo (se o tempo apertar, nesta ordem)
1. Benchmarks com JMH (diferencial).
2. Gráficos além da curva de crescimento e da tabela de medições.
3. Acabamento visual da tela genérica.
4. Terceira versão (média) de um experimento, mantendo a ruim e a boa.

**Nunca cortar:** o teste de cada critério de aceite, a página de estudo no padrão DP-05, o método registrado de cada medição (P7), o registro de horas (H-01) e o teste concorrente da flash sale (P4).
