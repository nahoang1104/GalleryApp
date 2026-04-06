package com.example.piceditor;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImageHelper {

    private static final String TAG = "ImageHelper";
    private static final String DELETED_FOLDER_NAME = ".deleted_items";
    private static final String PREFS_DELETED_IMAGES = "DeletedImagesPrefs"; // Prefs for deleted mapping
    private static final String KEY_DELETED_MAP = "deletedMap"; // Key for the map within prefs
    private static final String PREFS_FAVORITE_IMAGES = "FavoriteImagesPrefs"; // Prefs for favorites
    private static final String KEY_FAVORITE_SET = "favoriteSet"; // Key for the set within prefs

    // --- Directory Handling ---

    /**
     * Lấy thư mục chứa các ảnh đã xóa (trong bộ nhớ riêng của ứng dụng).
     * Tạo thư mục nếu chưa tồn tại.
     */
    public File getDeletedDir(Context context) {
        File deletedDir = new File(context.getExternalFilesDir(null), DELETED_FOLDER_NAME);
        if (!deletedDir.exists()) {
            if (!deletedDir.mkdirs()) {
                Log.e(TAG, "Không thể tạo thư mục Đã xóa: " + deletedDir.getAbsolutePath());
            }
        }
        return deletedDir;
    }

    // --- Favorite Handling ---

    private SharedPreferences getFavoritePrefs(Context context) {
        return context.getSharedPreferences(PREFS_FAVORITE_IMAGES, Context.MODE_PRIVATE);
    }

    public Set<String> getFavoriteImagePaths(Context context) {
        SharedPreferences prefs = getFavoritePrefs(context);
        // Lấy bản sao để tránh sửa đổi trực tiếp Set trả về từ SharedPreferences
        return new HashSet<>(prefs.getStringSet(KEY_FAVORITE_SET, new HashSet<>()));
    }

    public boolean isFavorite(Context context, String imagePath) {
        return getFavoriteImagePaths(context).contains(imagePath);
    }

    public void addFavorite(Context context, String imagePath) {
        SharedPreferences prefs = getFavoritePrefs(context);
        Set<String> favorites = getFavoriteImagePaths(context); // Lấy bản sao
        if (favorites.add(imagePath)) { // Thêm vào bản sao
            prefs.edit().putStringSet(KEY_FAVORITE_SET, favorites).apply(); // Lưu lại toàn bộ Set
        }
    }

    public void removeFavorite(Context context, String imagePath) {
        SharedPreferences prefs = getFavoritePrefs(context);
        Set<String> favorites = getFavoriteImagePaths(context); // Lấy bản sao
        if (favorites.remove(imagePath)) { // Xóa khỏi bản sao
            prefs.edit().putStringSet(KEY_FAVORITE_SET, favorites).apply(); // Lưu lại toàn bộ Set
        }
    }


    // --- Deleted Image Handling ---

    private SharedPreferences getDeletedPrefs(Context context) {
        return context.getSharedPreferences(PREFS_DELETED_IMAGES, Context.MODE_PRIVATE);
    }

    /**
     * Lưu ánh xạ đường dẫn mới (trong thư mục đã xóa) -> đường dẫn gốc.
     * Sử dụng Map<String, String> lưu dưới dạng JSON string trong SharedPreferences.
     * (Lưu ý: Cách này không hiệu quả lắm nếu số lượng file lớn, DB sẽ tốt hơn).
     */
    private void saveDeletedMapping(Context context, String deletedPath, String originalPath) {
        SharedPreferences prefs = getDeletedPrefs(context);
        // Đơn giản là lưu từng cặp key-value
        prefs.edit().putString(deletedPath, originalPath).apply();
        Log.d(TAG, "Đã lưu ánh xạ: " + deletedPath + " -> " + originalPath);
    }

    /**
     * Lấy đường dẫn gốc từ đường dẫn đã xóa.
     */
    private String getOriginalPath(Context context, String deletedPath) {
        SharedPreferences prefs = getDeletedPrefs(context);
        String originalPath = prefs.getString(deletedPath, null);
        Log.d(TAG, "Lấy ánh xạ cho " + deletedPath + " -> " + originalPath);
        return originalPath;
    }

    /**
     * Xóa ánh xạ khi khôi phục hoặc xóa vĩnh viễn.
     */
    private void removeDeletedMapping(Context context, String deletedPath) {
        SharedPreferences prefs = getDeletedPrefs(context);
        prefs.edit().remove(deletedPath).apply();
        Log.d(TAG, "Đã xóa ánh xạ cho: " + deletedPath);
    }

    /**
     * Lấy danh sách đường dẫn của các ảnh trong thư mục Đã xóa.
     */
    public List<String> getDeletedImagePaths(Context context) {
        List<String> deletedPaths = new ArrayList<>();
        File deletedDir = getDeletedDir(context);
        if (deletedDir.exists() && deletedDir.isDirectory()) {
            File[] files = deletedDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deletedPaths.add(file.getAbsolutePath());
                }
            }
        }
        Log.d(TAG, "Tìm thấy " + deletedPaths.size() + " ảnh trong thư mục đã xóa.");
        return deletedPaths;
    }

    /**
     * Di chuyển file ảnh vào thư mục Đã xóa.
     * Trả về true nếu thành công, false nếu thất bại.
     */
    public boolean moveToDeleted(Context context, String originalPath) {
        File originalFile = new File(originalPath);
        if (!originalFile.exists()) {
            Log.e(TAG, "File gốc không tồn tại: " + originalPath);
            return false;
        }

        File deletedDir = getDeletedDir(context);
        // Tạo tên file mới, có thể thêm timestamp để tránh trùng lặp nếu cần
        String newFileName = originalFile.getName();
        File deletedFile = new File(deletedDir, newFileName);
        int counter = 0;
        while (deletedFile.exists()) { // Xử lý trùng tên file đơn giản
            counter++;
            String nameWithoutExt = newFileName.substring(0, newFileName.lastIndexOf('.'));
            String ext = newFileName.substring(newFileName.lastIndexOf('.'));
            newFileName = nameWithoutExt + "_" + counter + ext;
            deletedFile = new File(deletedDir, newFileName);
        }


        // Cố gắng di chuyển bằng renameTo trước (đơn giản nhất)
        boolean success = originalFile.renameTo(deletedFile);

        if (success) {
            Log.i(TAG, "Di chuyển thành công (renameTo): " + originalPath + " -> " + deletedFile.getAbsolutePath());
            // Xóa khỏi MediaStore nếu có thể (tùy chọn, renameTo thường đủ)
            try {
                ContentResolver resolver = context.getContentResolver();
                Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                String selection = MediaStore.Images.Media.DATA + "=?";
                String[] selectionArgs = new String[]{originalPath};
                int deletedRows = resolver.delete(contentUri, selection, selectionArgs);
                Log.d(TAG, "Đã xóa " + deletedRows + " khỏi MediaStore cho: " + originalPath);
            } catch (Exception e) {
                Log.w(TAG, "Lỗi khi xóa khỏi MediaStore sau khi renameTo: " + e.getMessage());
            }
            saveDeletedMapping(context, deletedFile.getAbsolutePath(), originalPath);
            return true;
        } else {
            Log.w(TAG, "renameTo thất bại cho: " + originalPath + ". Thử sao chép và xóa.");
            // Nếu renameTo thất bại (thường do khác filesystem hoặc quyền), thử sao chép và xóa
            try (InputStream in = new FileInputStream(originalFile);
                 OutputStream out = new FileOutputStream(deletedFile)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                Log.i(TAG, "Sao chép thành công: " + originalPath + " -> " + deletedFile.getAbsolutePath());

                // Sao chép xong, giờ xóa file gốc
                if (originalFile.delete()) {
                    Log.i(TAG, "Xóa file gốc thành công sau khi sao chép: " + originalPath);
                    saveDeletedMapping(context, deletedFile.getAbsolutePath(), originalPath);
                    // Cần thông báo cho MediaStore biết file gốc đã bị xóa
                    try {
                        ContentResolver resolver = context.getContentResolver();
                        Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        String selection = MediaStore.Images.Media.DATA + "=?";
                        String[] selectionArgs = new String[]{originalPath};
                        int deletedRows = resolver.delete(contentUri, selection, selectionArgs);
                        Log.d(TAG, "Đã xóa " + deletedRows + " khỏi MediaStore cho: " + originalPath);
                    } catch (Exception e) {
                        Log.w(TAG, "Lỗi khi xóa khỏi MediaStore sau khi copy/delete: " + e.getMessage());
                    }
                    return true;
                } else {
                    Log.e(TAG, "Xóa file gốc thất bại sau khi sao chép: " + originalPath);
                    // Nếu xóa thất bại, nên xóa file đã sao chép để tránh file rác
                    deletedFile.delete();
                    return false;
                }
            } catch (IOException e) {
                Log.e(TAG, "Lỗi khi sao chép file: " + originalPath, e);
                return false;
            }
        }
    }


    /**
     * Khôi phục file ảnh từ thư mục Đã xóa về vị trí gốc.
     * Trả về true nếu thành công, false nếu thất bại.
     */
    public boolean restoreFromDeleted(Context context, String deletedPath) {
        File deletedFile = new File(deletedPath);
        if (!deletedFile.exists()) {
            Log.e(TAG, "File đã xóa không tồn tại: " + deletedPath);
            return false;
        }

        String originalPath = getOriginalPath(context, deletedPath);
        if (originalPath == null) {
            Log.e(TAG, "Không tìm thấy đường dẫn gốc cho: " + deletedPath);
            return false;
        }

        File originalFile = new File(originalPath);
        File parentDir = originalFile.getParentFile();

        // Đảm bảo thư mục cha tồn tại
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                Log.e(TAG, "Không thể tạo thư mục gốc: " + parentDir.getAbsolutePath());
                return false;
            }
        }

        // Xử lý nếu file gốc đã tồn tại (ví dụ: người dùng tạo lại file cùng tên)
        if (originalFile.exists()) {
            Log.w(TAG, "File gốc đã tồn tại: " + originalPath + ". Sẽ cố gắng ghi đè hoặc đổi tên.");
            // Có thể thêm logic đổi tên thay vì ghi đè nếu cần
        }


        // Cố gắng di chuyển bằng renameTo
        boolean success = deletedFile.renameTo(originalFile);

        if (success) {
            Log.i(TAG, "Khôi phục thành công (renameTo): " + deletedPath + " -> " + originalPath);
            // Thông báo cho MediaStore biết file đã quay lại
            addFileToMediaStore(context, originalFile);
            removeDeletedMapping(context, deletedPath);
            return true;
        } else {
            Log.w(TAG, "renameTo thất bại khi khôi phục: " + deletedPath + ". Thử sao chép và xóa.");
            // Thử sao chép và xóa file trong thư mục deleted
            try (InputStream in = new FileInputStream(deletedFile);
                 OutputStream out = new FileOutputStream(originalFile)) { // Ghi đè nếu file gốc tồn tại
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                Log.i(TAG, "Sao chép khôi phục thành công: " + deletedPath + " -> " + originalPath);

                // Xóa file trong thư mục deleted
                if (deletedFile.delete()) {
                    Log.i(TAG, "Xóa file trong thư mục deleted thành công: " + deletedPath);
                    addFileToMediaStore(context, originalFile); // Thông báo cho MediaStore
                    removeDeletedMapping(context, deletedPath);
                    return true;
                } else {
                    Log.e(TAG, "Xóa file trong thư mục deleted thất bại: " + deletedPath);
                    // File đã được khôi phục nhưng file rác vẫn còn trong .deleted_items
                    // Vẫn trả về true vì file đã khôi phục, nhưng log lỗi lại
                    addFileToMediaStore(context, originalFile);
                    removeDeletedMapping(context, deletedPath);
                    return true; // Coi như thành công vì file đã ở vị trí gốc
                }

            } catch (IOException e) {
                Log.e(TAG, "Lỗi khi sao chép khôi phục file: " + deletedPath, e);
                return false;
            }
        }
    }

    /**
     * Xóa vĩnh viễn file trong thư mục Đã xóa.
     * Trả về true nếu thành công, false nếu thất bại.
     */
    public boolean deletePermanently(Context context, String deletedPath) {
        File deletedFile = new File(deletedPath);
        if (!deletedFile.exists()) {
            Log.e(TAG, "File cần xóa vĩnh viễn không tồn tại: " + deletedPath);
            // Vẫn xóa mapping nếu có
            removeDeletedMapping(context, deletedPath);
            return true; // Coi như thành công vì file không còn
        }

        if (deletedFile.delete()) {
            Log.i(TAG, "Xóa vĩnh viễn thành công: " + deletedPath);
            removeDeletedMapping(context, deletedPath);
            return true;
        } else {
            Log.e(TAG, "Xóa vĩnh viễn thất bại: " + deletedPath);
            return false;
        }
    }

    /**
     * Thêm thông tin file vào MediaStore để hệ thống nhận diện.
     */
    private void addFileToMediaStore(Context context, File file) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg"); // Hoặc loại mime phù hợp
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, file.lastModified()); // Lấy ngày sửa đổi làm ngày chụp tạm
        // Các thông tin khác có thể thêm nếu cần: DISPLAY_NAME, SIZE, etc.

        try {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                Log.i(TAG, "Đã thêm vào MediaStore: " + file.getAbsolutePath() + " với URI: " + uri);
            } else {
                Log.e(TAG, "Không thể thêm vào MediaStore: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi thêm vào MediaStore: " + file.getAbsolutePath(), e);
        }
    }
}