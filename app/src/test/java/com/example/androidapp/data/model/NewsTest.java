package com.example.androidapp.data.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import java.lang.reflect.Field;

public class NewsTest {

    @Test
    public void hasRelatedActivity_returnsTrue_whenActivityIdIsNonEmpty() throws Exception {
        News n = new News();
        setField(n, "activityId", "a1");
        assertTrue(n.hasRelatedActivity());
    }

    @Test
    public void hasRelatedActivity_returnsFalse_whenActivityIdIsNull() {
        News n = new News();
        assertFalse(n.hasRelatedActivity());
    }

    @Test
    public void hasRelatedActivity_returnsFalse_whenActivityIdIsEmptyString() throws Exception {
        News n = new News();
        setField(n, "activityId", "");
        assertFalse(n.hasRelatedActivity());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
