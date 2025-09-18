package com.example.pixfuse.screen

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.pixfuse.R
import com.example.pixfuse.SoundManager
import kotlinx.coroutines.*
import kotlin.math.min
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        // Fullscreen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        gameView = GameView(this)
        setContentView(gameView)

        // Back về Menu
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
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

// --- DATA CLASS ---
data class Asteroid(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    var hitCount: Int = 0 // số lần trúng đạn
)

data class Bullet(
    var x: Float,
    var y: Float,
    val speed: Float = 800f,
    val size: Float = 20f
)

// --- GAME VIEW ---
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Bitmap
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var characterBitmap: Bitmap? = null
    private var spaceBackgroundBitmap: Bitmap? = null
    private var asteroidBitmap: Bitmap? = null

    // Character
    private var characterX = 0f
    private var characterY = 0f
    private var characterSize = 0f

    // Asteroids
    private val asteroids = mutableListOf<Asteroid>()
    private var lastSpawnTime = 0L
    private var spawnInterval = 1500L
    private var gameStartTime = 0L

    // Bullets
    private val bullets = mutableListOf<Bullet>()
    private var lastBulletTime = 0L
    private val bulletInterval = 200L // bắn mỗi 0.2 giây

    // Background
    private var backgroundOffsetY = 0f
    private val scrollSpeed = 200f

    // Loop
    private val gameLoop = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isRunning = false
    private var lastTime = System.currentTimeMillis()
    private var check = false

    // Sound
    private val soundManager = SoundManager(context)

    init {
        backgroundPaint.isFilterBitmap = true
        loadCharacter()
        loadSpaceBackground()
        loadAsteroid()

        gameStartTime = System.currentTimeMillis()
        lastSpawnTime = gameStartTime

        startGameLoop()
        //spawnAsteroids(5) // Ví dụ spawn 5 thiên thạch
    }
    private fun spawnAsteroids(count: Int) {
        repeat(count){
            spawnAsteroid()
        }
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
        //val asteroidSize = min(w, h)* Random.nextFloat() * 0.1f + min(w, h) * 0.05f
        val asteroidSize = min(w, h) * 0.1f + min(w, h) * 0.05f
        val x = Random.nextFloat() * (w - asteroidSize)
        val y = -asteroidSize
        val speed = 100f + Random.nextFloat() * 200f
        asteroids.add(Asteroid(x, y, asteroidSize, speed))
    }

    private fun spawnBullet() {
        val bullet = Bullet(
            x = characterX,
            y = characterY - characterSize / 2f
        )
        bullets.add(bullet)
        soundManager.playSound(SoundManager.SoundType.SHOOT)
    }
    private fun startGameLoop() {
        if (!isRunning) {
            isRunning = true
            gameLoop.launch {
                while (isRunning) {
                    val currentTime = System.currentTimeMillis()
                    val deltaTime = (currentTime - lastTime) / 1000f
                    lastTime = currentTime
                    if(check==false){
                        spawnAsteroids(5)
                        check=true
                    }

                    // Spawn asteroid
                   // if (currentTime - lastSpawnTime > spawnInterval) {
                     //   spawnAsteroid()
                       // lastSpawnTime = currentTime
                        //spawnInterval = maxOf(500L, spawnInterval - 100L)
                    //}

                    // Spawn bullet
 //                   if (currentTime - lastBulletTime > bulletInterval) {
  //                      spawnBullet()
  //                      lastBulletTime = currentTime
   //                 }
                    // Update
                    backgroundOffsetY += scrollSpeed * deltaTime
                    if (backgroundOffsetY >= spaceBackgroundBitmap?.height?.toFloat() ?: height.toFloat()) {
                        backgroundOffsetY = 0f
                    }
                    updateAsteroids(deltaTime)
                    updateBullets(deltaTime)

                    // Collisions
                    checkCollisions()

                    invalidate()
                    delay(16)
                }
            }
        }
    }

//    private fun updateAsteroids(deltaTime: Float) {
//        val iterator = asteroids.iterator()
//        while (iterator.hasNext()) {
//            val asteroid = iterator.next()
//            asteroid.y += asteroid.speed * deltaTime
//            if (asteroid.y > height.toFloat() + asteroid.size) {
//                iterator.remove()
//}
//        }
//    }
    private fun updateAsteroids(deltaTime: Float) {
        asteroids.forEach { asteroid ->
            asteroid.y += asteroid.speed * deltaTime

            // Nếu thiên thạch chạm đáy màn hình → spawn lại từ trên
            if (asteroid.y - asteroid.size / 2f > height) {
                asteroid.y = -asteroid.size / 2f   // quay lại bên trên
                asteroid.hitCount = 0              // reset số lần trúng đạn
            }
        }
    }

    private fun updateBullets(deltaTime: Float) {
        val iterator = bullets.iterator()
        while (iterator.hasNext()) {
            val bullet = iterator.next()
            bullet.y -= bullet.speed * deltaTime
            if (bullet.y < -bullet.size) {
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

        // Collision character <-> asteroid
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

        // Collision bullet <-> asteroid
        val bulletIterator = bullets.iterator()
        while (bulletIterator.hasNext()) {
            val bullet = bulletIterator.next()
            val bulletRect = RectF(
                bullet.x - bullet.size / 2f,
                bullet.y - bullet.size / 2f,
                bullet.x + bullet.size / 2f,
                bullet.y + bullet.size / 2f
            )

            val asteroidIterator = asteroids.iterator()
            while (asteroidIterator.hasNext()) {
                val asteroid = asteroidIterator.next()
                val asteroidRect = RectF(
                    asteroid.x - asteroid.size / 2f,
                    asteroid.y - asteroid.size / 2f,
                    asteroid.x + asteroid.size / 2f,
                    asteroid.y + asteroid.size / 2f
                )

                if (RectF.intersects(bulletRect, asteroidRect)) {
                    bulletIterator.remove()
                    asteroid.hitCount++
                    if (asteroid.hitCount >= 3) {
                        asteroidIterator.remove()
                        soundManager.playSound(SoundManager.SoundType.EXPLOSION)
                    }
                    break
                }
            }
        }
    }

    private fun endGame() {
        isRunning = false
        soundManager.playSound(SoundManager.SoundType.GAME_OVER)
        soundManager.stopBGM()
        val intent = Intent(context, GameOverActivity::class.java).apply {
            putExtra(GameOverActivity.EXTRA_IS_WIN, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        characterSize = min(w, h) * 0.15f
        characterX = w / 2f
        characterY = h - characterSize / 2f
        characterX = characterX.coerceIn(characterSize / 2f, (w - characterSize / 2f).toFloat())
        characterY = characterY.coerceIn(characterSize / 2f, (h - characterSize / 2f).toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawScrollingBackground(canvas)
        drawAsteroids(canvas)
        drawBullets(canvas)
        drawCharacter(canvas)
    }

    private fun drawScrollingBackground(canvas: Canvas) {
        val bg = spaceBackgroundBitmap ?: return
        val bgHeight = bg.height.toFloat()
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val normalizedOffset = backgroundOffsetY % bgHeight
        val topOffset = if (normalizedOffset > 0) normalizedOffset - bgHeight else normalizedOffset

        val srcRect = Rect(0, 0, bg.width, bg.height)
        val topDestRect = RectF(0f, topOffset, viewWidth, topOffset + viewHeight)
        canvas.drawBitmap(bg, srcRect, topDestRect, backgroundPaint)

        val bottomOffset = topOffset + bgHeight
        val bottomDestRect = RectF(0f, bottomOffset, viewWidth, bottomOffset + viewHeight)
        if (bottomOffset < viewHeight) {
            canvas.drawBitmap(bg, srcRect, bottomDestRect, backgroundPaint)
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

    private fun drawBullets(canvas: Canvas) {
        val paint = Paint().apply { color = Color.YELLOW }
        bullets.forEach { b ->
            canvas.drawCircle(b.x, b.y, b.size / 2f, paint)
        }
    }

    private fun drawCharacter(canvas: Canvas) {
        val character = characterBitmap ?: return
        val halfSize = characterSize / 2f
        val newX = characterX.coerceIn(halfSize, (width - halfSize).toFloat())
        val newY = characterY.coerceIn(halfSize, (height - halfSize).toFloat())

        if (newX != characterX || newY != characterY) {
            soundManager.playSound(SoundManager.SoundType.WALL_HIT)
        }

        characterX = newX
        characterY = newY

        val srcRect = Rect(0, 0, character.width, character.height)
        val destRect = RectF(
            characterX - halfSize,
            characterY - halfSize,
            characterX + halfSize,
            characterY + halfSize
        )
        canvas.drawBitmap(character, srcRect, destRect, backgroundPaint)
    }


   private var lastX = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                spawnBullet() // bấm xuống thì bắn đạn
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                if (Math.abs(dx) > 10) { // ngưỡng để nhận là vuốt
                    characterX += dx   // di chuyển ngang
                    lastX = event.x
                }
            }
        }
        invalidate()
        return true
    }

    fun resume() {
        isRunning = false
        gameStartTime = System.currentTimeMillis()
        lastSpawnTime = gameStartTime
        spawnInterval = 1500L
        asteroids.clear()
        bullets.clear()
        startGameLoop()
    }

    fun pause() {
        isRunning = false
    }
}
