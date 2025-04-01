package edu.cuhk.a3310_final_proj;

import android.os.Bundle;
import java.util.Locale;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.libraries.places.api.Places;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

import edu.cuhk.a3310_final_proj.fragments.CurrencyFragment;
import edu.cuhk.a3310_final_proj.fragments.HomeFragment;
import edu.cuhk.a3310_final_proj.fragments.PreferencesFragment;
import edu.cuhk.a3310_final_proj.fragments.TripPlanningFragment;
import edu.cuhk.a3310_final_proj.fragments.TripViewFragment;
import edu.cuhk.a3310_final_proj.fragments.WeatherFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize Firebase App Check
        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance());
        // Initialize Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_api_key));
        }
        // Initialize the bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set up navigation listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_plan) {
                selectedFragment = new TripPlanningFragment();
            } else if (itemId == R.id.nav_currency) {
                selectedFragment = new CurrencyFragment();
            } // Comment out the weather tab temporarily
        /*else if (itemId == R.id.nav_weather) {
            selectedFragment = new WeatherFragment();
        }*/ else if (itemId == R.id.nav_settings) {
                selectedFragment = new PreferencesFragment();
            } else if (itemId == R.id.nav_trip) {
                selectedFragment = new TripViewFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }

            return false;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }
}
