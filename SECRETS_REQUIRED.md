# Segredos necessários para builds assinadas

Este arquivo registra **quais** segredos o projeto precisa, mas nunca os valores.

## Android

- `ANDROID_KEYSTORE_BASE64` — keystore de assinatura codificado em Base64 para uso temporário no runner.
- `ANDROID_KEYSTORE_PASSWORD` — senha do keystore.
- `ANDROID_KEY_ALIAS` — alias da chave.
- `ANDROID_KEY_PASSWORD` — senha da chave.

## Outros serviços

Tokens/API keys futuros devem seguir o mesmo padrão: valor em GitHub Actions Secrets, apenas o nome documentado no Git.

## Regra

Não adicionar `.jks`, `.keystore`, `.p12`, `.pem`, `.key`, senhas ou tokens ao histórico Git. Mesmo um repositório privado pode ser clonado ou ter permissões alteradas, e um segredo removido de um commit continua recuperável no histórico.
