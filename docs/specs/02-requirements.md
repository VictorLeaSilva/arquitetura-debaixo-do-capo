---
id: SPEC-02
status: draft
---
# 02 · Requisitos Refinados e Critérios de Aceite

Escopo: o MVP (`00 §1`, capacidades F1–F7). Revisado conforme `spec-reviews/SPEC-REVIEW-001.md`.

Convenção: cada critério tem ID `AC-<req>.<n>` em Dado/Quando/Então. Cada teste referencia o requisito que cobre:
- JUnit: `@Tag("RF-001")`;
- Vitest: o ID no nome do teste, `it('[RF-008] ...')`;
- testes de script (`scripts/test/`): uma linha `# covers: RNF-006` no caso de teste.

`scripts/check-traceability.sh` falha se um requisito obrigatório (`§3`) não tiver ao menos um teste.

Autorização (401/403) e isolamento entre clientes **não se aplicam**: o laboratório é local, sem login e sem multiusuário (A5, D-13). Os erros HTTP seguem `03 §5.1`. Os testes automatizados usam `LAB_SEED_PRODUCTS=10000`; os números com 1.000.000 de produtos são provados por evidência medida (RNF-008).

## 1. Requisitos funcionais

### RF-001 · Carregar os dados de referência (F1)
Os dados são gerados de forma determinística (D-09), numa única transação, na primeira inicialização ou quando o tamanho configurado muda.
- **AC-001.1** Dado um banco vazio e `LAB_SEED_PRODUCTS=10000`, quando a aplicação termina de inicializar, então `product` tem `10000` linhas, `category` tem `50`, cada produto tem entre 3 e 8 linhas em `product_attribute`, e `flash_sale_stock` tem 1 linha (produto 1, `available` = 10).
- **AC-001.2** Dado um banco já carregado com 10.000 produtos e o mesmo `LAB_SEED_PRODUCTS`, quando a aplicação reinicia, então nenhuma linha é inserida nem apagada e o log registra o evento `seed.skipped`.
- **AC-001.3** Dado `LAB_SEED_PRODUCTS=10000`, quando a carga termina, então o produto `SKU-0000042` tem `name` = `Produto 42`, categoria `Categoria 43`, `price` = `365.98`, `created_at` = `2026-01-01T00:00:00Z` e exatamente 3 atributos: `attr-1` = `valor-302`, `attr-2` = `valor-604` e `attr-3` = `valor-906`. O mesmo vale em dois bancos diferentes (determinismo).
- **AC-001.4** Dado `LAB_SEED_PRODUCTS` igual a `0`, `-5`, `1000001` ou `abc`, quando a aplicação inicializa, então ela não sobe, o log cita a chave `LAB_SEED_PRODUCTS` e o valor recebido, e nenhuma linha é inserida.
- **AC-001.5** Dado o ponto de falha de teste da carga (`03 §3.1`, A1) configurado para falhar depois de inserir os produtos e antes dos atributos, quando a carga roda, então nada é confirmado (`product` continua com as linhas de antes); e, quando a aplicação reinicia sem o ponto de falha, a carga termina e as contagens de AC-001.1 valem.
- **AC-001.6** Dado um banco com 10.000 produtos, pedidos e 2 execuções em `lab_run`, quando a aplicação reinicia com `LAB_SEED_PRODUCTS=20000`, então `product` tem exatamente `20000` linhas geradas do zero, `customer_order` fica vazia, as 2 linhas de `lab_run` **continuam** intactas (Q-011), e o log registra `seed.completed`.

### RF-002 · Listar e detalhar os experimentos
- **AC-002.1** Dado o laboratório no ar, quando `GET /api/v1/lab/experiments`, então a resposta é `200` com os 3 experimentos do MVP (`list-intersection`, `n-plus-one`, `flash-sale-last-units`), cada um com `id`, `title`, `summary` e `studyPage`. Descritores que existem só no classpath de teste (`03 §4.1`) não aparecem fora dos testes.
- **AC-002.2** Dado o experimento `n-plus-one`, quando `GET /api/v1/lab/experiments/n-plus-one`, então a resposta é `200` com o descritor completo, igual ao arquivo `n-plus-one.json` (`03 §4`).
- **AC-002.3** Dado um id inexistente, quando `GET /api/v1/lab/experiments/foo`, então a resposta é `404` com `type` `/problems/experiment-not-found`.
- **AC-002.4** Dado um arquivo de descritor que não obedece ao JSON Schema (por exemplo, sem `variants`), quando a aplicação inicializa, então ela não sobe, e o log cita o arquivo e o campo inválido.

### RF-003 · Executar um experimento
- **AC-003.1** Dado o experimento `list-intersection`, quando `POST /api/v1/lab/experiments/list-intersection/runs` com `{"variants":["bad","good"],"parameters":{"sizes":[1000,5000],"repetitions":3}}`, então a resposta é `201`, com o header `Location: /api/v1/lab/runs/{id}` e um corpo com `status` `COMPLETED`, 4 resultados (2 versões × 2 tamanhos), `traceId`, `environment` (campos de `03 §3`) e os parâmetros efetivos. Uma linha nova existe em `lab_run`.
- **AC-003.2** Dado um parâmetro omitido, quando a execução é pedida sem `repetitions`, então o valor padrão do descritor (`5`) é usado e aparece nos parâmetros efetivos.
- **AC-003.3** Dado `"variants":[]` ou `"variants":["worst"]`, quando a execução é pedida, então a resposta é `400` com `type` `/problems/invalid-parameter` e o campo `variants` em `errors`; nada é gravado em `lab_run`.
- **AC-003.4** Dado `"sizes":[1234]` (fora da lista permitida), `"repetitions":0`, `"repetitions":11` ou um parâmetro desconhecido `"foo":1`, quando a execução é pedida, então a resposta é `400` com `type` `/problems/invalid-parameter` e o nome do parâmetro em `errors`; nada é gravado.
- **AC-003.5** Dado `"variants":["bad"],"parameters":{"sizes":[100000]}` e o limite da versão `bad` de 20.000, quando a execução é pedida, então a resposta é `422` com `type` `/problems/size-exceeds-variant-limit`, citando a versão e o limite; nada é gravado.
- **AC-003.6** Dado um corpo que não é JSON válido, quando a execução é pedida, então a resposta é `400` com `type` `/problems/malformed-request`.
- **AC-003.7** Dado um experimento inexistente, quando `POST /api/v1/lab/experiments/foo/runs`, então a resposta é `404` com `type` `/problems/experiment-not-found`.
- **AC-003.8** Dada uma execução do experimento de teste `test-blocking` (`03 §4.1`) parada dentro da trava, esperando o sinal do teste, quando um segundo `POST` válido (de qualquer experimento) chega, então ele recebe `409` com `type` `/problems/run-in-progress`; e, depois que o teste libera o sinal, a primeira recebe `201`. Existe exatamente uma linha nova em `lab_run`.
- **AC-003.9** Dada uma execução do experimento de teste `test-failing`, cuja versão lança uma exceção, quando ela roda, então a resposta é `500` com `type` `/problems/run-failed` e a propriedade `runId`; a linha em `lab_run` fica com `status` `FAILED`; e uma execução seguinte de `list-intersection` recebe `201` (a trava foi liberada).
- **AC-003.10** Precedência dos erros, quando mais de um se aplica: `404` → `413` → `400` → `422 size-exceeds-variant-limit` → `422 insufficient-data` → `409`. Exemplo: um experimento inexistente com corpo inválido responde `404`; `"variants":["bad"],"sizes":[100000]` com `LAB_SEED_PRODUCTS=10000` responde `size-exceeds-variant-limit`.
- **AC-003.11** Dado um corpo com mais de 16 KB [meta], quando a execução é pedida, então a resposta é `413` com `type` `/problems/payload-too-large`; nada é gravado.

### RF-004 · Consultar uma execução
- **AC-004.1** Dada uma execução concluída, quando `GET /api/v1/lab/runs/{id}`, então a resposta é `200` com o mesmo corpo devolvido no `201` de RF-003.
- **AC-004.2** Dado um UUID que não existe, quando `GET /api/v1/lab/runs/{id}`, então a resposta é `404` com `type` `/problems/run-not-found`; dado um id que não é UUID (`abc`), a resposta é `400` com `type` `/problems/invalid-parameter`.
- **AC-004.3** Dada uma execução com `status` `FAILED`, quando `GET /api/v1/lab/runs/{id}`, então a resposta é `200` com `status` `FAILED`, `results` = `null` e `failureReason` preenchido.
- **AC-004.4** Dada uma linha `RUNNING` deixada por uma aplicação que caiu no meio de uma execução, quando a aplicação reinicia, então essa linha passa a `FAILED` com `failureReason` = `interrompida: a aplicação reiniciou durante a execução`.

### RF-005 · Experimento "interseção de listas" (F2)
Pergunta: *quais dos n SKUs de um lote existem no catálogo de n produtos?* O lote tem n/2 SKUs existentes, sorteados **sem reposição**, e n/2 inexistentes, embaralhados com semente fixa (`03 §3.1`, A3).
- **AC-005.1** Dado o mesmo lote de tamanho 10.000, quando as versões `bad` (laço com `List.contains`, O(n²)), `medium` (ordenação e busca binária, O(n log n)) e `good` (`HashSet`, O(n)) são executadas, então as três devolvem `matches` = `5000` e o mesmo `checksum` (P3).
- **AC-005.2** Dados os tamanhos 1.000 e 5.000, quando uma execução mede as versões `bad` e `good`, então cada resultado traz `durationMsMedian`, `durationMsMin` e `durationMsMax`.
- **AC-005.3** Dado um `Stopwatch` falso que devolve 1000 ms nas 2 primeiras medições (aquecimentos) e 10, 20 e 30 ms nas seguintes, quando 3 repetições são executadas, então `durationMsMedian` = `20.0`, `durationMsMin` = `10.0` e `durationMsMax` = `30.0` (os aquecimentos ficam fora).
- **AC-005.4** Dado `LAB_SEED_PRODUCTS=10000` e o tamanho 10.000, quando as versões `medium` e `good` são executadas, então a execução termina com `COMPLETED`, e o número de comandos SQL executados **durante** o intervalo medido é `0`: a leitura do catálogo acontece antes da medição (P3).
- **AC-005.5** Dado um tamanho maior do que o número de produtos carregados (100.000 com `LAB_SEED_PRODUCTS=10000`, nas versões `medium` e `good`), quando a execução é pedida, então a resposta é `422` com `type` `/problems/insufficient-data`.

### RF-006 · Experimento "N+1" (F3)
Pergunta: *quantas consultas ao banco são feitas para listar uma página de n produtos com os seus atributos?* O parâmetro `pageSizes` faz o papel de "tamanho" nos resultados, no span e no gráfico.
- **AC-006.1** Dado `"pageSizes":[50]`, quando a versão `bad` (carregamento preguiçoso, um acesso por produto) é executada, então `sqlStatements` = `51` (1 + n).
- **AC-006.2** Dado `"pageSizes":[50]`, quando a versão `medium` (*batch fetching* com lote de 25) é executada, então `sqlStatements` = `3` (1 + ⌈50/25⌉).
- **AC-006.3** Dado `"pageSizes":[50]`, quando a versão `good` (uma consulta com JOIN e projeção para DTO) é executada, então `sqlStatements` = `1`.
- **AC-006.4** Dados `"pageSizes":[10,50,100,500]`, quando as três versões são executadas, então todas devolvem os mesmos produtos, na mesma ordem (por `id` crescente), com os mesmos atributos: `products`, `attributes` e `checksum` iguais nas três (P3).
- **AC-006.5** Dado `"pageSizes":[20]`, quando a execução é pedida, então a resposta é `400` com o campo `pageSizes` em `errors`.

### RF-007 · Experimento "últimas unidades" da flash sale (F4)
Pergunta: *o que acontece quando muitos compradores disputam ao mesmo tempo poucas unidades?* Cada versão roda **uma vez**, sem aquecimento, e o estoque é reiniciado antes de cada versão (`03 §3.1`, A5).
- **AC-007.1** Dados `initialStock` = 10 e `buyers` = 1000, quando a versão `good` (atualização atômica condicional) é executada, então `unitsSold` = `10`, `oversold` = `0`, `finalAvailable` = `0` e `rejectedSoldOut` = `990`; e existem exatamente 10 linhas em `customer_order` com o `lab_run_id` da execução e `lab_variant` = `good`.
- **AC-007.2** Dados `initialStock` = 10, `buyers` = 200 e a barreira de teste da janela de corrida (todos os compradores leem o estoque antes de qualquer um gravar; Q-006), quando a versão `bad` (ler numa transação, gravar em outra, sem proteção) é executada, então `oversold` > `0` e `unitsSold` > `10`, em 20 de 20 repetições do teste.
- **AC-007.3** Dados `initialStock` = 10 e `buyers` = 200 liberados juntos por um latch, num PostgreSQL real, quando a versão `good` é executada 20 vezes, então as 20 terminam com `oversold` = `0` e `finalAvailable` = `0`.
- **AC-007.4** Dada a tabela `flash_sale_stock`, quando um teste tenta gravar `available` = `-1`, então o banco recusa (constraint `CHECK (available >= 0)`).
- **AC-007.5** Dados `initialStock` = 0 ou 101, ou `buyers` = 5 ou 2001, quando a execução é pedida, então a resposta é `400` com o parâmetro em `errors`.
- **AC-007.6** Dada uma execução, quando ela termina, então cada versão traz também `durationMs`, `purchasesPerSecond` e `latencyP95Ms`. A latência de uma compra vai da liberação do latch até o fim da transação de gravação, incluindo a espera por uma conexão (`03 §3.1`, A5).
- **AC-007.7** Dado `raceWindowMs` = 2, quando a execução é pedida, então a resposta é `400` com `raceWindowMs` em `errors`. Dado `raceWindowMs` omitido, então o padrão `1` aparece nos parâmetros efetivos.

### RF-008 · Tela genérica do laboratório (F2–F4)
Uma única tela React monta qualquer experimento a partir do descritor dele (Q-002, D-05).
- **AC-008.1** Dado o descritor de `list-intersection` devolvido pela API, quando a tela abre o experimento, então ela mostra o título, o resumo, uma opção por versão, um controle por parâmetro (com o valor padrão já selecionado) e um link para a página de estudo, sem nenhum código específico desse experimento.
- **AC-008.2** Dado um descritor de teste com um quarto experimento fictício, quando a tela o abre, então ela o monta da mesma forma (prova de que a tela é genérica).
- **AC-008.3** Dada uma execução em andamento, quando o botão "Executar" foi clicado, então ele fica desabilitado e um indicador de progresso aparece até a resposta chegar.
- **AC-008.4** Dada uma resposta `201`, quando a tela a recebe, então ela mostra uma tabela com uma linha por versão × tamanho e uma coluna por medição, com a unidade do descritor. Quando o descritor tem `chart` e há mais de um tamanho, mostra também um gráfico com uma linha por versão: no eixo x, o parâmetro `chart.x`; no eixo y, a medição `chart.y`; na escala de `chart.scale` (para `list-intersection`, logarítmica).
- **AC-008.5** Dada uma resposta `409`, quando a tela a recebe, então mostra a mensagem "Já existe uma execução em andamento. Aguarde e tente de novo." e reabilita o botão; dada uma resposta `400`, `413` ou `422`, então mostra o `detail` do erro junto do parâmetro citado.
- **AC-008.6** Dado um resultado com `traceId`, quando a tabela é exibida, então há um link "Ver o trace" para a tela Explore do Grafana (`VITE_GRAFANA_URL`), na fonte de dados de UID `tempo`, com esse `traceId`.
- **AC-008.7** Dada uma resposta `500` com `type` `/problems/run-failed`, quando a tela a recebe, então mostra "A execução falhou." com o `detail` e o `runId`, e reabilita o botão.
- **AC-008.8** Dado que o `POST` passou de 120 s [meta] sem resposta, quando o tempo estoura, então a tela mostra "A execução passou de 120 segundos e o resultado não chegou. Tente um tamanho menor." e reabilita o botão.
- **AC-008.9** Dado que a API ainda não está pronta (`GET /actuator/health/readiness` diferente de `UP`, por exemplo durante a carga de dados), quando a tela abre, então mostra "Carregando os dados de referência. Isso pode levar alguns minutos na primeira vez." e desabilita o botão "Executar" até a API ficar pronta.

### RF-009 · Telemetria de cada execução (F5)
- **AC-009.1** Dada uma execução concluída, quando a resposta chega, então `traceId` tem 32 caracteres hexadecimais e é o trace da requisição HTTP; nele há um span `lab.run`, filho do span HTTP, com um span filho `lab.variant` por versão × tamanho, com os atributos `lab.experiment.id`, `lab.variant` e `lab.size`.
- **AC-009.2** Dada uma execução concluída ou com falha, quando ela termina, então é escrita uma linha de log em JSON com `event` = `lab.run.completed` (ou `lab.run.failed`) e os campos `run_id`, `experiment_id`, `status`, `duration_ms` e `trace_id`, com o mesmo `trace_id` da resposta.
- **AC-009.3** Dada uma execução concluída do experimento `n-plus-one`, quando ela termina, então o contador Micrometer `lab.runs` com as tags `experiment=n-plus-one` e `status=COMPLETED` aumenta em 1, e o timer `lab.run.duration` com a tag `experiment=n-plus-one` registra uma observação (nomes exportados em `03 §8`).
- **AC-009.4** Dada a pilha de observabilidade local no ar, quando o `traceId` de uma execução de `n-plus-one` com `"pageSizes":[50]` é procurado no Grafana, então o trace aparece com os spans de AC-009.1, as consultas SQL aparecem como spans filhos de `lab.variant` (51 na versão `bad` e 1 na `good`), e as linhas de log da execução aparecem ligadas ao trace no Loki. **Prova por evidência** (captura e consulta registradas).

### RF-010 · Site de documentação e páginas de estudo (F6)
- **AC-010.1** Dado o site em `site/`, quando `npm run build` é executado, então termina com código 0; e um link interno quebrado, inserido de propósito, faz o build falhar.
- **AC-010.2** Dada cada página de estudo em `site/docs/experimentos/`, quando o verificador de documentação (`scripts/doc-lint.sh`) roda, então ele confirma estas seções, nesta ordem: "Testado com", "Termos usados nesta página", "O que é", "O que cada comando e configuração faz", "Por que importa", "Alternativas", "Por que escolhemos e por que testamos assim", "Como verificar", "Armadilhas", "Windows × Linux", "Depende da versão", "Resultados medidos", "Perguntas de verificação". Uma página sem uma delas faz o script sair com código 1, citando o arquivo e a seção que falta. As seções "Windows × Linux" e "Depende da versão" podem dizer "Não se aplica" e o porquê.
- **AC-010.3** Dado um bloco de código numa página de estudo, quando o verificador roda, então: um bloco nas linguagens de terminal (`bash`, `sh`, `shell`, `console`, `powershell`, `pwsh`, `ps1`, `cmd`) precisa estar num grupo de abas `groupId="shell"`, com uma aba `bash` e uma aba `powershell`; e um bloco sem linguagem declarada também reprova. Nos dois casos, o script sai com código 1, citando o arquivo e a linha.
- **AC-010.4** Dado cada descritor de experimento, quando o teste de consistência roda, então o caminho em `studyPage` existe em `site/docs/`, e as perguntas de verificação do descritor são as mesmas da seção "Perguntas de verificação" da página.
- **AC-010.5** Dado que a pessoa escolheu a aba PowerShell numa página, quando abre outra página, então a aba PowerShell já vem selecionada. **Prova por evidência** (recurso nativo do Docusaurus, demonstrado na evidência da tarefa).

## 2. Requisitos não funcionais
Cada RNF diz **o mecanismo** e **como se prova**.

### RNF-001 · Ambiente reproduzível em poucos comandos
Mecanismo: Docker Compose com perfil `mvp`, contendo PostgreSQL, a aplicação, o front-end e a pilha de observabilidade, todas com imagens de versão fixa (D-11).
- **AC-RNF-001.1** Dado um clone limpo e o Docker instalado, quando os comandos do README são executados, então o laboratório abre em `http://localhost:5173` com, no máximo, 3 comandos [meta], em bash e em PowerShell.
- **AC-RNF-001.2** Dado o `compose.yaml`, quando o verificador de compose (`scripts/compose-lint.sh`) roda, então nenhuma imagem usa a tag `latest` nem fica sem tag; uma imagem `postgres:latest` inserida de propósito faz o script sair com código 1.
- **Prova:** AC-RNF-001.1 por evidência (comandos executados nos dois shells numa stack isolada, com a saída registrada); AC-RNF-001.2 por teste de script.

### RNF-002 · Comparação justa entre versões
Mecanismo: cada experimento tem um teste de equivalência entre as versões e um `checksum` do resultado funcional (P3). Os descritores declaram `fairness`: `same-result` ou `demonstrates-error`.
- **AC-RNF-002.1** Dado um experimento `same-result`, quando suas versões rodam com as mesmas entradas, então o `checksum` é igual em todas (AC-005.1, AC-006.4).
- **AC-RNF-002.2** Dado o experimento `flash-sale-last-units` (`demonstrates-error`), quando o descritor é validado, então ele declara qual medição evidencia o erro (`oversold`); um descritor `demonstrates-error` sem `evidenceMeasurement` é recusado na inicialização.
- **Prova:** teste.

### RNF-003 · Medições isoladas (uma execução por vez)
Mecanismo: *advisory lock* do PostgreSQL adquirido antes de executar e liberado no fim, inclusive em falha (D-04).
- **AC-RNF-003.1** Igual a AC-003.8 e AC-003.9.
- **Prova:** teste com o experimento `test-blocking` e o `test-failing`.

### RNF-004 · Fronteiras de arquitetura verificadas
Mecanismo: teste de arquitetura (ArchUnit, Q-007), com as regras de `01 §3`.
- **AC-RNF-004.1** Dado o código dos pacotes `..inventory.domain..` e `..ordering.domain..`, quando o teste roda, então nenhuma classe depende de `org.springframework..`, `jakarta.persistence..` ou `org.hibernate..`.
- **AC-RNF-004.2** Dado qualquer pacote `..experiment..` (onde ficam as versões ruins), quando o teste roda, então só classes de `..lab..` e de outros pacotes `..experiment..` dependem dele.
- **AC-RNF-004.3** Dados dois módulos diferentes, quando o teste roda, então um só depende do pacote `api` do outro, **ou** do pacote `experiment` do outro quando quem depende está em `..lab..` ou em `..experiment..` (a exceção de AC-RNF-004.2).
- **AC-RNF-004.4** Dado qualquer pacote de produção, quando o teste roda, então nenhum campo cujo nome contenha `price`, `amount`, `total` ou `cost` (sem diferenciar maiúsculas) é do tipo `double`, `float`, `Double` ou `Float`.
- **Prova:** teste.

### RNF-005 · Determinismo dos testes
Mecanismo: `Clock`, gerador de IDs e `Stopwatch` injetados; sementes fixas; imagens pinadas; Testcontainers com PostgreSQL real (nunca banco em memória).
- **AC-RNF-005.1** Dado o gate completo, quando é executado 3 vezes seguidas no mesmo commit, então os resultados dos testes são idênticos.
- **Prova:** evidência (3 execuções registradas na T-002) + padrões proibidos do gate (`Thread.sleep` em teste).

### RNF-006 · Páginas de estudo no padrão de explicação
Mecanismo: verificador de documentação (`scripts/doc-lint.sh`) no gate + revisão humana do conteúdo (DP-05).
- **AC-RNF-006.1** Os casos de AC-010.2 e AC-010.3 têm testes de script, com uma página válida e uma página para cada tipo de falha, e rodam no CI.
- **AC-RNF-006.2** Dada uma afirmação de versão ("a partir da versão X…"), quando a página é revisada, então há um link para as notas de versão oficiais na seção "Depende da versão". **Prova por revisão** (R10).
- **Prova:** teste de script (AC-RNF-006.1) + revisão (AC-RNF-006.2).

### RNF-007 · Cabe na máquina de referência
Mecanismo: limites de memória por container no `compose.yaml`; perfil `mvp` sem os bancos das fatias futuras.
- **AC-RNF-007.1** Dado o perfil `mvp` com 1.000.000 de produtos carregados, quando uma execução de cada experimento termina, então a soma da memória dos containers (`docker stats`) fica em até 4 GB [meta], abaixo dos ~7,6 GB disponíveis (A2).
- **Prova:** evidência medida (data, ambiente A2, saída do `docker stats`).

### RNF-008 · Carga de 1 milhão de produtos
- **AC-RNF-008.1** Dado o ambiente A2 e `LAB_SEED_PRODUCTS=1000000`, quando a carga roda num banco vazio, então termina em até 10 minutos [meta], com as contagens equivalentes às de AC-001.1 (1.000.000 de produtos) e o tempo real registrado no log (`seed.completed` com `duration_ms`) e no gauge `lab_seed_duration_seconds`.
- **AC-RNF-008.2** Dado o mesmo ambiente, quando as versões `medium` e `good` de `list-intersection` rodam com tamanho 1.000.000, então a execução termina com `COMPLETED`.
- **Prova:** evidência medida (G4).

### RNF-009 · Segurança mínima de um laboratório local
Mecanismo: portas publicadas só em `127.0.0.1`; a API aceita apenas ids e parâmetros declarados nos descritores (nunca código, SQL ou caminho de arquivo); corpo limitado a 16 KB; CORS só para a origem da tela; erros sem stack trace; nenhum segredo versionado.
- **AC-RNF-009.1** Dado o `compose.yaml`, quando `scripts/compose-lint.sh` roda, então toda porta publicada está ligada a `127.0.0.1`; uma porta `"8080:8080"` inserida de propósito faz o script sair com código 1.
- **AC-RNF-009.2** Dado um erro `500`, quando a resposta é examinada, então o corpo não contém nome de classe Java nem stack trace.
- **AC-RNF-009.3** Dado o repositório, quando o detector de segredos roda no CI, então não há achados.
- **AC-RNF-009.4** Dada uma requisição à API com `Origin: http://localhost:5173`, quando a resposta chega, então traz `Access-Control-Allow-Origin: http://localhost:5173`; com `Origin: http://evil.example`, a resposta não traz esse header.
- **AC-RNF-009.5** Dado o servidor do front-end, quando `GET /` é feito, então a resposta traz `X-Content-Type-Options: nosniff`. **Prova por evidência** (comando `curl -I` registrado).
- **Prova:** teste (AC-RNF-009.1, 009.2, 009.4) + CI (AC-RNF-009.3) + evidência (AC-RNF-009.5).

### RNF-010 · Registro de horas para avaliar a H-01
- **AC-RNF-010.1** Dada cada tarefa concluída, quando o checklist de pronto é marcado, então o registro de horas dos bastidores (`.bastidores/registro-horas.md`, fora do repositório público, DP-22) tem as horas dessa tarefa por tipo de trabalho: especificação, planejamento, código, testes, revisão, documentação, post.
- **Prova:** evidência (revisão do arquivo a cada tarefa).

### RNF-011 · Publicação dos resultados (F7)
- **AC-RNF-011.1** Dado o MVP concluído, quando `docs/produto/publicacoes.md` é examinado, então lista 2 posts publicados (link e data), cada um citando uma medição registrada numa execução (`runId`, data, ambiente).
- **Prova:** evidência.

### RNF-012 · Gate automático em cada tarefa
Mecanismo: os scripts do harness versionados em `scripts/` e configurados por `scripts/sdd.conf` (`05 §4`); o CI roda os testes do harness e confere a matriz de rastreabilidade.
- **AC-RNF-012.1** Dado o repositório, quando `bash scripts/test/run-tests.sh` roda, localmente e no CI, então todos os cenários passam.
- **AC-RNF-012.2** Dada uma tarefa cujo diff tem um arquivo fora de `allowed_paths`, ou um teste novo sem a marcação do requisito coberto, quando `scripts/verify-task.sh` roda, então reprova no item 2 (escopo) ou no item 4 (rastreabilidade), citando o arquivo ou o requisito.
- **AC-RNF-012.3** Dada uma dependência, um plugin Maven ou uma GitHub Action nova que não está em `allowed_new_dependencies`, quando `scripts/verify-task.sh` roda, então reprova no item 3, citando a coordenada no formato de Q-016.
- **AC-RNF-012.4** Dada a matriz `docs/specs/traceability.md` diferente da gerada, quando o CI roda, então o job `harness` falha.
- **AC-RNF-012.5** Dado um PR de um branch `task/T-xxx`, quando o checklist de pronto da tarefa tem algum item sem marca ou o relatório `docs/specs/reviews/T-xxx.md` não existe ou não tem veredito PASS/PASS_WITH_NOTES, então o job `pronto` falha e a proteção da `main` impede o merge (Q-022); com tudo completo, o job passa.
- **Prova:** testes de script (`# covers: RNF-012`) + CI + evidência da violação proposital.

### RNF-013 · Repositório público protegido e apresentável
Mecanismo: configurações de segurança do GitHub (proteção da `main`, varredura de segredos com bloqueio no push, Dependabot, CodeQL, relato privado de vulnerabilidade, permissões do Actions), arquivos de comunidade e uma apresentação que explique o projeto a quem chega pelo link.
- **AC-RNF-013.1** Dada a `main`, quando alguém tenta enviar um commit direto, forçar um push ou apagar o branch, então o GitHub recusa; e um PR só pode ser mesclado com os jobs obrigatórios do CI verdes.
- **AC-RNF-013.2** Dado o repositório, quando um push contém um segredo reconhecido pelo GitHub, então o push é bloqueado; e as análises do CodeQL e os alertas do Dependabot estão ativos.
- **AC-RNF-013.3** Dado quem chega ao repositório, quando abre a página inicial, então encontra o que é o projeto, por que existe, como rodar, o roteiro, as licenças, como contribuir, como relatar uma vulnerabilidade e o código de conduta; e o link compartilhado mostra a imagem de prévia própria.
- **Prova:** evidência (configurações conferidas pela API pública do GitHub quando possível e, nas demais, pelo checklist executado pelo autor).

### RNF-014 · Decisões de arquitetura rastreáveis
Mecanismo: um ADR por decisão (`docs/architecture/decisions/`), um índice, os diagramas C4 de contexto e de containers e a tabela RNF → mecanismo; um verificador (`scripts/adr-lint.sh`) no CI confere a correspondência.
- **AC-RNF-014.1** Dada cada decisão `D-nn` da tabela de `03 §1`, quando `scripts/adr-lint.sh` roda, então existe exatamente um ADR cuja linha "Origem" cita aquela decisão; uma decisão sem ADR, ou com dois, faz o script sair com código 1, citando a decisão.
- **AC-RNF-014.2** Dado cada ADR, quando o verificador roda, então ele tem "Status" (`proposto`, `aceito` ou `substituído por ADR-nnn`), "Data", "Origem" e as seções "Contexto", "Decisão", "Alternativas rejeitadas" (com pelo menos uma linha na tabela) e "Consequências"; um ADR sem alguma delas reprova, citando o arquivo e o que falta.
- **AC-RNF-014.3** Dado o índice `docs/architecture/decisions/README.md`, quando o verificador roda, então ele lista todos os ADRs e só eles, cada um com o título; e a tabela `docs/architecture/rnf-mecanismos.md` tem uma linha para cada RNF de `02`.
- **Prova:** testes de script (`# covers: RNF-014`) + CI.

## 3. Rastreabilidade
- **Obrigatórios com teste rastreado:** RF-001 a RF-010 (exceto AC-009.4 e AC-010.5, provados por evidência) e RNF-001 (só AC-RNF-001.2), RNF-002, RNF-003, RNF-004, RNF-006 (só AC-RNF-006.1), RNF-009 (AC-RNF-009.1, 009.2 e 009.4), RNF-012 (AC-RNF-012.1 a 012.3 e 012.5), RNF-014.
- **Provados por evidência ou revisão (sem tag):** AC-009.4, AC-010.5, AC-RNF-001.1, RNF-005, AC-RNF-006.2, RNF-007, RNF-008, AC-RNF-009.3 (CI), AC-RNF-009.5, RNF-010, RNF-011, RNF-013.
- A matriz `docs/specs/traceability.md` é gerada pelo script e versionada.
