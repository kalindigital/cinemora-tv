# Tarefas — Cinemora TV

- [x] Criar base Android TV nativa com Kotlin e Compose em paisagem.
- [x] Implementar login com Servidor, Usuário e Senha, sem registrar credenciais no repositório.
- [x] Consultar catálogo VOD autorizado, exibir categorias e reproduzir vídeo.
- [x] Implementar navegação por D-pad com estado de foco visível.
- [x] Criar testes unitários e uma checagem estática executável localmente.
- [x] Corrigir incompatibilidade do Media3 1.10.1 com compileSdk 35 e fixar Java/Kotlin 17.
- [x] Corrigir compilação Kotlin do parse de autenticação e do foco dos cartões.
- [x] Corrigir foco/rolagem do login para o botão "Entrar no catálogo" ficar visível.
- [x] Normalizar a nota do card para uma casa decimal (X.X) e ocultar nota 0.

## Feature: catálogo completo, navegação, cache e visual moderno

Plano: docs/PLANO-catalogo-navegacao-cache.md

### Fase 1 — Base (persistência, cache, auto-login)
- [ ] Camada pura: política de validade do cache (6h/12h/24h) com testes.
- [ ] Camada pura: serialização do catálogo (JSON round-trip) com testes.
- [ ] Camada pura: favoritos (toggle) e histórico de assistidos (dedupe, recentes) com testes.
- [ ] `LocalStore` (SharedPreferences + filesDir): credenciais, dados do usuário, catálogo em cache.
- [ ] `CinemoraRepository` + `MainViewModel` (autoLogin, refresh, clearCache, favoritar, assistido, logout).

### Fase 2 — Canais (TV ao vivo) e Séries
- [x] Modelos e parsing de canais (get_live_*) com testes.
- [x] Modelos e parsing de séries (get_series*, get_series_info) com testes.
- [x] URLs de stream de live/série e player HLS (media3-exoplayer-hls).

### Fase 3 — Navegação/sidebar
- [x] Shell com Perfil, Pesquisa, Canais, Filmes, Séries, Categorias, Definições.

### Fase 4 — Home moderna + detalhes
- [x] Hero com arte real, fileiras Continuar assistindo e Favoritos, carrosséis por categoria.
- [x] Tela de detalhes estilo Netflix (Assistir/Favoritar; séries com episódios).

### Fase 5 — Definições
- [x] Validade do cache + limpar cache + info da conta.

### Fase 6 — Login estilo Netflix
- [x] Fundo cinematográfico, marca e card centralizado.

### Refinamentos de UI (round 2)
- [x] Tema vermelho (acento, botões, fundo do login) no lugar do azul.
- [x] Navegação em barra de topo estilo playbox (logo + abas; Pesquisa/Definições/Perfil como ícones) com o banner atrás do header.
- [x] Hero com mais espaço no topo, "SELEÇÃO DO DIA" e arte atrás da barra.
- [x] Sinopse de filme buscada via get_vod_info (a listagem não retorna plot).
- [x] Detalhe de série com duas listas (Temporadas | Episódios) e títulos de episódio limpos.
- [x] Busca sem duplicatas (dedupe por título) e cards com título em 2 linhas mantendo ano/nota.
- [x] Login sem card: logo + CINEMORA centralizados, só os campos, botão "Entrar".
- [x] Ícone e banner do app: logo centralizada em fundo preto.

### Player e navegação (round 3)
- [x] Player em View pura: controles respondem ao D-pad; abrem só no botão central.
- [x] Tela não apaga durante a reprodução (FLAG_KEEP_SCREEN_ON).
- [x] Retomar de onde parou (com 15s de volta); posição descartada ao terminar ou em canal ao vivo.
- [x] Avanço progressivo: 15s → 30s → 90s em cliques seguidos.
- [x] Detalhe substitui o conteúdo (corrige foco preso na lista de trás).
- [x] Estado da aba/item preservado ao voltar do player (rememberSaveable).
- [x] Banner de seleção também na aba Séries; botão não corta mais.

### Recomendação por IA e ordenação
- [x] Botão IA no header com entrada por voz (pt-BR) e por texto.
- [x] OpenAI (chave em local.properties, fora do código) sugere títulos; casamos com o catálogo local.
- [x] Ordenação das fileiras: do provedor, A-Z, lançamento e nota (em Definições, com testes).

### Player avançado, IA e chave por QR
- [x] Avanço automático do próximo episódio (contagem de 10s, Voltar cancela).
- [x] Raízes de certificado embutidas (121 da Mozilla) para as capas carregarem em TV antiga.
- [x] Recomendações no rodapé do detalhe de filme e série, e ao terminar um filme.
- [x] Sinal de "filme concluído" persistido em disco (a TV destrói a Activity durante o player).
- [x] Chave da OpenAI por QR code: servidor local na TV + página no celular com link de ajuda da OpenAI.
