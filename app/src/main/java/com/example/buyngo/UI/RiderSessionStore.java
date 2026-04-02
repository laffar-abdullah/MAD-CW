package com.example.buyngo.UI;

import android.content.Context;
import android.content.SharedPreferences;

final class RiderSessionStore {
    private static final String PREFS_NAME = "buyngo_rider_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "name";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_VEHICLE = "vehicle";

    private RiderSessionStore() {
        // Utility class
    }

    static RiderProfile authenticate(String email, String password) {
        // Demo credential set used for the current coursework build.
        if ("rider@buyngo.com".equalsIgnoreCase(email) && "rider123".equals(password)) {
            return new RiderProfile("James Rider", "rider@buyngo.com", "077 775 5668", "Motorbike - XYZ 4521");
        }
        if ("abc.rider@buyngo.com".equalsIgnoreCase(email) && "abc123".equals(password)) {
            return new RiderProfile("Alex Rider", "abc.rider@buyngo.com", "071 224 1188", "Scooter - BNG 1092");
        }
        return null;
    }

    static void saveSession(Context context, RiderProfile profile) {
        // Save minimal rider identity needed across rider screens.
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_EMAIL, profile.email)
                .putString(KEY_NAME, profile.name)
                .putString(KEY_PHONE, profile.phone)
                .putString(KEY_VEHICLE, profile.vehicle)
                .apply();
    }

    static boolean isLoggedIn(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    static RiderProfile getCurrentRider(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!preferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return null;
        }

        String name = preferences.getString(KEY_NAME, "James Rider");
        String email = preferences.getString(KEY_EMAIL, "rider@buyngo.com");
        String phone = preferences.getString(KEY_PHONE, "077 775 5668");
        String vehicle = preferences.getString(KEY_VEHICLE, "Motorbike - XYZ 4521");
        return new RiderProfile(name, email, phone, vehicle);
    }

    static String getCurrentRiderEmail(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!preferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return null;
        }
        return preferences.getString(KEY_EMAIL, null);
    }

    static void clearSession(Context context) {
        // Remove all rider session fields on logout.
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().clear().apply();
    }

    static final class RiderProfile {
        final String name;
        final String email;
        final String phone;
        final String vehicle;

        RiderProfile(String name, String email, String phone, String vehicle) {
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.vehicle = vehicle;
        }
    }
}
