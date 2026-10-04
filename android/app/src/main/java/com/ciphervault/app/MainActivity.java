package com.ciphervault.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class MainActivity extends BaseActivity {

    private static final String TAG_HOME = "tag_home";
    private static final String TAG_FILES = "tag_files";
    private static final String TAG_UPLOAD = "tag_upload";
    private static final String TAG_SETTINGS = "tag_settings";
    private static final String KEY_CURRENT_TAB = "key_current_tab";

    private SessionManager sessionManager;
    private int currentTabIndex = 0;
    private Fragment currentFragment;
    private ExtendedFloatingActionButton fabUpload;

    private HomeFragment homeFragment;
    private FilesFragment filesFragment;
    private UploadFragment uploadFragment;
    private SettingsFragment settingsFragment;

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

        fabUpload = findViewById(R.id.fabUpload);
        if (fabUpload != null) {
            fabUpload.setOnClickListener(v -> navigateToTab(2));
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            View navHost = findViewById(R.id.nav_host_container);
            if (navHost != null) {
                navHost.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            }
            View nav = findViewById(R.id.layoutPillNav);
            if (nav != null) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) nav.getLayoutParams();
                int densityMargin = (int) (16 * getResources().getDisplayMetrics().density);
                lp.bottomMargin = systemBars.bottom + densityMargin;
                nav.setLayoutParams(lp);
            }
            if (fabUpload != null) {
                ViewGroup.MarginLayoutParams fabLp = (ViewGroup.MarginLayoutParams) fabUpload.getLayoutParams();
                int fabDensityMargin = (int) (88 * getResources().getDisplayMetrics().density);
                fabLp.bottomMargin = systemBars.bottom + fabDensityMargin;
                fabUpload.setLayoutParams(fabLp);
            }
            return insets;
        });

        // Restore cached fragments across process recreation
        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState != null) {
            homeFragment = (HomeFragment) fm.findFragmentByTag(TAG_HOME);
            filesFragment = (FilesFragment) fm.findFragmentByTag(TAG_FILES);
            uploadFragment = (UploadFragment) fm.findFragmentByTag(TAG_UPLOAD);
            settingsFragment = (SettingsFragment) fm.findFragmentByTag(TAG_SETTINGS);
            currentTabIndex = savedInstanceState.getInt(KEY_CURRENT_TAB, 0);
        }

        PillNavHelper.setup(this, this::selectTab);

        if (savedInstanceState == null) {
            handleIntent(getIntent());
        } else {
            selectTab(currentTabIndex);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_TAB, currentTabIndex);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("EXTRA_OPEN_FILES_TAB", false)) {
            String category = intent.getStringExtra("EXTRA_FILTER_CATEGORY");
            PillNavHelper.setTabVisualOnly(this, 1);
            selectTab(1);
            if (category != null && filesFragment != null) {
                filesFragment.applyCategoryFilter(category);
            }
        } else {
            selectTab(0);
        }
    }

    private void selectTab(int index) {
        currentTabIndex = index;
        updateFabVisibility(index);

        Fragment targetFragment;
        String targetTag;

        switch (index) {
            case 0:
                if (homeFragment == null) homeFragment = new HomeFragment();
                targetFragment = homeFragment;
                targetTag = TAG_HOME;
                break;
            case 1:
                if (filesFragment == null) filesFragment = new FilesFragment();
                targetFragment = filesFragment;
                targetTag = TAG_FILES;
                break;
            case 2:
                if (uploadFragment == null) uploadFragment = new UploadFragment();
                targetFragment = uploadFragment;
                targetTag = TAG_UPLOAD;
                break;
            case 3:
            default:
                if (settingsFragment == null) settingsFragment = new SettingsFragment();
                targetFragment = settingsFragment;
                targetTag = TAG_SETTINGS;
                break;
        }

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

        // Hide all fragments except target
        for (Fragment f : fm.getFragments()) {
            if (f != null && f != targetFragment && f.isAdded()) {
                ft.hide(f);
            }
        }

        if (!targetFragment.isAdded()) {
            ft.add(R.id.nav_host_container, targetFragment, targetTag);
        } else {
            ft.show(targetFragment);
        }

        ft.commit();
        currentFragment = targetFragment;
    }

    private void updateFabVisibility(int index) {
        if (fabUpload == null) return;
        if (index == 2) {
            fabUpload.hide();
        } else {
            fabUpload.show();
        }
    }

    public void launchFilePicker() {
        navigateToTab(2);
        if (uploadFragment != null) {
            uploadFragment.launchPicker();
        }
    }

    public void refreshCurrentTab() {
        if (currentFragment instanceof HomeFragment) {
            ((HomeFragment) currentFragment).loadDashboardData();
        } else if (currentFragment instanceof FilesFragment) {
            ((FilesFragment) currentFragment).loadFiles();
        }
    }

    public void navigateToTab(int index) {
        PillNavHelper.selectTab(this, index);
    }

    public void navigateToCategory(String category) {
        PillNavHelper.setTabVisualOnly(this, 1);
        selectTab(1);
        if (category != null && filesFragment != null) {
            filesFragment.applyCategoryFilter(category);
        }
    }
}