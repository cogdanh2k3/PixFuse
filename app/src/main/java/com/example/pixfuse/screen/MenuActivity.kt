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

        // Buttons với style cải thiện
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
            textSize = 24f // Tăng kích thước chữ để dễ nhìn hơn
            setTextColor(Color.WHITE)
            setShadowLayer(4f, 2f, 2f, Color.BLACK) // Thêm shadow để chữ nổi bật hơn
            typeface = Typeface.DEFAULT_BOLD
            background = createButtonDrawable(color)
            elevation = 12f // Tăng elevation để nổi bật
            isAllCaps = false // Không all caps để chữ tự nhiên hơn
            setPadding(32, 16, 32, 16) // Thêm padding để button rộng hơn
        }
    }

    private fun createButtonDrawable(color: Int): Drawable {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20f // Tăng corner radius cho bo tròn hơn
            setColor(color)
            setStroke(4, Color.parseColor("#FFFFFF")) // Giữ viền trắng
        }
        return drawable
    }

    private fun layoutViews() {
        // Post to ensure view is measured
        containerLayout.post {
            val width = containerLayout.width
            val height = containerLayout.height

            // Logo
            val logoWidth = (width * 0.6f).toInt() // 60% of screen width
            val logoHeight = (logoWidth * 0.4f).toInt() // Aspect ratio for logo
            val logoParams = ConstraintLayout.LayoutParams(logoWidth, logoHeight).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToTop = playButton.id // Dưới logo là play button
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = (height * 0.1f).toInt() // Margin top 10%
                verticalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED
            }
            logoImage.id = View.generateViewId()
            logoImage.layoutParams = logoParams

            // Buttons layout
            val buttonWidth = (width * 0.7f).toInt()
            val buttonHeight = (height * 0.08f).toInt()
            val buttonMargin = (height * 0.03f).toInt()

            // Play button
            playButton.id = View.generateViewId()
            val playParams = ConstraintLayout.LayoutParams(buttonWidth, buttonHeight).apply {
                topToBottom = logoImage.id
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = buttonMargin
            }
            playButton.layoutParams = playParams

            // High Scores button
            highScoresButton.id = View.generateViewId()
            val highParams = ConstraintLayout.LayoutParams(buttonWidth, buttonHeight).apply {
                topToBottom = playButton.id
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = buttonMargin
            }
            highScoresButton.layoutParams = highParams

            // Settings button
            settingsButton.id = View.generateViewId()
            val settingsParams = ConstraintLayout.LayoutParams(buttonWidth, buttonHeight).apply {
                topToBottom = highScoresButton.id
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = buttonMargin
            }
            settingsButton.layoutParams = settingsParams

            // About button
            aboutButton.id = View.generateViewId()
            val aboutParams = ConstraintLayout.LayoutParams(buttonWidth, buttonHeight).apply {
                topToBottom = settingsButton.id
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = buttonMargin
                bottomMargin = (height * 0.1f).toInt() // Margin bottom 10%
            }
            aboutButton.layoutParams = aboutParams
        }
    }

    private fun setupAnimations() {
        // Fade in animation cho logo
        logoImage.alpha = 0f
        logoImage.animate()
            .alpha(1f)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Scale in animation cho buttons
        val buttons = listOf(playButton, highScoresButton, settingsButton, aboutButton)
        buttons.forEachIndexed { index, button ->
            button.scaleX = 0.8f
            button.scaleY = 0.8f
            button.alpha = 0f
            button.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(600)
                .setStartDelay((index * 200L) + 500L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun setupClickListeners() {
        playButton.setOnClickListener {
            animateButtonClick(it) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        highScoresButton.setOnClickListener {
            animateButtonClick(it) {
                // TODO: Implement High Scores Activity
            }
        }

        settingsButton.setOnClickListener {
            animateButtonClick(it) {
                // TODO: Implement Settings Activity
            }
        }

        aboutButton.setOnClickListener {
            animateButtonClick(it) {
                // TODO: Implement About Activity
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
        containerLayout.findViewById<AnimatedBackgroundView>(0)?.resume() // ID 0 cho background
    }

    override fun onPause() {
        super.onPause()
        // Tạm dừng animation background nếu cần
        containerLayout.findViewById<AnimatedBackgroundView>(0)?.pause()
    }
}

// Animated background view with PNG background and floating particles
class AnimatedBackgroundView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particles = mutableListOf<Particle>()
    private var animationRunning = false
    private var backgroundBitmap: Bitmap? = null

    init {
        // Load PNG background
        loadBackground()

        // Initialize particles
        repeat(20) {
            particles.add(createRandomParticle())
        }
    }

    private fun loadBackground() {
        // Giả sử PNG background là R.drawable.menu_background - thay bằng tên thực tế của bạn
        backgroundBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.background1)
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

        // Draw PNG background (tile nếu cần)
        val bg = backgroundBitmap
        if (bg != null) {
            val bgWidth = bg.width.toFloat()
            val bgHeight = bg.height.toFloat()
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()

            // Tile horizontally and vertically if background smaller than screen
            val horizontalTiles = (viewWidth / bgWidth).toInt() + 1
            val verticalTiles = (viewHeight / bgHeight).toInt() + 1

            for (i in 0 until horizontalTiles) {
                for (j in 0 until verticalTiles) {
                    val x = i * bgWidth
                    val y = j * bgHeight
                    canvas.drawBitmap(bg, x, y, paint)
                }
            }
        } else {
            // Fallback gradient nếu không load được PNG
            val gradient = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                Color.parseColor("#FAF8EF"), Color.parseColor("#E8E4D3"),
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.shader = null
        }

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