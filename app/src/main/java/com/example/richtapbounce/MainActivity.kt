package com.example.richtapbounce

import android.content.res.AssetManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.apprichtap.haptic.RichTapUtils
import com.commit451.gimbal.Gimbal
import com.example.richtapbounce.databinding.ActivityMainBinding
import com.example.richtapbounce.Box2d.OnCollisionListener
import org.jbox2d.dynamics.Body
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import kotlin.math.floor

class MainActivity : AppCompatActivity(), SensorEventListener {

    private val TAG = "RichTap-SAMPLE"
    private lateinit var binding: ActivityMainBinding

    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null
    private lateinit var gimbal: Gimbal

    private var ballIndex = 0
    // Based on experimentation dropping the ball from a maximum possible height
    // 小球从最高处落下碰地时的最大速度值，以此折算振动强度；可酌情调整
    private var maxFallingVelocity = 26.5F

    // Haptic effect description files
    private lateinit var heBallToBound: String
    private lateinit var heBallWithBall: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gimbal = Gimbal(this)
        gimbal.lock()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize RichTap SDK to play haptics
        RichTapUtils.getInstance().init(this)
        // Load haptics assets
        // 加载触感描述文件
        heBallToBound = loadHeFromAssets("wall.he")
        heBallWithBall = loadHeFromAssets("ball.he")

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // 监听碰撞事件（球与球、球与边界）
        binding.jboxContainer.setOnCollisionListener(onCollisionListener)

        repeat(2) {
            addOneBall()
        }

        binding.btnImpulse.setOnClickListener {
            binding.jboxContainer.onRandomChanged()
        }
        binding.btnAdd.setOnClickListener {
            if (binding.jboxContainer.childCount < 5) { // 5 balls at most
                addOneBall()
                binding.jboxContainer.requestLayout()
            }
        }
        binding.btnMinus.setOnClickListener {
            binding.jboxContainer.run {
                if (childCount > 1) {
                    removeOneBody()
                    requestLayout()
                }
            }
        }
    }

    private fun addOneBall() {
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.CENTER
        val imageView = ImageView(this)
        imageView.setImageResource(R.mipmap.richtap_logo)
        imageView.setTag(R.id.wd_view_circle_tag, true)
        imageView.id = ballIndex++
        binding.jboxContainer.addView(imageView, layoutParams)
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onDestroy() {
        RichTapUtils.getInstance().stop()
        RichTapUtils.getInstance().quit()

        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()

            R.id.about -> {
                AlertDialog.Builder(this).apply {
                    setTitle("About...")
                    setMessage("App Version: ${BuildConfig.VERSION_NAME}\n" +
                            "RichTap SDK: ${RichTapUtils.VERSION_NAME}")
                    setCancelable(true)
                    setPositiveButton("OK") { _, _ ->}
                    show()
                }
            }

            R.id.close -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gimbal.normalizeGravityEvent(event)
            // Multiply a coefficient to speed up the balls' falling
            // 为了加快小球下坠速度，X方向的重力系数放大6倍，Y方向的重力系数放大8倍
            binding.jboxContainer.changeWorldGravity(-event.values[0]*6, event.values[1]*8)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private val onCollisionListener = object : OnCollisionListener {
        override fun onCollisionEntered(viewIdA: Int, viewIdB: Int) {
            Log.i(TAG, "${getViewName(viewIdA)} is collided with ${getViewName(viewIdB)}, thread: ${Thread.currentThread().id}")
            if (isBall(viewIdA)) {
                if (isBall(viewIdB)) {
                    RichTapUtils.getInstance().playHaptic(heBallWithBall, 0)
                } else {
                    // Calculate vibration amplitude according to the velocity of the ball
                    // 根据小球碰撞时的速度动态计算振动的强度
                    val view = binding.jboxContainer.findViewById<View>(viewIdA)
                    RichTapUtils.getInstance().playHaptic(heBallToBound, 0, view.getAmplitude())
                }
            } else {
                if (isBall(viewIdB)) {
                    val view = binding.jboxContainer.findViewById<View>(viewIdB)
                    RichTapUtils.getInstance().playHaptic(heBallToBound, 0, view.getAmplitude())
                }
            }
        }

        override fun onCollisionExited(viewIdA: Int, viewIdB: Int) {
            Log.i(TAG, "${getViewName(viewIdA)} is collided with ${getViewName(viewIdB)} - exited")
        }
    }

    fun View.getAmplitude(): Int {
        val body = getTag(R.id.wd_view_body_tag) as Body
        val vel = body.linearVelocity.length()
        if (vel > maxFallingVelocity) maxFallingVelocity = vel
        return floor(vel / maxFallingVelocity * 255).toInt()
    }

    private fun getViewName(id: Int): String {
        return when (id) {
            R.id.physics_bound_left -> "Bound-left"
            R.id.physics_bound_right -> "Bound-right"
            R.id.physics_bound_top -> "Bound-top"
            R.id.physics_bound_bottom -> "Bound-bottom"
            else -> "Ball-$id"
        }
    }

    private fun isBall(id: Int): Boolean {
        return id != R.id.physics_bound_left && id != R.id.physics_bound_right &&
            id != R.id.physics_bound_top && id != R.id.physics_bound_bottom
    }

    private fun loadHeFromAssets(fileName: String): String {
        val sb = StringBuilder()
        try {
            val stream = assets.open(fileName, AssetManager.ACCESS_STREAMING)
            val reader = BufferedReader(InputStreamReader(stream, "utf-8"))
            reader.use {
                reader.forEachLine {
                    sb.append(it)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return sb.toString()
    }
}