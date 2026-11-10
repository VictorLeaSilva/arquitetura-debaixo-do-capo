# Spike H-05: os bancos distribuídos cabem na máquina?

Um *spike* é um experimento curto feito para responder a uma pergunta antes de construir algo maior. Este responde à hipótese **H-05** (`docs/produto/hipoteses.md`): os clusters de 3 nós da fatia 6 (dados distribuídos) cabem na memória que o Docker tem numa máquina comum, com a base do MVP no ar e ainda sobrando espaço para a aplicação?

## O que é medido

| Cenário | O que sobe | Por que é o mais pesado |
|---|---|---|
| `cassandra` | 3 nós de Apache Cassandra 5.0.9, um de cada vez, + a base do MVP | Cada nó é uma JVM com o próprio *heap* (memória do Java) e memória fora dele |
| `mongo` | um *replica set* de 3 nós de MongoDB 9.0.4 + a base do MVP | São 3 processos `mongod`, cada um com o próprio cache |
| `simultaneo` | os dois clusters juntos + a base do MVP | Diz se um experimento pode usar os dois bancos ao mesmo tempo |

Em cada cenário, o roteiro sobe tudo, espera ficar pronto, faz uma carga de 10.000 inserções (com `QUORUM` no Cassandra e `writeConcern: majority` no MongoDB, que só confirmam depois de a maioria dos nós gravar), mede a memória de todos os containers a cada 5 segundos durante a carga e por mais 60 segundos, confere que os dados estão lá e derruba só o que ele próprio subiu.

O critério: um cenário **cabe** quando o **pico** da soma da memória fica em até **6,6 GiB**, ou seja, os ~7,6 GiB do Docker menos 1 GiB reservado para a aplicação, que ainda não roda em container.

## Como rodar

Pré-requisitos: Docker com o Compose v2 e o `.env` da raiz (se ele não existir, o roteiro cria um temporário a partir do `.env.example` e o apaga no fim). Na primeira vez, o Docker baixa cerca de 1 GB de imagens.

```bash
bash infra/spike-h05/medir.sh cassandra 512M
bash infra/spike-h05/medir.sh cassandra 1G
bash infra/spike-h05/medir.sh mongo
bash infra/spike-h05/medir.sh simultaneo 512M
```

```powershell
bash infra/spike-h05/medir.sh cassandra 512M
bash infra/spike-h05/medir.sh cassandra 1G
bash infra/spike-h05/medir.sh mongo
bash infra/spike-h05/medir.sh simultaneo 512M
```

No Windows, o comando é o mesmo no PowerShell: ele chama o `bash` do Git Bash, que precisa estar instalado.

O segundo argumento é o *heap* de cada nó do Cassandra. Com o coletor G1, que o Cassandra 5 usa por padrão, só o `MAX_HEAP_SIZE` vale: o `HEAP_NEWSIZE` que aparece em tutoriais antigos era do coletor CMS e agora é ignorado.

Cada rodada leva alguns minutos e deixa a máquina mais lenta enquanto roda.

## Onde ficam os resultados

Em `resultados/<cenario>-<heap>-<data>.md`: data, ambiente, parâmetros, a confirmação de que o cluster ficou pronto e os dados foram gravados, o pico e a média ociosa no total e o pico por container. A decisão da H-05, com os números das rodadas, fica em `docs/produto/hipoteses.md`.
