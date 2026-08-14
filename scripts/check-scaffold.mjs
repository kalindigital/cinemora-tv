import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const manifest = read('../app/src/main/AndroidManifest.xml');
const appGradle = read('../app/build.gradle.kts');
const client = read('../app/src/main/java/br/com/cinemora/tv/data/ProviderClient.kt');
const ui = read('../app/src/main/java/br/com/cinemora/tv/ui/CinemoraApp.kt');

assert.match(manifest, /LEANBACK_LAUNCHER/, 'o app deve aparecer no launcher Android TV');
assert.match(appGradle, /compileSdk = 35/, 'o SDK de compilação deve permanecer compatível com o AGP 8.7.3');
assert.doesNotMatch(appGradle, /media3-(?:exoplayer|ui):1\.10\./, 'Media3 1.10 exige compileSdk 36');
assert.match(appGradle, /jvmTarget = "17"/, 'Kotlin deve usar o mesmo alvo Java suportado pelo Android');
assert.match(client, /get_vod_categories/, 'o catálogo deve consultar categorias');
assert.match(client, /get_vod_streams/, 'o catálogo deve consultar vídeos');
assert.match(client, /JSONObject\(request\(credentials, null\)\)/, 'a resposta de autenticação deve ser convertida de texto para JSON');
assert.doesNotMatch(ui, /androidx\.compose\.foundation\.onFocusChanged/, 'onFocusChanged não existe no pacote foundation');
assert.match(ui, /androidx\.compose\.ui\.focus\.onFocusChanged/, 'onFocusChanged deve vir do pacote ui.focus');
assert.match(ui, /androidx\.compose\.foundation\.layout\.weight/, 'o layout dividido deve importar Modifier.weight');
assert.match(ui, /onFocusChanged/, 'os cartões precisam destacar o foco do controle remoto');
assert.match(ui, /Servidor/, 'a tela de login deve conter o campo Servidor');
assert.match(ui, /Usuário/, 'a tela de login deve conter o campo Usuário');
assert.match(ui, /Senha/, 'a tela de login deve conter o campo Senha');
console.log('Scaffold Android TV validado: login, catálogo, foco e player estão presentes.');
