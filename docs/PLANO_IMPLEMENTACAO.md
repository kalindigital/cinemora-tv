# Plano de implementação — Cinemora TV

## Objetivo

Criar um cliente Android TV para uma conta de conteúdo autorizada: a pessoa informa servidor, usuário e senha, explora vídeos organizados por categoria e os reproduz com o controle remoto.

## Direção visual

- **Público e função:** pessoa diante da TV, a poucos metros da tela; a interface precisa privilegiar foco, leitura rápida e ações pelo D-pad.
- **Paleta:** Ink `#07090D` (fundo), Panel `#10151D` (cartões), Mist `#E7EDF5` (leitura), Signal `#77D7FF` (foco), Coral `#FF715B` (ação de assistir), Slate `#9EABB9` (metadados).
- **Tipografia:** sans-serif larga e pesada para títulos; sans-serif regular para sinopses; caixa-alta com espaçamento para rótulos de navegação.
- **Layout:** login dividido em duas metades; catálogo com seleção principal horizontal e fileiras de capas. As fileiras mantêm o contexto da categoria na TV.
- **Assinatura:** em vez de depender de vermelho e de referências de marca de terceiros, o app usa um anel azul de foco bem visível e uma ação coral. Isso torna a navegação pelo controle parte da identidade visual.

## Entregas

- [x] Projeto Kotlin/Compose configurado como aplicativo Android TV (Leanback launcher e paisagem).
- [x] Tela de login com Servidor, Usuário e Senha; nenhum dado de acesso incluído no código.
- [x] Cliente de catálogo VOD compatível com API de provedor, agrupando títulos por categoria.
- [x] Catálogo com foco por D-pad, destaque do item selecionado e ação de reprodução.
- [x] Reprodução com Media3/ExoPlayer.
- [x] Testes unitários para geração da URL de reprodução e checagem estática local.

## Funcionamento

1. A pessoa informa os dados da sua conta de conteúdo licenciado.
2. O app valida a conta e consulta as categorias e vídeos VOD do servidor.
3. Os vídeos são agrupados nas fileiras do catálogo; setas movimentam o foco e Enter abre o player.
4. A URL do vídeo é montada somente em memória e entregue ao ExoPlayer.

## Próxima evolução antes de publicar

- Armazenar sessão com Android Keystore, preferencialmente exigindo HTTPS.
- Tratar favoritos, continuar assistindo e perfis locais.
- Adicionar ícones adaptativos, captura de telas e testes instrumentados em emulador/TV física.
