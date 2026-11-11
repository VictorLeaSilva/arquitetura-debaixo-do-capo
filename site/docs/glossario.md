---
title: Glossário
sidebar_position: 3
---

# Glossário

Os termos que aparecem nas páginas de estudo, em ordem alfabética. Cada página também define, no começo, os termos de que precisa.

## `127.0.0.1` (localhost)
O endereço que um computador usa para falar com ele mesmo. Um programa que escuta só em `127.0.0.1` não pode ser acessado por outra máquina da rede; por isso o laboratório publica todas as portas nesse endereço.

## ADR (*Architecture Decision Record*)
Um documento curto que registra uma decisão de arquitetura: o contexto, o que foi escolhido, as alternativas rejeitadas e as consequências. Os deste projeto ficam em `docs/architecture/decisions/` no repositório.

## bash
Um *shell* (programa que interpreta os comandos digitados no terminal) usado no Linux e no macOS; no Windows, vem com o Git for Windows como Git Bash. [Manual do bash](https://www.gnu.org/software/bash/manual/).

## Build
O processo que transforma o código-fonte no resultado que roda ou é publicado: compilar, juntar arquivos e gerar as páginas. No site, `npm run build` gera as páginas HTML em `site/build/`.

## Container
Um processo que roda isolado do resto do computador, com os próprios arquivos e a própria rede, a partir de uma imagem (um pacote com o programa e tudo de que ele precisa). Sobe em segundos e some sem deixar resto.

## Docker
A ferramenta que cria e roda os containers. O laboratório usa o Docker Compose, que sobe vários containers de uma vez a partir do arquivo `compose.yaml`. [Documentação do Docker](https://docs.docker.com/).

## LTS (*Long-Term Support*)
Uma versão com suporte estendido: recebe correções de segurança por anos, enquanto as outras param de receber em poucos meses. O projeto usa as versões LTS do Java e do Node.js.

## Node.js
Um programa que executa JavaScript fora do navegador. O site de documentação precisa dele para ser gerado. [Versões do Node.js](https://nodejs.org/en/about/previous-releases).

## npm
O gerenciador de pacotes que vem com o Node.js: baixa as bibliotecas listadas no `package.json` e roda os comandos definidos nele (`npm start`, `npm run build`). [Documentação do npm](https://docs.npmjs.com/).

## Porta
Um número de 0 a 65535 que identifica qual programa recebe uma conexão num mesmo computador. Neste projeto, por exemplo, o Grafana usa a 3000 e o site usa a 3001.

## PowerShell
O *shell* padrão do Windows. O projeto testa os comandos no Windows PowerShell 5.1, o que vem instalado no Windows 11. [Documentação do PowerShell](https://learn.microsoft.com/powershell/).

## Terminal
A janela onde se digitam comandos em texto. O programa que interpreta esses comandos é o *shell* (bash ou PowerShell).
