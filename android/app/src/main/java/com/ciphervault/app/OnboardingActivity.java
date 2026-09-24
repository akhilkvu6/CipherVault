package com.ciphervault.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OnboardingActivity extends BaseActivity {

    private ViewPager2 vpOnboarding;
    private Button btnSkip;
    private Button btnNext;
    private TextView tvPageIndicator;
    private SessionManager sessionManager;

    private static class OnboardingPage {
        final int titleRes;
        final int descRes;

        OnboardingPage(int titleRes, int descRes) {
            this.titleRes = titleRes;
            this.descRes = descRes;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        View rootView = getLayoutInflater().inflate(R.layout.activity_onboarding, null);
        setContentView(rootView);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);

        vpOnboarding = findViewById(R.id.vpOnboarding);
        btnSkip = findViewById(R.id.btnSkip);
        btnNext = findViewById(R.id.btnNext);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);

        List<OnboardingPage> pages = new ArrayList<>();
        pages.add(new OnboardingPage(R.string.onboarding_p1_title, R.string.onboarding_p1_desc));
        pages.add(new OnboardingPage(R.string.onboarding_p2_title, R.string.onboarding_p2_desc));
        pages.add(new OnboardingPage(R.string.onboarding_p3_title, R.string.onboarding_p3_desc));
        pages.add(new OnboardingPage(R.string.onboarding_p4_title, R.string.onboarding_p4_desc));

        OnboardingAdapter adapter = new OnboardingAdapter(pages);
        vpOnboarding.setAdapter(adapter);

        vpOnboarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updatePageUI(position, pages.size());
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());
        btnNext.setOnClickListener(v -> {
            int current = vpOnboarding.getCurrentItem();
            if (current < pages.size() - 1) {
                vpOnboarding.setCurrentItem(current + 1, true);
            } else {
                finishOnboarding();
            }
        });

        updatePageUI(0, pages.size());
    }

    private void updatePageUI(int position, int totalPages) {
        tvPageIndicator.setText(String.format(Locale.US, "%d / %d", position + 1, totalPages));
        if (position == totalPages - 1) {
            btnNext.setText(R.string.btn_get_started);
            btnSkip.setVisibility(View.INVISIBLE);
        } else {
            btnNext.setText(R.string.btn_next);
            btnSkip.setVisibility(View.VISIBLE);
        }
    }

    private void finishOnboarding() {
        sessionManager.setOnboardingCompleted(true);
        Intent intent = new Intent(OnboardingActivity.this, ConnectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.ViewHolder> {

        private final List<OnboardingPage> pages;

        OnboardingAdapter(List<OnboardingPage> pages) {
            this.pages = pages;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_page, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OnboardingPage page = pages.get(position);
            holder.tvTitle.setText(page.titleRes);
            holder.tvDescription.setText(page.descRes);
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvTitle;
            final TextView tvDescription;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvOnboardingTitle);
                tvDescription = itemView.findViewById(R.id.tvOnboardingDescription);
            }
        }
    }
}