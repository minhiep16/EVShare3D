package com.example.evshare.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedData<T> {

    private List<T> items = Collections.emptyList();
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean isFirst;
    private boolean isLast;
    private boolean hasNext;
    private boolean hasPrevious;

    public PagedData() {
    }

    public PagedData(List<T> items, int page, int size, long totalElements) {
        this.items = items != null ? items : Collections.emptyList();
        this.page = page;
        this.size = size > 0 ? size : 20;
        this.totalElements = totalElements;
        this.totalPages = this.size > 0 ? (int) Math.ceil((double) totalElements / this.size) : 0;
        this.isFirst = page == 0;
        this.isLast = page >= (this.totalPages - 1);
        this.hasNext = page < (this.totalPages - 1);
        this.hasPrevious = page > 0;
    }

    public static <T> PagedData<T> of(List<T> items, int page, int size, long totalElements) {
        return new PagedData<>(items, page, size, totalElements);
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return isFirst;
    }

    public void setFirst(boolean first) {
        isFirst = first;
    }

    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
}
