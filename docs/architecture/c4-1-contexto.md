# C4 · Nível 1: contexto

O **modelo C4** descreve um sistema em quatro níveis de zoom, cada um para um público. O **nível 1, contexto**, é o mais aberto: mostra o sistema como uma caixa só, quem o usa e com que outros sistemas ele conversa. Serve para qualquer pessoa entender, em um minuto, do que se trata, sem nenhum detalhe técnico interno.

```mermaid
flowchart TB
    estudante["Pessoa que estuda arquitetura<br/>[Pessoa]<br/>Roda os experimentos e lê as páginas de estudo"]
    autor["Autor<br/>[Pessoa]<br/>Constrói o laboratório e publica os resultados"]

    lab["Laboratório Arquitetura Debaixo do Capô<br/>[Sistema]<br/>Mostra conceitos de arquitetura num e-commerce com flash sale,<br/>em versões ruim, média e boa, medidas lado a lado"]

    navegador["Navegador<br/>[Sistema externo]<br/>Mostra a tela do laboratório, o Grafana e o site"]
    docker["Docker da máquina<br/>[Sistema externo]<br/>Roda os containers do laboratório"]
    github["GitHub<br/>[Sistema externo]<br/>Guarda o código e roda o CI em cada PR"]

    estudante -- "usa pelo" --> navegador
    navegador -- "abre as telas do" --> lab
    autor -- "desenvolve e publica no" --> github
    github -- "verifica cada mudança do (CI)" --> lab
    lab -- "roda dentro do" --> docker
```

| Elemento | O que é |
|---|---|
| Pessoa que estuda arquitetura | Quem roda os experimentos na própria máquina e lê as explicações |
| Autor | Quem constrói o laboratório, mede e publica os resultados |
| Laboratório | O sistema inteiro: a tela, a aplicação, o banco, a observabilidade e o site (detalhados no [nível 2](c4-2-containers.md)) |
| Navegador | Onde a pessoa vê a tela do laboratório, o Grafana e as páginas de estudo |
| Docker da máquina | Onde todos os containers rodam, com as portas presas a `127.0.0.1` (ADR-013) |
| GitHub | Onde o código fica e onde o CI confere cada PR (gates, harness, segredos, dependências) |
