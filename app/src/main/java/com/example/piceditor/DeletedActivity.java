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
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DeletedActivity extends AppCompatActivity {

    private static final String TAG = "DeletedActivity";
    private RecyclerView recycler;
    private ArrayList<String> deletedImages;
    private GalleryAdapter adapter;
    private TextView totalImagesTextView;
    private ImageHelper imageHelper;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deleted); // Tạo layout này

        imageHelper = new ImageHelper();
        toolbar = findViewById(R.id.toolbar_deleted);
        recycler = findViewById(R.id.deleted_recycler); // Đổi ID
        totalImagesTextView = findViewById(R.id.deleted_total_images); // Đổi ID
        deletedImages = new ArrayList<>();
        // Sử dụng constructor mới của GalleryAdapter, truyền true cho isDeletedAlbum
        adapter = new GalleryAdapter(this, deletedImages, true);
        GridLayoutManager manager = new GridLayoutManager(this, 3);

        // --- Cài đặt Toolbar ---
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.anh_da_xoa); // Đặt tiêu đề
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Hiển thị nút back
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        // ---------------------

        recycler.setAdapter(adapter);
        recycler.setLayoutManager(manager);

        loadDeletedImages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load lại khi quay lại để cập nhật nếu có ảnh bị xóa hoặc khôi phục từ activity khác
        loadDeletedImages();
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


    private void loadDeletedImages() {
        Log.d(TAG, "Đang tải ảnh đã xóa...");
        deletedImages.clear();
        List<String> deletedPaths = imageHelper.getDeletedImagePaths(this);

        if (deletedPaths.isEmpty()) {
            Log.d(TAG, "Không có ảnh nào trong thư mục đã xóa.");
            Toast.makeText(this, R.string.khong_co_anh_nao, Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "Tìm thấy " + deletedPaths.size() + " ảnh đã xóa.");
            deletedImages.addAll(deletedPaths);
            // Sắp xếp theo ngày sửa đổi (ngày file được tạo trong thư mục deleted)
            Collections.sort(deletedImages, (path1, path2) -> Long.compare(new File(path2).lastModified(), new File(path1).lastModified()));
        }

        updateTotalImages();
        adapter.notifyDataSetChanged();

        // Hiển thị/ẩn RecyclerView
        if (deletedImages.isEmpty()) {
            recycler.setVisibility(View.GONE);
            findViewById(R.id.empty_view_deleted).setVisibility(View.VISIBLE); // Thêm TextView này
        } else {
            recycler.setVisibility(View.VISIBLE);
            findViewById(R.id.empty_view_deleted).setVisibility(View.GONE);
        }
        Log.d(TAG, "Đã cập nhật RecyclerView ảnh đã xóa.");
    }

    // Hàm này được gọi từ Adapter sau khi khôi phục/xóa thành công
    public void updateTotalImages() {
        totalImagesTextView.setText("Tổng số: " + deletedImages.size());
        // Cập nhật lại hiển thị empty view
        if (deletedImages.isEmpty()) {
            recycler.setVisibility(View.GONE);
            findViewById(R.id.empty_view_deleted).setVisibility(View.VISIBLE);
        } else {
            recycler.setVisibility(View.VISIBLE);
            findViewById(R.id.empty_view_deleted).setVisibility(View.GONE);
        }
    }
}