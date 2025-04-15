package edu.cuhk.a3310_final_proj.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.firebase.FavoriteManager;
import edu.cuhk.a3310_final_proj.models.Hotel;

public class HotelAdapter extends RecyclerView.Adapter<HotelAdapter.HotelViewHolder> {

    private Context context;
    private List<Hotel> hotels;
    private FavoriteManager favoriteManager;

    public HotelAdapter(Context context, List<Hotel> hotels) {
        this.context = context;
        this.hotels = hotels;
        this.favoriteManager = FavoriteManager.getInstance();
    }

    @NonNull
    @Override
    public HotelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_hotel, parent, false);
        return new HotelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HotelViewHolder holder, int position) {
        Hotel hotel = hotels.get(position);

        holder.tvHotelName.setText(hotel.getName());

        // Set description if available
        if (hotel.getDescription() != null && !hotel.getDescription().isEmpty()) {
            holder.tvHotelDescription.setText(hotel.getDescription());
            holder.tvHotelDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvHotelDescription.setVisibility(View.GONE);
        }

        // Set address if available
        if (hotel.getAddress() != null && !hotel.getAddress().isEmpty()) {
            holder.tvHotelAddress.setText(hotel.getAddress());
            holder.tvHotelAddress.setVisibility(View.VISIBLE);
        } else {
            holder.tvHotelAddress.setVisibility(View.GONE);
        }
        if (hotel.getImageUrl() != null && !hotel.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(hotel.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.ivHotelImage);
            holder.ivHotelImage.setVisibility(View.VISIBLE);
        } else {
            holder.ivHotelImage.setVisibility(View.GONE);
        }

        if (hotel.getLatitude() != 0 && hotel.getLongitude() != 0) {
            holder.btnViewOnMap.setVisibility(View.VISIBLE);
            holder.btnViewOnMap.setOnClickListener(v -> {
                openInGoogleMaps(hotel);
            });
        } else {
            holder.btnViewOnMap.setVisibility(View.GONE);
        }
        checkAndUpdateFavoriteStatus(holder.btnFavoriteHotel, hotel);

        holder.btnFavoriteHotel.setOnClickListener(v -> {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(context, "Please sign in to save favorites", Toast.LENGTH_SHORT).show();
                return;
            }

            favoriteManager.checkIfFavorite(hotel, new FavoriteManager.FavoriteCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean isFavorite) {
                    if (isFavorite) {
                        favoriteManager.removeFavoriteHotel(hotel, new FavoriteManager.FavoriteCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                holder.btnFavoriteHotel.setImageResource(R.drawable.ic_favorite_border);
                                Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(context, "Failed to remove from favorites", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {

                        favoriteManager.addFavoriteHotel(hotel, new FavoriteManager.FavoriteCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                holder.btnFavoriteHotel.setImageResource(R.drawable.ic_favorite_filled);
                                Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(context, "Failed to add to favorites", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(context, "Error checking favorite status", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void checkAndUpdateFavoriteStatus(ImageButton favoriteButton, Hotel hotel) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_border);
            return;
        }

        favoriteManager.checkIfFavorite(hotel, new FavoriteManager.FavoriteCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isFavorite) {
                if (isFavorite) {
                    favoriteButton.setImageResource(R.drawable.ic_favorite_filled);
                } else {
                    favoriteButton.setImageResource(R.drawable.ic_favorite_border);
                }
            }

            @Override
            public void onFailure(Exception e) {
                favoriteButton.setImageResource(R.drawable.ic_favorite_border);
            }
        });
    }

    private void openInGoogleMaps(Hotel hotel) {
        // Create a URI for Google Maps using the hotel's coordinates
        Uri gmmIntentUri = Uri.parse("geo:" + hotel.getLatitude() + "," + hotel.getLongitude()
                + "?q=" + hotel.getLatitude() + "," + hotel.getLongitude()
                + "(" + Uri.encode(hotel.getName()) + ")");

        // Create an Intent from the URI
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

        // Make the intent specific to Google Maps
        mapIntent.setPackage("com.google.android.apps.maps");

        // Check if Google Maps is installed
        if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(mapIntent);
        } else {
            // Fallback to browser if Google Maps is not installed
            Uri browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query="
                    + Uri.encode(hotel.getName())
                    + "&ll=" + hotel.getLatitude() + "," + hotel.getLongitude());
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
            context.startActivity(browserIntent);
        }
    }

    @Override
    public int getItemCount() {
        return hotels.size();
    }

    public class HotelViewHolder extends RecyclerView.ViewHolder {

        TextView tvHotelName, tvHotelAddress, tvHotelDescription;
        ImageView ivHotelImage;
        ImageButton btnViewOnMap, btnFavoriteHotel;

        public HotelViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHotelName = itemView.findViewById(R.id.tv_hotel_name);
            tvHotelAddress = itemView.findViewById(R.id.tv_hotel_address);
            tvHotelDescription = itemView.findViewById(R.id.tv_hotel_description);
            ivHotelImage = itemView.findViewById(R.id.iv_hotel_image);
            btnViewOnMap = itemView.findViewById(R.id.btn_view_on_map);
            btnFavoriteHotel = itemView.findViewById(R.id.btn_favorite_hotel);
        }
    }
}
