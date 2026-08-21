package com.elevadorcom.ioritv

import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.elevadorcom.ioritv.databinding.ActivityHomeBinding
import com.elevadorcom.ioritv.utils.ThemeUtils

/**
 * Activity hospedeira única (migração para Fragments).
 * Hospeda o NavHostFragment das abas (Home, Cadastros, Finanças) e a toolbar única
 * com menu de tema e logout — antes duplicados em RankingActivity/MainActivity2/MainActivity4.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica o tema apropriado usando ThemeUtils
        ThemeUtils.applyTheme(this)

        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Blur real do fundo Glass (RenderEffect, API 31+).
        // A camada de aurora recebe o blur e o conteúdo translúcido (toolbar, cards,
        // bottom nav) revela o vidro fosco. Abaixo da API 31 o fallback é o próprio
        // backdrop translúcido + blobs suaves (sem blur).
        if (ThemeUtils.getSavedThemeMode(this) == ThemeUtils.MODE_GLASS) {
            binding.glassBlurLayer.visibility = View.VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                binding.glassBlurLayer.setRenderEffect(
                    RenderEffect.createBlurEffect(24f, 24f, Shader.TileMode.CLAMP)
                )
            }
        }

        // Configurar a Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Meu Ioritv"

        // Conectar o Bottom Navigation e a ActionBar ao NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
        setupActionBarWithNavController(navController)

        // Título dinâmico: "Meu Ioritv" nas abas; "Adicionar/Editar Cadastro" no formulário.
        // No formulário o bottom nav é ocultado (paridade com as telas antigas de formulário).
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            when (destination.id) {
                R.id.nav_cliente_form -> {
                    supportActionBar?.title =
                        if (arguments?.getString("clienteId").isNullOrEmpty()) "Adicionar Cadastro" else "Editar Cadastro"
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> {
                    supportActionBar?.title = "Meu Ioritv"
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
            }
        }

        // Garantir o título correto na primeira exibição (destino inicial)
        supportActionBar?.title = "Meu Ioritv"
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_theme -> {
                showSettingsMenu()
                true
            }
            R.id.menu_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private var popupWindow: PopupWindow? = null

    private fun showSettingsMenu() {
        // Fechar popup anterior se existir
        popupWindow?.dismiss()

        // Inflar o layout do menu
        val menuView = layoutInflater.inflate(R.layout.menu_bottom_sheet, null)

        // Criar PopupWindow centralizado na tela
        popupWindow = PopupWindow(
            menuView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true // focusable
        ).apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            elevation = 24f
            isOutsideTouchable = true
        }

        // Referências
        val themeGroup = menuView.findViewById<RadioGroup>(R.id.themeRadioGroup)
        val menuItemTheme = menuView.findViewById<View>(R.id.menuItemTheme)
        val submenuTheme = menuView.findViewById<View>(R.id.submenuTheme)
        val arrowTheme = menuView.findViewById<ImageView>(R.id.arrowTheme)
        val menuTitle = menuView.findViewById<TextView>(R.id.menuTitle)

        // Cores do tema atual
        val isGlass = ThemeUtils.getSavedThemeMode(this) == ThemeUtils.MODE_GLASS
        val isDark = ThemeUtils.getSavedThemeMode(this) == AppCompatDelegate.MODE_NIGHT_YES || isGlass

        // Aplicar background e cores baseado no tema
        val bg = menuView.background as? android.graphics.drawable.GradientDrawable
        val textColor: Int
        val textSecondaryColor: Int
        val iconTint: Int
        val radioTint: Int
        val dividerColor: Int

        when {
            isGlass -> {
                // Glass: fundo translúcido escuro igual aos cards
                bg?.setColor(0xCC282D32.toInt()) // #282D32 com 80% alpha
                textColor = 0xFFF0F1F8.toInt() // glass_on_surface
                textSecondaryColor = 0xFFB0B4CC.toInt() // glass_on_surface_variant
                iconTint = 0xFFB0B4CC.toInt()
                radioTint = 0xFF8B9CF0.toInt() // glass_primary
                dividerColor = 0x33FFFFFF.toInt() // glass_outline_variant
            }
            isDark -> {
                // Escuro: fundo sólido escuro
                bg?.setColor(getColor(R.color.md_theme_dark_surfaceContainerHigh))
                textColor = getColor(R.color.md_theme_dark_onSurface)
                textSecondaryColor = getColor(R.color.md_theme_dark_onSurfaceVariant)
                iconTint = getColor(R.color.md_theme_dark_onSurfaceVariant)
                radioTint = getColor(R.color.md_theme_dark_primary)
                dividerColor = getColor(R.color.md_theme_dark_outlineVariant)
            }
            else -> {
                // Claro/Automático: fundo sólido claro
                bg?.setColor(getColor(R.color.md_theme_light_surfaceContainerHigh))
                textColor = getColor(R.color.md_theme_light_onSurface)
                textSecondaryColor = getColor(R.color.md_theme_light_onSurfaceVariant)
                iconTint = getColor(R.color.md_theme_light_onSurfaceVariant)
                radioTint = getColor(R.color.md_theme_light_primary)
                dividerColor = getColor(R.color.md_theme_light_outlineVariant)
            }
        }

        // Aplicar cores aos textos
        menuTitle.setTextColor(textColor)
        menuView.findViewById<TextView>(R.id.labelTheme).setTextColor(textSecondaryColor)
        menuView.findViewById<TextView>(R.id.radio_theme_auto).setTextColor(textColor)
        menuView.findViewById<TextView>(R.id.radio_theme_light).setTextColor(textColor)
        menuView.findViewById<TextView>(R.id.radio_theme_dark).setTextColor(textColor)
        menuView.findViewById<TextView>(R.id.radio_theme_glass).setTextColor(textColor)

        // Aplicar tint aos ícones
        arrowTheme.setColorFilter(iconTint)
        menuView.findViewById<ImageView>(R.id.iconTheme).setColorFilter(iconTint)

        // Aplicar tint aos radio buttons
        for (i in 0 until themeGroup.childCount) {
            val child = themeGroup.getChildAt(i)
            if (child is android.widget.RadioButton) {
                child.buttonTintList = android.content.res.ColorStateList.valueOf(radioTint)
            }
        }

        // Aplicar cor do divisor
        menuView.findViewById<View>(R.id.menuDivider).setBackgroundColor(dividerColor)
        menuView.findViewById<View>(R.id.menuDividerTop).setBackgroundColor(dividerColor)

        // Abrir submenu de temas por padrão
        submenuTheme.visibility = View.VISIBLE
        arrowTheme.rotation = 90f

        // Marcar o tema atual no RadioGroup
        val currentTheme = ThemeUtils.getSavedThemeMode(this)
        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> themeGroup.check(R.id.radio_theme_auto)
            AppCompatDelegate.MODE_NIGHT_NO -> themeGroup.check(R.id.radio_theme_light)
            AppCompatDelegate.MODE_NIGHT_YES -> themeGroup.check(R.id.radio_theme_dark)
            ThemeUtils.MODE_GLASS -> themeGroup.check(R.id.radio_theme_glass)
        }

        // Submenu: expansão/colapso da seção Aparência com animação
        menuItemTheme.setOnClickListener {
            if (submenuTheme.visibility == View.VISIBLE) {
                arrowTheme.animate().rotation(0f).setDuration(200).start()
                submenuTheme.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction {
                        submenuTheme.visibility = View.GONE
                        submenuTheme.alpha = 1f
                    }
                    .start()
            } else {
                submenuTheme.alpha = 0f
                submenuTheme.visibility = View.VISIBLE
                submenuTheme.animate().alpha(1f).setDuration(200).start()
                arrowTheme.animate().rotation(90f).setDuration(200).start()
            }
        }

        // Listener para mudança de tema
        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val themeMode = when (checkedId) {
                R.id.radio_theme_auto -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                R.id.radio_theme_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.radio_theme_dark -> AppCompatDelegate.MODE_NIGHT_YES
                R.id.radio_theme_glass -> ThemeUtils.MODE_GLASS
                else -> return@setOnCheckedChangeListener
            }
            ThemeUtils.saveThemeMode(this, themeMode)
            recreate()
        }

        // Mostrar popup centralizado na tela
        popupWindow?.showAtLocation(binding.root, android.view.Gravity.CENTER, 0, 0)
    }

    private fun logout() {
        // Limpa o estado de login nas SharedPreferences
        val sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        // Redireciona para a LoginActivity
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
