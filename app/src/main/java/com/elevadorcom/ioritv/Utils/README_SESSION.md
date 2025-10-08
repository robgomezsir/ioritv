# SessionManager - Gerenciamento de Sessão de Usuário

## Descrição
O `SessionManager` é uma classe utilitária singleton que gerencia a persistência da sessão de autenticação do usuário no aplicativo IORI.TV.

## Funcionalidades

### 1. Persistência de Login
- Mantém o usuário logado mesmo após fechar e reabrir o app
- Sessão válida por 30 dias
- Sincronização automática com Firebase Authentication

### 2. Segurança
- Verifica se o email do usuário corresponde ao Firebase
- Valida se o email foi verificado
- Limpa automaticamente sessões expiradas
- Logout automático se houver inconsistências

### 3. Informações da Sessão
- Email do usuário
- ID do usuário (UID)
- Timestamp do último login
- Dias desde o último login

## Como Usar

### Inicializar o SessionManager
```kotlin
val sessionManager = SessionManager.getInstance(context)
```

### Salvar Login
```kotlin
// Após login bem-sucedido
sessionManager.saveLoginStatus()
```

### Verificar se está Logado
```kotlin
if (sessionManager.isUserLoggedIn()) {
    // Usuário está logado e sessão é válida
    navigateToMainScreen()
} else {
    // Usuário não está logado ou sessão expirou
    showLoginScreen()
}
```

### Fazer Logout
```kotlin
sessionManager.clearLoginStatus()
```

### Obter Informações do Usuário
```kotlin
val email = sessionManager.getUserEmail()
val userId = sessionManager.getUserId()
val daysSinceLogin = sessionManager.getDaysSinceLastLogin()
```

### Verificar Expiração Próxima
```kotlin
if (sessionManager.isSessionNearExpiry()) {
    // Alerta o usuário que a sessão expira em breve (últimos 3 dias)
    showSessionExpiryWarning()
}
```

## Uso em Outras Activities

Para adicionar verificação de sessão em outras Activities:

```kotlin
class MainActivity : AppCompatActivity() {
    
    private lateinit var sessionManager: SessionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sessionManager = SessionManager.getInstance(this)
        
        // Verifica se o usuário está logado
        if (!sessionManager.isUserLoggedIn()) {
            // Redireciona para tela de login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        // Continua com o fluxo normal
        setContentView(R.layout.activity_main)
    }
    
    private fun logout() {
        sessionManager.clearLoginStatus()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
```

## Configurações

### Alterar Timeout de Sessão
Edite a constante `SESSION_TIMEOUT` em `SessionManager.kt`:
```kotlin
private const val SESSION_TIMEOUT = 30L * 24 * 60 * 60 * 1000 // 30 dias
```

### Alterar Alerta de Expiração
Edite a função `isSessionNearExpiry()`:
```kotlin
fun isSessionNearExpiry(): Boolean {
    val daysSinceLogin = getDaysSinceLastLogin()
    return daysSinceLogin >= 27 // Alerta nos últimos 3 dias
}
```

## Notas Importantes

1. **Singleton Pattern**: O SessionManager usa o padrão Singleton para garantir uma única instância na aplicação

2. **Thread-Safe**: Implementação thread-safe usando `@Volatile` e `synchronized`

3. **Application Context**: Usa o application context para evitar memory leaks

4. **Firebase Sync**: Sempre sincroniza com o Firebase Authentication para garantir consistência

5. **Auto-Update**: Atualiza automaticamente o timestamp de último login quando verificado

## Troubleshooting

### Usuário sendo deslogado automaticamente
- Verifique se o email está verificado no Firebase
- Confirme que não passou 30 dias desde o último login
- Verifique se há consistência entre SharedPreferences e Firebase Auth

### Sessão não persiste
- Certifique-se de chamar `saveLoginStatus()` após login bem-sucedido
- Verifique permissões de armazenamento
- Confirme que o Firebase Auth está configurado corretamente

## Versão
- Versão atual: 4.2
- Data: 2025-10-08

