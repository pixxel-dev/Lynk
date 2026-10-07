import re

with open('app/src/main/java/ru/doGood/Lynk/service/ForegroundOverlayService.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Change view.alpha = alphaFloat to view.alpha = 1f everywhere so we don't double-apply alpha.
# Note: we should only change the one for the View, not the params!
content = re.sub(r'(\w+)\.alpha = alphaFloat', lambda m: f'{m.group(1)}.alpha = 1f' if 'params' not in m.group(1).lower() else m.group(0), content)

# But wait, let's just make it simpler: we know exactly which lines:
# qv.alpha = alphaFloat -> qv.alpha = 1f
for var in ['combinedView', 'combinedViewSecondary', 'qv', 'qvSec', 'fv', 'fvSec', 'hv', 'hvSec', 'bv', 'bvSec', 'rv', 'rvSec']:
    content = content.replace(f'{var}.alpha = alphaFloat', f'{var}.alpha = 1f')

# 2. Add populateCombinedLinearLayout
populate_func = """
    private fun populateCombinedLinearLayout(
        container: LinearLayout,
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
    ) {
        val colorHex = getProfileString(profileId, "button_color", "button_color", "#7C4DFF")
        val shapeStr = getProfileString(profileId, "button_shape", "button_shape", "CIRCLE")
        
        container.orientation = if (isHorizontal) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL

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
    }

    private fun createCombinedLinearLayout(
"""
content = content.replace("    private fun createCombinedLinearLayout(", populate_func)

# rewrite createCombinedLinearLayout to use populateCombinedLinearLayout
old_create = re.search(r'    private fun createCombinedLinearLayout\(\n.*?\n    \): LinearLayout \{\n.*?\n        return container\n    \}', content, re.DOTALL)
if old_create:
    new_create = """    private fun createCombinedLinearLayout(
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
        val container = LinearLayout(context)
        populateCombinedLinearLayout(container, context, showQuickLaunch, showFullscreen, showHome, showBack, showRefresh, showFreeform, sizePx, isHorizontal, profileId)
        return container
    }"""
    content = content.replace(old_create.group(0), new_create)

# Modify updateProfileCombinedOverlay
old_combined = re.search(r'        val estHeight = if \(isHorizontal\) sizePx else max\(sizePx \* numButtons, sizePx\)\n\n        val combinedView = createCombinedLinearLayout\(.*?setupCombinedTouchesForProfile\(profileId, views\)\n    \}', content, re.DOTALL)
if old_combined:
    new_combined = """        val estHeight = if (isHorizontal) sizePx else max(sizePx * numButtons, sizePx)

        if (views.combinedView != null) {
            val combinedView = views.combinedView!!
            combinedView.removeAllViews()
            populateCombinedLinearLayout(combinedView, this, showQuickLaunch, showFullscreen, showHome, showBack, showRefresh, showFreeform, sizePx, isHorizontal, profileId)
            
            val params = views.combinedParams!!
            params.width = estWidth
            params.height = estHeight
            params.x = x
            params.y = y
            params.alpha = alphaFloat
            defaultWindowManager.updateViewLayout(combinedView, params)
            
            val secWM = secondaryWindowManager
            val secView = views.combinedViewSecondary
            val paramsSec = views.combinedParamsSecondary
            if (secWM != null && secView != null && paramsSec != null && secWM != defaultWindowManager && secondaryContext != null) {
                secView.removeAllViews()
                populateCombinedLinearLayout(secView, secondaryContext!!, showQuickLaunch, showFullscreen, showHome, showBack, showRefresh, showFreeform, sizePx, isHorizontal, profileId)
                paramsSec.width = estWidth
                paramsSec.height = estHeight
                paramsSec.x = max(100, screenWidth - x - estWidth)
                paramsSec.y = max(300, screenHeight - y - estHeight)
                paramsSec.alpha = alphaFloat
                secWM.updateViewLayout(secView, paramsSec)
            }
            setupCombinedTouchesForProfile(profileId, views)
            return
        }

        val combinedView = createCombinedLinearLayout(this, showQuickLaunch, showFullscreen, showHome, showBack, showRefresh, showFreeform, sizePx, isHorizontal, profileId)
        combinedView.visibility = View.VISIBLE
        combinedView.alpha = 1f
        views.combinedView = combinedView

        val params = WindowManager.LayoutParams(
            estWidth,
            estHeight,
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

        Log.d("LynkOverlay", "Adding overlay view to WindowManager...")
        defaultWindowManager.addView(views.combinedView, params)

        val secWM = secondaryWindowManager
        val secContext = secondaryContext
        if (secWM != null && secWM != defaultWindowManager && secContext != null) {
            val secView = createCombinedLinearLayout(secContext, showQuickLaunch, showFullscreen, showHome, showBack, showRefresh, showFreeform, sizePx, isHorizontal, profileId)
            secView.visibility = View.VISIBLE
            secView.alpha = 1f
            views.combinedViewSecondary = secView

            val paramsSec = WindowManager.LayoutParams(
                estWidth,
                estHeight,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = max(100, screenWidth - x - estWidth)
                this.y = max(300, screenHeight - y - estHeight)
                this.alpha = alphaFloat
            }
            views.combinedParamsSecondary = paramsSec
            Log.d("LynkOverlay", "Adding overlay view to WindowManager...")
            secWM.addView(views.combinedViewSecondary, paramsSec)
        }

        setupCombinedTouchesForProfile(profileId, views)
    }"""
    content = content.replace(old_combined.group(0), new_combined)

# Remove hideProfileCombinedOverlay(views) from updateProfileCombinedOverlay!
content = content.replace("        hideProfileCombinedOverlay(views)\n\n        val defY", "        val defY")

with open('app/src/main/java/ru/doGood/Lynk/service/ForegroundOverlayService.kt', 'w', encoding='utf-8') as f:
    f.write(content)
