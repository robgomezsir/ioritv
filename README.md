# 📺 IORI.Tv

**Sistema de gerenciamento de cadastros IPTV com interface moderna, skins de tema e relatórios financeiros.**

[![Android](https://img.shields.io/badge/Android-24%2B-green?logo=android)](https://developer.android.com/about/versions/nougat)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple?logo=kotlin)](https://kotlinlang.org/)
[![Material Design 3](https://img.shields.io/badge/Material-Design-3-blue)](https://m3.material.io/)
[![License](https://img.shields.io/badge/License-Proprietary-red)](#licença)

---

## 📸 Capturas de Tela

| Tema Claro | Tema Escuro | Skin Glass |
|:---:|:---:|:---:|
| ![Light](docs/screenshots/light.png) | ![Dark](docs/screenshots/dark.png) | ![Glass](docs/screenshots/glass.png) |

---

## ✨ Funcionalidades

### 🏠 Dashboard
- Visão geral com métricas de clientes ativos, receita e lucro
- Gráfico de saúde financeira com indicador visual de adimplência
- Fila de vencimentos dos próximos 7 dias
- Indicadores de lucro líquido, despesas e margem

### 📋 Carteira (Cadastros)
- CRUD completo de clientes (criar, ler, editar, excluir)
- Filtros por status: Ativo, A Vencer, Vencido, Standby, Inativo
- Busca por nome
- Cards expansíveis com detalhes do cliente
- Controle de créditos e datas de vencimento

### 💰 Finanças
- Registro de despesas operacionais
- Métricas: receita total, lucro líquido, margem de lucro, despesas
- Exportação de relatórios para Excel (.xlsx)
- FAB com ações rápidas (adicionar despesa, baixar relatório)

### 🎨 Temas e Skins
- **Tema Claro** — fundo branco suave com superfícies neutras
- **Tema Escuro** — fundo `#0E1415` com superfícies escuras harmoniosas
- **Skin Glass (Glassmorphism)** — aplicada sobre tema claro ou escuro
  - Aurora Frost animada com gradiente
  - Blur dinâmico (RenderEffect no Android 12+)
  - Superfícies translúcidas com sheen specular
  - Bordas luminosas nos cards
- **Tema Automático** — segue a configuração do sistema

### 🔐 Autenticação
- Login com Firebase Authentication
- Tela de recuperação de senha
- Cadastro de novos usuários
- Splash screen com vídeo animado

### 📊 Relatórios
- Exportação financeira em Excel com formatação profissional
- Dados incluem: receita, custos, despesas, lucro líquido, margem

---

## 🏗️ Arquitetura

```
app/src/main/java/com/elevadorcom/ioritv/
├── HomeActivity.kt              # Activity principal (hospedeira de fragments)
├── SplashActivity.kt            # Splash screen com vídeo
├── LoginActivity.kt             # Autenticação
├── RegisterActivity.kt          # Cadastro de usuários
├── ForgotPasswordActivity.kt    # Recuperação de senha
├── IORITvApplication.kt         # Application class
│
├── HomeFragment.kt              # Dashboard / Dashboard principal
├── ClientesFragment.kt          # Lista de clientes (Carteira)
├── ClienteFormFragment.kt       # Formulário criar/editar cliente
├── FinancasFragment.kt          # Gestão financeira
│
├── ClienteAdapter.kt            # Adapter da lista de clientes
├── DespesaAdapter.kt            # Adapter da lista de despesas
│
├── Utils/
│   ├── ThemeUtils.kt            # Gerenciamento de temas
│   ├── GlassUtils.kt            # Utilitário de glassmorphism
│   ├── DialogUtils.kt           # Estilização de diálogos Material3
│   ├── MoneyTextWatcher.kt      # Formatação monetária
│   ├── SituacaoUtil.kt          # Cálculo de situação do cliente
│   ├── ExcelExportUtil.kt       # Exportação para Excel
│   └── FullScreenVideoView.kt   # VideoView fullscreen para splash
│
├── model/
│   └── DespesaItem.kt           # Data class de despesa
│
└── binding/                     # ViewBinding gerado
```

---

## 🛠️ Tecnologias

| Tecnologia | Versão | Uso |
|------------|--------|-----|
| **Kotlin** | 1.9+ | Linguagem principal |
| **Jetpack Compose** | — | UI declarativa (parcial) |
| **Material Design 3** | 1.12+ | Design system |
| **Navigation Component** | 2.7+ | Navegação entre fragments |
| **Firebase Auth** | 32+ | Autenticação |
| **Firebase Firestore** | 24+ | Banco de dados NoSQL |
| **Firebase Messaging** | 23+ | Notificações push |
| **OneSignal** | — | Notificações push avançadas |
| **ViewBinding** | — | Acesso seguro às views |
| **Apache POI** | 5.2+ | Exportação Excel |
| **Coil** | 2.5+ | Carregamento de imagens |
| **ConstraintLayout** | 2.1+ | Layouts flexíveis |
| **RecyclerView** | 1.3+ | Listas performáticas |
| **MaterialDatePicker** | — | Seleção de datas |

---

## 📦 Instalação

### Pré-requisitos
- Android Studio Hedgehog (2023.1.1) ou superior
- JDK 17
- Dispositivo Android com minSdk 24 (Android 7.0)
- Conta Firebase configurada

### Passos

```bash
# 1. Clone o repositório
git clone https://github.com/robgomezsir/ioritv.git
cd ioritv

# 2. Abra no Android Studio e aguarde a sincronização do Gradle

# 3. Configure o google-services.json
#    Baixe do Firebase Console e coloque em app/

# 4. Execute no dispositivo ou emulador
./gradlew installDebug
```

### Download da Release

Acesse a página de [Releases](https://github.com/robgomezsir/ioritv/releases) para baixar o APK mais recente.

```bash
# Instalar via ADB
adb install app-release.apk
```

---

## 🎨 Guia de Temas

### Alternância de Temas
Acesse o menu de configurações (ícone ⚙️ no canto superior direito) para alternar entre:

| Modo | Descrição |
|------|-----------|
| **Automático** | Segue o tema do sistema Android |
| **Claro** | Fundo branco `#F5FAFB` |
| **Escuro** | Fundo `#0E1415` |
| **Glass** | Skin overlay com glassmorphism (pode ser combinado com claro ou escuro) |

### Paleta de Cores

#### Tema Escuro
| Token | Cor | Uso |
|-------|-----|-----|
| `background` | `#0E1415` | Fundo principal |
| `surface` | `#111A1B` | Superfícies |
| `surfaceContainer` | `#171D1E` | Cards |
| `primary` | `#82D3E0` | Acentos |
| `onSurface` | `#DEE3E5` | Texto principal |

#### Skin Glass
| Token | Cor | Uso |
|-------|-----|-----|
| `glass_surface` | `#1AFFFFFF` (10%) | Superfícies translúcidas |
| `glass_surface_container` | `#33FFFFFF` (20%) | Containers |
| `glass_on_surface` | `#DEE3E5` | Texto |
| `glass_outline_variant` | `#59FFFFFF` (35%) | Bordas |

---

## 📋 Regras de Versionamento

| Campo | Formato | Exemplo |
|-------|---------|---------|
| `versionName` | `X.Y.Z` | `2.0.2` |
| `versionCode` | `XYZ` (3 dígitos) | `202` |

### Semver
- **X** (Major): Mudanças que quebram compatibilidade
- **Y** (Minor): Novas funcionalidades
- **Z** (Patch): Correções de bugs

---

## 🚀 Publicação de Release

### Checklist (sempre seguir para novas versões)

1. **Atualizar versionamento** em `app/build.gradle.kts`:
   ```kotlin
   versionCode = XYZ
   versionName = "X.Y.Z"
   ```

2. **Build release**:
   ```bash
   ./gradlew clean assembleRelease --no-daemon
   ```

3. **Testar no dispositivo**:
   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

4. **Commit com mensagem descritiva**:
   ```bash
   git add -A
   git commit -m "release: vX.Y.Z - [descrição]"
   git push origin main
   ```

5. **Criar Release no GitHub**:
   ```bash
   gh release create vX.Y.Z \
     app/build/outputs/apk/release/app-release.apk \
     --title "vX.Y.Z - [Título]" \
     --notes-file CHANGELOG.md
   ```

6. **Atualizar CHANGELOG.md** com as mudanças da versão.

---

## 📄 Changelog

### v2.0.2 (22/08/2026)
- ✨ Skin Glass com toolbar transparente e aurora visível
- ✨ Status bar e nav bar sincronizadas com o tema
- ✨ Dialogs Material3 com 95% opacidade no glass
- ✨ Campo "Descrição" no modal de despesas
- ✨ Scroll dinâmico no formulário de cadastro
- ✨ Cores de contraste no status de saúde financeira
- 🔧 Cards da Carteira com background uniforme no glass
- 🔧 FAB da Carteira com ícone original (adduser.png)
- 🔧 Espaçamento Material 3 entre FABs secundários

### v2.0.1
- 🐛 Correção de crash ao alternar tema claro
- 🐛 Correção de cores hardcoded nos badges
- 🔧 Ajuste de espaçamento nos cards de cadastro

### v2.0.0
- 🚀 Migração para estrutura de Fragments (HomeActivity única)
- 🚀 Skin Glass (Glassmorphism) com aurora animada
- 🚀 Tema Material Design 3 completo
- ✨ Dashboard com métricas financeiras
- ✨ Exportação de relatórios Excel
- ✨ Sistema de temas (Automático, Claro, Escuro, Glass)
- ✨ Login com Firebase Authentication

---

## 🛡️ Segurança

- **Backup**: `allowBackup` com exclusão de SharedPreferences de tema
- **API Keys**: Protegidas via `google-services.json` (não commitado)
- **Autenticação**: Firebase Auth com sessão persistente
- **Dados**: Firestore com regras de segurança server-side

---

## 📁 Estrutura do Repositório

```
ioritv/
├── app/
│   ├── src/main/
│   │   ├── java/           # Código Kotlin
│   │   ├── res/            # Recursos (layouts, drawables, valores)
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts    # Dependências do módulo
│   └── google-services.json # Config Firebase (não commitado)
├── gradle/
│   └── libs.versions.toml  # Catálogo de versões
├── build.gradle.kts        # Build do projeto
├── settings.gradle.kts     # Configurações
├── README.md               # Este arquivo
└── CHANGELOG.md            # Histórico de versões
```

---

## 🤝 Contribuição

1. Fork o repositório
2. Crie uma branch para sua feature (`git checkout -b feature/nova-feature`)
3. Commit suas mudanças (`git commit -m 'feat: adicionar nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request

### Convenções de Commit

| Prefixo | Descrição |
|---------|-----------|
| `feat:` | Nova funcionalidade |
| `fix:` | Correção de bug |
| `refactor:` | Refatoração sem mudar comportamento |
| `docs:` | Documentação |
| `style:` | Formatação, sem mudança de código |
| `test:` | Adição/correção de testes |
| `chore:` | Tarefas de manutenção |

---

## ⚙️ Configuração Firebase

### Firestore Collections

```
clientes/
  ├── NOME: String
  ├── USUARIO: String
  ├── SENHA: String
  ├── WHATSAPP: String
  ├── MODELO: String
  ├── INICIO: Timestamp
  ├── TERMINO: Timestamp
  ├── CREDITOS: Number
  ├── MAC: String
  ├── OTP: String
  ├── DEVICE: String
  ├── SERVIDOR: String
  ├── VALOR: Number
  ├── CUSTO: Number
  ├── DESCONTO: Number
  ├── SITUACAO: String
  └── VENCIMENTO: String

despesas/
  ├── data: String (dd/MM/yyyy)
  ├── descricao: String
  ├── detalhes: String
  ├── valor: Number
  └── dataTimestamp: Number
```

---

## 📌 Requisitos Mínimos

| Requisito | Versão |
|-----------|--------|
| Android | 7.0 (API 24) |
| Target SDK | 36 |
| Java | 17 |
| Gradle | 8.5+ |

---

## 📄 Licença

© 2026 Elevador.com — Todos os direitos reservados.

Este software é proprietário e confidencial. A reprodução ou distribuição não autorizada é estritamente proibida.

---

## 📞 Contato

- **Desenvolvedor**: robgomezsir
- **Repositório**: [github.com/robgomezsir/ioritv](https://github.com/robgomezsir/ioritv)
- **Issues**: [Abra uma issue](https://github.com/robgomezsir/ioritv/issues)

---

*Feito com ❤️ para Android*
