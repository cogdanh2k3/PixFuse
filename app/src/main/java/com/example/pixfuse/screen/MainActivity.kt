package com.example.pixfuse.screen

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.pixfuse.R
import com.example.pixfuse.game.SupportItem
import kotlinx.coroutines.*
import kotlin.math.min
import kotlin.random.Random
import com.example.pixfuse.Object.Asteroid
import com.example.pixfuse.Object.Bomb
import com.example.pixfuse.Object.EnemyBullet
import com.example.pixfuse.Object.Laser
import com.example.pixfuse.Object.SupportManager

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
    private val bulletInterval = 500L // bắn mỗi 0.2 giây

    // Background
    private var backgroundOffsetY = 0f
    private val scrollSpeed = 200f

    // Loop
    private val gameLoop = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isRunning = false
    private var lastTime = System.currentTimeMillis()
    private var check = false
    // Sound
    private var soundPool: SoundPool
    private var soundShoot = 0
    private var soundWall = 0
    private var soundExplosion = 0
    // Music
    private var bgMusic: MediaPlayer? = null
    private var isMusicOn = true
    private var isSoundOn = true
    private var soundButtonRect = RectF()
    private var musicButtonRect = RectF()
/*    private var soundBitmap: Bitmap? = null
    private var musicBitmap: Bitmap? = null*/
    private var soundBitmap: Bitmap? = null
    private var soundInactiveBitmap: Bitmap? = null
    private var musicBitmap: Bitmap? = null
    private var musicInactiveBitmap: Bitmap? = null
    //Ho tro
    // Support Items
    private val supportItems = mutableListOf<SupportItem>()
    private var lastSupportSpawn = 0L
    private val supportInterval = 3000L // spawn mỗi 8 giây
    private var bulletLevel = 1 // mặc định 1 viên, tối đa 3
    private val bombs = mutableListOf<Bomb>()
    private var bombMode = false
    private var bombModeEndTime = 0L

    private lateinit var heartFull: Bitmap
    private lateinit var heartEmpty: Bitmap
    private lateinit var coinBitmap: Bitmap
    private val supportManager = SupportManager()
    private val enemyBullets = mutableListOf<EnemyBullet>()

    private fun loadHearts() {
        heartFull = BitmapFactory.decodeResource(context.resources, R.drawable.full_heart)
        heartEmpty = BitmapFactory.decodeResource(context.resources, R.drawable.empty_heart)
        coinBitmap = BitmapFactory.decodeResource(resources, R.drawable.coin)
    }
    init {
        // Khởi tạo SoundPool
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .build()

        // Load âm thanh từ res/raw
        soundShoot = soundPool.load(context, R.raw.fire, 1)
        soundWall = soundPool.load(context, R.raw.explosion1, 1)
        soundExplosion = soundPool.load(context, R.raw.explosion, 1)
        backgroundPaint.isFilterBitmap = true
        loadCharacter()
        loadSpaceBackground()
        loadAsteroid()
        loadHearts()
        gameStartTime = System.currentTimeMillis()
        lastSpawnTime = gameStartTime

        startGameLoop()
        // Nhạc nền
        soundBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.sound)
        soundInactiveBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.sound_inactive)

        musicBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.music)
        musicInactiveBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.music_inactive)

        bgMusic = MediaPlayer.create(context, R.raw.music)
        bgMusic?.isLooping = true
        bgMusic?.setVolume(0.5f, 0.5f)
        bgMusic?.start()

    }
    //Support Item
private fun updateSupportItems(deltaTime: Float) {
    val iterator = supportItems.iterator()
    while (iterator.hasNext()) {
        val item = iterator.next()
        if (item.update(deltaTime, height.toFloat())) {
            iterator.remove()
        }
    }

    val current = System.currentTimeMillis()
    if (current - lastSupportSpawn > supportInterval) {
        supportItems.add(SupportItem.spawn(context, width.toFloat(), height.toFloat()))
        lastSupportSpawn = current
    }
}

    private fun drawSupportItems(canvas: Canvas) {
        supportItems.forEach { it.draw(canvas) }
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

    private var asteroidBitmaps: List<Bitmap>? = null

    private fun loadAsteroid() {
        asteroidBitmaps = listOf(
            BitmapFactory.decodeResource(context.resources, R.drawable.bullbasaur_8),
            BitmapFactory.decodeResource(context.resources, R.drawable.charmander_4),
            BitmapFactory.decodeResource(context.resources, R.drawable.abra_1024),
            BitmapFactory.decodeResource(context.resources, R.drawable.eevee_32),
            BitmapFactory.decodeResource(context.resources, R.drawable.snorlax_128)
        )
    }

    private fun spawnAsteroid() {
        val w = width.toFloat()
        val h = height.toFloat()
        val asteroidSize = min(w, h) * 0.1f + min(w, h) * 0.05f
        val x = Random.nextFloat() * (w - asteroidSize)
        val y = -asteroidSize
        val speed = 200f + Random.nextFloat() * 200f

        val bmp = asteroidBitmaps?.random()  // chọn ngẫu nhiên sprite

        // Xác suất 30% trở thành quái bắn
        val canShoot = (Random.nextFloat() < 0.3f)

        asteroids.add(
            Asteroid(
                x = x,
                y = y,
                size = asteroidSize,
                speed = speed,
                hitCount = 0,
                canShoot = canShoot,
                lastShotTime = 0L,
                bitmap = bmp
            )
        )
    }
private fun spawnBullet() {
    when (bulletLevel) {
        1 -> {
            bullets.add(Bullet(x = characterX, y = characterY - characterSize / 2f))
        }
        2 -> {
            bullets.add(Bullet(x = characterX - 20, y = characterY - characterSize / 2f))
            bullets.add(Bullet(x = characterX + 20, y = characterY - characterSize / 2f))
        }
        3 -> {
            bullets.add(Bullet(x = characterX, y = characterY - characterSize / 2f))
            bullets.add(Bullet(x = characterX - 30, y = characterY - characterSize / 2f))
            bullets.add(Bullet(x = characterX + 30, y = characterY - characterSize / 2f))
        }
        4 -> {
            bullets.add(Bullet(x = characterX - 20, y = characterY - characterSize / 2f))
            bullets.add(Bullet(x = characterX + 20, y = characterY - characterSize / 2f))
            bullets.add(Bullet(x = characterX - 40, y = characterY - characterSize / 2f))
            bullets.add(Bullet(x = characterX + 40, y = characterY - characterSize / 2f))

        }
        5 -> {
            bullets.add(Bullet(x = characterX, y = characterY - characterSize / 2f))
            bullets.add(Bullet(x = characterX - 30, y = characterY - characterSize / 2f))
            bullets.add(Bullet(x = characterX + 30, y = characterY - characterSize / 2f))
            bullets.add(Bullet(x = characterX - 60, y = characterY - characterSize / 2f))
            bullets.add(Bullet(x = characterX + 60, y = characterY - characterSize / 2f))
        }
    }

    if (isSoundOn) {
        soundPool.play(soundShoot, 0.5f, 0.5f, 1, 0, 1f)
    }
}

    private fun startGameLoop() {
        if (!isRunning) {
            isRunning = true
            gameLoop.launch {
                while (isRunning) {
                    val currentTime = System.currentTimeMillis()
                    val deltaTime = (currentTime - lastTime) / 1000f
                    lastTime = currentTime

                    if (!check) {
                        spawnAsteroids(5)
                        check = true
                    }

                    // Update
                    backgroundOffsetY += scrollSpeed * deltaTime
                    if (backgroundOffsetY >= (spaceBackgroundBitmap?.height?.toFloat() ?: height.toFloat())) {
                        backgroundOffsetY = 0f
                    }

                    updateBullets(deltaTime)   // ✅ truyền deltaTime
                    updateAsteroids(deltaTime)
                    updateSupportItems(deltaTime)
                    updateLasers()
                    updateBombs(deltaTime)

                    for (asteroid in asteroids) {
                        if (asteroid.canShoot && currentTime - asteroid.lastShotTime > 1000) {
                            // Spawn đạn rơi xuống từ tâm asteroid
                            enemyBullets.add(EnemyBullet(asteroid.x, asteroid.y + asteroid.size / 2f))
                            asteroid.lastShotTime = currentTime
                        }
                    }
                    val bulletIterator = enemyBullets.iterator()
                    while (bulletIterator.hasNext()) {
                        val bullet = bulletIterator.next()
                        bullet.y += bullet.speed

                        // Nếu ra khỏi màn hình thì xóa
                        if (bullet.y > height) {
                            bulletIterator.remove()
                        }
                    }
                    supportManager.update()
                    if (bombMode && System.currentTimeMillis() > bombModeEndTime) {
                        bombMode = false
                    }
                    // Collisions
                    checkCollisions()

                    invalidate()
                    delay(16) // ~60fps
                }
            }
        }
    }
    private fun updateBombs(deltaTime: Float) {
        val iterator = bombs.iterator()
        while (iterator.hasNext()) {
            val bomb = iterator.next()

            if (!bomb.isExploded) {
                bomb.y -= bomb.speed * deltaTime

                // Check va chạm bom với quái
                val hitEnemy = asteroids.any { asteroid ->
                    val dx = asteroid.x - bomb.x
                    val dy = asteroid.y - bomb.y
                    val distance = dx * dx + dy * dy
                    val minDist = (asteroid.size / 2f + bomb.size / 2f)
                    distance <= minDist * minDist
                }

                if (hitEnemy) {
                    bomb.isExploded = true
                    bomb.explodeTime = System.currentTimeMillis()
                    bomb.explodeRadius = bomb.size * 3f   // tăng phạm vi nổ (gấp 3 lần size bom)
                    handleBombExplosion(bomb)
                }

                // Nếu bay ra ngoài màn hình thì xóa
                if (bomb.y + bomb.size < 0) {
                    iterator.remove()
                }

            } else {
                // Bom tồn tại 0.4 giây sau khi nổ
                if (System.currentTimeMillis() - bomb.explodeTime > 400) {
                    iterator.remove()
                }
            }
        }
    }
    private fun handleBombExplosion(bomb: Bomb) {
        val bx = bomb.x
        val by = bomb.y
        val r = bomb.explodeRadius

        asteroids.forEach { asteroid ->
            val dx = asteroid.x - bx
            val dy = asteroid.y - by
            if (dx * dx + dy * dy <= r * r) {
                // Reset quái về random trên cao
                asteroid.x = Random.nextFloat() * width
                asteroid.y = -asteroid.size / 2f
                asteroid.hitCount = 0

                if (isSoundOn) {
                    soundPool.play(soundExplosion, 1f, 1f, 1, 0, 1f)
                }
            }
        }
    }
private fun updateAsteroids(deltaTime: Float) {
    asteroids.forEach { asteroid ->
        asteroid.y += asteroid.speed * deltaTime

        if (asteroid.y - asteroid.size / 2f > height) {
            asteroid.x = Random.nextFloat() * width          // random ngang
            asteroid.y = -asteroid.size / 2f     // random cao hơn màn hình
            asteroid.hitCount = 0
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

        // Auto-fire
        val now = System.currentTimeMillis()
        if (now - lastBulletTime > bulletInterval) {
            if (bombMode) {
                spawnBomb()
            } else {
                spawnBullet()
            }
            lastBulletTime = now
        }
    }
    private fun spawnBomb() {
        val bomb = Bomb(
            x = characterX,
            y = characterY - characterSize / 2f,
            size = characterSize * 0.8f,
            speed = 400f,
            explodeRadius = characterSize * 3f
        )
        bombs.add(bomb)

        if (isSoundOn) {
            soundPool.play(soundShoot, 0.5f, 0.5f, 1, 0, 1f)
        }
    }

    private fun drawHearts(canvas: Canvas) {
        val heartSize = 80   // kích thước tim (pixel)
        val margin = 20      // khoảng cách giữa các tim

        // Vẽ tim (full / empty)
        for (i in 0 until supportManager.maxHP) {
            val x = margin + i * (heartSize + margin)
            val y = margin

            val heartToDraw = if (i < supportManager.currentHP) heartFull else heartEmpty
            val scaledHeart = Bitmap.createScaledBitmap(heartToDraw, heartSize, heartSize, true)

            canvas.drawBitmap(scaledHeart, x.toFloat(), y.toFloat(), null)
        }

        // Coin
        val coinSize = 64
        val coinX = margin.toFloat()
        val coinY = (heartSize + margin * 2).toFloat()   // dưới tim

        val scaledCoin = Bitmap.createScaledBitmap(coinBitmap, coinSize, coinSize, true)
        canvas.drawBitmap(scaledCoin, coinX, coinY, null)

        val textPaint = Paint().apply {
            color = Color.YELLOW
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText(
            "x ${supportManager.coinCount}",
            coinX + coinSize + 10f,
            coinY + coinSize * 0.75f,
            textPaint
        )

        // Kill count (dưới coin)
        val killTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val killY = coinY + coinSize + 50f
        canvas.drawText("Kills: ${supportManager.killCount}", coinX, killY, killTextPaint)
    }

    private fun checkCollisions() {
        val characterRect = RectF(
            characterX - characterSize / 2f,
            characterY - characterSize / 2f,
            characterX + characterSize / 2f,
            characterY + characterSize / 2f
        )

        // ======================
        // Collision character <-> asteroid
        // ======================
        for (asteroid in asteroids) {
            val asteroidRect = RectF(
                asteroid.x - asteroid.size / 2f,
                asteroid.y - asteroid.size / 2f,
                asteroid.x + asteroid.size / 2f,
                asteroid.y + asteroid.size / 2f
            )
            if (RectF.intersects(characterRect, asteroidRect)) {
                val isDead = supportManager.takeDamage()
                if (isDead) {
                    endGame()
                    return
                } else {
                    // reset asteroid khi va chạm
                    asteroid.x = Random.nextFloat() * width
                    asteroid.y = -asteroid.size / 2f
                    asteroid.hitCount = 0
                }
            }
// Quái rơi chạm đáy màn hình => thua luôn
            if (asteroid.y + asteroid.size / 2f >= height) {
                endGame()
                return
            }
        }

        // ======================
        // Collision character <-> support items
        // ======================
        val itemIterator = supportItems.iterator()
        while (itemIterator.hasNext()) {
            val item = itemIterator.next()
            if (RectF.intersects(characterRect, item.getRect())) {
                itemIterator.remove()
                when (item.type) {
                    SupportItem.Type.BULLET_UPGRADE -> {
                        bulletLevel = (bulletLevel + 1).coerceAtMost(5)
                    }
                    SupportItem.Type.BIG_BULLET -> shootBigBullet()
                    SupportItem.Type.LASER -> shootLaser()
                    SupportItem.Type.BOMB -> {
                        bombMode = true
                        bombModeEndTime = System.currentTimeMillis() + 10_000
                    }
                    SupportItem.Type.SHIELD -> supportManager.activateShield(3)
                    SupportItem.Type.HEART -> supportManager.heal(1)
                    SupportItem.Type.COIN -> supportManager.addCoin(1)   // ➕ coin
                }
            }
        }

        // ======================
        // Collision laser <-> asteroid
        // ======================
        lasers.forEach { laser ->
            val laserRect = RectF(laser.x - 10, 0f, laser.x + 10, laser.y)
            asteroids.forEach { asteroid ->
                val asteroidRect = RectF(
                    asteroid.x - asteroid.size / 2f,
                    asteroid.y - asteroid.size / 2f,
                    asteroid.x + asteroid.size / 2f,
                    asteroid.y + asteroid.size / 2f
                )
                if (RectF.intersects(laserRect, asteroidRect)) {
                    asteroid.x = Random.nextFloat() * width
                    asteroid.y = -asteroid.size / 2f
                    asteroid.hitCount = 0
                    if (isSoundOn) {
                        soundPool.play(soundExplosion, 1f, 1f, 1, 0, 1f)
                    }
                }
            }
        }

        // ======================
        // Collision bullet <-> asteroid
        // ======================
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
                        supportManager.killCount++
                        asteroid.x = Random.nextFloat() * width
                        asteroid.y = -asteroid.size / 2f
                        asteroid.hitCount = 0
                        if (isSoundOn) {
                            soundPool.play(soundExplosion, 1f, 1f, 1, 0, 1f)
                        }
                        // TODO: hiệu ứng nổ asteroid
                    }
                    break
                }
            }
        }
        val enemyBulletIterator = enemyBullets.iterator()
        while (enemyBulletIterator.hasNext()) {
            val bullet = enemyBulletIterator.next()
            val bulletRect = RectF(
                bullet.x - bullet.size / 2f,
                bullet.y - bullet.size / 2f,
                bullet.x + bullet.size / 2f,
                bullet.y + bullet.size / 2f
            )
            if (RectF.intersects(characterRect, bulletRect)) {
                enemyBulletIterator.remove()
                val isDead = supportManager.takeDamage()
                if (isDead) {
                    endGame()
                    return
                }
            }
        }
    }
    private fun drawBombs(canvas: Canvas) {
        bombs.forEach { bomb ->
            if (!bomb.isExploded) {
                // Vẽ quả bom xanh lá cây với viền đậm
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.GREEN
                    style = Paint.Style.FILL
                }
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(0, 150, 0) // xanh đậm
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                }

                canvas.drawCircle(bomb.x, bomb.y, bomb.size / 2f, fillPaint)
                canvas.drawCircle(bomb.x, bomb.y, bomb.size / 2f, strokePaint)

            } else {
                // Hiệu ứng nổ với gradient
                val gradient = RadialGradient(
                    bomb.x, bomb.y, bomb.explodeRadius,
                    intArrayOf(Color.YELLOW, Color.RED, Color.TRANSPARENT),
                    floatArrayOf(0.2f, 0.7f, 1f),
                    Shader.TileMode.CLAMP
                )
                val explosionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = gradient
                    style = Paint.Style.FILL
                }

                // Vẽ vòng tròn nổ
                canvas.drawCircle(bomb.x, bomb.y, bomb.explodeRadius, explosionPaint)

                // Thêm sóng tròn đỏ bên ngoài
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.RED
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                    alpha = 180
                }
                canvas.drawCircle(bomb.x, bomb.y, bomb.explodeRadius, ringPaint)
            }
        }
    }

    private fun shootBigBullet() {
        bullets.add(Bullet(
            x = characterX,
            y = characterY - characterSize / 2f,
            size = 60f, // gấp 3 lần size mặc định
            speed = 1000f // có thể nhanh hơn đạn thường
        ))
        if (isSoundOn) {
            soundPool.play(soundShoot, 0.8f, 0.8f, 1, 0, 1f)
        }
    }
    private val lasers = mutableListOf<Laser>()

    private fun shootLaser() {
        lasers.add(Laser(x = characterX, y = characterY - characterSize / 2f))
        if (isSoundOn) {
            soundPool.play(soundShoot, 1f, 1f, 1, 0, 1f)
        }
    }
    private fun updateLasers() {
        val current = System.currentTimeMillis()
        val iterator = lasers.iterator()
        while (iterator.hasNext()) {
            val laser = iterator.next()
            if (current - laser.spawnTime > laser.duration) {
                iterator.remove()
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
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        characterSize = min(w, h) * 0.15f
        characterX = w / 2f
        characterY = h - characterSize / 2f
        characterX = characterX.coerceIn(characterSize / 2f, (w - characterSize / 2f).toFloat())
        characterY = characterY.coerceIn(characterSize / 2f, (h - characterSize / 2f).toFloat())

        val buttonSize = min(w, h) * 0.1f
        soundButtonRect = RectF(0f, h/2f - buttonSize/2, buttonSize, h/2f + buttonSize/2)
        musicButtonRect = RectF(w - buttonSize, h/2f - buttonSize/2, w.toFloat(), h/2f + buttonSize/2)

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawScrollingBackground(canvas)
        drawAsteroids(canvas)
        drawBullets(canvas)
        drawCharacter(canvas)
        drawButtons(canvas)
        drawSupportItems(canvas)
        drawLasers(canvas)
        drawBombs(canvas)
        drawHearts(canvas)
        // Vẽ nhân vật
     //   canvas.drawBitmap(characterBitmap, characterX - characterSize/2f, characterY - characterSize/2f, null)

// Vẽ khiên bao quanh
        if (supportManager.isShieldActive()) {
            val shieldPaint = Paint().apply {
                color = Color.CYAN
                style = Paint.Style.STROKE
                strokeWidth = 8f
                isAntiAlias = true
            }
            // vòng ngoài
            canvas.drawCircle(characterX, characterY, characterSize.toFloat(), shieldPaint)

            // hiệu ứng glow
            shieldPaint.alpha = 100
            canvas.drawCircle(characterX, characterY, characterSize * 1.3f, shieldPaint)
        }

// Hiệu ứng nhấp nháy khi invincible
        if (supportManager.isInvincible) {
            val blinkPaint = Paint().apply {
                color = Color.WHITE
                alpha = ((System.currentTimeMillis() / 200) % 2 * 180).toInt() // nhấp nháy 200ms
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(characterX, characterY, characterSize.toFloat(), blinkPaint)
        }
        val paintEnemyBullet = Paint().apply {
            color = Color.RED
            isAntiAlias = true
        }
        enemyBullets.forEach { bullet ->
            canvas.drawCircle(bullet.x, bullet.y, bullet.size / 2f, paintEnemyBullet)
        }
    }
    private fun drawLasers(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.CYAN
            strokeWidth = 25f
            alpha = 180
        }
        lasers.forEach { laser ->
            canvas.drawLine(laser.x, laser.y, laser.x, 0f, paint)
        }
    }

    private fun drawButtons(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // ----- SOUND BUTTON -----
        // Vẽ nền trắng (to hơn icon một chút)
        val soundBg = RectF(
            soundButtonRect.left - 15,
            soundButtonRect.top - 15,
            soundButtonRect.right + 15,
            soundButtonRect.bottom + 15
        )
        paint.color = Color.WHITE
        canvas.drawRoundRect(soundBg, 20f, 20f, paint) // bo góc 20px

        // Vẽ icon sound
        val soundBmp = if (isSoundOn) soundBitmap else soundInactiveBitmap
        soundBmp?.let { bmp ->
            val src = Rect(0, 0, bmp.width, bmp.height)
            canvas.drawBitmap(bmp, src, soundButtonRect, null)
        }

        // ----- MUSIC BUTTON -----
        val musicBg = RectF(
            musicButtonRect.left - 15,
            musicButtonRect.top - 15,
            musicButtonRect.right + 15,
            musicButtonRect.bottom + 15
        )
        paint.color = Color.WHITE
        canvas.drawRoundRect(musicBg, 20f, 20f, paint)

        val musicBmp = if (isMusicOn) musicBitmap else musicInactiveBitmap
        musicBmp?.let { bmp ->
            val src = Rect(0, 0, bmp.width, bmp.height)
            canvas.drawBitmap(bmp, src, musicButtonRect, null)
        }
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

/*    private fun drawAsteroids(canvas: Canvas) {
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
    }*/
private fun drawAsteroids(canvas: Canvas) {
    asteroids.forEach { ast ->
        val bmp = ast.bitmap ?: return@forEach
        val halfSize = ast.size / 2f
        val srcRect = Rect(0, 0, bmp.width, bmp.height)
        val destRect = RectF(
            ast.x - halfSize,
            ast.y - halfSize,
            ast.x + halfSize,
            ast.y + halfSize
        )
        canvas.drawBitmap(bmp, srcRect, destRect, backgroundPaint)
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
   private var lastX = 0f
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Check bấm vào nút sound
                if (soundButtonRect.contains(x, y)) {
                    isSoundOn = !isSoundOn
                    isMusicOn = isSoundOn
                    if (isMusicOn) {
                        bgMusic?.start()
                    } else {
                        bgMusic?.pause()
                    }
                    return true
                }

                // Check bấm vào nút music
                if (musicButtonRect.contains(x, y)) {
                    isMusicOn = !isMusicOn
                    if (isMusicOn) {
                        bgMusic?.start()
                    } else {
                        bgMusic?.pause()
                    }
                    return true
                }

                // Nếu không bấm nút nào → di chuyển máy bay
                characterX = x
                characterY = y
            }

            MotionEvent.ACTION_MOVE -> {
                // Di chuyển máy bay theo tay
                characterX = x
                characterY = y
            }
        }

        // Giữ trong màn hình
        characterX = characterX.coerceIn(characterSize / 2f, width - characterSize / 2f)
        characterY = characterY.coerceIn(characterSize / 2f, height - characterSize / 2f)

        invalidate()
        return true
    }

    /*
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {
                    val x = event.x
                    val y = event.y
                    if (soundButtonRect.contains(x, y)) {
                        isSoundOn = !isSoundOn
                        isMusicOn=isSoundOn
                        if (isMusicOn) {
                            bgMusic?.start()
                        } else {
                            bgMusic?.pause()
                        }
                        return true
                    }
                    if (musicButtonRect.contains(x, y)) {
                        isMusicOn = !isMusicOn

                        if (isMusicOn) {
                            bgMusic?.start()
                        } else {
                            bgMusic?.pause()
                        }
                        return true
                    }
                    lastX = event.x
                    spawnBullet() // bấm xuống thì bắn đạn
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - lastX
                    if (Math.abs(dx) > 10) { // ngưỡng để nhận là vuốt
                        characterX += dx   // di chuyển ngang
                        lastX = event.x
                    }

                    // Check biên trong ACTION_MOVE
                    if (characterX - characterSize / 2f < 0) {
                        characterX = characterSize / 2f
                        if(isSoundOn)
                        soundPool.play(soundWall, 1f, 1f, 1, 0, 1f)
                    } else if (characterX + characterSize / 2f > width) {
                        characterX = width - characterSize / 2f
                        if(isSoundOn)
                        soundPool.play(soundWall, 1f, 1f, 1, 0, 1f)
                    }
                }
            }

            invalidate()
            return true
        }
    */


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
