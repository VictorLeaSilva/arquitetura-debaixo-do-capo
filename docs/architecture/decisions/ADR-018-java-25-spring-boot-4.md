# ADR-018 · Java 25 e Spring Boot 4.1

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** 00 §3 (Linguagem e Framework)

## Contexto
A linguagem e o framework definem o que o laboratório consegue mostrar e por quanto tempo as escolhas continuam atuais. **LTS** (*Long-Term Support*) é a versão do Java com suporte estendido; a mais recente é a 25. O Spring Boot 4.0 foi lançado em novembro de 2025 com suporte de primeira classe ao Java 25, e a 4.1.2 é a GA mais recente em 2026-11-07 (A3).

A spec registra a escolha (`00 §3`, premissa A3), mas não as alternativas consideradas para esta linha da stack. As da tabela abaixo foram escritas a partir das fontes conferidas na T-001 e não acrescentam nenhuma decisão.

## Decisão
Java 25 (LTS) e Spring Boot 4.1.2, fixados na T-001. O enforcer do Maven barra qualquer JDK abaixo do 25.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Java 21 (a LTS anterior) | Ainda é muito usada, mas o projeto quer mostrar a versão atual e as particularidades dela (DP-05) |
| Spring Boot 3.x | A linha 4 é a que traz o suporte de primeira classe ao Java 25 e a reorganização dos starters que o projeto documenta |

## Consequências
- Positivas: o projeto envelhece mais devagar; as diferenças entre versões viram conteúdo ("Depende da versão").
- Negativas e mitigação: ferramentas mais novas às vezes ainda não declaram suporte ao Java 25; cada uma é conferida nas fontes oficiais antes de entrar (T-002).
- Como se prova: AC-T001.4 (o enforcer reprova outro JDK) e `docs/versoes.md`.
