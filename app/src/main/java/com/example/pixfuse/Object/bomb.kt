package com.example.pixfuse.Object

data class Bomb(
    var x: Float,
    var y: Float,
    val size: Float = 40f,
    val speed: Float = 500f,
    var isExploded: Boolean = false,
    var explodeRadius: Float = 150f,
    var explodeTime: Long = 0L
)