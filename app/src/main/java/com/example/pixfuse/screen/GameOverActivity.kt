package com.example.pixfuse.screen

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.pixfuse.R

class GameOverActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IS_WIN = "extra_is_win"
    }

    private lateinit var backgroundView: FrameLayout
    private lateinit var loseImage: ImageView
    private lateinit var titleText: TextView
    private lateinit var tryAgainButton: Button
    private lateinit var menuButton: Button
    private lateinit var shopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        val isWin = intent.getBooleanExtra(EXTRA_IS_WIN, false)

        setupViews(isWin)
        setupAnimations()
        setupClickListeners()

        // Xử lý khi nhấn back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Không làm gì cả
            }
        })
    }

    private fun setupViews(isWin: Boolean) {
        backgroundView = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparent overlay
        }
        setContentView(backgroundView)

        // Lose image (chỉ hiển thị khi thua)
        loseImage = ImageView(this).apply {
            if (!isWin) {
                setImageResource(R.drawable.youlose) // ảnh you_lose.png trong res/drawable
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
            } else {
                visibility = ImageView.GONE
            }
        }

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
        shopButton = createStyledButton("SHOP", Color.parseColor("#2196F3"))

        // Add views
        backgroundView.apply {
            addView(loseImage)
            addView(titleText)
            addView(tryAgainButton)
            addView(menuButton)
            addView(shopButton)
        }

        layoutViews()
    }

/*    private fun createStyledButton(text: String, color: Int): Button {
        return Button(this).apply {
            this.text = text
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = createButtonDrawable(color)
            elevation = 8f
        }
    }*/

    private fun createButtonDrawable(color: Int): Drawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f
            setColor(color)
        }
    }

    private fun createStyledButton(text: String, color: Int): Button {
        return Button(this).apply {
            this.text = text
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = createButtonDrawable(color)
            elevation = 10f
            setPadding(40, 0, 40, 0) // tăng khoảng cách bên trong nút
            isAllCaps = false        // giữ nguyên chữ hoa thường
        }
    }

    private fun layoutViews() {
        backgroundView.post {
            val width = backgroundView.width
            val height = backgroundView.height

            // Lose image
            loseImage.layoutParams = FrameLayout.LayoutParams(
                width / 2,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = height / 8   // đặt cao hơn, cách trên khoảng 1/8 màn hình
            }

            // Title
            val titleParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = height / 3   // đặt title ngay dưới ảnh
            }
            titleText.layoutParams = titleParams

            // Buttons
            val buttonWidth = width * 2 / 3
            val buttonHeight = 120

            tryAgainButton.layoutParams = FrameLayout.LayoutParams(buttonWidth, buttonHeight).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = height / 2
            }

            menuButton.layoutParams = FrameLayout.LayoutParams(buttonWidth, buttonHeight).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = height / 2 + 170
            }

            shopButton.layoutParams = FrameLayout.LayoutParams(buttonWidth, buttonHeight).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = height / 2 + 340
            }
        }
    }


    private fun setupAnimations() {
        val views = listOf(loseImage, titleText, tryAgainButton, menuButton, shopButton)
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

        shopButton.setOnClickListener {
            // Tạm thời chưa cần chức năng
        }
    }
}
