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
    private final List<Trip> tripList;
    private final TripClickListener tripClickListener;
    private final SimpleDateFormat dateFormatter;

    public interface TripClickListener {

        void onTripClicked(Trip trip);
    }

    public HomeTripAdapter(Context context, List<Trip> tripList, TripClickListener tripClickListener) {
        this.context = context;
        this.tripList = tripList;
        this.tripClickListener = tripClickListener;
        this.dateFormatter = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_home_trip, parent, false);
        return new TripViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip currentTrip = tripList.get(position);
        holder.bind(currentTrip);
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    class TripViewHolder extends RecyclerView.ViewHolder {

        TextView tripNameTextView, destinationTextView, dateRangeTextView;
        ImageView tripImageView;

        TripViewHolder(View itemView) {
            super(itemView);
            tripNameTextView = itemView.findViewById(R.id.tv_trip_name);
            destinationTextView = itemView.findViewById(R.id.tv_destination);
            dateRangeTextView = itemView.findViewById(R.id.tv_date_range);
            tripImageView = itemView.findViewById(R.id.iv_trip_image);
        }

        void bind(Trip trip) {
            tripNameTextView.setText(trip.getName());
            destinationTextView.setText(trip.getDestination());

            displayDateRange(trip);
            loadImage(trip);

            itemView.setOnClickListener(v -> {
                if (tripClickListener != null) {
                    tripClickListener.onTripClicked(trip);
                }
            });
        }

        private void displayDateRange(Trip trip) {
            if (trip.getStartDate() != null && trip.getEndDate() != null) {
                String startDate = dateFormatter.format(trip.getStartDate());
                String endDate = dateFormatter.format(trip.getEndDate());
                String dateRange = startDate + " - " + endDate;
                dateRangeTextView.setText(dateRange);
            } else {
                dateRangeTextView.setText(R.string.dates_not_set); // Use a string resource
            }
        }

        private void loadImage(Trip trip) {
            if (trip.getImageUrl() != null && !trip.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(trip.getImageUrl())
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(tripImageView);
                tripImageView.setVisibility(View.VISIBLE);
            } else {
                tripImageView.setVisibility(View.GONE);
            }
        }
    }
}
