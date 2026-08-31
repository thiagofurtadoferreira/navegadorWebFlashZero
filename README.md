# Navegador Web Flash Zero

Repositório oficial privado do projeto **Navegador Web Flash Zero**.

## Versões preservadas

| Plataforma / arquivo | Versão | Status GitHub | Validação |
|---|---:|---|---|
| Android — projeto Gradle | **v0.4.18** | ✅ GitHub | ✅ Validado |
| Android — APK completo | **v0.4.18** | ✅ GitHub | ✅ ZIP/CRC + DEX + assinatura |
| Android — source ZIP completo | **v0.4.18** | ✅ GitHub | ✅ ZIP íntegro |
| Windows — código-fonte | **Preview v0.1.0 x64** | ✅ GitHub | ✅ Build Actions |
| Windows — pacote portátil | **Preview v0.1.0 x64** | 🟡 GitHub Actions artifact | ✅ ZIP + SHA-256 |
| iOS — source ZIP | **v0.1.0** | ⏳ Upload pendente | ✅ Validado localmente |
| DRM autorizado / Media3 — source ZIP | **v0.4.0** | ⏳ Upload pendente | ✅ Validado localmente |
| Keystores, senhas e tokens | — | 🔐 Secret store | 🔐 Não versionados em texto aberto |

Inventário detalhado e hashes: [`STATUS.md`](STATUS.md).

- Pacote Android: `org.navegadorwebdozero.preview`
- Estado Android: versão estável de referência antes das alterações da v0.4.19
- APK Android SHA-256: `f5c944f1d1d1abf2d782143ea6b607f31967f105667fa954aaffbc9e9df49ba2`
- Windows ZIP SHA-256: `d9883f9e9a41f658628538d15e23a4ef2a1bc5f14f9ca411b8a43259aa3735b9`

## Estrutura

- `android/` — estrutura do projeto Android/Gradle.
- `windows/` — código da versão Windows baseada em Electron.
- `.github/workflows/windows-build.yml` — compilação reproduzível Windows x64.
- `tools/android/v0.4.18/` — snapshot dos arquivos-chave da versão Android estável.
- `artifacts/android/v0.4.18/` — APK, source ZIP, ícone, notas e validação Android.
- `artifacts/windows/v0.1.0/` — registro e validação da compilação Windows.

## Windows Preview v0.1.0

A primeira build Windows x64 foi compilada com sucesso no GitHub Actions a partir do commit `8eb61cb4c4a5b3658951463d61d59f79281ff3c0`, usando Electron 43.4.0 e `@electron/packager` 20.3.0. O pacote é portátil e não possui assinatura Authenticode nesta prévia, portanto o Windows SmartScreen pode exibir um aviso no primeiro uso.

O ZIP portátil excede o limite normal de arquivo individual do GitHub e é preservado como GitHub Actions artifact `9545875547`.

## Estado da gravação por área Android

Na v0.4.18 a seleção X1/X2/Y1/Y2 e o controle de parada funcionam. O MP4 ainda usa a captura da tela inteira; o recorte físico da região selecionada e os perfis de resolução pertencem ao trabalho da v0.4.19 e não foram misturados nesta referência estável.

## Segurança

Código, documentação, builds e artefatos não secretos são preservados no repositório. Chaves privadas de assinatura, senhas, tokens e outros segredos permanecem fora do histórico Git e devem ser fornecidos ao CI por GitHub Actions Secrets ou cofre equivalente. Isso continua valendo mesmo para repositórios privados.
