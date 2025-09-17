package com.example.pixfuse.Object

data class Asteroid(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    var hitCount: Int = 0   // số lần trúng đạn
)
