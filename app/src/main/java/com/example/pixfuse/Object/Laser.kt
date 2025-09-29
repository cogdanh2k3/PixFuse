package com.example.pixfuse.Object

data class Laser(
    var x: Float,
    var y: Float,
    val width: Float = 20f,
    val duration: Long = 300, // tồn tại 0.3s
    var spawnTime: Long = System.currentTimeMillis()
)
