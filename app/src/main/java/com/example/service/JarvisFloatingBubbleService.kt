package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.audio.SciFiSoundEffects

class JarvisFloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var rootLayout: FrameLayout? = null
    private var bubbleOrb: FrameLayout? = null
    private var menuBar: LinearLayout? = null
    private var isMenuExpanded: Boolean = false
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            buildFloatingView()
            windowManager?.addView(rootLayout, params)
            isRunning = true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding bubble view to WindowManager", e)
            stopSelf()
        }
    }

    override fun onDestroy() {
        try {
            if (rootLayout != null && rootLayout?.isAttachedToWindow == true) {
                windowManager?.removeView(rootLayout)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing bubble view", e)
        }
        isRunning = false
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildFloatingView() {
        val dm = resources.displayMetrics
        val density = dm.density

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (dm.widthPixels - (80 * density).toInt()).coerceAtLeast(0)
            y = (dm.heightPixels / 3)
        }

        rootLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Circular Bubble Orb
        bubbleOrb = FrameLayout(this).apply {
            val orbSize = (58 * density).toInt()
            layoutParams = FrameLayout.LayoutParams(orbSize, orbSize).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(Color.parseColor("#00F0FF"), Color.parseColor("#7928CA"))
                orientation = GradientDrawable.Orientation.TL_BR
                setStroke((2.5f * density).toInt(), Color.parseColor("#00F0FF"))
            }

            // Glow core
            addView(TextView(context).apply {
                text = "A"
                textSize = 22f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            })
        }

        // Expanded mini action menu
        menuBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            val pad = (6 * density).toInt()
            setPadding(pad * 2, pad, pad * 2, pad)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24 * density
                setColor(Color.parseColor("#E60D1117"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#00F0FF"))
            }
            val lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = (64 * density).toInt()
                gravity = Gravity.CENTER_VERTICAL
            }
            layoutParams = lp

            // Action: Voice
            addView(createMiniActionItem("🎙️", "ভয়েস") {
                SciFiSoundEffects.playCommandSuccess()
                collapseMenu()
                onVoiceTrigger?.invoke()
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("AUTO_START_VOICE", true)
                }
                startActivity(intent)
            })

            // Action: Screen Explain
            addView(createMiniActionItem("👁️", "স্ক্রিন") {
                SciFiSoundEffects.playRadarScan()
                collapseMenu()
                onScreenExplainTrigger?.invoke()
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("AUTO_EXPLAIN_SCREEN", true)
                }
                startActivity(intent)
            })

            // Action: Close Menu
            addView(createMiniActionItem("✖️", "") {
                collapseMenu()
            })
        }

        rootLayout?.addView(menuBar)
        rootLayout?.addView(bubbleOrb)

        // Draggable touch listener
        bubbleOrb?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                val p = params ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = p.x
                        initialY = p.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isClick = false
                        }
                        p.x = initialX + dx
                        p.y = initialY + dy
                        try {
                            windowManager?.updateViewLayout(rootLayout, p)
                        } catch (_: Exception) {}
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            toggleMenu()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun createMiniActionItem(icon: String, label: String, onClick: () -> Unit): View {
        val density = resources.displayMetrics.density
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            addView(TextView(context).apply {
                text = icon
                textSize = 16f
                gravity = Gravity.CENTER
            })
            if (label.isNotEmpty()) {
                addView(TextView(context).apply {
                    text = label
                    textSize = 10f
                    setTextColor(Color.parseColor("#00F0FF"))
                    gravity = Gravity.CENTER
                })
            }
            setOnClickListener { onClick() }
        }
    }

    private fun toggleMenu() {
        if (isMenuExpanded) {
            collapseMenu()
        } else {
            expandMenu()
        }
    }

    private fun expandMenu() {
        SciFiSoundEffects.playCommandSuccess()
        menuBar?.visibility = View.VISIBLE
        isMenuExpanded = true
        try {
            windowManager?.updateViewLayout(rootLayout, params)
        } catch (_: Exception) {}
    }

    private fun collapseMenu() {
        menuBar?.visibility = View.GONE
        isMenuExpanded = false
        try {
            windowManager?.updateViewLayout(rootLayout, params)
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "JarvisFloatingBubble"
        var isRunning: Boolean = false
            private set

        var onVoiceTrigger: (() -> Unit)? = null
        var onScreenExplainTrigger: (() -> Unit)? = null

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            try {
                val intent = Intent(context, JarvisFloatingBubbleService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start JarvisFloatingBubbleService", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, JarvisFloatingBubbleService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop JarvisFloatingBubbleService", e)
            }
        }
    }
}