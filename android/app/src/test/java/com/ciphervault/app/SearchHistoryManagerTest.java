package com.ciphervault.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchHistoryManagerTest {

    @Test
    public void testSearchCountItemSortingDescending() {
        List<SearchHistoryManager.SearchCountItem> items = new ArrayList<>();
        items.add(new SearchHistoryManager.SearchCountItem("Sony A7IV", 2));
        items.add(new SearchHistoryManager.SearchCountItem("Canon EOS R5", 10));
        items.add(new SearchHistoryManager.SearchCountItem("1080p", 5));
        items.add(new SearchHistoryManager.SearchCountItem("H.264", 1));

        Collections.sort(items);

        assertEquals("Canon EOS R5", items.get(0).query);
        assertEquals(10, items.get(0).count);

        assertEquals("1080p", items.get(1).query);
        assertEquals(5, items.get(1).count);

        assertEquals("Sony A7IV", items.get(2).query);
        assertEquals(2, items.get(2).count);

        assertEquals("H.264", items.get(3).query);
        assertEquals(1, items.get(3).count);
    }

    @Test
    public void testSearchCountItemDeterministicTieBreaker() {
        List<SearchHistoryManager.SearchCountItem> items = new ArrayList<>();
        items.add(new SearchHistoryManager.SearchCountItem("Zebra", 3));
        items.add(new SearchHistoryManager.SearchCountItem("Apple", 3));
        items.add(new SearchHistoryManager.SearchCountItem("Mango", 3));

        Collections.sort(items);

        // When counts are tied, alphabetical tie-breaking
        assertEquals("Apple", items.get(0).query);
        assertEquals("Mango", items.get(1).query);
        assertEquals("Zebra", items.get(2).query);
    }
}
