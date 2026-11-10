# ADR-018 · Java 25 e Spring Boot 4.1

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** 00 §3 (Linguagem e Framework), com as alternativas da Q-028

## Contexto
A linguagem e o framework definem o que o laboratório consegue mostrar e por quanto tempo as escolhas continuam atuais. **LTS** (*Long-Term Support*) é uma versão do Java com suporte estendido; a 25 é a mais recente. O Spring Boot 4.1.2 era a versão GA (estável) mais recente em 2026-11-07, com suporte ao Java 25 (premissa A3).

O `00 §3` registra a escolha; as alternativas rejeitadas foram decididas pelo autor na Q-028.

## Decisão
Java 25 (LTS) e Spring Boot 4.1.2, fixados na T-001. O enforcer do Maven barra qualquer JDK abaixo do 25.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Java 21 (a LTS anterior) | Ainda é muito usada, mas o projeto quer mostrar a versão atual e as particularidades dela (DP-05) |
| Spring Boot 3.5 (a última da linha 3) | A linha 4 é a que dá suporte de primeira classe ao Java 25 |

## Consequências
- Positivas: o projeto envelhece mais devagar; as diferenças entre versões viram conteúdo ("Depende da versão").
- Negativas e mitigação: ferramentas mais novas às vezes ainda não declaram suporte ao Java 25; cada uma é conferida nas fontes oficiais antes de entrar (T-002).
- Como se prova: AC-T001.4 (o enforcer reprova outro JDK) e `docs/versoes.md`.
