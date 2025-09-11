package com.example.pixfuse.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import com.example.pixfuse.R
import kotlinx.coroutines.*
import kotlin.math.*

class MainActivity : AppCompatActivity() {
    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set fullscreen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        gameView = GameView(this)
        setContentView(gameView)

        // Thiết lập OnBackPressedDispatcher
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

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Game logic components
    private val board = Board(4)
    private val gameManager = GameManager(board, context)

    // Drawing components
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var logoBitmap: Bitmap? = null

    // Dynamic background
    private val backgroundColors = listOf(
        Color.parseColor("#FAF8EF"), // Original background
        Color.parseColor("#E8F0FE"), // Light blue
        Color.parseColor("#FFF0F5"), // Light pink
        Color.parseColor("#E6FFE6")  // Light green
    )
    private var colorTransitionTime = 0f
    private val colorTransitionDuration = 5f // Seconds for one full color transition cycle
    private var currentColorIndex = 0

    // Touch handling
    private val gestureDetector: GestureDetectorCompat

    // Animation and timing
    private val gameLoop = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isRunning = false
    private var lastTime = System.currentTimeMillis()

    // Layout properties
    private var tileSize = 0f
    private var boardStartX = 0f
    private var boardStartY = 0f
    private var padding = 8f

    // Pokemon tile bitmaps cache
    private val tileBitmaps = mutableMapOf<Int, Bitmap>()

    init {
        // Initialize paints
        backgroundPaint.color = backgroundColors[0]

        tilePaint.style = Paint.Style.FILL

        textPaint.apply {
            color = Color.parseColor("#776E65")
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        scorePaint.apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        // Initialize gesture detector
        gestureDetector = GestureDetectorCompat(context, GestureListener())

        // Load tile PNGs and logo
        loadTilePNGs()
        loadLogo()

        // Start game
        gameManager.spawnTile()
        gameManager.spawnTile()

        startGameLoop()
    }

    private fun loadTilePNGs() {
        val tileImages = mapOf(
            2 to R.drawable.pikachu_2,       // Pikachu
            4 to R.drawable.charmander_4,    // Charmander
            8 to R.drawable.bullbasaur_8,    // Bulbasaur
            16 to R.drawable.squirtle_16,    // Squirtle
            32 to R.drawable.eevee_32,       // Eevee
            64 to R.drawable.jigglypuff_64,  // Jigglypuff
            128 to R.drawable.snorlax_128,   // Snorlax
            256 to R.drawable.dratini_256,   // Dratini
            512 to R.drawable.pidgey_512,    // Pidgey
            1024 to R.drawable.abra_1024,    // Abra
            2048 to R.drawable.venonat_2048  // Venonat
        )

        for ((value, resId) in tileImages) {
            tileBitmaps[value] = BitmapFactory.decodeResource(context.resources, resId)
        }
    }

    private fun loadLogo() {
        logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
    }

    private fun startGameLoop() {
        if (!isRunning) {
            isRunning = true
            gameLoop.launch {
                while (isRunning) {
                    val currentTime = System.currentTimeMillis()
                    val deltaTime = (currentTime - lastTime) / 1000f
                    lastTime = currentTime

                    // Update background color transition
                    colorTransitionTime += deltaTime
                    if (colorTransitionTime >= colorTransitionDuration) {
                        colorTransitionTime = 0f
                        currentColorIndex = (currentColorIndex + 1) % backgroundColors.size
                    }

                    board.update(deltaTime)
                    if (gameManager.isMoved) {
                        gameManager.update() // Calls spawnTile, checkWin, checkLose
                        if (gameManager.hasWon || gameManager.hasLost) {
                            endGame()
                        }
                    }

                    invalidate()
                    delay(16) // ~60 FPS
                }
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Calculate tile size and board position
        tileSize = (min(w, h) * 0.75f) / 4f
        boardStartX = (w - (tileSize * 4 + padding * 3)) / 2
        boardStartY = h * 0.35f
        board.updateLayout(boardStartX, boardStartY, tileSize, padding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calculate interpolated background color
        val nextColorIndex = (currentColorIndex + 1) % backgroundColors.size
        val fraction = colorTransitionTime / colorTransitionDuration
        val currentColor = backgroundColors[currentColorIndex]
        val nextColor = backgroundColors[nextColorIndex]
        val interpolatedColor = interpolateColor(currentColor, nextColor, fraction)
        canvas.drawColor(interpolatedColor)

        drawHeader(canvas)
        drawBoard(canvas)
        drawGameState(canvas)
    }

    private fun interpolateColor(startColor: Int, endColor: Int, fraction: Float): Int {
        val startA = (startColor shr 24) and 0xff
        val startR = (startColor shr 16) and 0xff
        val startG = (startColor shr 8) and 0xff
        val startB = startColor and 0xff

        val endA = (endColor shr 24) and 0xff
        val endR = (endColor shr 16) and 0xff
        val endG = (endColor shr 8) and 0xff
        val endB = endColor and 0xff

        val resultA = (startA + fraction * (endA - startA)).toInt()
        val resultR = (startR + fraction * (endR - startR)).toInt()
        val resultG = (startG + fraction * (endG - startG)).toInt()
        val resultB = (startB + fraction * (endB - startB)).toInt()

        return (resultA shl 24) or (resultR shl 16) or (resultG shl 8) or resultB
    }

    private fun drawHeader(canvas: Canvas) {
        // Draw logo instead of text
        logoBitmap?.let { bitmap ->
            val logoWidth = width * 0.4f // Kích thước logo bằng 40% chiều rộng màn hình
            val logoHeight = logoWidth * (bitmap.height.toFloat() / bitmap.width.toFloat()) // Giữ tỷ lệ
            val logoRect = RectF(
                (width - logoWidth) / 2,
                height * 0.08f,
                (width + logoWidth) / 2,
                height * 0.08f + logoHeight
            )
            canvas.drawBitmap(bitmap, null, logoRect, null)
        }

        // Score boxes
        val scoreBoxWidth = width * 0.2f
        val scoreBoxHeight = height * 0.08f
        val scoreY = height * 0.25f

        // Score box background
        tilePaint.color = Color.parseColor("#BBADA0")
        val scoreRect = RectF(width * 0.25f, scoreY - scoreBoxHeight/2, width * 0.45f, scoreY + scoreBoxHeight/2)
        canvas.drawRoundRect(scoreRect, 8f, 8f, tilePaint)

        // Best score box background
        val bestRect = RectF(width * 0.55f, scoreY - scoreBoxHeight/2, width * 0.75f, scoreY + scoreBoxHeight/2)
        canvas.drawRoundRect(bestRect, 8f, 8f, tilePaint)

        // Score text
        scorePaint.textSize = height * 0.025f
        scorePaint.color = Color.parseColor("#EEE4DA")
        canvas.drawText("SCORE", width * 0.35f, scoreY - height * 0.02f, scorePaint)
        canvas.drawText("BEST", width * 0.65f, scoreY - height * 0.02f, scorePaint)

        scorePaint.textSize = height * 0.035f
        scorePaint.color = Color.WHITE
        canvas.drawText(gameManager.score.toString(), width * 0.35f, scoreY + height * 0.02f, scorePaint)
        canvas.drawText(gameManager.highScore.toString(), width * 0.65f, scoreY + height * 0.02f, scorePaint)
    }

    private fun drawBoard(canvas: Canvas) {
        // Draw board background
        tilePaint.color = Color.parseColor("#BBADA0")
        val boardRect = RectF(
            boardStartX - padding,
            boardStartY - padding,
            boardStartX + 4 * tileSize + 3 * padding + padding,
            boardStartY + 4 * tileSize + 3 * padding + padding
        )
        canvas.drawRoundRect(boardRect, 12f, 12f, tilePaint)

        // Draw empty tile backgrounds
        tilePaint.color = Color.parseColor("#CDC1B4")
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                val x = boardStartX + col * (tileSize + padding)
                val y = boardStartY + row * (tileSize + padding)
                val tileRect = RectF(x, y, x + tileSize, y + tileSize)
                canvas.drawRoundRect(tileRect, 8f, 8f, tilePaint)
            }
        }

        // Draw tiles
        board.draw(canvas, tileBitmaps)
    }

    private fun drawGameState(canvas: Canvas) {
        // Only draw overlay for game over state
        if (gameManager.hasLost) {
            drawOverlay(canvas, "GAME OVER!", Color.parseColor("#EE4C2C"))
        }
        // Win state is handled by navigation to WinActivity
    }

    private fun drawOverlay(canvas: Canvas, message: String, color: Int) {
        // Semi-transparent overlay
        canvas.drawColor(Color.parseColor("#80FFFFFF"))

        // Message background
        tilePaint.color = color
        val messageRect = RectF(
            width * 0.1f, height * 0.4f,
            width * 0.9f, height * 0.6f
        )
        canvas.drawRoundRect(messageRect, 16f, 16f, tilePaint)

        // Message text
        textPaint.color = Color.WHITE
        textPaint.textSize = height * 0.06f
        canvas.drawText(message, width * 0.5f, height * 0.52f, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private val swipeThreshold = 100f
        private val swipeVelocityThreshold = 100f

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val diffX = e2.x - (e1?.x ?: 0f)
            val diffY = e2.y - (e1?.y ?: 0f)

            if (abs(diffX) > abs(diffY)) {
                if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                    if (diffX > 0) {
                        gameManager.moveRight()
                    } else {
                        gameManager.moveLeft()
                    }
                    return true
                }
            } else {
                if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                    if (diffY > 0) {
                        gameManager.moveDown()
                    } else {
                        gameManager.moveUp()
                    }
                    return true
                }
            }
            return false
        }
    }

    fun resume() {
        startGameLoop()
    }

    fun pause() {
        isRunning = false
        gameManager.saveHighScore()
    }

    private fun endGame() {
        isRunning = false // Stop the game loop
        gameManager.saveHighScore()
        val intent = if (gameManager.hasWon) {
            Intent(context, WinActivity::class.java).apply {
                putExtra(WinActivity.EXTRA_SCORE, gameManager.score)
                putExtra(WinActivity.EXTRA_HIGH_SCORE, gameManager.highScore)
                putExtra(WinActivity.EXTRA_LEVEL, 1) // Adjust level as needed
            }
        } else {
            Intent(context, GameOverActivity::class.java).apply {
                putExtra(GameOverActivity.EXTRA_SCORE, gameManager.score)
                putExtra(GameOverActivity.EXTRA_HIGH_SCORE, gameManager.highScore)
                putExtra(GameOverActivity.EXTRA_IS_WIN, false)
            }
        }
        context.startActivity(intent)
        (context as Activity).finish()
    }
}