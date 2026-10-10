package com.example.amanahstaffapp;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single entry point for all activity-logging POST calls to the deception
 * backend. Every call runs on a background thread; optional callbacks are
 * delivered back on the main thread.
 *
 * NOTE: API_BASE_URL and DECOY_KEY are injected via BuildConfig from
 * local.properties (decoy.apiBaseUrl / decoy.apiKey), which is git-ignored.
 * 10.0.2.2 is the emulator alias for the host machine.
 */
public class ApiClient {

    private static final String TAG = "AmanahApi";

    /**
     * Backend location, set via {@code decoy.apiBaseUrl} in local.properties.
     * Must end with '/'. Defaults to the local emulator URL if unset.
     *
     * Local testing (emulator, plain HTTP allowed only for 10.0.2.2/localhost
     * by res/xml/network_security_config.xml):
     *     http://10.0.2.2/deception-monitor/
     * Deployment (HTTPS required for every other host):
     *     https://<domain>/
     */
    public static final String API_BASE_URL = BuildConfig.API_BASE_URL;

    /** Activity-logging endpoint, derived from {@link #API_BASE_URL}. */
    private static final String LOG_ACTIVITY_URL = API_BASE_URL + "api/log_activity.php";

    /**
     * Shared secret sent as the {@code X-Decoy-Key} header on every POST. The
     * backend rejects requests without a matching key (HTTP 401).
     *
     * ROTATE THIS BEFORE DEPLOYMENT — set {@code decoy.apiKey} in
     * local.properties and update the matching value on the server.
     */
    public static final String DECOY_KEY = BuildConfig.DECOY_KEY;

    /** Source tag for every event originating from this app. */
    public static final String SOURCE = "android";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Result of a logging call. */
    public static class ApiResponse {
        /** True if the network request completed (regardless of match result). */
        public boolean networkOk;
        public int httpCode;
        public String rawBody = "";
        /**
         * True if the backend's {@code credential_match} field was true — i.e.
         * the submitted credentials matched a decoy account scoped to this
         * portal. This is the sole authority on login success.
         */
        public boolean credentialMatch;
    }

    public interface ApiCallback {
        void onResult(ApiResponse response);
    }

    /** Fire-and-forget tracking event (page_view, file_interaction, logout, ...). */
    public static void track(Context context, String activityType, String detail) {
        logActivity(context, activityType, null, null, detail, null);
    }

    /**
     * Sends an activity event. If a callback is supplied it is invoked on the
     * main thread once the request finishes.
     */
    public static void logActivity(Context context,
                                   String activityType,
                                   String attemptedUsername,
                                   String attemptedPassword,
                                   String detail,
                                   ApiCallback callback) {

        final String sessionToken = SessionManager.get(context).getSessionToken();
        final String deviceInfo = buildDeviceInfo();

        // Verification aid: prints the token accompanying every event so you can
        // confirm in Logcat (filter by tag "AmanahApi") that one visit reuses a
        // single session_token across all activities.
        Log.d(TAG, "send activity_type=" + activityType + " session_token=" + sessionToken);

        EXECUTOR.execute(() -> {
            ApiResponse response = doPost(sessionToken, activityType,
                    attemptedUsername, attemptedPassword, detail, deviceInfo);
            if (callback != null) {
                MAIN.post(() -> callback.onResult(response));
            }
        });
    }

    private static ApiResponse doPost(String sessionToken,
                                      String activityType,
                                      String attemptedUsername,
                                      String attemptedPassword,
                                      String detail,
                                      String deviceInfo) {
        ApiResponse result = new ApiResponse();
        HttpURLConnection conn = null;
        try {
            StringBuilder body = new StringBuilder();
            appendField(body, "session_token", sessionToken);
            appendField(body, "source", SOURCE);
            appendField(body, "activity_type", activityType);
            appendField(body, "attempted_username", attemptedUsername);
            appendField(body, "attempted_password", attemptedPassword);
            appendField(body, "detail", detail);
            appendField(body, "device_info", deviceInfo);

            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);

            URL url = new URL(LOG_ACTIVITY_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("X-Decoy-Key", DECOY_KEY);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }

            result.httpCode = conn.getResponseCode();
            result.networkOk = result.httpCode > 0;
            result.rawBody = readStream(result.httpCode >= 400
                    ? conn.getErrorStream() : conn.getInputStream());
            result.credentialMatch = parseCredentialMatch(result.rawBody);

            // These are handled silently on purpose: they never change what the
            // user sees or how the app behaves. They only surface in Logcat so
            // the operator can diagnose backend/auth issues.
            if (result.httpCode == HttpURLConnection.HTTP_UNAUTHORIZED) { // 401
                Log.e(TAG, "Backend rejected request: 401 Unauthorized. The "
                        + "X-Decoy-Key header is missing or does not match the "
                        + "server. Update decoy.apiKey in local.properties to the deployed key.");
            } else if (result.httpCode == 429) { // Too Many Requests
                Log.w(TAG, "Backend rate limited request: 429 Too Many Requests. "
                        + "This event was not recorded server-side; backing off is "
                        + "up to the server. App behaviour is unaffected.");
            }

        } catch (Exception e) {
            Log.w(TAG, "logActivity failed: " + e.getMessage());
            result.networkOk = false;
        } finally {
            if (conn != null) conn.disconnect();
        }
        return result;
    }

    /**
     * Reads the backend's {@code credential_match} verdict — the single source
     * of truth for login success.
     *
     * The backend validates the submitted credentials against the
     * decoy_credentials table (the same table the web decoy uses) and scopes the
     * check to accounts whose portal matches this request's source ('android' or
     * 'both'). The app keeps no copy of the decoy list.
     *
     * IMPORTANT: a generic acknowledgement (e.g. {"ok":true} / {"status":
     * "success"}) is NOT a credential match, so we deliberately ignore generic
     * success/ok/status fields here — only the dedicated {@code credential_match}
     * field counts. We fail closed: a non-match, an error page, or no response at
     * all all return false.
     */
    private static boolean parseCredentialMatch(String rawBody) {
        if (rawBody == null || rawBody.trim().isEmpty()) return false;
        try {
            JSONObject json = new JSONObject(rawBody);
            return json.optBoolean("credential_match", false);
        } catch (Exception ignored) {
            // Not JSON (e.g. an HTML error page) — no authoritative match signal.
            return false;
        }
    }

    private static String buildDeviceInfo() {
        return "Android " + Build.VERSION.RELEASE
                + " (SDK " + Build.VERSION.SDK_INT + ") / "
                + Build.MANUFACTURER + " " + Build.MODEL;
    }

    private static void appendField(StringBuilder sb, String key, String value) {
        try {
            if (sb.length() > 0) sb.append('&');
            sb.append(URLEncoder.encode(key, "UTF-8")).append('=')
                    .append(URLEncoder.encode(value == null ? "" : value, "UTF-8"));
        } catch (Exception ignored) {
        }
    }

    private static String readStream(InputStream in) {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }
}
