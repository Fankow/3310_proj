package edu.cuhk.a3310_final_proj.adapters;

import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.models.Location;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    private List<Location> locations = new ArrayList<>();
    private Context context;
    private LocationAdapterListener listener;
    private int dayIndex;  // To track which day this adapter belongs to

    public interface LocationAdapterListener {

        void onEditLocation(Location location, int position, int dayIndex);

        void onDeleteLocation(Location location, int position, int dayIndex);
    }

    public LocationAdapter(Context context, int dayIndex, LocationAdapterListener listener) {
        this.context = context;
        this.dayIndex = dayIndex;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        Location location = locations.get(position);
        holder.bind(location, position);

        holder.btnOpenInMaps.setOnClickListener(v -> {
            openInGoogleMaps(location);
        });
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    public void setLocations(List<Location> locations) {
        this.locations.clear();
        if (locations != null) {
            this.locations.addAll(locations);
        }
        notifyDataSetChanged();
    }

    public void addLocation(Location location) {
        this.locations.add(location);
        notifyItemInserted(locations.size() - 1);
    }

    public void updateLocation(Location location, int position) {
        this.locations.set(position, location);
        notifyItemChanged(position);
    }

    public void removeLocation(int position) {
        this.locations.remove(position);
        notifyItemRemoved(position);
    }

    public List<Location> getLocations() {
        return new ArrayList<>(locations);
    }

    class LocationViewHolder extends RecyclerView.ViewHolder {

        TextView tvLocationName, tvLocationTime;
        ImageButton btnEdit, btnDelete, btnOpenInMaps;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocationName = itemView.findViewById(R.id.tv_location_name);
            tvLocationTime = itemView.findViewById(R.id.tv_location_time);
            btnEdit = itemView.findViewById(R.id.btn_edit_location);
            btnDelete = itemView.findViewById(R.id.btn_delete_location);
            btnOpenInMaps = itemView.findViewById(R.id.btn_open_in_maps);
        }

        public void bind(Location location, int position) {
            tvLocationName.setText(location.getName());

            // Display time range
            String timeDisplay = "";
            if (location.getStartTime() != null && !location.getStartTime().isEmpty()) {
                timeDisplay += location.getStartTime();

                if (location.getEndTime() != null && !location.getEndTime().isEmpty()) {
                    timeDisplay += " - " + location.getEndTime();
                }
            }
            tvLocationTime.setText(timeDisplay);

            // Set click listeners for edit and delete
            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditLocation(location, position, dayIndex);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteLocation(location, position, dayIndex);
                }
            });
        }
    }

    private void openInGoogleMaps(Location location) {
        // Implementation for opening location in Google Maps
    }
}
