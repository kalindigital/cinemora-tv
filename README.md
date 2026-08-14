# Cinemora TV

App de catálogo para Android TV (Kotlin + Compose) que consome um provedor Xtream:
filmes, séries e canais ao vivo, com recomendação por IA opcional.

## Recursos

- Login do provedor com auto-login e cache do catálogo em disco (validade configurável)
- Filmes, Séries, Canais e Categorias em carrosséis, com busca e ordenação (A-Z, lançamento, nota)
- Detalhes estilo Netflix: continuar/reiniciar, favoritos, temporadas e episódios com marcação de vistos
- Player: retomada de onde parou, avanço progressivo (15s → 30s → 90s) e próximo episódio automático
- Recomendações relacionadas e busca por IA (voz ou texto) usando a sua chave da OpenAI
- Atualização pelo próprio app a partir do GitHub Releases

## Como compilar

Crie um `local.properties` na raiz (não versionado) com:

```properties
sdk.dir=/caminho/para/Android/sdk

# Opcional: chave usada apenas no build de debug
OPENAI_API_KEY=sk-...

# Repositório consultado pelo atualizador
GITHUB_REPO=usuario/repositorio

# Assinatura do release (mantenha o .jks em backup)
RELEASE_STORE_FILE=/caminho/para/cinemora-release.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=cinemora
RELEASE_KEY_PASSWORD=...
```

```bash
./gradlew :app:assembleDebug     # build local
./gradlew :app:assembleRelease   # APK assinado para publicar
./gradlew :app:testDebugUnitTest # testes
```

O APK público **não** embute chave da OpenAI: cada usuário configura a sua pelo QR code
em Definições → Recomendação por IA.

## Publicar uma atualização

```bash
./publicar.sh 1.1.0 "Correção das capas e avanço automático de episódio"
```

O script sobe a versão, compila o APK assinado, cria a tag e publica o release no GitHub.
O app detecta o novo release, mostra o changelog e instala a atualização.

## Instalação na TV

Copie o APK para um pendrive e instale pelo gerenciador de arquivos, ou use ADB por rede:

```bash
adb connect IP_DA_TV:5555
./gradlew :app:installDebug
```
