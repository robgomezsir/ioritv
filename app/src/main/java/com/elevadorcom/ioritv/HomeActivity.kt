package com.elevadorcom.ioritv

import android.content.Intent
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
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
 *
 * Glass agora é uma SKIN OVERLAY aplicada sobre o tema base (claro ou escuro).
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplica o tema base usando ThemeUtils (light/dark)
        ThemeUtils.applyTheme(this)

        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isDark = ThemeUtils.isDarkTheme(this)
        val isGlass = ThemeUtils.isGlassEnabled(this)

        // ── Glass overlay: aurora + blur ──────────────────────────────
        if (isGlass) {
            binding.glassBlurLayer.visibility = View.VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                binding.glassBlurLayer.setRenderEffect(
                    RenderEffect.createBlurEffect(24f, 24f, Shader.TileMode.CLAMP)
                )
            }
            // Tornar o container de fragments transparente para o aurora brilhar através
            binding.navHostFragment.setBackgroundColor(Color.TRANSPARENT)
        }

        // Configurar a Toolbar + NavController
        setSupportActionBar(binding.toolbar)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
        setupActionBarWithNavController(navController)
        supportActionBar?.title = "Meu Ioritv"

        // ── Barras de sistema (status + navigation) ──────────────────
        val statusBarColor: Int
        val navBarColor: Int
        val lightBars: Boolean

        when {
            isGlass -> {
                statusBarColor = Color.TRANSPARENT
                navBarColor = Color.TRANSPARENT
                lightBars = false
            }
            isDark -> {
                statusBarColor = getColor(R.color.md_theme_dark_background)
                navBarColor = getColor(R.color.md_theme_dark_background)
                lightBars = false
            }
            else -> {
                statusBarColor = getColor(R.color.md_theme_light_background)
                navBarColor = getColor(R.color.md_theme_light_background)
                lightBars = true
            }
        }

        window.statusBarColor = statusBarColor
        window.navigationBarColor = navBarColor

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { ctrl ->
                ctrl.setSystemBarsAppearance(
                    if (lightBars) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
                ctrl.setSystemBarsAppearance(
                    if (lightBars) WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (window.decorView.systemUiVisibility).let {
                var flags = it
                if (lightBars) {
                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                    flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                }
                flags
            }
        }

        // ── Cores da toolbar + bottom nav (ULTIMA coisa — após todo setup) ──
        // setupActionBarWithNavController reseta o background — aplicamos AQUI no final.
        applyToolbarAndNavColors(isDark, isGlass)

        // Título dinâmico
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

    /**
     * Aplica cores na toolbar e bottom nav.
     * Usa ColorDrawable + postDelayed para sobrescrever definitivamente
     * o MaterialShapeDrawable + backgroundTint do Material3 Toolbar.
     */
    private fun applyToolbarAndNavColors(isDark: Boolean, isGlass: Boolean) {
        val toolbarColor: Int
        val bottomNavColor: Int

        when {
            isGlass && isDark -> {
                toolbarColor = getColor(R.color.glass_surface_container_high)
                bottomNavColor = getColor(R.color.glass_surface_container_high)
                binding.toolbar.setTitleTextColor(getColor(R.color.glass_on_surface))
                binding.toolbar.setSubtitleTextColor(getColor(R.color.glass_on_surface_variant))
                binding.toolbar.navigationIcon?.setTint(getColor(R.color.glass_on_surface))
                binding.bottomNavigation.background = getDrawable(R.drawable.bg_glass_bottom_nav)
                val glassItemColor = android.content.res.ColorStateList.valueOf(getColor(R.color.glass_on_surface_variant))
                binding.bottomNavigation.itemIconTintList = glassItemColor
                binding.bottomNavigation.itemTextColor = glassItemColor
            }
            isGlass && !isDark -> {
                toolbarColor = getColor(R.color.glass_surface_container_high)
                bottomNavColor = getColor(R.color.glass_surface_container_high)
                binding.toolbar.setTitleTextColor(getColor(R.color.glass_on_surface))
                binding.toolbar.setSubtitleTextColor(getColor(R.color.glass_on_surface_variant))
                binding.toolbar.navigationIcon?.setTint(getColor(R.color.glass_on_surface))
                binding.bottomNavigation.background = getDrawable(R.drawable.bg_glass_bottom_nav)
                val glassItemColor = android.content.res.ColorStateList.valueOf(getColor(R.color.glass_on_surface_variant))
                binding.bottomNavigation.itemIconTintList = glassItemColor
                binding.bottomNavigation.itemTextColor = glassItemColor
            }
            isDark -> {
                toolbarColor = getColor(R.color.md_theme_dark_background)
                bottomNavColor = getColor(R.color.md_theme_dark_background)
            }
            else -> {
                toolbarColor = getColor(R.color.md_theme_light_background)
                bottomNavColor = getColor(R.color.md_theme_light_background)
            }
        }

        // Forçar background: limpar tint + substituir drawable inteiro
        // Usar postDelayed(200ms) para rodar DEPOIS de todos os layout passes do Material3
        binding.toolbar.postDelayed({
            binding.toolbar.background = null
            binding.toolbar.backgroundTintList = null
            binding.toolbar.background = android.graphics.drawable.ColorDrawable(toolbarColor)
        }, 200)

        if (!isGlass) {
            binding.bottomNavigation.postDelayed({
                binding.bottomNavigation.background = null
                binding.bottomNavigation.backgroundTintList = null
                binding.bottomNavigation.background = android.graphics.drawable.ColorDrawable(bottomNavColor)
            }, 200)
        }
    }

    private var popupWindow: PopupWindow? = null

    private fun showSettingsMenu() {
        popupWindow?.dismiss()

        val menuView = layoutInflater.inflate(R.layout.menu_bottom_sheet, null)

        popupWindow = PopupWindow(
            menuView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
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
        val switchGlass = menuView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchGlass)

        // Estado atual
        val baseThemeMode = ThemeUtils.getSavedThemeMode(this)
        val glassEnabled = ThemeUtils.isGlassEnabled(this)
        val isDark = ThemeUtils.isDarkTheme(this)

        // Cores do menu baseado no tema (glass ou não)
        val bg = menuView.background as? android.graphics.drawable.GradientDrawable
        val textColor: Int
        val textSecondaryColor: Int
        val iconTint: Int
        val radioTint: Int
        val dividerColor: Int

        when {
            glassEnabled && isDark -> {
                bg?.setColor(0xCC252B2C.toInt())
                textColor = 0xFFDEE3E5.toInt()
                textSecondaryColor = 0xFFBFC8CA.toInt()
                iconTint = 0xFFBFC8CA.toInt()
                radioTint = 0xFF82D3E0.toInt()
                dividerColor = 0x45FFFFFF.toInt()
            }
            glassEnabled && !isDark -> {
                bg?.setColor(0xE6FFFFFF.toInt())
                textColor = 0xFF171D1E.toInt()
                textSecondaryColor = 0xFF3F484A.toInt()
                iconTint = 0xFF3F484A.toInt()
                radioTint = 0xFF006874.toInt()
                dividerColor = 0xFFDBE4E6.toInt()
            }
            isDark -> {
                bg?.setColor(getColor(R.color.md_theme_dark_surfaceContainerHigh))
                textColor = getColor(R.color.md_theme_dark_onSurface)
                textSecondaryColor = getColor(R.color.md_theme_dark_onSurfaceVariant)
                iconTint = getColor(R.color.md_theme_dark_onSurfaceVariant)
                radioTint = getColor(R.color.md_theme_dark_primary)
                dividerColor = getColor(R.color.md_theme_dark_outlineVariant)
            }
            else -> {
                bg?.setColor(getColor(R.color.md_theme_light_surfaceContainerHigh))
                textColor = getColor(R.color.md_theme_light_onSurface)
                textSecondaryColor = getColor(R.color.md_theme_light_onSurfaceVariant)
                iconTint = getColor(R.color.md_theme_light_onSurfaceVariant)
                radioTint = getColor(R.color.md_theme_light_primary)
                dividerColor = getColor(R.color.md_theme_light_outlineVariant)
            }
        }

        // Aplicar cores
        menuTitle.setTextColor(textColor)
        menuView.findViewById<TextView>(R.id.labelTheme).setTextColor(textSecondaryColor)
        menuView.findViewById<TextView>(R.id.radio_theme_auto).setTextColor(textColor)
        menuView.findViewById<TextView>(R.id.radio_theme_light).setTextColor(textColor)
        menuView.findViewById<TextView>(R.id.radio_theme_dark).setTextColor(textColor)

        // Tint ícones
        arrowTheme.setColorFilter(iconTint)
        menuView.findViewById<ImageView>(R.id.iconTheme).setColorFilter(iconTint)

        // Tint radio buttons
        for (i in 0 until themeGroup.childCount) {
            val child = themeGroup.getChildAt(i)
            if (child is android.widget.RadioButton) {
                child.buttonTintList = android.content.res.ColorStateList.valueOf(radioTint)
            }
        }

        // Dividers
        menuView.findViewById<View>(R.id.menuDivider).setBackgroundColor(dividerColor)
        menuView.findViewById<View>(R.id.menuDividerTop).setBackgroundColor(dividerColor)

        // Abrir submenu por padrão
        submenuTheme.visibility = View.VISIBLE
        arrowTheme.rotation = 90f

        // ── Marcar tema base atual no RadioGroup (apenas 3 opções) ──
        when (baseThemeMode) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> themeGroup.check(R.id.radio_theme_auto)
            AppCompatDelegate.MODE_NIGHT_NO -> themeGroup.check(R.id.radio_theme_light)
            AppCompatDelegate.MODE_NIGHT_YES -> themeGroup.check(R.id.radio_theme_dark)
            ThemeUtils.MODE_GLASS -> themeGroup.check(R.id.radio_theme_light) // migração: glass base era light
            else -> themeGroup.check(R.id.radio_theme_auto)
        }

        // ── Toggle Glass ──
        switchGlass.isChecked = glassEnabled
        switchGlass.thumbTintList = android.content.res.ColorStateList.valueOf(radioTint)

        // Tocar na row inteira alterna o switch
        menuView.findViewById<View>(R.id.glassToggleRow).setOnClickListener {
            switchGlass.isChecked = !switchGlass.isChecked
        }

        switchGlass.setOnCheckedChangeListener { _, isChecked ->
            ThemeUtils.setGlassEnabled(this, isChecked)
            recreate()
        }

        // ── Expansão/colapso do submenu ──
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

        // ── Listener para mudança de tema base ──
        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val themeMode = when (checkedId) {
                R.id.radio_theme_auto -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                R.id.radio_theme_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.radio_theme_dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> return@setOnCheckedChangeListener
            }
            ThemeUtils.saveThemeMode(this, themeMode)
            recreate()
        }

        popupWindow?.showAtLocation(binding.root, android.view.Gravity.CENTER, 0, 0)
    }

    private fun logout() {
        val sharedPreferences = getSharedPreferences("login_prefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
