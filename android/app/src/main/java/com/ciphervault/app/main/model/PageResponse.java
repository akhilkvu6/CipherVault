package com.ciphervault.app.main.model;

import java.util.List;

public class PageResponse<T> {
    public List<T> content;
    public int totalPages;
    public long totalElements;
    public int size;
    public int number;
}