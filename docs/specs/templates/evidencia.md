# Evidências T-xxx (implementador)

Data: <AAAA-MM-DD>. Ambiente: <SO, runtime, versões relevantes, imagens>.

## Plano (escrito antes do código)
| AC | Teste (arquivo#nome) | Arquivos de produção | Item de "Decisões da tarefa" |
|---|---|---|---|
Todos os arquivos acima casam com `allowed_paths`: <sim | lacuna registrada em Q-nnn>.

## Base e spec antes do código
- **Base:** <branch e commit>.
- **Branch de spec:** <docs/spec-T-xxx, commits e Q-nnn decididas> | nenhuma mudança de spec.
- Lacunas surgidas durante a implementação: <nenhuma | Q-nnn>.

## Teste primeiro
1. Testes escritos antes do código (esqueletos mínimos só para compilar: <quais>).
2. **Vermelho:** <comando>; <n> executados, <f> falhas e <e> erros; por teste: <...>; nenhum teste de tarefa anterior falhou.
3. **Verde:** <ajustes feitos nos próprios testes e por quê>.

## Escolhas dentro da latitude
| Escolha | Item da latitude que a permite |
|---|---|
(nenhuma | lista)

## Gate
- `<comando do build>`: <resultado e números>.
- Problemas apontados pelos gates e como foram corrigidos (sem supressão | supressão com motivo/validade): <...>.
- `BASE=<base> scripts/verify-task.sh T-xxx`: <resultado; log em docs/specs/reviews/T-xxx.verify.log>.

## Matriz de conformidade (autorrevisão)
| Decisão ou AC | arquivo:linha | Teste que falharia se quebrasse |
|---|---|---|

## Riscos provados
| Risco | Teste | Forma | Resultado |
|---|---|---|---|

## README vivo
<N/A: a tarefa não muda como subir/usar — porquê> | <shell, versão, comando, saída observada, para cada passo afetado, em stack isolada>

## Números medidos
<N/A | valor, data, ambiente, comando, arquivo de resultado versionado>
