#!/usr/bin/env bash
# Testes do verificador das páginas de estudo (scripts/doc-lint.sh), uma página temporária por cenário.
#   bash scripts/test/test-doc-lint.sh
set -u
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LINT="$(cd "$AQUI/.." && pwd)/doc-lint.sh"
PASSOU=0; FALHOU=0

# Uma página de estudo válida: as 13 seções na ordem e os comandos em abas shell.
pagina() {
  cat > "$1" <<'EOF'
---
title: Página de teste
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

# Página de teste

## Testado com
Node 26.

## Termos usados nesta página
- **Termo:** definição.

## O que é
Texto.

## O que cada comando e configuração faz
Texto.

## Por que importa
Texto.

## Alternativas
Texto.

## Por que escolhemos e por que testamos assim
Texto.

## Como verificar
<Tabs groupId="shell">
<TabItem value="bash" label="bash">

```bash
npm run build
```

</TabItem>
<TabItem value="powershell" label="PowerShell">

```powershell
npm run build
```

</TabItem>
</Tabs>

## Armadilhas
Texto.

## Windows × Linux
Texto.

## Depende da versão
Texto.

## Resultados medidos
Texto.

## Perguntas de verificação
1. Pergunta?
EOF
}

# caso <nome> <exit esperado> <texto esperado> <preparo (roda no diretório, com a página em p.mdx)>
caso() {
  local nome="$1" esperado="$2" texto="$3" preparo="$4" d rc saida
  d="$(mktemp -d)"
  pagina "$d/p.mdx"
  (cd "$d" && eval "$preparo") > /dev/null 2>&1
  saida="$(cd "$d" && bash "$LINT" p.mdx 2>&1)"; rc=$?
  if [ "$rc" -eq "$esperado" ] && printf '%s' "$saida" | grep -qF -- "$texto"; then
    PASSOU=$((PASSOU + 1)); echo "ok   $nome"
  else
    FALHOU=$((FALHOU + 1)); echo "FAIL $nome (exit $rc, esperado $esperado; texto: $texto)"; printf '%s\n' "$saida" | sed 's/^/     /'
  fi
  rm -rf "$d"
}

# Linha que um bloco acrescentado no fim da página válida ocupa (a página tem N linhas; o bloco começa em N+1).
N="$(d="$(mktemp -d)"; pagina "$d/p.mdx"; wc -l < "$d/p.mdx" | tr -d ' '; rm -rf "$d")"
L=$((N + 1))

# covers: RF-010 RNF-006
caso "1. página válida" 0 "doc-lint: ok (1 páginas)" ":"
# covers: RF-010 RNF-006
caso "2. sem a seção Armadilhas" 1 'ERRO p.mdx: falta a seção "Armadilhas"' "sed -i '/^## Armadilhas/d' p.mdx"
# covers: RF-010 RNF-006
caso "3. Alternativas antes de Por que importa" 1 "seção fora de ordem" \
  "sed -i 's/^## Por que importa\$/## TROCA/; s/^## Alternativas\$/## Por que importa/; s/^## TROCA\$/## Alternativas/' p.mdx"
# covers: RF-010 RNF-006
caso "4. seção repetida" 1 "ERRO p.mdx:$L: seção repetida \"Armadilhas\"" "printf '## Armadilhas\n' >> p.mdx"
# covers: RF-010 RNF-006
caso "5. Windows × Linux e Depende da versão com Não se aplica" 0 "doc-lint: ok (1 páginas)" \
  "sed -i '/^## Windows × Linux/{n;s/.*/Não se aplica: o comando é o mesmo nos dois sistemas./}; /^## Depende da versão/{n;s/.*/Não se aplica: nada muda entre as versões citadas./}' p.mdx"
# covers: RF-010 RNF-006
caso "6. bloco bash fora das abas" 1 "ERRO p.mdx:$L: bloco bash fora de um grupo de abas groupId=\"shell\"" \
  "printf '\`\`\`bash\nls\n\`\`\`\n' >> p.mdx"
# covers: RF-010 RNF-006
caso "7. bloco powershell em abas sem groupId shell" 1 "bloco powershell fora de um grupo de abas groupId=\"shell\"" \
  "printf '<Tabs>\n<TabItem value=\"powershell\">\n\n\`\`\`powershell\nls\n\`\`\`\n\n</TabItem>\n</Tabs>\n' >> p.mdx"
# covers: RF-010 RNF-006
caso "8. grupo shell sem a aba powershell" 1 "ERRO p.mdx:$L: grupo de abas shell sem a aba powershell" \
  "printf '<Tabs groupId=\"shell\">\n<TabItem value=\"bash\">\n\n\`\`\`bash\nls\n\`\`\`\n\n</TabItem>\n</Tabs>\n' >> p.mdx"
# covers: RF-010 RNF-006
caso "8b. grupo shell sem a aba bash" 1 "ERRO p.mdx:$L: grupo de abas shell sem a aba bash" \
  "printf '<Tabs groupId=\"shell\">\n<TabItem value=\"powershell\">\n\n\`\`\`powershell\nls\n\`\`\`\n\n</TabItem>\n</Tabs>\n' >> p.mdx"
# covers: RF-010 RNF-006
caso "6b. bloco sh fora das abas" 1 "ERRO p.mdx:$L: bloco sh fora de um grupo de abas groupId=\"shell\"" \
  "printf '\`\`\`sh\nls\n\`\`\`\n' >> p.mdx"
# covers: RF-010 RNF-006
caso "6c. bloco cmd fora das abas" 1 "ERRO p.mdx:$L: bloco cmd fora de um grupo de abas groupId=\"shell\"" \
  "printf '\`\`\`cmd\ndir\n\`\`\`\n' >> p.mdx"
# covers: RF-010 RNF-006
caso "9. bloco sem linguagem" 1 "ERRO p.mdx:$L: bloco de código sem linguagem" "printf '\`\`\`\nx\n\`\`\`\n' >> p.mdx"
# covers: RF-010 RNF-006
caso "10. bloco java fora das abas" 0 "doc-lint: ok (1 páginas)" "printf '\`\`\`java\nclass A {}\n\`\`\`\n' >> p.mdx"
# covers: RF-010 RNF-006
caso "11. ## Armadilhas só dentro de um bloco de código" 1 'falta a seção "Armadilhas"' \
  "sed -i '/^## Armadilhas/d' p.mdx; printf '\`\`\`text\n## Armadilhas\n\`\`\`\n' >> p.mdx"
# covers: RF-010 RNF-006
caso "12. página válida com CRLF" 0 "doc-lint: ok (1 páginas)" "sed -i 's/\$/\r/' p.mdx"
# covers: RF-010 RNF-006
inexistente() {
  local saida rc
  saida="$(bash "$LINT" nao-existe.mdx 2>&1)"; rc=$?
  if [ "$rc" -eq 2 ] && printf '%s' "$saida" | grep -qF "ERRO arquivo não encontrado: nao-existe.mdx"; then
    PASSOU=$((PASSOU + 1)); echo "ok   14. arquivo inexistente"
  else FALHOU=$((FALHOU + 1)); echo "FAIL 14. arquivo inexistente (exit $rc)"; printf '%s\n' "$saida" | sed 's/^/     /'; fi
}

# covers: RF-010 RNF-006
reais() {
  local saida rc
  saida="$(bash "$LINT" 2>&1)"; rc=$?
  if [ "$rc" -eq 0 ]; then PASSOU=$((PASSOU + 1)); echo "ok   13. os arquivos reais do projeto passam ($saida)"
  else FALHOU=$((FALHOU + 1)); echo "FAIL 13. os arquivos reais do projeto passam (exit $rc)"; printf '%s\n' "$saida" | sed 's/^/     /'; fi
}
inexistente
reais

echo "---"; echo "$PASSOU passaram, $FALHOU falharam"
[ "$FALHOU" -eq 0 ]
