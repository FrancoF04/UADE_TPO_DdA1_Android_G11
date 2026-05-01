package com.example.androidapp.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.example.androidapp.data.model.News;
import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NewsCacheTest {

    private Context ctx;
    private NewsCache cache;

    @Before
    public void setUp() {
        ctx = ApplicationProvider.getApplicationContext();
        cache = new NewsCache(ctx, new Gson());
        new File(ctx.getFilesDir(), "news_cache.json").delete();
    }

    @After
    public void tearDown() {
        new File(ctx.getFilesDir(), "news_cache.json").delete();
    }

    @Test
    public void exists_isFalse_whenFileMissing() {
        assertFalse(cache.exists());
    }

    @Test
    public void read_returnsNull_whenFileMissing() {
        assertNull(cache.read());
    }

    @Test
    public void save_then_read_roundTripsItems() throws Exception {
        News a = makeNews("n1", "a1", "Promo");
        News b = makeNews("n3", null, "Destino");
        cache.save(Arrays.asList(a, b));

        assertTrue(cache.exists());
        List<News> out = cache.read();
        assertNotNull(out);
        assertEquals(2, out.size());
        assertEquals("n1", out.get(0).getId());
        assertEquals("a1", out.get(0).getActivityId());
        assertEquals("n3", out.get(1).getId());
        assertNull(out.get(1).getActivityId());
    }

    @Test
    public void save_emptyList_readsBackEmpty() {
        cache.save(Collections.<News>emptyList());
        List<News> out = cache.read();
        assertNotNull(out);
        assertTrue(out.isEmpty());
    }

    private static News makeNews(String id, String activityId, String title) throws Exception {
        News n = new News();
        setField(n, "id", id);
        setField(n, "activityId", activityId);
        setField(n, "title", title);
        return n;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }
}
