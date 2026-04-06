package com.example.piceditor;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> {

    private final Context context;
    private final List<Album> albumList;

    public AlbumAdapter(Context context, List<Album> albumList) {
        this.context = context;
        this.albumList = albumList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.album_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Album currentAlbum = albumList.get(position);

        holder.albumName.setText(currentAlbum.getName());
        holder.imageCount.setText(String.format("%d ảnh", currentAlbum.getImageCount()));

        // Load ảnh bìa
        String coverPath = currentAlbum.getCoverImagePath();
        if (coverPath != null && new File(coverPath).exists()) {
            Glide.with(context)
                    .load(coverPath)
                    .placeholder(R.drawable.baseline_photo_library_24) // Ảnh placeholder
                   .centerCrop()
                    .into(holder.coverImage);
        } else {
            // Nếu không có ảnh bìa hoặc ảnh không tồn tại, hiển thị ảnh mặc định
            Glide.with(context)
                    .load(R.drawable.baseline_photo_library_24) // Ảnh mặc định
                    .centerCrop()
                    .into(holder.coverImage);
        }

        // Xử lý sự kiện click vào một album
        holder.itemView.setOnClickListener(v -> {
            // Mở Activity mới để xem chi tiết ảnh trong album
            // Truyền tên album hoặc danh sách đường dẫn ảnh qua Intent
            Intent intent = new Intent(context, AlbumViewActivity.class); // Tạo Activity này ở bước sau
            intent.putExtra("album_name", currentAlbum.getName());
            // Chuyển Set thành ArrayList để có thể gửi qua Intent
            intent.putStringArrayListExtra("image_paths", new ArrayList<>(currentAlbum.getImagePaths()));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    // Cập nhật danh sách album
    public void updateAlbums(List<Album> newAlbums) {
        albumList.clear();
        albumList.addAll(newAlbums);
        notifyDataSetChanged();
    }


    // ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImage;
        TextView albumName;
        TextView imageCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImage = itemView.findViewById(R.id.album_cover_image);
            albumName = itemView.findViewById(R.id.album_name);
            imageCount = itemView.findViewById(R.id.album_image_count);
        }
    }
}