package com.example.pixfuse.Object

import android.graphics.Bitmap

data class Asteroid(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    var hitCount: Int = 0,
    var bitmap: Bitmap? = null ,  // thêm sprite riêng cho mỗi asteroid
    var canShoot: Boolean = false,   // 👈 thêm
    var lastShotTime: Long = 0L      // 👈 để bắn theo giây
)
