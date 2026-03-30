package com.example.androidapp.ui.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.androidapp.data.model.Activity;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for Activity model creation and data integrity used by adapters.
 * RecyclerView.Adapter methods require Android framework and are tested
 * via instrumented tests.
 */
public class ActivityAdapterTest {

    @Test
    public void testActivityCreation_allFieldsAccessible() {
        Activity activity = createActivity("1", "City Tour");

        assertNotNull(activity);
        assertEquals("1", activity.getId());
        assertEquals("City Tour", activity.getName());
        assertEquals("Dest", activity.getDestination());
        assertEquals("Cat", activity.getCategory());
        assertEquals(100, activity.getPrice(), 0.01);
        assertEquals(5, activity.getAvailableSpots());
    }

    @Test
    public void testActivityList_multipleItems() {
        List<Activity> activities = Arrays.asList(
                createActivity("1", "Activity 1"),
                createActivity("2", "Activity 2"),
                createActivity("3", "Activity 3")
        );

        assertEquals(3, activities.size());
        assertEquals("Activity 1", activities.get(0).getName());
        assertEquals("Activity 3", activities.get(2).getName());
    }

    @Test
    public void testActivityPriceFormatting_free() {
        Activity activity = createActivityWithPrice("1", "Free Tour", 0);
        assertEquals(0, activity.getPrice(), 0.01);
    }

    @Test
    public void testActivityPriceFormatting_paid() {
        Activity activity = createActivityWithPrice("2", "Paid Tour", 5000);
        assertEquals(5000, activity.getPrice(), 0.01);
    }

    @Test
    public void testActivitySpots() {
        Activity activity = new Activity("id", "Name", "Dest", "Cat", "Desc",
                null, null, "2h", 100, "ARS",
                0, 10, "2026-01-01", "Point",
                null, "ES", null, "Policy", false);

        assertEquals(0, activity.getAvailableSpots());
        assertEquals(10, activity.getTotalSpots());
    }

    private Activity createActivity(String id, String name) {
        return new Activity(id, name, "Dest", "Cat", "Desc",
                null, null, "2h", 100, "ARS",
                5, 10, "2026-01-01", "Point",
                null, "ES", null, "Policy", false);
    }

    private Activity createActivityWithPrice(String id, String name, double price) {
        return new Activity(id, name, "Dest", "Cat", "Desc",
                null, null, "2h", price, "ARS",
                5, 10, "2026-01-01", "Point",
                null, "ES", null, "Policy", false);
    }
}
