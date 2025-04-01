// Create TripAdapter.java in edu.cuhk.a3310_final_proj.adapters
package edu.cuhk.a3310_final_proj.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private final Context context;
    private final List<Trip> tripList;
    private final TripAdapterListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

    public interface TripAdapterListener {

        void onTripClicked(Trip trip);

        void onEditTrip(Trip trip);

        void onDeleteTrip(Trip trip);
    }

    public TripAdapter(Context context, List<Trip> tripList, TripAdapterListener listener) {
        this.context = context;
        this.tripList = tripList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);

        // Set trip name and destination
        holder.tvTripName.setText(trip.getName());
        holder.tvDestination.setText(trip.getDestination());

        // Format and set dates
        if (trip.getStartDate() != null && trip.getEndDate() != null) {
            String dateRange = dateFormat.format(trip.getStartDate()) + " - "
                    + dateFormat.format(trip.getEndDate());
            holder.tvDateRange.setText(dateRange);
        }

        // Set budget and currency
        if (trip.getBudget() > 0) {
            holder.tvBudget.setText(String.format(Locale.getDefault(),
                    "%.2f %s", trip.getBudget(), trip.getCurrency()));
            holder.tvBudget.setVisibility(View.VISIBLE);
        } else {
            holder.tvBudget.setVisibility(View.GONE);
        }

        // Load trip image if available
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

        // Set click listeners
        holder.itemView.setOnClickListener(v -> listener.onTripClicked(trip));
        holder.btnEdit.setOnClickListener(v -> listener.onEditTrip(trip));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteTrip(trip));
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {

        TextView tvTripName, tvDestination, tvDateRange, tvBudget;
        ImageView ivTripImage;
        ImageButton btnEdit, btnDelete;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTripName = itemView.findViewById(R.id.tv_trip_name);
            tvDestination = itemView.findViewById(R.id.tv_destination);
            tvDateRange = itemView.findViewById(R.id.tv_date_range);
            tvBudget = itemView.findViewById(R.id.tv_budget);
            ivTripImage = itemView.findViewById(R.id.iv_trip_image);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
