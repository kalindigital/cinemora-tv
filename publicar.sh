#!/usr/bin/env bash
# Publica uma nova versão: sobe o versionCode/versionName, compila o APK assinado,
# cria a tag e o release no GitHub com o changelog.
set -euo pipefail

VERSAO="${1:-}"
CHANGELOG="${2:-}"
if [ -z "$VERSAO" ] || [ -z "$CHANGELOG" ]; then
  echo "uso: ./publicar.sh <versao> \"<changelog>\"" >&2
  echo "ex.:  ./publicar.sh 1.1.0 \"- Avanço automático de episódio\"" >&2
  exit 1
fi

GRADLE="app/build.gradle.kts"
CODE_ATUAL=$(grep -E "versionCode = " "$GRADLE" | grep -oE "[0-9]+")
CODE_NOVO=$((CODE_ATUAL + 1))

sed -i '' "s/versionCode = $CODE_ATUAL/versionCode = $CODE_NOVO/" "$GRADLE"
sed -i '' "s/versionName = \".*\"/versionName = \"$VERSAO\"/" "$GRADLE"
echo "versão $VERSAO (código $CODE_NOVO)"

./gradlew :app:testDebugUnitTest :app:assembleRelease

git add -A
git commit -m "Versão $VERSAO"
git tag "v$VERSAO"
git push origin HEAD --tags

# O APK é anexado ao release, nunca versionado (fica fora da árvore do git).
APK="$(mktemp -d)/cinemora-tv-$VERSAO.apk"
cp app/build/outputs/apk/release/app-release.apk "$APK"
env -u GH_TOKEN gh release create "v$VERSAO" "$APK" --title "Cinemora $VERSAO" --notes "$CHANGELOG"
rm -f "$APK"
echo "release v$VERSAO publicado — o app vai oferecer a atualização"
