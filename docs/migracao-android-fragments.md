# Plano de Migração — Android para Single-Activity + Fragments

**Projeto:** IORI.TV (app Android `com.elevadorcom.ioritv`)
**Data:** 18/08/2026
**Escopo:** Somente o app Android. Banco de dados, cálculos e back-end (Cloud Functions) **inalterados**.
**Status (18/08/2026):** Fases 1–7 **concluídas**, compiladas e testadas no device (Moto G56, Android 16). Fase 8 (regressão formal) pendente.

---

## Status da migração

| Fase | Descrição | Status |
|---|---|---|
| 0 | Preparação | ⏭️ Absorvida pela Fase 1 (sem commit de baseline separado) |
| 1 | Esqueleto single-activity + 1 aba | ✅ Concluída e testada |
| 2 | HomeFragment (RankingActivity) | ✅ Concluída e testada |
| 3 | ClientesFragment (MainActivity2) | ✅ Concluída e testada |
| 4 | FinancasFragment (MainActivity4) | ✅ Concluída e testada |
| 5 | ClienteFormFragment (MainActivity + EditCadastroActivity) | ✅ Concluída e testada |
| 6 | Centralização (WorkManager + OneSignal) | ✅ Concluída e verificada no device |
| 7 | Limpeza (Activities/layouts órfãos) | ✅ Concluída e testada |
| 8 | Regressão formal | ⏳ Pendente — coberta pontualmente pelos testes de cada fase |

---

## 1. Contexto e objetivos

Hoje o app usa **11 Activities**, sendo que as 3 abas do bottom nav (`RankingActivity`, `MainActivity2`, `MainActivity4`) são Activities separadas que se chamam via `startActivity`/`finish`. Cada troca de aba recria a tela, refaz todas as leituras do Firestore e duplica código (toolbar, menu de tema, logout, WorkManager, helpers de data).

**Objetivo:** migrar para **1 Activity hospedeira + Fragments** usando o Navigation Component (já presente nas dependências: `androidx.navigation:navigation-fragment-ktx:2.8.5` e `navigation-ui-ktx:2.8.5`).

**Público:** app de uso exclusivo de 1 admin, sem distribuição. Não há requisitos de multi-usuário, conta de serviço, onboarding ou assinatura de release para loja.

---

## 2. Restrições (invariantes — NÃO mudar)

| Invariante | Detalhe |
|---|---|
| **Firestore intacto** | Mesmas coleções e campos: `clientes/{id}` (NOME, USUARIO, SENHA, WHATSAPP, MODELO, INICIO, TERMINO, CREDITOS, VENCIMENTO, MAC, OTP, DEVICE, VALOR, CUSTO, DESCONTO, SERVIDOR, SITUACAO), `despesas/{id}` (data, descricao, valor, dataTimestamp), `configuracoes/custoTotal` (valor, ultimaAtualizacao). Sem migração de dados. |
| **Cálculos idênticos** | `TERMINO = INICIO + CREDITOS meses`; status: ATIVO (>2 dias), A VENCER (0..2), VENCIDO (−14..−1), STANDBY (≤ −15). Strings de VENCIMENTO idênticas ("Faltam X dias", "Vence hoje", "Já são X dias vencidos"...). |
| **Back-end intacto** | `functions/index.js` (onClienteWrite + checkSituacaoDaily) não é tocado. |
| **Preferências intactas** | Mesmas chaves SharedPreferences: `AppSettings`/`theme_mode`, `login_prefs` (sessão), `AppPreferences`/`totalCredit` (padrão 150). Usuários existentes mantêm tema, sessão e crédito total. |
| **Mensagens/UX** | Toasts, títulos e botões de diálogos, content descriptions de acessibilidade e textos mantidos (evita regressões percebidas). |

---

## 3. Estado atual — inventário

### Activities de pré-autenticação (ficam como estão)
- `SplashActivity` (vídeo → Login), `LoginActivity` (Auth + biometria + manter conectado), `RegisterActivity`, `ForgotPasswordActivity`.

### Activities que viram Fragments
| Activity atual | Fragment alvo | Conteúdo |
|---|---|---|
| `RankingActivity` (664 ln) | `HomeFragment` | Bar chart por situação, progresso "150 contas", slogan, imagens pódio, edição de crédito total (dialog) |
| `MainActivity2` (354 ln) | `ClientesFragment` | Lista (RecyclerView + `ClienteAdapter`), busca, filtros por status, swipe-to-refresh, exclusão |
| `MainActivity4` (1045 ln) | `FinancasFragment` | 12 cards de métricas, CRUD de despesas (dialogs), custo total, exportação Excel |
| `MainActivity` (342 ln) | `ClienteFormFragment` (modo criar) | Formulário completo do cliente |
| `EditCadastroActivity` (231 ln) | `ClienteFormFragment` (modo editar) | Mesmo formulário, carrega por `clienteId` |

### Código órfão / morto (confirmado por busca — todos removidos na Fase 7 ✅)
- `App.kt` — classe `Application` **não registrada** no manifest (o registrado é `IORITvApplication`). Contém init do **OneSignal** e agendamento do WorkManager → hoje OneSignal **não inicializa** (notificações quebradas). Mover o init do OneSignal para `IORITvApplication` preserva a intenção original.
- `DespesaActivity.kt` + `activity_despesa.xml` — não está no manifest, nunca lançada.
- `DespesasActivity.kt` + `activity_despesas.xml` — no manifest mas nunca lançada; grava com **schema errado** ("Data"/"Despesa"/"Valor" em vez de data/descricao/valor/dataTimestamp).
- `SettingsActivity.kt` — no manifest, nunca lançada (sem intent-filter, não exportada).

### Duplicação a eliminar durante a migração
- **Agendamento WorkManager**: `MainActivity.configurarWorkManager()`, `MainActivity2.configurarWorkManager()`, `App.kt.setupDailyWork()` — 3 cópias com nomes únicos diferentes ("UpdateWorker" vs "UpdateWork") → consolidar 1 único agendamento no `IORITvApplication`, nome único, `ExistingPeriodicWorkPolicy.KEEP`. Comportamento do `UpdateWorker` (cálculo de SITUACAO/VENCIMENTO) **inalterado**.
- **Menu de tema + logout**: copiado em RankingActivity, MainActivity2, MainActivity4 → 1 toolbar na Activity hospedeira.
- **Helpers de data**: `clearTime`, `calculateDaysDifference`, `calcularVencimento`, `calculateTerminoDate`, `getInitialDelay` duplicados em 7 arquivos → extrair para utilitários existentes (`SituacaoUtil`, `DateUtils` novo).
- **Cálculo de status**: `SituacaoUtil` + lógica inline no `UpdateWorker`, `MainActivity`, `ClienteAdapter`, `MainActivity4` → usar **sempre** `SituacaoUtil` (que já bate com as Cloud Functions). ⚠️ `MainActivity4.loadFinancialMetrics()` usa janelas divergentes (A VENCER 0..3, VENCIDO −15..−30) — alinhar à `SituacaoUtil` mantendo as mesmas categorias visuais e textos. **Não alterar** `functions/index.js`.

---

## 4. Arquitetura alvo

```
HomeActivity (nova)  ← substitui RankingActivity/MainActivity2/MainActivity4
├── MaterialToolbar (única; título por destino via OnDestinationChangedListener)
│   └── menu: Tema (theme_menu.xml) + Sair (toolbar_menu.xml)
├── FragmentContainerView (NavHostFragment)
│   ├── HomeFragment          (start destination — nav_home)
│   ├── ClientesFragment      (nav_cadastros)
│   ├── FinancasFragment      (nav_financas)
│   └── ClienteFormFragment   (destino de detalhe, com back)
└── BottomNavigationView (única; ids atuais nav_home/nav_cadastros/nav_financas)
```

- **Login/Splash** passam a apontar para `HomeActivity` (hoje apontam para `RankingActivity`).
- Nome `HomeActivity` evita conflito com a `MainActivity` atual (formulário), que será removida ao final.
- Nav graph com `app:launchSingleTop="true"` + `popUpTo` no start destination para o botão voltar sair do app em vez de ciclar abas (paridade com o comportamento atual de `finish()`).
- `ClienteFormFragment` recebe `clienteId` via argumento (null = criar). O fluxo "Liberar TV" do `ClienteAdapter` passa a navegar via nav controller em vez de `Intent`.

---

## 5. Fases da migração

Cada fase termina com o app **compilando e funcionando** (critério de aceite). Fazer em commits separados, uma aba por vez.

### Fase 0 — Preparação
- `git status` limpo, commit de baseline.
- Criar `HomeActivity` + `activity_home.xml` + `nav_graph.xml` vazios (apenas NavHostFragment + BottomNavigationView com os 3 itens).
- `SplashActivity`/`LoginActivity` continuam apontando para `RankingActivity` até a Fase 2 (nenhuma mudança de fluxo ainda).

### Fase 1 — Esqueleto single-activity com 1 aba — ✅ concluída
- `HomeFragment` vazio como start destination.
- Login redireciona para `HomeActivity`.
- **Critério:** app abre, mostra toolbar + aba home vazia, troca de tema e logout funcionam a partir da toolbar única.
- **Entregue:** `HomeActivity` + `activity_home.xml` + `nav_graph.xml` + `HomeFragment` placeholder; toolbar/menu/bottom nav únicos (o menu de tema/logout deixa de ser duplicado nas 3 abas). Login manual **e** biometria (bug #1) apontam para `HomeActivity`. Build OK.

### Fase 2 — HomeFragment (conteúdo da RankingActivity) — ✅ concluída
- Extrair o conteúdo de `activity_ranking_with_nav.xml` (abaixo da toolbar) para `fragment_home.xml` (ou usar ViewBinding do fragment).
- Mover: `setupBarChart`, `updateProgressAndClientes`, `updateRankingImages`, dialog de edição de créditos, leitura/escrita de `AppPreferences/totalCredit`.
- **Critério:** home idêntica à atual (gráfico, progresso, slogan, crédito). Comportamento de `onResume` (recarregar crédito) preservado em `onResume` do fragment.
- **Entregue:** gráfico, progresso /150, slogan, pódio, crédito total (dialog), mascote iorinho e loading shimmer migrados. Removidos `creditosASubtrair` (extra de intent que ninguém envia — código morto) e `optimizePerformance()` (definida e nunca chamada). Render verificado no device (78 contas, 52%, crédito 150).

### Fase 3 — ClientesFragment (conteúdo da MainActivity2) — ✅ concluída
- Mover lista/busca/filtros/swipe-refresh/exclusão; `ClienteAdapter` **inalterado**.
- Swipe-to-refresh: `SwipeRefreshLayout` pode viver dentro do fragment normalmente.
- **Critério:** busca, filtros, expandir card, "Liberar TV" (navega para o form) e exclusão funcionando.
- **Entregue:** lista/busca/filtros/swipe-refresh/exclusão portados; o listener de snapshot agora é removido no `onDestroyView` (a Activity não fazia — evita vazamento ao trocar de aba); sem agendamento de WorkManager no fragment. Testado no device (lista real + filtro ATIVO). FAB/"Liberar TV" ainda abriam Activities antigas até a Fase 5.

### Fase 4 — FinancasFragment (conteúdo da MainActivity4) — ✅ concluída
- Extrair `activity_main4.xml` (763 linhas) para `fragment_financas.xml` com os mesmos ids.
- Mover métricas, dialogs de despesa/custo, exportação Excel e acessibilidade dos cards.
- **Critério:** 12 cards corretos, CRUD de despesas, exportação Excel, `onResume` recarregando métricas (paridade com o atual).
- **Entregue:** 12 cards, CRUD de despesas, custo total, exportação Excel e acessibilidade; cálculos **idênticos** à MainActivity4 (janelas 0..3 / −15..−30 mantidas — mesma lógica do web-app). Testado no device (Margem de Lucro 14,43%, despesas reais, 0 crashes).
- **⚠️ Pós-migração (18/08/2026):** janelas **unificadas** com as Cloud Functions (fonte da verdade) — ATIVO ≥3, A VENCER 0..2, VENCIDO −14..−1, STANDBY ≤−15 — via `SituacaoUtil.calcularSituacao`; descrições dos cards atualizadas no layout.

### Fase 5 — ClienteFormFragment (merge de MainActivity + EditCadastroActivity) — ✅ concluída
- Um único formulário com modo via argumento `clienteId`.
- Cópia do `initialDate`/`adjustTotalCredits` (mexe em `AppPreferences/totalCredit`) preservada.
- **Critério:** criar cliente (volta para a lista) e editar cliente (fluxo "Liberar TV") idênticos.
- **Entregue:** formulário unificado (UI moderna da MainActivity, cards Material 3); argumento `clienteId` (null = criar); título dinâmico + seta voltar + bottom nav oculto na `HomeActivity`; FAB e "Liberar TV" navegam via nav controller (callback `onEditClick` no `ClienteAdapter`). Corrigido o crash NPE (bug #2). Testado no device: criar ("Adicionar Cadastro") e editar (dados de ALEX BRUNO carregados).

### Fase 6 — Centralização — ✅ concluída
- `IORITvApplication`: mover init do OneSignal (vindo do `App.kt` morto) + agendamento único do WorkManager (`ExistingPeriodicWorkPolicy.KEEP`, nome único "UpdateWorker").
- Remover chamadas duplicadas de WorkManager das Activities antigas.
- `SituacaoUtil` como única fonte de cálculo; remover lógicas inline; `DateUtils` para helpers de data.
- **Entregue:** OneSignal (bug #3) + agendamento único (bug #4) no `IORITvApplication`; cópias removidas; `App.kt` deletado. Verificado no device: OneSignal inicializando e **exatamente 1** job `UpdateWorker` ENQUEUED. ⚠️ A extração de `DateUtils`/unificação total em `SituacaoUtil` **não foi executada** — decisão consciente para preservar os números exatos das telas; permanece como melhoria futura junto com a decisão sobre janelas divergentes.
- **⚠️ Pós-migração (18/08/2026):** janelas de status **unificadas** em todas as frentes, com a Cloud Function `calculateSituacao` como fonte da verdade: `UpdateWorker` agora delega ao `SituacaoUtil` e gera strings de VENCIMENTO idênticas ao CF (sem mais "ping-pong" de escrita); `ClienteAdapter` corrigido (`>2` dias); web-app `clientStatus.ts` alinhado. Resta como melhoria futura apenas a extração de `DateUtils`.

### Fase 7 — Limpeza — ✅ concluída
- Remover do manifest: `RankingActivity`, `MainActivity2`, `MainActivity4`, `MainActivity`, `EditCadastroActivity`, `DespesasActivity`, `SettingsActivity`, `DespesaActivity` (não está no manifest).
- Deletar arquivos órfãos: `App.kt`, `DespesaActivity.kt`, `DespesasActivity.kt`, `SettingsActivity.kt` e layouts/menus sem uso (`activity_ranking_with_nav.xml`, `activity_main2.xml`, `activity_main4.xml`, `activity_main.xml`, `activity_edit_cadastro.xml`, `activity_settings.xml`, `activity_despesa.xml`, `activity_despesas.xml`, `item_financeiro.xml`, `loading_finances.xml`, `loading_ranking.xml`, `context_menu.xml`, `menu_item.xml` — validar cada um antes de remover).
- Manter: `item_cliente.xml` (usado pelo adapter), `fragment_biometric_prompt.xml`, `dialog_*.xml`.
- Renomear o app label/ícone não é escopo.
- **Entregue:** 9 `.kt` (8 Activities + `FinanceiroAdapter`), 12 layouts e 3 menus órfãos removidos (`menu_cliente`, `menu_despesa`, `popup_menu`); manifest com apenas as Activities vivas (Splash/Login/Register/Forgot/Home); temas `Theme.IORITv.MainActivity2` (light/dark) removidos; import órfão de `EditCadastroActivity` corrigido no `ClienteAdapter`. ⚠️ `loading_ranking.xml` **foi mantido** (o plano listava para remoção, mas o `fragment_home.xml` o inclui via `<include>` — validação da fase). Build OK + reinstalado no device: 3 abas, criar/editar e 0 crashes.

### Fase 8 — Regressão
- Smoke test manual: login → 3 abas → criar/editar cliente → despesas → exportar Excel → tema claro/escuro → logout → reabrir com sessão ativa.
- Comparar toasts, textos e estados com o app atual lado a lado.
- `./gradlew assembleDebug` sem erros; opcional: instalar no device.

---

## 6. Checklist de paridade de comportamento

- [x] Troca de abas não recria dados (melhoria) — mas o carregamento inicial de cada aba permanece igual.
- [x] `onResume` do Finanças recarrega custo total + métricas + despesas (portado; recarga preservada).
- [x] Crédito total (`AppPreferences/totalCredit`, default 150) lido/escrito nos mesmos momentos (150 visível na Home).
- [x] Sessão (`login_prefs` + biometria + manter conectado) inalterada (restaurada após reinstall no device).
- [x] OneSignal passa a inicializar (correção — ver bug #3).
- [~] Todos os toasts/textos/diálogos idênticos (portados linha a linha; não exaustivamente validados).
- [x] WorkManager roda 1x/dia com nome único (ver bug #4).
- [x] Botão voltar sai do app nas abas e volta do formulário (testado: form → lista → home).

---

## 7. Riscos e mitigações

| Risco | Mitigação |
|---|---|
| Extração do `activity_main4.xml` (763 ln) quebra ids usados no código | Manter os mesmos `@+id/` no `fragment_financas.xml`; usar ViewBinding do fragment; testar após a Fase 4 |
| Back stack do bottom nav vira "ciclo de abas" | `launchSingleTop` + `popUpTo(startDestination)` no nav graph |
| `MainActivity4` tem janelas de status divergentes | ✅ Resolvido (18/08/2026): janelas unificadas à `SituacaoUtil`/Cloud Functions em todas as frentes (Android, UpdateWorker, ClienteAdapter, web-app) |
| Duplo agendamento do WorkManager | Consolidar no `IORITvApplication` com nome único e `KEEP` |
| Regressão silenciosa (textos/estados) | Checklist da seção 6 + smoke test manual (app de 1 admin, sem suíte automatizada) |

**Riscos que se materializaram:** duplo agendamento do WorkManager (bug #4) e callback assíncrono disparando após a destruição da view (bug #2 — não previsto na tabela acima).

---

## 8. Registro de bugs encontrados e corrigidos

| # | Fase | Bug | Causa raiz | Correção |
|---|---|---|---|---|
| 1 | 1 | Login por biometria caía na `RankingActivity` antiga | `BiometricPromptFragment.onAuthenticationSucceeded` tinha o destino hardcoded — só o login manual havia sido redirecionado | Navegar para `HomeActivity`; encontrado em teste no device |
| 2 | 5 | Crash `NullPointerException` no `HomeFragment` ao trocar de aba com load do Firestore pendente | Callback assíncrono disparava depois da view ser destruída: com Activities a tela vivia para sempre; com Fragments a view morre na troca de aba | Guardas `if (_binding == null) return@...` em todos os callbacks assíncronos dos 3 fragments (Home, Finanças, Clientes) |
| 3 | 6 | OneSignal nunca inicializava (notificações quebradas) | `App.kt` (com o init) não era o Application registrado no manifest — o registrado era `IORITvApplication` | Init movido para `IORITvApplication.setupPushNotifications()`; verificado no device |
| 4 | 6 | WorkManager agendado com 3 nomes únicos diferentes ("UpdateWorker", "UpdateWork", "UpdateWorkerOnAppOpen") — risco de 2+ jobs/dia | Cópias duplicadas em `MainActivity`, `MainActivity2` e `App.kt` | Agendamento único no `IORITvApplication` (nome "UpdateWorker", `ExistingPeriodicWorkPolicy.KEEP`); diagnóstico no device mostra 1 job ENQUEUED |

**Mudança de comportamento intencional:** o "force-run" do `UpdateWorker` a cada abertura de tela (MainActivity2) foi removido — a atualização de status passou a depender do job diário + Cloud Functions (`onClienteWrite` + `checkSituacaoDaily`), que permanecem intactas.

---

## 9. Fora de escopo (deliberadamente)

- Firestore rules, web-app, Cloud Functions, migração de dados.
- Distribuição/assinatura de release, multi-usuário, permissões por papel.
- Redesign visual (pode ser fase futura — a migração preserva o visual atual; a arquitetura nova facilita redesenhar depois).
