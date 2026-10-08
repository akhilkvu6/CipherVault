package com.ciphervault.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class MainActivity extends BaseActivity {

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {});

    private static final String TAG_HOME = "tag_home";
    private static final String TAG_VAULT = "tag_vault";
    private static final String TAG_TRANSFERS = "tag_transfers";
    private static final String TAG_SETTINGS = "tag_settings";

    private static final String KEY_CURRENT_TAB = "key_current_tab";

    private SessionManager sessionManager;
    private int currentTabIndex = 0;
    private boolean isNavigatingFromCode = false;

    private BottomNavigationView bottomNav;
    private ExtendedFloatingActionButton fabUpload;

    private FragmentHome fragmentHome;
    private FragmentVault fragmentVault;
    private FragmentTransfers fragmentTransfers;
    private FragmentSettings fragmentSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        fabUpload = findViewById(R.id.fabUpload);

        if (fabUpload != null) {
            fabUpload.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, UploadStagingActivity.class);
                startActivity(intent);
            });
        }

        View root = findViewById(R.id.main_root);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                View navHost = findViewById(R.id.nav_host_container);
                if (navHost != null) {
                    navHost.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
                }

                if (bottomNav != null) {
                    bottomNav.setPadding(0, 0, 0, systemBars.bottom);
                }

                if (fabUpload != null) {
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) fabUpload.getLayoutParams();
                    int baseMargin = (int) (96 * getResources().getDisplayMetrics().density);
                    params.bottomMargin = systemBars.bottom + baseMargin;
                    fabUpload.setLayoutParams(params);
                }

                return insets;
            });
        }

        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState != null) {
            fragmentHome = (FragmentHome) fm.findFragmentByTag(TAG_HOME);
            fragmentVault = (FragmentVault) fm.findFragmentByTag(TAG_VAULT);
            fragmentTransfers = (FragmentTransfers) fm.findFragmentByTag(TAG_TRANSFERS);
            fragmentSettings = (FragmentSettings) fm.findFragmentByTag(TAG_SETTINGS);
            currentTabIndex = savedInstanceState.getInt(KEY_CURRENT_TAB, 0);
        }

        setupBottomNavigation();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (savedInstanceState == null) {
            handleIntent(getIntent());
        } else {
            selectTab(currentTabIndex);
        }

        // Back button navigation
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentTabIndex != 0) {
                    selectTab(0);
                } else {
                    finish();
                }
            }
        });
    }

    private void setupBottomNavigation() {
        if (bottomNav == null) return;

        bottomNav.setOnItemSelectedListener(item -> {
            if (isNavigatingFromCode) return true;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                switchTabFragment(0);
                return true;
            } else if (itemId == R.id.nav_vault) {
                switchTabFragment(1);
                return true;
            } else if (itemId == R.id.nav_transfers) {
                switchTabFragment(2);
                return true;
            } else if (itemId == R.id.nav_settings) {
                switchTabFragment(3);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(KEY_CURRENT_TAB, currentTabIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("EXTRA_OPEN_TRANSFERS_TAB", false)) {
            selectTab(2);
        } else if (intent != null && intent.getBooleanExtra("EXTRA_OPEN_FILES_TAB", false)) {
            String category = intent.getStringExtra("EXTRA_FILTER_CATEGORY");
            selectTab(1);
            if (category != null && fragmentVault != null) {
                fragmentVault.applyCategoryFilter(category);
            }
        } else {
            selectTab(0);
        }
    }

    public void selectTab(int index) {
        if (index < 0 || index > 3) index = 0;
        int targetId;
        switch (index) {
            case 0:
                targetId = R.id.nav_home;
                break;
            case 1:
                targetId = R.id.nav_vault;
                break;
            case 2:
                targetId = R.id.nav_transfers;
                break;
            case 3:
            default:
                targetId = R.id.nav_settings;
                break;
        }

        if (bottomNav != null && bottomNav.getSelectedItemId() != targetId) {
            isNavigatingFromCode = true;
            try {
                bottomNav.setSelectedItemId(targetId);
            } finally {
                isNavigatingFromCode = false;
            }
        }
        switchTabFragment(index);
    }

    private void switchTabFragment(int index) {
        if (index < 0 || index > 3) index = 0;
        currentTabIndex = index;

        Fragment targetFragment;
        String targetTag;

        switch (index) {
            case 0:
                if (fragmentHome == null) fragmentHome = new FragmentHome();
                targetFragment = fragmentHome;
                targetTag = TAG_HOME;
                break;
            case 1:
                if (fragmentVault == null) fragmentVault = new FragmentVault();
                targetFragment = fragmentVault;
                targetTag = TAG_VAULT;
                break;
            case 2:
                if (fragmentTransfers == null) fragmentTransfers = new FragmentTransfers();
                targetFragment = fragmentTransfers;
                targetTag = TAG_TRANSFERS;
                break;
            case 3:
            default:
                if (fragmentSettings == null) fragmentSettings = new FragmentSettings();
                targetFragment = fragmentSettings;
                targetTag = TAG_SETTINGS;
                break;
        }

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

        for (Fragment f : fm.getFragments()) {
            if (f != null && f != targetFragment && f.isAdded()) {
                tx.hide(f);
            }
        }

        if (!targetFragment.isAdded()) {
            tx.add(R.id.nav_host_container, targetFragment, targetTag);
        } else {
            tx.show(targetFragment);
        }

        tx.commit();
    }

    public void navigateToTab(int index) {
        selectTab(index);
    }

    public void navigateToVaultCategory(String category) {
        selectTab(1);
        try {
            getSupportFragmentManager().executePendingTransactions();
        } catch (Exception ignored) {}
        if (fragmentVault != null) {
            fragmentVault.applyCategoryFilter(category);
        }
    }
}