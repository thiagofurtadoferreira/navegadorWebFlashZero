# Navegador Web Flash Zero

Repositório oficial do projeto **Navegador Web Flash Zero**.

## Referência estável Android

A versão Android preservada como base estável é a **v0.4.18**, anterior às alterações rejeitadas da v0.4.19.

- Versão: `0.4.18`
- versionCode: `418`
- Pacote: `org.navegadorwebdozero.preview`
- APK estável SHA-256: `f5c944f1d1d1abf2d782143ea6b607f31967f105667fa954aaffbc9e9df49ba2`
- MainActivity.java SHA-256: `ea6aba20aed1a79fdd36f38282c144a0710945132b860ce46a1ec1fbadad92f6`

## Estrutura Android

- `android/` — projeto Android/Gradle da v0.4.18.
- `android/app/src/main/java/org/navegadorwebdozero/preview/MainActivity.java` — Activity principal restaurada e validada a partir do snapshot estável.
- `tools/android/v0.4.18/` — snapshot Base64/GZIP, hashes e ferramenta de restauração reproduzível do `MainActivity.java`.
- `.github/workflows/restore-android-v0418.yml` — valida e restaura automaticamente o `MainActivity.java` a partir do snapshot caso seja necessário.

## Integridade da recuperação

O `MainActivity.java` foi reconstruído a partir do snapshot compactado preservado no repositório recuperado e validado por SHA-256 antes de ser gravado nesta árvore. Nenhum código da v0.4.19 rejeitada foi usado para substituir a Activity estável.

## Estado funcional preservado

A v0.4.18 mantém a referência funcional anterior ao trabalho da v0.4.19. Entre os recursos preservados estão a seleção X1/X2/Y1/Y2 para gravação, controle de parada, reprodução em segundo plano e estrutura do navegador existente nessa versão.

## Segurança

Keystores, chaves privadas, senhas, tokens e outros segredos não são versionados. Os nomes de secrets necessários para builds assinadas estão documentados em `SECRETS_REQUIRED.md`.
