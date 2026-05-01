package com.example.androidapp.util;

import com.example.androidapp.data.model.Filters;

import java.util.HashMap;
import java.util.Map;

public final class FilterQueryBuilder {

    private FilterQueryBuilder() {}

    public static Map<String, String> build(Filters filters) {
        Map<String, String> map = new HashMap<>();
        if (filters == null) return map;
        if (filters.destination != null) map.put("destination", filters.destination);
        if (filters.category != null) map.put("category", filters.category);
        if (filters.date != null) map.put("date", filters.date);
        if (filters.priceMin != null) map.put("priceMin", String.valueOf(filters.priceMin));
        if (filters.priceMax != null) map.put("priceMax", String.valueOf(filters.priceMax));
        return map;
    }
}
