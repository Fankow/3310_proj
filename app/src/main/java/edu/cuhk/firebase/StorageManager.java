package edu.cuhk.a3310_final_proj.firebase;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

/**
 * Manages Firebase Storage operations
 */
public class StorageManager {

    private static final String TAG = "StorageManager";
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private static StorageManager instance;

    // Private constructor for singleton
    private StorageManager() {
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    // Singleton instance getter
    public static StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }

    /**
     * Upload an image for a trip
     *
     * @param tripId Trip ID
     * @param imageUri Image URI
     * @param listener Callback for success/failure
     */
    public void uploadTripImage(String tripId, Uri imageUri, final UploadCallback listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            listener.onFailure(new Exception("No user logged in"));
            return;
        }

        // Create unique filename
        String fileName = UUID.randomUUID().toString() + ".jpg";
        final String imagePath = "trips/" + currentUser.getUid() + "/" + tripId + "/" + fileName;

        StorageReference imageRef = storageRef.child(imagePath);

        // Upload file
        UploadTask uploadTask = imageRef.putFile(imageUri);

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Get download URL
                imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri downloadUri) {
                        Log.d(TAG, "Upload successful: " + downloadUri.toString());
                        listener.onSuccess(downloadUri.toString());
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error getting download URL", e);
                        listener.onFailure(e);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Error uploading image", e);
                listener.onFailure(e);
            }
        });
    }

    /**
     * Delete an image from storage
     *
     * @param imageUrl Image URL to delete
     * @param listener Callback for success/failure
     */
    public void deleteImage(String imageUrl, final DeleteCallback listener) {
        try {
            // Get reference from URL
            StorageReference imageRef = storage.getReferenceFromUrl(imageUrl);

            imageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "Image deleted successfully");
                    listener.onSuccess();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error deleting image", e);
                    listener.onFailure(e);
                }
            });
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid URL format", e);
            listener.onFailure(e);
        }
    }

    /**
     * Callback interface for Upload operations
     */
    public interface UploadCallback {

        void onSuccess(String downloadUrl);

        void onFailure(Exception e);
    }

    /**
     * Callback interface for Delete operations
     */
    public interface DeleteCallback {

        void onSuccess();

        void onFailure(Exception e);
    }
}
