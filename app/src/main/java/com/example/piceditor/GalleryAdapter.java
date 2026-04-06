package com.example.piceditor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast; // Thêm import
import androidx.appcompat.app.AlertDialog; // Thêm import

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<String> images_list;
    private boolean isDeletedAlbum = false; // Cờ xác định có phải album Đã xóa không
    private ImageHelper imageHelper; // Helper để xử lý file

    // Constructor gốc
    public GalleryAdapter(Context context, ArrayList<String> images_list) {
        this.context = context;
        this.images_list = images_list;
        this.isDeletedAlbum = false;
        this.imageHelper = new ImageHelper();
    }

    // Constructor mới cho album đặc biệt (Đã xóa, Yêu thích)
    public GalleryAdapter(Context context, ArrayList<String> images_list, boolean isDeletedAlbum) {
        this.context = context;
        this.images_list = images_list;
        this.isDeletedAlbum = isDeletedAlbum;
        this.imageHelper = new ImageHelper();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        String imagePath = images_list.get(position);
        File image_file = new File(imagePath);

        if (image_file.exists()) {
            Glide.with(context)
                    .load(image_file)
                    .placeholder(R.drawable.baseline_photo_library_24) // Ảnh chờ tải
                    .into(holder.image);
        } else {
        }

        holder.image.setOnClickListener(v -> {
            if (isDeletedAlbum) {
                // Hiển thị dialog lựa chọn cho ảnh trong album Đã xóa
                showDeletedItemOptions(position, imagePath);
            } else {
                // Mở ảnh bình thường
                Intent intent = new Intent(context, ViewPicture.class);
                intent.putExtra("image_file", imagePath);
                // Thêm cờ để biết có phải từ album yêu thích không (nếu cần)
                // intent.putExtra("is_favorite_album", isFavoriteAlbum);
                context.startActivity(intent);
            }
        });
    }

    private void showDeletedItemOptions(int position, String deletedPath) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Tùy chọn")
                .setItems(new CharSequence[]{
                        context.getString(R.string.khoi_phuc),
                        context.getString(R.string.xoa_vinh_vien)
                }, (dialog, which) -> {
                    switch (which) {
                        case 0: // Khôi phục
                            restoreImage(position, deletedPath);
                            break;
                        case 1: // Xóa vĩnh viễn
                            showDeletePermanentlyConfirmation(position, deletedPath);
                            break;
                    }
                });
        builder.create().show();
    }

    private void showDeletePermanentlyConfirmation(int position, String deletedPath) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.xoa_vinh_vien)
                .setMessage(R.string.xac_nhan_xoa_vinh_vien)
                .setPositiveButton(R.string.dong_y, (dialog, which) -> deleteImagePermanently(position, deletedPath))
                .setNegativeButton(R.string.huy, null)
                .show();
    }


    private void restoreImage(int position, String deletedPath) {
        if (imageHelper.restoreFromDeleted(context, deletedPath)) {
            Toast.makeText(context, R.string.da_khoi_phuc_anh, Toast.LENGTH_SHORT).show();
            images_list.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, images_list.size());
            // Cập nhật lại tổng số ảnh nếu Activity có TextView hiển thị
            if (context instanceof DeletedActivity) {
                ((DeletedActivity) context).updateTotalImages();
            }
        } else {
            Toast.makeText(context, R.string.loi_khi_khoi_phuc_anh, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteImagePermanently(int position, String deletedPath) {
        if (imageHelper.deletePermanently(context, deletedPath)) {
            Toast.makeText(context, R.string.da_xoa_anh_vinh_vien, Toast.LENGTH_SHORT).show();
            images_list.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, images_list.size());
            // Cập nhật lại tổng số ảnh nếu Activity có TextView hiển thị
            if (context instanceof DeletedActivity) {
                ((DeletedActivity) context).updateTotalImages();
            }
        } else {
            Toast.makeText(context, R.string.loi_khi_xoa_anh, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public int getItemCount() {
        return images_list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.gallery_image);
        }
    }
}