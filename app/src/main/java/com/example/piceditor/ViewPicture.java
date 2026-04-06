package com.example.piceditor;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull; // Cần giữ lại nếu dùng @NonNull ở đâu đó, nhưng không bắt buộc cho logic này
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Có thể cần nếu dùng checkSelfPermission ở đâu đó khác
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;

// import android.Manifest; // Không cần thiết cho logic đơn giản này nữa
import android.content.Context;
// import android.content.DialogInterface; // Không cần thiết trực tiếp
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager; // Có thể cần nếu check quyền ở chỗ khác
import android.app.WallpaperManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.jsibbold.zoomage.ZoomageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
// import java.util.Set; // Chỉ giữ lại nếu ImageHelper cần

public class ViewPicture extends AppCompatActivity {

    private static final String TAG = "ViewPicture";
    // Constants for preferences
    private static final String PREFS_NAME = "ImageDescriptions";
    private static final String DESCRIPTION_PREFIX = "desc_";

    private ZoomageView image;
    private String image_file_path;
    private ImageView backButton, likeIcon, deleteIcon, moreIcon;
    private LinearLayout shareLayout, likeLayout, editLayout, deleteLayout, moreLayout;
    private TextView likeText;
    private String imageDescription = "";
    private ImageHelper imageHelper;
    private boolean isFavorite = false;

    // --- ActivityResultLaunchers ---
    // Launcher để xử lý kết quả trả về từ màn hình cài đặt MANAGE_EXTERNAL_STORAGE (Android 11+)
    private final ActivityResultLauncher<Intent> manageStorageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // Không cần kiểm tra result code, chỉ cần kiểm tra lại quyền
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        Log.d(TAG, "MANAGE_EXTERNAL_STORAGE đã được cấp sau khi quay lại từ cài đặt.");
                        // Thử thực hiện lại hành động xóa sau khi cấp quyền thành công
                        Toast.makeText(this, R.string.quyen_da_cap_thu_lai, Toast.LENGTH_SHORT).show(); // Thông báo cho user thử lại
                        // Hoặc gọi lại performDeleteAction() nếu muốn tự động thử lại:
                        // performDeleteAction();
                    } else {
                        Log.w(TAG, "MANAGE_EXTERNAL_STORAGE vẫn chưa được cấp sau khi quay lại từ cài đặt.");
                        Toast.makeText(this, R.string.quyen_quan_ly_file_bi_tu_choi, Toast.LENGTH_LONG).show();
                    }
                }
            });

    // Launcher để xử lý kết quả trả về từ màn hình cài đặt chung của ứng dụng (Android 6+)
    // Dùng cho việc kiểm tra quyền WRITE_EXTERNAL_STORAGE hoặc các quyền khác nếu cần
    private final ActivityResultLauncher<Intent> appSettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Log.d(TAG, "Quay lại từ màn hình cài đặt ứng dụng.");
                // Có thể kiểm tra lại quyền WRITE_EXTERNAL_STORAGE ở đây nếu muốn
                // và thông báo người dùng thử lại hoặc tự động thử lại
                Toast.makeText(this, R.string.kiem_tra_quyen_thu_lai, Toast.LENGTH_SHORT).show();
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_picture);

        imageHelper = new ImageHelper();

        image_file_path = getIntent().getStringExtra("image_file");
        if (image_file_path == null || image_file_path.isEmpty()) {
            Toast.makeText(this, R.string.khong_the_lay_duong_dan_anh, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        File file = new File(image_file_path);
        if (!file.exists()) {
            Toast.makeText(this, R.string.khong_tim_thay_anh, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Khởi tạo các View
        image = findViewById(R.id.image);
        backButton = findViewById(R.id.back_icon);
        shareLayout = findViewById(R.id.share_layout);
        likeLayout = findViewById(R.id.like_layout);
        editLayout = findViewById(R.id.edit_layout);
        deleteLayout = findViewById(R.id.delete_layout);
        moreLayout = findViewById(R.id.more_layout);
        likeIcon = findViewById(R.id.like_icon);
        likeText = findViewById(R.id.like_txt);
        deleteIcon = findViewById(R.id.delete_icon);
        moreIcon = findViewById(R.id.more_icon);

        loadDescription();
        Glide.with(this).load(image_file_path).into(image);
        setupButtonClickListeners();
        updateLikeIconState();
    }

    private void setupButtonClickListeners() {
        backButton.setOnClickListener(v -> finish());
        shareLayout.setOnClickListener(v -> setupShareButton());
        likeLayout.setOnClickListener(v -> toggleFavorite());
        editLayout.setOnClickListener(v -> {
            Toast.makeText(this, R.string.chuc_nang_chua_co, Toast.LENGTH_SHORT).show();
        });
        // Khi nhấn nút xóa, hiển thị dialog xác nhận trước
        deleteLayout.setOnClickListener(v -> confirmDelete());
        moreLayout.setOnClickListener(v -> showMoreMenu(moreIcon));
    }

    private void loadDescription() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String key = DESCRIPTION_PREFIX + image_file_path.hashCode();
        imageDescription = prefs.getString(key, "");
    }

    private void saveDescription() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String key = DESCRIPTION_PREFIX + image_file_path.hashCode();
        editor.putString(key, imageDescription);
        editor.apply();
    }

    private void updateLikeIconState() {
        isFavorite = imageHelper.isFavorite(this, image_file_path);

        // Giả sử bạn luôn dùng icon đường viền làm cơ sở
        likeIcon.setImageResource(R.drawable.ic_favorite); // Hoặc một icon cơ sở khác

        if (isFavorite) {
            // Đặt màu tint là màu đỏ khi đã thích
            likeIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.your_red_color))); // Định nghĩa your_red_color trong colors.xml
            likeText.setText(R.string.b_th_ch);
        } else {
            // Xóa màu tint hoặc đặt về màu mặc định khi chưa thích
            likeIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.black))); // Định nghĩa your_default_icon_color (vd: màu xám) trong colors.xml
            // Hoặc xóa hẳn tint để dùng màu gốc của drawable/theme:
            // likeIcon.setImageTintList(null);
            likeText.setText(R.string.th_ch);
        }
    }

    // Hàm toggleFavorite giữ nguyên như của bạn
    private void toggleFavorite() {
        if (isFavorite) {
            imageHelper.removeFavorite(this, image_file_path);
            Toast.makeText(this, R.string.da_xoa_khoi_yeu_thich, Toast.LENGTH_SHORT).show();
        } else {
            imageHelper.addFavorite(this, image_file_path);
            Toast.makeText(this, R.string.da_them_vao_yeu_thich, Toast.LENGTH_SHORT).show();
        }
        updateLikeIconState(); // Gọi hàm cập nhật UI sau khi thay đổi trạng thái
    }
    private void showMoreMenu(View anchorView) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.detail_image_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.add_description) {
                showAddDescriptionDialog();
                return true;
            } else if (id == R.id.add_to_album) {
                Toast.makeText(this, R.string.chuc_nang_chua_co, Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.secure_image) {
                Toast.makeText(this, R.string.chuc_nang_chua_co, Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.main_screen) {
                setAsWallpaper(WallpaperManager.FLAG_SYSTEM);
                return true;
            } else if (id == R.id.lock_screen) {
                setAsWallpaper(WallpaperManager.FLAG_LOCK);
                return true;
            } else if (id == R.id.details) {
                showImageDetails();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void showImageDetails() {
        File file = new File(image_file_path);
        if (!file.exists()) {
            Toast.makeText(this, R.string.khong_tim_thay_anh, Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.chi_ti_t_nh);

        View view = getLayoutInflater().inflate(R.layout.dialog_image_details, null);
        builder.setView(view);

        TextView tvFileName = view.findViewById(R.id.tvFileName);
        TextView tvFilePath = view.findViewById(R.id.tvFilePath);
        TextView tvFileSize = view.findViewById(R.id.tvFileSize);
        TextView tvDateTaken = view.findViewById(R.id.tvDateTaken);
        TextView tvDescription = view.findViewById(R.id.tvDescription);

        tvFileName.setText(getString(R.string.detail_ten) + file.getName());
        tvFilePath.setText(getString(R.string.detail_duong_dan) + file.getAbsolutePath());
        String sizeString = android.text.format.Formatter.formatFileSize(this, file.length());
        tvFileSize.setText(getString(R.string.detail_kich_thuoc) + sizeString);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        tvDateTaken.setText(getString(R.string.detail_ngay_tao) + sdf.format(new Date(file.lastModified())));
        tvDescription.setText(getString(R.string.detail_mo_ta) + (imageDescription.isEmpty() ? getString(R.string.chua_co_mo_ta) : imageDescription));

        builder.setPositiveButton(R.string.ok, null);
        builder.show();
    }

    private void showAddDescriptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.th_m_m_t);

        View view = getLayoutInflater().inflate(R.layout.dialog_add_description, null);
        builder.setView(view);

        EditText etDescription = view.findViewById(R.id.etDescription);
        etDescription.setText(imageDescription);

        builder.setPositiveButton(R.string.luu, (dialog, which) -> {
            imageDescription = etDescription.getText().toString().trim();
            saveDescription();
            Toast.makeText(ViewPicture.this, R.string.da_luu_mo_ta, Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton(R.string.huy, null);
        builder.show();
    }

    private void setAsWallpaper(int which) {
        if (image_file_path == null || image_file_path.isEmpty()) {
            Toast.makeText(this, R.string.khong_the_lay_duong_dan_anh, Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap bitmap = null;
        try {
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitmap = BitmapFactory.decodeFile(image_file_path, options);

            if (bitmap == null) {
                Toast.makeText(this, R.string.khong_the_doc_file_anh, Toast.LENGTH_SHORT).show();
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setBitmap(bitmap, null, true, which);
                String message = (which == WallpaperManager.FLAG_SYSTEM) ?
                        getString(R.string.da_dat_hinh_nen_chinh) : getString(R.string.da_dat_hinh_nen_khoa);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            } else {
                wallpaperManager.setBitmap(bitmap);
                Toast.makeText(this, R.string.da_dat_hinh_nen, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.loi_khi_dat_hinh_nen) + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "IOException setting wallpaper", e);
        } catch (OutOfMemoryError e) {
            Toast.makeText(this, R.string.anh_qua_lon_lam_hinh_nen, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "OutOfMemoryError setting wallpaper", e);
        }
        // Không nên recycle bitmap ở đây vì WallpaperManager có thể vẫn giữ tham chiếu
    }

    private void setupShareButton() {
        if (image_file_path == null || image_file_path.isEmpty()) {
            Toast.makeText(this, R.string.khong_the_lay_duong_dan_anh, Toast.LENGTH_SHORT).show();
            return;
        }
        File imageFile = new File(image_file_path);
        if (!imageFile.exists()) {
            Toast.makeText(this, R.string.khong_tim_thay_anh, Toast.LENGTH_SHORT).show();
            return;
        }
        Uri imageUri;
        try {
            imageUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".provider",
                    imageFile
            );
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "FileProvider URI generation error: " + e.getMessage());
            Toast.makeText(this, R.string.loi_tao_uri_chia_se, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.chia_se_anh_qua)));
    }


    // --- Delete Logic (SIMPLIFIED) ---

    /**
     * Hiển thị hộp thoại xác nhận trước khi xóa.
     */
    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.x_a_nh)
                .setMessage(R.string.xac_nhan_xoa_anh)
                .setPositiveButton(R.string.dong_y, (dialog, which) -> performDeleteAction()) // Nếu đồng ý, thực hiện xóa
                .setNegativeButton(R.string.huy, null) // Nếu hủy, không làm gì cả
                .show();
    }

    /**
     * Thực hiện hành động xóa/di chuyển ảnh.
     * Nếu thất bại, gọi handleDeleteFailure().
     */
    private void performDeleteAction() {
        Log.i(TAG, "Attempting to delete/move file: " + image_file_path);
        if (image_file_path == null || image_file_path.isEmpty()) {
            Toast.makeText(this, R.string.khong_the_lay_duong_dan_anh, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = imageHelper.moveToDeleted(this, image_file_path);

        if (success) {
            Log.i(TAG, "File moved successfully to deleted folder.");
            Toast.makeText(this, R.string.da_chuyen_vao_da_xoa, Toast.LENGTH_SHORT).show();
            // Thông báo cho các thành phần khác nếu cần
            Intent intent = new Intent("com.example.piceditor.IMAGE_DELETED");
            intent.putExtra("deleted_path", image_file_path);
            sendBroadcast(intent);
            finish(); // Đóng activity sau khi xóa thành công
        } else {
            Log.w(TAG, "Deletion FAILED for file: " + image_file_path);
            // Xóa thất bại -> Gọi hàm xử lý lỗi (hiển thị dialog yêu cầu kiểm tra quyền)
            handleDeleteFailure();
        }
    }

    /**
     * Xử lý trường hợp xóa thất bại. Hiển thị dialog đề nghị kiểm tra quyền.
     */
    private void handleDeleteFailure() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.xoa_that_bai_title) // "Xóa thất bại"
                .setMessage(R.string.xoa_that_bai_kiem_tra_quyen_message) // "Không thể xóa ảnh. Vui lòng kiểm tra quyền của ứng dụng trong Cài đặt và thử lại."
                .setPositiveButton(R.string.di_den_cai_dat, (dialog, which) -> {
                    // Mở cài đặt phù hợp dựa trên phiên bản Android
                    openRelevantSettings();
                })
                .setNegativeButton(R.string.huy, (dialog, which) -> {
                    // Người dùng chọn không đi đến cài đặt
                    Toast.makeText(this, R.string.xoa_da_huy, Toast.LENGTH_SHORT).show(); // "Đã hủy thao tác xóa."
                })
                .setCancelable(false) // Không cho hủy bằng cách chạm ra ngoài
                .show();
    }

    /**
     * Mở màn hình cài đặt phù hợp để người dùng kiểm tra quyền.
     * - Android 11+: Mở cài đặt MANAGE_EXTERNAL_STORAGE.
     * - Android 6-10: Mở cài đặt chi tiết của ứng dụng (nơi có quyền WRITE_EXTERNAL_STORAGE).
     * - Dưới Android 6: Cũng mở cài đặt chi tiết của ứng dụng.
     */
    private void openRelevantSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Ưu tiên mở cài đặt MANAGE_EXTERNAL_STORAGE cho Android 11+
            openManageStorageSettings();
        } else {
            // Mở cài đặt chung của ứng dụng cho các phiên bản cũ hơn
            openAppSettings();
        }
    }


    /**
     * Mở màn hình cài đặt MANAGE_EXTERNAL_STORAGE cho ứng dụng (Android 11+).
     * Sử dụng manageStorageLauncher để xử lý kết quả trả về.
     */
    private void openManageStorageSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                manageStorageLauncher.launch(intent); // Dùng launcher này
            } catch (Exception e) {
                Log.e(TAG, "Could not open MANAGE_APP_ALL_FILES_ACCESS_PERMISSION settings", e);
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    manageStorageLauncher.launch(intent); // Thử fallback
                } catch (Exception e2) {
                    Log.e(TAG, "Could not open ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION settings", e2);
                    Toast.makeText(this, R.string.khong_the_mo_cai_dat_quyen, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Mở màn hình cài đặt chi tiết của ứng dụng (Android 6+ hoặc thấp hơn).
     * Sử dụng appSettingsLauncher để xử lý kết quả trả về.
     */
    private void openAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
            // Có thể dùng appSettingsLauncher nếu bạn muốn xử lý khi người dùng quay lại
            // appSettingsLauncher.launch(intent);
            // Hoặc startActivity đơn giản nếu không cần xử lý kết quả ngay lập tức
            startActivity(intent);
            Toast.makeText(this, R.string.vui_long_kiem_tra_quyen_files, Toast.LENGTH_LONG).show(); // Nhắc người dùng

        } catch (Exception e) {
            Log.e(TAG, "Could not open ACTION_APPLICATION_DETAILS_SETTINGS", e);
            Toast.makeText(this, R.string.khong_the_mo_cai_dat_ung_dung, Toast.LENGTH_LONG).show(); // "Không thể mở cài đặt ứng dụng."

        }
    }

} // Kết thúc class ViewPicture