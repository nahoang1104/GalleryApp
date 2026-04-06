package com.example.piceditor;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Environment.MEDIA_MOUNTED;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

public class CreateAlbumActivity extends AppCompatActivity {

    private static final String TAG = "CreateAlbumActivity";
    private static final int PERMISSION_REQUEST_READ_STORAGE = 101;
    private static final String ALBUM_PREFS_NAME = "AppAlbums"; // Tên SharedPreferences để lưu albums

    private ImageView closeButton, applyButton;
    private RecyclerView recyclerView;
    private TextView albumTitleTextView;
    private ArrayList<String> allImages;
    private SelectImageAdapter adapter; // Sử dụng adapter mới
    private String albumName;
    private ImageHelper imageHelper; // Dùng để lấy thư mục đã xóa (nếu cần lọc)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_album);

        imageHelper = new ImageHelper(); // Khởi tạo
        albumName = getIntent().getStringExtra("album_name"); // Lấy tên album từ Intent

        closeButton = findViewById(R.id.cancel_icon);
        applyButton = findViewById(R.id.apply_icon);
        albumTitleTextView = findViewById(R.id.album_title);
        recyclerView = findViewById(R.id.gallery_recycler);

        allImages = new ArrayList<>();
        adapter = new SelectImageAdapter(this, allImages); // Khởi tạo adapter
        GridLayoutManager manager = new GridLayoutManager(this, 3); // Hoặc 4 cột tùy ý
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

        // Hiển thị tên album
        if (albumName != null && !albumName.trim().isEmpty()) {
            albumTitleTextView.setText(albumName);
        } else {
            // Xử lý trường hợp tên album không hợp lệ (có thể đóng activity hoặc yêu cầu nhập lại)
            Toast.makeText(this, "Tên album không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return; // Thoát khỏi onCreate nếu tên không hợp lệ
        }

        // Thiết lập sự kiện click
        closeButton.setOnClickListener(v -> finish()); // Đóng activity
        applyButton.setOnClickListener(v -> saveAlbum()); // Lưu album

        // Kiểm tra quyền và tải ảnh
        checkAndRequestPermission();
    }

    private void checkAndRequestPermission() {
        String readPermission;
        if (SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) { // Android 13+
            readPermission = READ_MEDIA_IMAGES;
        } else { // Dưới Android 13
            readPermission = READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, readPermission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{readPermission}, PERMISSION_REQUEST_READ_STORAGE);
        } else {
            // Quyền đã được cấp, tải ảnh
            loadImages();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền được cấp, tải ảnh
                loadImages();
            } else {
                // Quyền bị từ chối
                Toast.makeText(this, "Cần quyền đọc bộ nhớ để chọn ảnh", Toast.LENGTH_LONG).show();
                finish(); // Đóng activity nếu không có quyền
            }
        }
    }

    private void loadImages() {
        Log.d(TAG, "Bắt đầu tải ảnh để chọn...");
        allImages.clear(); // Xóa danh sách cũ

        boolean isSDPresent = Environment.getExternalStorageState().equals(MEDIA_MOUNTED);
        if (!isSDPresent) {
            Log.e(TAG, "Không tìm thấy bộ nhớ ngoài.");
            return;
        }

        final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_MODIFIED};
        Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        // Lọc bỏ ảnh trong thư mục Đã xóa
        String deletedFolderPath = imageHelper.getDeletedDir(this).getAbsolutePath();
        String selection = MediaStore.Images.Media.DATA + " NOT LIKE ?";
        String[] selectionArgs = new String[]{deletedFolderPath + "%"};

        String orderBy = MediaStore.Images.Media.DATE_MODIFIED + " DESC"; // Sắp xếp theo ngày mới nhất

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(queryUri, columns, selection, selectionArgs, orderBy);
            if (cursor != null) {
                int dataColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                while (cursor.moveToNext()) {
                    String path = cursor.getString(dataColumnIndex);
                    if (path != null && new File(path).exists()) {
                        allImages.add(path);
                    }
                }
                Log.d(TAG, "Đã tải " + allImages.size() + " ảnh.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi tải ảnh: ", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Thông báo cho adapter biết dữ liệu đã thay đổi
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Adapter đã được thông báo cập nhật.");
    }

    private void saveAlbum() {
        Set<String> selectedImagePaths = adapter.getSelectedPaths(); // Lấy danh sách ảnh đã chọn

        if (selectedImagePaths.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lưu vào SharedPreferences
        SharedPreferences albumPrefs = getSharedPreferences(ALBUM_PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = albumPrefs.edit();

        // Lưu Set<String> chứa đường dẫn ảnh với key là tên album
        editor.putStringSet(albumName, selectedImagePaths);
        boolean success = editor.commit(); // Sử dụng commit() để biết kết quả ngay lập tức (hoặc apply() chạy nền)

        if (success) {
            Toast.makeText(this, "Đã lưu album '" + albumName + "' (" + selectedImagePaths.size() + " ảnh)", Toast.LENGTH_LONG).show();
            // Có thể gửi Intent hoặc dùng cách khác để báo AllAlbumsActivity cập nhật nếu cần
            setResult(RESULT_OK); // Đặt kết quả thành công để Activity trước đó có thể biết
            finish(); // Đóng activity sau khi lưu thành công
        } else {
            Toast.makeText(this, "Lỗi khi lưu album", Toast.LENGTH_SHORT).show();
        }
    }
}