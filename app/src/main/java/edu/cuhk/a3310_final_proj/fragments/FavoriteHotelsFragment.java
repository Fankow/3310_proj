package edu.cuhk.a3310_final_proj.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.adapters.HotelAdapter;
import edu.cuhk.a3310_final_proj.firebase.FavoriteManager;
import edu.cuhk.a3310_final_proj.models.Hotel;

public class FavoriteHotelsFragment extends Fragment {

    private static final String TAG = "FavoriteHotelsFragment";
    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private HotelAdapter adapter;
    private List<Hotel> favoriteHotels = new ArrayList<>();
    private FavoriteManager favoriteManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorite_hotels, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        favoriteManager = FavoriteManager.getInstance();

        recyclerView = view.findViewById(R.id.recycler_view_favorites);
        emptyStateText = view.findViewById(R.id.empty_state_text);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HotelAdapter(requireContext(), favoriteHotels);
        recyclerView.setAdapter(adapter);

        loadFavoriteHotels();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data each time fragment resumes
        loadFavoriteHotels();
    }

    private void loadFavoriteHotels() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            emptyStateText.setText("Please sign in to view your favorite hotels");
            emptyStateText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        favoriteManager.getFavoriteHotels(new FavoriteManager.FavoriteCallback<List<Hotel>>() {
            @Override
            public void onSuccess(List<Hotel> result) {
                favoriteHotels.clear();
                favoriteHotels.addAll(result);

                if (favoriteHotels.isEmpty()) {
                    emptyStateText.setText("You haven't saved any hotels to your favorites yet");
                    emptyStateText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error loading favorite hotels", e);
                emptyStateText.setText("Error loading your favorite hotels");
                emptyStateText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }
}
