# Changelog

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/),
e este projeto adere ao [Semantic Versioning](https://semver.org/lang/pt-BR/).

---

## [2.0.2] - 2026-08-22

### ✨ Adicionado
- **Skin Glass com toolbar transparente**: aurora brilha através da toolbar e barras de sistema
- **Campo "Descrição"**: adicionado ao modal Adicionar/Editar Despesa com ícone de documento
- **Scroll dinâmico**: formulário de cadastro sobe automaticamente quando o teclado abre (via ViewTreeObserver)
- **Cores de contraste no status de saúde financeira**:
  - Excelente (≥80%): verde `#2E9E5A`
  - Em observação (≥50%): dourado `#E8913A`
  - Precisa de atenção (<50%): vermelho (error)
- **Espaçamento Material 3** entre FABs secundários na aba Finanças (160dp)

### 🔧 Alterado
- **ThemeUtils**: aplica `Base.Theme.IORITv.Glass` automaticamente quando `glass_enabled=true`
- **DialogUtils**: dialogs glass com 95% opacidade (`0xF20E1415` escuro / `0xF2FFFFFF` claro)
- **FinancasFragment**: todos os dialogs agora usam `styleAlertDialogButtons`
- **ClienteAdapter**: `expandedLayout` transparente (uniforme com o card)
- **FAB Carteira**: tint removido — mostra PNG `adduser.png` com cores originais

### 🐛 Corrigido
- Status bar e nav bar sincronizadas com a cor da toolbar em todos os modos
- Toolbar glass agora usa tema transparente via XML (não apenas programático)
- Dialogs com fundo legível no glass (antes era translúcido demais)
- FABs secundários muito próximos (colados)

### 📦 Build
- `versionCode`: 202
- `versionName`: 2.0.2
- `minSdk`: 24 | `targetSdk`: 36

---

## [2.0.1] - 2026-08-21

### 🐛 Corrigido
- Crash ao alternar para tema claro
- Cores hardcoded nos drawables de badges
- Status bar e nav bar não acompanhavam o tema

### 🔧 Alterado
- Cards de cadastro com espaçamento uniforme
- Data picker Material3 restaurado com tema correto
- Botões de dialogs estilizados com Material3

---

## [2.0.0] - 2026-08-21

### 🚀 Inicial
- **Migração completa para Fragments** (HomeActivity única)
- **Skin Glass (Glassmorphism)** com aurora animada e blur dinâmico
- **Tema Material Design 3** completo (claro, escuro, automático)
- **Dashboard** com métricas de saúde financeira
- **Carteira**: CRUD de clientes com filtros e busca
- **Finanças**: registro de despesas e exportação Excel
- **Sistema de temas** com alternância em tempo real
- **Login Firebase** com recuperação de senha
- **Splash screen** com vídeo animado

---

## [1.0.0] - 2026-08-15

### 🚀 Inicial
- Versão inicial do IORI.Tv
- Funcionalidades básicas de cadastro e gerenciamento
