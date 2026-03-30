package com.example.androidapp.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

public class ActivityTest {

    private final Gson gson = new Gson();

    @Test
    public void testActivitySerialization() {
        Guide guide = new Guide("Carlos", 4.5);
        List<String> included = Arrays.asList("Transporte", "Almuerzo");
        List<String> gallery = Arrays.asList("url1", "url2");

        Activity activity = new Activity(
                "123", "City Tour", "Buenos Aires", "Turismo",
                "Un recorrido por la ciudad", "http://img.jpg", gallery,
                "3 horas", 5000, "ARS", 10, 20,
                "2026-04-15", "Plaza de Mayo", guide, "Espanol",
                included, "Cancelacion gratuita 24h antes", true);

        String json = gson.toJson(activity);
        Activity deserialized = gson.fromJson(json, Activity.class);

        assertNotNull(deserialized);
        assertEquals("123", deserialized.getId());
        assertEquals("City Tour", deserialized.getName());
        assertEquals("Buenos Aires", deserialized.getDestination());
        assertEquals("Turismo", deserialized.getCategory());
        assertEquals(5000, deserialized.getPrice(), 0.01);
        assertEquals(10, deserialized.getAvailableSpots());
        assertTrue(deserialized.isFeatured());
        assertNotNull(deserialized.getGuide());
        assertEquals("Carlos", deserialized.getGuide().getName());
        assertEquals(4.5, deserialized.getGuide().getRating(), 0.01);
        assertEquals(2, deserialized.getIncluded().size());
        assertEquals(2, deserialized.getGalleryUrls().size());
    }

    @Test
    public void testActivityDeserializationFromJson() {
        String json = "{\"id\":\"abc\",\"name\":\"Kayak\",\"destination\":\"Tigre\","
                + "\"category\":\"Aventura\",\"description\":\"Kayak en el delta\","
                + "\"price\":0,\"availableSpots\":5,\"totalSpots\":10,\"featured\":false}";

        Activity activity = gson.fromJson(json, Activity.class);

        assertNotNull(activity);
        assertEquals("abc", activity.getId());
        assertEquals("Kayak", activity.getName());
        assertEquals(0, activity.getPrice(), 0.01);
        assertFalse(activity.isFeatured());
    }

    @Test
    public void testActivityListDeserialization() {
        String json = "[{\"id\":\"1\",\"name\":\"A\"},{\"id\":\"2\",\"name\":\"B\"}]";
        Type listType = new TypeToken<List<Activity>>() {}.getType();

        List<Activity> activities = gson.fromJson(json, listType);

        assertNotNull(activities);
        assertEquals(2, activities.size());
        assertEquals("1", activities.get(0).getId());
        assertEquals("2", activities.get(1).getId());
    }
}
