package com.example.pixfuse.Object

data class Bullet(
    var x: Float,
    var y: Float,
    val speed: Float = 800f,   // px/s
    val size: Float = 20f      // đường kính viên đạn
)
