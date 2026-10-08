package com.ciphervault.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Controller for metadata search bottom sheet.
 * Features:
 * 1. Single unified search input field matching indexed metadata without decrypting physical ciphertext.
 * 2. Dynamic metadata suggestions derived from actual indexed database occurrences (no hardcoded chips).
 * 3. Latest searches (max 10, newest first, deduplicated, persistent).
 * 4. Top 4 popular executed searches table with occurrence counts.
 */
public final class MetadataSearchBottomSheet {

    public static class FilterCriteria {
        public String query;
        public String cameraMake;
        public String cameraModel;
        public String resolution;
        public String codec;
        public String artist;
        public String author;
        public String genre;

        public boolean isEmpty() {
            return (query == null || query.trim().isEmpty())
                    && (cameraMake == null || cameraMake.trim().isEmpty())
                    && (cameraModel == null || cameraModel.trim().isEmpty())
                    && (resolution == null || resolution.trim().isEmpty())
                    && (codec == null || codec.trim().isEmpty())
                    && (artist == null || artist.trim().isEmpty())
                    && (author == null || author.trim().isEmpty())
                    && (genre == null || genre.trim().isEmpty());
        }

        public int getActiveCount() {
            int count = 0;
            if (query != null && !query.trim().isEmpty()) count++;
            if (cameraMake != null && !cameraMake.trim().isEmpty()) count++;
            if (cameraModel != null && !cameraModel.trim().isEmpty()) count++;
            if (resolution != null && !resolution.trim().isEmpty()) count++;
            if (codec != null && !codec.trim().isEmpty()) count++;
            if (artist != null && !artist.trim().isEmpty()) count++;
            if (author != null && !author.trim().isEmpty()) count++;
            if (genre != null && !genre.trim().isEmpty()) count++;
            return count;
        }

        public void clear() {
            query = null;
            cameraMake = null;
            cameraModel = null;
            resolution = null;
            codec = null;
            artist = null;
            author = null;
            genre = null;
        }

        public void copyFrom(FilterCriteria other) {
            if (other == null) {
                clear();
                return;
            }
            this.query = other.query;
            this.cameraMake = other.cameraMake;
            this.cameraModel = other.cameraModel;
            this.resolution = other.resolution;
            this.codec = other.codec;
            this.artist = other.artist;
            this.author = other.author;
            this.genre = other.genre;
        }
    }

    public interface OnFiltersAppliedListener {
        void onFiltersApplied(FilterCriteria criteria);
    }

    private MetadataSearchBottomSheet() {}

    public static void show(
            @NonNull Context context,
            @Nullable FilterCriteria initialCriteria,
            @NonNull OnFiltersAppliedListener listener) {

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(
                R.layout.bottom_sheet_metadata_search, null, false);
        dialog.setContentView(view);

        TextInputEditText etMetaQuery = view.findViewById(R.id.etMetaQuery);

        View layoutMetaSuggestions = view.findViewById(R.id.layoutMetaSuggestions);
        ChipGroup chipGroupMetaSuggestions = view.findViewById(R.id.chipGroupMetaSuggestions);
        View progressSuggestionsLoading = view.findViewById(R.id.progressSuggestionsLoading);

        View layoutLatestSearches = view.findViewById(R.id.layoutLatestSearches);
        ChipGroup chipGroupLatestSearches = view.findViewById(R.id.chipGroupLatestSearches);

        View layoutTopSearches = view.findViewById(R.id.layoutTopSearches);
        LinearLayout layoutTopSearchRows = view.findViewById(R.id.layoutTopSearchRows);

        Button btnClear = view.findViewById(R.id.btnClearFilters);
        Button btnCancel = view.findViewById(R.id.btnCancelFilters);
        Button btnApply = view.findViewById(R.id.btnApplyFilters);

        // Populate initial query
        if (initialCriteria != null && initialCriteria.query != null && etMetaQuery != null) {
            etMetaQuery.setText(initialCriteria.query);
            etMetaQuery.setSelection(initialCriteria.query.length());
        }

        // 1. DYNAMIC METADATA SUGGESTIONS from backend catalog
        if (chipGroupMetaSuggestions != null) {
            chipGroupMetaSuggestions.removeAllViews();
            if (progressSuggestionsLoading != null) progressSuggestionsLoading.setVisibility(View.VISIBLE);
            ApiClient.getApiService(context).getSuggestions("").enqueue(new Callback<List<String>>() {
                @Override
                public void onResponse(@NonNull Call<List<String>> call, @NonNull Response<List<String>> response) {
                    if (progressSuggestionsLoading != null) progressSuggestionsLoading.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        if (layoutMetaSuggestions != null) layoutMetaSuggestions.setVisibility(View.VISIBLE);
                        chipGroupMetaSuggestions.removeAllViews();
                        for (String sugg : response.body()) {
                            if (sugg == null || sugg.trim().isEmpty()) continue;
                            Chip chip = new Chip(context);
                            chip.setText(sugg.trim());
                            chip.setCheckable(false);
                            chip.setClickable(true);
                            chip.setOnClickListener(v -> {
                                if (etMetaQuery != null) {
                                    etMetaQuery.setText(sugg.trim());
                                }
                                executeSearch(context, sugg.trim(), listener, dialog);
                            });
                            chipGroupMetaSuggestions.addView(chip);
                        }
                    } else {
                        if (layoutMetaSuggestions != null) layoutMetaSuggestions.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<String>> call, @NonNull Throwable t) {
                    if (progressSuggestionsLoading != null) progressSuggestionsLoading.setVisibility(View.GONE);
                    if (layoutMetaSuggestions != null) layoutMetaSuggestions.setVisibility(View.GONE);
                }
            });
        }

        // 2. LATEST SEARCHES (Max 10, newest first, deduplicated)
        List<String> latestSearches = SearchHistoryManager.getLatestSearches(context);
        if (latestSearches.isEmpty()) {
            if (layoutLatestSearches != null) layoutLatestSearches.setVisibility(View.GONE);
        } else {
            if (layoutLatestSearches != null) layoutLatestSearches.setVisibility(View.VISIBLE);
            if (chipGroupLatestSearches != null) {
                chipGroupLatestSearches.removeAllViews();
                for (String query : latestSearches) {
                    Chip chip = new Chip(context);
                    chip.setText(query);
                    chip.setCheckable(false);
                    chip.setClickable(true);
                    chip.setChipIconResource(R.drawable.ic_lucide_clock);
                    chip.setChipIconSize(36f);
                    chip.setChipIconTint(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cv_primary)));
                    chip.setOnClickListener(v -> {
                        if (etMetaQuery != null) {
                            etMetaQuery.setText(query);
                        }
                        executeSearch(context, query, listener, dialog);
                    });
                    chipGroupLatestSearches.addView(chip);
                }
            }
        }

        // 3. TOP 4 POPULAR SEARCHES TABLE
        List<SearchHistoryManager.SearchCountItem> topSearches = SearchHistoryManager.getTopMostSeenSearches(context, 4);
        if (topSearches.isEmpty()) {
            if (layoutTopSearches != null) layoutTopSearches.setVisibility(View.GONE);
        } else {
            if (layoutTopSearches != null) layoutTopSearches.setVisibility(View.VISIBLE);
            if (layoutTopSearchRows != null) {
                layoutTopSearchRows.removeAllViews();
                float density = context.getResources().getDisplayMetrics().density;
                int padV = (int) (6 * density);
                int padH = (int) (8 * density);

                for (SearchHistoryManager.SearchCountItem item : topSearches) {
                    LinearLayout row = new LinearLayout(context);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(padH, padV, padH, padV);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setClickable(true);
                    row.setFocusable(true);
                    row.setBackgroundResource(android.R.drawable.list_selector_background);

                    TextView tvQuery = new TextView(context);
                    LinearLayout.LayoutParams lpQuery = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                    tvQuery.setLayoutParams(lpQuery);
                    tvQuery.setText(item.query);
                    tvQuery.setTextSize(13f);
                    tvQuery.setTextColor(ContextCompat.getColor(context, R.color.cv_primary));

                    TextView tvCount = new TextView(context);
                    tvCount.setText(String.valueOf(item.count));
                    tvCount.setTextSize(12f);
                    tvCount.setPadding((int) (8 * density), (int) (2 * density), (int) (8 * density), (int) (2 * density));
                    tvCount.setTextColor(ContextCompat.getColor(context, R.color.cv_text_secondary));
                    tvCount.setBackgroundResource(R.drawable.bg_tag_pill);

                    row.addView(tvQuery);
                    row.addView(tvCount);

                    row.setOnClickListener(v -> {
                        if (etMetaQuery != null) {
                            etMetaQuery.setText(item.query);
                        }
                        executeSearch(context, item.query, listener, dialog);
                    });

                    layoutTopSearchRows.addView(row);
                }
            }
        }

        // Keyboard search action
        if (etMetaQuery != null) {
            etMetaQuery.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String q = etMetaQuery.getText() != null ? etMetaQuery.getText().toString().trim() : "";
                    executeSearch(context, q, listener, dialog);
                    return true;
                }
                return false;
            });
        }

        // Clear All
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                if (etMetaQuery != null) etMetaQuery.setText("");
                FilterCriteria empty = new FilterCriteria();
                listener.onFiltersApplied(empty);
                dialog.dismiss();
            });
        }

        // Cancel
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        // Apply / Search
        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                String q = (etMetaQuery != null && etMetaQuery.getText() != null)
                        ? etMetaQuery.getText().toString().trim() : "";
                executeSearch(context, q, listener, dialog);
            });
        }

        dialog.show();
    }

    private static void executeSearch(
            @NonNull Context context,
            @NonNull String query,
            @NonNull OnFiltersAppliedListener listener,
            @NonNull BottomSheetDialog dialog) {

        FilterCriteria criteria = new FilterCriteria();
        criteria.query = query;

        if (!query.isEmpty()) {
            SearchHistoryManager.recordSearch(context, query);
            AuditLogger.log(context, "Search Executed", "SUCCESS", "Metadata search query: " + query);
        }

        listener.onFiltersApplied(criteria);
        dialog.dismiss();
    }
}
