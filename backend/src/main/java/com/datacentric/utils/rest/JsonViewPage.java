package com.datacentric.utils.rest;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.annotation.JsonView;

@JsonView(JsonViewPage.Views.Public.class)
public class JsonViewPage<T> {

    public static final class Views {
        public interface Public {
        }
    }

    @JsonView(Views.Public.class)
    private final List<T> content;

    @JsonView(Views.Public.class)
    private final long totalElements;

    @JsonView(Views.Public.class)
    private final int totalPages;

    @JsonView(Views.Public.class)
    private final int number;

    @JsonView(Views.Public.class)
    private final int size;

    @JsonView(Views.Public.class)
    private final boolean first;

    @JsonView(Views.Public.class)
    private final boolean last;

    @JsonView(Views.Public.class)
    private final boolean empty;

    @JsonView(Views.Public.class)
    private final int numberOfElements;

    public JsonViewPage(final Page<T> page, final Pageable pageable) {
        content = page.getContent();
        totalElements = page.getTotalElements();
        totalPages = page.getTotalPages();
        number = page.getNumber();
        size = page.getSize();
        first = page.isFirst();
        last = page.isLast();
        empty = page.isEmpty();
        numberOfElements = page.getNumberOfElements();
    }

    public JsonViewPage(final Page<T> page) {
        this(page, page.getPageable());
    }
}
