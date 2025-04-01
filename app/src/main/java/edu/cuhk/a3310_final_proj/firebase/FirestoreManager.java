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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.cuhk.a3310_final_proj.models.Trip;
import edu.cuhk.a3310_final_proj.models.User;

/**
 * Manages Firestore database operations
 */
public class FirestoreManager {

    private static final String TAG = "FirestoreManager";
    private FirebaseFirestore db;
    private static FirestoreManager instance;

    // Collection names
    private static final String USERS_COLLECTION = "users";
    private static final String TRIPS_COLLECTION = "trips";

    // Private constructor for singleton
    private FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    // Singleton instance getter
    public static FirestoreManager getInstance() {
        if (instance == null) {
            instance = new FirestoreManager();
        }
        return instance;
    }

    /**
     * Get current user from Firebase Auth
     *
     * @return FirebaseUser or null if not logged in
     */
    private FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    /**
     * Save user profile to Firestore
     *
     * @param user User object to save
     * @param listener Callback for success/failure
     */
    public void saveUserProfile(User user, final DataCallback<User> listener) {
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            listener.onFailure(new Exception("No user logged in"));
            return;
        }

        // Set user ID to match Firebase Auth ID
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

    /**
     * Get user profile from Firestore
     *
     * @param listener Callback for success/failure
     */
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

    /**
     * Save a trip to Firestore
     *
     * @param trip Trip object to save
     * @param listener Callback for success/failure
     */
    public void saveTrip(final Trip trip, final DataCallback<Trip> listener) {
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            listener.onFailure(new Exception("No user logged in"));
            return;
        }

        // Set the owner ID if it's a new trip
        if (trip.getOwner_id() == null || trip.getOwner_id().isEmpty()) {
            trip.setOwner_id(currentUser.getUid());
        }

        // Update timestamps
        trip.setUpdatedAt(new Date());
        if (trip.getCreatedAt() == null) {
            trip.setCreatedAt(new Date());
        }

        CollectionReference tripsRef = db.collection(TRIPS_COLLECTION);

        if (trip.getId() == null || trip.getId().isEmpty()) {
            // Create new trip
            tripsRef.add(trip)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            trip.setId(documentReference.getId());

                            // Update the document with its ID
                            documentReference.set(trip)
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
            tripsRef.document(trip.getId())
                    .set(trip)
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

    /**
     * Get all trips for the current user
     *
     * @param listener Callback for success/failure
     */
    public void getUserTrips(final DataCallback<List<Trip>> listener) {
        FirebaseUser currentUser = getCurrentUser();
        if (currentUser == null) {
            listener.onFailure(new Exception("No user logged in"));
            return;
        }

        db.collection(TRIPS_COLLECTION)
                .whereEqualTo("ownerId", currentUser.getUid())
                .orderBy("startDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<Trip> trips = new ArrayList<>();
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Trip trip = document.toObject(Trip.class);
                            trips.add(trip);
                        }
                        listener.onSuccess(trips);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        listener.onFailure(e);
                    }
                });
    }

    /**
     * Get a specific trip by ID
     *
     * @param tripId Trip ID
     * @param listener Callback for success/failure
     */
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

    /**
     * Delete a trip
     *
     * @param tripId Trip ID to delete
     * @param listener Callback for success/failure
     */
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

    /**
     * Generic callback interface for data operations
     */
    public interface DataCallback<T> {

        void onSuccess(T result);

        void onFailure(Exception e);
    }
}
