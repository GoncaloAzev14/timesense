package com.datacentric.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollectionUtils {
    private CollectionUtils() {
    }

    /**
     * Return an immutable empty list if the input list is null otherwise return the
     * input
     * list.
     *
     * @param list the input list
     */
    public static <T> List<T> nullSafeList(List<T> list) {
        return nullSafeList(list, true);
    }

    /**
     * Return an empty list if the input list is null otherwise return the input
     * list.
     *
     * @param list      the input list
     * @param immutable if true, return an immutable empty list
     */
    public static <T> List<T> nullSafeList(List<T> list, boolean immutable) {
        return list == null ? (immutable ? Collections.emptyList() : new ArrayList<>()) : list;
    }
}
