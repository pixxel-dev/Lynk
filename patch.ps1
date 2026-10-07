$content = Get-Content -Path "app/src/main/java/ru/doGood/Lynk/service/ForegroundOverlayService.kt" -Raw
$content = $content -replace 'hideProfileCombinedOverlay\(views\)\r?\n\r?\n\s*val defY', 'val defY'

# Replace the creation logic in updateProfileCombinedOverlay
$old_combined = 'val combinedView = createCombinedLinearLayout\(this, showQuickLaunch, showFullscreen, showHome, showBack, showRefresh, showFreeform, sizePx, isHorizontal, profileId\)\r?\n\s*combinedView\.visibility = View\.VISIBLE\r?\n\s*combinedView\.alpha = alphaFloat\r?\n\s*views\.combinedView = combinedView\r?\n\r?\n\s*val params = WindowManager\.LayoutParams\(\r?\n\s*estWidth,\r?\n\s*estHeight,\r?\n\s*WindowManager\.LayoutParams\.TYPE_APPLICATION_OVERLAY,\r?\n\s*WindowManager\.LayoutParams\.FLAG_NOT_FOCUSABLE or WindowManager\.LayoutParams\.FLAG_LAYOUT_IN_SCREEN,\r?\n\s*PixelFormat\.TRANSLUCENT\r?\n\s*\)\.apply \{\r?\n\s*gravity = Gravity\.TOP or Gravity\.START\r?\n\s*this\.x = x\r?\n\s*this\.y = y\r?\n\s*this\.alpha = alphaFloat\r?\n\s*\}\r?\n\s*views\.combinedParams = params\r?\n\r?\n\s*Log\.d\("LynkOverlay", "Adding overlay view to WindowManager\.\.\."\)\r?\n\s*defaultWindowManager\.addView\(views\.combinedView, params\)\r?\n\r?\n\s*val secWM = secondaryWindowManager\r?\n\s*val secContext = secondaryContext\r?\n\s*if \(secWM != null && secWM != defaultWindowManager && secContext != null\) \{\r?\n\s*val secView = createCombinedLinearLayout\(secContext, showQuickLaunch, showFullscreen, showHome, showBack, showRefresh, showFreeform, sizePx, isHorizontal, profileId\)\r?\n\s*secView\.visibility = View\.VISIBLE\r?\n\s*secView\.alpha = alphaFloat\r?\n\s*views\.combinedViewSecondary = secView\r?\n\r?\n\s*val paramsSec = WindowManager\.LayoutParams\(\r?\n\s*estWidth,\r?\n\s*estHeight,\r?\n\s*WindowManager\.LayoutParams\.TYPE_APPLICATION_OVERLAY,\r?\n\s*WindowManager\.LayoutParams\.FLAG_NOT_FOCUSABLE or WindowManager\.LayoutParams\.FLAG_LAYOUT_IN_SCREEN,\r?\n\s*PixelFormat\.TRANSLUCENT\r?\n\s*\)\.apply \{\r?\n\s*gravity = Gravity\.TOP or Gravity\.START\r?\n\s*this\.x = max\(100, screenWidth - x - estWidth\)\r?\n\s*this\.y = max\(300, screenHeight - y - estHeight\)\r?\n\s*this\.alpha = alphaFloat\r?\n\s*\}\r?\n\s*views\.combinedParamsSecondary = paramsSec\r?\n\s*Log\.d\("LynkOverlay", "Adding overlay view to WindowManager\.\.\."\)\r?\n\s*secWM\.addView\(views\.combinedViewSecondary, paramsSec\)\r?\n\s*\}'

$new_combined = 'if (views.combinedView == null) {
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
                secWM.addView(views.combinedViewSecondary, paramsSec)
            }
        } else {
            val cv = views.combinedView!!
            val tempCv = createCombinedLinearLayout(this, showQuickLaunch, showFullscreen, showHome, showBack, showRefresh, showFreeform, sizePx, isHorizontal, profileId)
            cv.removeAllViews()
            while (tempCv.childCount > 0) {
                val child = tempCv.getChildAt(0)
                tempCv.removeView(child)
                cv.addView(child)
            }
            cv.orientation = tempCv.orientation
            cv.alpha = 1f

            val params = views.combinedParams!!
            params.width = estWidth
            params.height = estHeight
            params.x = x
            params.y = y
            params.alpha = alphaFloat
            defaultWindowManager.updateViewLayout(cv, params)

            val secWM = secondaryWindowManager
            val secContext = secondaryContext
            val secView = views.combinedViewSecondary
            val paramsSec = views.combinedParamsSecondary
            if (secWM != null && secContext != null && secView != null && paramsSec != null) {
                val tempSecCv = createCombinedLinearLayout(secContext, showQuickLaunch, showFullscreen, showHome, showBack, showRefresh, showFreeform, sizePx, isHorizontal, profileId)
                secView.removeAllViews()
                while (tempSecCv.childCount > 0) {
                    val child = tempSecCv.getChildAt(0)
                    tempSecCv.removeView(child)
                    secView.addView(child)
                }
                secView.orientation = tempSecCv.orientation
                secView.alpha = 1f
                paramsSec.width = estWidth
                paramsSec.height = estHeight
                paramsSec.x = max(100, screenWidth - x - estWidth)
                paramsSec.y = max(300, screenHeight - y - estHeight)
                paramsSec.alpha = alphaFloat
                secWM.updateViewLayout(secView, paramsSec)
            }
        }'

$content = [System.Text.RegularExpressions.Regex]::Replace($content, $old_combined, $new_combined)

$content = $content -replace 'qv\.alpha = alphaFloat', 'qv.alpha = 1f'
$content = $content -replace 'qvSec\.alpha = alphaFloat', 'qvSec.alpha = 1f'
$content = $content -replace 'fv\.alpha = alphaFloat', 'fv.alpha = 1f'
$content = $content -replace 'fvSec\.alpha = alphaFloat', 'fvSec.alpha = 1f'
$content = $content -replace 'hv\.alpha = alphaFloat', 'hv.alpha = 1f'
$content = $content -replace 'hvSec\.alpha = alphaFloat', 'hvSec.alpha = 1f'
$content = $content -replace 'bv\.alpha = alphaFloat', 'bv.alpha = 1f'
$content = $content -replace 'bvSec\.alpha = alphaFloat', 'bvSec.alpha = 1f'
$content = $content -replace 'rv\.alpha = alphaFloat', 'rv.alpha = 1f'
$content = $content -replace 'rvSec\.alpha = alphaFloat', 'rvSec.alpha = 1f'
$content = $content -replace '(\bviews\.freeformView(?:Secondary)?)\.alpha = alphaFloat', '$1.alpha = 1f'

Set-Content -Path "app/src/main/java/ru/doGood/Lynk/service/ForegroundOverlayService.kt" -Value $content -Encoding UTF8
