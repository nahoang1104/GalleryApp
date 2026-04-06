package com.example.piceditor;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CreateSlideShowActivity extends AppCompatActivity {
    ImageView cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_slide_show);
        cancelButton = findViewById(R.id.cancel_icon);

        cancelButton.setOnClickListener(v -> {
            finish();
        });
    }
}