package edu.cuhk.a3310_final_proj.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.models.Hotel;

public class HotelAdapter extends RecyclerView.Adapter<HotelAdapter.HotelViewHolder> {
    private Context context;
    private List<Hotel> hotels;
    public HotelAdapter(Context context, List<Hotel> hotels){
        this.context = context;
        this.hotels = hotels;
    }

    @NonNull
    @Override
    public HotelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_hotel, parent,false);
        return new HotelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HotelViewHolder holder, int position) {
        Hotel hotel = hotels.get(position);
        holder.tvHotelName.setText(hotel.getName());
    }

    @Override
    public int getItemCount() {
        return hotels.size();
    }
    public class HotelViewHolder extends RecyclerView.ViewHolder{
        TextView tvHotelName, tvHotelAddress;
        public HotelViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHotelName = itemView.findViewById(R.id.tv_hotel_name);
            tvHotelAddress = itemView.findViewById(R.id.tv_hotel_address);
        }
    }
}
