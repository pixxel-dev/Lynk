package ru.doGood.Lynk.service

import android.app.ActivityOptions
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
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

    private var quickLaunchView: View? = null
    private var quickLaunchParams: WindowManager.LayoutParams? = null
    private var quickLaunchViewSecondary: View? = null
    private var quickLaunchParamsSecondary: WindowManager.LayoutParams? = null

    private var fullscreenToggleView: View? = null
    private var fullscreenToggleParams: WindowManager.LayoutParams? = null
    private var fullscreenToggleViewSecondary: View? = null
    private var fullscreenToggleParamsSecondary: WindowManager.LayoutParams? = null

    private var homeView: View? = null
    private var homeParams: WindowManager.LayoutParams? = null
    private var homeViewSecondary: View? = null
    private var homeParamsSecondary: WindowManager.LayoutParams? = null

    private var backView: View? = null
    private var backParams: WindowManager.LayoutParams? = null
    private var backViewSecondary: View? = null
    private var backParamsSecondary: WindowManager.LayoutParams? = null

    private var refreshView: View? = null
    private var refreshParams: WindowManager.LayoutParams? = null
    private var refreshViewSecondary: View? = null
    private var refreshParamsSecondary: WindowManager.LayoutParams? = null

    private var combinedView: LinearLayout? = null
    private var combinedParams: WindowManager.LayoutParams? = null
    private var combinedViewSecondary: LinearLayout? = null
    private var combinedParamsSecondary: WindowManager.LayoutParams? = null

    private var isTargetAppFullscreen = false
    private var currentForegroundPackage = ""
    private var activeFullscreenPackage = ""

    private var handler: Handler? = null
    private var packageCheckerRunnable: Runnable? = null
    private var usageStatsManager: UsageStatsManager? = null

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

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lynk Floating Service")
            .setContentText("Служба плавающих кнопок активна")
            .setSmallIcon(R.drawable.ic_menu)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 2000

        val usageEvents = usm.queryEvents(beginTime, endTime)
        val event = UsageEvents.Event()
        var currentApp = currentForegroundPackage

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentApp = event.packageName
            }
        }

        if (currentApp != null && currentApp != currentForegroundPackage) {
            handlePackageChange(currentApp)
        }
    }

    private fun handlePackageChange(newPackage: String) {
        if (isTargetAppFullscreen &&
            newPackage != activeFullscreenPackage &&
            newPackage != packageName &&
            newPackage != "com.android.launcher3" &&
            newPackage != "com.android.systemui"
        ) {
            isTargetAppFullscreen = false
            activeFullscreenPackage = ""
        }

        currentForegroundPackage = newPackage
        updateOverlayButtons()
    }

    fun updateOverlayButtons() {
        val separateEnabled = prefs.getBoolean("separate_buttons_enabled", false)

        if (separateEnabled) {
            hideCombinedOverlay()
            updateQuickLaunchButton()
            checkFullscreenCondition()
            updateHomeButton()
            updateBackButton()
            updateRefreshButton()
        } else {
            hideSeparateButtons()
            updateCombinedOverlay()
        }
    }

    private fun hideSeparateButtons() {
        quickLaunchView?.let { defaultWindowManager.removeView(it) }
        quickLaunchView = null
        quickLaunchViewSecondary?.let { secondaryWindowManager?.removeView(it) }
        quickLaunchViewSecondary = null

        fullscreenToggleView?.let { defaultWindowManager.removeView(it) }
        fullscreenToggleView = null
        fullscreenToggleViewSecondary?.let { secondaryWindowManager?.removeView(it) }
        fullscreenToggleViewSecondary = null

        homeView?.let { defaultWindowManager.removeView(it) }
        homeView = null
        homeViewSecondary?.let { secondaryWindowManager?.removeView(it) }
        homeViewSecondary = null

        backView?.let { defaultWindowManager.removeView(it) }
        backView = null
        backViewSecondary?.let { secondaryWindowManager?.removeView(it) }
        backViewSecondary = null

        refreshView?.let { defaultWindowManager.removeView(it) }
        refreshView = null
        refreshViewSecondary?.let { secondaryWindowManager?.removeView(it) }
        refreshViewSecondary = null
    }

    private fun hideCombinedOverlay() {
        combinedView?.let { defaultWindowManager.removeView(it) }
        combinedView = null

        combinedViewSecondary?.let { secondaryWindowManager?.removeView(it) }
        combinedViewSecondary = null
    }

    private fun updateCombinedOverlay() {
        val showQuickLaunch = prefs.getBoolean("quick_launch_enabled", false)
        val fullscreenApps = prefs.getStringSet("fullscreen_apps", Collections.emptySet()) ?: emptySet()
        val isCurrentAppTarget = fullscreenApps.contains(currentForegroundPackage)
        val showFullscreen = (prefs.getBoolean("fullscreen_overlay_enabled", false) && isCurrentAppTarget) || isTargetAppFullscreen
        val showHome = prefs.getBoolean("home_navigator_enabled", false)
        val showBack = prefs.getBoolean("back_navigator_enabled", false)
        val showRefresh = prefs.getBoolean("refresh_navigator_enabled", false)

        if (!showQuickLaunch && !showFullscreen && !showHome && !showBack && !showRefresh) {
            hideCombinedOverlay()
            return
        }

        val opacityPercent = prefs.getInt("opacity_percent", 85)
        val alphaFloat = (opacityPercent.coerceIn(10, 100)) / 100.0f

        val sizeDp = prefs.getInt("combined_button_size", 48)
        val sizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, sizeDp.toFloat(), resources.displayMetrics
        ).toInt()

        hideCombinedOverlay()

        val x = prefs.getInt("combined_pos_x", 0)
        val y = prefs.getInt("combined_pos_y", 200)
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val distLeft = x
        val distRight = max(0, screenWidth - x)
        val distTop = y
        val distBottom = max(0, screenHeight - y)
        val minDist = min(min(distLeft, distRight), min(distTop, distBottom))
        val isHorizontal = (minDist == distTop || minDist == distBottom)

        combinedView = createCombinedLinearLayout(this, showQuickLaunch, showFullscreen, showHome, showBack, showRefresh, sizePx, isHorizontal)
        combinedView?.alpha = alphaFloat

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
            this.alpha = alphaFloat
        }
        combinedParams = params

        defaultWindowManager.addView(combinedView, params)

        val secWM = secondaryWindowManager
        val secContext = secondaryContext
        if (secWM != null && secWM != defaultWindowManager && secContext != null) {
            combinedViewSecondary = createCombinedLinearLayout(secContext, showQuickLaunch, showFullscreen, showHome, showBack, showRefresh, sizePx, isHorizontal)
            combinedViewSecondary?.alpha = alphaFloat
            val marginPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics).toInt()
            val numButtons = (if (showQuickLaunch) 1 else 0) + (if (showFullscreen) 1 else 0) + (if (showHome) 1 else 0) + (if (showBack) 1 else 0) + (if (showRefresh) 1 else 0)
            val estWidth = if (isHorizontal) (sizePx + 2 * marginPx) * numButtons else (sizePx + 2 * marginPx)
            val estHeight = if (isHorizontal) (sizePx + 2 * marginPx) else (sizePx + 2 * marginPx) * numButtons

            val paramsSec = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = screenWidth - x - estWidth
                this.y = screenHeight - y - estHeight
                this.alpha = alphaFloat
            }
            combinedParamsSecondary = paramsSec
            secWM.addView(combinedViewSecondary, paramsSec)
        }

        setupCombinedTouches()
    }

    private fun setupCombinedTouches() {
        val cv = combinedView ?: return
        val params = combinedParams ?: return
        val showQuickLaunch = prefs.getBoolean("quick_launch_enabled", false)
        val fullscreenApps = prefs.getStringSet("fullscreen_apps", Collections.emptySet()) ?: emptySet()
        val isCurrentAppTarget = fullscreenApps.contains(currentForegroundPackage)
        val showFullscreen = (prefs.getBoolean("fullscreen_overlay_enabled", false) && isCurrentAppTarget) || isTargetAppFullscreen
        val showHome = prefs.getBoolean("home_navigator_enabled", false)
        val showBack = prefs.getBoolean("back_navigator_enabled", false)
        val showRefresh = prefs.getBoolean("refresh_navigator_enabled", false)

        var index = 0
        if (showQuickLaunch && cv.childCount > index) {
            val qlItem = cv.getChildAt(index++)
            setupDragAndSnapTouch(qlItem, cv, params, combinedViewSecondary, combinedParamsSecondary, { showQuickLaunchMenu() }, "combined")
        }
        if (showFullscreen && cv.childCount > index) {
            val fsItem = cv.getChildAt(index++)
            setupDragAndSnapTouch(fsItem, cv, params, combinedViewSecondary, combinedParamsSecondary, { toggleFullscreen() }, "combined")
        }
        if (showHome && cv.childCount > index) {
            val homeItem = cv.getChildAt(index++)
            setupDragAndSnapTouch(homeItem, cv, params, combinedViewSecondary, combinedParamsSecondary, { performHomeAction() }, "combined")
        }
        if (showBack && cv.childCount > index) {
            val backItem = cv.getChildAt(index++)
            setupDragAndSnapTouch(backItem, cv, params, combinedViewSecondary, combinedParamsSecondary, { performBackAction() }, "combined")
        }
        if (showRefresh && cv.childCount > index) {
            val refreshItem = cv.getChildAt(index)
            setupDragAndSnapTouch(refreshItem, cv, params, combinedViewSecondary, combinedParamsSecondary, { performRefreshAction() }, "combined")
        }
    }

    private fun createCombinedLinearLayout(
        context: Context,
        showQuickLaunch: Boolean,
        showFullscreen: Boolean,
        showHome: Boolean,
        showBack: Boolean,
        showRefresh: Boolean,
        sizePx: Int,
        isHorizontalMode: Boolean
    ): LinearLayout {
        val layout = LinearLayout(context).apply {
            orientation = if (isHorizontalMode) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val marginPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics).toInt()

        if (showQuickLaunch) {
            val item = LayoutInflater.from(context).inflate(R.layout.overlay_layout, layout, false)
            val iv = item.findViewById<ImageView>(R.id.overlay_image_view)
            iv.setImageResource(R.drawable.ic_menu)
            iv.layoutParams.width = sizePx
            iv.layoutParams.height = sizePx

            val lp = item.layoutParams as LinearLayout.LayoutParams
            if (isHorizontalMode) lp.setMargins(marginPx, 0, marginPx, 0) else lp.setMargins(0, marginPx, 0, marginPx)
            layout.addView(item)
        }

        if (showFullscreen) {
            val item = LayoutInflater.from(context).inflate(R.layout.overlay_layout, layout, false)
            val iv = item.findViewById<ImageView>(R.id.overlay_image_view)
            iv.setImageResource(if (isTargetAppFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen_enter)
            iv.layoutParams.width = sizePx
            iv.layoutParams.height = sizePx

            val lp = item.layoutParams as LinearLayout.LayoutParams
            if (isHorizontalMode) lp.setMargins(marginPx, 0, marginPx, 0) else lp.setMargins(0, marginPx, 0, marginPx)
            layout.addView(item)
        }

        if (showHome) {
            val item = LayoutInflater.from(context).inflate(R.layout.overlay_layout, layout, false)
            val iv = item.findViewById<ImageView>(R.id.overlay_image_view)
            iv.setImageResource(R.drawable.ic_home)
            iv.layoutParams.width = sizePx
            iv.layoutParams.height = sizePx

            val lp = item.layoutParams as LinearLayout.LayoutParams
            if (isHorizontalMode) lp.setMargins(marginPx, 0, marginPx, 0) else lp.setMargins(0, marginPx, 0, marginPx)
            layout.addView(item)
        }

        if (showBack) {
            val item = LayoutInflater.from(context).inflate(R.layout.overlay_layout, layout, false)
            val iv = item.findViewById<ImageView>(R.id.overlay_image_view)
            iv.setImageResource(R.drawable.ic_back)
            iv.layoutParams.width = sizePx
            iv.layoutParams.height = sizePx

            val lp = item.layoutParams as LinearLayout.LayoutParams
            if (isHorizontalMode) lp.setMargins(marginPx, 0, marginPx, 0) else lp.setMargins(0, marginPx, 0, marginPx)
            layout.addView(item)
        }

        if (showRefresh) {
            val item = LayoutInflater.from(context).inflate(R.layout.overlay_layout, layout, false)
            val iv = item.findViewById<ImageView>(R.id.overlay_image_view)
            iv.setImageResource(R.drawable.ic_refresh)
            iv.layoutParams.width = sizePx
            iv.layoutParams.height = sizePx

            val lp = item.layoutParams as LinearLayout.LayoutParams
            if (isHorizontalMode) lp.setMargins(marginPx, 0, marginPx, 0) else lp.setMargins(0, marginPx, 0, marginPx)
            layout.addView(item)
        }

        return layout
    }

    private fun checkFullscreenCondition() {
        val fullscreenApps = prefs.getStringSet("fullscreen_apps", Collections.emptySet()) ?: emptySet()
        val isCurrentAppTarget = fullscreenApps.contains(currentForegroundPackage)
        val shouldShowFullscreenBtn = prefs.getBoolean("fullscreen_overlay_enabled", false) && isCurrentAppTarget

        if (shouldShowFullscreenBtn || isTargetAppFullscreen) {
            showFullscreenToggleButton()
        } else {
            hideFullscreenToggleButton()
        }
    }

    fun updateQuickLaunchButton() {
        val isEnabled = prefs.getBoolean("quick_launch_enabled", false)
        if (!isEnabled) {
            quickLaunchView?.let { defaultWindowManager.removeView(it) }
            quickLaunchView = null
            quickLaunchViewSecondary?.let { secondaryWindowManager?.removeView(it) }
            quickLaunchViewSecondary = null
            return
        }

        val opacityPercent = prefs.getInt("ql_opacity_percent", prefs.getInt("opacity_percent", 85))
        val alphaFloat = (opacityPercent.coerceIn(10, 100)) / 100.0f

        val sizeDp = prefs.getInt("ql_button_size", 48)
        val sizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, sizeDp.toFloat(), resources.displayMetrics
        ).toInt()

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        if (quickLaunchView == null) {
            val qv = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
            qv.alpha = alphaFloat
            val iv = qv.findViewById<ImageView>(R.id.overlay_image_view)
            iv.setImageResource(R.drawable.ic_menu)

            val params = WindowManager.LayoutParams(
                sizePx, sizePx, layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = prefs.getInt("ql_pos_x", 0)
                y = prefs.getInt("ql_pos_y", 200)
                alpha = alphaFloat
            }

            quickLaunchParams = params
            quickLaunchView = qv
            defaultWindowManager.addView(qv, params)
        } else {
            val qv = quickLaunchView!!
            qv.alpha = alphaFloat
            val params = quickLaunchParams!!
            params.alpha = alphaFloat
            val iv = qv.findViewById<ImageView>(R.id.overlay_image_view)
            iv.layoutParams.width = sizePx
            iv.layoutParams.height = sizePx
            params.width = sizePx
            params.height = sizePx
            defaultWindowManager.updateViewLayout(qv, params)
        }

        val secWM = secondaryWindowManager
        val secContext = secondaryContext
        if (secWM != null && secWM != defaultWindowManager && secContext != null) {
            if (quickLaunchViewSecondary == null) {
                val qvSec = LayoutInflater.from(secContext).inflate(R.layout.overlay_layout, null)
                qvSec.alpha = alphaFloat
                val ivSec = qvSec.findViewById<ImageView>(R.id.overlay_image_view)
                ivSec.setImageResource(R.drawable.ic_menu)

                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels

                val paramsSec = WindowManager.LayoutParams(
                    sizePx, sizePx, layoutFlag,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = screenWidth - (quickLaunchParams?.x ?: 0) - sizePx
                    y = screenHeight - (quickLaunchParams?.y ?: 200) - sizePx
                    alpha = alphaFloat
                }

                quickLaunchParamsSecondary = paramsSec
                quickLaunchViewSecondary = qvSec
                secWM.addView(qvSec, paramsSec)
            } else {
                val qvSec = quickLaunchViewSecondary!!
                qvSec.alpha = alphaFloat
                val paramsSec = quickLaunchParamsSecondary!!
                paramsSec.alpha = alphaFloat
                val ivSec = qvSec.findViewById<ImageView>(R.id.overlay_image_view)
                ivSec.layoutParams.width = sizePx
                ivSec.layoutParams.height = sizePx
                paramsSec.width = sizePx
                paramsSec.height = sizePx
                secWM.updateViewLayout(qvSec, paramsSec)
            }
        }

        setupDragAndSnapTouch(
            quickLaunchView!!, quickLaunchView!!, quickLaunchParams!!,
            quickLaunchViewSecondary, quickLaunchParamsSecondary,
            { showQuickLaunchMenu() }, "ql"
        )
    }

    private fun showFullscreenToggleButton() {
        val opacityPercent = prefs.getInt("fs_opacity_percent", prefs.getInt("opacity_percent", 85))
        val alphaFloat = (opacityPercent.coerceIn(10, 100)) / 100.0f

        val sizeDp = prefs.getInt("fs_button_size", 48)
        val sizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, sizeDp.toFloat(), resources.displayMetrics
        ).toInt()

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        if (fullscreenToggleView != null) {
            val fv = fullscreenToggleView!!
            fv.alpha = alphaFloat
            val params = fullscreenToggleParams!!
            params.alpha = alphaFloat
            val iv = fv.findViewById<ImageView>(R.id.overlay_image_view)
            iv.setImageResource(if (isTargetAppFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen_enter)
            iv.layoutParams.width = sizePx
            iv.layoutParams.height = sizePx
            params.width = sizePx
            params.height = sizePx
            defaultWindowManager.updateViewLayout(fv, params)
        } else {
            val fv = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
            fv.alpha = alphaFloat
            val iv = fv.findViewById<ImageView>(R.id.overlay_image_view)
            iv.setImageResource(if (isTargetAppFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen_enter)

            val params = WindowManager.LayoutParams(
                sizePx, sizePx, layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = prefs.getInt("fs_pos_x", 100)
                y = prefs.getInt("fs_pos_y", 100)
                alpha = alphaFloat
            }

            fullscreenToggleParams = params
            fullscreenToggleView = fv
            defaultWindowManager.addView(fv, params)
        }

        val secWM = secondaryWindowManager
        val secContext = secondaryContext
        if (secWM != null && secWM != defaultWindowManager && secContext != null) {
            if (fullscreenToggleViewSecondary != null) {
                val fvSec = fullscreenToggleViewSecondary!!
                fvSec.alpha = alphaFloat
                val paramsSec = fullscreenToggleParamsSecondary!!
                paramsSec.alpha = alphaFloat
                val ivSec = fvSec.findViewById<ImageView>(R.id.overlay_image_view)
                ivSec.setImageResource(if (isTargetAppFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen_enter)
                ivSec.layoutParams.width = sizePx
                ivSec.layoutParams.height = sizePx
                paramsSec.width = sizePx
                paramsSec.height = sizePx
                secWM.updateViewLayout(fvSec, paramsSec)
            } else {
                val fvSec = LayoutInflater.from(secContext).inflate(R.layout.overlay_layout, null)
                fvSec.alpha = alphaFloat
                val ivSec = fvSec.findViewById<ImageView>(R.id.overlay_image_view)
                ivSec.setImageResource(if (isTargetAppFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen_enter)

                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels

                val paramsSec = WindowManager.LayoutParams(
                    sizePx, sizePx, layoutFlag,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = screenWidth - (fullscreenToggleParams?.x ?: 100) - sizePx
                    y = screenHeight - (fullscreenToggleParams?.y ?: 100) - sizePx
                    alpha = alphaFloat
                }

                fullscreenToggleParamsSecondary = paramsSec
                fullscreenToggleViewSecondary = fvSec
                secWM.addView(fvSec, paramsSec)
            }
        }

        setupDragAndSnapTouch(
            fullscreenToggleView!!, fullscreenToggleView!!, fullscreenToggleParams!!,
            fullscreenToggleViewSecondary, fullscreenToggleParamsSecondary,
            { toggleFullscreen() }, "fs"
        )
    }

    private fun hideFullscreenToggleButton() {
        fullscreenToggleView?.let { defaultWindowManager.removeView(it) }
        fullscreenToggleView = null
        fullscreenToggleViewSecondary?.let { secondaryWindowManager?.removeView(it) }
        fullscreenToggleViewSecondary = null
    }

    private fun updateHomeButton() {
        val isEnabled = prefs.getBoolean("home_navigator_enabled", false)
        if (!isEnabled) {
            homeView?.let { defaultWindowManager.removeView(it) }
            homeView = null
            homeViewSecondary?.let { secondaryWindowManager?.removeView(it) }
            homeViewSecondary = null
            return
        }

        val opacityPercent = prefs.getInt("home_opacity_percent", prefs.getInt("opacity_percent", 85))
        val alphaFloat = (opacityPercent.coerceIn(10, 100)) / 100.0f

        val sizeDp = prefs.getInt("home_button_size", 48)
        val sizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, sizeDp.toFloat(), resources.displayMetrics
        ).toInt()

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        if (homeView == null) {
            val hv = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
            hv.alpha = alphaFloat
            val iv = hv.findViewById<ImageView>(R.id.overlay_image_view)
            iv.setImageResource(R.drawable.ic_home)

            val params = WindowManager.LayoutParams(
                sizePx, sizePx, layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = prefs.getInt("home_pos_x", 0)
                y = prefs.getInt("home_pos_y", 300)
                alpha = alphaFloat
            }

            homeParams = params
            homeView = hv
            defaultWindowManager.addView(hv, params)
        } else {
            val hv = homeView!!
            hv.alpha = alphaFloat
            val params = homeParams!!
            params.alpha = alphaFloat
            val iv = hv.findViewById<ImageView>(R.id.overlay_image_view)
            iv.layoutParams.width = sizePx
            iv.layoutParams.height = sizePx
            params.width = sizePx
            params.height = sizePx
            defaultWindowManager.updateViewLayout(hv, params)
        }

        val secWM = secondaryWindowManager
        val secContext = secondaryContext
        if (secWM != null && secWM != defaultWindowManager && secContext != null) {
            if (homeViewSecondary == null) {
                val hvSec = LayoutInflater.from(secContext).inflate(R.layout.overlay_layout, null)
                hvSec.alpha = alphaFloat
                val ivSec = hvSec.findViewById<ImageView>(R.id.overlay_image_view)
                ivSec.setImageResource(R.drawable.ic_home)

                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels

                val paramsSec = WindowManager.LayoutParams(
                    sizePx, sizePx, layoutFlag,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = screenWidth - (homeParams?.x ?: 0) - sizePx
                    y = screenHeight - (homeParams?.y ?: 300) - sizePx
                    alpha = alphaFloat
                }

                homeParamsSecondary = paramsSec
                homeViewSecondary = hvSec
                secWM.addView(hvSec, paramsSec)
            } else {
                val hvSec = homeViewSecondary!!
                hvSec.alpha = alphaFloat
                val paramsSec = homeParamsSecondary!!
                paramsSec.alpha = alphaFloat
                val ivSec = hvSec.findViewById<ImageView>(R.id.overlay_image_view)
                ivSec.layoutParams.width = sizePx
                ivSec.layoutParams.height = sizePx
                paramsSec.width = sizePx
                paramsSec.height = sizePx
                secWM.updateViewLayout(hvSec, paramsSec)
            }
        }

        setupDragAndSnapTouch(
            homeView!!, homeView!!, homeParams!!,
            homeViewSecondary, homeParamsSecondary,
            { performHomeAction() }, "home"
        )
    }

    private fun updateBackButton() {
        val isEnabled = prefs.getBoolean("back_navigator_enabled", false)
        if (!isEnabled) {
            backView?.let { defaultWindowManager.removeView(it) }
            backView = null
            backViewSecondary?.let { secondaryWindowManager?.removeView(it) }
            backViewSecondary = null
            return
        }

        val opacityPercent = prefs.getInt("back_opacity_percent", prefs.getInt("opacity_percent", 85))
        val alphaFloat = (opacityPercent.coerceIn(10, 100)) / 100.0f

        val sizeDp = prefs.getInt("back_button_size", 48)
        val sizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, sizeDp.toFloat(), resources.displayMetrics
        ).toInt()

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        if (backView == null) {
            val bv = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
            bv.alpha = alphaFloat
            val iv = bv.findViewById<ImageView>(R.id.overlay_image_view)
            iv.setImageResource(R.drawable.ic_back)

            val params = WindowManager.LayoutParams(
                sizePx, sizePx, layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = prefs.getInt("back_pos_x", 0)
                y = prefs.getInt("back_pos_y", 400)
                alpha = alphaFloat
            }

            backParams = params
            backView = bv
            defaultWindowManager.addView(bv, params)
        } else {
            val bv = backView!!
            bv.alpha = alphaFloat
            val params = backParams!!
            params.alpha = alphaFloat
            val iv = bv.findViewById<ImageView>(R.id.overlay_image_view)
            iv.layoutParams.width = sizePx
            iv.layoutParams.height = sizePx
            params.width = sizePx
            params.height = sizePx
            defaultWindowManager.updateViewLayout(bv, params)
        }

        val secWM = secondaryWindowManager
        val secContext = secondaryContext
        if (secWM != null && secWM != defaultWindowManager && secContext != null) {
            if (backViewSecondary == null) {
                val bvSec = LayoutInflater.from(secContext).inflate(R.layout.overlay_layout, null)
                bvSec.alpha = alphaFloat
                val ivSec = bvSec.findViewById<ImageView>(R.id.overlay_image_view)
                ivSec.setImageResource(R.drawable.ic_back)

                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels

                val paramsSec = WindowManager.LayoutParams(
                    sizePx, sizePx, layoutFlag,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = screenWidth - (backParams?.x ?: 0) - sizePx
                    y = screenHeight - (backParams?.y ?: 400) - sizePx
                    alpha = alphaFloat
                }

                backParamsSecondary = paramsSec
                backViewSecondary = bvSec
                secWM.addView(bvSec, paramsSec)
            } else {
                val bvSec = backViewSecondary!!
                bvSec.alpha = alphaFloat
                val paramsSec = backParamsSecondary!!
                paramsSec.alpha = alphaFloat
                val ivSec = bvSec.findViewById<ImageView>(R.id.overlay_image_view)
                ivSec.layoutParams.width = sizePx
                ivSec.layoutParams.height = sizePx
                paramsSec.width = sizePx
                paramsSec.height = sizePx
                secWM.updateViewLayout(bvSec, paramsSec)
            }
        }

        setupDragAndSnapTouch(
            backView!!, backView!!, backParams!!,
            backViewSecondary, backParamsSecondary,
            { performBackAction() }, "back"
        )
    }

    private fun updateRefreshButton() {
        val isEnabled = prefs.getBoolean("refresh_navigator_enabled", false)
        if (!isEnabled) {
            refreshView?.let { defaultWindowManager.removeView(it) }
            refreshView = null
            refreshViewSecondary?.let { secondaryWindowManager?.removeView(it) }
            refreshViewSecondary = null
            return
        }

        val opacityPercent = prefs.getInt("refresh_opacity_percent", prefs.getInt("opacity_percent", 85))
        val alphaFloat = (opacityPercent.coerceIn(10, 100)) / 100.0f

        val sizeDp = prefs.getInt("refresh_button_size", 48)
        val sizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, sizeDp.toFloat(), resources.displayMetrics
        ).toInt()

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        if (refreshView == null) {
            val rv = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
            rv.alpha = alphaFloat
            val iv = rv.findViewById<ImageView>(R.id.overlay_image_view)
            iv.setImageResource(R.drawable.ic_refresh)

            val params = WindowManager.LayoutParams(
                sizePx, sizePx, layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = prefs.getInt("refresh_pos_x", 0)
                y = prefs.getInt("refresh_pos_y", 500)
                alpha = alphaFloat
            }

            refreshParams = params
            refreshView = rv
            defaultWindowManager.addView(rv, params)
        } else {
            val rv = refreshView!!
            rv.alpha = alphaFloat
            val params = refreshParams!!
            params.alpha = alphaFloat
            val iv = rv.findViewById<ImageView>(R.id.overlay_image_view)
            iv.layoutParams.width = sizePx
            iv.layoutParams.height = sizePx
            params.width = sizePx
            params.height = sizePx
            defaultWindowManager.updateViewLayout(rv, params)
        }

        val secWM = secondaryWindowManager
        val secContext = secondaryContext
        if (secWM != null && secWM != defaultWindowManager && secContext != null) {
            if (refreshViewSecondary == null) {
                val rvSec = LayoutInflater.from(secContext).inflate(R.layout.overlay_layout, null)
                rvSec.alpha = alphaFloat
                val ivSec = rvSec.findViewById<ImageView>(R.id.overlay_image_view)
                ivSec.setImageResource(R.drawable.ic_refresh)

                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels

                val paramsSec = WindowManager.LayoutParams(
                    sizePx, sizePx, layoutFlag,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = screenWidth - (refreshParams?.x ?: 0) - sizePx
                    y = screenHeight - (refreshParams?.y ?: 500) - sizePx
                    alpha = alphaFloat
                }

                refreshParamsSecondary = paramsSec
                refreshViewSecondary = rvSec
                secWM.addView(rvSec, paramsSec)
            } else {
                val rvSec = refreshViewSecondary!!
                rvSec.alpha = alphaFloat
                val paramsSec = refreshParamsSecondary!!
                paramsSec.alpha = alphaFloat
                val ivSec = rvSec.findViewById<ImageView>(R.id.overlay_image_view)
                ivSec.layoutParams.width = sizePx
                ivSec.layoutParams.height = sizePx
                paramsSec.width = sizePx
                paramsSec.height = sizePx
                secWM.updateViewLayout(rvSec, paramsSec)
            }
        }

        setupDragAndSnapTouch(
            refreshView!!, refreshView!!, refreshParams!!,
            refreshViewSecondary, refreshParamsSecondary,
            { performRefreshAction() }, "refresh"
        )
    }

    private fun performHomeAction() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        } catch (_: Exception) {
        }
    }

    private fun performBackAction() {
        Thread {
            try {
                val inst = android.app.Instrumentation()
                inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            } catch (e: Exception) {
                try {
                    Runtime.getRuntime().exec("input keyevent 4")
                } catch (_: Exception) {
                }
            }
        }.start()
    }

    private fun performRefreshAction() {
        val updateIntent = Intent("ru.doGood.Lynk.ACTION_UPDATE_OVERLAY").apply {
            setPackage(packageName)
        }
        sendBroadcast(updateIntent)

        val refreshUiIntent = Intent("ru.doGood.Lynk.ACTION_REFRESH_UI").apply {
            setPackage(packageName)
        }
        sendBroadcast(refreshUiIntent)

        updateOverlayButtons()
    }

    private fun toggleFullscreen() {
        if (!isTargetAppFullscreen) {
            activeFullscreenPackage = currentForegroundPackage
        }

        isTargetAppFullscreen = !isTargetAppFullscreen

        fullscreenToggleView?.findViewById<ImageView>(R.id.overlay_image_view)?.setImageResource(
            if (isTargetAppFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen_enter
        )
        fullscreenToggleViewSecondary?.findViewById<ImageView>(R.id.overlay_image_view)?.setImageResource(
            if (isTargetAppFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen_enter
        )

        var targetPackage = if (isTargetAppFullscreen) currentForegroundPackage else activeFullscreenPackage
        if (targetPackage.isEmpty()) {
            targetPackage = currentForegroundPackage
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            try {
                if (isTargetAppFullscreen) {
                    val bundle = ActivityOptions.makeBasic()
                        .setLaunchDisplayId(SECONDARY_DISPLAY_ID)
                        .toBundle()
                    startActivity(launchIntent, bundle)
                } else {
                    startActivity(launchIntent)
                    activeFullscreenPackage = ""
                }
            } catch (e: Exception) {
                startActivity(launchIntent)
            }
        }

        updateOverlayButtons()
    }

    private fun showQuickLaunchMenu() {
        val targetPackages = prefs.getStringSet("quick_launch_apps", Collections.emptySet()) ?: emptySet()
        if (targetPackages.isEmpty()) {
            return
        }

        val packages = ArrayList(targetPackages)
        val pm = packageManager
        val appNames = Array(packages.size) { i ->
            try {
                pm.getApplicationLabel(pm.getApplicationInfo(packages[i], 0)).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                packages[i]
            }
        }

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Быстрый запуск")
            .setItems(appNames) { _, i ->
                val launchIntent = packageManager.getLaunchIntentForPackage(packages[i])
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                }
            }
            .create()

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        dialog.window?.setType(layoutFlag)
        dialog.show()
    }

    private fun setupDragAndSnapTouch(
        touchView: View,
        dragView: View,
        params: WindowManager.LayoutParams,
        dragViewSecondary: View?,
        paramsSecondary: WindowManager.LayoutParams?,
        onClick: Runnable?,
        prefPrefix: String
    ) {
        touchView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = (event.rawX - initialTouchX).toInt()
                        val deltaY = (event.rawY - initialTouchY).toInt()
                        val touchSlop = ViewConfiguration.get(this@ForegroundOverlayService).scaledTouchSlop
                        if (Math.abs(deltaX) > touchSlop || Math.abs(deltaY) > touchSlop) {
                            isClick = false
                        }
                        if (!isClick) {
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
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            onClick?.run()
                        } else {
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

                            prefs.edit()
                                .putInt("${prefPrefix}_pos_x", params.x)
                                .putInt("${prefPrefix}_pos_y", params.y)
                                .apply()
                        }
                        return true
                    }
                }
                return false
            }
        })
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
        hideSeparateButtons()
        hideCombinedOverlay()
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
