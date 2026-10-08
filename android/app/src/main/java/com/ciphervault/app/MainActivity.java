package com.ciphervault.app;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.ciphervault.app.motion.NavigationMotionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Single-Activity application shell managing NavHostFragment, BottomNavigationView,
 * and centralized bottom-navigation visibility and directional transitions.
 */
public class MainActivity extends AppCompatActivity {

    private final NavigationMotionManager motionManager = new NavigationMotionManager();
    private BottomNavigationView bottomNavigationView;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            setupBottomNavigation();
            setupBackNavigation();
        }
    }

    private void setupBackNavigation() {
        // Top-level back behavior: exit application on any of the four roots (Correction 1)
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navController != null) {
                    NavDestination currentDest = navController.getCurrentDestination();
                    if (currentDest != null && NavigationMotionManager.shouldExitOnBack(currentDest.getId())) {
                        finish();
                        return;
                    }
                    if (navController.popBackStack()) {
                        return;
                    }
                }
                finish();
            }
        });
    }

    private void setupBottomNavigation() {
        // Tab selection with directional motion and multiple back-stacks (A13, B4)
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int targetIndex = NavigationMotionManager.getTabIndexForMenuItemId(item.getItemId());
            if (targetIndex != -1) {
                if (targetIndex == motionManager.getCurrentTabIndex()) {
                    // Reselection (A14): Pop back-stack to the root destination of this section
                    navController.popBackStack(item.getItemId(), false);
                    return true;
                }

                int startDestId = 0;
                NavGraph graph = navController.getGraph();
                NavDestination startDest = graph.findNode(graph.getStartDestinationId());
                if (startDest != null) {
                    startDestId = startDest.getId();
                }

                NavOptions navOptions = motionManager.createTabNavOptions(
                        this,
                        startDestId,
                        targetIndex
                );

                motionManager.setCurrentTabIndex(targetIndex);
                try {
                    navController.navigate(item.getItemId(), null, navOptions);
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
            return false;
        });

        // Tab reselection handling (A14)
        bottomNavigationView.setOnItemReselectedListener(item -> {
            navController.popBackStack(item.getItemId(), false);
        });

        // Centralized bottom navigation visibility and tab sync (A16)
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            boolean isTopLevel = (id == R.id.dest_home
                    || id == R.id.dest_files
                    || id == R.id.dest_activity
                    || id == R.id.dest_profile);

            bottomNavigationView.setVisibility(isTopLevel ? View.VISIBLE : View.GONE);

            // Sync menu checked item and motion manager when returning to top-level tab
            int tabIndex = -1;
            int menuItemId = -1;
            if (id == R.id.dest_home) {
                tabIndex = NavigationMotionManager.TAB_INDEX_HOME;
                menuItemId = R.id.nav_home;
            } else if (id == R.id.dest_files) {
                tabIndex = NavigationMotionManager.TAB_INDEX_FILES;
                menuItemId = R.id.nav_files;
            } else if (id == R.id.dest_activity) {
                tabIndex = NavigationMotionManager.TAB_INDEX_ACTIVITY;
                menuItemId = R.id.nav_activity;
            } else if (id == R.id.dest_profile) {
                tabIndex = NavigationMotionManager.TAB_INDEX_PROFILE;
                menuItemId = R.id.nav_profile;
            }

            if (tabIndex != -1) {
                motionManager.setCurrentTabIndex(tabIndex);
                MenuItem item = bottomNavigationView.getMenu().findItem(menuItemId);
                if (item != null && !item.isChecked()) {
                    item.setChecked(true);
                }
            }
        });
    }

    public NavigationMotionManager getMotionManager() {
        return motionManager;
    }
}