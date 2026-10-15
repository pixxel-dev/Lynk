package ru.doGood.Lynk.feature.dashboard

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.regex.Pattern

/**
 * Статичная системная врезка в статус-бар Flyme Auto (Display 1001 / Default).
 * Не плавает поверх контента, а жестко фиксируется в верхней кромке экрана.
 */
object FlymeStatusBarDockManager {

    private const val PREFS_NAME = "flyme_statusbar_dock_prefs"
    private const val KEY_ENABLED = "dock_enabled"
    private const val KEY_POSITION = "dock_position" // 0 = Справа, 1 = По центру, 2 = Слева
    private const val KEY_OFFSET_X = "dock_offset_x"

    private const val DISPLAY_CENTER_MAIN = 1001
    private const val DISPLAY_PASSENGER_PSD = 1002

    private var dockView: View? = null
    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isImmersed = false

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val focusTaskPattern = Pattern.compile("u(\\d+)\\s+([a-zA-Z0-9_.]+)/([a-zA-Z0-9_.$]+)")

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }

    fun getPosition(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_POSITION, 0)
    }

    fun getOffsetX(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_OFFSET_X, 220)
    }

    fun setPosition(context: Context, position: Int, offsetXDp: Int = getOffsetX(context)) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_POSITION, position)
            .putInt(KEY_OFFSET_X, offsetXDp)
            .apply()
        if (isShowing()) {
            showDock(context)
        }
    }

    fun isShowing(): Boolean = dockView != null

    fun toggleDock(context: Context, enable: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enable)
            .apply()
        if (enable) {
            showDock(context)
        } else {
            hideDock()
        }
    }

    fun showDock(context: Context) {
        val appContext = context.applicationContext
        if (!Settings.canDrawOverlays(appContext)) {
            Toast.makeText(
                appContext,
                "Выдайте разрешение «Поверх других окон» (SYSTEM_ALERT_WINDOW)",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        hideDock()

        val targetContext = resolveCenterDisplayContext(appContext)
        val wm = targetContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val pos = getPosition(appContext)
        val offsetX = dp(targetContext, getOffsetX(appContext))

        val gravity = when (pos) {
            1 -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
            2 -> Gravity.TOP or Gravity.START
            else -> Gravity.TOP or Gravity.END
        }

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            dp(targetContext, 34),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            this.x = if (pos == 1) 0 else offsetX
            this.y = dp(targetContext, 4)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val barView = buildDockBarView(targetContext)
        runCatching {
            wm.addView(barView, params)
            dockView = barView
            layoutParams = params
        }
    }

    fun hideDock() {
        val view = dockView ?: return
        runCatching {
            windowManager?.removeViewImmediate(view)
        }
        dockView = null
        layoutParams = null
    }

    private fun buildDockBarView(context: Context): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 6), dp(context, 2), dp(context, 6), dp(context, 2))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(context, 17).toFloat()
                // Полупрозрачный графитовый цвет под штатный статус-бар Flyme Auto
                setColor(Color.parseColor("#CC181A20"))
                setStroke(dp(context, 1), Color.parseColor("#4DFFFFFF"))
            }
        }

        // Кнопка 1: Полный экран (Immersive Mode)
        val btnFullscreen = createActionButton(context, "⛶ Экран") { btn ->
            toggleFullscreenMode(btn)
        }
        // Долгое нажатие меняет позицию врезки в статус-баре (Справа -> Центр -> Слева)
        btnFullscreen.setOnLongClickListener {
            val nextPos = (getPosition(context) + 1) % 3
            setPosition(context, nextPos)
            true
        }

        // Разделитель
        val divider1 = createDivider(context)

        // Кнопка 2: Переброс на экран пассажира (1001 <-> 1002)
        val btnSwapScreen = createActionButton(context, "⇄ 1002") {
            swapActiveAppBetweenDisplays()
        }

        // Разделитель
        val divider2 = createDivider(context)

        // Кнопка 3: Домой
        val btnHome = createActionButton(context, "⌂") {
            executor.execute {
                val userId = resolveDriverUserId()
                execPrivileged("input keyevent 3 || am start --user $userId -a android.intent.action.MAIN -c android.intent.category.HOME")
            }
        }

        container.addView(btnFullscreen)
        container.addView(divider1)
        container.addView(btnSwapScreen)
        container.addView(divider2)
        container.addView(btnHome)

        return container
    }

    private fun createActionButton(
        context: Context,
        label: String,
        onClick: (TextView) -> Unit
    ): TextView {
        return TextView(context).apply {
            text = label
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(context, 10), dp(context, 2), dp(context, 10), dp(context, 2))
            setOnClickListener { onClick(this) }
        }
    }

    private fun createDivider(context: Context): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(context, 1), dp(context, 14)).apply {
                marginStart = dp(context, 2)
                marginEnd = dp(context, 2)
            }
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
        }
    }

    /**
     * Переключает принудительный полноэкранный режим для всех или активного приложения в Flyme Auto.
     */
    private fun toggleFullscreenMode(buttonView: TextView) {
        executor.execute {
            isImmersed = !isImmersed
            if (isImmersed) {
                // Включаем скрытие системных панелей для текущего окна и глобальную политику Immersive
                execPrivileged(
                    "settings put global policy_control immersive.full=*;" +
                            "cmd window set-ignore-orientation-request true"
                )
            } else {
                // Возвращаем штатный вид баров
                execPrivileged(
                    "settings put global policy_control null*;" +
                            "wm overscan reset"
                )
            }

            mainHandler.post {
                buttonView.text = if (isImmersed) "❐ Обычный" else "⛶ Экран"
                buttonView.setTextColor(
                    if (isImmersed) Color.parseColor("#4FC3F7") else Color.WHITE
                )
            }
        }
    }

    /**
     * Перебрасывает текущее открытое приложение между центральным экраном (1001)
     * и экраном переднего пассажира PSD (1002).
     */
    private fun swapActiveAppBetweenDisplays() {
        executor.execute {
            val userId = resolveDriverUserId()
            val dumpsys = execPrivileged("dumpsys window displays")

            val matcher = focusTaskPattern.matcher(dumpsys)
            var targetPackage: String? = null
            var targetActivity: String? = null

            while (matcher.find()) {
                val pkg = matcher.group(2) ?: continue
                if (!pkg.startsWith("com.flyme.auto.launcher") &&
                    !pkg.startsWith("com.flyme.auto.wallpapercontrol") &&
                    !pkg.equals("ru.doGood.Lynk") &&
                    !pkg.equals("com.android.systemui")
                ) {
                    targetPackage = pkg
                    targetActivity = matcher.group(3)
                    break
                }
            }

            if (targetPackage != null && targetActivity != null) {
                val onPassenger = dumpsys.contains("displayId=1002") && dumpsys.contains(targetPackage)
                val nextDisplay = if (onPassenger) DISPLAY_CENTER_MAIN else DISPLAY_PASSENGER_PSD
                val component = "$targetPackage/$targetActivity"
                execPrivileged(
                    "am start --user $userId --display $nextDisplay --activity-reorder-to-front -n $component"
                )
            }
        }
    }

    /**
     * Привязывает врезку к центральному экрану водителя (Display 1001 или DEFAULT_DISPLAY).
     */
    private fun resolveCenterDisplayContext(context: Context): Context {
        return runCatching {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val centerDisplay = dm.displays.firstOrNull { it.displayId == DISPLAY_CENTER_MAIN }
                ?: dm.getDisplay(Display.DEFAULT_DISPLAY)
            if (centerDisplay != null) context.createDisplayContext(centerDisplay) else context
        }.getOrDefault(context)
    }

    private fun execPrivileged(command: String): String {
        var process: Process? = null
        val output = StringBuilder()
        try {
            val shizukuProc = runCatching {
                val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
                val ping = shizukuClass.getMethod("pingBinder").invoke(null) as? Boolean ?: false
                val perm = shizukuClass.getMethod("checkSelfPermission").invoke(null) as? Int
                    ?: PackageManager.PERMISSION_DENIED
                if (ping && perm == PackageManager.PERMISSION_GRANTED) {
                    val newProc = shizukuClass.getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    ).apply { isAccessible = true }
                    newProc.invoke(null, arrayOf("sh", "-c", command), null, null) as? Process
                } else null
            }.getOrNull()

            process = shizukuProc ?: Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
            }
            process.waitFor()
        } catch (_: Exception) {
        } finally {
            process?.destroy()
        }
        return output.toString()
    }

    private fun dp(context: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}