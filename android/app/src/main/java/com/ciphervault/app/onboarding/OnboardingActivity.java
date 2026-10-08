package com.ciphervault.app.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.ciphervault.app.BaseActivity;
import com.ciphervault.app.MainActivity;
import com.ciphervault.app.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class OnboardingActivity extends BaseActivity {

    private ViewPager2 viewPager;
    private OnboardingPageIndicator pageIndicator;
    private TextView tvPageCounter;
    private MaterialButton btnSkip;
    private MaterialButton btnPrimaryAction;

    private OnboardingPreferences preferences;
    private List<OnboardingPageData> pages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboarding);

        preferences = new OnboardingPreferences(this);
        pages = OnboardingPage.getPages();

        initViews();
        setupInsets();
        setupViewPager();
        setupListeners();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        pageIndicator = findViewById(R.id.pageIndicator);
        tvPageCounter = findViewById(R.id.tvPageCounter);
        btnSkip = findViewById(R.id.btnSkip);
        btnPrimaryAction = findViewById(R.id.btnPrimaryAction);

        pageIndicator.setPageCount(pages.size());
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.onboardingRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            updateUiForPage(position);
        }
    };

    private void setupViewPager() {
        OnboardingAdapter adapter = new OnboardingAdapter(this, pages);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setPageTransformer(new OnboardingPageTransformer());

        viewPager.registerOnPageChangeCallback(pageChangeCallback);

        updateUiForPage(0);
    }

    @Override
    protected void onDestroy() {
        if (viewPager != null) {
            viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        }
        super.onDestroy();
    }

    private void setupListeners() {
        btnSkip.setOnClickListener(v -> finishOnboarding());

        btnPrimaryAction.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < pages.size() - 1) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                finishOnboarding();
            }
        });
    }

    private void updateUiForPage(int position) {
        pageIndicator.setCurrentPage(position, true);
        tvPageCounter.setText(String.format(Locale.getDefault(), getString(R.string.onboarding_page_indicator_format), position + 1, pages.size()));

        boolean isLastPage = (position == pages.size() - 1);
        if (btnSkip != null) {
            btnSkip.setVisibility(isLastPage ? View.INVISIBLE : View.VISIBLE);
        }
        if (isLastPage) {
            btnPrimaryAction.setText(R.string.action_get_started);
            btnPrimaryAction.setContentDescription(getString(R.string.cd_btn_get_started));
        } else {
            btnPrimaryAction.setText(R.string.action_continue);
            btnPrimaryAction.setContentDescription(getString(R.string.cd_btn_continue));
        }
    }

    private void finishOnboarding() {
        preferences.setOnboardingCompleted(true);

        Intent intent = new Intent(this, com.ciphervault.app.ConnectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        finish();
    }
}
