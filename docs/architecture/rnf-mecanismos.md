# Requisitos não funcionais → mecanismos

Um **requisito não funcional** (RNF) descreve uma qualidade do sistema — reprodutível, seguro, justo nas medições —, e não uma função. Esta tabela liga cada RNF de `docs/specs/02-requirements.md` ao **mecanismo** que o garante, à forma de provar e à tarefa responsável. É a ponte entre "o que queremos" e "o que no código ou na infraestrutura faz isso acontecer".

| RNF | Atributo de qualidade | Mecanismo | Como se prova | Tarefa |
|---|---|---|---|---|
| RNF-001 | Reprodutibilidade | Docker Compose com o perfil `mvp`, imagens de versão fixa e o README em poucos comandos | Verificador do compose (teste) e README executado nos dois shells (evidência) | T-004, T-018 |
| RNF-002 | Medição justa | Teste de equivalência por *checksum* entre as versões; descritor com `fairness` | Testes | T-011, T-013, T-014 |
| RNF-003 | Isolamento das medições | *Advisory lock* do PostgreSQL (ADR-004) | Teste concorrente com os experimentos de teste | T-012 |
| RNF-004 | Manutenibilidade (fronteiras) | Regras do ArchUnit (ADR-025) | Testes com violações propositais | T-002 |
| RNF-005 | Determinismo dos testes | `Clock`, gerador de IDs e `Stopwatch` injetados; sementes fixas; PostgreSQL real | Três execuções idênticas (evidência) | T-002 |
| RNF-006 | Didática (padrão de explicação) | `scripts/doc-lint.sh` no CI e revisão humana | Testes do script e revisão | T-006, T-017, T-022 |
| RNF-007 | Capacidade da máquina de referência | Limites de memória por container e subir só o necessário | Medição do `docker stats` (evidência; linha de base na T-004, spike na T-007) | T-004, T-007, T-018 |
| RNF-008 | Desempenho da carga | SQL determinístico numa transação (ADR-009) | Tempo medido da carga de 1 milhão (evidência) | T-018 |
| RNF-009 | Segurança local | Portas em `127.0.0.1` (ADR-013), CORS restrito, erros sem stack trace, detector de segredos | Testes, verificador do compose e CI | T-002, T-004, T-012, T-018 |
| RNF-010 | Custo do estudo (H-01) | Registro de horas nos bastidores | Revisão do registro a cada tarefa (evidência) | todas |
| RNF-011 | Publicação com origem | Posts que citam o `runId`, a data e o ambiente de uma execução (ADR-017) | Lista de publicações (evidência) | T-030, T-031 |
| RNF-012 | Gate automático | Harness no repositório e job `harness` no CI | Testes do harness e da configuração | T-003, T-009 |
| RNF-013 | Repositório protegido e apresentável | Ruleset da `main`, segredos, CodeQL, Dependabot, arquivos de comunidade | Configurações conferidas (evidência) | T-008 |
| RNF-014 | Rastreabilidade das decisões | Um ADR por decisão e `scripts/adr-lint.sh` no CI | Testes do script | T-005 |
