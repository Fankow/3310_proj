package edu.cuhk.a3310_final_proj.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cuhk.a3310_final_proj.models.Trip;
import edu.cuhk.a3310_final_proj.models.User;

/**
 * Manages Firestore database operations
 */
public class FirestoreManager {

    private static final String TAG = "FirestoreManager";
    private FirebaseFirestore db;
    private static FirestoreManager instance;

    private static final String USERS_COLLECTION = "users";
    private static final String TRIPS_COLLECTION = "trips";

    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static FirestoreManager getInstance() {
        if (instance == null) {
            instance = new FirestoreManager();
        }
        return instance;
    }


    private FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public void saveUserProfile(User user, final DataCallback<User> listener) {
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            listener.onFailure(new Exception("No user logged in"));
            return;
        }
        user.setId(currentUser.getUid());

        db.collection(USERS_COLLECTION).document(currentUser.getUid())
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        listener.onSuccess(user);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        listener.onFailure(e);
                    }
                });
    }

    public void getUserProfile(final DataCallback<User> listener) {
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            listener.onFailure(new Exception("No user logged in"));
            return;
        }

        db.collection(USERS_COLLECTION).document(currentUser.getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            listener.onSuccess(user);
                        } else {
                            listener.onSuccess(null);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error getting user profile", e);
                        listener.onFailure(e);
                    }
                });
    }

    public void saveTrip(final Trip trip, final DataCallback<Trip> listener) {
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            listener.onFailure(new Exception("No user logged in"));
            return;
        }

        if (trip.getOwner_id() == null || trip.getOwner_id().isEmpty()) {
            trip.setOwner_id(currentUser.getUid());
        }

        Map<String, Object> tripData = new HashMap<>();
        tripData.put("name", trip.getName());
        tripData.put("destination", trip.getDestination());
        tripData.put("startDate", trip.getStartDate());
        tripData.put("endDate", trip.getEndDate());
        tripData.put("flightNumber", trip.getFlightNumber());
        tripData.put("budget", trip.getBudget());
        tripData.put("currency", trip.getCurrency());
        tripData.put("notes", trip.getNotes());
        tripData.put("imageUrl", trip.getImageUrl());
        tripData.put("locations", trip.getLocations());
        tripData.put("documents", trip.getDocuments());
        tripData.put("createdAt", trip.getCreatedAt());
        tripData.put("updatedAt", trip.getUpdatedAt());

        if (trip.getExpenses() != null) {
            tripData.put("expenses", trip.getExpenses());
        }

        tripData.put("owner_id", currentUser.getUid());

        tripData.put("updatedAt", new Date());
        if (trip.getCreatedAt() == null) {
            tripData.put("createdAt", new Date());
        }

        CollectionReference tripsRef = db.collection(TRIPS_COLLECTION);

        if (trip.getId() == null || trip.getId().isEmpty()) {
            // Create new trip
            tripsRef.add(tripData)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            trip.setId(documentReference.getId());

                            // Update the document with its ID
                            tripData.put("id", documentReference.getId());
                            documentReference.set(tripData)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            listener.onSuccess(trip);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            listener.onFailure(e);
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            listener.onFailure(e);
                        }
                    });
        } else {
            // Update existing trip
            tripData.put("id", trip.getId());
            tripsRef.document(trip.getId())
                    .set(tripData)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            listener.onSuccess(trip);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            listener.onFailure(e);
                        }
                    });
        }
    }

    public void getUserTrips(final DataCallback<List<Trip>> listener) {
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            listener.onFailure(new Exception("No user logged in"));
            return;
        }

        Log.d("FirestoreManager", "Getting trips for user: " + currentUser.getUid());

        db.collection(TRIPS_COLLECTION)
                .whereEqualTo("owner_id", currentUser.getUid())
                .orderBy("startDate", Query.Direction.DESCENDING)
                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("FirestoreManager", "Query succeeded, got " + queryDocumentSnapshots.size() + " trips");
                    List<Trip> trips = new ArrayList<>();

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Trip trip = document.toObject(Trip.class);
                            if (trip != null) {
                                trip.setId(document.getId());
                                trips.add(trip);
                                Log.d("FirestoreManager", "Added trip: " + trip.getName());
                            }
                        } catch (Exception e) {
                            Log.e("FirestoreManager", "Error converting document: " + document.getId(), e);
                        }
                    }

                    listener.onSuccess(trips);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreManager", "Failed to get trips: " + e.getMessage(), e);
                    listener.onFailure(e);
                });
    }

    public void getTripById(String tripId, final DataCallback<Trip> listener) {
        db.collection(TRIPS_COLLECTION).document(tripId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            Trip trip = documentSnapshot.toObject(Trip.class);
                            listener.onSuccess(trip);
                        } else {
                            listener.onSuccess(null);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        listener.onFailure(e);
                    }
                });
    }

    public void deleteTrip(String tripId, final DataCallback<Void> listener) {
        if (tripId == null || tripId.isEmpty()) {
            listener.onFailure(new Exception("Invalid trip ID"));
            return;
        }

        db.collection(TRIPS_COLLECTION).document(tripId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(listener::onFailure);
    }

    public interface DataCallback<T> {

        void onSuccess(T result);

        void onFailure(Exception e);
    }
}
