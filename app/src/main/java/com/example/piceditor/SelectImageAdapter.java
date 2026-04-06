package com.example.piceditor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectImageAdapter extends RecyclerView.Adapter<SelectImageAdapter.ViewHolder> {

    private final Context context;
    private final List<String> imagePaths;
    private final Set<String> selectedPaths; // Lưu trữ đường dẫn ảnh đã chọn

    public SelectImageAdapter(Context context, List<String> imagePaths) {
        this.context = context;
        this.imagePaths = imagePaths;
        this.selectedPaths = new HashSet<>(); // Khởi tạo Set rỗng
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.gallery_item_selectable, parent, false); // Sử dụng layout mới
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String path = imagePaths.get(position);
        File imageFile = new File(path);

        if (imageFile.exists()) {
            Glide.with(context)
                    .load(imageFile)
                    .placeholder(R.drawable.baseline_photo_library_24)
                    .centerCrop()
                    .into(holder.image);
        } else {
            Glide.with(context);
        }

        // Cập nhật giao diện dựa trên trạng thái chọn
        if (selectedPaths.contains(path)) {
            holder.selectionOverlay.setVisibility(View.VISIBLE); // Hiển thị overlay hoặc dấu tick
            holder.image.setAlpha(0.6f); // Làm mờ nhẹ ảnh đã chọn
        } else {
            holder.selectionOverlay.setVisibility(View.GONE); // Ẩn overlay
            holder.image.setAlpha(1.0f); // Ảnh bình thường
        }

        // Xử lý sự kiện click để chọn/bỏ chọn
        holder.itemView.setOnClickListener(v -> {
            if (selectedPaths.contains(path)) {
                selectedPaths.remove(path); // Bỏ chọn
            } else {
                selectedPaths.add(path); // Chọn
            }
            // Cập nhật lại item view này để thay đổi giao diện
            notifyItemChanged(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return imagePaths.size();
    }

    // Phương thức để lấy danh sách các ảnh đã chọn
    public Set<String> getSelectedPaths() {
        return selectedPaths;
    }

    // ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        View selectionOverlay; // View để hiển thị trạng thái đã chọn (ví dụ: dấu tick, viền)

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.gallery_image_selectable); // Đổi ID
            selectionOverlay = itemView.findViewById(R.id.selection_overlay); // Ánh xạ overlay
        }
    }
}