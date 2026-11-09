# Condição do ambiente e Git

## Diagnóstico observado

O diretório de trabalho contém `.git`, mas ele está vazio e com permissão `dr-xr-xr-x`. `git status` responde que não é um repositório, e `git init` não pode criar os metadados porque o ponto está somente leitura. O restante do projeto é gravável.

Essa é uma restrição do ambiente/montagem, não uma configuração do projeto. Nenhum comando para apagar, substituir ou alterar à força esse caminho foi executado.

## Correção segura recomendada

1. Encerrar a sessão ou contêiner que fornece o ponto `.git` somente leitura.
2. Confirmar que não há metadados Git a preservar; nesta inspeção o diretório apareceu vazio.
3. Tornar o caminho `.git` gravável pelo proprietário do projeto ou remover apenas a montagem externa que o sobrepõe.
4. Dentro de `/home/guilherme/raizesnordeste`, executar `git init` e conferir `git status`.
5. Revisar todos os arquivos antes do primeiro commit, especialmente `.env`, artefatos em `target` e credenciais.

Se o diretório contiver metadados em outra execução, deve-se fazer backup e investigar a origem antes de qualquer remoção.
