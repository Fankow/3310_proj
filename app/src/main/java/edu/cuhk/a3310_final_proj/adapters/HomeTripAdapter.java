package edu.cuhk.a3310_final_proj.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.models.Trip;

public class HomeTripAdapter extends RecyclerView.Adapter<HomeTripAdapter.TripViewHolder> {

    private final Context context;
    private final List<Trip> trips;
    private final TripClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface TripClickListener {

        void onTripClicked(Trip trip);
    }

    public HomeTripAdapter(Context context, List<Trip> trips, TripClickListener listener) {
        this.context = context;
        this.trips = trips;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_home_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = trips.get(position);

        holder.tvTripName.setText(trip.getName());
        holder.tvDestination.setText(trip.getDestination());

        // Format date range
        if (trip.getStartDate() != null && trip.getEndDate() != null) {
            String dateRange = dateFormat.format(trip.getStartDate())
                    + " - " + dateFormat.format(trip.getEndDate());
            holder.tvDateRange.setText(dateRange);
        } else {
            holder.tvDateRange.setText("Dates not set");
        }

        // Load image if available
        if (trip.getImageUrl() != null && !trip.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(trip.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.ivTripImage);
            holder.ivTripImage.setVisibility(View.VISIBLE);
        } else {
            holder.ivTripImage.setVisibility(View.GONE);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTripClicked(trip);
            }
        });
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {

        TextView tvTripName, tvDestination, tvDateRange;
        ImageView ivTripImage;

        TripViewHolder(View itemView) {
            super(itemView);
            tvTripName = itemView.findViewById(R.id.tv_trip_name);
            tvDestination = itemView.findViewById(R.id.tv_destination);
            tvDateRange = itemView.findViewById(R.id.tv_date_range);
            ivTripImage = itemView.findViewById(R.id.iv_trip_image);
        }
    }
}
