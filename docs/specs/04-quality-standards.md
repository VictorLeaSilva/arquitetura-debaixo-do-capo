---
id: SPEC-04
status: draft
---
# 04 · Padrões de Qualidade e Segurança

Princípio: **o CI é a autoridade**; hooks locais só aceleram o feedback. Nenhuma ferramenta entra sem função clara (P2). Versões pinadas. As ferramentas marcadas "a confirmar na T-002" dependem do suporte a Java 25, verificado nas notas de versão oficiais (A3).

## 1. Gates
| Gate | Quando | O que roda | Bloqueia |
|---|---|---|---|
| G1 local | antes do commit (opcional, manual) | formatador (`spotless:apply`), `doc-lint.sh` | Nada (é atalho; o CI repete tudo) |
| G2 build completo | local e CI | formatação, análise estática, testes unitários, arquitetura, integração (Testcontainers), cobertura; no front-end: lint, tipos, testes; no site: build e `doc-lint.sh` | PR |
| G3 CI complementar | PR | detector de segredos, revisão de dependências (CVE), varredura das imagens Docker, verificador do `compose.yaml` (tags fixas, portas em `127.0.0.1`) | PR |
| G4 manual | antes de publicar um post | carga medida (RNF-007, RNF-008), JMH (diferencial), mutação (opcional) | Relatório |
| G5 release do MVP | tag `mvp-1.0` | auditoria de rastreabilidade, README executado nos dois shells | Release |

Sem hook obrigatório de pré-commit: o framework mais comum para isso depende de Python, que não está instalado (`05 §9`); o G1 é um comando que o autor roda quando quiser.

## 2. Ferramentas e limites
| Ferramenta | Papel | Regra de falha |
|---|---|---|
| Spotless + Palantir Java Format (a confirmar na T-002) | Formatação única do Java | Qualquer diferença |
| Análise estática do Java: SpotBugs ou Error Prone (escolha na T-002, conforme o suporte a Java 25) | Bugs comuns | Achado de prioridade alta ou média |
| ArchUnit | Fronteiras de RNF-004 | Qualquer violação |
| JaCoCo | Cobertura | Piso de 70% de linhas no total e 90% nos pacotes `..inventory..` e `..ordering..` [meta] |
| ESLint + Prettier + `tsc --noEmit` | Lint, formatação e tipos do front-end e do site | Qualquer erro |
| Vitest (cobertura v8) | Testes do front-end | Teste falhando; piso de 70% de linhas [meta] |
| `scripts/doc-lint.sh` | Estrutura das páginas de estudo (AC-010.2, AC-010.3) | Seção ausente; bloco de terminal fora das abas `shell`; bloco sem linguagem |
| `scripts/compose-lint.sh` | `compose.yaml` seguro e reprodutível (AC-RNF-001.2, AC-RNF-009.1) | Imagem sem tag fixa; porta publicada fora de `127.0.0.1` |
| Gitleaks | Segredos no repositório | Qualquer achado |
| `actions/dependency-review-action` | CVE de dependências novas no PR | Severidade alta ou crítica |
| Trivy | Imagens Docker do projeto | Severidade alta ou crítica com correção disponível |

## 3. Testes
- Pirâmide: unidade (domínio, algoritmos dos experimentos com `Stopwatch` falso) → integração com Testcontainers e PostgreSQL real (repositórios, N+1, flash sale, trava de execução) → API com MockMvc/cliente HTTP contra o contrato OpenAPI → componentes React com Vitest → poucos testes de ponta a ponta (opcionais no MVP).
- Determinismo (RNF-005): `Clock`, gerador de IDs e `Stopwatch` injetados; `Random(42)` nos lotes; imagens pinadas; sem `sleep`; sem rede externa; nada de banco em memória (H2) no lugar do PostgreSQL.
- Testes de integração usam `LAB_SEED_PRODUCTS=10000` (rápidos); a carga de 1 milhão é medida no G4.

### 3.1 Testes de risco obrigatórios (na forma exigida)
| Risco | Forma exigida | Tarefa |
|---|---|---|
| Venda acima do estoque na versão correta | 200 compradores **simultâneos** (liberados por latch), PostgreSQL real, 20 repetições sem nenhum `oversold` > 0 (AC-007.3) | Fase 2 (tarefa definida no `/sdd-planejar`) |
| A versão ingênua não demonstra o erro de forma estável | Barreira de teste da `RaceWindow` (Q-006), leitura e gravação em transações separadas, 20 de 20 repetições com `oversold` > 0 (AC-007.2) | Fase 2 |
| Duas execuções simultâneas contaminando as medições | Experimento `test-blocking` segurando a trava + um segundo `POST`: `409`; depois, `201` (AC-003.8) | Fase 1 |
| Trava presa depois de uma falha | Experimento `test-failing`; a execução seguinte recebe `201` (AC-003.9) | Fase 1 |
| Contagem de SQL errada no N+1 | PostgreSQL real; o número exato de comandos por versão (AC-006.1–AC-006.3) | Fase 1 |
| Versões "comparadas" com entradas diferentes | Teste de equivalência por `checksum` (RNF-002) | Fase 1 e Fase 2 |
| Carga de dados não determinística | Duas cargas em bancos diferentes; comparação de um produto e dos atributos (AC-001.3) | Fase 1 |
| Carga pela metade depois de uma queda | `SeedFailpoint` lançando exceção entre produtos e atributos; nada confirmado; reinício completo (AC-001.5) | Fase 1 |
| Execuções publicadas apagadas por uma recarga | Recarga com outro `LAB_SEED_PRODUCTS`; as linhas de `lab_run` continuam (AC-001.6) | Fase 1 |

As colunas "Tarefa" são preenchidas com os IDs T-0xx pelo `/sdd-planejar`, quando as Fases 1 e 2 forem detalhadas.

## 4. Segurança
- OWASP API Security Top 10 (2023), aplicado ao que existe no MVP:
  - **API4** (consumo irrestrito): limites de parâmetros nos descritores, uma execução por vez, tamanho máximo do corpo da requisição de 16 KB [meta].
  - **API8** (configuração insegura): portas só em `127.0.0.1`, erros sem stack trace, CORS liberado apenas para a origem da tela local.
  - **API1, API2, API5** (autorização e autenticação): não se aplicam no MVP (D-13); voltam na fatia 8.
- Segredos: só em `.env` (ignorado pelo git); o repositório tem `.env.example` com valores de desenvolvimento evidentes (`lab`/`lab`).
- Cabeçalhos HTTP de segurança padrão do Spring; `X-Content-Type-Options: nosniff` também no servidor do front-end.

## 5. Exceções e supressões
Toda exceção de regra ou supressão de achado tem **motivo, tarefa e data de validade**, fica versionada ao lado da configuração da ferramenta (ou em `docs/security/`), e volta a falhar depois da validade. Exemplo esperado: as versões ruins dos experimentos podem disparar regras de análise estática (laço quadrático, consulta dentro de laço). A supressão vale **só** para o pacote `..experiment..`, com o motivo "versão ruim proposital (P3)" e a tarefa que a criou.
