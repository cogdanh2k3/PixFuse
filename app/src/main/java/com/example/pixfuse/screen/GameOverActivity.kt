package com.example.pixfuse.screen

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class GameOverActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IS_WIN = "extra_is_win"
    }

    private lateinit var backgroundView: FrameLayout
    private lateinit var titleText: TextView
    private lateinit var tryAgainButton: Button
    private lateinit var menuButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        val isWin = intent.getBooleanExtra(EXTRA_IS_WIN, false)

        setupViews(isWin)
        setupAnimations()
        setupClickListeners()

        // Thiết lập OnBackPressedCallback để chỉ xử lý khi nhấn back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Không tự động quay về menu, để người dùng quyết định
                // Hoặc bạn có thể thêm logic quay về menu nếu muốn
            }
        })
    }

    private fun setupViews(isWin: Boolean) {
        backgroundView = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparent overlay
        }
        setContentView(backgroundView)

        // Title
        titleText = TextView(this).apply {
            text = if (isWin) "YOU WIN!" else "GAME OVER!"
            textSize = 42f
            setTextColor(if (isWin) Color.parseColor("#EDC22E") else Color.parseColor("#EE4C2C"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        // Buttons
        tryAgainButton = createStyledButton("TRY AGAIN", Color.parseColor("#4CAF50"))
        menuButton = createStyledButton("MAIN MENU", Color.parseColor("#FF9800"))

        // Add views
        backgroundView.apply {
            addView(titleText)
            addView(tryAgainButton)
            addView(menuButton)
        }

        layoutViews()
    }

    private fun createStyledButton(text: String, color: Int): Button {
        return Button(this).apply {
            this.text = text
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = createButtonDrawable(color)
            elevation = 8f
        }
    }

    private fun createButtonDrawable(color: Int): Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f
            setColor(color)
        }
    }

    private fun layoutViews() {
        backgroundView.post {
            val width = backgroundView.width
            val height = backgroundView.height

            // Center everything vertically
            val centerY = height / 2

            // Title
            titleText.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val titleParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = centerY - 100
            }
            titleText.layoutParams = titleParams

            // Buttons
            val buttonWidth = width * 2 / 3
            val buttonHeight = 70
            val buttonParams = FrameLayout.LayoutParams(
                buttonWidth,
                buttonHeight
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }

            tryAgainButton.layoutParams = buttonParams
            tryAgainButton.layout(
                (width - buttonWidth) / 2,
                centerY + 50,
                (width + buttonWidth) / 2,
                centerY + 50 + buttonHeight
            )

            menuButton.layoutParams = buttonParams
            menuButton.layout(
                (width - buttonWidth) / 2,
                centerY + 140,
                (width + buttonWidth) / 2,
                centerY + 140 + buttonHeight
            )
        }
    }

    private fun setupAnimations() {
        // Scale in animation for all views
        val views = listOf(titleText, tryAgainButton, menuButton)
        views.forEachIndexed { index, view ->
            view.scaleX = 0f
            view.scaleY = 0f
            view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setStartDelay((index * 100).toLong())
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun setupClickListeners() {
        tryAgainButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        menuButton.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }
}