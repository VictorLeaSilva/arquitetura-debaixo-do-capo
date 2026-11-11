# C4 · Nível 2: containers

No modelo C4, um **container** é qualquer coisa que roda e guarda dados ou executa código: uma aplicação, um banco, um site. (Não é o mesmo que um container Docker, embora aqui quase todos rodem num.) O **nível 2** abre a caixa do [nível 1](c4-1-contexto.md) e mostra esses containers, as tecnologias de cada um e como eles conversam. Serve para quem vai desenvolver, operar ou estudar o sistema.

Os elementos marcados como **planejado** ainda não existem; chegam nas tarefas indicadas.

```mermaid
flowchart LR
    pessoa["Pessoa que estuda<br/>[Pessoa]"]

    subgraph maquina["Docker da máquina · docker compose --profile mvp"]
        tela["Tela do laboratório<br/>[Container: React + Vite, servida estática]<br/>Monta qualquer experimento a partir do descritor<br/>127.0.0.1:5173 · planejado (T-016, T-018)"]
        app["Aplicação<br/>[Container: Java 25 + Spring Boot 4.1]<br/>Módulos catalog, inventory, ordering e lab<br/>API /api/v1/lab · 127.0.0.1:8080 · planejado (T-010 a T-018)"]
        db[("PostgreSQL 18.6<br/>[Container: banco de dados]<br/>Catálogo, estoque, pedidos e execuções<br/>127.0.0.1:5432")]
        obs["Observabilidade<br/>[Container: grafana/otel-lgtm 0.40.0]<br/>Coletor OTel, Tempo, Loki, Prometheus e Grafana<br/>127.0.0.1:3000 (Grafana) e 127.0.0.1:4318 (OTLP)"]
    end

    site["Site de documentação<br/>[Docusaurus, npm start]<br/>Páginas de estudo · 127.0.0.1:3001<br/>publicação no GitHub Pages planejada (Q-032, T-029)"]

    pessoa -- "HTTP" --> tela
    pessoa -- "HTTP" --> site
    pessoa -- "HTTP: traces, logs, métricas" --> obs
    tela -- "HTTP/JSON: /api/v1/lab" --> app
    app -- "JDBC" --> db
    app -- "OTLP por HTTP" --> obs
    tela -. "link: Ver o trace" .-> obs
    tela -. "link: página de estudo" .-> site
```

| Container | Tecnologia | O que faz | Decisões |
|---|---|---|---|
| Tela do laboratório | React, TypeScript, Vite, Recharts | Monta a tela de cada experimento a partir do descritor e mostra as medições | ADR-005, ADR-020, ADR-023 |
| Aplicação | Java 25, Spring Boot 4.1, Maven | Executa os experimentos, uma execução por vez, e carrega os dados de referência | ADR-001 a ADR-004, ADR-006 a ADR-009, ADR-018, ADR-021 |
| PostgreSQL | PostgreSQL 18.6 | Guarda catálogo, estoque, pedidos e execuções; o volume persiste entre as subidas | ADR-009, ADR-014, ADR-017, ADR-019 |
| Observabilidade | grafana/otel-lgtm | Recebe a telemetria por OTLP e a mostra no Grafana | ADR-010, ADR-015, ADR-016, ADR-026 |
| Site de documentação | Docusaurus, Node 26 | As páginas de estudo, com comandos em bash e PowerShell; roda na máquina por `npm start`, fora do Docker (Q-030) | ADR-022 |

O nível 3 (os componentes dentro da aplicação) entra quando os módulos existirem, na Fase 1.
