package com.example.androidapp.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.androidapp.data.model.User;

import org.junit.Before;
import org.junit.Test;

public class SessionManagerTest {

    private SharedPreferences mockPrefs;
    private SharedPreferences.Editor mockEditor;
    private SessionManager sessionManager;

    @Before
    public void setUp() {
        Context mockContext = mock(Context.class);
        mockPrefs = mock(SharedPreferences.class);
        mockEditor = mock(SharedPreferences.Editor.class);

        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);
        when(mockEditor.clear()).thenReturn(mockEditor);

        sessionManager = new SessionManager(mockContext);
    }

    @Test
    public void testIsLoggedIn_withToken_returnsTrue() {
        when(mockPrefs.getString("token", null)).thenReturn("some_token");

        assertTrue(sessionManager.isLoggedIn());
    }

    @Test
    public void testIsLoggedIn_withoutToken_returnsFalse() {
        when(mockPrefs.getString("token", null)).thenReturn(null);

        assertFalse(sessionManager.isLoggedIn());
    }

    @Test
    public void testGetToken_returnsStoredToken() {
        when(mockPrefs.getString("token", null)).thenReturn("my_token");

        assertEquals("my_token", sessionManager.getToken());
    }

    @Test
    public void testGetBearerToken_returnsBearerPrefix() {
        when(mockPrefs.getString("token", null)).thenReturn("my_token");

        assertEquals("Bearer my_token", sessionManager.getBearerToken());
    }

    @Test
    public void testSaveSession_savesAllFields() {
        User user = new User("id1", "test@email.com", "testuser", "Test User", null);
        sessionManager.saveSession("token123", user);

        verify(mockEditor).putString("token", "token123");
        verify(mockEditor).putString("user_id", "id1");
        verify(mockEditor).putString("user_name", "testuser");
        verify(mockEditor).putString("user_email", "test@email.com");
        verify(mockEditor).putString("user_full_name", "Test User");
        verify(mockEditor).apply();
    }

    @Test
    public void testClear_clearsPreferences() {
        sessionManager.clear();

        verify(mockEditor).clear();
        verify(mockEditor).apply();
    }

    @Test
    public void testGetUserEmail_returnsEmail() {
        when(mockPrefs.getString("user_email", null)).thenReturn("email@test.com");

        assertEquals("email@test.com", sessionManager.getUserEmail());
    }

    @Test
    public void testGetUserName_returnsNull_whenNotSet() {
        when(mockPrefs.getString("user_name", null)).thenReturn(null);

        assertNull(sessionManager.getUserName());
    }
}
