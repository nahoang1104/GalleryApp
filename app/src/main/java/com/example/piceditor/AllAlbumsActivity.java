package com.example.piceditor;
import java.io.File;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log; // Thêm
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher; // Thêm
import androidx.activity.result.contract.ActivityResultContracts; // Thêm
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator; // Thêm
import java.util.HashSet; // Thêm
import java.util.List; // Thêm
import java.util.Map; // Thêm
import java.util.Set; // Thêm

public class AllAlbumsActivity extends AppCompatActivity {

    private static final String TAG = "AllAlbumsActivity"; // Thêm TAG
    private static final String ALBUM_PREFS_NAME = "AppAlbums"; // Tên SharedPreferences

    private RecyclerView recycler;
    private List<Album> albumList; // Danh sách đối tượng Album
    private AlbumAdapter adapter; // Sử dụng AlbumAdapter
    private GridLayoutManager manager;
    private ImageView moreButton, addAlbumButton;
    private TextView totalAlbumsTextView, allAlbumsTextView, allPhotosTextView; // Đổi tên totalimages

    // ActivityResultLauncher để nhận kết quả từ CreateAlbumActivity
    private final ActivityResultLauncher<Intent> createAlbumLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Nếu CreateAlbumActivity trả về RESULT_OK (đã lưu thành công)
                    Log.d(TAG, "Nhận kết quả OK từ CreateAlbumActivity, tải lại albums.");
                    loadAlbums(); // Tải lại danh sách album
                } else {
                    Log.d(TAG, "Kết quả từ CreateAlbumActivity không phải OK.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_albums);

        recycler = findViewById(R.id.gallery_recycler);
        albumList = new ArrayList<>(); // Khởi tạo danh sách Album
        adapter = new AlbumAdapter(this, albumList); // Khởi tạo AlbumAdapter
        // Sử dụng 2 cột cho hiển thị album sẽ đẹp hơn
        manager = new GridLayoutManager(this, 2);
        totalAlbumsTextView = findViewById(R.id.gallery_total_images); // Đảm bảo ID này đúng trong layout
        moreButton = findViewById(R.id.more);
        addAlbumButton = findViewById(R.id.add_album);
        allPhotosTextView = findViewById(R.id.textView3); // All Photos
        allAlbumsTextView = findViewById(R.id.more_txt); // All Albums

        // Thiết lập RecyclerView
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(manager);

        // Cập nhật giao diện tab
        allPhotosTextView.setAlpha(0.5f);
        allAlbumsTextView.setAlpha(1.0f); // Tab Album đang được chọn

        // Thiết lập sự kiện click
        addAlbumButton.setOnClickListener(v -> showAddAlbumDialog());
        changeToPhotos();
        setMoreButton();

        // Tải danh sách album khi Activity được tạo
        loadAlbums();
    }

    // Load lại albums khi Activity quay lại foreground (ví dụ sau khi xóa ảnh trong AlbumViewActivity)
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume được gọi, tải lại albums.");
        loadAlbums();
    }

    private void changeToPhotos() {
        allPhotosTextView.setOnClickListener(v -> {
            Intent intent = new Intent(AllAlbumsActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Đóng activity này khi chuyển sang MainActivity
        });
    }

    private void setMoreButton() {
        moreButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(AllAlbumsActivity.this, v);
            // Có thể tạo menu riêng cho màn hình album nếu cần
            popupMenu.getMenuInflater().inflate(R.menu.more_menu, popupMenu.getMenu());

            // Ẩn các tùy chọn không phù hợp với màn hình album
            popupMenu.getMenu().findItem(R.id.sort_by_name).setVisible(false);
            popupMenu.getMenu().findItem(R.id.sort_by_date).setVisible(false);
            // Có thể thêm sắp xếp album theo tên, ngày tạo...

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.sync_cloud) {
                    Toast.makeText(AllAlbumsActivity.this, "Đồng bộ ảnh với Cloud", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.create_slideshow) {
                    // Cần logic để chọn ảnh từ các album cho slideshow
                    Toast.makeText(AllAlbumsActivity.this, "Tạo slideshow từ album chưa hỗ trợ", Toast.LENGTH_SHORT).show();
                    // Intent intent = new Intent(AllAlbumsActivity.this, CreateSlideShowActivity.class);
                    // startActivity(intent);
                    return true;
                }
                else if (id == R.id.view_favorites) {
                    Intent favIntent = new Intent(AllAlbumsActivity.this, FavoritesActivity.class);
                    startActivity(favIntent);
                    return true;
                } else if (id == R.id.view_deleted) {
                    Intent delIntent = new Intent(AllAlbumsActivity.this, DeletedActivity.class);
                    startActivity(delIntent);
                    return true;
                }
                else if (id == R.id.theme_dark) {
                    setAppTheme(AppCompatDelegate.MODE_NIGHT_YES);
                    return true;
                } else if (id == R.id.theme_light) {
                    setAppTheme(AppCompatDelegate.MODE_NIGHT_NO);
                    return true;
                }
                // Thêm các action khác cho màn hình album nếu cần
                return false;
            });
            popupMenu.show();
        });
    }

    private void showAddAlbumDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tên Album Mới");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS); // Viết hoa chữ đầu
        input.setHint("Nhập tên album");
        builder.setView(input);

        builder.setPositiveButton("Tạo", (dialog, which) -> {
            String albumName = input.getText().toString().trim();
            if (!albumName.isEmpty()) {
                // Kiểm tra xem tên album đã tồn tại chưa
                SharedPreferences albumPrefs = getSharedPreferences(ALBUM_PREFS_NAME, MODE_PRIVATE);
                if (albumPrefs.contains(albumName)) {
                    Toast.makeText(this, "Tên album '" + albumName + "' đã tồn tại!", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(this, CreateAlbumActivity.class);
                    intent.putExtra("album_name", albumName);
                    // Sử dụng ActivityResultLauncher để khởi chạy và nhận kết quả
                    createAlbumLauncher.launch(intent);
                }
            } else {
                Toast.makeText(this, "Tên album không được để trống!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void loadAlbums() {
        Log.d(TAG, "Bắt đầu tải danh sách albums từ SharedPreferences...");
        albumList.clear(); // Xóa danh sách cũ
        SharedPreferences albumPrefs = getSharedPreferences(ALBUM_PREFS_NAME, MODE_PRIVATE);
        Map<String, ?> allEntries = albumPrefs.getAll(); // Lấy tất cả các cặp key-value

        if (allEntries.isEmpty()) {
            Log.d(TAG, "Không tìm thấy album nào trong SharedPreferences.");
        } else {
            Log.d(TAG, "Tìm thấy " + allEntries.size() + " mục trong SharedPreferences.");
        }


        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String albumName = entry.getKey();
            Object value = entry.getValue();

            // Kiểm tra xem value có phải là Set<String> không
            if (value instanceof Set) {
                try {
                    // Ép kiểu an toàn
                    @SuppressWarnings("unchecked") // Bỏ qua cảnh báo ép kiểu không kiểm tra
                    Set<String> imagePaths = (Set<String>) value;

                    // Lọc bỏ các đường dẫn không còn tồn tại trong Set
                    Set<String> validPaths = new HashSet<>();
                    boolean pathsChanged = false;
                    for (String path : imagePaths) {
                        if (path != null && new File(path).exists()) {
                            validPaths.add(path);
                        } else {
                            Log.w(TAG,"Đường dẫn không hợp lệ hoặc file không tồn tại trong album '" + albumName + "': " + path);
                            pathsChanged = true;
                        }
                    }

                    // Nếu có đường dẫn không hợp lệ, cập nhật lại SharedPreferences
                    if (pathsChanged) {
                        Log.d(TAG, "Cập nhật lại SharedPreferences cho album: " + albumName);
                        albumPrefs.edit().putStringSet(albumName, validPaths).apply(); // Lưu lại set đã lọc
                    }


                    // Chỉ thêm album vào danh sách nếu nó có ảnh hợp lệ
                    if (!validPaths.isEmpty()) {
                        albumList.add(new Album(albumName, validPaths));
                        Log.d(TAG, "Đã thêm album: " + albumName + " với " + validPaths.size() + " ảnh hợp lệ.");
                    } else {
                        Log.w(TAG, "Album '" + albumName + "' không có ảnh hợp lệ nào, sẽ bị bỏ qua (hoặc có thể xóa khỏi Prefs).");
                        // Tùy chọn: Xóa album rỗng khỏi SharedPreferences
                        // albumPrefs.edit().remove(albumName).apply();
                    }


                } catch (ClassCastException e) {
                    // Xử lý trường hợp value không phải là Set<String> (dữ liệu lỗi)
                    Log.e(TAG, "Dữ liệu lỗi trong SharedPreferences cho key: " + albumName, e);
                    // Có thể xóa key bị lỗi này
                    // albumPrefs.edit().remove(albumName).apply();
                }
            } else {
                Log.w(TAG, "Dữ liệu không phải Set được tìm thấy trong SharedPreferences cho key: " + albumName);
                // Xử lý dữ liệu không mong muốn (có thể xóa)
                // albumPrefs.edit().remove(albumName).apply();
            }
        }

        // Sắp xếp album theo tên (hoặc tiêu chí khác nếu muốn)
        Collections.sort(albumList, Comparator.comparing(Album::getName, String.CASE_INSENSITIVE_ORDER));

        // Cập nhật TextView tổng số album
        totalAlbumsTextView.setText("Tổng số: " + albumList.size());

        // Cập nhật adapter
        adapter.updateAlbums(albumList); // Tạo hàm này trong AlbumAdapter
        Log.d(TAG, "Đã cập nhật AlbumAdapter với " + albumList.size() + " albums.");

        // Hiển thị thông báo nếu không có album nào
        if (albumList.isEmpty()) {
            recycler.setVisibility(View.GONE);
            // Hiển thị một TextView thông báo (thêm vào layout nếu chưa có)
            findViewById(R.id.empty_view_albums).setVisibility(View.VISIBLE);
        } else {
            recycler.setVisibility(View.VISIBLE);
            findViewById(R.id.empty_view_albums).setVisibility(View.GONE);
        }
    }

    private void setAppTheme(int theme) {
        AppCompatDelegate.setDefaultNightMode(theme);
        recreate(); // Cần recreate để áp dụng theme cho toàn bộ Activity
    }

    // Hàm này không cần thiết nữa nếu dùng recreate()
    /*
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
    */
}