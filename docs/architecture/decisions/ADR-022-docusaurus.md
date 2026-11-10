# ADR-022 · Docusaurus no site de documentação

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** Q-003

## Contexto
As páginas de estudo seguem um padrão de explicação (DP-05) que inclui comandos em bash e em PowerShell, em abas, com botão de copiar. O Markdown puro do GitHub não tem abas.

## Decisão
O site de documentação é feito com Docusaurus já no MVP (T-006), com as abas bash/PowerShell sincronizadas entre as páginas.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Markdown no GitHub primeiro, site depois | As páginas teriam de ser reescritas quando o site chegasse |

## Consequências
- Positivas: abas sincronizadas e botão de copiar nativos; o site pode ser publicado no GitHub Pages.
- Negativas e mitigação: mais uma pilha (Node) no projeto; ela fica isolada em `site/`, com o próprio build no CI.
- Como se prova: AC-010.1 a AC-010.5 (T-006 e as páginas de cada experimento).
