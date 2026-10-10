package com.example.amanahstaffapp;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;

/**
 * Stores a single session token for the whole app session so every tracked
 * event ties back to the same visitor. The token is generated once on first
 * launch and persisted in SharedPreferences.
 *
 * TOKEN FORMAT: exactly 64 lowercase hexadecimal characters, no prefix and no
 * dashes — i.e. bin2hex() of 32 SecureRandom bytes. This mirrors what the web
 * decoy sends and what api/log_activity.php accepts (/^[a-f0-9]{64}$/). Any
 * other shape is rejected server-side, which would spawn a fresh session row
 * per request, so the format must match exactly.
 */
public class SessionManager {

    private static final String PREFS = "amanah_staff_prefs";
    private static final String KEY_SESSION_TOKEN = "session_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_NOTIFICATIONS_OPENED_AT = "notifications_opened_at";
    private static final String KEY_HANDOVER_OPENED_AT = "handover_opened_at";
    private static final String KEY_SHIFT_REMINDERS = "shift_reminders";
    private static final String KEY_HANDOVER_ALERTS = "handover_alerts";

    /** The exact shape the backend accepts: 64 lowercase hex chars, nothing else. */
    private static final String TOKEN_PATTERN = "^[a-f0-9]{64}$";

    /** 32 random bytes -> 64 hex chars. */
    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static SessionManager get(Context context) {
        return new SessionManager(context);
    }

    /**
     * Returns the persistent session token for the whole visit. It is created
     * exactly once (on first access after launch or after a logout) and reused
     * for every API call afterwards. It never returns null or empty, so callers
     * can never accidentally send an empty token.
     *
     * A stored token is reused only if it still matches the 64-hex backend
     * format. Anything else (empty, or a legacy "sess_"-prefixed token from a
     * previous build) is discarded and replaced with a valid one, so existing
     * installs upgrade cleanly on first launch after this change.
     *
     * commit() (synchronous) is used for the write so a background API thread
     * reading the token immediately after cannot miss it.
     */
    public String getSessionToken() {
        String token = prefs.getString(KEY_SESSION_TOKEN, null);
        if (token == null || !token.matches(TOKEN_PATTERN)) {
            token = generateToken();
            prefs.edit().putString(KEY_SESSION_TOKEN, token).commit();
        }
        return token;
    }

    /** Builds a 64-char lowercase-hex token from 32 SecureRandom bytes. */
    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(TOKEN_BYTES * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    public void setUsername(String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public void clearUsername() {
        prefs.edit().remove(KEY_USERNAME).apply();
    }

    /**
     * When the Notifications screen was last opened (epoch millis), or 0 if it
     * never has been. This is the only thing stored for the bell: its count is
     * worked out from the clock as the items posted after this moment.
     */
    public long getNotificationsOpenedAt() {
        return prefs.getLong(KEY_NOTIFICATIONS_OPENED_AT, 0L);
    }

    public void setNotificationsOpenedAt(long millis) {
        prefs.edit().putLong(KEY_NOTIFICATIONS_OPENED_AT, millis).apply();
    }

    /**
     * When Handover Notes was last opened (epoch millis), or 0 if it never has
     * been. Home's "Handover new" tile counts the notes written after it.
     */
    public long getHandoverOpenedAt() {
        return prefs.getLong(KEY_HANDOVER_OPENED_AT, 0L);
    }

    public void setHandoverOpenedAt(long millis) {
        prefs.edit().putLong(KEY_HANDOVER_OPENED_AT, millis).apply();
    }

    /**
     * The two notification switches in Settings. They are kept on the phone
     * only: nothing reads them and they are never sent anywhere.
     */
    public boolean getShiftReminders() {
        return prefs.getBoolean(KEY_SHIFT_REMINDERS, true);
    }

    public void setShiftReminders(boolean on) {
        prefs.edit().putBoolean(KEY_SHIFT_REMINDERS, on).apply();
    }

    public boolean getHandoverAlerts() {
        return prefs.getBoolean(KEY_HANDOVER_ALERTS, true);
    }

    public void setHandoverAlerts(boolean on) {
        prefs.edit().putBoolean(KEY_HANDOVER_ALERTS, on).apply();
    }

    /**
     * Ends the current visit: clears the session token (and username) so the
     * next call to {@link #getSessionToken()} starts a brand-new session. Call
     * this ONLY on logout — never between ordinary activities.
     */
    public void clearSession() {
        prefs.edit()
                .remove(KEY_SESSION_TOKEN)
                .remove(KEY_USERNAME)
                .commit();
    }
}
