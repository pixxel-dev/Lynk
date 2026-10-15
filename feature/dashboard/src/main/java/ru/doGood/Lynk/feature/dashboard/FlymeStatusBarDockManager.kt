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
import android.util.Log
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

    private const val TAG = "FlymeStatusBarDock"
    private const val PREFS_NAME = "flyme_statusbar_dock_prefs"
    private const val KEY_ENABLED = "dock_enabled"
    private const val KEY_POSITION = "dock_position" // 0 = Справа, 1 = По центру, 2 = Слева
    private const val KEY_SCREEN_POSITION = "dock_screen_position" // POSITION_TOP or POSITION_BOTTOM
    private const val KEY_OFFSET_X = "dock_offset_x"

    const val POSITION_TOP = "TOP"
    const val POSITION_BOTTOM = "BOTTOM"

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
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking isEnabled", e)
            false
        }
    }

    fun getPosition(context: Context): Int {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_POSITION, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting position", e)
            0
        }
    }

    fun getScreenPosition(context: Context): String {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SCREEN_POSITION, POSITION_TOP) ?: POSITION_TOP
        } catch (e: Exception) {
            Log.e(TAG, "Error getting screen position", e)
            POSITION_TOP
        }
    }

    fun getOffsetX(context: Context): Int {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_OFFSET_X, 220)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting offsetX", e)
            220
        }
    }

    fun setPosition(context: Context, screenPosition: String) {
        try {
            val validPosition = if (screenPosition == POSITION_BOTTOM) POSITION_BOTTOM else POSITION_TOP
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_SCREEN_POSITION, validPosition)
                .apply()
            if (isShowing()) {
                updateDockPosition(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting screen position", e)
        }
    }

    fun setPosition(context: Context, position: Int, offsetXDp: Int = getOffsetX(context)) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putInt(KEY_POSITION, position)
                .putInt(KEY_OFFSET_X, offsetXDp)
                .apply()
            if (isShowing()) {
                updateDockPosition(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting position", e)
        }
    }

    fun isShowing(): Boolean {
        return try {
            dockView != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking isShowing", e)
            false
        }
    }

    fun toggleDock(context: Context, enable: Boolean) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ENABLED, enable)
                .apply()
            if (enable) {
                showDock(context)
            } else {
                hideDock()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling dock", e)
        }
    }

    fun updateDockPosition(context: Context) {
        try {
            val view = dockView ?: run {
                showDock(context)
                return
            }
            val params = layoutParams ?: run {
                showDock(context)
                return
            }
            val wm = windowManager ?: run {
                showDock(context)
                return
            }

            val appContext = context.applicationContext
            val targetContext = resolveCenterDisplayContext(appContext)
            val pos = getPosition(appContext)
            val screenPos = getScreenPosition(appContext)
            val offsetX = dp(targetContext, getOffsetX(appContext))

            val verticalGravity = if (screenPos == POSITION_BOTTOM) Gravity.BOTTOM else Gravity.TOP
            val horizontalGravity = when (pos) {
                1 -> Gravity.CENTER_HORIZONTAL
                2 -> Gravity.START
                else -> Gravity.END
            }

            params.gravity = if (pos == 1) {
                verticalGravity or Gravity.CENTER_HORIZONTAL
            } else {
                verticalGravity or horizontalGravity
            }
            params.x = if (pos == 1) 0 else offsetX
            params.y = dpToPx(targetContext, 40)

            runCatching {
                wm.updateViewLayout(view, params)
            }.onFailure { e ->
                Log.e(TAG, "Failed to update view layout, recreating dock", e)
                showDock(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateDockPosition", e)
            showDock(context)
        }
    }

    fun showDock(context: Context) {
        try {
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
            val wm = targetContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (wm == null) {
                Log.e(TAG, "WindowManager service unavailable")
                return
            }
            windowManager = wm

            val pos = getPosition(appContext)
            val screenPos = getScreenPosition(appContext)
            val offsetX = dp(targetContext, getOffsetX(appContext))

            val verticalGravity = if (screenPos == POSITION_BOTTOM) Gravity.BOTTOM else Gravity.TOP
            val horizontalGravity = when (pos) {
                1 -> Gravity.CENTER_HORIZONTAL
                2 -> Gravity.START
                else -> Gravity.END
            }

            val gravity = if (pos == 1) {
                verticalGravity or Gravity.CENTER_HORIZONTAL
            } else {
                verticalGravity or horizontalGravity
            }

            val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                this.gravity = gravity
                this.x = if (pos == 1) 0 else offsetX
                this.y = dpToPx(targetContext, 40)
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
            }.onFailure { e ->
                Log.e(TAG, "Failed to add dock view to WindowManager", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in showDock", e)
        }
    }

    fun hideDock() {
        try {
            val view = dockView ?: return
            runCatching {
                windowManager?.removeViewImmediate(view)
            }.onFailure { e ->
                Log.e(TAG, "Failed to remove dock view from WindowManager", e)
            }
            dockView = null
            layoutParams = null
        } catch (e: Exception) {
            Log.e(TAG, "Error in hideDock", e)
        }
    }

    private fun buildDockBarView(context: Context): View {
        return try {
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(context, 8), dp(context, 3), dp(context, 8), dp(context, 3))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(context, 22).toFloat()
                    setColor(Color.parseColor("#CC181A20"))
                    setStroke(dp(context, 1), Color.parseColor("#4DFFFFFF"))
                }
                setOnTouchListener { _, event ->
                    Log.d(TAG, "Touch on dock container: ${event.action}")
                    false
                }
            }

            val btnFullscreen = createActionButton(context, "⛶ Экран") { btn ->
                toggleFullscreenMode(btn)
            }
            btnFullscreen.setOnLongClickListener {
                try {
                    val nextPos = (getPosition(context) + 1) % 3
                    setPosition(context, nextPos)
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling long click on fullscreen button", e)
                }
                true
            }

            val divider1 = createDivider(context)

            val btnSwapScreen = createActionButton(context, "⇄ 1002") {
                swapActiveAppBetweenDisplays()
            }

            val divider2 = createDivider(context)

            val btnHome = createActionButton(context, "⌂") {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                        addCategory(android.content.Intent.CATEGORY_HOME)
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Home fail", e)
                }
            }

            container.addView(btnFullscreen)
            container.addView(divider1)
            container.addView(btnSwapScreen)
            container.addView(divider2)
            container.addView(btnHome)

            container
        } catch (e: Exception) {
            Log.e(TAG, "Error building dock bar view", e)
            View(context)
        }
    }

    private fun createActionButton(
        context: Context,
        label: String,
        onClick: (TextView) -> Unit
    ): TextView {
        return try {
            TextView(context).apply {
                text = label
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.6f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(dp(context, 13), dp(context, 3), dp(context, 13), dp(context, 3))
                isClickable = true
                isFocusable = true
                setOnTouchListener { _, event ->
                    Log.d(TAG, "Touch on action button '$label': ${event.action}")
                    false
                }
                setOnClickListener {
                    try {
                        onClick(this)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in action button onClick", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating action button", e)
            TextView(context)
        }
    }

    private fun createDivider(context: Context): View {
        return try {
            View(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(context, 1), dp(context, 18)).apply {
                    marginStart = dp(context, 3)
                    marginEnd = dp(context, 3)
                }
                setBackgroundColor(Color.parseColor("#33FFFFFF"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating divider view", e)
            View(context)
        }
    }

    private fun toggleFullscreenMode(buttonView: TextView) {
        Log.d(TAG, "toggleFullscreenMode called")
        try {
            executor.execute {
                try {
                    isImmersed = !isImmersed
                    if (isImmersed) {
                        execPrivileged(
                            "settings put global policy_control immersive.full=*;" +
                                    "cmd window set-ignore-orientation-request true"
                        )
                    } else {
                        execPrivileged(
                            "settings put global policy_control null*;" +
                                    "wm overscan reset"
                        )
                    }

                    mainHandler.post {
                        try {
                            buttonView.text = if (isImmersed) "❐ Обычный" else "⛶ Экран"
                            buttonView.setTextColor(
                                if (isImmersed) Color.parseColor("#4FC3F7") else Color.WHITE
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating button UI after fullscreen toggle", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in toggleFullscreenMode thread", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting toggleFullscreenMode task", e)
        }
    }

    private fun swapActiveAppBetweenDisplays() {
        Log.d(TAG, "swapActiveAppBetweenDisplays called")
        try {
            executor.execute {
                try {
                    val userId = resolveDriverUserId()
                    val dumpsys = execPrivileged("dumpsys window displays")

                    val matcher = focusTaskPattern.matcher(dumpsys)
                    var targetPackage: String? = null
                    var targetActivity: String? = null

                    while (matcher.find()) {
                        val pkg = matcher.group(2) ?: continue
                        if (!pkg.startsWith("com.flyme.auto.launcher") &&
                            !pkg.startsWith("com.flyme.auto.wallpapercontrol") &&
                            pkg != "ru.doGood.Lynk" &&
                            pkg != "com.android.systemui"
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
                } catch (e: Exception) {
                    Log.e(TAG, "Error in swapActiveAppBetweenDisplays thread", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting swapActiveApp task", e)
        }
    }

    private fun resolveCenterDisplayContext(context: Context): Context {
        return try {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val centerDisplay = dm?.displays?.firstOrNull { it.displayId == DISPLAY_CENTER_MAIN }
                ?: dm?.getDisplay(Display.DEFAULT_DISPLAY)
            if (centerDisplay != null) context.createDisplayContext(centerDisplay) else context
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving center display context", e)
            context
        }
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
        } catch (e: Exception) {
            Log.e(TAG, "Error executing privileged command: $command", e)
        } finally {
            try {
                process?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying process", e)
            }
        }
        return output.toString()
    }

    private fun dp(context: Context, value: Int): Int {
        return try {
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value.toFloat(),
                context.resources.displayMetrics
            ).toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating dp value", e)
            value * 2
        }
    }

    private fun dpToPx(context: Context, value: Int): Int = dp(context, value)
}
