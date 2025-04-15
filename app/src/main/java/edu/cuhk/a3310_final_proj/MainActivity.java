package edu.cuhk.a3310_final_proj;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.content.Intent;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.libraries.places.api.Places;
import com.google.android.material.navigation.NavigationView;

import edu.cuhk.a3310_final_proj.fragments.CurrencyFragment;
import edu.cuhk.a3310_final_proj.fragments.FlightSearchFragment;
import edu.cuhk.a3310_final_proj.fragments.HomeFragment;
import edu.cuhk.a3310_final_proj.fragments.HotelNFlightFragment;
import edu.cuhk.a3310_final_proj.fragments.PreferencesFragment;
import edu.cuhk.a3310_final_proj.fragments.TripDetailFragment;
import edu.cuhk.a3310_final_proj.fragments.TripPlanningFragment;
import edu.cuhk.a3310_final_proj.fragments.TripViewFragment;
import edu.cuhk.a3310_final_proj.fragments.HotelSearchFragment;
import edu.cuhk.a3310_final_proj.fragments.FavoriteHotelsFragment;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;
    private Button plan;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_api_key));
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        plan = findViewById(R.id.btn_plan_new_trip);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // If this is the first time the app is opened, show the trip list fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new HomeFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_trips);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_trips) {
            selectedFragment = new TripViewFragment();
            getSupportActionBar().setTitle("My Trips");
        } else if (itemId == R.id.nav_plan) {
            selectedFragment = new TripPlanningFragment();
            getSupportActionBar().setTitle("Plan Trip");
        } else if (itemId == R.id.nav_currency) {
            selectedFragment = new CurrencyFragment();
            getSupportActionBar().setTitle("Currency Converter");
        } else if (itemId == R.id.nav_settings) {
            selectedFragment = new PreferencesFragment();
            getSupportActionBar().setTitle("Settings");
        } else if (itemId == R.id.nav_home) {
            selectedFragment = new HomeFragment();
            getSupportActionBar().setTitle("Home Page");
        }  else if (itemId == R.id.nav_hotel_search) {
            selectedFragment = new HotelSearchFragment();
            getSupportActionBar().setTitle("Hotel Search");
        } else if (itemId == R.id.nav_favorite_hotels) {
            selectedFragment = new FavoriteHotelsFragment();
            getSupportActionBar().setTitle("Favorite Hotels");
        } else if (itemId == R.id.nav_flight_search){
            selectedFragment = new FlightSearchFragment();
            getSupportActionBar().setTitle("Flight Search");
        }else if (itemId == R.id.nav_logout) {
            // Handle logout
            confirmLogout();
            return true;
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
        }

        // close the drawer after selecting item
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        // Redirect to login screen
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    @Override
    public void onBackPressed() {
        // press back will close the drawer
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
