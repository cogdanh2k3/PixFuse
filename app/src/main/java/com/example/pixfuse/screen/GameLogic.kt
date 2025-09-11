package com.example.pixfuse.screen

import android.content.Context
import android.graphics.*
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.collections.get
import kotlin.math.*
import kotlin.random.Random

// Animation class for smooth tile movements
data class TileAnimation(
    val value: Int,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    var elapsed: Float = 0f,
    val duration: Float = 0.15f
) {
    private val interpolator = AccelerateDecelerateInterpolator()

    fun update(deltaTime: Float): Boolean {
        elapsed += deltaTime
        return elapsed >= duration
    }

    fun getCurrentPosition(): Pair<Float, Float> {
        val progress = (elapsed / duration).coerceIn(0f, 1f)
        val smoothProgress = interpolator.getInterpolation(progress)

        val x = startX + (endX - startX) * smoothProgress
        val y = startY + (endY - startY) * smoothProgress

        return Pair(x, y)
    }
}

// Spawn animation for new tiles
data class SpawnAnimation(
    val value: Int,
    val row: Int,
    val col: Int,
    var elapsed: Float = 0f,
    val duration: Float = 0.25f
) {
    fun update(deltaTime: Float): Boolean {
        elapsed += deltaTime
        return elapsed >= duration
    }

    fun getScale(): Float {
        val progress = (elapsed / duration).coerceIn(0f, 1f)
        return 0.3f + 0.7f * sin(progress * PI.toFloat() / 2f)
    }
}

// Board class managing the game grid and animations
class Board(val size: Int) {
    private val grid = Array(size) { IntArray(size) { 0 } }
    private val animations = mutableListOf<TileAnimation>()
    private val spawnAnimations = mutableListOf<SpawnAnimation>()

    var boardStartX = 0f
    var boardStartY = 0f
    var tileSize = 0f
    var padding = 0f

    fun updateLayout(startX: Float, startY: Float, size: Float, pad: Float) {
        boardStartX = startX
        boardStartY = startY
        tileSize = size
        padding = pad
    }

    fun getTile(row: Int, col: Int): Int = grid[row][col]

    fun setTile(row: Int, col: Int, value: Int) {
        grid[row][col] = value
    }

    fun getEmptyCells(): List<Pair<Int, Int>> {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (grid[row][col] == 0) {
                    emptyCells.add(row to col)
                }
            }
        }
        return emptyCells
    }

    private fun gridToPosition(row: Int, col: Int): Pair<Float, Float> {
        val x = boardStartX + col * (tileSize + padding)
        val y = boardStartY + row * (tileSize + padding)
        return Pair(x, y)
    }

    fun addMoveAnimation(value: Int, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        val (startX, startY) = gridToPosition(fromRow, fromCol)
        val (endX, endY) = gridToPosition(toRow, toCol)
        animations.add(TileAnimation(value, startX, startY, endX, endY))
    }

    fun addSpawnAnimation(row: Int, col: Int, value: Int) {
        spawnAnimations.add(SpawnAnimation(value, row, col))
    }

    fun update(deltaTime: Float) {
        // Update move animations
        val animIterator = animations.iterator()
        while (animIterator.hasNext()) {
            val anim = animIterator.next()
            if (anim.update(deltaTime)) {
                animIterator.remove()
            }
        }

        // Update spawn animations
        val spawnIterator = spawnAnimations.iterator()
        while (spawnIterator.hasNext()) {
            val spawn = spawnIterator.next()
            if (spawn.update(deltaTime)) {
                spawnIterator.remove()
            }
        }
    }

    fun draw(canvas: Canvas, tileBitmaps: Map<Int, Bitmap>) {
        // Draw static tiles
        for (row in 0 until size) {
            for (col in 0 until size) {
                val value = grid[row][col]
                if (value != 0) {
                    val (x, y) = gridToPosition(row, col)
                    tileBitmaps[value]?.let { bitmap ->
                        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                        val destRect = RectF(x, y, x + tileSize, y + tileSize)
                        canvas.drawBitmap(bitmap, srcRect, destRect, null)
                    }
                }
            }
        }

        // Draw moving tiles (on top to avoid overlap issues)
        for (anim in animations) {
            val (x, y) = anim.getCurrentPosition()
            tileBitmaps[anim.value]?.let { bitmap ->
                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                val destRect = RectF(x, y, x + tileSize, y + tileSize)
                canvas.drawBitmap(bitmap, srcRect, destRect, null)
            }
        }

        // Draw spawning tiles with scale animation
        for (spawn in spawnAnimations) {
            val (x, y) = gridToPosition(spawn.row, spawn.col)
            val scale = spawn.getScale()
            val scaledSize = tileSize * scale
            val offset = (tileSize - scaledSize) / 2f

            tileBitmaps[spawn.value]?.let { bitmap ->
                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                val destRect = RectF(
                    x + offset, y + offset,
                    x + offset + scaledSize, y + offset + scaledSize
                )
                canvas.drawBitmap(bitmap, srcRect, destRect, null)
            }
        }
    }
}

// Game manager handling game logic
class GameManager(private val board: Board, private val context: Context) {
    var score = 0
        private set
    var highScore = 0
        private set
    var isMoved = false
    var hasWon = false
        private set
    var hasLost = false
        private set

    private val prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)

    init {
        highScore = prefs.getInt("high_score", 0)
    }

    fun spawnTile() {
        if (hasWon || hasLost) return

        val emptyCells = board.getEmptyCells()
        if (emptyCells.isNotEmpty()) {
            val (row, col) = emptyCells.random()
            val value = if (Random.nextFloat() < 0.9f) 2 else 4
            board.setTile(row, col, value)
            board.addSpawnAnimation(row, col, value)
        }
    }

    fun update() {
        if (isMoved) {
            spawnTile()
            checkWin()
            checkLose()
            isMoved = false

            if (score > highScore) {
                highScore = score
                saveHighScore()
            }
        }
    }

    private fun checkWin() {
        if (hasWon) return
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                if (board.getTile(row, col) == 2048) {
                    hasWon = true
                    return
                }
            }
        }
    }

    private fun checkLose() {
        if (hasLost || hasWon) return
        if (board.getEmptyCells().isNotEmpty()) return

        // Check if any moves are possible
        for (row in 0 until board.size) {
            for (col in 0 until board.size) {
                val currentValue = board.getTile(row, col)

                // Check right neighbor
                if (col + 1 < board.size && currentValue == board.getTile(row, col + 1)) {
                    return
                }

                // Check bottom neighbor
                if (row + 1 < board.size && currentValue == board.getTile(row + 1, col)) {
                    return
                }
            }
        }

        hasLost = true
    }

    fun saveHighScore() {
        prefs.edit().putInt("high_score", highScore).apply()
    }

    fun moveLeft() = moveRows(false)
    fun moveRight() = moveRows(true)
    fun moveUp() = moveColumns(false)
    fun moveDown() = moveColumns(true)

    private fun moveRows(reversed: Boolean) {
        var moved = false

        for (row in 0 until board.size) {
            val line = (0 until board.size).map { board.getTile(row, it) }
            val (newLine, animations) = processLine(line, reversed, row, true)

            // Apply changes to board
            for (col in 0 until board.size) {
                if (board.getTile(row, col) != newLine[col]) {
                    moved = true
                    board.setTile(row, col, newLine[col])
                }
            }

            // Add animations
            animations.forEach { board.addMoveAnimation(it.value, it.fromRow, it.fromCol, it.toRow, it.toCol) }
        }

        isMoved = moved
    }

    private fun moveColumns(reversed: Boolean) {
        var moved = false

        for (col in 0 until board.size) {
            val line = (0 until board.size).map { board.getTile(it, col) }
            val (newLine, animations) = processLine(line, reversed, col, false)

            // Apply changes to board
            for (row in 0 until board.size) {
                if (board.getTile(row, col) != newLine[row]) {
                    moved = true
                    board.setTile(row, col, newLine[row])
                }
            }

            // Add animations
            animations.forEach { board.addMoveAnimation(it.value, it.fromRow, it.fromCol, it.toRow, it.toCol) }
        }

        isMoved = moved
    }

    data class MoveAnimation(
        val value: Int,
        val fromRow: Int,
        val fromCol: Int,
        val toRow: Int,
        val toCol: Int
    )

    private fun processLine(
        line: List<Int>,
        reversed: Boolean,
        fixedIndex: Int,
        isRow: Boolean
    ): Pair<List<Int>, List<MoveAnimation>> {

        val workingLine = if (reversed) line.reversed() else line
        val nonZero = workingLine.filter { it != 0 }.toMutableList()
        val merged = mutableListOf<Int>()
        val animations = mutableListOf<MoveAnimation>()

        // Track original positions for animations
        val positions = workingLine.mapIndexed { index, value -> if (value != 0) index else -1 }.filter { it != -1 }.toMutableList()

        var i = 0
        while (i < nonZero.size) {
            if (i < nonZero.size - 1 && nonZero[i] == nonZero[i + 1]) {
                val mergedValue = nonZero[i] * 2
                score += mergedValue
                merged.add(mergedValue)

                // Merge animation: both tiles move to the same position
                val targetPos = merged.size - 1
                val actualTargetPos = if (reversed) board.size - 1 - targetPos else targetPos

                val fromPos1 = positions.removeAt(0)
                val fromPos2 = positions.removeAt(0)

                val actualFrom1 = if (reversed) board.size - 1 - fromPos1 else fromPos1
                val actualFrom2 = if (reversed) board.size - 1 - fromPos2 else fromPos2

                if (isRow) {
                    animations.add(MoveAnimation(nonZero[i], fixedIndex, actualFrom1, fixedIndex, actualTargetPos))
                    animations.add(MoveAnimation(nonZero[i + 1], fixedIndex, actualFrom2, fixedIndex, actualTargetPos))
                } else {
                    animations.add(MoveAnimation(nonZero[i], actualFrom1, fixedIndex, actualTargetPos, fixedIndex))
                    animations.add(MoveAnimation(nonZero[i + 1], actualFrom2, fixedIndex, actualTargetPos, fixedIndex))
                }

                i += 2
            } else {
                merged.add(nonZero[i])

                // Move animation
                val targetPos = merged.size - 1
                val actualTargetPos = if (reversed) board.size - 1 - targetPos else targetPos

                val fromPos = positions.removeAt(0)
                val actualFrom = if (reversed) board.size - 1 - fromPos else fromPos

                if (actualFrom != actualTargetPos) {
                    if (isRow) {
                        animations.add(MoveAnimation(nonZero[i], fixedIndex, actualFrom, fixedIndex, actualTargetPos))
                    } else {
                        animations.add(MoveAnimation(nonZero[i], actualFrom, fixedIndex, actualTargetPos, fixedIndex))
                    }
                }

                i++
            }
        }

        // Fill with zeros
        while (merged.size < board.size) {
            merged.add(0)
        }

        val finalLine = if (reversed) merged.reversed() else merged

        return Pair(finalLine, animations)
    }
}