package com.ciphervault.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

/**
 * About CipherVault Activity.
 * Provides complete project details, author information, GitHub repository access,
 * and deep technical documentation regarding AES-256-GCM, SHA-256 integrity,
 * forensic ExifTool metadata extraction, Spring Boot / MySQL infrastructure,
 * and the system sans-serif / Lucide Icons (Outlined) design system.
 */
public class AboutActivity extends BaseActivity {

    private static final String GITHUB_REPO_URL = "https://github.com/akhilkvu6/CipherVault";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        MaterialToolbar toolbar = findViewById(R.id.toolbarAbout);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        MaterialButton btnOpenGitHub = findViewById(R.id.btnOpenGitHub);
        if (btnOpenGitHub != null) {
            btnOpenGitHub.setOnClickListener(v -> {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL));
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to open repository link: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
