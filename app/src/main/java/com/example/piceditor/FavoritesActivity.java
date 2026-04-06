package com.example.piceditor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar; // Thêm Toolbar


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

public class FavoritesActivity extends AppCompatActivity {

    private static final String TAG = "FavoritesActivity";
    private RecyclerView recycler;
    private ArrayList<String> favoriteImages;
    private GalleryAdapter adapter;
    private TextView totalImagesTextView;
    private ImageHelper imageHelper;
    private Toolbar toolbar; // Thêm Toolbar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites); // Tạo layout này

        imageHelper = new ImageHelper();
        toolbar = findViewById(R.id.toolbar_favorites); // Ánh xạ Toolbar
        recycler = findViewById(R.id.favorites_recycler); // Đổi ID recycler
        totalImagesTextView = findViewById(R.id.favorites_total_images); // Đổi ID text view
        favoriteImages = new ArrayList<>();
        adapter = new GalleryAdapter(this, favoriteImages); // Adapter bình thường
        GridLayoutManager manager = new GridLayoutManager(this, 3);

        // --- Cài đặt Toolbar ---
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.anh_yeu_thich); // Đặt tiêu đề
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Hiển thị nút back
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        // ---------------------

        recycler.setAdapter(adapter);
        recycler.setLayoutManager(manager);

        loadFavoriteImages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load lại ảnh khi quay lại activity này để cập nhật nếu có thay đổi
        loadFavoriteImages();
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


    private void loadFavoriteImages() {
        Log.d(TAG, "Đang tải ảnh yêu thích...");
        favoriteImages.clear();
        Set<String> favoritePaths = imageHelper.getFavoriteImagePaths(this);

        if (favoritePaths.isEmpty()) {
            Log.d(TAG, "Không có ảnh yêu thích nào.");
            Toast.makeText(this, R.string.khong_co_anh_nao, Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "Tìm thấy " + favoritePaths.size() + " ảnh yêu thích.");
            // Lọc bỏ những file không còn tồn tại
            for (String path : favoritePaths) {
                if (new File(path).exists()) {
                    favoriteImages.add(path);
                } else {
                    Log.w(TAG, "Ảnh yêu thích không còn tồn tại: " + path + ". Sẽ xóa khỏi danh sách yêu thích.");
                    // Tự động xóa khỏi danh sách yêu thích nếu file không tồn tại
                    imageHelper.removeFavorite(this, path);
                }
            }
            // Sắp xếp theo ngày mới nhất (tùy chọn)
            Collections.sort(favoriteImages, (path1, path2) -> Long.compare(new File(path2).lastModified(), new File(path1).lastModified()));
        }

        updateTotalImages();
        adapter.notifyDataSetChanged();

        // Hiển thị/ẩn RecyclerView dựa trên danh sách
        if (favoriteImages.isEmpty()) {
            recycler.setVisibility(View.GONE);
            findViewById(R.id.empty_view_favorites).setVisibility(View.VISIBLE); // Thêm TextView này vào layout
        } else {
            recycler.setVisibility(View.VISIBLE);
            findViewById(R.id.empty_view_favorites).setVisibility(View.GONE);
        }
        Log.d(TAG, "Đã cập nhật RecyclerView ảnh yêu thích.");
    }

    public void updateTotalImages() {
        totalImagesTextView.setText("Tổng số: " + favoriteImages.size());
    }
}