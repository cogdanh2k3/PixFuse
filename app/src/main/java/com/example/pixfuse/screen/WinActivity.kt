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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback

class WinActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SCORE = "extra_score"
        const val EXTRA_HIGH_SCORE = "extra_high_score"
        const val EXTRA_LEVEL = "extra_level"
    }

    private lateinit var backgroundView: View
    private lateinit var titleText: TextView
    private lateinit var scoreText: TextView
    private lateinit var highScoreText: TextView
    private lateinit var tryAgainButton: Button
    private lateinit var nextLevelButton: Button
    private lateinit var menuButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val score = intent.getIntExtra(EXTRA_SCORE, 0)
        val highScore = intent.getIntExtra(EXTRA_HIGH_SCORE, 0)
        val level = intent.getIntExtra(EXTRA_LEVEL, 1)

        setupViews(score, highScore, level)
        setupAnimations()
        setupClickListeners()

        // Thiết lập OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Quay lại MenuActivity
                val intent = Intent(this@WinActivity, MenuActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
        })
    }

    private fun setupViews(score: Int, highScore: Int, level: Int) {
        backgroundView = View(this).apply {
            setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparent overlay
        }
        setContentView(backgroundView)

        // Title
        titleText = TextView(this).apply {
            text = "YOU WIN!"
            textSize = 42f
            setTextColor(Color.parseColor("#EDC22E"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        // Score display
        scoreText = TextView(this).apply {
            text = "Score: $score"
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        // High score display
        highScoreText = TextView(this).apply {
            text = "Best: $highScore"
            textSize = 24f
            setTextColor(Color.parseColor("#FFD700"))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        // Buttons
        tryAgainButton = createStyledButton("TRY AGAIN", Color.parseColor("#4CAF50"))
        nextLevelButton = createStyledButton("NEXT LEVEL", Color.parseColor("#2196F3"))
        menuButton = createStyledButton("MAIN MENU", Color.parseColor("#FF9800"))

        // Add views
        (backgroundView as ViewGroup).apply {
            addView(titleText)
            addView(scoreText)
            addView(highScoreText)
            addView(tryAgainButton)
            addView(nextLevelButton)
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
            titleText.layout(
                (width - titleText.measuredWidth) / 2,
                centerY - 200,
                (width + titleText.measuredWidth) / 2,
                centerY - 200 + titleText.measuredHeight
            )

            // Score texts
            scoreText.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            scoreText.layout(
                (width - scoreText.measuredWidth) / 2,
                centerY - 100,
                (width + scoreText.measuredWidth) / 2,
                centerY - 100 + scoreText.measuredHeight
            )

            highScoreText.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            highScoreText.layout(
                (width - highScoreText.measuredWidth) / 2,
                centerY - 40,
                (width + highScoreText.measuredWidth) / 2,
                centerY - 40 + highScoreText.measuredHeight
            )

            // Buttons
            val buttonWidth = width * 2 / 3
            val buttonHeight = 70

            tryAgainButton.layout(
                (width - buttonWidth) / 2,
                centerY + 50,
                (width + buttonWidth) / 2,
                centerY + 50 + buttonHeight
            )

            nextLevelButton.layout(
                (width - buttonWidth) / 2,
                centerY + 140,
                (width + buttonWidth) / 2,
                centerY + 140 + buttonHeight
            )

            menuButton.layout(
                (width - buttonWidth) / 2,
                centerY + 230,
                (width + buttonWidth) / 2,
                centerY + 230 + buttonHeight
            )
        }
    }

    private fun setupAnimations() {
        // Scale in animation for all views
        val views = listOf(titleText, scoreText, highScoreText, tryAgainButton, nextLevelButton, menuButton)
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
            intent.putExtra(EXTRA_LEVEL, intent.getIntExtra(EXTRA_LEVEL, 1))
            startActivity(intent)
            finish()
        }

        nextLevelButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(EXTRA_LEVEL, intent.getIntExtra(EXTRA_LEVEL, 1) + 1)
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