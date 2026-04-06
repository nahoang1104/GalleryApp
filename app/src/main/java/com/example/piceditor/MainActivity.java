package com.example.piceditor;

import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Environment.MEDIA_MOUNTED;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE; // Thêm

import android.content.BroadcastReceiver;
import android.content.Context; // Thêm
import android.content.Intent;
import android.content.IntentFilter; // Thêm
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log; // Thêm
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File; // Thêm
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator; // Thêm

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity"; // Thêm TAG
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int SORT_BY_DATE = 0;
    private static final int SORT_BY_NAME = 1;
    private int currentSortMode = SORT_BY_DATE;

    private RecyclerView recycler;
    private ArrayList<String> allImages; // Danh sách chứa tất cả ảnh gốc
    private ArrayList<String> displayedImages; // Danh sách ảnh đang hiển thị (sau khi lọc, tìm kiếm)
    private TextView allAlbumsTextView, allPhotosTextView;
    private GalleryAdapter adapter;
    private GridLayoutManager manager;
    private ImageView moreButton, searchIcon; // Thêm searchIcon
    private TextView totalimages;
    private EditText searchText;
    private ImageHelper imageHelper; // Thêm ImageHelper

    private boolean permissionsGranted = false; // Cờ kiểm tra quyền

    // BroadcastReceiver để nhận thông báo khi có ảnh bị xóa từ ViewPicture
    private final BroadcastReceiver imageDeletedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Nhận được broadcast IMAGE_DELETED, tải lại ảnh.");
            loadImages(); // Tải lại danh sách ảnh
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageHelper = new ImageHelper(); // Khởi tạo
        recycler = findViewById(R.id.gallery_recycler);
        allImages = new ArrayList<>();
        displayedImages = new ArrayList<>(); // Khởi tạo
        // Truyền displayedImages cho adapter
        adapter = new GalleryAdapter(this, displayedImages);
        manager = new GridLayoutManager(this, 3);
        totalimages = findViewById(R.id.gallery_total_images);
        moreButton = findViewById(R.id.more);
        searchIcon = findViewById(R.id.search); // Ánh xạ
        searchText = findViewById(R.id.search_text); // Ánh xạ

        recycler.setAdapter(adapter);
        recycler.setLayoutManager(manager);

        allPhotosTextView = findViewById(R.id.textView3);
        allAlbumsTextView = findViewById(R.id.more_txt);
        allAlbumsTextView.setAlpha(0.5f);

        changeToAlbums();
        setMoreButton();
        setupSearch(); // Gọi hàm cài đặt tìm kiếm
        checkAndRequestPermissions(); // Đổi tên hàm

        // Đăng ký BroadcastReceiver
        registerReceiver(imageDeletedReceiver, new IntentFilter("com.example.piceditor.IMAGE_DELETED"), RECEIVER_EXPORTED);

        // Tải ảnh nếu đã có quyền
        if (permissionsGranted) {
            loadImages();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy đăng ký BroadcastReceiver
        unregisterReceiver(imageDeletedReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Khi quay lại từ activity khác (vd: ViewPicture, DeletedActivity), load lại ảnh
        // để cập nhật thay đổi (xóa, khôi phục)
        if (permissionsGranted) {
            loadImages();
        }
    }

    private void changeToAlbums() {
        allAlbumsTextView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AllAlbumsActivity.class);
            startActivity(intent);
        });
    }

    private void setMoreButton() {
        moreButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
            popupMenu.getMenuInflater().inflate(R.menu.more_menu, popupMenu.getMenu());
            MenuItem sortByNameItem = popupMenu.getMenu().findItem(R.id.sort_by_name);
            MenuItem sortByDateItem = popupMenu.getMenu().findItem(R.id.sort_by_date);

            // Cập nhật trạng thái menu sắp xếp
            if (currentSortMode == SORT_BY_NAME) {
                sortByNameItem.setTitle("✓ Đang xếp theo tên");
                sortByDateItem.setTitle("Xếp theo ngày chụp");
            } else {
                sortByNameItem.setTitle("Xếp theo tên");
                sortByDateItem.setTitle("✓ Đang xếp theo ngày");
            }

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if (id == R.id.sync_cloud) {
                    Toast.makeText(MainActivity.this, "Chức năng Đồng bộ Cloud chưa có", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.create_slideshow) {
                    // Cần truyền danh sách ảnh hiện tại (displayedImages)
                    Intent intent = new Intent(MainActivity.this, CreateSlideShowActivity.class);
                    intent.putStringArrayListExtra("image_list", displayedImages);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.sort_by_name) {
                    if (currentSortMode != SORT_BY_NAME) {
                        currentSortMode = SORT_BY_NAME;
                        sortImages(); // Chỉ cần sắp xếp lại danh sách hiện có
                        Toast.makeText(MainActivity.this, "Đã xếp theo tên", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                } else if (id == R.id.sort_by_date) {
                    if (currentSortMode != SORT_BY_DATE) {
                        currentSortMode = SORT_BY_DATE;
                        sortImages(); // Chỉ cần sắp xếp lại danh sách hiện có
                        Toast.makeText(MainActivity.this, "Đã xếp theo ngày chụp", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                } else if (id == R.id.view_favorites) {
                    Intent favIntent = new Intent(MainActivity.this, FavoritesActivity.class);
                    startActivity(favIntent);
                    return true;
                } else if (id == R.id.view_deleted) {
                    Intent delIntent = new Intent(MainActivity.this, DeletedActivity.class);
                    startActivity(delIntent);
                    return true;
                } else if (id == R.id.theme_dark) {
                    setAppTheme(AppCompatDelegate.MODE_NIGHT_YES);
                    return true;
                } else if (id == R.id.theme_light) {
                    setAppTheme(AppCompatDelegate.MODE_NIGHT_NO);
                    return true;
                }
                return false;
            });

            popupMenu.show();
        });
    }

    // --- Permission Handling ---

    private void checkAndRequestPermissions() {
        String readPermission;
        String writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;

        // Chọn quyền đọc phù hợp với phiên bản Android
        if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            readPermission = Manifest.permission.READ_MEDIA_IMAGES;
        } else { // Dưới Android 13
            readPermission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        ArrayList<String> permissionsToRequest = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, readPermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(readPermission);
        }
        // Chỉ yêu cầu WRITE_EXTERNAL_STORAGE cho Android <= 10 (API 29) vì nó cần thiết và hoạt động ở đó
        if (SDK_INT <= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, writePermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(writePermission);
        }
        // Quyền Camera (nếu cần)
        if (ContextCompat.checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(CAMERA);
        }


        if (!permissionsToRequest.isEmpty()) {
            Log.d(TAG, "Yêu cầu các quyền: " + permissionsToRequest);
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "Tất cả quyền cần thiết đã được cấp.");
            permissionsGranted = true;
            // Không gọi loadImages() ở đây nữa, gọi trong onResume hoặc sau khi cấp quyền
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Log.d(TAG, "Tất cả quyền đã được cấp sau khi yêu cầu.");
                permissionsGranted = true;
                loadImages(); // Tải ảnh ngay sau khi cấp quyền thành công
            } else {
                Log.w(TAG, "Một số quyền bị từ chối.");
                Toast.makeText(this, "Một số quyền bị từ chối, chức năng có thể bị hạn chế.", Toast.LENGTH_LONG).show();
                // Có thể hiển thị dialog giải thích hoặc hướng dẫn đến cài đặt
                // Ví dụ: showPermissionDeniedDialog();
            }
        }
    }

    // --- Image Loading and Filtering ---

    private void loadImages() {
        if (!permissionsGranted) {
            Log.w(TAG, "loadImages() bị hủy do chưa cấp quyền.");
            //checkAndRequestPermissions(); // Có thể yêu cầu lại quyền ở đây
            return;
        }

        Log.d(TAG, "Bắt đầu tải ảnh...");
        allImages.clear(); // Xóa danh sách cũ trước khi tải mới

        // Kiểm tra bộ nhớ ngoài
        boolean isSDPresent = Environment.getExternalStorageState().equals(MEDIA_MOUNTED);
        if (!isSDPresent) {
            Log.e(TAG, "Không tìm thấy bộ nhớ ngoài.");
            Toast.makeText(this, "Không tìm thấy bộ nhớ ngoài", Toast.LENGTH_SHORT).show();
            updateDisplayedImages(); // Cập nhật UI với danh sách rỗng
            return;
        }

        final String[] columns = {
                MediaStore.Images.Media.DATA,       // Đường dẫn file
                MediaStore.Images.Media._ID,        // ID
                MediaStore.Images.Media.DISPLAY_NAME, // Tên file hiển thị
                MediaStore.Images.Media.DATE_TAKEN, // Ngày chụp
                MediaStore.Images.Media.DATE_MODIFIED // Ngày sửa đổi (fallback)
        };

        // URI để truy vấn ảnh từ bộ nhớ ngoài
        Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        // Selection để loại trừ ảnh trong thư mục Đã xóa của ứng dụng
        String deletedFolderPath = imageHelper.getDeletedDir(this).getAbsolutePath();
        String selection = MediaStore.Images.Media.DATA + " NOT LIKE ?";
        String[] selectionArgs = new String[]{deletedFolderPath + "%"}; // Loại bỏ tất cả file trong thư mục này


        // Sắp xếp (sẽ sắp xếp lại sau khi tải xong)
        String orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC, " + MediaStore.Images.Media.DATE_MODIFIED + " DESC"; // Mặc định theo ngày mới nhất

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(queryUri, columns, selection, selectionArgs, orderBy);

            if (cursor != null) {
                int dataColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                while (cursor.moveToNext()) {
                    String path = cursor.getString(dataColumnIndex);
                    if (path != null && new File(path).exists()) { // Kiểm tra file tồn tại
                        allImages.add(path);
                    } else {
                        Log.w(TAG, "MediaStore trả về đường dẫn không hợp lệ hoặc file không tồn tại: " + path);
                    }
                }
                Log.d(TAG, "Đã tải " + allImages.size() + " ảnh từ MediaStore (đã lọc thư mục đã xóa).");
            } else {
                Log.e(TAG, "Cursor null khi truy vấn MediaStore.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi truy vấn MediaStore: ", e);
            Toast.makeText(this, "Lỗi khi tải ảnh", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // Sắp xếp và cập nhật UI
        sortImages(); // Sẽ tự động gọi updateDisplayedImages
    }


    // Sắp xếp danh sách allImages và cập nhật displayedImages
    private void sortImages() {
        Log.d(TAG, "Sắp xếp ảnh theo chế độ: " + (currentSortMode == SORT_BY_NAME ? "Tên" : "Ngày"));
        if (currentSortMode == SORT_BY_NAME) {
            Collections.sort(allImages, Comparator.comparing(s -> new File(s).getName().toLowerCase()));
        } else { // SORT_BY_DATE
            Collections.sort(allImages, (path1, path2) -> {
                File file1 = new File(path1);
                File file2 = new File(path2);
                // So sánh theo ngày sửa đổi (lastModified) vì DATE_TAKEN có thể null
                return Long.compare(file2.lastModified(), file1.lastModified()); // Mới nhất trước
            });
        }
        // Sau khi sắp xếp, áp dụng bộ lọc tìm kiếm (nếu có) và cập nhật RecyclerView
        filterImages(searchText.getText().toString());
    }


    // Cài đặt chức năng tìm kiếm
    private void setupSearch() {
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterImages(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Có thể ẩn/hiện EditText khi nhấn icon search (tùy chọn)
        searchIcon.setOnClickListener(v -> {
            if (searchText.getVisibility() == View.VISIBLE) {
                searchText.setVisibility(View.GONE);
            } else {
                searchText.setVisibility(View.VISIBLE);
                searchText.requestFocus();
            }
        });
    }

    // Lọc danh sách ảnh dựa trên từ khóa tìm kiếm
    private void filterImages(String query) {
        Log.d(TAG, "Lọc ảnh với query: '" + query + "'");
        String lowerCaseQuery = query.toLowerCase().trim();
        displayedImages.clear(); // Xóa danh sách hiển thị hiện tại

        if (lowerCaseQuery.isEmpty()) {
            displayedImages.addAll(allImages); // Nếu không có query, hiển thị tất cả
            Log.d(TAG, "Query rỗng, hiển thị tất cả " + allImages.size() + " ảnh.");
        } else {
            for (String imagePath : allImages) {
                // Lọc theo tên file
                if (new File(imagePath).getName().toLowerCase().contains(lowerCaseQuery)) {
                    displayedImages.add(imagePath);
                }
                // Có thể thêm lọc theo mô tả (nếu đã lưu mô tả)
                // SharedPreferences descPrefs = getSharedPreferences("ImageDescriptions", MODE_PRIVATE);
                // String descKey = "desc_" + imagePath.hashCode();
                // String description = descPrefs.getString(descKey, "");
                // if (!description.isEmpty() && description.toLowerCase().contains(lowerCaseQuery)) {
                //    if (!displayedImages.contains(imagePath)) { // Tránh thêm trùng lặp
                //        displayedImages.add(imagePath);
                //    }
                // }
            }
            Log.d(TAG, "Sau khi lọc, có " + displayedImages.size() + " ảnh khớp.");
        }
        updateDisplayedImages(); // Cập nhật RecyclerView
    }

    // Cập nhật RecyclerView và TextView tổng số ảnh
    private void updateDisplayedImages() {
        totalimages.setText("Tổng số: " + displayedImages.size());
        adapter.notifyDataSetChanged(); // Thông báo cho adapter biết dữ liệu đã thay đổi
        Log.d(TAG, "Adapter đã được thông báo cập nhật.");

        if (displayedImages.isEmpty()) {
            // Hiển thị thông báo nếu không có ảnh nào (tùy chọn)
            // findViewById(R.id.empty_view).setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
            Log.d(TAG,"Danh sách hiển thị rỗng.");
        } else {
            // findViewById(R.id.empty_view).setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
        }
    }

    // --- Theme Handling ---

    private void setAppTheme(int theme) {
        AppCompatDelegate.setDefaultNightMode(theme);
        // Không cần recreate() vì theme thường được áp dụng khi activity khởi động lại
    }

    private int getCurrentThemePreference() {
        switch (AppCompatDelegate.getDefaultNightMode()) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                return 1;
            case AppCompatDelegate.MODE_NIGHT_YES:
                return 2;
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
            default:
                return 0;
        }
    }
}