package com.example.pixfuse.screen

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.pixfuse.R
import kotlin.math.sin
import kotlin.random.Random

class MenuActivity : AppCompatActivity() {

    private lateinit var logoImage: ImageView
    private lateinit var playButton: Button
    private lateinit var highScoresButton: Button
    private lateinit var settingsButton: Button
    private lateinit var aboutButton: Button
    private lateinit var containerLayout: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide action bar
        supportActionBar?.hide()

        // Set fullscreen (hide status bar)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Tạo container layout
        containerLayout = ConstraintLayout(this)
        setContentView(containerLayout)

        // Thêm AnimatedBackgroundView làm nền
        val backgroundView = AnimatedBackgroundView(this)
        containerLayout.addView(backgroundView, ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        ))

        // Thiết lập views
        setupViews()
        setupAnimations()
        setupClickListeners()
    }

    private fun setupViews() {
        // Logo instead of title text
        logoImage = ImageView(this).apply {
            setImageResource(R.drawable.logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }

        // Buttons
        playButton = createStyledButton("PLAY GAME", Color.parseColor("#EDC22E"))
        highScoresButton = createStyledButton("HIGH SCORES", Color.parseColor("#EE4C2C"))
        settingsButton = createStyledButton("SETTINGS", Color.parseColor("#A6C5FF"))
        aboutButton = createStyledButton("ABOUT", Color.parseColor("#6495ED"))

        // Thêm views vào container
        containerLayout.addView(logoImage)
        containerLayout.addView(playButton)
        containerLayout.addView(highScoresButton)
        containerLayout.addView(settingsButton)
        containerLayout.addView(aboutButton)

        // Layout views
        layoutViews()
    }

    private fun createStyledButton(text: String, color: Int): Button {
        return Button(this).apply {
            this.text = text
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = createButtonDrawable(color)
            elevation = 8f
            isAllCaps = true
        }
    }

    private fun createButtonDrawable(color: Int): Drawable {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setColor(color)
            setStroke(4, Color.parseColor("#FFFFFF"))
        }
        return drawable
    }

    private fun layoutViews() {
        // Post to ensure view is measured
        containerLayout.post {
            val width = containerLayout.width
            val height = containerLayout.height

            // Logo
            val logoWidth = (width * 0.6f).toInt() // Adjust width as needed, e.g., 60% of screen width
            val logoHeight = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            logoImage.measure(
                View.MeasureSpec.makeMeasureSpec(logoWidth, View.MeasureSpec.EXACTLY),
                logoHeight
            )
            logoImage.layout(
                (width - logoImage.measuredWidth) / 2,
                height / 6,
                (width + logoImage.measuredWidth) / 2,
                height / 6 + logoImage.measuredHeight
            )

            // Buttons
            val buttonWidth = width * 3 / 4
            val buttonHeight = 80
            val buttonSpacing = 24
            val startY = height / 3

            val buttons = listOf(playButton, highScoresButton, settingsButton, aboutButton)
            buttons.forEachIndexed { index, button ->
                val y = startY + index * (buttonHeight + buttonSpacing)
                button.layout(
                    (width - buttonWidth) / 2,
                    y,
                    (width + buttonWidth) / 2,
                    y + buttonHeight
                )
            }
        }
    }

    private fun setupAnimations() {
        // Fade in animations
        logoImage.alpha = 0f
        logoImage.animate()
            .alpha(1f)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        val buttons = listOf(playButton, highScoresButton, settingsButton, aboutButton)
        buttons.forEachIndexed { index, button ->
            button.alpha = 0f
            button.translationY = 100f
            button.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .setStartDelay((index * 150).toLong())
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun setupClickListeners() {
        playButton.setOnClickListener {
            animateButtonClick(it) {
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }

        highScoresButton.setOnClickListener {
            animateButtonClick(it) {
                // TODO: Implement high scores screen
            }
        }

        settingsButton.setOnClickListener {
            animateButtonClick(it) {
                // TODO: Implement settings screen
            }
        }

        aboutButton.setOnClickListener {
            animateButtonClick(it) {
                // TODO: Implement about dialog
            }
        }
    }

    private fun animateButtonClick(view: View, action: () -> Unit) {
        val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f)
        scaleDown.duration = 100

        val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f)
        scaleUp.duration = 100

        scaleDown.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                scaleUp.start()
                action.invoke()
            }
        })

        scaleDown.start()
    }

    override fun onResume() {
        super.onResume()
        // Kích hoạt animation background nếu cần
        findViewById<AnimatedBackgroundView>(containerLayout.id)?.resume()
    }

    override fun onPause() {
        super.onPause()
        // Tạm dừng animation background nếu cần
        findViewById<AnimatedBackgroundView>(containerLayout.id)?.pause()
    }
}

// Animated background view with floating Pokemon-themed particles
class AnimatedBackgroundView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particles = mutableListOf<Particle>()
    private var animationRunning = false

    init {
        // Initialize particles
        repeat(20) {
            particles.add(createRandomParticle())
        }
    }

    private fun createRandomParticle(): Particle {
        val colors = listOf(
            Color.parseColor("#FFD700"), // Gold
            Color.parseColor("#FF6347"), // Red
            Color.parseColor("#32CD32"), // Green
            Color.parseColor("#1E90FF"), // Blue
            Color.parseColor("#DDA0DD")  // Plum
        )

        return Particle(
            x = Random.nextFloat() * 1000f,
            y = Random.nextFloat() * 2000f,
            size = Random.nextFloat() * 30 + 10f,
            color = colors.random(),
            speedX = Random.nextFloat() * 2 - 1f,
            speedY = Random.nextFloat() * 2 + 1f,
            alpha = Random.nextInt(50, 150)
        )
    }

    fun resume() {
        animationRunning = true
        post(animationRunnable)
    }

    fun pause() {
        animationRunning = false
        removeCallbacks(animationRunnable)
    }

    private val animationRunnable = object : Runnable {
        override fun run() {
            if (animationRunning) {
                updateParticles()
                invalidate()
                postDelayed(this, 16) // ~60 FPS
            }
        }
    }

    private fun updateParticles() {
        particles.forEach { particle ->
            particle.x += particle.speedX
            particle.y += particle.speedY

            // Wrap around screen
            if (particle.x < -50) particle.x = width + 50f
            if (particle.x > width + 50) particle.x = -50f
            if (particle.y < -50) particle.y = height + 50f
            if (particle.y > height + 50) particle.y = -50f

            // Animate alpha for twinkling effect
            particle.alpha = (50 + 50 * sin(System.currentTimeMillis() * 0.005 + particle.x * 0.01)).toInt()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw gradient background
        val gradient = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            Color.parseColor("#FAF8EF"), Color.parseColor("#E8E4D3"),
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Draw particles
        particles.forEach { particle ->
            paint.color = Color.argb(particle.alpha, Color.red(particle.color), Color.green(particle.color), Color.blue(particle.color))
            canvas.drawCircle(particle.x, particle.y, particle.size, paint)
        }
    }

    data class Particle(
        var x: Float,
        var y: Float,
        val size: Float,
        val color: Int,
        val speedX: Float,
        val speedY: Float,
        var alpha: Int
    )
}