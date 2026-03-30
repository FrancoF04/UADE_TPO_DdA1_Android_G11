package com.example.androidapp.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.List;

public class ApiResponseTest {

    private final Gson gson = new Gson();

    @Test
    public void testSuccessResponseDeserialization() {
        String json = "{\"success\":true,\"data\":{\"id\":\"1\",\"name\":\"Test\"},\"error\":null}";
        Type type = new TypeToken<ApiResponse<Activity>>() {}.getType();

        ApiResponse<Activity> response = gson.fromJson(json, type);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertNull(response.getError());
    }

    @Test
    public void testErrorResponseDeserialization() {
        String json = "{\"success\":false,\"data\":null,\"error\":\"Not found\"}";
        Type type = new TypeToken<ApiResponse<Activity>>() {}.getType();

        ApiResponse<Activity> response = gson.fromJson(json, type);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertNull(response.getData());
        assertEquals("Not found", response.getError());
    }

    @Test
    public void testResponseWithMetaDeserialization() {
        String json = "{\"success\":true,\"data\":[{\"id\":\"1\"}],"
                + "\"meta\":{\"total\":100,\"page\":1,\"limit\":10}}";
        Type type = new TypeToken<ApiResponse<List<Activity>>>() {}.getType();

        ApiResponse<List<Activity>> response = gson.fromJson(json, type);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getMeta());
        assertEquals(100, response.getMeta().getTotal());
        assertEquals(1, response.getMeta().getPage());
        assertEquals(10, response.getMeta().getLimit());
    }

    @Test
    public void testAuthResponseDeserialization() {
        String json = "{\"success\":true,\"data\":{\"token\":\"jwt123\","
                + "\"user\":{\"id\":\"u1\",\"email\":\"test@test.com\","
                + "\"username\":\"testuser\",\"fullName\":\"Test User\"}}}";
        Type type = new TypeToken<ApiResponse<AuthResponse>>() {}.getType();

        ApiResponse<AuthResponse> response = gson.fromJson(json, type);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("jwt123", response.getData().getToken());
        assertNotNull(response.getData().getUser());
        assertEquals("test@test.com", response.getData().getUser().getEmail());
        assertEquals("testuser", response.getData().getUser().getUsername());
    }
}
