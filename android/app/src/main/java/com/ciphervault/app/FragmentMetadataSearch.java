package com.ciphervault.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ciphervault.app.transfer.TransferManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Unified single-screen dynamic cascading metadata search destination for CipherVault.
 * Recreates the reference design hierarchy:
 * - Header & Subtitle
 * - Rounded search input
 * - Single-selection File Type chips
 * - Dynamically populated cascading metadata facet rows (Camera Make, Camera Model, Resolution, Date Taken, Lens, etc.)
 * - Inline "+ More" / "Show less" chip expansion without extra screens
 * - Active filters bar with dismissible chips and "Clear all"
 * - Results count and sort menu
 * - File results list reusing FilesAdapter, thumbnails, download, and delete actions
 */
public class FragmentMetadataSearch extends Fragment implements FilesAdapter.OnDownloadClickListener, FilesAdapter.OnDeleteClickListener {

    private ApiService apiService;
    private Call<FileFacetsResponse> activeFacetsCall;
    private Call<List<StoredFile>> activeSearchCall;
    private long currentRequestId = 0;

    // Search and Filter State
    private String selectedFileType = "ALL";
    private String metadataQuery = "";
    private final Map<String, Set<String>> selectedFilters = new LinkedHashMap<>();
    private final Map<String, Boolean> expandedFacets = new HashMap<>();

    // Sorting and Results State
    private FileSortOption currentSortOption = FileSortOption.DATE_NEW_OLD;
    private final List<StoredFile> activeResults = new ArrayList<>();
    private List<FileFacetsResponse.FacetField> lastFacetFields = new ArrayList<>();

    // Debounce Handler for Search Input
    private final Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private static final long SEARCH_DEBOUNCE_MS = 300L;

    // Views
    private NestedScrollView metadataScrollView;
    private TextView tvMetadataTitle;
    private TextView tvMetadataSubtitle;
    private TextInputLayout metadataSearchLayout;
    private TextInputEditText etMetadataSearch;

    private TextView tvFileTypeLabel;
    private ChipGroup chipsFileType;
    private LinearLayout dynamicFacetContainer;
    private TextView tvFacetStatus;

    private LinearLayout activeFiltersHeader;
    private TextView tvActiveFiltersLabel;
    private MaterialButton btnClearAllFilters;
    private ChipGroup chipsActiveFilters;

    private LinearLayout resultsHeader;
    private TextView tvResultCount;
    private MaterialButton btnSortResults;

    private FrameLayout metadataResultsContainer;
    private RecyclerView rvMetadataResults;
    private ProgressBar metadataProgress;
    private TextView tvMetadataEmptyState;
    private LinearLayout layoutMetadataSearchError;
    private TextView tvMetadataErrorState;
    private MaterialButton btnRetrySearch;

    private FilesAdapter filesAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_metadata_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = requireContext();
        apiService = ApiClient.getApiService(context);

        initViews(view);
        setupRecyclerView();
        setupSearchInput();
        setupSortButton();

        Bundle args = getArguments();
        if (args != null && args.containsKey("query")) {
            String initialQuery = args.getString("query");
            if (initialQuery != null && !initialQuery.trim().isEmpty()) {
                metadataQuery = initialQuery.trim();
                if (etMetadataSearch != null) {
                    etMetadataSearch.setText(metadataQuery);
                }
            }
        }

        refreshFacetsAndResults();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchDebounceHandler.removeCallbacksAndMessages(null);
        cancelCalls();
    }

    private void cancelCalls() {
        if (activeFacetsCall != null) {
            activeFacetsCall.cancel();
            activeFacetsCall = null;
        }
        if (activeSearchCall != null) {
            activeSearchCall.cancel();
            activeSearchCall = null;
        }
    }

    private void initViews(View view) {
        metadataScrollView = view.findViewById(R.id.metadataScrollView);
        tvMetadataTitle = view.findViewById(R.id.tvMetadataTitle);
        tvMetadataSubtitle = view.findViewById(R.id.tvMetadataSubtitle);
        metadataSearchLayout = view.findViewById(R.id.metadataSearchLayout);
        etMetadataSearch = view.findViewById(R.id.etMetadataSearch);

        tvFileTypeLabel = view.findViewById(R.id.tvFileTypeLabel);
        chipsFileType = view.findViewById(R.id.chipsFileType);
        dynamicFacetContainer = view.findViewById(R.id.dynamicFacetContainer);
        tvFacetStatus = view.findViewById(R.id.tvFacetStatus);

        activeFiltersHeader = view.findViewById(R.id.activeFiltersHeader);
        tvActiveFiltersLabel = view.findViewById(R.id.tvActiveFiltersLabel);
        btnClearAllFilters = view.findViewById(R.id.btnClearAllFilters);
        chipsActiveFilters = view.findViewById(R.id.chipsActiveFilters);

        resultsHeader = view.findViewById(R.id.resultsHeader);
        tvResultCount = view.findViewById(R.id.tvResultCount);
        btnSortResults = view.findViewById(R.id.btnSortResults);

        metadataResultsContainer = view.findViewById(R.id.metadataResultsContainer);
        rvMetadataResults = view.findViewById(R.id.rvMetadataResults);
        metadataProgress = view.findViewById(R.id.metadataProgress);
        tvMetadataEmptyState = view.findViewById(R.id.tvMetadataEmptyState);
        layoutMetadataSearchError = view.findViewById(R.id.layoutMetadataSearchError);
        tvMetadataErrorState = view.findViewById(R.id.tvMetadataErrorState);
        btnRetrySearch = view.findViewById(R.id.btnRetrySearch);

        if (btnClearAllFilters != null) {
            btnClearAllFilters.setOnClickListener(v -> onClearAllFilters());
        }
        if (btnRetrySearch != null) {
            btnRetrySearch.setOnClickListener(v -> refreshFacetsAndResults());
        }
    }

    private void setupRecyclerView() {
        if (rvMetadataResults != null && getContext() != null) {
            rvMetadataResults.setLayoutManager(new LinearLayoutManager(requireContext()));
            filesAdapter = new FilesAdapter(this, this);
            rvMetadataResults.setAdapter(filesAdapter);
            rvMetadataResults.setNestedScrollingEnabled(false);
        }
    }

    private void setupSearchInput() {
        if (etMetadataSearch == null) return;

        etMetadataSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                searchDebounceHandler.removeCallbacksAndMessages(null);
                String q = etMetadataSearch.getText() != null ? etMetadataSearch.getText().toString().trim() : "";
                if (!Objects.equals(q, metadataQuery)) {
                    metadataQuery = q;
                    refreshFacetsAndResults();
                }
                return true;
            }
            return false;
        });

        etMetadataSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                searchDebounceHandler.removeCallbacksAndMessages(null);
                searchDebounceHandler.postDelayed(() -> {
                    if (!isAdded()) return;
                    String q = s != null ? s.toString().trim() : "";
                    if (!Objects.equals(q, metadataQuery)) {
                        metadataQuery = q;
                        refreshFacetsAndResults();
                    }
                }, SEARCH_DEBOUNCE_MS);
            }
        });
    }

    private void setupSortButton() {
        if (btnSortResults == null) return;

        btnSortResults.setOnClickListener(v -> {
            String[] options = new String[]{
                    "Date (Newest first)",
                    "Date (Oldest first)",
                    "Name (A to Z)",
                    "Name (Z to A)",
                    "Size (Largest first)",
                    "Size (Smallest first)"
            };

            int checkedItem = 0;
            switch (currentSortOption) {
                case DATE_NEW_OLD: checkedItem = 0; break;
                case DATE_OLD_NEW: checkedItem = 1; break;
                case NAME_ASC: checkedItem = 2; break;
                case NAME_DESC: checkedItem = 3; break;
                case SIZE_HIGH_LOW: checkedItem = 4; break;
                case SIZE_LOW_HIGH: checkedItem = 5; break;
            }

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Sort Files By")
                    .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                        switch (which) {
                            case 0: currentSortOption = FileSortOption.DATE_NEW_OLD; break;
                            case 1: currentSortOption = FileSortOption.DATE_OLD_NEW; break;
                            case 2: currentSortOption = FileSortOption.NAME_ASC; break;
                            case 3: currentSortOption = FileSortOption.NAME_DESC; break;
                            case 4: currentSortOption = FileSortOption.SIZE_HIGH_LOW; break;
                            case 5: currentSortOption = FileSortOption.SIZE_LOW_HIGH; break;
                        }
                        applySortAndDisplay();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    public void executeSearch(String query) {
        this.metadataQuery = query != null ? query.trim() : "";
        if (etMetadataSearch != null) {
            etMetadataSearch.setText(this.metadataQuery);
        }
        refreshFacetsAndResults();
    }

    // --- Selection Handlers ---

    private void onFileTypeSelected(String fileType) {
        String newType = (fileType == null || fileType.trim().isEmpty()) ? "ALL" : fileType.trim().toUpperCase(Locale.ROOT);
        if (Objects.equals(selectedFileType, newType)) {
            return;
        }

        selectedFileType = newType;
        // A file-type change invalidates incompatible metadata selections
        selectedFilters.clear();
        refreshFacetsAndResults();
    }

    private void onFacetValueSelected(String field, String value, boolean selected) {
        Set<String> values = selectedFilters.computeIfAbsent(
                field,
                ignored -> new LinkedHashSet<>()
        );

        if (selected) {
            values.add(value);
        } else {
            values.remove(value);
        }

        if (values.isEmpty()) {
            selectedFilters.remove(field);
        }

        // Cascading invalidations: if cameraMake changes, remove cameraModel and lens
        if ("cameraMake".equalsIgnoreCase(field)) {
            selectedFilters.remove("cameraModel");
            selectedFilters.remove("lens");
        }

        refreshFacetsAndResults();
    }

    private void onClearAllFilters() {
        selectedFilters.clear();
        refreshFacetsAndResults();
    }

    // --- Main Facet and Result Refresh Pipeline ---

    private void refreshFacetsAndResults() {
        if (!isAdded()) return;

        final long requestId = ++currentRequestId;
        cancelCalls();

        if (metadataProgress != null) {
            metadataProgress.setVisibility(View.VISIBLE);
        }
        if (layoutMetadataSearchError != null) {
            layoutMetadataSearchError.setVisibility(View.GONE);
        }

        renderActiveFiltersBar();

        String catParam = "ALL".equalsIgnoreCase(selectedFileType) ? null : selectedFileType;
        String queryParam = metadataQuery.isEmpty() ? null : metadataQuery;

        String makeParam = joinFilterValues("cameraMake");
        String modelParam = joinFilterValues("cameraModel");
        String resParam = joinFilterValues("resolution");
        String codecParam = joinFilterValues("codec");
        String artistParam = joinFilterValues("artist");
        String authorParam = joinFilterValues("author");
        String genreParam = joinFilterValues("genre");
        String lensParam = joinFilterValues("lens");
        String dateTakenParam = joinFilterValues("dateTaken");

        activeFacetsCall = apiService.getFacets(
                queryParam,
                catParam,
                makeParam,
                modelParam,
                resParam,
                codecParam,
                artistParam,
                authorParam,
                genreParam,
                lensParam,
                null,
                null,
                dateTakenParam
        );

        activeFacetsCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<FileFacetsResponse> call, @NonNull Response<FileFacetsResponse> response) {
                if (!isAdded() || requestId != currentRequestId) return;

                if (response.isSuccessful() && response.body() != null) {
                    FileFacetsResponse facetsResp = response.body();
                    List<FileFacetsResponse.FacetField> returnedFacets = facetsResp.getFacets();
                    lastFacetFields = returnedFacets != null ? returnedFacets : new ArrayList<>();

                    // Reconcile only against a successful, actual facets response.
                    reconcileSelections(lastFacetFields);

                    // Keep file-type choices visible even if the backend omits category facets.
                    renderFileTypeChips(lastFacetFields);
                    renderDynamicFacetRows(lastFacetFields);
                    updateFacetStatus(lastFacetFields, null);
                    renderActiveFiltersBar();
                } else {
                    // Previously this branch skipped both chip renderers, leaving an empty
                    // File Type section while results could still appear.
                    lastFacetFields = new ArrayList<>();
                    renderFileTypeChips(lastFacetFields);
                    renderDynamicFacetRows(lastFacetFields);
                    updateFacetStatus(null, "Metadata filters could not be loaded. Retry to load available filters.");
                    renderActiveFiltersBar();
                }

                performFileSearch(requestId, queryParam, catParam, makeParam, modelParam, resParam,
                        codecParam, artistParam, authorParam, genreParam, lensParam, dateTakenParam);
            }

            @Override
            public void onFailure(@NonNull Call<FileFacetsResponse> call, @NonNull Throwable t) {
                if (!isAdded() || requestId != currentRequestId || call.isCanceled()) return;

                // Preserve usable file-type filtering, but never invent metadata values.
                lastFacetFields = new ArrayList<>();
                renderFileTypeChips(lastFacetFields);
                renderDynamicFacetRows(lastFacetFields);
                updateFacetStatus(null, "Metadata filters are unavailable. Check the connection and retry.");
                renderActiveFiltersBar();

                // Results may still load, but facet failure is shown explicitly.
                performFileSearch(requestId, queryParam, catParam, makeParam, modelParam, resParam,
                        codecParam, artistParam, authorParam, genreParam, lensParam, dateTakenParam);
            }
        });
    }

    private void performFileSearch(
            long requestId,
            String queryParam,
            String catParam,
            String makeParam,
            String modelParam,
            String resParam,
            String codecParam,
            String artistParam,
            String authorParam,
            String genreParam,
            String lensParam,
            String dateTakenParam) {

        activeSearchCall = apiService.searchFiles(
                queryParam,
                catParam,
                makeParam,
                modelParam,
                resParam,
                codecParam,
                artistParam,
                authorParam,
                genreParam,
                lensParam,
                null,
                null,
                dateTakenParam
        );

        activeSearchCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<StoredFile>> call, @NonNull Response<List<StoredFile>> response) {
                if (!isAdded() || requestId != currentRequestId) return;

                if (metadataProgress != null) {
                    metadataProgress.setVisibility(View.GONE);
                }

                if (response.isSuccessful() && response.body() != null) {
                    activeResults.clear();
                    activeResults.addAll(response.body());
                    if (layoutMetadataSearchError != null) layoutMetadataSearchError.setVisibility(View.GONE);
                    applySortAndDisplay();
                } else {
                    showErrorState();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StoredFile>> call, @NonNull Throwable t) {
                if (!isAdded() || requestId != currentRequestId || call.isCanceled()) return;

                if (metadataProgress != null) {
                    metadataProgress.setVisibility(View.GONE);
                }
                showErrorState();
            }
        });
    }

    private String joinFilterValues(String field) {
        Set<String> set = selectedFilters.get(field);
        if (set == null || set.isEmpty()) return null;
        return TextUtils.join(",", set);
    }

    /**
     * Reconciles existing selections against returned available facet values.
     * Prunes selections that are no longer valid under new upstream filters.
     */
    private void reconcileSelections(List<FileFacetsResponse.FacetField> facetFields) {
        if (facetFields == null || selectedFilters.isEmpty()) return;

        Map<String, Set<String>> validValuesByField = new HashMap<>();
        for (FileFacetsResponse.FacetField facet : facetFields) {
            Set<String> vals = new HashSet<>();
            for (FileFacetsResponse.FacetValue val : facet.getValues()) {
                if (val.getValue() != null) {
                    vals.add(val.getValue().toLowerCase(Locale.ROOT));
                }
            }
            validValuesByField.put(facet.getField().toLowerCase(Locale.ROOT), vals);
        }

        List<String> fieldsToRemove = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : selectedFilters.entrySet()) {
            String fieldKey = entry.getKey().toLowerCase(Locale.ROOT);
            Set<String> availableSet = validValuesByField.get(fieldKey);

            if (availableSet == null) {
                // Facet is no longer available in this query context
                fieldsToRemove.add(entry.getKey());
            } else {
                // Remove values that are no longer present in available options
                entry.getValue().removeIf(val -> !availableSet.contains(val.toLowerCase(Locale.ROOT)));
                if (entry.getValue().isEmpty()) {
                    fieldsToRemove.add(entry.getKey());
                }
            }
        }

        for (String rem : fieldsToRemove) {
            selectedFilters.remove(rem);
        }
    }

    // --- Facet Rendering ---

    private void updateFacetStatus(
            @Nullable List<FileFacetsResponse.FacetField> facetFields,
            @Nullable String errorMessage) {
        if (tvFacetStatus == null) return;

        if (errorMessage != null) {
            tvFacetStatus.setText(errorMessage);
            tvFacetStatus.setVisibility(View.VISIBLE);
            return;
        }

        boolean hasMetadataOptions = false;
        if (facetFields != null) {
            for (FileFacetsResponse.FacetField facet : facetFields) {
                if (facet == null || "category".equalsIgnoreCase(facet.getField())) continue;
                if (facet.isAvailable() && facet.getValues() != null && !facet.getValues().isEmpty()) {
                    hasMetadataOptions = true;
                    break;
                }
            }
        }

        if (hasMetadataOptions) {
            tvFacetStatus.setText("");
            tvFacetStatus.setVisibility(View.GONE);
        } else {
            tvFacetStatus.setText("No metadata filters are available for this selection yet.");
            tvFacetStatus.setVisibility(View.VISIBLE);
        }
    }

    private void renderFileTypeChips(List<FileFacetsResponse.FacetField> facetFields) {
        if (chipsFileType == null || getContext() == null) return;
        Context context = requireContext();

        chipsFileType.removeAllViews();

        FileFacetsResponse.FacetField catFacet = null;
        if (facetFields != null) {
            for (FileFacetsResponse.FacetField f : facetFields) {
                if ("category".equalsIgnoreCase(f.getField())) {
                    catFacet = f;
                    break;
                }
            }
        }

        List<FileFacetsResponse.FacetValue> typeValues = new ArrayList<>();
        if (catFacet != null && catFacet.getValues() != null && !catFacet.getValues().isEmpty()) {
            typeValues.addAll(catFacet.getValues());
        } else {
            typeValues.add(new FileFacetsResponse.FacetValue("ALL", "All", 0));
            typeValues.add(new FileFacetsResponse.FacetValue("IMAGES", "Photos", 0));
            typeValues.add(new FileFacetsResponse.FacetValue("VIDEOS", "Videos", 0));
            typeValues.add(new FileFacetsResponse.FacetValue("DOCUMENTS", "Documents", 0));
            typeValues.add(new FileFacetsResponse.FacetValue("AUDIO", "Audio", 0));
        }

        for (FileFacetsResponse.FacetValue val : typeValues) {
            boolean isChecked = selectedFileType.equalsIgnoreCase(val.getValue())
                    || ("ALL".equalsIgnoreCase(selectedFileType) && "ALL".equalsIgnoreCase(val.getValue()));

            Chip chip = createFilterChip(context, chipsFileType, val.getLabel(), val.getCount() > 0 ? val.getCount() : -1, isChecked);
            chip.setOnClickListener(v -> {
                if (isChecked && !"ALL".equalsIgnoreCase(val.getValue())) {
                    onFileTypeSelected("ALL");
                } else {
                    onFileTypeSelected(val.getValue());
                }
            });
            chipsFileType.addView(chip);
        }
    }

    private void renderDynamicFacetRows(List<FileFacetsResponse.FacetField> facetFields) {
        if (dynamicFacetContainer == null || getContext() == null) return;
        dynamicFacetContainer.removeAllViews();

        if (facetFields == null) return;

        for (FileFacetsResponse.FacetField facet : facetFields) {
            if (facet == null || "category".equalsIgnoreCase(facet.getField())) {
                continue; // Rendered separately in chipsFileType
            }
            if (!facet.isAvailable()) {
                continue;
            }

            List<FileFacetsResponse.FacetValue> values = facet.getValues();
            if (values == null || values.isEmpty()) {
                continue;
            }

            renderFacetRow(facet.getField(), facet.getLabel(), values);
        }
    }

    private void renderFacetRow(String field, String displayLabel, List<FileFacetsResponse.FacetValue> options) {
        if (options == null || options.isEmpty() || getContext() == null) return;
        Context context = requireContext();

        // Row Title Label
        TextView tvLabel = new TextView(context);
        tvLabel.setText(displayLabel);
        tvLabel.setTextSize(14f);
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        int onSurface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, ContextCompat.getColor(context, R.color.md_theme_light_onSurface));
        tvLabel.setTextColor(onSurface);

        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = (int) (14 * getResources().getDisplayMetrics().density);
        labelParams.bottomMargin = (int) (6 * getResources().getDisplayMetrics().density);
        tvLabel.setLayoutParams(labelParams);
        dynamicFacetContainer.addView(tvLabel);

        // ChipGroup with inline wrapping
        ChipGroup group = new ChipGroup(context);
        group.setSingleLine(false);
        group.setSingleSelection(false);
        group.setChipSpacingHorizontal((int) (8 * getResources().getDisplayMetrics().density));
        group.setChipSpacingVertical((int) (8 * getResources().getDisplayMetrics().density));

        boolean isExpanded = Boolean.TRUE.equals(expandedFacets.get(field));
        int limit = (isExpanded || options.size() <= 4) ? options.size() : 4;

        Set<String> currentSelected = selectedFilters.get(field);

        for (int i = 0; i < limit; i++) {
            FileFacetsResponse.FacetValue opt = options.get(i);
            boolean isChecked = currentSelected != null && containsIgnoreCase(currentSelected, opt.getValue());

            Chip chip = createFilterChip(context, group, opt.getLabel(), opt.getCount(), isChecked);
            chip.setOnClickListener(v -> {
                boolean nowSelected = !isChecked;
                onFacetValueSelected(field, opt.getValue(), nowSelected);
            });
            group.addView(chip);
        }

        // Inline "+ More" / "Show less" chip
        if (options.size() > 4) {
            Chip moreChip = (Chip) LayoutInflater.from(context).inflate(R.layout.item_filter_chip_more, group, false);
            if (isExpanded) {
                moreChip.setText("Show less");
                moreChip.setChipIconResource(R.drawable.ic_lucide_chevron_down);
            } else {
                int remaining = options.size() - 4;
                moreChip.setText("+" + remaining + " more");
                moreChip.setChipIconResource(R.drawable.ic_lucide_chevron_right);
            }
            moreChip.setCheckable(false);
            moreChip.setClickable(true);

            moreChip.setOnClickListener(v -> {
                expandedFacets.put(field, !isExpanded);
                renderDynamicFacetRows(lastFacetFields);
            });

            group.addView(moreChip);
        }

        dynamicFacetContainer.addView(group);
    }

    private Chip createFilterChip(Context context, @Nullable ViewGroup parent, String label, long count, boolean isChecked) {
        Chip chip;
        if (parent != null) {
            chip = (Chip) LayoutInflater.from(context).inflate(R.layout.item_filter_chip, parent, false);
        } else {
            chip = (Chip) LayoutInflater.from(context).inflate(R.layout.item_filter_chip, null);
        }
        String text = count >= 0 ? label + " (" + count + ")" : label;
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(isChecked);
        return chip;
    }

    private boolean containsIgnoreCase(Set<String> set, String target) {
        if (set == null || target == null) return false;
        for (String s : set) {
            if (target.equalsIgnoreCase(s)) return true;
        }
        return false;
    }

    // --- Active Filters Bar ---

    private void renderActiveFiltersBar() {
        if (chipsActiveFilters == null || activeFiltersHeader == null || getContext() == null) return;
        Context context = requireContext();

        chipsActiveFilters.removeAllViews();
        List<ActiveFilterItem> items = new ArrayList<>();

        if (!metadataQuery.isEmpty()) {
            items.add(new ActiveFilterItem("Query: \"" + metadataQuery + "\"", () -> {
                metadataQuery = "";
                if (etMetadataSearch != null) etMetadataSearch.setText("");
                refreshFacetsAndResults();
            }));
        }

        if (!"ALL".equalsIgnoreCase(selectedFileType) && !selectedFileType.isEmpty()) {
            items.add(new ActiveFilterItem("Type: " + getCategoryDisplayName(selectedFileType), () -> {
                onFileTypeSelected("ALL");
            }));
        }

        for (Map.Entry<String, Set<String>> entry : selectedFilters.entrySet()) {
            String field = entry.getKey();
            for (String val : entry.getValue()) {
                items.add(new ActiveFilterItem(getFacetDisplayName(field) + ": " + val, () -> {
                    onFacetValueSelected(field, val, false);
                }));
            }
        }

        if (items.isEmpty()) {
            activeFiltersHeader.setVisibility(View.GONE);
            chipsActiveFilters.setVisibility(View.GONE);
        } else {
            activeFiltersHeader.setVisibility(View.VISIBLE);
            chipsActiveFilters.setVisibility(View.VISIBLE);

            for (ActiveFilterItem item : items) {
                Chip chip = (Chip) LayoutInflater.from(context).inflate(R.layout.item_filter_chip_active, chipsActiveFilters, false);
                chip.setText(item.label);
                chip.setClickable(false);
                chip.setOnCloseIconClickListener(v -> item.onRemove.run());
                chipsActiveFilters.addView(chip);
            }
        }
    }

    private String getCategoryDisplayName(String cat) {
        if ("IMAGES".equalsIgnoreCase(cat) || "PHOTOS".equalsIgnoreCase(cat)) return "Photos";
        if ("VIDEOS".equalsIgnoreCase(cat)) return "Videos";
        if ("DOCUMENTS".equalsIgnoreCase(cat) || "DOCS".equalsIgnoreCase(cat)) return "Documents";
        if ("AUDIO".equalsIgnoreCase(cat)) return "Audio";
        if ("ARCHIVES".equalsIgnoreCase(cat)) return "Archives";
        return cat;
    }

    private String getFacetDisplayName(String field) {
        if ("cameraMake".equalsIgnoreCase(field)) return "Make";
        if ("cameraModel".equalsIgnoreCase(field)) return "Model";
        if ("resolution".equalsIgnoreCase(field)) return "Resolution";
        if ("lens".equalsIgnoreCase(field)) return "Lens";
        if ("codec".equalsIgnoreCase(field)) return "Codec";
        if ("dateTaken".equalsIgnoreCase(field)) return "Year";
        if ("artist".equalsIgnoreCase(field)) return "Artist";
        if ("author".equalsIgnoreCase(field)) return "Author";
        if ("genre".equalsIgnoreCase(field)) return "Genre";
        return field;
    }

    // --- Results & Sorting ---

    private void applySortAndDisplay() {
        Collections.sort(activeResults, FileComparator.getComparator(currentSortOption));

        if (filesAdapter != null) {
            filesAdapter.setFiles(activeResults);
        }

        int count = activeResults.size();
        String countStr;
        if (count == 0) {
            countStr = "0 files found";
        } else if (count == 1) {
            countStr = "1 file found";
        } else {
            countStr = count + " files found";
        }

        if (tvResultCount != null) {
            tvResultCount.setText(countStr);
        }

        if (count == 0) {
            if (tvMetadataEmptyState != null) tvMetadataEmptyState.setVisibility(View.VISIBLE);
            if (rvMetadataResults != null) rvMetadataResults.setVisibility(View.GONE);
        } else {
            if (tvMetadataEmptyState != null) tvMetadataEmptyState.setVisibility(View.GONE);
            if (rvMetadataResults != null) rvMetadataResults.setVisibility(View.VISIBLE);
        }
    }

    private void showErrorState() {
        if (rvMetadataResults != null) rvMetadataResults.setVisibility(View.GONE);
        if (tvMetadataEmptyState != null) tvMetadataEmptyState.setVisibility(View.GONE);
        if (layoutMetadataSearchError != null) layoutMetadataSearchError.setVisibility(View.VISIBLE);
        if (tvResultCount != null) tvResultCount.setText("Search failed");
    }

    // --- File Actions ---

    @Override
    public void onDownloadClick(StoredFile file) {
        if (file == null || file.getId() == null || getContext() == null) return;
        Toast.makeText(requireContext(), "Downloading " + file.getOriginalFilename(), Toast.LENGTH_SHORT).show();
        TransferManager.getInstance(requireContext()).enqueueDownload(file);
    }

    @Override
    public void onDeleteClick(StoredFile file) {
        if (file == null || file.getId() == null || getContext() == null) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete File")
                .setMessage("Are you sure you want to permanently delete \"" + file.getOriginalFilename() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    apiService.deleteFile(file.getId()).enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "File deleted", Toast.LENGTH_SHORT).show();
                                refreshFacetsAndResults();
                            } else {
                                Toast.makeText(requireContext(), "Could not delete file", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void hideKeyboard() {
        if (getActivity() != null && getActivity().getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            }
        }
    }

    private static class ActiveFilterItem {
        final String label;
        final Runnable onRemove;

        ActiveFilterItem(String label, Runnable onRemove) {
            this.label = label;
            this.onRemove = onRemove;
        }
    }
}
