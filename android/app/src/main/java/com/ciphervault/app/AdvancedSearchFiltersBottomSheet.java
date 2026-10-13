package com.ciphervault.app;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class AdvancedSearchFiltersBottomSheet {

    public interface OnFiltersAppliedListener {
        void onFiltersApplied(MetadataSearchBottomSheet.FilterCriteria criteria);
    }

    private AdvancedSearchFiltersBottomSheet() {}

    public static void show(
            @NonNull Context context,
            @Nullable MetadataSearchBottomSheet.FilterCriteria initialCriteria,
            @NonNull OnFiltersAppliedListener listener) {

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(
                R.layout.bottom_sheet_advanced_search_filters, null, false);
        dialog.setContentView(view);

        MetadataSearchBottomSheet.FilterCriteria criteria =
                new MetadataSearchBottomSheet.FilterCriteria();
        if (initialCriteria != null) {
            criteria.copyFrom(initialCriteria);
        }

        ImageButton btnClose = view.findViewById(R.id.btnCloseFiltersSheet);
        ChipGroup cgFileType = view.findViewById(R.id.cgFilterFileType);
        ChipGroup cgDateTaken = view.findViewById(R.id.cgFilterDateTaken);
        TextInputEditText etCameraMake = view.findViewById(R.id.etFilterCameraMake);
        TextInputEditText etCameraModel = view.findViewById(R.id.etFilterCameraModel);
        TextInputEditText etResolution = view.findViewById(R.id.etFilterResolution);
        TextInputEditText etCodec = view.findViewById(R.id.etFilterCodec);
        TextInputEditText etMinSizeMb = view.findViewById(R.id.etFilterMinSizeMb);
        TextInputEditText etMaxSizeMb = view.findViewById(R.id.etFilterMaxSizeMb);
        TextInputEditText etAuthor = view.findViewById(R.id.etFilterAuthor);
        MaterialButton btnReset = view.findViewById(R.id.btnResetFilters);
        MaterialButton btnApply = view.findViewById(R.id.btnApplyFilters);

        // Pre-populate File Type
        if (cgFileType != null) {
            String cat = criteria.category != null ? criteria.category.toUpperCase(Locale.US) : "ALL";
            switch (cat) {
                case "IMAGES":
                case "PHOTOS":
                    cgFileType.check(R.id.chipTypeImages);
                    break;
                case "VIDEOS":
                    cgFileType.check(R.id.chipTypeVideos);
                    break;
                case "DOCUMENTS":
                case "PDFS":
                    cgFileType.check(R.id.chipTypeDocuments);
                    break;
                case "AUDIO":
                    cgFileType.check(R.id.chipTypeAudio);
                    break;
                case "OTHER":
                case "ARCHIVES":
                    cgFileType.check(R.id.chipTypeOther);
                    break;
                default:
                    cgFileType.check(R.id.chipTypeAll);
                    break;
            }
        }

        // Pre-populate Date Taken
        if (cgDateTaken != null) {
            String dRange = criteria.dateRange != null ? criteria.dateRange.toUpperCase(Locale.US) : "ANY";
            switch (dRange) {
                case "TODAY":
                    cgDateTaken.check(R.id.chipDateToday);
                    break;
                case "WEEK":
                    cgDateTaken.check(R.id.chipDateWeek);
                    break;
                case "MONTH":
                    cgDateTaken.check(R.id.chipDateMonth);
                    break;
                case "YEAR":
                    cgDateTaken.check(R.id.chipDateYear);
                    break;
                default:
                    cgDateTaken.check(R.id.chipDateAny);
                    break;
            }
        }

        // Pre-populate inputs
        if (etCameraMake != null && criteria.cameraMake != null) {
            etCameraMake.setText(criteria.cameraMake);
        }
        if (etCameraModel != null && criteria.cameraModel != null) {
            etCameraModel.setText(criteria.cameraModel);
        }
        if (etResolution != null && criteria.resolution != null) {
            etResolution.setText(criteria.resolution);
        }
        if (etCodec != null && criteria.codec != null) {
            etCodec.setText(criteria.codec);
        }
        if (etAuthor != null && criteria.author != null) {
            etAuthor.setText(criteria.author);
        }
        if (etMinSizeMb != null && criteria.minSize != null && criteria.minSize > 0) {
            double mb = criteria.minSize / (1024.0 * 1024.0);
            etMinSizeMb.setText(String.format(Locale.US, "%.1f", mb));
        }
        if (etMaxSizeMb != null && criteria.maxSize != null && criteria.maxSize > 0) {
            double mb = criteria.maxSize / (1024.0 * 1024.0);
            etMaxSizeMb.setText(String.format(Locale.US, "%.1f", mb));
        }

        // Actions
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                if (cgFileType != null) cgFileType.check(R.id.chipTypeAll);
                if (cgDateTaken != null) cgDateTaken.check(R.id.chipDateAny);
                if (etCameraMake != null) etCameraMake.setText("");
                if (etCameraModel != null) etCameraModel.setText("");
                if (etResolution != null) etResolution.setText("");
                if (etCodec != null) etCodec.setText("");
                if (etMinSizeMb != null) etMinSizeMb.setText("");
                if (etMaxSizeMb != null) etMaxSizeMb.setText("");
                if (etAuthor != null) etAuthor.setText("");
                criteria.clear();
            });
        }

        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                // Read Category
                if (cgFileType != null) {
                    int checkedId = cgFileType.getCheckedChipId();
                    if (checkedId == R.id.chipTypeImages) criteria.category = "IMAGES";
                    else if (checkedId == R.id.chipTypeVideos) criteria.category = "VIDEOS";
                    else if (checkedId == R.id.chipTypeDocuments) criteria.category = "DOCUMENTS";
                    else if (checkedId == R.id.chipTypeAudio) criteria.category = "AUDIO";
                    else if (checkedId == R.id.chipTypeOther) criteria.category = "OTHER";
                    else criteria.category = "ALL";
                }

                // Read Date Taken
                if (cgDateTaken != null) {
                    int checkedDateId = cgDateTaken.getCheckedChipId();
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.US);
                    SimpleDateFormat sdfMonth = new SimpleDateFormat("yyyy:MM", Locale.US);
                    SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy:MM:dd", Locale.US);

                    if (checkedDateId == R.id.chipDateToday) {
                        criteria.dateRange = "TODAY";
                        criteria.dateTaken = sdfDay.format(cal.getTime());
                    } else if (checkedDateId == R.id.chipDateWeek) {
                        criteria.dateRange = "WEEK";
                        criteria.dateTaken = sdfMonth.format(cal.getTime());
                    } else if (checkedDateId == R.id.chipDateMonth) {
                        criteria.dateRange = "MONTH";
                        criteria.dateTaken = sdfMonth.format(cal.getTime());
                    } else if (checkedDateId == R.id.chipDateYear) {
                        criteria.dateRange = "YEAR";
                        criteria.dateTaken = sdfYear.format(cal.getTime());
                    } else {
                        criteria.dateRange = "ANY";
                        criteria.dateTaken = null;
                    }
                }

                // Read text inputs
                criteria.cameraMake = etCameraMake != null && etCameraMake.getText() != null
                        ? etCameraMake.getText().toString().trim() : null;
                criteria.cameraModel = etCameraModel != null && etCameraModel.getText() != null
                        ? etCameraModel.getText().toString().trim() : null;
                criteria.resolution = etResolution != null && etResolution.getText() != null
                        ? etResolution.getText().toString().trim() : null;
                criteria.codec = etCodec != null && etCodec.getText() != null
                        ? etCodec.getText().toString().trim() : null;
                criteria.author = etAuthor != null && etAuthor.getText() != null
                        ? etAuthor.getText().toString().trim() : null;

                // Validate and parse sizes
                Long minBytes = null;
                Long maxBytes = null;
                if (etMinSizeMb != null && etMinSizeMb.getText() != null) {
                    String minStr = etMinSizeMb.getText().toString().trim();
                    if (!TextUtils.isEmpty(minStr)) {
                        try {
                            double mb = Double.parseDouble(minStr);
                            if (mb < 0) {
                                Toast.makeText(context, "Minimum size cannot be negative", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            minBytes = (long) (mb * 1024 * 1024);
                        } catch (NumberFormatException e) {
                            Toast.makeText(context, "Invalid minimum size", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }

                if (etMaxSizeMb != null && etMaxSizeMb.getText() != null) {
                    String maxStr = etMaxSizeMb.getText().toString().trim();
                    if (!TextUtils.isEmpty(maxStr)) {
                        try {
                            double mb = Double.parseDouble(maxStr);
                            if (mb < 0) {
                                Toast.makeText(context, "Maximum size cannot be negative", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            maxBytes = (long) (mb * 1024 * 1024);
                        } catch (NumberFormatException e) {
                            Toast.makeText(context, "Invalid maximum size", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }

                if (minBytes != null && maxBytes != null && minBytes > maxBytes) {
                    Toast.makeText(context, "Minimum size cannot exceed maximum size", Toast.LENGTH_SHORT).show();
                    return;
                }

                criteria.minSize = minBytes;
                criteria.maxSize = maxBytes;

                listener.onFiltersApplied(criteria);
                dialog.dismiss();
            });
        }

        dialog.show();
    }
}
