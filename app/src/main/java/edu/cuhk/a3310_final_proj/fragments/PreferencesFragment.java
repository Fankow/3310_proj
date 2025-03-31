package edu.cuhk.a3310_final_proj.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import edu.cuhk.a3310_final_proj.R;

public class PreferencesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // We'll create a simple layout for now - replace with actual layout later
        return inflater.inflate(R.layout.fragment_preferences, container, false);
    }
}
