package net.sath.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import net.sath.data.PreferencesManager
import kotlin.math.abs

class FloatingOverlayService : Service() {

    companion object {
        @Volatile
        var isOverlayRunning = false
            private set

        const val ACTION_UPDATE_SIZE_FREEZE = "net.sath.action.UPDATE_SIZE_FREEZE"
        const val ACTION_UPDATE_SIZE_GHOST = "net.sath.action.UPDATE_SIZE_GHOST"
        const val ACTION_UPDATE_SIZE_TELE = "net.sath.action.UPDATE_SIZE_TELE"
        const val ACTION_UPDATE_ALPHA_FREEZE = "net.sath.action.UPDATE_ALPHA_FREEZE"
        const val ACTION_UPDATE_ALPHA_GHOST = "net.sath.action.UPDATE_ALPHA_GHOST"
        const val ACTION_UPDATE_ALPHA_TELE = "net.sath.action.UPDATE_ALPHA_TELE"
        const val ACTION_UPDATE_TOGGLES = "net.sath.action.UPDATE_TOGGLES"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: PreferencesManager
    private var toneGenerator: ToneGenerator? = null

    // Individual views & params so each button is independently draggable
    private var freezeView: TextView? = null
    private var freezeParams: WindowManager.LayoutParams? = null
    private var isFreezeActive = false

    private var ghostView: TextView? = null
    private var ghostParams: WindowManager.LayoutParams? = null
    private var isGhostActive = false

    private var teleView: TextView? = null
    private var teleParams: WindowManager.LayoutParams? = null
    private var isTeleActive = false

    private var penView: TextView? = null
    private var penParams: WindowManager.LayoutParams? = null

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let { action ->
                when (action) {
                    ACTION_UPDATE_SIZE_FREEZE, ACTION_UPDATE_SIZE_GHOST,
                    ACTION_UPDATE_SIZE_TELE, ACTION_UPDATE_ALPHA_FREEZE,
                    ACTION_UPDATE_ALPHA_GHOST, ACTION_UPDATE_ALPHA_TELE,
                    ACTION_UPDATE_TOGGLES -> {
                        updateAllWidgetsAppearance()
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            toneGenerator = null
        }

        val filter = IntentFilter().apply {
            addAction(ACTION_UPDATE_SIZE_FREEZE)
            addAction(ACTION_UPDATE_SIZE_GHOST)
            addAction(ACTION_UPDATE_SIZE_TELE)
            addAction(ACTION_UPDATE_ALPHA_FREEZE)
            addAction(ACTION_UPDATE_ALPHA_GHOST)
            addAction(ACTION_UPDATE_ALPHA_TELE)
            addAction(ACTION_UPDATE_TOGGLES)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(updateReceiver, filter)
        }

        initAllFloatingButtons()
        isOverlayRunning = true
    }

    private fun playFeedbackSound(isOn: Boolean) {
        try {
            toneGenerator?.startTone(if (isOn) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_PROP_BEEP2, 120)
        } catch (ignored: Exception) {}
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun getOverlayLayoutType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun initAllFloatingButtons() {
        val cfg = prefs.loadConfig()

        // 1. HUD Handle Button
        createHudHandleButton(cfg.switchPen)

        // 2. Freeze Button (Independent Window)
        createFreezeButton(cfg.switchFreeze, cfg.sizeFreeze, cfg.alphaFreeze)

        // 3. Ghost Button (Independent Window)
        createGhostButton(cfg.switchGhost, cfg.sizeGhost, cfg.alphaGhost)

        // 4. Teleport Button (Independent Window)
        createTeleportButton(cfg.switchTele, cfg.sizeTele, cfg.alphaTele)
    }

    private fun createHudHandleButton(isVisible: Boolean) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayLayoutType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(20)
            y = dpToPx(80)
        }
        penParams = params

        val view = TextView(this).apply {
            text = "⚡ HUD"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8))
            background = GradientDrawable().apply {
                setColor(0xEE0A1322.toInt())
                setStroke(dpToPx(1), 0xFF5C9DFF.toInt())
                cornerRadius = dpToPx(16).toFloat()
            }
            visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        attachDraggableListener(view, params) {
            // Optional tap behavior for HUD
            playFeedbackSound(true)
        }

        penView = view
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createFreezeButton(isVisible: Boolean, sizeDp: Int, alphaPercent: Int) {
        val sizePx = dpToPx(sizeDp)
        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            getOverlayLayoutType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(20)
            y = dpToPx(160)
        }
        freezeParams = params

        val view = TextView(this).apply {
            text = if (isFreezeActive) "ACTIVE" else "FREEZE"
            textSize = 13f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = makeCircleDrawable(if (isFreezeActive) 0xFF00E676.toInt() else 0xFFFF4554.toInt())
            alpha = alphaPercent / 100f
            visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        attachDraggableListener(view, params) {
            isFreezeActive = !isFreezeActive
            playFeedbackSound(isFreezeActive)
            view.text = if (isFreezeActive) "ACTIVE" else "FREEZE"
            view.background = makeCircleDrawable(if (isFreezeActive) 0xFF00E676.toInt() else 0xFFFF4554.toInt())

            val intent = Intent(this@FloatingOverlayService, GameBoosterVpnService::class.java).apply {
                action = GameBoosterVpnService.ACTION_FREEZE
                putExtra("enabled", isFreezeActive)
            }
            startService(intent)
        }

        freezeView = view
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createGhostButton(isVisible: Boolean, sizeDp: Int, alphaPercent: Int) {
        val sizePx = dpToPx(sizeDp)
        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            getOverlayLayoutType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(20)
            y = dpToPx(280)
        }
        ghostParams = params

        val view = TextView(this).apply {
            text = "GHOST"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = makeCircleDrawable(if (isGhostActive) 0xFFC084FC.toInt() else 0xDD1E1B4B.toInt())
            alpha = alphaPercent / 100f
            visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        attachDraggableListener(view, params) {
            isGhostActive = !isGhostActive
            playFeedbackSound(isGhostActive)
            view.background = makeCircleDrawable(if (isGhostActive) 0xFFC084FC.toInt() else 0xDD1E1B4B.toInt())

            val intent = Intent(this@FloatingOverlayService, GameBoosterVpnService::class.java).apply {
                action = GameBoosterVpnService.ACTION_GHOST
                putExtra("enabled", isGhostActive)
            }
            startService(intent)
        }

        ghostView = view
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createTeleportButton(isVisible: Boolean, sizeDp: Int, alphaPercent: Int) {
        val sizePx = dpToPx(sizeDp)
        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            getOverlayLayoutType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(20)
            y = dpToPx(400)
        }
        teleParams = params

        val view = TextView(this).apply {
            text = "TELE"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = makeCircleDrawable(if (isTeleActive) 0xFF31D7FF.toInt() else 0xDD083344.toInt())
            alpha = alphaPercent / 100f
            visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        attachDraggableListener(view, params) {
            isTeleActive = !isTeleActive
            playFeedbackSound(isTeleActive)
            view.background = makeCircleDrawable(if (isTeleActive) 0xFF31D7FF.toInt() else 0xDD083344.toInt())

            val intent = Intent(this@FloatingOverlayService, GameBoosterVpnService::class.java).apply {
                action = GameBoosterVpnService.ACTION_TELEPORT
                putExtra("enabled", isTeleActive)
            }
            startService(intent)
        }

        teleView = view
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun attachDraggableListener(
        view: View,
        params: WindowManager.LayoutParams,
        onClick: () -> Unit
    ) {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = true

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false
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
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (abs(dx) > 10 || abs(dy) > 10) {
                            isClick = false
                        }
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager.updateViewLayout(view, params)
                        } catch (ignored: Exception) {}
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            onClick()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun makeCircleDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke(dpToPx(2), 0xCCFFFFFF.toInt())
        }
    }

    private fun updateAllWidgetsAppearance() {
        val cfg = prefs.loadConfig()

        // 1. Freeze Button Update
        freezeView?.let { view ->
            freezeParams?.let { params ->
                val sz = dpToPx(cfg.sizeFreeze)
                params.width = sz
                params.height = sz
                view.alpha = cfg.alphaFreeze / 100f
                view.visibility = if (cfg.switchFreeze) View.VISIBLE else View.GONE
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (ignored: Exception) {}
            }
        }

        // 2. Ghost Button Update
        ghostView?.let { view ->
            ghostParams?.let { params ->
                val sz = dpToPx(cfg.sizeGhost)
                params.width = sz
                params.height = sz
                view.alpha = cfg.alphaGhost / 100f
                view.visibility = if (cfg.switchGhost) View.VISIBLE else View.GONE
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (ignored: Exception) {}
            }
        }

        // 3. Teleport Button Update
        teleView?.let { view ->
            teleParams?.let { params ->
                val sz = dpToPx(cfg.sizeTele)
                params.width = sz
                params.height = sz
                view.alpha = cfg.alphaTele / 100f
                view.visibility = if (cfg.switchTele) View.VISIBLE else View.GONE
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (ignored: Exception) {}
            }
        }

        // 4. Pen Button Update
        penView?.let { view ->
            view.visibility = if (cfg.switchPen) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroy() {
        isOverlayRunning = false
        try {
            unregisterReceiver(updateReceiver)
        } catch (ignored: Exception) {}

        freezeView?.let { safeRemove(it) }
        ghostView?.let { safeRemove(it) }
        teleView?.let { safeRemove(it) }
        penView?.let { safeRemove(it) }

        toneGenerator?.release()
        super.onDestroy()
    }

    private fun safeRemove(view: View) {
        try {
            windowManager.removeView(view)
        } catch (ignored: Exception) {}
    }
}
