package ru.doGood.Lynk.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ru.doGood.Lynk.R
import java.util.Collections
import kotlin.math.max
import kotlin.math.min

class ForegroundOverlayService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val CHANNEL_ID = "OverlayServiceChannel"
        const val SECONDARY_DISPLAY_ID = 1003
    }

    private lateinit var defaultWindowManager: WindowManager
    private var secondaryWindowManager: WindowManager? = null
    private var secondaryContext: Context? = null

    private lateinit var prefs: SharedPreferences

    private class ProfileOverlayViews(val profileId: Int) {
        var quickLaunchView: View? = null
        var quickLaunchParams: WindowManager.LayoutParams? = null
        var quickLaunchViewSecondary: View? = null
        var quickLaunchParamsSecondary: WindowManager.LayoutParams? = null

        var fullscreenToggleView: View? = null
        var fullscreenToggleParams: WindowManager.LayoutParams? = null
        var fullscreenToggleViewSecondary: View? = null
        var fullscreenToggleParamsSecondary: WindowManager.LayoutParams? = null

        var homeView: View? = null
        var homeParams: WindowManager.LayoutParams? = null
        var homeViewSecondary: View? = null
        var homeParamsSecondary: WindowManager.LayoutParams? = null

        var backView: View? = null
        var backParams: WindowManager.LayoutParams? = null
        var backViewSecondary: View? = null
        var backParamsSecondary: WindowManager.LayoutParams? = null

        var refreshView: View? = null
        var refreshParams: WindowManager.LayoutParams? = null
        var refreshViewSecondary: View? = null
        var refreshParamsSecondary: WindowManager.LayoutParams? = null

        var freeformView: View? = null
        var freeformParams: WindowManager.LayoutParams? = null
        var freeformViewSecondary: View? = null
        var freeformParamsSecondary: WindowManager.LayoutParams? = null

        var combinedView: LinearLayout? = null
        var combinedParams: WindowManager.LayoutParams? = null
        var combinedViewSecondary: LinearLayout? = null
        var combinedParamsSecondary: WindowManager.LayoutParams? = null
    }

    private val profileViewsMap = HashMap<Int, ProfileOverlayViews>()

    private var freeformContainerView: View? = null
    private var freeformContainerParams: WindowManager.LayoutParams? = null

    private var isTargetAppFullscreen = false
    private var currentForegroundPackage = ""
    private var activeFullscreenPackage = ""

    private var handler: Handler? = null
    private var packageCheckerRunnable: Runnable? = null
    private var usageStatsManager: UsageStatsManager? = null
    private var activeSettingsDialogView: View? = null

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
    ).toInt()

    private val overlayUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "ru.doGood.Lynk.ACTION_UPDATE_OVERLAY") {
                updateOverlayButtons()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        currentForegroundPackage = packageName
        createNotificationChannel()

        startForegroundServiceInternal()

        defaultWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupSecondaryDisplayContext()

        prefs = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(this)

        val filter = IntentFilter("ru.doGood.Lynk.ACTION_UPDATE_OVERLAY")
        ContextCompat.registerReceiver(this, overlayUpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        handler = Handler(Looper.getMainLooper())
        setupPackageChecker()

        updateOverlayButtons()
    }

    private fun startForegroundServiceInternal() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lynk Floating Service")
            .setContentText("Служба плавающих кнопок активна")
            .setSmallIcon(R.drawable.ic_menu)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceInternal()
        updateOverlayButtons()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupSecondaryDisplayContext() {
        val displayManager = getSystemService(DISPLAY_SERVICE) as? DisplayManager
        val secondaryDisplay = displayManager?.getDisplay(SECONDARY_DISPLAY_ID)
        if (secondaryDisplay != null) {
            val displayContext = createDisplayContext(secondaryDisplay)
            secondaryContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                displayContext.createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
            } else {
                displayContext
            }
            secondaryWindowManager = secondaryContext?.getSystemService(WINDOW_SERVICE) as? WindowManager
        } else {
            secondaryContext = this
            secondaryWindowManager = defaultWindowManager
        }
    }

    private fun setupPackageChecker() {
        packageCheckerRunnable = object : Runnable {
            override fun run() {
                checkForegroundApp()
                handler?.postDelayed(this, 1000)
            }
        }
        handler?.post(packageCheckerRunnable!!)
    }

    private fun checkForegroundApp() {
        val usm = usageStatsManager ?: return
        val time = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 3000, time)
        val sortedStats = stats?.sortedByDescending { it.lastTimeUsed }
        val newPackage = sortedStats?.firstOrNull()?.packageName ?: return

        if (newPackage != currentForegroundPackage) {
            if (activeFullscreenPackage.isNotEmpty() && newPackage != activeFullscreenPackage) {
                isTargetAppFullscreen = false
                activeFullscreenPackage = ""
            }
            currentForegroundPackage = newPackage
            updateOverlayButtons()
        }
    }

    // --- Profile Preferences Helper Methods ---

    private fun getPrefKey(profileId: Int, key: String): String = "overlay_${profileId}_$key"

    private fun isProfileEnabled(profileId: Int): Boolean {
        val pKey = getPrefKey(profileId, "enabled")
        if (prefs.contains(pKey)) {
            return prefs.getBoolean(pKey, false)
        }
        if (profileId == 1) {
            return prefs.getBoolean("quick_launch_enabled", false) ||
                   prefs.getBoolean("fullscreen_overlay_enabled", false) ||
                   prefs.getBoolean("home_navigator_enabled", false) ||
                   prefs.getBoolean("back_navigator_enabled", false) ||
                   prefs.getBoolean("refresh_navigator_enabled", false) ||
                   prefs.getBoolean("freeform_window_enabled", false) ||
                   prefs.getBoolean("separate_buttons_enabled", false)
        }
        return false
    }

    private fun getProfileBool(profileId: Int, key: String, legacyKey: String, def: Boolean): Boolean {
        val pKey = getPrefKey(profileId, key)
        return if (prefs.contains(pKey)) prefs.getBoolean(pKey, def)
               else if (profileId == 1) prefs.getBoolean(legacyKey, def)
               else def
    }

    private fun getProfileInt(profileId: Int, key: String, legacyKey: String, def: Int): Int {
        val pKey = getPrefKey(profileId, key)
        return if (prefs.contains(pKey)) prefs.getInt(pKey, def)
               else if (profileId == 1) prefs.getInt(legacyKey, def)
               else def
    }

    private fun getProfileString(profileId: Int, key: String, legacyKey: String, def: String): String {
        val pKey = getPrefKey(profileId, key)
        return if (prefs.contains(pKey)) prefs.getString(pKey, def) ?: def
               else if (profileId == 1) prefs.getString(legacyKey, def) ?: def
               else def
    }

    private fun getProfileStringSet(profileId: Int, key: String, legacyKey: String): Set<String> {
        val pKey = getPrefKey(profileId, key)
        return if (prefs.contains(pKey)) prefs.getStringSet(pKey, Collections.emptySet()) ?: emptySet()
               else if (profileId == 1) prefs.getStringSet(legacyKey, Collections.emptySet()) ?: emptySet()
               else emptySet()
    }

    private fun getAllProfileIds(): List<Int> {
        val jsonStr = prefs.getString("overlay_profiles_json", null)
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val jsonArray = org.json.JSONArray(jsonStr)
                val ids = mutableListOf<Int>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    ids.add(obj.getInt("id"))
                }
                if (ids.isNotEmpty()) return ids
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val ids = mutableListOf<Int>()
        for (i in 1..100) {
            val pKey = getPrefKey(i, "enabled")
            val pName = getPrefKey(i, "name")
            if (prefs.contains(pKey) || prefs.contains(pName) || i == 1) {
                ids.add(i)
            }
        }
        return if (ids.isNotEmpty()) ids else listOf(1)
    }

    private fun getDefaultY(profileId: Int, buttonType: String): Int {
        val offset = ((profileId - 1) % 10) * 150
        return when (buttonType) {
            "combined", "ql" -> 200 + offset
            "fs" -> 100 + offset
            "home" -> 300 + offset
            "back" -> 400 + offset
            "refresh" -> 500 + offset
            "freeform" -> 600 + offset
            else -> 200 + offset
        }
    }

    // --- Main Overlay Update Loop ---

    fun updateOverlayButtons() {
        val currentProfileIds = getAllProfileIds()
        val activeViewIds = ArrayList(profileViewsMap.keys)
        for (id in activeViewIds) {
            if (!currentProfileIds.contains(id) || !isProfileEnabled(id)) {
                removeProfileOverlayButtons(id)
            }
        }
        for (profileId in currentProfileIds) {
            if (isProfileEnabled(profileId)) {
                updateProfileOverlayButtons(profileId)
            }
        }
    }

    private fun removeProfileOverlayButtons(profileId: Int) {
        val views = profileViewsMap[profileId] ?: return

        views.quickLaunchView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
        views.quickLaunchView = null
        views.quickLaunchViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
        views.quickLaunchViewSecondary = null

        views.fullscreenToggleView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
        views.fullscreenToggleView = null
        views.fullscreenToggleViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
        views.fullscreenToggleViewSecondary = null

        views.homeView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
        views.homeView = null
        views.homeViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
        views.homeViewSecondary = null

        views.backView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
        views.backView = null
        views.backViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
        views.backViewSecondary = null

        views.refreshView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
        views.refreshView = null
        views.refreshViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
        views.refreshViewSecondary = null

        views.freeformView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
        views.freeformView = null
        views.freeformViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
        views.freeformViewSecondary = null

        views.combinedView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
        views.combinedView = null
        views.combinedViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
        views.combinedViewSecondary = null

        profileViewsMap.remove(profileId)
    }

    private fun updateProfileOverlayButtons(profileId: Int) {
        val separateEnabled = getProfileBool(profileId, "separate_buttons", "separate_buttons_enabled", false)
        val views = profileViewsMap.getOrPut(profileId) { ProfileOverlayViews(profileId) }

        if (separateEnabled) {
            hideProfileCombinedOverlay(views)
            updateProfileQuickLaunchButton(profileId, views)
            updateProfileFullscreenButton(profileId, views)
            updateProfileHomeButton(profileId, views)
            updateProfileBackButton(profileId, views)
            updateProfileRefreshButton(profileId, views)
            updateProfileFreeformButton(profileId, views)
        } else {
            hideProfileSeparateButtons(views)
            updateProfileCombinedOverlay(profileId, views)
        }
    }

    private fun hideProfileSeparateButtons(views: ProfileOverlayViews) {
        views.quickLaunchView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
        views.quickLaunchView = null
        views.quickLaunchViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
        views.quickLaunchViewSecondary = null

        views.fullscreenToggleView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
        views.fullscreenToggleView = null
        views.fullscreenToggleViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
        views.fullscreenToggleViewSecondary = null

        views.homeView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
        views.homeView = null
        views.homeViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
        views.homeViewSecondary = null

        views.backView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
        views.backView = null
        views.backViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
        views.backViewSecondary = null

        views.refreshView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
        views.refreshView = null
        views.refreshViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
        views.refreshViewSecondary = null

        views.freeformView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
        views.freeformView = null
        views.freeformViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
        views.freeformViewSecondary = null
    }

    private fun hideProfileCombinedOverlay(views: ProfileOverlayViews) {
        views.combinedView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
        views.combinedView = null
        views.combinedViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
        views.combinedViewSecondary = null
    }

    // --- Combined Overlay rendering per profile ---

    private fun updateProfileCombinedOverlay(profileId: Int, views: ProfileOverlayViews) {
        val showQuickLaunch = getProfileBool(profileId, "quick_launch_enabled", "quick_launch_enabled", false)
        val fullscreenApps = getProfileStringSet(profileId, "fullscreen_apps", "fullscreen_apps")
        val isCurrentAppTarget = fullscreenApps.contains(currentForegroundPackage)
        val showFullscreen = (getProfileBool(profileId, "fullscreen_overlay_enabled", "fullscreen_overlay_enabled", false) && isCurrentAppTarget) || isTargetAppFullscreen
        val showHome = getProfileBool(profileId, "home_navigator_enabled", "home_navigator_enabled", false)
        val showBack = getProfileBool(profileId, "back_navigator_enabled", "back_navigator_enabled", false)
        val showRefresh = getProfileBool(profileId, "refresh_navigator_enabled", "refresh_navigator_enabled", false)
        val showFreeform = getProfileBool(profileId, "freeform_window_enabled", "freeform_window_enabled", false)

        if (!showQuickLaunch && !showFullscreen && !showHome && !showBack && !showRefresh && !showFreeform) {
            hideProfileCombinedOverlay(views)
            return
        }

        val opacityPercent = getProfileInt(profileId, "opacity_percent", "opacity_percent", 85)
        val alphaFloat = (opacityPercent.coerceIn(10, 100)) / 100.0f

        val sizeDp = getProfileInt(profileId, "combined_button_size", "combined_button_size", 48)
        val sizePx = dpToPx(sizeDp)

        hideProfileCombinedOverlay(views)

        val defY = getDefaultY(profileId, "combined")
        val x = getProfileInt(profileId, "combined_pos_x", "combined_pos_x", 0)
        val y = getProfileInt(profileId, "combined_pos_y", "combined_pos_y", defY)
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val distLeft = x
        val distRight = max(0, screenWidth - x)
        val distTop = y
        val distBottom = max(0, screenHeight - y)
        val minDist = min(min(distLeft, distRight), min(distTop, distBottom))
        val isHorizontal = (minDist == distTop || minDist == distBottom)

        views.combinedView = createCombinedLinearLayout(this, showQuickLaunch, showFullscreen, showHome, showBack, showRefresh, showFreeform, sizePx, isHorizontal, profileId)
        views.combinedView?.alpha = alphaFloat

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
            this.alpha = alphaFloat
        }
        views.combinedParams = params

        defaultWindowManager.addView(views.combinedView, params)

        val secWM = secondaryWindowManager
        val secContext = secondaryContext
        if (secWM != null && secWM != defaultWindowManager && secContext != null) {
            views.combinedViewSecondary = createCombinedLinearLayout(secContext, showQuickLaunch, showFullscreen, showHome, showBack, showRefresh, showFreeform, sizePx, isHorizontal, profileId)
            views.combinedViewSecondary?.alpha = alphaFloat
            val marginPx = dpToPx(4)
            val numButtons = (if (showQuickLaunch) 1 else 0) + (if (showFullscreen) 1 else 0) + (if (showHome) 1 else 0) + (if (showBack) 1 else 0) + (if (showRefresh) 1 else 0) + (if (showFreeform) 1 else 0)
            val estWidth = if (isHorizontal) (sizePx + 2 * marginPx) * numButtons else (sizePx + 2 * marginPx)
            val estHeight = if (isHorizontal) (sizePx + 2 * marginPx) else (sizePx + 2 * marginPx) * numButtons

            val paramsSec = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = screenWidth - x - estWidth
                this.y = screenHeight - y - estHeight
                this.alpha = alphaFloat
            }
            views.combinedParamsSecondary = paramsSec
            secWM.addView(views.combinedViewSecondary, paramsSec)
        }

        setupCombinedTouchesForProfile(profileId, views)
    }

    private fun setupCombinedTouchesForProfile(profileId: Int, views: ProfileOverlayViews) {
        val cv = views.combinedView ?: return
        val params = views.combinedParams ?: return
        val showQuickLaunch = getProfileBool(profileId, "quick_launch_enabled", "quick_launch_enabled", false)
        val fullscreenApps = getProfileStringSet(profileId, "fullscreen_apps", "fullscreen_apps")
        val isCurrentAppTarget = fullscreenApps.contains(currentForegroundPackage)
        val showFullscreen = (getProfileBool(profileId, "fullscreen_overlay_enabled", "fullscreen_overlay_enabled", false) && isCurrentAppTarget) || isTargetAppFullscreen
        val showHome = getProfileBool(profileId, "home_navigator_enabled", "home_navigator_enabled", false)
        val showBack = getProfileBool(profileId, "back_navigator_enabled", "back_navigator_enabled", false)
        val showRefresh = getProfileBool(profileId, "refresh_navigator_enabled", "refresh_navigator_enabled", false)
        val showFreeform = getProfileBool(profileId, "freeform_window_enabled", "freeform_window_enabled", false)

        val prefPrefix = "overlay_${profileId}_combined"

        var index = 0
        if (showQuickLaunch && cv.childCount > index) {
            val qlItem = cv.getChildAt(index++)
            setupDragAndSnapTouch(qlItem, cv, params, views.combinedViewSecondary, views.combinedParamsSecondary, { showQuickLaunchMenuForProfile(profileId) }, prefPrefix, profileId)
        }
        if (showFullscreen && cv.childCount > index) {
            val fsItem = cv.getChildAt(index++)
            setupDragAndSnapTouch(fsItem, cv, params, views.combinedViewSecondary, views.combinedParamsSecondary, { toggleFullscreen() }, prefPrefix, profileId)
        }
        if (showHome && cv.childCount > index) {
            val homeItem = cv.getChildAt(index++)
            setupDragAndSnapTouch(homeItem, cv, params, views.combinedViewSecondary, views.combinedParamsSecondary, { performHomeAction() }, prefPrefix, profileId)
        }
        if (showBack && cv.childCount > index) {
            val backItem = cv.getChildAt(index++)
            setupDragAndSnapTouch(backItem, cv, params, views.combinedViewSecondary, views.combinedParamsSecondary, { performBackAction() }, prefPrefix, profileId)
        }
        if (showRefresh && cv.childCount > index) {
            val refreshItem = cv.getChildAt(index++)
            setupDragAndSnapTouch(refreshItem, cv, params, views.combinedViewSecondary, views.combinedParamsSecondary, { performRefreshAction() }, prefPrefix, profileId)
        }
        if (showFreeform && cv.childCount > index) {
            val freeformItem = cv.getChildAt(index++)
            setupDragAndSnapTouch(freeformItem, cv, params, views.combinedViewSecondary, views.combinedParamsSecondary, { performFreeformAction() }, prefPrefix, profileId)
        }
    }

    private fun createCombinedLinearLayout(
        context: Context,
        showQuickLaunch: Boolean,
        showFullscreen: Boolean,
        showHome: Boolean,
        showBack: Boolean,
        showRefresh: Boolean,
        showFreeform: Boolean,
        sizePx: Int,
        isHorizontal: Boolean,
        profileId: Int
    ): LinearLayout {
        val colorHex = getProfileString(profileId, "button_color", "button_color", "#7C4DFF")
        val shapeStr = getProfileString(profileId, "button_shape", "button_shape", "CIRCLE")

        val container = LinearLayout(context).apply {
            orientation = if (isHorizontal) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(16).toFloat()
                setColor(Color.parseColor("#33000000"))
            }
        }

        if (showQuickLaunch) {
            val qlColor = getProfileString(profileId, "ql_color_hex", "ql_color_hex", colorHex)
            val qlShape = getProfileString(profileId, "ql_shape", "ql_shape", shapeStr)
            container.addView(createSingleOverlayButton(context, R.drawable.ic_menu, qlColor, qlShape, sizePx))
        }
        if (showFullscreen) {
            val fsColor = getProfileString(profileId, "fs_color_hex", "fs_color_hex", colorHex)
            val fsShape = getProfileString(profileId, "fs_shape", "fs_shape", shapeStr)
            val icon = if (isTargetAppFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen_enter
            container.addView(createSingleOverlayButton(context, icon, fsColor, fsShape, sizePx))
        }
        if (showHome) {
            val homeColor = getProfileString(profileId, "home_color_hex", "home_color_hex", colorHex)
            val homeShape = getProfileString(profileId, "home_shape", "home_shape", shapeStr)
            container.addView(createSingleOverlayButton(context, R.drawable.ic_home, homeColor, homeShape, sizePx))
        }
        if (showBack) {
            val backColor = getProfileString(profileId, "back_color_hex", "back_color_hex", colorHex)
            val backShape = getProfileString(profileId, "back_shape", "back_shape", shapeStr)
            container.addView(createSingleOverlayButton(context, R.drawable.ic_back, backColor, backShape, sizePx))
        }
        if (showRefresh) {
            val refreshColor = getProfileString(profileId, "refresh_color_hex", "refresh_color_hex", colorHex)
            val refreshShape = getProfileString(profileId, "refresh_shape", "refresh_shape", shapeStr)
            container.addView(createSingleOverlayButton(context, R.drawable.ic_refresh, refreshColor, refreshShape, sizePx))
        }
        if (showFreeform) {
            val freeformColor = getProfileString(profileId, "freeform_color_hex", "freeform_color_hex", colorHex)
            val freeformShape = getProfileString(profileId, "freeform_shape", "freeform_shape", shapeStr)
            container.addView(createSingleOverlayButton(context, R.drawable.ic_freeform, freeformColor, freeformShape, sizePx))
        }

        return container
    }

    // --- Separate Buttons rendering per profile ---

    private fun updateProfileQuickLaunchButton(profileId: Int, views: ProfileOverlayViews) {
        val isEnabled = getProfileBool(profileId, "quick_launch_enabled", "quick_launch_enabled", false)
        if (!isEnabled) {
            views.quickLaunchView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
            views.quickLaunchView = null
            views.quickLaunchViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
            views.quickLaunchViewSecondary = null
            return
        }

        val opacityPercent = getProfileInt(profileId, "ql_opacity_percent", "ql_opacity_percent", getProfileInt(profileId, "opacity_percent", "opacity_percent", 85))
        val alphaFloat = (opacityPercent.coerceIn(10, 100)) / 100.0f
        val sizeDp = getProfileInt(profileId, "ql_button_size", "ql_button_size", 48)
        val sizePx = dpToPx(sizeDp)

        val colorHex = getProfileString(profileId, "ql_color_hex", "ql_color_hex", getProfileString(profileId, "button_color", "button_color", "#6750A4"))
        val shapeStr = getProfileString(profileId, "ql_shape", "ql_shape", getProfileString(profileId, "button_shape", "button_shape", "CIRCLE"))

        val defY = getDefaultY(profileId, "ql")
        val x = getProfileInt(profileId, "ql_pos_x", "ql_pos_x", 0)
        val y = getProfileInt(profileId, "ql_pos_y", "ql_pos_y", defY)

        val prefPrefix = "overlay_${profileId}_ql"

        if (views.quickLaunchView == null) {
            val qv = createSingleOverlayButton(this, R.drawable.ic_menu, colorHex, shapeStr, sizePx)
            qv.alpha = alphaFloat

            val params = WindowManager.LayoutParams(
                sizePx, sizePx,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = x
                this.y = y
                this.alpha = alphaFloat
            }
            views.quickLaunchParams = params
            views.quickLaunchView = qv
            defaultWindowManager.addView(qv, params)

            val secWM = secondaryWindowManager
            val secContext = secondaryContext
            if (secWM != null && secWM != defaultWindowManager && secContext != null) {
                val qvSec = createSingleOverlayButton(secContext, R.drawable.ic_menu, colorHex, shapeStr, sizePx)
                qvSec.alpha = alphaFloat
                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels
                val paramsSec = WindowManager.LayoutParams(
                    sizePx, sizePx,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    this.x = screenWidth - x - sizePx
                    this.y = screenHeight - y - sizePx
                    this.alpha = alphaFloat
                }
                views.quickLaunchParamsSecondary = paramsSec
                views.quickLaunchViewSecondary = qvSec
                secWM.addView(qvSec, paramsSec)
            }

            setupDragAndSnapTouch(qv, qv, params, views.quickLaunchViewSecondary, views.quickLaunchParamsSecondary, { showQuickLaunchMenuForProfile(profileId) }, prefPrefix, profileId)
        } else {
            val qv = views.quickLaunchView!!
            val params = views.quickLaunchParams!!
            updateSingleButtonVisuals(qv, R.drawable.ic_menu, colorHex, shapeStr, sizePx)
            qv.alpha = alphaFloat
            params.width = sizePx
            params.height = sizePx
            params.alpha = alphaFloat
            defaultWindowManager.updateViewLayout(qv, params)

            val qvSec = views.quickLaunchViewSecondary
            val paramsSec = views.quickLaunchParamsSecondary
            if (qvSec != null && paramsSec != null) {
                updateSingleButtonVisuals(qvSec, R.drawable.ic_menu, colorHex, shapeStr, sizePx)
                qvSec.alpha = alphaFloat
                paramsSec.width = sizePx
                paramsSec.height = sizePx
                paramsSec.alpha = alphaFloat
                secondaryWindowManager?.updateViewLayout(qvSec, paramsSec)
            }
        }
    }

    private fun updateProfileFullscreenButton(profileId: Int, views: ProfileOverlayViews) {
        val fullscreenApps = getProfileStringSet(profileId, "fullscreen_apps", "fullscreen_apps")
        val isCurrentAppTarget = fullscreenApps.contains(currentForegroundPackage)
        val shouldShow = (getProfileBool(profileId, "fullscreen_overlay_enabled", "fullscreen_overlay_enabled", false) && isCurrentAppTarget) || isTargetAppFullscreen

        if (!shouldShow) {
            views.fullscreenToggleView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
            views.fullscreenToggleView = null
            views.fullscreenToggleViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
            views.fullscreenToggleViewSecondary = null
            return
        }

        val opacityPercent = getProfileInt(profileId, "fs_opacity_percent", "fs_opacity_percent", getProfileInt(profileId, "opacity_percent", "opacity_percent", 85))
        val alphaFloat = (opacityPercent.coerceIn(10, 100)) / 100.0f
        val sizeDp = getProfileInt(profileId, "fs_button_size", "fs_button_size", 48)
        val sizePx = dpToPx(sizeDp)

        val colorHex = getProfileString(profileId, "fs_color_hex", "fs_color_hex", getProfileString(profileId, "button_color", "button_color", "#1976D2"))
        val shapeStr = getProfileString(profileId, "fs_shape", "fs_shape", getProfileString(profileId, "button_shape", "button_shape", "CIRCLE"))
        val iconRes = if (isTargetAppFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen_enter

        val defY = getDefaultY(profileId, "fs")
        val x = getProfileInt(profileId, "fs_pos_x", "fs_pos_x", 100)
        val y = getProfileInt(profileId, "fs_pos_y", "fs_pos_y", defY)

        val prefPrefix = "overlay_${profileId}_fs"

        if (views.fullscreenToggleView == null) {
            val fv = createSingleOverlayButton(this, iconRes, colorHex, shapeStr, sizePx)
            fv.alpha = alphaFloat

            val params = WindowManager.LayoutParams(
                sizePx, sizePx,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = x
                this.y = y
                this.alpha = alphaFloat
            }
            views.fullscreenToggleParams = params
            views.fullscreenToggleView = fv
            defaultWindowManager.addView(fv, params)

            val secWM = secondaryWindowManager
            val secContext = secondaryContext
            if (secWM != null && secWM != defaultWindowManager && secContext != null) {
                val fvSec = createSingleOverlayButton(secContext, iconRes, colorHex, shapeStr, sizePx)
                fvSec.alpha = alphaFloat
                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels
                val paramsSec = WindowManager.LayoutParams(
                    sizePx, sizePx,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    this.x = screenWidth - x - sizePx
                    this.y = screenHeight - y - sizePx
                    this.alpha = alphaFloat
                }
                views.fullscreenToggleParamsSecondary = paramsSec
                views.fullscreenToggleViewSecondary = fvSec
                secWM.addView(fvSec, paramsSec)
            }

            setupDragAndSnapTouch(fv, fv, params, views.fullscreenToggleViewSecondary, views.fullscreenToggleParamsSecondary, { toggleFullscreen() }, prefPrefix, profileId)
        } else {
            val fv = views.fullscreenToggleView!!
            val params = views.fullscreenToggleParams!!
            updateSingleButtonVisuals(fv, iconRes, colorHex, shapeStr, sizePx)
            fv.alpha = alphaFloat
            params.width = sizePx
            params.height = sizePx
            params.alpha = alphaFloat
            defaultWindowManager.updateViewLayout(fv, params)

            val fvSec = views.fullscreenToggleViewSecondary
            val paramsSec = views.fullscreenToggleParamsSecondary
            if (fvSec != null && paramsSec != null) {
                updateSingleButtonVisuals(fvSec, iconRes, colorHex, shapeStr, sizePx)
                fvSec.alpha = alphaFloat
                paramsSec.width = sizePx
                paramsSec.height = sizePx
                paramsSec.alpha = alphaFloat
                secondaryWindowManager?.updateViewLayout(fvSec, paramsSec)
            }
        }
    }

    private fun updateProfileHomeButton(profileId: Int, views: ProfileOverlayViews) {
        val isEnabled = getProfileBool(profileId, "home_navigator_enabled", "home_navigator_enabled", false)
        if (!isEnabled) {
            views.homeView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
            views.homeView = null
            views.homeViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
            views.homeViewSecondary = null
            return
        }

        val opacityPercent = getProfileInt(profileId, "home_opacity_percent", "home_opacity_percent", getProfileInt(profileId, "opacity_percent", "opacity_percent", 85))
        val alphaFloat = (opacityPercent.coerceIn(10, 100)) / 100.0f
        val sizeDp = getProfileInt(profileId, "home_button_size", "home_button_size", 48)
        val sizePx = dpToPx(sizeDp)

        val colorHex = getProfileString(profileId, "home_color_hex", "home_color_hex", getProfileString(profileId, "button_color", "button_color", "#388E3C"))
        val shapeStr = getProfileString(profileId, "home_shape", "home_shape", getProfileString(profileId, "button_shape", "button_shape", "CIRCLE"))

        val defY = getDefaultY(profileId, "home")
        val x = getProfileInt(profileId, "home_pos_x", "home_pos_x", 0)
        val y = getProfileInt(profileId, "home_pos_y", "home_pos_y", defY)

        val prefPrefix = "overlay_${profileId}_home"

        if (views.homeView == null) {
            val hv = createSingleOverlayButton(this, R.drawable.ic_home, colorHex, shapeStr, sizePx)
            hv.alpha = alphaFloat

            val params = WindowManager.LayoutParams(
                sizePx, sizePx,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = x
                this.y = y
                this.alpha = alphaFloat
            }
            views.homeParams = params
            views.homeView = hv
            defaultWindowManager.addView(hv, params)

            val secWM = secondaryWindowManager
            val secContext = secondaryContext
            if (secWM != null && secWM != defaultWindowManager && secContext != null) {
                val hvSec = createSingleOverlayButton(secContext, R.drawable.ic_home, colorHex, shapeStr, sizePx)
                hvSec.alpha = alphaFloat
                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels
                val paramsSec = WindowManager.LayoutParams(
                    sizePx, sizePx,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    this.x = screenWidth - x - sizePx
                    this.y = screenHeight - y - sizePx
                    this.alpha = alphaFloat
                }
                views.homeParamsSecondary = paramsSec
                views.homeViewSecondary = hvSec
                secWM.addView(hvSec, paramsSec)
            }

            setupDragAndSnapTouch(hv, hv, params, views.homeViewSecondary, views.homeParamsSecondary, { performHomeAction() }, prefPrefix, profileId)
        } else {
            val hv = views.homeView!!
            val params = views.homeParams!!
            updateSingleButtonVisuals(hv, R.drawable.ic_home, colorHex, shapeStr, sizePx)
            hv.alpha = alphaFloat
            params.width = sizePx
            params.height = sizePx
            params.alpha = alphaFloat
            defaultWindowManager.updateViewLayout(hv, params)

            val hvSec = views.homeViewSecondary
            val paramsSec = views.homeParamsSecondary
            if (hvSec != null && paramsSec != null) {
                updateSingleButtonVisuals(hvSec, R.drawable.ic_home, colorHex, shapeStr, sizePx)
                hvSec.alpha = alphaFloat
                paramsSec.width = sizePx
                paramsSec.height = sizePx
                paramsSec.alpha = alphaFloat
                secondaryWindowManager?.updateViewLayout(hvSec, paramsSec)
            }
        }
    }

    private fun updateProfileBackButton(profileId: Int, views: ProfileOverlayViews) {
        val isEnabled = getProfileBool(profileId, "back_navigator_enabled", "back_navigator_enabled", false)
        if (!isEnabled) {
            views.backView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
            views.backView = null
            views.backViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
            views.backViewSecondary = null
            return
        }

        val opacityPercent = getProfileInt(profileId, "back_opacity_percent", "back_opacity_percent", getProfileInt(profileId, "opacity_percent", "opacity_percent", 85))
        val alphaFloat = (opacityPercent.coerceIn(10, 100)) / 100.0f
        val sizeDp = getProfileInt(profileId, "back_button_size", "back_button_size", 48)
        val sizePx = dpToPx(sizeDp)

        val colorHex = getProfileString(profileId, "back_color_hex", "back_color_hex", getProfileString(profileId, "button_color", "button_color", "#D32F2F"))
        val shapeStr = getProfileString(profileId, "back_shape", "back_shape", getProfileString(profileId, "button_shape", "button_shape", "CIRCLE"))

        val defY = getDefaultY(profileId, "back")
        val x = getProfileInt(profileId, "back_pos_x", "back_pos_x", 0)
        val y = getProfileInt(profileId, "back_pos_y", "back_pos_y", defY)

        val prefPrefix = "overlay_${profileId}_back"

        if (views.backView == null) {
            val bv = createSingleOverlayButton(this, R.drawable.ic_back, colorHex, shapeStr, sizePx)
            bv.alpha = alphaFloat

            val params = WindowManager.LayoutParams(
                sizePx, sizePx,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = x
                this.y = y
                this.alpha = alphaFloat
            }
            views.backParams = params
            views.backView = bv
            defaultWindowManager.addView(bv, params)

            val secWM = secondaryWindowManager
            val secContext = secondaryContext
            if (secWM != null && secWM != defaultWindowManager && secContext != null) {
                val bvSec = createSingleOverlayButton(secContext, R.drawable.ic_back, colorHex, shapeStr, sizePx)
                bvSec.alpha = alphaFloat
                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels
                val paramsSec = WindowManager.LayoutParams(
                    sizePx, sizePx,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    this.x = screenWidth - x - sizePx
                    this.y = screenHeight - y - sizePx
                    this.alpha = alphaFloat
                }
                views.backParamsSecondary = paramsSec
                views.backViewSecondary = bvSec
                secWM.addView(bvSec, paramsSec)
            }

            setupDragAndSnapTouch(bv, bv, params, views.backViewSecondary, views.backParamsSecondary, { performBackAction() }, prefPrefix, profileId)
        } else {
            val bv = views.backView!!
            val params = views.backParams!!
            updateSingleButtonVisuals(bv, R.drawable.ic_back, colorHex, shapeStr, sizePx)
            bv.alpha = alphaFloat
            params.width = sizePx
            params.height = sizePx
            params.alpha = alphaFloat
            defaultWindowManager.updateViewLayout(bv, params)

            val bvSec = views.backViewSecondary
            val paramsSec = views.backParamsSecondary
            if (bvSec != null && paramsSec != null) {
                updateSingleButtonVisuals(bvSec, R.drawable.ic_back, colorHex, shapeStr, sizePx)
                bvSec.alpha = alphaFloat
                paramsSec.width = sizePx
                paramsSec.height = sizePx
                paramsSec.alpha = alphaFloat
                secondaryWindowManager?.updateViewLayout(bvSec, paramsSec)
            }
        }
    }

    private fun updateProfileRefreshButton(profileId: Int, views: ProfileOverlayViews) {
        val isEnabled = getProfileBool(profileId, "refresh_navigator_enabled", "refresh_navigator_enabled", false)
        if (!isEnabled) {
            views.refreshView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
            views.refreshView = null
            views.refreshViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
            views.refreshViewSecondary = null
            return
        }

        val opacityPercent = getProfileInt(profileId, "refresh_opacity_percent", "refresh_opacity_percent", getProfileInt(profileId, "opacity_percent", "opacity_percent", 85))
        val alphaFloat = (opacityPercent.coerceIn(10, 100)) / 100.0f
        val sizeDp = getProfileInt(profileId, "refresh_button_size", "refresh_button_size", 48)
        val sizePx = dpToPx(sizeDp)

        val colorHex = getProfileString(profileId, "refresh_color_hex", "refresh_color_hex", getProfileString(profileId, "button_color", "button_color", "#FFA000"))
        val shapeStr = getProfileString(profileId, "refresh_shape", "refresh_shape", getProfileString(profileId, "button_shape", "button_shape", "CIRCLE"))

        val defY = getDefaultY(profileId, "refresh")
        val x = getProfileInt(profileId, "refresh_pos_x", "refresh_pos_x", 0)
        val y = getProfileInt(profileId, "refresh_pos_y", "refresh_pos_y", defY)

        val prefPrefix = "overlay_${profileId}_refresh"

        if (views.refreshView == null) {
            val rv = createSingleOverlayButton(this, R.drawable.ic_refresh, colorHex, shapeStr, sizePx)
            rv.alpha = alphaFloat

            val params = WindowManager.LayoutParams(
                sizePx, sizePx,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = x
                this.y = y
                this.alpha = alphaFloat
            }
            views.refreshParams = params
            views.refreshView = rv
            defaultWindowManager.addView(rv, params)

            val secWM = secondaryWindowManager
            val secContext = secondaryContext
            if (secWM != null && secWM != defaultWindowManager && secContext != null) {
                val rvSec = createSingleOverlayButton(secContext, R.drawable.ic_refresh, colorHex, shapeStr, sizePx)
                rvSec.alpha = alphaFloat
                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels
                val paramsSec = WindowManager.LayoutParams(
                    sizePx, sizePx,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    this.x = screenWidth - x - sizePx
                    this.y = screenHeight - y - sizePx
                    this.alpha = alphaFloat
                }
                views.refreshParamsSecondary = paramsSec
                views.refreshViewSecondary = rvSec
                secWM.addView(rvSec, paramsSec)
            }

            setupDragAndSnapTouch(rv, rv, params, views.refreshViewSecondary, views.refreshParamsSecondary, { performRefreshAction() }, prefPrefix, profileId)
        } else {
            val rv = views.refreshView!!
            val params = views.refreshParams!!
            updateSingleButtonVisuals(rv, R.drawable.ic_refresh, colorHex, shapeStr, sizePx)
            rv.alpha = alphaFloat
            params.width = sizePx
            params.height = sizePx
            params.alpha = alphaFloat
            defaultWindowManager.updateViewLayout(rv, params)

            val rvSec = views.refreshViewSecondary
            val paramsSec = views.refreshParamsSecondary
            if (rvSec != null && paramsSec != null) {
                updateSingleButtonVisuals(rvSec, R.drawable.ic_refresh, colorHex, shapeStr, sizePx)
                rvSec.alpha = alphaFloat
                paramsSec.width = sizePx
                paramsSec.height = sizePx
                paramsSec.alpha = alphaFloat
                secondaryWindowManager?.updateViewLayout(rvSec, paramsSec)
            }
        }
    }

    private fun updateProfileFreeformButton(profileId: Int, views: ProfileOverlayViews) {
        val isEnabled = getProfileBool(profileId, "freeform_window_enabled", "freeform_window_enabled", false)
        if (!isEnabled) {
            views.freeformView?.let { try { defaultWindowManager.removeView(it) } catch (_: Exception) {} }
            views.freeformView = null
            views.freeformViewSecondary?.let { try { secondaryWindowManager?.removeView(it) } catch (_: Exception) {} }
            views.freeformViewSecondary = null
            return
        }

        val opacityPercent = getProfileInt(profileId, "freeform_opacity_percent", "freeform_opacity_percent", getProfileInt(profileId, "opacity_percent", "opacity_percent", 85))
        val alphaFloat = (opacityPercent.coerceIn(10, 100)) / 100.0f
        val sizeDp = getProfileInt(profileId, "freeform_button_size", "freeform_button_size", 48)
        val sizePx = dpToPx(sizeDp)

        val colorHex = getProfileString(profileId, "freeform_color_hex", "freeform_color_hex", getProfileString(profileId, "button_color", "button_color", "#00897B"))
        val shapeStr = getProfileString(profileId, "freeform_shape", "freeform_shape", getProfileString(profileId, "button_shape", "button_shape", "CIRCLE"))

        val defY = getDefaultY(profileId, "freeform")
        val x = getProfileInt(profileId, "freeform_pos_x", "freeform_pos_x", 0)
        val y = getProfileInt(profileId, "freeform_pos_y", "freeform_pos_y", defY)

        val prefPrefix = "overlay_${profileId}_freeform"

        if (views.freeformView == null) {
            val fv = createSingleOverlayButton(this, R.drawable.ic_freeform, colorHex, shapeStr, sizePx)
            fv.alpha = alphaFloat

            val params = WindowManager.LayoutParams(
                sizePx, sizePx,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = x
                this.y = y
                this.alpha = alphaFloat
            }
            views.freeformParams = params
            views.freeformView = fv
            defaultWindowManager.addView(fv, params)

            val secWM = secondaryWindowManager
            val secContext = secondaryContext
            if (secWM != null && secWM != defaultWindowManager && secContext != null) {
                val fvSec = createSingleOverlayButton(secContext, R.drawable.ic_freeform, colorHex, shapeStr, sizePx)
                fvSec.alpha = alphaFloat
                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels
                val paramsSec = WindowManager.LayoutParams(
                    sizePx, sizePx,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    this.x = screenWidth - x - sizePx
                    this.y = screenHeight - y - sizePx
                    this.alpha = alphaFloat
                }
                views.freeformParamsSecondary = paramsSec
                views.freeformViewSecondary = fvSec
                secWM.addView(fvSec, paramsSec)
            }

            setupDragAndSnapTouch(fv, fv, params, views.freeformViewSecondary, views.freeformParamsSecondary, { performFreeformAction() }, prefPrefix, profileId)
        } else {
            val fv = views.freeformView!!
            val params = views.freeformParams!!
            updateSingleButtonVisuals(fv, R.drawable.ic_freeform, colorHex, shapeStr, sizePx)
            fv.alpha = alphaFloat
            params.width = sizePx
            params.height = sizePx
            params.alpha = alphaFloat
            defaultWindowManager.updateViewLayout(fv, params)

            val fvSec = views.freeformViewSecondary
            val paramsSec = views.freeformParamsSecondary
            if (fvSec != null && paramsSec != null) {
                updateSingleButtonVisuals(fvSec, R.drawable.ic_freeform, colorHex, shapeStr, sizePx)
                fvSec.alpha = alphaFloat
                paramsSec.width = sizePx
                paramsSec.height = sizePx
                paramsSec.alpha = alphaFloat
                secondaryWindowManager?.updateViewLayout(fvSec, paramsSec)
            }
        }
    }

    // --- Custom Path Drawable & Outline Helper Classes ---

    private class PathDrawable(
        private val pathBuilder: (Float, Float) -> Path,
        private var color: Int
    ) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = this@PathDrawable.color
        }
        private val path = Path()

        override fun onBoundsChange(bounds: Rect) {
            super.onBoundsChange(bounds)
            path.reset()
            val w = bounds.width().toFloat()
            val h = bounds.height().toFloat()
            if (w > 0 && h > 0) {
                path.set(pathBuilder(w, h))
            }
        }

        override fun draw(canvas: Canvas) {
            canvas.drawPath(path, paint)
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
            invalidateSelf()
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
            invalidateSelf()
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    private fun createStarPath(w: Float, h: Float): Path {
        val path = Path()
        val cx = w / 2f
        val cy = h / 2f
        val outerRadius = min(w, h) / 2f * 0.95f
        val innerRadius = outerRadius * 0.42f
        val points = 5
        val angleStep = Math.PI / points

        for (i in 0 until (points * 2)) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val angle = -Math.PI / 2 + i * angleStep
            val x = (cx + r * kotlin.math.cos(angle)).toFloat()
            val y = (cy + r * kotlin.math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private fun createOctagonPath(w: Float, h: Float): Path {
        val path = Path()
        val corner = min(w, h) * 0.28f
        path.moveTo(corner, 0f)
        path.lineTo(w - corner, 0f)
        path.lineTo(w, corner)
        path.lineTo(w, h - corner)
        path.lineTo(w - corner, h)
        path.lineTo(corner, h)
        path.lineTo(0f, h - corner)
        path.lineTo(0f, corner)
        path.close()
        return path
    }

    private fun createHeartPath(w: Float, h: Float): Path {
        val path = Path()
        val cx = w / 2f
        val topY = h * 0.25f
        val bottomY = h * 0.88f

        path.moveTo(cx, bottomY)
        path.cubicTo(
            cx - w * 0.55f, h * 0.55f,
            cx - w * 0.55f, h * 0.08f,
            cx - w * 0.26f, h * 0.08f
        )
        path.cubicTo(
            cx - w * 0.08f, h * 0.08f,
            cx, topY,
            cx, topY
        )
        path.cubicTo(
            cx, topY,
            cx + w * 0.08f, h * 0.08f,
            cx + w * 0.26f, h * 0.08f
        )
        path.cubicTo(
            cx + w * 0.55f, h * 0.08f,
            cx + w * 0.55f, h * 0.55f,
            cx, bottomY
        )
        path.close()
        return path
    }

    private fun applyCustomPathOutline(view: View, pathBuilder: (Float, Float) -> Path) {
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val w = view.width.toFloat().takeIf { it > 0 } ?: dpToPx(48).toFloat()
                val h = view.height.toFloat().takeIf { it > 0 } ?: dpToPx(48).toFloat()
                val path = pathBuilder(w, h)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        outline.setPath(path)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        @Suppress("DEPRECATION")
                        if (path.isConvex) {
                            outline.setConvexPath(path)
                        } else {
                            outline.setPath(path)
                        }
                    }
                } catch (_: Exception) {
                    outline.setRect(0, 0, view.width, view.height)
                }
            }
        }
        view.clipToOutline = true
    }

    // --- Button Creation & Visual Helpers ---

    private fun createSingleOverlayButton(
        context: Context,
        iconRes: Int,
        colorHex: String,
        shapeStr: String,
        sizePx: Int
    ): FrameLayout {
        val frame = FrameLayout(context)
        val iconSize = (sizePx * 0.55f).toInt()
        val img = ImageView(context).apply {
            setImageResource(iconRes)
            setColorFilter(Color.WHITE)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val lp = FrameLayout.LayoutParams(iconSize, iconSize).apply {
            gravity = Gravity.CENTER
        }
        frame.addView(img, lp)
        applyButtonBackground(frame, colorHex, shapeStr)
        return frame
    }

    private fun updateSingleButtonVisuals(
        frame: View,
        iconRes: Int,
        colorHex: String,
        shapeStr: String,
        sizePx: Int
    ) {
        if (frame is FrameLayout && frame.childCount > 0) {
            val img = frame.getChildAt(0) as? ImageView
            img?.let {
                it.setImageResource(iconRes)
                val iconSize = (sizePx * 0.55f).toInt()
                val lp = it.layoutParams as? FrameLayout.LayoutParams ?: FrameLayout.LayoutParams(iconSize, iconSize)
                lp.width = iconSize
                lp.height = iconSize
                lp.gravity = Gravity.CENTER
                it.layoutParams = lp
            }
        }
        applyButtonBackground(frame, colorHex, shapeStr)
    }

    private fun applyButtonBackground(view: View, colorHex: String, shapeStr: String) {
        val parsedColor = try {
            Color.parseColor(colorHex)
        } catch (_: Exception) {
            Color.parseColor("#7C4DFF")
        }

        when (shapeStr.uppercase()) {
            "STAR" -> {
                view.background = PathDrawable(::createStarPath, parsedColor)
                applyCustomPathOutline(view, ::createStarPath)
            }
            "OCTAGON" -> {
                view.background = PathDrawable(::createOctagonPath, parsedColor)
                applyCustomPathOutline(view, ::createOctagonPath)
            }
            "HEART" -> {
                view.background = PathDrawable(::createHeartPath, parsedColor)
                applyCustomPathOutline(view, ::createHeartPath)
            }
            "ROUNDED_SQUARE" -> {
                val bg = GradientDrawable().apply {
                    setColor(parsedColor)
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(12).toFloat()
                }
                view.background = bg
                view.outlineProvider = ViewOutlineProvider.BACKGROUND
                view.clipToOutline = true
            }
            "SQUARE" -> {
                val bg = GradientDrawable().apply {
                    setColor(parsedColor)
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 0f
                }
                view.background = bg
                view.outlineProvider = ViewOutlineProvider.BACKGROUND
                view.clipToOutline = true
            }
            else -> { // CIRCLE
                val bg = GradientDrawable().apply {
                    setColor(parsedColor)
                    shape = GradientDrawable.OVAL
                }
                view.background = bg
                view.outlineProvider = ViewOutlineProvider.BACKGROUND
                view.clipToOutline = true
            }
        }
    }

    // --- Button Actions ---

    private fun toggleFullscreen() {
        val app = applicationContext
        isTargetAppFullscreen = !isTargetAppFullscreen
        if (isTargetAppFullscreen) {
            activeFullscreenPackage = currentForegroundPackage
        } else {
            activeFullscreenPackage = ""
        }
        updateOverlayButtons()
    }

    private fun performHomeAction() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun performBackAction() {
        try {
            val intent = Intent("ru.doGood.Lynk.ACTION_BACK_KEY")
            sendBroadcast(intent)
        } catch (_: Exception) {}
    }

    private fun performRefreshAction() {
        updateOverlayButtons()
    }

    private fun performFreeformAction() {
        toggleFreeformContainer()
    }

    private fun toggleFreeformContainer() {
        if (freeformContainerView != null) {
            try { defaultWindowManager.removeView(freeformContainerView) } catch (_: Exception) {}
            freeformContainerView = null
            return
        }

        val context = this
        val density = resources.displayMetrics.density
        fun dp(valDp: Int): Int = (valDp * density).toInt()

        val rootContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#1E1E2C"))
                setStroke(dp(1), Color.parseColor("#3F3F56"))
            }
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(8))
        }

        val titleTv = TextView(context).apply {
            text = "Lynk Freeform Container"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.parseColor("#FF5252"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(dp(8), 0, dp(8), 0)
            setOnClickListener {
                toggleFreeformContainer()
            }
        }

        headerLayout.addView(titleTv)
        headerLayout.addView(closeBtn)
        rootContainer.addView(headerLayout)

        val contentTv = TextView(context).apply {
            text = "Плавающее окно Lynk активна.\nВыберите действие или перетащите окно за заголовок."
            setTextColor(Color.parseColor("#B0BEC5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(4), 0, dp(8))
        }
        rootContainer.addView(contentTv)

        val params = WindowManager.LayoutParams(
            dp(280),
            dp(180),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }

        headerLayout.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        defaultWindowManager.updateViewLayout(rootContainer, params)
                        return true
                    }
                }
                return false
            }
        })

        freeformContainerParams = params
        freeformContainerView = rootContainer
        defaultWindowManager.addView(rootContainer, params)
    }

    private fun showQuickLaunchMenuForProfile(profileId: Int) {
        val appPkgs = getProfileStringSet(profileId, "quick_launch_apps", "quick_launch_apps")
        val context = this
        val density = resources.displayMetrics.density
        fun dpToPx(dp: Int): Int = (dp * density).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(16).toFloat()
                setColor(Color.parseColor("#CC1E1E2C"))
            }
        }

        val pm = packageManager
        for (pkg in appPkgs) {
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val icon = pm.getApplicationIcon(appInfo)
                val img = ImageView(context).apply {
                    setImageDrawable(icon)
                    val p = dpToPx(8)
                    setPadding(p, p, p, p)
                    setOnClickListener {
                        val launchIntent = pm.getLaunchIntentForPackage(pkg)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(launchIntent)
                        }
                    }
                }
                container.addView(img, LinearLayout.LayoutParams(dpToPx(48), dpToPx(48)))
            } catch (_: Exception) {}
        }

        if (container.childCount == 0) {
            val tv = TextView(context).apply {
                text = "Нет выбранных ПО"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            }
            container.addView(tv)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        container.setOnClickListener {
            try { defaultWindowManager.removeView(container) } catch (_: Exception) {}
        }

        defaultWindowManager.addView(container, params)
    }

    // --- Touch Drag and Snap Logic ---

    private fun setupDragAndSnapTouch(
        touchView: View,
        dragView: View,
        params: WindowManager.LayoutParams,
        dragViewSecondary: View?,
        paramsSecondary: WindowManager.LayoutParams?,
        onClick: Runnable?,
        prefPrefix: String,
        profileId: Int
    ) {
        touchView.isClickable = true
        if (onClick != null) {
            touchView.setOnClickListener { onClick.run() }
        }
        touchView.setOnLongClickListener {
            touchView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showQuickSettingsDialog(prefPrefix)
            true
        }

        touchView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        val touchSlop = ViewConfiguration.get(this@ForegroundOverlayService).scaledTouchSlop

                        if (!isDragging && (Math.abs(deltaX) > touchSlop || Math.abs(deltaY) > touchSlop)) {
                            isDragging = true
                            val cancelEvent = MotionEvent.obtain(event)
                            cancelEvent.action = MotionEvent.ACTION_CANCEL
                            v.onTouchEvent(cancelEvent)
                            cancelEvent.recycle()
                        }

                        if (isDragging) {
                            params.x = initialX + deltaX
                            params.y = initialY + deltaY
                            defaultWindowManager.updateViewLayout(dragView, params)

                            if (dragViewSecondary != null && paramsSecondary != null) {
                                val screenWidth = resources.displayMetrics.widthPixels
                                val screenHeight = resources.displayMetrics.heightPixels
                                paramsSecondary.x = screenWidth - params.x - dragView.width
                                paramsSecondary.y = screenHeight - params.y - dragView.height
                                secondaryWindowManager?.updateViewLayout(dragViewSecondary, paramsSecondary)
                            }
                            return true
                        }
                        return false
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            val screenWidth = resources.displayMetrics.widthPixels
                            val screenHeight = resources.displayMetrics.heightPixels

                            val distLeft = params.x
                            val distRight = max(0, screenWidth - (params.x + dragView.width))
                            val distTop = params.y
                            val distBottom = max(0, screenHeight - (params.y + dragView.height))

                            val minDist = min(min(distLeft, distRight), min(distTop, distBottom))
                            val isHorizontal: Boolean

                            if (minDist == distLeft) {
                                params.x = 0
                                isHorizontal = false
                            } else if (minDist == distRight) {
                                params.x = max(0, screenWidth - dragView.width)
                                isHorizontal = false
                            } else if (minDist == distTop) {
                                params.y = 0
                                isHorizontal = true
                            } else {
                                params.y = max(0, screenHeight - dragView.height)
                                isHorizontal = true
                            }

                            if (dragView is LinearLayout) {
                                dragView.orientation = if (isHorizontal) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
                            }
                            defaultWindowManager.updateViewLayout(dragView, params)

                            if (dragViewSecondary != null && paramsSecondary != null) {
                                paramsSecondary.x = screenWidth - params.x - dragView.width
                                paramsSecondary.y = screenHeight - params.y - dragView.height
                                if (dragViewSecondary is LinearLayout) {
                                    dragViewSecondary.orientation = if (isHorizontal) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
                                }
                                secondaryWindowManager?.updateViewLayout(dragViewSecondary, paramsSecondary)
                            }

                            val editor = prefs.edit()
                                .putInt("${prefPrefix}_pos_x", params.x)
                                .putInt("${prefPrefix}_pos_y", params.y)

                            if (profileId == 1) {
                                val legacyKey = prefPrefix.removePrefix("overlay_1_")
                                editor.putInt("${legacyKey}_pos_x", params.x)
                                    .putInt("${legacyKey}_pos_y", params.y)
                            }

                            editor.apply()
                            isDragging = false
                            return true
                        }
                        return false
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        isDragging = false
                        return false
                    }
                }
                return false
            }
        })
    }

    private fun showQuickSettingsDialog(prefPrefix: String) {
        activeSettingsDialogView?.let {
            try {
                defaultWindowManager.removeView(it)
            } catch (_: Exception) {}
            activeSettingsDialogView = null
        }

        val context = this
        val density = resources.displayMetrics.density
        fun dpToPx(dp: Int): Int = (dp * density).toInt()

        val sizeKey = if (prefPrefix.endsWith("combined")) "${prefPrefix}_button_size" else "${prefPrefix}_button_size"
        val opacityKey = if (prefPrefix.endsWith("combined")) "${prefPrefix}_opacity_percent" else "${prefPrefix}_opacity_percent"

        val currentSize = prefs.getInt(sizeKey, 48)
        val currentOpacity = prefs.getInt(opacityKey, 85)

        val containerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(16).toFloat()
                setColor(Color.parseColor("#1E1E2C"))
                setStroke(dpToPx(1), Color.parseColor("#3F3F56"))
            }
        }

        val headerText = TextView(context).apply {
            text = "Быстрые настройки кнопки"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dpToPx(8))
        }
        containerLayout.addView(headerText)

        val sizeText = TextView(context).apply {
            text = "Размер: ${currentSize}dp"
            setTextColor(Color.parseColor("#B0BEC5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        containerLayout.addView(sizeText)

        val sizeSeekBar = SeekBar(context).apply {
            max = 120
            progress = currentSize
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val valDp = max(24, progress)
                    sizeText.text = "Размер: ${valDp}dp"
                    prefs.edit().putInt(sizeKey, valDp).apply()
                    updateOverlayButtons()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        containerLayout.addView(sizeSeekBar)

        val opacityText = TextView(context).apply {
            text = "Прозрачность: $currentOpacity%"
            setTextColor(Color.parseColor("#B0BEC5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dpToPx(8), 0, 0)
        }
        containerLayout.addView(opacityText)

        val opacitySeekBar = SeekBar(context).apply {
            max = 100
            progress = currentOpacity
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val valPercent = max(10, progress)
                    opacityText.text = "Прозрачность: $valPercent%"
                    prefs.edit().putInt(opacityKey, valPercent).apply()
                    updateOverlayButtons()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        containerLayout.addView(opacitySeekBar)

        val closeBtn = TextView(context).apply {
            text = "Закрыть"
            setTextColor(Color.parseColor("#7C4DFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(12), 0, 0)
            setOnClickListener {
                activeSettingsDialogView?.let {
                    try {
                        defaultWindowManager.removeView(it)
                    } catch (_: Exception) {}
                    activeSettingsDialogView = null
                }
            }
        }
        containerLayout.addView(closeBtn)

        val params = WindowManager.LayoutParams(
            dpToPx(240),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        containerLayout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                activeSettingsDialogView?.let {
                    try {
                        defaultWindowManager.removeView(it)
                    } catch (_: Exception) {}
                    activeSettingsDialogView = null
                }
                true
            } else {
                false
            }
        }

        activeSettingsDialogView = containerLayout
        defaultWindowManager.addView(containerLayout, params)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Оверлей Служба",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        packageCheckerRunnable?.let { handler?.removeCallbacks(it) }

        for (profileId in 1..3) {
            removeProfileOverlayButtons(profileId)
        }

        freeformContainerView?.let {
            try { defaultWindowManager.removeView(it) } catch (_: Exception) {}
        }
        freeformContainerView = null

        if (::prefs.isInitialized) {
            prefs.unregisterOnSharedPreferenceChangeListener(this)
        }
        try {
            unregisterReceiver(overlayUpdateReceiver)
        } catch (_: Exception) {}
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key != null) {
            updateOverlayButtons()
        }
    }
}
