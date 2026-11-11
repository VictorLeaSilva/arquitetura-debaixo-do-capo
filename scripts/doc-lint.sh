#!/usr/bin/env bash
# Verificador das páginas de estudo (RF-010: AC-010.2 e AC-010.3; RNF-006).
#   bash scripts/doc-lint.sh [arquivo ...]
# Sem argumentos, confere a página-modelo e todo .md/.mdx em site/docs/experimentos/ (a pasta pode não existir).
# Confere:
#   - as 13 seções "## <nome>" do padrão DP-05, uma vez cada e nesta ordem (outros títulos podem aparecer entre elas);
#   - todo bloco de código tem linguagem;
#   - bloco de terminal (bash, sh, shell, console, powershell, pwsh, ps1, cmd) fica num <Tabs groupId="shell">,
#     e esse grupo tem as abas value="bash" e value="powershell".
# Saída: as linhas de ERRO e exit 1; "doc-lint: ok (N páginas)" e exit 0; arquivo inexistente, exit 2.
# Só usa bash, awk, find e sort (POSIX; roda no Git Bash e com o mawk do Ubuntu). Aceita CRLF.
set -u
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [ $# -gt 0 ]; then
  ARQUIVOS=("$@")
else
  cd "$RAIZ" || exit 2
  ARQUIVOS=("site/docs/como-estudar/modelo-de-pagina.mdx")
  if [ -d site/docs/experimentos ]; then
    while IFS= read -r f; do ARQUIVOS+=("$f"); done < <(find site/docs/experimentos -type f \( -name '*.md' -o -name '*.mdx' \) | LC_ALL=C sort)
  fi
fi

for f in "${ARQUIVOS[@]}"; do
  [ -f "$f" ] || { echo "ERRO arquivo não encontrado: $f"; exit 2; }
done

# shellcheck disable=SC2016
PROGRAMA='
function erro(msg) { print "ERRO " ARQ ":" NR ": " msg; erros++ }
BEGIN {
  ARQ = ENVIRON["DOC_LINT_ARQ"]
  n = split("Testado com|Termos usados nesta página|O que é|O que cada comando e configuração faz|Por que importa|Alternativas|Por que escolhemos e por que testamos assim|Como verificar|Armadilhas|Windows × Linux|Depende da versão|Resultados medidos|Perguntas de verificação", S, "|")
  for (i = 1; i <= n; i++) idx[S[i]] = i
  split("bash sh shell console powershell pwsh ps1 cmd", T, " ")
  for (i in T) terminal[T[i]] = 1
  ultima = 0; erros = 0; emcodigo = 0; emabas = 0
}
{ sub(/\r$/, "") }
emcodigo {
  if ($0 ~ /^[ \t]*```+[ \t]*$/) emcodigo = 0
  next
}
/^[ \t]*```/ {
  ling = $0
  sub(/^[ \t]*`+[ \t]*/, "", ling)
  sub(/[ \t{].*$/, "", ling)
  if (ling == "") erro("bloco de código sem linguagem")
  else if ((ling in terminal) && !(emabas && abasshell)) erro("bloco " ling " fora de um grupo de abas groupId=\"shell\"")
  emcodigo = 1
  next
}
/<Tabs([ \t>]|$)/ {
  emabas = 1; abasshell = ($0 ~ /groupId="shell"/); linhaabas = NR; abash = 0; apowershell = 0
}
emabas && /<TabItem/ {
  if ($0 ~ /value="bash"/) abash = 1
  if ($0 ~ /value="powershell"/) apowershell = 1
}
/<\/Tabs>/ {
  if (emabas && abasshell) {
    if (!abash) { print "ERRO " ARQ ":" linhaabas ": grupo de abas shell sem a aba bash"; erros++ }
    if (!apowershell) { print "ERRO " ARQ ":" linhaabas ": grupo de abas shell sem a aba powershell"; erros++ }
  }
  emabas = 0; abasshell = 0
}
/^## / {
  nome = $0
  sub(/^## /, "", nome); sub(/[ \t]+$/, "", nome)
  if (nome in idx) {
    if (nome in visto) erro("seção repetida \"" nome "\"")
    else {
      visto[nome] = 1
      if (idx[nome] < ultima) erro("seção fora de ordem \"" nome "\"")
      else ultima = idx[nome]
    }
  }
}
END {
  for (i = 1; i <= n; i++)
    if (!(S[i] in visto)) { print "ERRO " ARQ ": falta a seção \"" S[i] "\""; erros++ }
  exit (erros > 0)
}'

FALHOU=0
for f in "${ARQUIVOS[@]}"; do
  DOC_LINT_ARQ="$f" awk "$PROGRAMA" "$f" || FALHOU=1
done

if [ "$FALHOU" -ne 0 ]; then exit 1; fi
echo "doc-lint: ok (${#ARQUIVOS[@]} páginas)"
