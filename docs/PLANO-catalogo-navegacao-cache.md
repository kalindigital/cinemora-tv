# Plano de implementação — Navegação, catálogo completo, cache e visual moderno

Data: 2026-08-13
Projeto: cinemora-tv (Android TV / Xtream Codes)

## Objetivo

Transformar o app de uma tela única de filmes (VOD) num catálogo completo estilo
Netflix, com navegação lateral, TV ao vivo, séries, cache em disco com auto-login,
tela de detalhes e visual moderno.

## Requisitos aprovados

1. **Sidebar** com: Perfil, Pesquisa, Canais, Filmes, Séries, Categorias, Definições.
2. **Canais (TV ao vivo)** e **Séries** — dados novos via Xtream + player.
3. **Cache em disco** com validade configurável (6h/12h/24h) + botão "Limpar cache".
4. **Auto-login**: credenciais salvas no aparelho; ao reabrir, entra direto no catálogo.
   "Sair / trocar conta" no Perfil.
5. **Hero moderno** com a arte real do filme em destaque.
6. Fileiras **Continuar assistindo** (assistidos) e **Favoritos** (dados locais).
7. **Detalhes estilo Netflix** (opção A) ao clicar no card — filmes e séries: arte,
   sinopse, ▶ Assistir e ★ Favoritar; séries também com temporadas/episódios.
8. **Categorias como carrosséis**: cada categoria é uma fileira horizontal; navega para
   o lado dentro dela e desce para as próximas.
9. **Login estilo Netflix** (fundo cinematográfico, marca, card centralizado).

## Arquitetura

- **Camada pura e testável** (roda no JVM, sem Android): política de cache (validade),
  serialização do catálogo (JSON) e regras de favoritos/assistidos.
- **`LocalStore`** (SharedPreferences + `filesDir`): persiste credenciais, dados do
  usuário (favoritos/assistidos), catálogo em cache + timestamp e a validade escolhida.
- **`CinemoraRepository`**: orquestra login (cache → rede), refresh, limpar cache,
  favoritar, registrar assistido, logout.
- **`MainViewModel`**: expõe `autoLogin`, `signIn`, `refresh`, `clearCache`,
  `toggleFavorite`, `recordWatched`, `setCacheTtl`, `logout`.
- **UI Compose Material3** (sem trocar para tv.material3), com navegação por D-pad já
  corrigida (foco por ↑/↓ + rolagem).

## Fases

1. **Base**: camada pura (validade do cache, serialização, favoritos/assistidos),
   `LocalStore`, `CinemoraRepository`, auto-login. **Testes unitários** das regras puras.
2. **Canais + Séries**: modelos `Channel`/`Series`/`Season`/`Episode`, chamadas Xtream
   (`get_live_*`, `get_series*`, `get_series_info`), URLs de stream, player HLS
   (`media3-exoplayer-hls`). Testes de parsing.
3. **Navegação/sidebar**: shell com as 7 seções, foco e troca de conteúdo.
4. **Home moderna + detalhes**: hero com arte, fileiras Continuar/Favoritos, carrosséis
   por categoria, tela de detalhes (Assistir/Favoritar; séries com episódios).
5. **Definições**: validade do cache + limpar cache + info da conta.
6. **Login estilo Netflix**.

## Testes

- Fase 1: validade do cache, round-trip da serialização, toggle de favoritos e dedupe
  do histórico de assistidos.
- Fase 2: parsing de canais e séries a partir de payloads JSON de exemplo.
- Regras puras rodam com `junit` no JVM (sem device). `org.json` adicionado ao
  `testImplementation` para testar a serialização.

## Fora de escopo (por enquanto)

- EPG (guia de programação) dos canais.
- Busca no servidor (a Pesquisa filtra o catálogo já carregado).
- Perfis múltiplos (só a conta do provedor).
