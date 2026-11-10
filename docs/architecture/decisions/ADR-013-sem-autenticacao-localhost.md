# ADR-013 · Sem autenticação; portas só em 127.0.0.1

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-13 em 03

## Contexto
O laboratório roda na máquina de quem estuda, sem multiusuário (premissa A5). Autenticação e autorização entram como tema próprio na fatia 8. `127.0.0.1` é o endereço que só a própria máquina alcança; uma porta publicada nele não fica acessível pela rede.

## Decisão
Sem login no MVP. Todas as portas publicadas pelo Docker Compose ficam presas a `127.0.0.1`.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Login já no MVP | Sem requisito que o justifique (DP-21); a fatia 8 trata o tema com profundidade |

## Consequências
- Positivas: nada a configurar para usar; nada exposto fora da máquina.
- Negativas e mitigação: o laboratório não pode ser publicado como está; a fatia 8 cobre isso.
- Como se prova: AC-RNF-009.1, pelo `scripts/compose-lint.sh` no CI (T-004).
