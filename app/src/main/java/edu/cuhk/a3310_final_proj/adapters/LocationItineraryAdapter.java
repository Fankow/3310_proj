package edu.cuhk.a3310_final_proj.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.models.Location;

public class LocationItineraryAdapter extends RecyclerView.Adapter<LocationItineraryAdapter.LocationViewHolder> {

    private List<Location> locations;
    private Context context;

    public LocationItineraryAdapter(Context context, List<Location> locations) {
        this.context = context;
        this.locations = locations;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_location_itinerary, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        Location location = locations.get(position);

        holder.tvLocationName.setText(location.getName());

        String timeRange = "";
        if (location.getStartTime() != null && !location.getStartTime().isEmpty()) {
            timeRange = location.getStartTime();

            if (location.getEndTime() != null && !location.getEndTime().isEmpty()) {
                timeRange += " - " + location.getEndTime();
            }
        }

        if (!timeRange.isEmpty()) {
            holder.tvTimeRange.setText(timeRange);
            holder.tvTimeRange.setVisibility(View.VISIBLE);
        } else {
            holder.tvTimeRange.setVisibility(View.GONE);
        }

        // Show notes if available
        if (location.getNotes() != null && !location.getNotes().isEmpty()) {
            holder.tvNotes.setText(location.getNotes());
            holder.tvNotes.setVisibility(View.VISIBLE);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }

        // Add click listener to open in Maps
        holder.itemView.setOnClickListener(v -> {
            openInGoogleMaps(location);
        });
    }

    private void openInGoogleMaps(Location location) {

        if (location.getLatitude() != 0 && location.getLongitude() != 0) {
            Uri gmmIntentUri = Uri.parse("geo:" + location.getLatitude() + "," + location.getLongitude()
                    + "?q=" + location.getLatitude() + "," + location.getLongitude()
                    + "(" + Uri.encode(location.getName()) + ")");
            openMapsIntent(gmmIntentUri);
        } else {

            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(location.getName()));
            openMapsIntent(gmmIntentUri);
        }
    }

    private void openMapsIntent(Uri gmmIntentUri) {
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(mapIntent);
        } else {
            Uri browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query="
                    + Uri.encode(gmmIntentUri.getQueryParameter("q")));
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
            context.startActivity(browserIntent);
        }
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {

        TextView tvLocationName, tvTimeRange, tvNotes;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLocationName = itemView.findViewById(R.id.tv_location_name);
            tvTimeRange = itemView.findViewById(R.id.tv_time_range);
            tvNotes = itemView.findViewById(R.id.tv_location_notes);
        }
    }
}
