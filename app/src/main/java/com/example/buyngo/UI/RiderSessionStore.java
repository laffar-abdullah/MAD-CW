package com.example.buyngo.UI;

import android.content.Context;
import android.content.SharedPreferences;

/**
   Stores rider credentials in SharedPreferences so the app remembers who is
   logged in across activity re-starts.  All methods are package-private so
   only rider UI classes can call them.
*/
final class RiderSessionStore {

    // SharedPreferences file used exclusively for rider session data.
    private static final String PREFS_NAME = "buyngo_rider_session";

    // Keys stored inside the preferences file.
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_EMAIL        = "email";
    private static final String KEY_NAME         = "name";
    private static final String KEY_PHONE        = "phone";
    private static final String KEY_VEHICLE      = "vehicle";
    private static final String KEY_BIRTHDATE    = "birthdate";
    private static final String KEY_PROFILE_IMAGE_URL = "profile_image_url";

    // Utility class — no instances.
    private RiderSessionStore() { }


    static void saveSession(Context context, RiderProfile profile) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_EMAIL,   profile.email)
                .putString(KEY_NAME,    profile.name)
                .putString(KEY_PHONE,   profile.phone)
                .putString(KEY_VEHICLE, profile.vehicle)
                .putString(KEY_BIRTHDATE, profile.birthdate)
                .putString(KEY_PROFILE_IMAGE_URL, profile.profileImageUrl)
                .apply();
    }

    /**
     * @return {@code true} while there is an active rider session.
     */
    static boolean isLoggedIn(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Reads back the full profile stored by {@link #saveSession}.
     *
     * @return the current rider's profile, or {@code null} when no session
     *         is active.
     */
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
                prefs.getString(KEY_BIRTHDATE, "1990-01-01"),
                prefs.getString(KEY_PROFILE_IMAGE_URL, null));
    }

    /**
     * Returns the logged-in rider's email address, used by
     * {@link OrderStatusStore} to scope per-rider data keys.
     *
     * @return the email string, or {@code null} when no session is active.
     */
    static String getCurrentRiderEmail(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return null;
        }
        return prefs.getString(KEY_EMAIL, null);
    }

    /**
     * Wipes all session data.  Called by the logout button in
     * {@link RidProfileActivity}.
     */
    static void clearSession(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    // ── Data class ──────────────────────────────────────────────────────────

    /** Immutable snapshot of a rider's profile. */
    static final class RiderProfile {
        final String name;
        final String email;
        final String phone;
        final String vehicle;
        final String birthdate;
        final String profileImageUrl;

        RiderProfile(String name, String email, String phone, String vehicle, String birthdate, String profileImageUrl) {
            this.name            = name;
            this.email           = email;
            this.phone           = phone;
            this.vehicle         = vehicle;
            this.birthdate       = birthdate;
            this.profileImageUrl = profileImageUrl;
        }
    }
}
