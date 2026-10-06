package com.ciphervault.app.onboarding;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ciphervault.app.R;
import com.ciphervault.app.onboarding.animations.BaseAnimationView;
import com.ciphervault.app.onboarding.animations.EncryptionAnimation;
import com.ciphervault.app.onboarding.animations.FilesAnimation;
import com.ciphervault.app.onboarding.animations.IntegrityAnimation;
import com.ciphervault.app.onboarding.animations.StorageAnimation;
import com.ciphervault.app.onboarding.animations.VaultAnimation;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.PageViewHolder> {

    private final Context context;
    private final List<OnboardingPageData> pages;

    public OnboardingAdapter(Context context, List<OnboardingPageData> pages) {
        this.context = context;
        this.pages = pages;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_onboarding_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        OnboardingPageData data = pages.get(position);
        holder.bind(data, position);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout visualContainer;
        private final TextView tvBadge;
        private final TextView tvTitle;
        private final TextView tvDescription;
        private final LinearLayout pillsContainer;
        private final LinearLayout expandableCard;
        private final TextView tvExpandableTitle;
        private final TextView tvExpandableContent;
        private final ImageView ivExpandIcon;

        private boolean isExpanded = false;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            visualContainer = itemView.findViewById(R.id.visualContainer);
            tvBadge = itemView.findViewById(R.id.tvBadge);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            pillsContainer = itemView.findViewById(R.id.pillsContainer);
            expandableCard = itemView.findViewById(R.id.expandableCard);
            tvExpandableTitle = itemView.findViewById(R.id.tvExpandableTitle);
            tvExpandableContent = itemView.findViewById(R.id.tvExpandableContent);
            ivExpandIcon = itemView.findViewById(R.id.ivExpandIcon);
        }

        void bind(OnboardingPageData data, int position) {
            Context ctx = itemView.getContext();

            tvBadge.setText(data.getBadgeTextResId());
            tvTitle.setText(data.getTitleResId());
            tvDescription.setText(data.getDescriptionResId());

            // Build Animation View
            visualContainer.removeAllViews();
            BaseAnimationView animView = createAnimationViewForPage(ctx, position);
            if (animView != null) {
                visualContainer.addView(animView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));
            }

            // Supporting concept pills (Screen 1)
            pillsContainer.removeAllViews();
            if (data.hasPills()) {
                pillsContainer.setVisibility(View.VISIBLE);
                for (int pillTextRes : data.getPillTextResIds()) {
                    TextView pillView = new TextView(ctx);
                    pillView.setText(pillTextRes);
                    pillView.setTextSize(12f);
                    pillView.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.cv_text_secondary));
                    pillView.setBackgroundResource(R.drawable.bg_tag_pill);
                    int padH = (int) (12 * ctx.getResources().getDisplayMetrics().density);
                    int padV = (int) (6 * ctx.getResources().getDisplayMetrics().density);
                    pillView.setPadding(padH, padV, padH, padV);

                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    lp.rightMargin = (int) (8 * ctx.getResources().getDisplayMetrics().density);
                    pillView.setLayoutParams(lp);
                    pillsContainer.addView(pillView);
                }
            } else {
                pillsContainer.setVisibility(View.GONE);
            }

            // Expandable section (Screens 2 & 3)
            if (data.hasExpandable()) {
                expandableCard.setVisibility(View.VISIBLE);
                tvExpandableTitle.setText(data.getExpandableTitleResId());
                tvExpandableContent.setText(data.getExpandableContentResId());
                isExpanded = false;
                tvExpandableContent.setVisibility(View.GONE);
                ivExpandIcon.setRotation(0f);

                expandableCard.setOnClickListener(v -> {
                    isExpanded = !isExpanded;
                    tvExpandableContent.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                    float targetRotation = isExpanded ? 180f : 0f;
                    ObjectAnimator.ofFloat(ivExpandIcon, "rotation", ivExpandIcon.getRotation(), targetRotation)
                            .setDuration(200)
                            .start();
                });
            } else {
                expandableCard.setVisibility(View.GONE);
            }
        }

        private BaseAnimationView createAnimationViewForPage(Context context, int position) {
            switch (position) {
                case 0:
                    return new VaultAnimation(context);
                case 1:
                    return new EncryptionAnimation(context);
                case 2:
                    return new IntegrityAnimation(context);
                case 3:
                    return new FilesAnimation(context);
                case 4:
                    return new StorageAnimation(context);
                default:
                    return null;
            }
        }
    }
}
