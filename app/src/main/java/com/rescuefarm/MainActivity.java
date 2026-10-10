package com.rescuefarm;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.rescuefarm.R;
import com.rescuefarm.data.repository.AuthRepositoryFactory;
import com.rescuefarm.service.sync.OfflineSyncScheduler;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OfflineSyncScheduler.schedule(getApplicationContext());
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainRoot), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        NavController navController = ((NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.main)).getNavController();
        ImageView homeButton = findViewById(R.id.navHomeButton);
        ImageView communityButton = findViewById(R.id.navCommunityButton);
        ImageView ordersButton = findViewById(R.id.navOrdersButton);
        ImageView profileButton = findViewById(R.id.navProfileButton);
        MaterialButton centerCartButton = findViewById(R.id.centerCartButton);
        centerCartButton.setOnClickListener(unused -> navController.navigate(R.id.cartFragment));
        homeButton.setOnClickListener(unused -> navigateTo(navController, R.id.guestHomeFragment));
        communityButton.setOnClickListener(unused -> navigateTo(navController, R.id.feedFragment));
        ordersButton.setOnClickListener(unused ->
                navigateTo(navController, R.id.orderConfirmationFragment));
        profileButton.setOnClickListener(unused -> {
            if (!AuthRepositoryFactory.create(this).isAuthenticated()) {
                navController.navigate(R.id.loginFragment);
            } else {
                navigateTo(navController, R.id.profileFragment);
            }
        });
        navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                updateBottomNavigation(centerCartButton, homeButton, communityButton,
                        ordersButton, profileButton, destination)
        );
    }

    private void navigateTo(NavController navController, int destinationId) {
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() != destinationId) {
            navController.navigate(destinationId);
        }
    }

    private void updateBottomNavigation(MaterialButton centerCartButton,
                                        ImageView homeButton,
                                        ImageView communityButton,
                                        ImageView ordersButton,
                                        ImageView profileButton,
                                        NavDestination destination) {
        int destinationId = destination.getId();
        boolean show = destinationId == R.id.guestHomeFragment
                || destinationId == R.id.feedFragment
                || destinationId == R.id.cartFragment
                || destinationId == R.id.orderConfirmationFragment
                || destinationId == R.id.profileFragment;
        findViewById(R.id.customerBottomNavigationContainer).setVisibility(
                show ? android.view.View.VISIBLE : android.view.View.GONE
        );
        int inactive = ContextCompat.getColor(this, R.color.rescue_on_surface_variant);
        int active = ContextCompat.getColor(this, R.color.rescue_splash_green);
        tintNavigationIcon(homeButton, destinationId == R.id.guestHomeFragment, active, inactive);
        tintNavigationIcon(communityButton, destinationId == R.id.feedFragment, active, inactive);
        tintNavigationIcon(ordersButton, destinationId == R.id.orderConfirmationFragment,
                active, inactive);
        tintNavigationIcon(profileButton, destinationId == R.id.profileFragment, active, inactive);
        centerCartButton.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void tintNavigationIcon(ImageView icon, boolean selected, int active, int inactive) {
        icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                selected ? active : inactive
        ));
    }
}
