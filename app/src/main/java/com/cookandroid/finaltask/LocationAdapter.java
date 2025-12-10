package com.cookandroid.finaltask;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class LocationAdapter extends ArrayAdapter<SavedLocation> {

    private final Context context;
    private final List<SavedLocation> locations;
    private OnItemClickListener onItemClickListener;
    private OnDragStartListener onDragStartListener;

    public interface OnItemClickListener {
        void onDeleteClick(int position);
    }

    public interface OnDragStartListener {
        void onDragStart(View view, int position);
    }

    public LocationAdapter(Context context, List<SavedLocation> locations) {
        super(context, R.layout.location_item, locations);
        this.context = context;
        this.locations = locations;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnDragStartListener(OnDragStartListener listener) {
        this.onDragStartListener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.location_item, parent, false);
            holder = new ViewHolder();
            holder.dragHandle = convertView.findViewById(R.id.imageViewDragHandle);
            holder.locationName = convertView.findViewById(R.id.textViewLocationName);
            holder.deleteButton = convertView.findViewById(R.id.imageViewDelete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        SavedLocation location = locations.get(position);
        holder.locationName.setText(location.getLocationName());

        // 삭제 버튼 클릭
        holder.deleteButton.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onDeleteClick(position);
            }
        });

        // 드래그 핸들 터치
        final View finalConvertView = convertView;
        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (onDragStartListener != null) {
                    onDragStartListener.onDragStart(finalConvertView, position);
                }
                return true;
            }
            return false;
        });

        return convertView;
    }

    static class ViewHolder {
        ImageView dragHandle;
        TextView locationName;
        ImageView deleteButton;
    }
}