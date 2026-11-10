#!/usr/bin/env bash
# Spike H-05 (T-007): mede a memória de um cenário de dados distribuídos com a base do MVP no ar.
#   bash infra/spike-h05/medir.sh cassandra 512M     (cluster de 3 nós de Cassandra, heap de 512 MB por nó)
#   bash infra/spike-h05/medir.sh cassandra 1G
#   bash infra/spike-h05/medir.sh mongo              (replica set de 3 nós de MongoDB)
#   bash infra/spike-h05/medir.sh simultaneo 512M    (Cassandra e MongoDB juntos)
# Grava o resultado em infra/spike-h05/resultados/<cenario>-<heap>-<data>.md e derruba só o que subiu.
set -u
# No Git Bash (Windows), o MSYS traduz argumentos que começam com "/" para caminhos do Windows
# (/opt/cassandra/bin/cqlsh viraria C:/Program Files/Git/opt/...) antes de chegar ao docker. Desliga isso; no Linux não faz nada.
export MSYS_NO_PATHCONV=1

CENARIO="${1:-}"; HEAP="${2:-512M}"
case "$CENARIO" in cassandra|mongo|simultaneo) ;; *) echo "uso: medir.sh <cassandra|mongo|simultaneo> [heap]" >&2; exit 2 ;; esac
case "$HEAP" in *[0-9]M|*[0-9]G) ;; *) echo "heap inválido: $HEAP (ex.: 512M, 1G)" >&2; exit 2 ;; esac

# Caminhos no formato do Windows (C:/dev/...) no Git Bash (pwd -W), porque a tradução automática está desligada acima.
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && { pwd -W 2> /dev/null || pwd; })"
RAIZ="$(cd "$AQUI/../.." && { pwd -W 2> /dev/null || pwd; })"
LINHAS=10000                 # [meta] volume da carga, igual para os dois bancos
AMOSTRA=5                    # segundos entre duas amostras de memória
OCIOSO=60                    # segundos de amostragem depois da carga
DATA="$(date -u +%Y-%m-%dT%H%M%SZ)"
SAIDA="$AQUI/resultados/${CENARIO}-${HEAP}-${DATA}.md"
BRUTO="$AQUI/resultados/${CENARIO}-${HEAP}-${DATA}.amostras.tsv"
AMOSTRAS="$(mktemp)"
CQ="docker compose -f $AQUI/compose.cassandra.yaml"
MQ="docker compose -f $AQUI/compose.mongo.yaml"

log() { printf '[%s] %s\n' "$(date -u +%H:%M:%S)" "$*" >&2; }
falha() { log "FALHA: $*"; [ -s "$AMOSTRAS.err" ] && { log "erros da carga:"; tail -5 "$AMOSTRAS.err" >&2; }; derrubar; exit 1; }

# Converte "123.4MiB" / "1.2GiB" / "800kB" do docker stats em MiB.
para_mib() { awk '{ v = $1 + 0; if ($1 ~ /GiB/) v *= 1024; else if ($1 ~ /KiB|kB/) v /= 1024; printf "%.1f", v }'; }

# Uma amostra: "segundos<TAB>container<TAB>MiB" de todos os containers da base e do cenário.
amostrar() {
  local t="$1"
  docker stats --no-stream --format '{{.Name}}\t{{.MemUsage}}' \
    | grep -E 'arquitetura-debaixo-do-capo-|spike-h05-' | while IFS=$'\t' read -r nome uso; do
        printf '%s\t%s\t%s\n' "$t" "$nome" "$(printf '%s' "${uso%% /*}" | para_mib)"
      done >> "$AMOSTRAS"
}

# Amostra a cada $AMOSTRA segundos enquanto o processo $1 estiver vivo; devolve o próximo instante.
amostrar_durante() {
  local pid="$1" t="$2"
  while kill -0 "$pid" 2> /dev/null; do amostrar "$t"; sleep "$AMOSTRA"; t=$((t + AMOSTRA)); done
  echo "$t"
}

BASE_SUBIU=0
subir_base() {
  if docker compose -f "$RAIZ/compose.yaml" --profile mvp ps --status running -q 2> /dev/null | grep -q .; then
    log "base do MVP já estava no ar: será mantida"
  else
    [ -f "$RAIZ/.env" ] || { cp "$RAIZ/.env.example" "$RAIZ/.env"; ENV_CRIADO=1; }
    log "subindo a base do MVP"
    docker compose -f "$RAIZ/compose.yaml" --profile mvp up -d --wait > /dev/null 2>&1 || falha "a base do MVP não subiu"
    BASE_SUBIU=1
  fi
}

derrubar() {
  case "$CENARIO" in cassandra|simultaneo) $CQ down -v > /dev/null 2>&1 ;; esac
  case "$CENARIO" in mongo|simultaneo) $MQ down -v > /dev/null 2>&1 ;; esac
  if [ "$BASE_SUBIU" -eq 1 ]; then docker compose -f "$RAIZ/compose.yaml" --profile mvp down > /dev/null 2>&1; fi
  if [ "${ENV_CRIADO:-0}" -eq 1 ]; then rm -f "$RAIZ/.env"; fi
  rm -f "$AMOSTRAS.cql" "$AMOSTRAS.js" "$AMOSTRAS.err"
}

subir_cassandra() {
  log "subindo o Cassandra (3 nós, heap $HEAP), um nó de cada vez"
  CASSANDRA_HEAP="$HEAP" $CQ up -d --wait > /dev/null 2>&1 || falha "o Cassandra não subiu"
  local un; un="$($CQ exec -T cassandra-1 nodetool status | grep -c '^UN')"
  [ "$un" -eq 3 ] || falha "nodetool status mostra $un nó(s) UN, esperado 3"
  NODETOOL="$($CQ exec -T cassandra-1 nodetool status | grep -E '^(UN|DN|UJ|UL)')"
  XMX="$(for n in 1 2 3; do $CQ exec -T "cassandra-$n" sh -c "ps -o args= -C java | grep -o -- '-Xmx[0-9]*[MmGg]' | head -1"; done | tr '\n' ' ')"
}

carga_cassandra() {
  log "carga no Cassandra: $LINHAS linhas com QUORUM"
  {
    echo "CONSISTENCY QUORUM;"
    echo "CREATE KEYSPACE IF NOT EXISTS spike WITH replication = {'class': 'NetworkTopologyStrategy', 'dc1': 3};"
    echo "CREATE TABLE IF NOT EXISTS spike.item (id int PRIMARY KEY, nome text);"
    awk -v n="$LINHAS" 'BEGIN { for (i = 1; i <= n; i++) printf "INSERT INTO spike.item (id, nome) VALUES (%d, '\''item-%d'\'');\n", i, i }'
  } > "$AMOSTRAS.cql"
  $CQ exec -T cassandra-1 /opt/cassandra/bin/cqlsh < "$AMOSTRAS.cql" > /dev/null 2>> "$AMOSTRAS.err"
}

conferir_cassandra() {
  CONTAGEM_C="$(printf 'CONSISTENCY QUORUM;\nSELECT count(*) FROM spike.item;\n' | $CQ exec -T cassandra-1 /opt/cassandra/bin/cqlsh | grep -E '^ *[0-9]+ *$' | tr -d ' ')"
  [ "$CONTAGEM_C" = "$LINHAS" ] || falha "Cassandra contou '$CONTAGEM_C', esperado $LINHAS"
}

subir_mongo() {
  log "subindo o MongoDB (replica set de 3 nós)"
  $MQ up -d --wait > /dev/null 2>&1 || falha "o MongoDB não subiu"
  $MQ exec -T mongo-1 mongosh --quiet --eval "rs.initiate({_id: 'rs0', members: [{_id: 0, host: 'mongo-1:27017'}, {_id: 1, host: 'mongo-2:27017'}, {_id: 2, host: 'mongo-3:27017'}]})" > /dev/null \
    || falha "rs.initiate falhou"
  local i; for i in $(seq 1 60); do
    ESTADOS="$($MQ exec -T mongo-1 mongosh --quiet --eval "rs.status().members.map(m => m.stateStr).sort().join(',')" 2> /dev/null)"
    [ "$ESTADOS" = "PRIMARY,SECONDARY,SECONDARY" ] && break
    sleep 2
  done
  [ "$ESTADOS" = "PRIMARY,SECONDARY,SECONDARY" ] || falha "replica set em '$ESTADOS'"
  CACHE="$(for n in 1 2 3; do $MQ exec -T "mongo-$n" mongosh --quiet --eval "db.serverStatus().wiredTiger.cache['maximum bytes configured']"; done | tr '\n' ' ')"
}

carga_mongo() {
  log "carga no MongoDB: $LINHAS documentos com writeConcern majority"
  printf 'const docs = []; for (let i = 1; i <= %d; i++) docs.push({_id: i, nome: "item-" + i});\ndb.getSiblingDB("spike").item.insertMany(docs, {writeConcern: {w: "majority"}});\n' "$LINHAS" > "$AMOSTRAS.js"
  local primario; primario="$($MQ exec -T mongo-1 mongosh --quiet --eval "rs.status().members.find(m => m.stateStr === 'PRIMARY').name.split(':')[0]")"
  $MQ exec -T "$primario" mongosh --quiet < "$AMOSTRAS.js" > /dev/null 2>> "$AMOSTRAS.err"
}

conferir_mongo() {
  local secundario; secundario="$($MQ exec -T mongo-1 mongosh --quiet --eval "rs.status().members.find(m => m.stateStr === 'SECONDARY').name.split(':')[0]")"
  CONTAGEM_M="$($MQ exec -T "$secundario" mongosh --quiet --eval "db.getMongo().setReadPref('secondary'); db.getSiblingDB('spike').item.countDocuments({})")"
  [ "$CONTAGEM_M" = "$LINHAS" ] || falha "MongoDB contou '$CONTAGEM_M' no secundário, esperado $LINHAS"
}

# --- roteiro ---
subir_base
case "$CENARIO" in cassandra|simultaneo) subir_cassandra ;; esac
case "$CENARIO" in mongo|simultaneo) subir_mongo ;; esac

T=0
case "$CENARIO" in cassandra|simultaneo) carga_cassandra & T="$(amostrar_durante $! "$T")"; wait $! 2> /dev/null ;; esac
case "$CENARIO" in mongo|simultaneo) carga_mongo & T="$(amostrar_durante $! "$T")"; wait $! 2> /dev/null ;; esac
FIM_CARGA="$T"
log "amostrando por mais ${OCIOSO}s depois da carga"
while [ "$T" -lt $((FIM_CARGA + OCIOSO)) ]; do amostrar "$T"; sleep "$AMOSTRA"; T=$((T + AMOSTRA)); done

case "$CENARIO" in cassandra|simultaneo) conferir_cassandra ;; esac
case "$CENARIO" in mongo|simultaneo) conferir_mongo ;; esac

# --- resultado ---
TOTAIS="$(awk -F'\t' '{ s[$1] += $3 } END { for (t in s) printf "%d\t%.1f\n", t, s[t] }' "$AMOSTRAS" | sort -n)"
PICO="$(printf '%s\n' "$TOTAIS" | awk -F'\t' 'BEGIN { m = 0 } $2 > m { m = $2 } END { printf "%.1f", m }')"
OCIOSO_MEDIA="$(printf '%s\n' "$TOTAIS" | awk -F'\t' -v f="$FIM_CARGA" '$1 >= f { s += $2; n++ } END { if (n) printf "%.1f", s / n; else print "n/a" }')"
CABE="$(awk -v p="$PICO" 'BEGIN { print (p <= 6.6 * 1024) ? "cabe" : "não cabe" }')"
mkdir -p "$AQUI/resultados"
{
  echo "# Resultado do spike H-05: $CENARIO, heap $HEAP"
  echo
  echo "- Data (UTC): $DATA"
  echo "- Ambiente: $(uname -s) / Docker $(docker version --format '{{.Server.Version}}'), memória do Docker $(docker info --format '{{.MemTotal}}' | awk '{ printf "%.2f GiB", $1 / 1073741824 }'), $(docker info --format '{{.NCPU}}') CPUs"
  echo "- Carga: $LINHAS linhas/documentos; amostras de memória a cada ${AMOSTRA}s durante a carga e por ${OCIOSO}s depois"
  case "$CENARIO" in cassandra|simultaneo) echo "- Cassandra: -Xmx por nó: $XMX; contagem com QUORUM: $CONTAGEM_C"; echo '```'; echo "$NODETOOL"; echo '```' ;; esac
  case "$CENARIO" in mongo|simultaneo) echo "- MongoDB: estados $ESTADOS; cache configurado (bytes) por nó: $CACHE; contagem no secundário: $CONTAGEM_M" ;; esac
  echo
  echo "| Medida | MiB |"
  echo "|---|---|"
  echo "| Pico da soma (base + cenário) | $PICO |"
  echo "| Média ociosa depois da carga | $OCIOSO_MEDIA |"
  echo
  echo "**Critério da H-05 (pico até 6,6 GiB = 6758,4 MiB): $CABE.**"
  echo
  echo "## Por container (MiB)"
  echo
  echo "| Container | Pico | Média ociosa depois da carga |"
  echo "|---|---|---|"
  awk -F'\t' -v f="$FIM_CARGA" '
    $3 > m[$2] { m[$2] = $3 }
    $1 >= f { s[$2] += $3; n[$2]++ }
    END { for (c in m) printf "| %s | %.1f | %s |\n", c, m[c], (n[c] ? sprintf("%.1f", s[c] / n[c]) : "n/a") }' "$AMOSTRAS" | sort
  echo
  echo "## Soma da memória por amostra (MiB)"
  echo
  echo "Segundos contados a partir do início da carga; a carga termina em ${FIM_CARGA}s. As amostras brutas, por container, estão em \`$(basename "$BRUTO")\`."
  echo
  echo "| Segundo | Fase | Soma |"
  echo "|---|---|---|"
  printf '%s\n' "$TOTAIS" | awk -F'\t' -v f="$FIM_CARGA" '{ printf "| %d | %s | %.1f |\n", $1, ($1 < f ? "carga" : "ociosa"), $2 }'
} > "$SAIDA"
# As amostras brutas (segundo, container, MiB) ficam versionadas ao lado do resultado: os números podem ser recalculados.
{ printf 'segundo\tcontainer\tmib\n'; sort -n -k1,1 -k2,2 "$AMOSTRAS"; } > "$BRUTO"
rm -f "$AMOSTRAS"
derrubar
log "resultado: $SAIDA (pico $PICO MiB: $CABE)"
