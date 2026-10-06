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

        // User must be authenticated.
        if (!sessionManager.isLoggedIn()) {

            Intent intent =
                    new Intent(
                            this,
                            LoginActivity.class
                    );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();

            return;
        }

        setContentView(R.layout.activity_main);

        fabUpload = findViewById(R.id.fabUpload);

        if (fabUpload != null) {

            fabUpload.setOnClickListener(v ->
                    navigateToTab(2)
            );
        }

        View root = findViewById(R.id.main_root);

        if (root != null) {

            ViewCompat.setOnApplyWindowInsetsListener(
                    root,
                    (view, insets) -> {

                        Insets systemBars =
                                insets.getInsets(
                                        WindowInsetsCompat.Type.systemBars()
                                );

                        View navHost =
                                findViewById(
                                        R.id.nav_host_container
                                );

                        if (navHost != null) {

                            navHost.setPadding(
                                    systemBars.left,
                                    systemBars.top,
                                    systemBars.right,
                                    0
                            );
                        }

                        View nav =
                                findViewById(
                                        R.id.layoutPillNav
                                );

                        if (nav != null) {

                            ViewGroup.MarginLayoutParams params =
                                    (ViewGroup.MarginLayoutParams)
                                            nav.getLayoutParams();

                            int bottomMargin =
                                    (int) (
                                            12
                                                    * getResources()
                                                    .getDisplayMetrics()
                                                    .density
                                    );

                            params.bottomMargin =
                                    systemBars.bottom
                                            + bottomMargin;

                            nav.setLayoutParams(params);
                        }

                        if (fabUpload != null) {

                            ViewGroup.MarginLayoutParams params =
                                    (ViewGroup.MarginLayoutParams)
                                            fabUpload.getLayoutParams();

                            int bottomMargin =
                                    (int) (
                                            84
                                                    * getResources()
                                                    .getDisplayMetrics()
                                                    .density
                                    );

                            params.bottomMargin =
                                    systemBars.bottom
                                            + bottomMargin;

                            fabUpload.setLayoutParams(params);
                        }

                        return insets;
                    }
            );
        }

        // Restore fragments after configuration/process recreation.
        FragmentManager fragmentManager =
                getSupportFragmentManager();

        if (savedInstanceState != null) {

            homeFragment =
                    (HomeFragment)
                            fragmentManager.findFragmentByTag(
                                    TAG_HOME
                            );

            filesFragment =
                    (FilesFragment)
                            fragmentManager.findFragmentByTag(
                                    TAG_FILES
                            );

            uploadFragment =
                    (UploadFragment)
                            fragmentManager.findFragmentByTag(
                                    TAG_UPLOAD
                            );

            settingsFragment =
                    (SettingsFragment)
                            fragmentManager.findFragmentByTag(
                                    TAG_SETTINGS
                            );

            currentTabIndex =
                    savedInstanceState.getInt(
                            KEY_CURRENT_TAB,
                            0
                    );
        }

        // Configure bottom navigation.
        PillNavHelper.setup(
                this,
                this::selectTab
        );

        if (savedInstanceState == null) {

            handleIntent(getIntent());

        } else {

            selectTab(currentTabIndex);
        }
    }

    @Override
    protected void onSaveInstanceState(
            @NonNull Bundle outState
    ) {

        outState.putInt(
                KEY_CURRENT_TAB,
                currentTabIndex
        );

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);

        setIntent(intent);

        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        if (intent != null
                && intent.getBooleanExtra(
                "EXTRA_OPEN_FILES_TAB",
                false
        )) {

            String category =
                    intent.getStringExtra(
                            "EXTRA_FILTER_CATEGORY"
                    );

            PillNavHelper.setTabVisualOnly(
                    this,
                    1
            );

            selectTab(1);

            if (category != null
                    && filesFragment != null) {

                filesFragment.applyCategoryFilter(
                        category
                );
            }

        } else {

            selectTab(0);
        }
    }

    private void selectTab(int index) {

        if (index < 0 || index > 3) {
            index = 0;
        }

        currentTabIndex = index;

        updateFabVisibility(index);

        Fragment targetFragment;
        String targetTag;

        switch (index) {

            case 0:

                if (homeFragment == null) {
                    homeFragment =
                            new HomeFragment();
                }

                targetFragment =
                        homeFragment;

                targetTag =
                        TAG_HOME;

                break;

            case 1:

                if (filesFragment == null) {
                    filesFragment =
                            new FilesFragment();
                }

                targetFragment =
                        filesFragment;

                targetTag =
                        TAG_FILES;

                break;

            case 2:

                if (uploadFragment == null) {
                    uploadFragment =
                            new UploadFragment();
                }

                targetFragment =
                        uploadFragment;

                targetTag =
                        TAG_UPLOAD;

                break;

            case 3:

            default:

                if (settingsFragment == null) {
                    settingsFragment =
                            new SettingsFragment();
                }

                targetFragment =
                        settingsFragment;

                targetTag =
                        TAG_SETTINGS;

                break;
        }

        FragmentManager fragmentManager =
                getSupportFragmentManager();

        FragmentTransaction transaction =
                fragmentManager.beginTransaction();

        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );

        // Hide other fragments.
        for (Fragment fragment :
                fragmentManager.getFragments()) {

            if (fragment != null
                    && fragment != targetFragment
                    && fragment.isAdded()) {

                transaction.hide(fragment);
            }
        }

        // Add or show selected fragment.
        if (!targetFragment.isAdded()) {

            transaction.add(
                    R.id.nav_host_container,
                    targetFragment,
                    targetTag
            );

        } else {

            transaction.show(targetFragment);
        }

        transaction.commit();

        currentFragment =
                targetFragment;
    }

    private void updateFabVisibility(int index) {

        if (fabUpload == null) {
            return;
        }

        if (index == 2) {

            // Upload screen already contains
            // its own upload controls.
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

            ((HomeFragment)
                    currentFragment)
                    .loadDashboardData();

        } else if (currentFragment instanceof FilesFragment) {

            ((FilesFragment)
                    currentFragment)
                    .loadFiles();
        }
    }

    public void navigateToTab(int index) {

        if (index < 0 || index > 3) {
            return;
        }

        PillNavHelper.selectTab(
                this,
                index
        );
    }

    public void navigateToCategory(String category) {

        PillNavHelper.setTabVisualOnly(
                this,
                1
        );

        selectTab(1);

        if (category != null
                && filesFragment != null) {

            filesFragment.applyCategoryFilter(
                    category
            );
        }
    }
}