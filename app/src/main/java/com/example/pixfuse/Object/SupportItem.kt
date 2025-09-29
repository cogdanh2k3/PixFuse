package com.example.pixfuse.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import com.example.pixfuse.R
import kotlin.random.Random

class SupportItem(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    private val bitmap: Bitmap,
    val type: Type
) {
    enum class Type {
        BULLET_UPGRADE,
        BIG_BULLET,
        LASER,
        BOMB ,               // ✅ loại mới
        HEART,
        SHIELD
    }

    fun update(deltaTime: Float, screenHeight: Float): Boolean {
        y += speed * deltaTime
        return y > screenHeight + size
    }

    fun draw(canvas: Canvas) {
        val rect = RectF(
            x - size / 2f,
            y - size / 2f,
            x + size / 2f,
            y + size / 2f
        )
        canvas.drawBitmap(bitmap, null, rect, null)
    }

    fun getRect(): RectF {
        return RectF(
            x - size / 2f,
            y - size / 2f,
            x + size / 2f,
            y + size / 2f
        )
    }

    companion object {
        fun spawn(context: Context, screenWidth: Float, screenHeight: Float): SupportItem {
            val size = screenWidth * 0.1f
            val x = Random.nextFloat() * (screenWidth - size)
            val y = -size
            val speed = 150f + Random.nextFloat() * 100f

            // random loại item
            val type = Type.values().random()
            val resId = when (type) {
                Type.BULLET_UPGRADE -> R.drawable.addbullet
                Type.BIG_BULLET -> R.drawable.bigbullet
                Type.LASER -> R.drawable.laser
                Type.BOMB -> R.drawable.bomb      // cần icon mới
                Type.HEART -> R.drawable.full_heart
                Type.SHIELD -> R.drawable.shield
            }
            val bitmap = BitmapFactory.decodeResource(context.resources, resId)

            return SupportItem(x, y, size, speed, bitmap, type)
        }
    }
}

