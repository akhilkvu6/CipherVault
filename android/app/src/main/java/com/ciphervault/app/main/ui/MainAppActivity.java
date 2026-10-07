package com.ciphervault.app.main.ui;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.ciphervault.app.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainAppActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_app);

        View mainAppRoot = findViewById(R.id.mainAppRoot);
        bottomNav = findViewById(R.id.bottomNavigation);

        ViewCompat.setOnApplyWindowInsetsListener(mainAppRoot, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int extraTop = (int) (10 * getResources().getDisplayMetrics().density);
            v.setPadding(systemBars.left, systemBars.top + extraTop, systemBars.right, 0);

            if (bottomNav != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bottomNav.getLayoutParams();
                int baseMargin = (int) (20 * getResources().getDisplayMetrics().density);
                lp.bottomMargin = systemBars.bottom + baseMargin;
                bottomNav.setLayoutParams(lp);
            }
            return insets;
        });

        bottomNav.setOnItemSelectedListener(item -> {
            clearBackStack();
            Fragment selected = null;
            int id = item.getItemId();
            if (id == R.id.nav_home) selected = new HomeFragment();
            else if (id == R.id.nav_files) selected = new FilesFragment();
            else if (id == R.id.nav_transfers) selected = new TransfersFragment();
            else if (id == R.id.nav_settings) selected = new SettingsFragment();
            
            if (selected != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, selected)
                    .commit();
                return true;
            }
            return false;
        });
        
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else if (getIntent().getBooleanExtra("FROM_MANAGE_STORAGE", false)) {
                    finish();
                } else if (bottomNav.getSelectedItemId() != R.id.nav_home) {
                    bottomNav.setSelectedItemId(R.id.nav_home);
                } else {
                    finish();
                }
            }
        });

        if (savedInstanceState == null) {
            String startTab = getIntent().getStringExtra("START_TAB");
            if ("FILES".equals(startTab)) {
                bottomNav.setSelectedItemId(R.id.nav_files);
            } else {
                bottomNav.setSelectedItemId(R.id.nav_home);
            }
        }
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String startTab = intent.getStringExtra("START_TAB");
        if ("FILES".equals(startTab)) {
            bottomNav.setSelectedItemId(R.id.nav_files);
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (current instanceof FilesFragment) {
                ((FilesFragment) current).setCategory(intent.getStringExtra("FILTER_CATEGORY"));
            }
        }
    }

    public String getFilterCategory() {
        return getIntent().getStringExtra("FILTER_CATEGORY");
    }

    public void navigateToFiles() {
        bottomNav.setSelectedItemId(R.id.nav_files);
    }

    public void navigateToTransfers() {
        bottomNav.setSelectedItemId(R.id.nav_transfers);
    }
    
    public void addFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit();
    }
    
    public void clearBackStack() {
        getSupportFragmentManager().popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
    
    public void showBottomNav(boolean show) {
        bottomNav.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
