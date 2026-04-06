package com.example.piceditor;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class AlbumViewActivity extends AppCompatActivity {

    private static final String TAG = "AlbumViewActivity";

    private RecyclerView recyclerView;
    private GalleryAdapter adapter; // Sử dụng GalleryAdapter để hiển thị ảnh
    private ArrayList<String> imagePaths;
    private TextView totalImagesTextView;
    private Toolbar toolbar;
    private String albumName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_view);

        toolbar = findViewById(R.id.toolbar_album_view);
        recyclerView = findViewById(R.id.album_view_recycler);
        totalImagesTextView = findViewById(R.id.album_view_total_images);
        imagePaths = new ArrayList<>();

        // Lấy dữ liệu từ Intent
        albumName = getIntent().getStringExtra("album_name");
        ArrayList<String> pathsFromIntent = getIntent().getStringArrayListExtra("image_paths");

        if (albumName == null || pathsFromIntent == null) {
            Log.e(TAG, "Không nhận được tên album hoặc danh sách ảnh từ Intent.");
            Toast.makeText(this, "Lỗi khi mở album", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        imagePaths.addAll(pathsFromIntent);

        // Sắp xếp ảnh theo ngày sửa đổi (hoặc tiêu chí khác)
        Collections.sort(imagePaths, (path1, path2) ->
                Long.compare(new java.io.File(path2).lastModified(), new java.io.File(path1).lastModified())
        );


        // --- Cài đặt Toolbar ---
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(albumName); // Đặt tên album làm tiêu đề
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Hiển thị nút back
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        // ---------------------

        adapter = new GalleryAdapter(this, imagePaths); // Dùng GalleryAdapter bình thường
        GridLayoutManager manager = new GridLayoutManager(this, 3); // 3 hoặc 4 cột

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(manager);

        updateTotalImages();
        checkEmptyView();
    }

    // Xử lý nút back trên Toolbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Đóng activity hiện tại
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Load lại ảnh khi quay lại từ ViewPicture (nếu có ảnh bị xóa khỏi album trong tương lai)
    @Override
    protected void onResume() {
        super.onResume();
        // Trong trường hợp này, dữ liệu được truyền qua Intent nên không cần load lại
        // Trừ khi có chức năng xóa ảnh khỏi album ngay trong activity này
        // Nếu có, cần load lại từ SharedPreferences hoặc nguồn dữ liệu khác.
        Log.d(TAG,"onResume AlbumViewActivity");
        // checkEmptyView(); // Cập nhật lại nếu có thể xóa ảnh trong view này
    }


    private void updateTotalImages() {
        totalImagesTextView.setText("Tổng số: " + imagePaths.size());
    }

    private void checkEmptyView() {
        if (imagePaths.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            findViewById(R.id.empty_view_album_details).setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            findViewById(R.id.empty_view_album_details).setVisibility(View.GONE);
        }
    }
}