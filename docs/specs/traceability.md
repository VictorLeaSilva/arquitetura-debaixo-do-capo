# Matriz de rastreabilidade

Gerado por `scripts/check-traceability.sh --write`. Não edite à mão.

| Requisito | Obrigatório com teste | Testes | Tarefas |
|---|---|---|---|
| RF-001 Carregar os dados de referência (F1) | sim | `backend/src/test/java/io/github/victorleasilva/capo/SchemaMigrationTest.java` `backend/src/test/java/io/github/victorleasilva/capo/lab/seed/ReferenceDataSeederTest.java` `backend/src/test/java/io/github/victorleasilva/capo/lab/seed/SeedSettingsTest.java` | T-010 |
| RF-002 Listar e detalhar os experimentos | sim | `backend/src/test/java/io/github/victorleasilva/capo/lab/descriptor/DescriptorStartupTest.java` `backend/src/test/java/io/github/victorleasilva/capo/lab/descriptor/ExperimentCatalogTest.java` `backend/src/test/java/io/github/victorleasilva/capo/lab/web/ExperimentApiTest.java` `backend/src/test/java/io/github/victorleasilva/capo/lab/web/OpenApiContractTest.java` | T-011 T-013 |
| RF-003 Executar um experimento | sim | `backend/src/test/java/io/github/victorleasilva/capo/lab/run/FairnessTest.java` `backend/src/test/java/io/github/victorleasilva/capo/lab/run/RunRequestValidatorTest.java` `backend/src/test/java/io/github/victorleasilva/capo/lab/web/RunApiTest.java` `backend/src/test/java/io/github/victorleasilva/capo/lab/web/RunLockTest.java` `backend/src/test/java/io/github/victorleasilva/capo/lab/web/RunPrecedenceTest.java` `backend/src/test/java/io/github/victorleasilva/capo/lab/web/RunValidationApiTest.java` | T-012 T-013 T-019 |
| RF-004 Consultar uma execução | sim | `backend/src/test/java/io/github/victorleasilva/capo/lab/run/OrphanRunTest.java` `backend/src/test/java/io/github/victorleasilva/capo/lab/web/RunApiTest.java` | T-012 |
| RF-005 Experimento "interseção de listas" (F2) | sim | — | T-013 |
| RF-006 Experimento "N+1" (F3) | sim | — | — |
| RF-007 Experimento "últimas unidades" da flash sale (F4) | sim | — | — |
| RF-008 Tela genérica do laboratório (F2–F4) | sim | — | — |
| RF-009 Telemetria de cada execução (F5) | sim | — | — |
| RF-010 Site de documentação e páginas de estudo (F6) | sim | `scripts/test/test-doc-lint.sh` `scripts/test/test-site-build.sh` | T-006 |
| RNF-001 Ambiente reproduzível em poucos comandos | sim | `scripts/test/test-compose-lint.sh` | T-004 |
| RNF-002 Comparação justa entre versões | sim | `backend/src/test/java/io/github/victorleasilva/capo/lab/descriptor/ExperimentCatalogTest.java` | T-011 T-013 |
| RNF-003 Medições isoladas (uma execução por vez) | sim | `backend/src/test/java/io/github/victorleasilva/capo/lab/web/RunApiTest.java` `backend/src/test/java/io/github/victorleasilva/capo/lab/web/RunLockTest.java` | T-012 |
| RNF-004 Fronteiras de arquitetura verificadas | sim | `backend/src/test/java/io/github/victorleasilva/capo/architecture/ArchitectureRulesTest.java` `backend/src/test/java/io/github/victorleasilva/capo/architecture/ArchitectureTest.java` | T-002 |
| RNF-005 Determinismo dos testes | evidência | — | T-002 |
| RNF-006 Páginas de estudo no padrão de explicação | sim | `scripts/test/test-doc-lint.sh` | T-006 |
| RNF-007 Cabe na máquina de referência | evidência | — | T-007 |
| RNF-008 Carga de 1 milhão de produtos | evidência | — | — |
| RNF-009 Segurança mínima de um laboratório local | sim | `backend/src/test/java/io/github/victorleasilva/capo/lab/web/CorsTest.java` `backend/src/test/java/io/github/victorleasilva/capo/lab/web/RunApiTest.java` `scripts/test/test-compose-lint.sh` | T-002 T-004 T-012 T-019 |
| RNF-010 Registro de horas para avaliar a H-01 | evidência | — | T-001 |
| RNF-011 Publicação dos resultados (F7) | evidência | — | — |
| RNF-012 Gate automático em cada tarefa | sim | `scripts/test/test-sdd-conf.sh` | T-003 |
| RNF-013 Repositório público protegido e apresentável | evidência | — | T-008 |
| RNF-014 Decisões de arquitetura rastreáveis | sim | `scripts/test/test-adr-lint.sh` | T-005 |
