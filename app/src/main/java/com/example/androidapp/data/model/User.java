package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class User {

    @SerializedName("id")
    private final String id;

    @SerializedName("email")
    private final String email;

    @SerializedName("username")
    private final String username;

    @SerializedName("fullName")
    private final String fullName;

    @SerializedName("phoneNumber")
    private final String phoneNumber;

    @SerializedName("preferences")
    private final UserPreferences preferences;

    public User(String id, String email, String username, String fullName, String phoneNumber, UserPreferences preferences) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.preferences = preferences;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public UserPreferences getPreferences() { return preferences; }

    public static class UserPreferences {
        @SerializedName("categories")
        private final List<String> categories;
        
        @SerializedName("destinations")
        private final List<String> destinations;

        public UserPreferences(List<String> categories, List<String> destinations) {
            this.categories = categories;
            this.destinations = destinations;
        }

        public List<String> getCategories() { return categories; }
        public List<String> getDestinations() { return destinations; }
    }

    public static class UserResponse {
        @SerializedName("user")
        private final User user;

        public UserResponse(User user) {
            this.user = user;
        }

        public User getUser() {
            return user;
        }
    }
}
