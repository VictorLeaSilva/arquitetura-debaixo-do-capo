# ADR-015 · Consultas SQL como spans no trace

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-15 em 03

## Contexto
Um **span** é uma etapa de um trace, com início, fim e atributos. Ver cada consulta SQL como um span dentro da execução de um experimento é a imagem mais forte do N+1: 51 spans na versão ruim contra 1 na boa. O suporte nativo do Spring Boot não cria spans de JDBC sozinho.

## Decisão
Usar `net.ttddyy.observation:datasource-micrometer-spring-boot`, na linha 2.x (a compatível com o Spring Boot 4; 2.3.0 em 2026-11-09), adicionada na T-015 (Q-009).

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Não mostrar SQL no trace no MVP | Perde a demonstração visual do experimento N+1 |
| Agente Java do OpenTelemetry | Fica para a fatia 7, comparado com o suporte nativo num experimento próprio (ADR-026) |

## Consequências
- Positivas: as consultas aparecem como filhas do span de cada versão, sem código no domínio.
- Negativas e mitigação: uma dependência a mais; a contagem exata do experimento não depende dela (ADR-007).
- Como se prova: AC-009.4, com o trace no Grafana (T-015).
