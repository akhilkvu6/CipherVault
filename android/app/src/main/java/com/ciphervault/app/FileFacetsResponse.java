package com.ciphervault.app;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FileFacetsResponse implements Serializable {

    @SerializedName("totalMatchingFiles")
    private long totalMatchingFiles;

    @SerializedName("facets")
    private List<FacetField> facets = new ArrayList<>();

    public FileFacetsResponse() {
    }

    public FileFacetsResponse(long totalMatchingFiles, List<FacetField> facets) {
        this.totalMatchingFiles = totalMatchingFiles;
        this.facets = facets != null ? facets : new ArrayList<>();
    }

    public long getTotalMatchingFiles() {
        return totalMatchingFiles;
    }

    public void setTotalMatchingFiles(long totalMatchingFiles) {
        this.totalMatchingFiles = totalMatchingFiles;
    }

    public List<FacetField> getFacets() {
        return facets != null ? facets : new ArrayList<>();
    }

    public void setFacets(List<FacetField> facets) {
        this.facets = facets != null ? facets : new ArrayList<>();
    }

    public static class FacetField implements Serializable {

        @SerializedName("field")
        private String field;

        @SerializedName("label")
        private String label;

        @SerializedName("available")
        private boolean available;

        @SerializedName("values")
        private List<FacetValue> values = new ArrayList<>();

        public FacetField() {
        }

        public FacetField(String field, String label, boolean available, List<FacetValue> values) {
            this.field = field;
            this.label = label;
            this.available = available;
            this.values = values != null ? values : new ArrayList<>();
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public List<FacetValue> getValues() {
            return values != null ? values : new ArrayList<>();
        }

        public void setValues(List<FacetValue> values) {
            this.values = values != null ? values : new ArrayList<>();
        }
    }

    public static class FacetValue implements Serializable {

        @SerializedName("value")
        private String value;

        @SerializedName("label")
        private String label;

        @SerializedName("count")
        private long count;

        public FacetValue() {
        }

        public FacetValue(String value, String label, long count) {
            this.value = value;
            this.label = label;
            this.count = count;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }
}
