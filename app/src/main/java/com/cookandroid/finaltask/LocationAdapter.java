package com.cookandroid.finaltask;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.ViewHolder> { // RecyclerView.Adapter 상속

    private final List<SavedLocation> locations;
    private final OnItemClickListener onItemClickListener;
    private final OnStartDragListener onStartDragListener;

    // ItemTouchHelper를 위한 인터페이스 추가
    public interface OnItemMoveListener {
        void onItemMove(int fromPosition, int toPosition);
        void onDragFinish(List<SavedLocation> locations);
    }

    // 기존의 OnItemClickListener 유지
    public interface OnItemClickListener {
        void onDeleteClick(int position);
        void onItemClick(int position); // 아이템 클릭 시 순서 이동 다이얼로그를 위해 추가
    }

    // 드래그 시작 리스너 유지
    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public LocationAdapter(List<SavedLocation> locations, OnItemClickListener itemClickListener, OnStartDragListener dragStartListener) {
        this.locations = locations;
        this.onItemClickListener = itemClickListener;
        this.onStartDragListener = dragStartListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.location_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedLocation location = locations.get(position);
        holder.locationName.setText(location.getLocationName());

        // 1. 삭제 버튼 클릭 리스너
        holder.deleteButton.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onDeleteClick(position);
            }
        });

        // 2. 드래그 핸들 터치 리스너 (실제 드래그 시작)
        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (onStartDragListener != null) {
                    onStartDragListener.onStartDrag(holder);
                }
                return true;
            }
            return false;
        });

        // 3. 전체 아이템 클릭 리스너 (순서 이동 다이얼로그를 위해 유지)
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    // 아이템 이동 처리 (드래그 앤 드롭 시 호출됨)
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(locations, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(locations, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        // 드래그가 끝난 것은 ItemTouchHelper.Callback에서 처리하므로 여기서 saveLocationOrder를 호출하지 않습니다.
    }

    public List<SavedLocation> getLocations() {
        return locations;
    }

    // ViewHolder 클래스
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView dragHandle;
        TextView locationName;
        ImageView deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // location_item.xml의 ID를 사용합니다.
            dragHandle = itemView.findViewById(R.id.imageViewDragHandle);
            locationName = itemView.findViewById(R.id.textViewLocationName);
            deleteButton = itemView.findViewById(R.id.imageViewDelete);
        }
    }
}