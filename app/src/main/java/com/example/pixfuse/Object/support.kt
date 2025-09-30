package com.example.pixfuse.Object

class SupportManager {

    var bombMode = false
    private var bombModeEndTime = 0L

    var shieldCount = 0

    var isInvincible = false
    private var invincibleEndTime = 0L

    var currentHP = 3
    var maxHP = 5

    fun activateBombMode(duration: Long = 10_000) {
        bombMode = true
        bombModeEndTime = System.currentTimeMillis() + duration
    }

    fun activateShield(count: Int = 3) {
        shieldCount = count
    }

    fun heal(amount: Int = 1) {
        currentHP = (currentHP + amount).coerceAtMost(maxHP)
    }

    fun takeDamage(): Boolean {
        if (isInvincible) return false

        if (shieldCount > 0) {
            shieldCount--
            return false
        }

        currentHP--
        if (currentHP <= 0) {
            // chết game over
            return true
        }

        // bật trạng thái miễn nhiễm 5s sau khi mất máu
        isInvincible = true
        invincibleEndTime = System.currentTimeMillis() + 5_000
        return false
    }

    fun update() {
        val now = System.currentTimeMillis()

        if (bombMode && now > bombModeEndTime) {
            bombMode = false
        }

        if (isInvincible && now > invincibleEndTime) {
            isInvincible = false
        }
    }
    fun isShieldActive(): Boolean {
        return shieldCount > 0
    }
    var coinCount = 0

    fun addCoin(amount: Int = 1) {
        coinCount += amount
    }
    var killCount = 0   // số kẻ địch bị hạ
}
