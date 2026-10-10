package com.example.amanahstaffapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Fake staff login. Records every attempt and only advances when the backend
 * reports the credentials matched a seeded decoy account. It never blocks,
 * rate-limits or locks out — a failed attempt just shows an ordinary error.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private ProgressBar progress;
    private TextView errorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // The login page has a light background, so use dark status-bar icons
        // (clock/battery/signal) here for a clean, readable status bar. The inner
        // screens keep the default light icons on the plum app bar.
        WindowInsetsControllerCompat statusIcons =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        statusIcons.setAppearanceLightStatusBars(true);

        // Ensure the persistent session token exists from first launch.
        SessionManager.get(this).getSessionToken();

        usernameInput = findViewById(R.id.input_username);
        passwordInput = findViewById(R.id.input_password);
        loginButton = findViewById(R.id.btn_login);
        progress = findViewById(R.id.login_progress);
        errorText = findViewById(R.id.text_login_error);

        loginButton.setOnClickListener(v -> attemptLogin());

        // Nothing is reset from here: the tap is recorded and the visitor is
        // pointed at the helpdesk.
        findViewById(R.id.text_forgot_password).setOnClickListener(v -> {
            ApiClient.track(this, "page_view", "Tapped Forgot password");
            new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.forgot_password_message)
                    .setPositiveButton(R.string.action_ok, null)
                    .show();
        });
    }

    private void attemptLogin() {
        final String username = usernameInput.getText() == null
                ? "" : usernameInput.getText().toString().trim();
        final String password = passwordInput.getText() == null
                ? "" : passwordInput.getText().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            showError(R.string.login_empty);
            return;
        }

        clearError();
        setLoading(true);

        // Always record the attempt first, then decide navigation from the result.
        ApiClient.logActivity(this, "login_attempt", username, password,
                "Staff login attempt", response -> {
                    setLoading(false);

                    // The backend is the single source of truth: it validates the
                    // submitted credentials against the decoy_credentials table
                    // (scoped to accounts whose portal covers Android) and reports
                    // credential_match. We fail closed — if no authoritative match
                    // verdict comes back (wrong credentials, or the request failed:
                    // no network, timeout, rate-limited, or a rejected key), it
                    // counts as a non-match. A generic acknowledgement never counts.
                    //
                    // A network failure therefore looks identical to a wrong
                    // password — the ordinary "Invalid username or password" error,
                    // staying on the login screen — and can never grant access.
                    if (response.credentialMatch) {
                        // The backend already recorded login_success when it
                        // returned credential_match=true on the request above. We
                        // do NOT post the outcome back — that second POST carried
                        // empty credentials, so the server downgraded it to a
                        // login_attempt and inflated failed_attempts. The server
                        // owns the verdict; the app only navigates on it.
                        SessionManager.get(this).setUsername(username);
                        openDashboard(username);
                    } else {
                        // Ordinary failure — never block, rate-limit or lock out.
                        // Show a plain "invalid credentials" message and clear both
                        // fields (as the web decoy and typical login forms do),
                        // returning focus to the username field. Nothing here hints
                        // that the attempt was recorded.
                        showError(R.string.login_error);
                        usernameInput.setText("");
                        passwordInput.setText("");
                        usernameInput.requestFocus();
                    }
                });
    }

    private void openDashboard(String username) {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra(DashboardActivity.EXTRA_USERNAME, username);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
    }

    /** Shows an ordinary inline error under the fields (like any login form). */
    private void showError(int messageRes) {
        errorText.setText(messageRes);
        errorText.setVisibility(View.VISIBLE);
    }

    /** Hides the inline error when a fresh attempt starts. */
    private void clearError() {
        errorText.setText("");
        errorText.setVisibility(View.GONE);
    }
}
