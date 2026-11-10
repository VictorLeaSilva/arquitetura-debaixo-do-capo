# ADR-005 · Experimentos descritos por JSON e tela genérica

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-05 em 03

## Contexto
O laboratório vai ter muitos experimentos ao longo das fatias. Fazer uma tela à mão para cada um multiplicaria o código do front-end. Um **descritor** é um arquivo que descreve o experimento — título, versões, parâmetros, medições e perguntas de verificação — a partir do qual a tela se monta sozinha.

## Decisão
Cada experimento é um arquivo JSON (`classpath*:lab/experiments/*.json`), validado por um JSON Schema na inicialização: um descritor inválido impede a aplicação de subir. O front-end monta a tela a partir dele. Os testes acrescentam descritores próprios pelo classpath de teste.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Uma tela feita à mão por experimento | Código repetido em cada fatia; a tela genérica custa uma vez e serve a todos (Q-002) |
| Descritores guardados no banco | Não há necessidade de editar experimentos em tempo de execução; o arquivo versionado é mais simples e revisável |

## Consequências
- Positivas: um experimento novo é um JSON mais o código das versões; cada experimento vira um módulo autodescrito (DP-21).
- Negativas e mitigação: a tela só mostra o que o descritor sabe descrever; o schema evolui com uma versão (`schemaVersion`).
- Como se prova: os testes de RF-002 e AC-008.2 (a tela monta um experimento fictício).
