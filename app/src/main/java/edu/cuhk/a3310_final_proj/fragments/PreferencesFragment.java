package edu.cuhk.a3310_final_proj.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

import edu.cuhk.a3310_final_proj.LoginActivity;
import edu.cuhk.a3310_final_proj.R;

public class PreferencesFragment extends Fragment {

    private Button logoutButton;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_preferences, container, false);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Initialize views
        logoutButton = view.findViewById(R.id.btn_logout);

        // Set up logout button click listener
        logoutButton.setOnClickListener(v -> confirmLogout());

        return view;
    }

    /**
     * Shows a confirmation dialog before logging out
     */
    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Performs the actual logout operation
     */
    private void performLogout() {
        // Sign out from Firebase
        auth.signOut();

        // Show success message
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Redirect to login screen
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        // Clear the back stack so the user can't navigate back to the app without logging in
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Close the current activity (optional if you're using the intent flags above)
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}