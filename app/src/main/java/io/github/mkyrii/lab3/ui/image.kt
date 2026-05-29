package io.github.mkyrii.lab3.ui

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import io.github.mkyrii.lab3.R
import androidx.activity.OnBackPressedCallback

class ImageActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        imageView = findViewById(R.id.ivFullImage)

        val imageUrl = intent.getStringExtra("image_url")
        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .into(imageView)
        }

        // Клик по картинке - закрыть
        imageView.setOnClickListener {
            finish()
        }

        // Обработка кнопки "назад" через OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }
}