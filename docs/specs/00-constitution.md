---
id: SPEC-00
status: draft
---
# 00 · Constituição do Projeto

Projeto **Arquitetura Debaixo do Capô** (`arquitetura-debaixo-do-capo`). Origem: descoberta de produto em `docs/produto/` (gate verde em 2026-11-07).

## 1. Propósito
Um laboratório aberto, em português, que roda na máquina do leitor e mostra conceitos de arquitetura de software num e-commerce com flash sale, em versões ruim, média e boa, medidas lado a lado, explicando o que acontece por baixo, sem pressupor nada além de Java básico, inclusive quando **não** usar um padrão [docs/produto/visao.md, DP-12, DP-18]. Serve primeiro ao estudo do autor (perfil primário, DP-08) e depois a desenvolvedores que leem o repositório e os posts.

**Entregue (MVP)** = as 7 capacidades de `docs/produto/mvp.md` funcionando, testadas, documentadas e publicadas:

| | Capacidade | Classe |
|---|---|---|
| F1 | Subir o ambiente local com poucos comandos (meta: até 3, RNF-001) e carregar um volume grande de dados | **obrigatório** |
| F2 | Experimento "buscar itens numa lista" (ruim, média e boa), com tempo e curva de crescimento | **obrigatório** |
| F3 | Experimento "N+1", com o número de consultas de cada versão | **obrigatório** |
| F4 | Experimento "últimas unidades" da flash sale (venda ingênua × venda correta) | **obrigatório** |
| F5 | Telemetria básica de cada experimento | **obrigatório** |
| F6 | Página de estudo por experimento no padrão de explicação (DP-05), com comandos em bash e PowerShell | **obrigatório** |
| F7 | 2 posts com resultado medido | **obrigatório** (fora do código; registrado em `docs/produto/`) |
| — | Benchmarks com JMH para os algoritmos em memória, gráficos de dispersão, acabamento visual do laboratório | diferencial |

Depois do MVP, cada fatia do roadmap (DP-13) repete este ciclo com a sua própria especificação.

## 2. Princípios (usados para desempatar decisões)
| # | Princípio |
|---|-----------|
| P1 | Requisito obrigatório vem antes de qualquer diferencial. |
| P2 | Simplicidade comprovável e pragmatismo acima de cerimônia (DP-07): camada, interface, padrão ou componente só entra com um requisito, risco ou benefício visível que o justifique. Interface só para variação real, fronteira com o que não controlamos ou API pública de módulo. O que foi **rejeitado**, e por quê, vai para ADR. |
| P3 | **Comparação justa:** as versões de um experimento recebem as mesmas entradas, o mesmo volume e o mesmo ambiente. A versão ruim é ruim de propósito e rotulada como tal. Um teste prova que as versões dão o mesmo resultado funcional, exceto quando o experimento existe justamente para mostrar um erro (ex.: vender mais do que o estoque). |
| P4 | **Invariantes do domínio:** dinheiro é exato (`BigDecimal` criado a partir de texto, nunca `double`); na versão correta, o estoque nunca fica negativo; risco de concorrência se prova com teste concorrente, nunca sequencial. |
| P5 | **Ensinar sem pressupor:** a página de estudo faz parte do pronto. Toda explicação segue o padrão de 10 pontos (DP-05) e termina com perguntas de verificação (DP-21). |
| P6 | Todo comportamento tem critério de aceite testável e rastreável (requisito → teste). |
| P7 | Número só entra na documentação se medido (data, ambiente, método e repetições) ou se marcado como **meta**. Medições locais são comparativas: valem as razões entre versões, não os milissegundos absolutos. |
| P8 | A spec manda. Lacuna ou conflito: registrar em `open-questions.md`, não improvisar. |
| P9 | Algo só é declarado **provado** com evidência reproduzível (teste, script, resultado versionado) registrada em `docs/specs/reviews/T-xxx.evidence.md` e confirmada pela revisão independente. Fora disso: **a provar (T-xxx)** ou **só desenho**. |
| P10 | **Reprodutível por qualquer leitor:** só local e custo zero (DP-11), versões e imagens pinadas, comandos em bash e PowerShell. |

## 3. Stack fixada
| Área | Escolha | Observação |
|------|---------|------------|
| Linguagem | Java 25 (LTS) | Instalado na máquina do autor: 25.0.5 [fato: `java -version`, 2026-11-07]. Fallback só com ADR |
| Framework | Spring Boot | Versão estável mais recente compatível com Java 25, confirmada e pinada na T-001 (A3) |
| Build | Maven, via Maven Wrapper (`mvnw` / `mvnw.cmd`) | Q-004. Ninguém precisa instalar o Maven; versões pinadas |
| Persistência | PostgreSQL | MongoDB e Cassandra só na fatia 6 (dados distribuídos). Versão da imagem pinada na T-004 |
| Testes | JUnit + AssertJ (versões do BOM do Spring Boot); Testcontainers para integração; ArchUnit para arquitetura; teste concorrente para F4; JMH (diferencial); Vitest no front-end | `04-quality-standards.md` |
| Front-end | React + TypeScript + Vite: uma tela genérica que monta qualquer experimento a partir do descritor dele; gráficos com Recharts | Q-002, Q-005, DP-21. Versões pinadas na tarefa que cria o front |
| Contratos | OpenAPI contract-first para a API do laboratório; JSON Schema para o descritor de experimento | `03 §Contratos` |
| Observabilidade | Suporte nativo do Spring Boot (Micrometer + exportação OTLP) e pilha Grafana local | Q-008. Básica no MVP (F5); o agente Java do OpenTelemetry e a comparação com a instrumentação manual ficam para a fatia 7 |
| Infra local | Docker Compose com perfis (cada experimento sobe só o que precisa) | Máquina de referência: 16 GB de RAM, ~7,6 GB para o Docker (A2) |
| Docs | Docusaurus (abas bash/PowerShell sincronizadas, botão de copiar) + Markdown + Mermaid | Q-003 |

Cada escolha desta tabela vira um ADR na T-005, com a alternativa rejeitada. As principais: PostgreSQL (rejeitado o MySQL: o laboratório usa recursos do PostgreSQL como *advisory lock*, `jsonb` e índice parcial); React + Vite (rejeitados Angular e Vue: React é o mais pedido em vagas e o mais documentado; Vite é o padrão atual de build); Maven (Q-004); Docusaurus (Q-003); Recharts (Q-005).

## 3.1 Arquitetura interna
Monólito modular, com um módulo por domínio (`01-domain-capabilities.md`). A profundidade da arquitetura é escolhida **por módulo** (P2):
- Módulos de leitura simples (catálogo): camadas diretas, controller → service → repositório, sem portas nem mapeamento duplicado.
- Módulos com regra de negócio e concorrência (estoque e pedido da flash sale): hexagonal enxuta, com `domain` puro (sem Spring nem JPA) → `application` (casos de uso + portas) → `adapter.in/out`.
- O módulo `lab` executa experimentos e coleta medições; não contém regra de negócio da loja.
- Dependência entre módulos só pela API pública do módulo. As regras são verificadas só com ArchUnit no MVP (Q-007); o Spring Modulith é reavaliado na fatia 5.

## 4. Convenções
- Documentação em português do Brasil; código inteiramente em inglês, inclusive os termos de domínio (`Product`, `Order`, `Stock`) [DP-04].
- Tempo: armazenado em UTC. Relógio sempre injetado (`java.time.Clock`).
- IDs: gerados por um gerador injetado, nunca direto no domínio (para testes determinísticos).
- Erros HTTP: Problem Details (RFC 9457, `application/problem+json`).
- Marcação de requisito em teste: JUnit `@Tag("RF-001")`; Vitest com o ID no nome do teste: `it('[RF-001] ...')`.
- Fim de linha LF em todo arquivo de texto, exceto `.bat`/`.cmd` (CRLF), garantido por `.gitattributes`.
- Branch `task/T-xxx-slug`; Conventional Commits; um PR por tarefa. Spec muda em `docs/spec-T-xxx`.
- **Operações de git são do autor:** criar branch, commit, push, trocar de branch e pull são feitos pelo humano. O agente prepara os arquivos e entrega os comandos prontos em PowerShell, explicados.

## 5. Glossário
- **Experimento:** uma pergunta de arquitetura respondida com medição, por exemplo "qual o custo do N+1?". Tem duas ou mais versões, uma carga de dados e as medições que coleta.
- **Versão (ruim, média, boa):** uma implementação do experimento. A ruim é proposital e serve de comparação.
- **Descritor de experimento:** os dados que descrevem um experimento (título, versões, medições, perguntas de verificação), dos quais a tela genérica monta a interface (DP-21).
- **Medição:** um número coletado numa execução, com data, ambiente, método e repetições (P7).
- **Fatia:** uma etapa do roadmap que entrega experimentos de um tema, com testes, páginas de estudo e post (DP-13).
- **Página de estudo:** a página do site de documentação que explica um experimento no padrão de 10 pontos (DP-05).
- **Flash sale:** uma venda relâmpago em que muitos compradores disputam poucas unidades ao mesmo tempo.
- **Overselling (venda acima do estoque):** vender mais unidades do que existem, efeito de uma condição de corrida.
- **Condição de corrida:** quando o resultado depende da ordem em que operações simultâneas acontecem. Exemplo: dois compradores leem "1 unidade" antes de qualquer um gravar a compra.
- **Execução (run):** uma rodada de um experimento com versões e parâmetros escolhidos; fica registrada em `lab_run`, com as medições e o ambiente.
- **Janela de corrida:** o intervalo entre ler um valor e gravar o novo valor, no qual outra operação pode se intrometer.
- **Checksum do resultado:** um resumo (hash) do resultado funcional de uma versão, usado para provar que versões diferentes chegaram à mesma resposta (P3).
- **Aquecimento (warmup):** execuções descartadas antes de medir, para a JVM compilar e otimizar o código (JIT) antes da medição.
- **Advisory lock:** uma trava do PostgreSQL identificada por um número escolhido pela aplicação, sem relação com nenhuma tabela.
- **Dados de referência:** o catálogo gerado de forma determinística para os experimentos (D-09).

## 6. Premissas assumidas (revisar; mudança exige atualizar spec e ADR)
| # | Premissa |
|---|----------|
| A1 | H-02 ("construir e medir ensina mais do que ler") é aceita como premissa e não será testada agora [DP-20]. |
| A2 | O ambiente de referência das medições é a máquina do autor: 16 GB de RAM, ~7,6 GB para o Docker, 12 CPUs lógicas, Windows 11 com Docker Desktop [fato: medido em 2026-11-07]. Toda medição publicada cita esse ambiente. |
| A3 | Existe uma versão estável do Spring Boot com suporte a Java 25. **A confirmar** na T-001, nas notas de versão oficiais. |
| A4 | O leitor sabe lógica de programação e Java básico [DP-09]. |
| A5 | Ninguém paga nem se cadastra: sem login, sem pagamento, sem multiusuário [DP-06, DP-21]. |

## 7. Fora de escopo (e onde aparece como evolução)
- Experimento de paginação e o restante da fatia 1 (índices, cache, top-K): fatia 1 completa, depois do MVP.
- O restante da fatia 3 (níveis de lock, idempotência, rate limiting): fatia 3 completa.
- Fatia 2 (padrões de projeto com carrinho e pagamento) e fatias 4 a 9: roadmap, uma por vez (DP-13).
- Serviço em segunda linguagem (Node/NestJS): ideia para a fatia 5, ainda sem decisão.
- MongoDB, Cassandra, mensageria, balanceador de carga: fatias 4 a 6.
- Login, progresso por pessoa e execução isolada por pessoa [DP-21].
- Qualquer uso de nuvem paga: fora para sempre [DP-11].
- Acabamento visual além do necessário para ler as medições.

## 8. Definição de Pronto global (release do MVP)
- Todos os requisitos obrigatórios com ≥ 1 teste rastreado e verde.
- Gate completo verde a partir de clone limpo.
- O README permite subir o ambiente e rodar um experimento em ≤ 3 comandos [meta], em bash e em PowerShell.
- Cada experimento tem página de estudo publicada no site, no padrão DP-05, com as caixas "Testado com" e "Depende da versão" (quando houver) e perguntas de verificação.
- Nenhum número de desempenho sem origem (medição com método ou meta rotulada).
- Horas registradas por tipo de trabalho, para a avaliação da H-01: MVP em até 10 semanas [meta, DP-17].
