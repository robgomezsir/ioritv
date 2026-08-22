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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.elevadorcom.ioritv.databinding.ActivityHomeBinding
import com.elevadorcom.ioritv.utils.ThemeUtils

/**
 * Activity hospedeira única (migração para Fragments).
 *
 * API 35+ (Android 15+): edge-to-edge obrigatório.
 * Estratégia dupla:
 *   1. window.statusBarColor / navigationBarColor → cores do system-drawn background
 *   2. WindowInsets padding → toolbar/bottom nav se estendem atrás das barras
 *
 * Assim tanto o background do sistema quanto o conteúdo do app combinam.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var currentIsDark = false
    private var currentIsGlass = false

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentIsDark = ThemeUtils.isDarkTheme(this)
        currentIsGlass = ThemeUtils.isGlassEnabled(this)

        // ── Glass overlay: aurora + blur ──────────────────────────────
        if (currentIsGlass) {
            binding.glassBlurLayer.visibility = View.VISIBLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                binding.glassBlurLayer.setRenderEffect(
                    RenderEffect.createBlurEffect(24f, 24f, Shader.TileMode.CLAMP)
                )
            }
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

        // ── WindowInsets: toolbar sob status bar, bottom nav sob nav bar ──
        setupWindowInsets()

        // ── Aplicar cores (toolbar + system bars) ──
        applyAllColors()
        // Re-aplicar após layout do Material3
        binding.toolbar.post { applyAllColors() }
        binding.toolbar.postDelayed({ applyAllColors() }, 200)

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

    override fun onResume() {
        super.onResume()
        applyAllColors()
        binding.toolbar.postDelayed({ applyAllColors() }, 100)
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
     * Configura WindowInsets para toolbar e bottom nav se estenderem
     * por trás das barras de sistema.
     *
     * toolbar.paddingTop = statusBarHeight → background preenche status bar
     * bottomNav.paddingBottom = navBarHeight → background preenche nav bar
     */
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Toolbar: padding-top = status bar height
            binding.toolbar.setPadding(
                binding.toolbar.paddingLeft,
                systemBars.top,
                binding.toolbar.paddingRight,
                binding.toolbar.paddingBottom
            )

            // Bottom nav: padding-bottom = nav bar height
            binding.bottomNavigation.setPadding(
                binding.bottomNavigation.paddingLeft,
                binding.bottomNavigation.paddingTop,
                binding.bottomNavigation.paddingRight,
                systemBars.bottom
            )

            insets
        }
    }

    /**
     * Aplica TODAS as cores: toolbar background, bottom nav, system bars.
     *
     * Estratégia dupla para API 35+:
     *   1. window.statusBarColor → cor do background desenhado pelo sistema
     *   2. Toolbar ColorDrawable → cor do conteúdo desenhado pelo app atrás da barra
     *   Ambas devem ser iguais para criar a extensão visual perfeita.
     */
    private fun applyAllColors() {
        val isDark = currentIsDark
        val isGlass = currentIsGlass

        val toolbarColor: Int
        val bottomNavColor: Int
        val lightBars: Boolean

        when {
            isGlass && isDark -> {
                toolbarColor = Color.TRANSPARENT
                bottomNavColor = Color.TRANSPARENT
                lightBars = false

                binding.toolbar.setTitleTextColor(getColor(R.color.glass_on_surface))
                binding.toolbar.setSubtitleTextColor(getColor(R.color.glass_on_surface_variant))
                binding.toolbar.navigationIcon?.setTint(getColor(R.color.glass_on_surface))
                binding.bottomNavigation.background = getDrawable(R.drawable.bg_glass_bottom_nav)
                val glassItemColor = android.content.res.ColorStateList.valueOf(getColor(R.color.glass_on_surface_variant))
                binding.bottomNavigation.itemIconTintList = glassItemColor
                binding.bottomNavigation.itemTextColor = glassItemColor
            }
            isGlass && !isDark -> {
                toolbarColor = Color.TRANSPARENT
                bottomNavColor = Color.TRANSPARENT
                lightBars = true

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
                lightBars = false

                binding.bottomNavigation.background = null
                binding.bottomNavigation.backgroundTintList = android.content.res.ColorStateList.valueOf(bottomNavColor)
                val itemColor = android.content.res.ColorStateList.valueOf(getColor(R.color.md_theme_dark_onSurfaceVariant))
                binding.bottomNavigation.itemIconTintList = itemColor
                binding.bottomNavigation.itemTextColor = itemColor
            }
            else -> {
                toolbarColor = getColor(R.color.md_theme_light_background)
                bottomNavColor = getColor(R.color.md_theme_light_background)
                lightBars = true

                binding.bottomNavigation.background = null
                binding.bottomNavigation.backgroundTintList = android.content.res.ColorStateList.valueOf(bottomNavColor)
                val itemColor = android.content.res.ColorStateList.valueOf(getColor(R.color.md_theme_light_onSurfaceVariant))
                binding.bottomNavigation.itemIconTintList = itemColor
                binding.bottomNavigation.itemTextColor = itemColor
            }
        }

        // ── Toolbar: ColorDrawable que preenche atrás da status bar ──
        binding.toolbar.background = null
        binding.toolbar.backgroundTintList = null
        binding.toolbar.background = ColorDrawable(toolbarColor)

        // ── Bottom nav (não-glass): ColorDrawable ──
        if (!isGlass) {
            binding.bottomNavigation.background = null
            binding.bottomNavigation.backgroundTintList = null
            binding.bottomNavigation.background = ColorDrawable(bottomNavColor)
        }

        // ══════════════════════════════════════════════════════════
        // SYSTEM BARS — Estratégia dupla:
        // 1. window.statusBarColor/navigationBarColor: cor do background
        //    que o sistema desenha (FORCE_DRAW_STATUS_BAR_BACKGROUND)
        // 2. WindowInsets padding: o conteúdo do app se estende atrás
        //    das barras transparentes
        //
        // Na API < 35: apenas o #1 funciona (barras não são transparentes)
        // Na API 35+: ambos funcionam juntos
        // ══════════════════════════════════════════════════════════
        @Suppress("DEPRECATION")
        window.statusBarColor = toolbarColor
        @Suppress("DEPRECATION")
        window.navigationBarColor = bottomNavColor

        // ── Aparência dos ícones das barras ──
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
            var flags = window.decorView.systemUiVisibility
            if (lightBars) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
            window.decorView.systemUiVisibility = flags
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

        val themeGroup = menuView.findViewById<RadioGroup>(R.id.themeRadioGroup)
        val menuItemTheme = menuView.findViewById<View>(R.id.menuItemTheme)
        val submenuTheme = menuView.findViewById<View>(R.id.submenuTheme)
        val arrowTheme = menuView.findViewById<ImageView>(R.id.arrowTheme)
        val menuTitle = menuView.findViewById<TextView>(R.id.menuTitle)
        val switchGlass = menuView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchGlass)

        val baseThemeMode = ThemeUtils.getSavedThemeMode(this)
        val glassEnabled = ThemeUtils.isGlassEnabled(this)
        val isDark = ThemeUtils.isDarkTheme(this)

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

        menuTitle.setTextColor(textColor)
        menuView.findViewById<TextView>(R.id.labelTheme).setTextColor(textSecondaryColor)
        menuView.findViewById<TextView>(R.id.radio_theme_auto).setTextColor(textColor)
        menuView.findViewById<TextView>(R.id.radio_theme_light).setTextColor(textColor)
        menuView.findViewById<TextView>(R.id.radio_theme_dark).setTextColor(textColor)

        arrowTheme.setColorFilter(iconTint)
        menuView.findViewById<ImageView>(R.id.iconTheme).setColorFilter(iconTint)

        for (i in 0 until themeGroup.childCount) {
            val child = themeGroup.getChildAt(i)
            if (child is android.widget.RadioButton) {
                child.buttonTintList = android.content.res.ColorStateList.valueOf(radioTint)
            }
        }

        menuView.findViewById<View>(R.id.menuDivider).setBackgroundColor(dividerColor)
        menuView.findViewById<View>(R.id.menuDividerTop).setBackgroundColor(dividerColor)

        submenuTheme.visibility = View.VISIBLE
        arrowTheme.rotation = 90f

        when (baseThemeMode) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> themeGroup.check(R.id.radio_theme_auto)
            AppCompatDelegate.MODE_NIGHT_NO -> themeGroup.check(R.id.radio_theme_light)
            AppCompatDelegate.MODE_NIGHT_YES -> themeGroup.check(R.id.radio_theme_dark)
            ThemeUtils.MODE_GLASS -> themeGroup.check(R.id.radio_theme_light)
            else -> themeGroup.check(R.id.radio_theme_auto)
        }

        switchGlass.isChecked = glassEnabled
        switchGlass.thumbTintList = android.content.res.ColorStateList.valueOf(radioTint)

        menuView.findViewById<View>(R.id.glassToggleRow).setOnClickListener {
            switchGlass.isChecked = !switchGlass.isChecked
        }

        switchGlass.setOnCheckedChangeListener { _, isChecked ->
            ThemeUtils.setGlassEnabled(this, isChecked)
            recreate()
        }

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
