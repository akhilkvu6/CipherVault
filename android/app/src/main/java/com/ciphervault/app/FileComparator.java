package com.ciphervault.app;

import java.util.Comparator;

public class FileComparator {

    public static Comparator<StoredFile> getComparator(FileSortOption option) {
        if (option == null) {
            option = FileSortOption.NAME_ASC;
        }

        switch (option) {
            case NAME_DESC:
                return (f1, f2) -> {
                    String n1 = f1 != null ? f1.getOriginalFilename() : "";
                    String n2 = f2 != null ? f2.getOriginalFilename() : "";
                    int res = String.CASE_INSENSITIVE_ORDER.compare(n2 != null ? n2 : "", n1 != null ? n1 : "");
                    if (res != 0) return res;
                    return compareIds(f2, f1);
                };

            case DATE_OLD_NEW:
                return (f1, f2) -> {
                    String d1 = f1 != null ? f1.getCreatedAt() : "";
                    String d2 = f2 != null ? f2.getCreatedAt() : "";
                    int res = compareDates(d1, d2);
                    if (res != 0) return res;
                    return compareIds(f1, f2);
                };

            case DATE_NEW_OLD:
                return (f1, f2) -> {
                    String d1 = f1 != null ? f1.getCreatedAt() : "";
                    String d2 = f2 != null ? f2.getCreatedAt() : "";
                    int res = compareDates(d2, d1);
                    if (res != 0) return res;
                    return compareIds(f2, f1);
                };

            case SIZE_LOW_HIGH:
                return (f1, f2) -> {
                    long s1 = (f1 != null && f1.getFileSize() != null) ? f1.getFileSize() : 0L;
                    long s2 = (f2 != null && f2.getFileSize() != null) ? f2.getFileSize() : 0L;
                    int res = Long.compare(s1, s2);
                    if (res != 0) return res;
                    return compareNamesAsc(f1, f2);
                };

            case SIZE_HIGH_LOW:
                return (f1, f2) -> {
                    long s1 = (f1 != null && f1.getFileSize() != null) ? f1.getFileSize() : 0L;
                    long s2 = (f2 != null && f2.getFileSize() != null) ? f2.getFileSize() : 0L;
                    int res = Long.compare(s2, s1);
                    if (res != 0) return res;
                    return compareNamesAsc(f1, f2);
                };

            case NAME_ASC:
            default:
                return (f1, f2) -> {
                    String n1 = f1 != null ? f1.getOriginalFilename() : "";
                    String n2 = f2 != null ? f2.getOriginalFilename() : "";
                    int res = String.CASE_INSENSITIVE_ORDER.compare(n1 != null ? n1 : "", n2 != null ? n2 : "");
                    if (res != 0) return res;
                    return compareIds(f1, f2);
                };
        }
    }

    private static int compareDates(String d1, String d2) {
        if (d1 == null && d2 == null) return 0;
        if (d1 == null || d1.trim().isEmpty()) return 1;
        if (d2 == null || d2.trim().isEmpty()) return -1;
        return d1.compareTo(d2);
    }

    private static int compareNamesAsc(StoredFile f1, StoredFile f2) {
        String n1 = f1 != null ? f1.getOriginalFilename() : "";
        String n2 = f2 != null ? f2.getOriginalFilename() : "";
        return String.CASE_INSENSITIVE_ORDER.compare(n1 != null ? n1 : "", n2 != null ? n2 : "");
    }

    private static int compareIds(StoredFile f1, StoredFile f2) {
        long id1 = (f1 != null && f1.getId() != null) ? f1.getId() : 0L;
        long id2 = (f2 != null && f2.getId() != null) ? f2.getId() : 0L;
        return Long.compare(id1, id2);
    }
}
