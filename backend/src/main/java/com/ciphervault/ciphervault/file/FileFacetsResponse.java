package com.ciphervault.ciphervault.file;

import java.util.ArrayList;
import java.util.List;

public class FileFacetsResponse {

    private long totalMatchingFiles;
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
        return facets;
    }

    public void setFacets(List<FacetField> facets) {
        this.facets = facets != null ? facets : new ArrayList<>();
    }

    public static class FacetField {
        private String field;
        private String label;
        private boolean available;
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
            return values;
        }

        public void setValues(List<FacetValue> values) {
            this.values = values != null ? values : new ArrayList<>();
        }
    }

    public static class FacetValue {
        private String value;
        private String label;
        private long count;

        public FacetValue() {
        }

        public FacetValue(String value, String label, long count) {
            this.value = value;
            this.label = label != null ? label : value;
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
