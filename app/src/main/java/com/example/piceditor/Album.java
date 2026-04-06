package com.example.piceditor;

import java.util.Set;

public class Album {
    private String name;
    private String coverImagePath; // Đường dẫn ảnh bìa (ảnh đầu tiên)
    private int imageCount;
    private Set<String> imagePaths; // Giữ lại Set đường dẫn gốc nếu cần

    public Album(String name, Set<String> imagePaths) {
        this.name = name;
        this.imagePaths = imagePaths;
        this.imageCount = imagePaths.size();
        // Lấy ảnh đầu tiên làm ảnh bìa (nếu có)
        if (!imagePaths.isEmpty()) {
            this.coverImagePath = imagePaths.iterator().next(); // Lấy phần tử đầu tiên
        } else {
            this.coverImagePath = null; // Hoặc một đường dẫn ảnh mặc định
        }
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getCoverImagePath() {
        return coverImagePath;
    }

    public int getImageCount() {
        return imageCount;
    }

    public Set<String> getImagePaths() {
        return imagePaths;
    }
}