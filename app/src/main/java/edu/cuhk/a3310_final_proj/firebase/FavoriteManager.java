package edu.cuhk.a3310_final_proj.firebase;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cuhk.a3310_final_proj.models.Hotel;

public class FavoriteManager {

    private static final String TAG = "FavoriteManager";
    private static final String COLLECTION_FAVORITES = "favorites";
    private static FavoriteManager instance;
    private final FirebaseFirestore db;

    public interface FavoriteCallback<T> {

        void onSuccess(T result);

        void onFailure(Exception e);
    }

    private FavoriteManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FavoriteManager getInstance() {
        if (instance == null) {
            instance = new FavoriteManager();
        }
        return instance;
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return null;
        }
        return user.getUid();
    }

    public void addFavoriteHotel(Hotel hotel, FavoriteCallback<Void> callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("User not logged in"));
            return;
        }

        String hotelId = hotel.getName() + "_" + hotel.getLatitude() + "_" + hotel.getLongitude();

        // Check if hotel is already a favorite
        db.collection(COLLECTION_FAVORITES)
                .whereEqualTo("user_id", userId)
                .whereEqualTo("hotel_id", hotelId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Already a favorite, just return successs
                        callback.onSuccess(null);
                    } else {
                        Map<String, Object> favoriteHotel = new HashMap<>();
                        favoriteHotel.put("user_id", userId);
                        favoriteHotel.put("hotel_id", hotelId);
                        favoriteHotel.put("name", hotel.getName());
                        favoriteHotel.put("address", hotel.getAddress());
                        favoriteHotel.put("latitude", hotel.getLatitude());
                        favoriteHotel.put("longitude", hotel.getLongitude());
                        favoriteHotel.put("image_url", hotel.getImageUrl());
                        favoriteHotel.put("description", hotel.getDescription());
                        favoriteHotel.put("saved_at", com.google.firebase.Timestamp.now());

                        db.collection(COLLECTION_FAVORITES)
                                .add(favoriteHotel)
                                .addOnSuccessListener(documentReference -> {
                                    callback.onSuccess(null);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error adding favorite", e);
                                    callback.onFailure(e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking if hotel is favorite", e);
                    callback.onFailure(e);
                });
    }

    public void removeFavoriteHotel(Hotel hotel, FavoriteCallback<Void> callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("User not logged in"));
            return;
        }

        String hotelId = hotel.getName() + "_" + hotel.getLatitude() + "_" + hotel.getLongitude();

        db.collection(COLLECTION_FAVORITES)
                .whereEqualTo("user_id", userId)
                .whereEqualTo("hotel_id", hotelId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onSuccess(null);
                        return;
                    }

                    // Get the document reference and delete it
                    DocumentReference docRef = queryDocumentSnapshots.getDocuments().get(0).getReference();
                    docRef.delete()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error removing favorite", e);
                                callback.onFailure(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding favorite to remove", e);
                    callback.onFailure(e);
                });
    }

    public void checkIfFavorite(Hotel hotel, FavoriteCallback<Boolean> callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onSuccess(false);
            return;
        }

        String hotelId = hotel.getName() + "_" + hotel.getLatitude() + "_" + hotel.getLongitude();

        db.collection(COLLECTION_FAVORITES)
                .whereEqualTo("user_id", userId)
                .whereEqualTo("hotel_id", hotelId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isFavorite = !queryDocumentSnapshots.isEmpty();
                    callback.onSuccess(isFavorite);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking if hotel is favorite", e);
                    callback.onFailure(e);
                });
    }

    public void getFavoriteHotels(FavoriteCallback<List<Hotel>> callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        db.collection(COLLECTION_FAVORITES)
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Hotel> favoriteHotels = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Hotel hotel = new Hotel();
                        hotel.setName(document.getString("name"));
                        hotel.setAddress(document.getString("address"));
                        hotel.setLatitude(document.getDouble("latitude"));
                        hotel.setLongitude(document.getDouble("longitude"));
                        hotel.setImageUrl(document.getString("image_url"));
                        hotel.setDescription(document.getString("description"));
                        favoriteHotels.add(hotel);
                    }

                    callback.onSuccess(favoriteHotels);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting favorite hotels", e);
                    callback.onFailure(e);
                });
    }
}
