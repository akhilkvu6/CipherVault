package com.ciphervault.app;

import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

public class MainActivity extends BaseActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, ConnectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

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
            return insets;
        });

        PillNavHelper.setup(this, index -> {
            switch (index) {
                case 0:
                    loadFragment(new HomeFragment());
                    break;
                case 1:
                    loadFragment(new FilesFragment());
                    break;
                case 2:
                    loadFragment(new UploadFragment());
                    break;
                case 3:
                    loadFragment(new SettingsFragment());
                    break;
            }
        });

        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
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
            loadFragment(FilesFragment.newInstance(category));
        } else {
            loadFragment(new HomeFragment());
        }
    }

    public void navigateToTab(int index) {
        PillNavHelper.selectTab(this, index);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.nav_host_container, fragment)
                .commit();
    }
}