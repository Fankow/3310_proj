package edu.cuhk.a3310_final_proj.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.models.DayItineraryItem;
import edu.cuhk.a3310_final_proj.models.Location;

public class DayItineraryAdapter extends RecyclerView.Adapter<DayItineraryAdapter.DayViewHolder> {

    private List<DayItineraryItem> dayItems;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.US);

    public DayItineraryAdapter(Context context, List<DayItineraryItem> dayItems) {
        this.context = context;
        this.dayItems = dayItems;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_day_itinerary, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        DayItineraryItem dayItem = dayItems.get(position);

        // Set the day title
        holder.tvDayTitle.setText(dayItem.getDayTitle());

        // Set the date if available
        if (dayItem.getDate() != null) {
            holder.tvDayDate.setText(dateFormat.format(dayItem.getDate()));
            holder.tvDayDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvDayDate.setVisibility(View.GONE);
        }

        // Set up the locations recycler view
        LocationItineraryAdapter adapter = new LocationItineraryAdapter(context, dayItem.getLocations());
        holder.rvLocations.setAdapter(adapter);
    }

    @Override
    public int getItemCount() {
        return dayItems.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {

        TextView tvDayTitle, tvDayDate;
        RecyclerView rvLocations;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayTitle = itemView.findViewById(R.id.tv_day_title);
            tvDayDate = itemView.findViewById(R.id.tv_day_date);
            rvLocations = itemView.findViewById(R.id.rv_day_locations);
            rvLocations.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
        }
    }
}
