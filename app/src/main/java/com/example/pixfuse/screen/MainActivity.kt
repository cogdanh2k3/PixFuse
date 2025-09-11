package com.example.pixfuse.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.pixfuse.R
import kotlinx.coroutines.*
import kotlin.math.min
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        // Set fullscreen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        gameView = GameView(this)
        setContentView(gameView)

        // Thiết lập OnBackPressedCallback
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Logic khi nhấn back: Quay lại MenuActivity
                startActivity(Intent(this@MainActivity, MenuActivity::class.java))
                finish()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
    }
}

// Data class for asteroid
data class Asteroid(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float
)

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Drawing components
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var characterBitmap: Bitmap? = null
    private var spaceBackgroundBitmap: Bitmap? = null
    private var asteroidBitmap: Bitmap? = null

    // Character position (follows touch)
    private var characterX = 0f
    private var characterY = 0f
    private var characterSize = 0f

    // Asteroids
    private val asteroids = mutableListOf<Asteroid>()
    private var lastSpawnTime = 0L
    private var spawnInterval = 1500L // Initial interval in ms (1.5 seconds)
    private var gameStartTime = 0L

    // Background scrolling
    private var backgroundOffsetY = 0f
    private val scrollSpeed = 200f // Pixels per second

    // Animation and timing
    private val gameLoop = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isRunning = false
    private var lastTime = System.currentTimeMillis()

    init {
        // Initialize paints
        backgroundPaint.isFilterBitmap = true

        // Load bitmaps
        loadCharacter()
        loadSpaceBackground()
        loadAsteroid()

        // Initial character position (bottom center, but will be set in onSizeChanged)
        characterX = 0f
        characterY = 0f

        gameStartTime = System.currentTimeMillis()
        lastSpawnTime = gameStartTime

        startGameLoop()
    }

    private fun loadCharacter() {
        characterBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.character)
    }

    private fun loadSpaceBackground() {
        spaceBackgroundBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.bg_space_seamless)
    }

    private fun loadAsteroid() {
        asteroidBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.asteroid_2)
    }

    private fun spawnAsteroid() {
        val w = width.toFloat()
        val h = height.toFloat()
        val asteroidSize = min(w, h) * Random.nextFloat() * 0.1f + min(w, h) * 0.05f // Random size between 5% and 15% of screen min dimension
        val x = Random.nextFloat() * (w - asteroidSize) // Random x from 0 to width - size
        val y = -asteroidSize // Start from top (negative to ensure fully off-screen initially)
        val speed = 100f + Random.nextFloat() * 200f // Random speed 100-300 px/s

        asteroids.add(Asteroid(x, y, asteroidSize, speed))
    }

    private fun startGameLoop() {
        if (!isRunning) {
            isRunning = true
            gameLoop.launch {
                while (isRunning) {
                    val currentTime = System.currentTimeMillis()
                    val deltaTime = (currentTime - lastTime) / 1000f
                    lastTime = currentTime

                    // Spawn asteroids periodically
                    if (currentTime - lastSpawnTime > spawnInterval) {
                        spawnAsteroid()
                        lastSpawnTime = currentTime
                        // Gradually decrease interval to increase difficulty
                        spawnInterval = maxOf(500L, spawnInterval - 100L) // Min 0.5s
                    }

                    // Update background scroll
                    backgroundOffsetY += scrollSpeed * deltaTime
                    if (backgroundOffsetY >= spaceBackgroundBitmap?.height?.toFloat() ?: height.toFloat()) {
                        backgroundOffsetY = 0f
                    }

                    // Update asteroids
                    updateAsteroids(deltaTime)

                    // Check collisions
                    checkCollisions()

                    invalidate()
                    delay(16) // ~60 FPS
                }
            }
        }
    }

    private fun updateAsteroids(deltaTime: Float) {
        val iterator = asteroids.iterator()
        while (iterator.hasNext()) {
            val asteroid = iterator.next()
            asteroid.y += asteroid.speed * deltaTime
            // Remove if off-screen
            if (asteroid.y > height.toFloat() + asteroid.size) {
                iterator.remove()
            }
        }
    }

    private fun checkCollisions() {
        val characterRect = RectF(
            characterX - characterSize / 2f,
            characterY - characterSize / 2f,
            characterX + characterSize / 2f,
            characterY + characterSize / 2f
        )

        for (asteroid in asteroids) {
            val asteroidRect = RectF(
                asteroid.x - asteroid.size / 2f,
                asteroid.y - asteroid.size / 2f,
                asteroid.x + asteroid.size / 2f,
                asteroid.y + asteroid.size / 2f
            )

            if (RectF.intersects(characterRect, asteroidRect)) {
                endGame()
                return
            }
        }
    }

    private fun endGame() {
        isRunning = false
        val intent = Intent(context, GameOverActivity::class.java).apply {
            putExtra(GameOverActivity.EXTRA_IS_WIN, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
        // Không gọi finish() để tránh đóng MainActivity ngay lập tức
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Calculate character size based on screen
        characterSize = min(w, h) * 0.15f // 15% of smaller dimension

        // Set initial position to bottom center
        characterX = w / 2f
        characterY = h - characterSize / 2f

        // Clamp initial position to screen bounds
        characterX = characterX.coerceIn(characterSize / 2f, (w - characterSize / 2f).toFloat())
        characterY = characterY.coerceIn(characterSize / 2f, (h - characterSize / 2f).toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw scrolling background
        drawScrollingBackground(canvas)

        // Draw asteroids
        drawAsteroids(canvas)

        // Draw character
        drawCharacter(canvas)
    }

    private fun drawScrollingBackground(canvas: Canvas) {
        val bg = spaceBackgroundBitmap ?: return
        val bgWidth = bg.width.toFloat()
        val bgHeight = bg.height.toFloat()
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        // Normalize offset to be within 0 to height
        val normalizedOffset = backgroundOffsetY % bgHeight
        val topOffset = if (normalizedOffset > 0) normalizedOffset - bgHeight else normalizedOffset

        // Draw top part
        val topSrcRect = Rect(0, 0, bg.width, bg.height)
        val topDestRect = RectF(0f, topOffset, viewWidth, topOffset + viewHeight)
        canvas.drawBitmap(bg, topSrcRect, topDestRect, backgroundPaint)

        // Draw bottom part
        val bottomOffset = topOffset + bgHeight
        val bottomDestRect = RectF(0f, bottomOffset, viewWidth, bottomOffset + viewHeight)
        if (bottomOffset < viewHeight) {
            canvas.drawBitmap(bg, topSrcRect, bottomDestRect, backgroundPaint)
        }

        // Tile horizontally if background is narrower than screen
        if (bgWidth < viewWidth) {
            val horizontalTiles = (viewWidth / bgWidth).toInt() + 1
            for (i in 1 until horizontalTiles) {
                val horizDestTop = RectF(i * bgWidth + topOffset, topOffset, (i + 1) * bgWidth, topOffset + viewHeight)
                canvas.drawBitmap(bg, topSrcRect, horizDestTop, backgroundPaint)
                if (bottomOffset < viewHeight) {
                    val horizDestBottom = RectF(i * bgWidth + bottomOffset, bottomOffset, (i + 1) * bgWidth, bottomOffset + viewHeight)
                    canvas.drawBitmap(bg, topSrcRect, horizDestBottom, backgroundPaint)
                }
            }
        }
    }

    private fun drawAsteroids(canvas: Canvas) {
        val asteroid = asteroidBitmap ?: return
        asteroids.forEach { ast ->
            val halfSize = ast.size / 2f
            val srcRect = Rect(0, 0, asteroid.width, asteroid.height)
            val destRect = RectF(
                ast.x - halfSize,
                ast.y - halfSize,
                ast.x + halfSize,
                ast.y + halfSize
            )
            canvas.drawBitmap(asteroid, srcRect, destRect, backgroundPaint)
        }
    }

    private fun drawCharacter(canvas: Canvas) {
        val character = characterBitmap ?: return
        val halfSize = characterSize / 2f

        // Clamp position to screen bounds (with margin)
        val clampedX = characterX.coerceIn(halfSize, (width - halfSize).toFloat())
        val clampedY = characterY.coerceIn(halfSize, (height - halfSize).toFloat())

        val srcRect = Rect(0, 0, character.width, character.height)
        val destRect = RectF(
            clampedX - halfSize,
            clampedY - halfSize,
            clampedX + halfSize,
            clampedY + halfSize
        )

        canvas.drawBitmap(character, srcRect, destRect, backgroundPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Follow touch position directly
        characterX = event.x
        characterY = event.y
        invalidate() // Redraw immediately for smooth following
        return true
    }

    fun resume() {
        isRunning = false // Stop current loop
        gameStartTime = System.currentTimeMillis()
        lastSpawnTime = gameStartTime
        spawnInterval = 1500L
        asteroids.clear()
        startGameLoop()
    }

    fun pause() {
        isRunning = false
    }
}