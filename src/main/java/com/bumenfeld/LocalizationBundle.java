package com.bumenfeld;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class LocalizationBundle {
    private final List<String> titles;
    private final Map<String, List<String>> categories;

    public LocalizationBundle(List<String> titles, Map<String, List<String>> categories) {
        this.titles = titles.isEmpty() ? List.of("{player} has fallen.") : List.copyOf(titles);
        this.categories = Map.copyOf(categories);
    }

    public List<String> getTitles() {
        return titles;
    }

    public List<String> getLines(String category) {
        return categories.getOrDefault(category, categories.getOrDefault("generic", Collections.emptyList()));
    }
}
