# Snapshot Android v0.4.18

Este diretório preserva o `MainActivity.java` original da versão estável Android v0.4.18 do Navegador Web Flash Zero.

O arquivo compactado original precisou ser representado em partes Base64 para migração segura entre repositórios:

- `MainActivity.java.gz.b64.part01` até `part13`
- `restore_main_activity.py` recompõe o GZIP, descompacta o Java e valida sua integridade antes de gravá-lo no projeto Android.

## Restaurar

Na raiz do repositório execute:

```bash
python3 tools/android/v0.4.18/restore_main_activity.py
```

O destino será:

`android/app/src/main/java/org/navegadorwebdozero/preview/MainActivity.java`

## Integridade

SHA-256 esperado do `MainActivity.java` restaurado:

`ea6aba20aed1a79fdd36f38282c144a0710945132b860ce46a1ec1fbadad92f6`

## Versão preservada

- versão: `0.4.18`
- versionCode: `418`
- pacote: `org.navegadorwebdozero.preview`
- APK estável SHA-256: `f5c944f1d1d1abf2d782143ea6b607f31967f105667fa954aaffbc9e9df49ba2`

A v0.4.18 é a referência estável anterior às alterações da v0.4.19. Chaves de assinatura, senhas e tokens não fazem parte deste snapshot.
