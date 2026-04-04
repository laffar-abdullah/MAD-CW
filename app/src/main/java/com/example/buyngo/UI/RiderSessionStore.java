package com.example.buyngo.UI;

import android.content.Context;
import android.content.SharedPreferences;


final class RiderSessionStore {

    // SharedPreferences file used exclusively for rider session data.
    private static final String PREFS_NAME = "buyngo_rider_session";

    // Keys stored inside the preferences file.
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_EMAIL        = "email";
    private static final String KEY_NAME         = "name";
    private static final String KEY_PHONE        = "phone";
    private static final String KEY_VEHICLE      = "vehicle";
    private static final String KEY_PROFILE_IMAGE_URL = "profile_image_url";

    // Utility class — no instances.
    private RiderSessionStore() { }

    // ── Session persistence ─────────────────────────────────────────────────

    
    static void saveSession(Context context, RiderProfile profile) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_EMAIL,   profile.email)
                .putString(KEY_NAME,    profile.name)
                .putString(KEY_PHONE,   profile.phone)
                .putString(KEY_VEHICLE, profile.vehicle)
                .putString(KEY_PROFILE_IMAGE_URL, profile.profileImageUrl)
                .apply();
    }

    
    static boolean isLoggedIn(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_IS_LOGGED_IN, false);
    }

    
    static RiderProfile getCurrentRider(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return null;
        }
        return new RiderProfile(
                prefs.getString(KEY_NAME,    "James Rider"),
                prefs.getString(KEY_EMAIL,   "rider@buyngo.com"),
                prefs.getString(KEY_PHONE,   "077 775 5668"),
            prefs.getString(KEY_VEHICLE, "Motorbike - XYZ 4521"),
            prefs.getString(KEY_PROFILE_IMAGE_URL, null));
    }

    
    static String getCurrentRiderEmail(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return null;
        }
        return prefs.getString(KEY_EMAIL, null);
    }

    
    static void clearSession(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    // ── Data class ──────────────────────────────────────────────────────────

    
    static final class RiderProfile {
        final String name;
        final String email;
        final String phone;
        final String vehicle;
        final String profileImageUrl;

        RiderProfile(String name, String email, String phone, String vehicle, String profileImageUrl) {
            this.name            = name;
            this.email           = email;
            this.phone           = phone;
            this.vehicle         = vehicle;
            this.profileImageUrl = profileImageUrl;
        }
    }
}

