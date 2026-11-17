package com.vgr.flasher

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.*

class MainActivity : Activity() {
    @Volatile private var running = false
    @Volatile private var unit = 350L
    @Volatile private var repeat = true
    private lateinit var cm: CameraManager
    private var camId: String? = null
    private lateinit var btn: Button

    private val morse = listOf("...-", "--.", ".-.") // V G R

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        camId = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad * 2, pad, pad)
            setBackgroundColor(Color.parseColor("#0B0D14"))
        }
        fun tv(t: String, size: Float, color: String) = TextView(this).apply {
            text = t; textSize = size; setTextColor(Color.parseColor(color)); gravity = Gravity.CENTER
        }
        root.addView(tv("Voyager 1 · one light-day", 22f, "#ECEAF3"))
        root.addView(tv("V  G  R", 44f, "#7C93FF"))
        root.addView(tv("···–    ––·    ·–·", 20f, "#9A9AA8"))

        btn = Button(this).apply { text = "Start"; textSize = 20f }
        root.addView(btn, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pad * 2 })

        val speedLabel = tv("Dot length: 350 ms", 14f, "#9A9AA8")
        root.addView(speedLabel, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pad })
        val seek = SeekBar(this).apply { max = 650; progress = 200 }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                unit = 150L + p; speedLabel.text = "Dot length: ${unit} ms"
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        root.addView(seek)

        val rep = CheckBox(this).apply {
            text = "Repeat"; isChecked = true; setTextColor(Color.parseColor("#ECEAF3"))
            setOnCheckedChangeListener { _, c -> repeat = c }
        }
        root.addView(rep)
        if (camId == null) root.addView(tv("No flashlight found on this device", 14f, "#FF8080"))
        setContentView(root)

        btn.setOnClickListener {
            if (running) { running = false } else { running = true; btn.text = "Stop"; startThread() }
        }
    }

    private fun torch(on: Boolean) {
        try { camId?.let { cm.setTorchMode(it, on) } } catch (_: Exception) {}
    }

    private fun startThread() {
        Thread {
            try {
                do {
                    for ((ci, letter) in morse.withIndex()) {
                        for (s in letter) {
                            if (!running) return@Thread
                            torch(true); Thread.sleep(if (s == '.') unit else 3 * unit)
                            torch(false); Thread.sleep(unit)
                        }
                        if (ci < morse.size - 1) Thread.sleep(2 * unit)
                    }
                    Thread.sleep(4 * unit)
                } while (running && repeat)
            } catch (_: InterruptedException) {
            } finally {
                torch(false); running = false
                runOnUiThread { btn.text = "Start" }
            }
        }.start()
    }

    override fun onPause() { super.onPause(); running = false; torch(false) }
}
