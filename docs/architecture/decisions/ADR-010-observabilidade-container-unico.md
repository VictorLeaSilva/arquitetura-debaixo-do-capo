# ADR-010 · Observabilidade num container único

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-10 em 03

## Contexto
A telemetria tem três sinais: **traces** (o caminho de uma requisição, com o tempo de cada etapa), **métricas** (números agregados no tempo) e **logs** (eventos). Cada um costuma ter um componente próprio: um coletor OpenTelemetry, o Tempo (traces), o Loki (logs), o Prometheus (métricas) e o Grafana (as telas). Rodar os cinco separados pesa na memória da máquina de referência (A2).

## Decisão
No MVP, uma imagem única de desenvolvimento com os cinco componentes (`grafana/otel-lgtm`), recebendo traces, métricas e logs por OTLP (o protocolo do OpenTelemetry). A aplicação usa o suporte nativo do Spring Boot (ADR-026).

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Os cinco componentes em containers separados | Mais memória e mais configuração no MVP; ficam para a fatia 7, onde cada um é ensinado |

## Consequências
- Positivas: um serviço só no `compose.yaml`; cerca de 440 MiB ociosos (medido na T-004).
- Negativas e mitigação: a imagem é declarada só para desenvolvimento e varia bastante de memória (de 527 a 1.184 MiB de pico no spike H-05); o limite de 2 GiB no `compose.yaml` segura o excesso.
- Como se prova: AC-T004.3 e AC-T004.4 (T-004) e AC-009.4 (T-015).
