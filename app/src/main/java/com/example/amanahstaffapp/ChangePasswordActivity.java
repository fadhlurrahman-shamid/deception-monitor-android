package com.example.amanahstaffapp;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

/**
 * A Change Password form that changes nothing, opened from Settings. The form
 * is validated on the phone like a real one; a submission that passes is told
 * that password changes need the TLC-Staff Wi-Fi. Every submit, passed or
 * failed, is recorded through the API as a 'page_view' with its outcome. The
 * typed passwords are never stored and never sent.
 */
public class ChangePasswordActivity extends BaseActivity {

    private TextInputEditText currentInput;
    private TextInputEditText newInput;
    private TextInputEditText confirmInput;
    private TextView message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        setupToolbar("Change Password");

        if (savedInstanceState == null) {
            ApiClient.track(this, "page_view", "Viewed Change Password");
        }

        StaffData.Profile profile = StaffData.forUser(session.getUsername());
        ((TextView) findViewById(R.id.text_settings_account))
                .setText(profile.staffId + "  ·  " + Masking.name(profile.displayName));

        currentInput = findViewById(R.id.input_current_password);
        newInput = findViewById(R.id.input_new_password);
        confirmInput = findViewById(R.id.input_confirm_password);
        message = findViewById(R.id.text_password_message);

        findViewById(R.id.btn_change_password).setOnClickListener(v -> submit());
    }

    private void submit() {
        String current = text(currentInput);
        String fresh = text(newInput);
        String confirm = text(confirmInput);

        // Checked on the phone, in this order, and the first problem is shown.
        // The current password is only compared with the new one; whether it
        // is right is never checked.
        if (current.isEmpty() || fresh.isEmpty() || confirm.isEmpty()) {
            reject(R.string.password_empty, "a field is empty");
            return;
        }
        if (!fresh.equals(confirm)) {
            reject(R.string.password_mismatch, "passwords do not match");
            return;
        }
        if (isWeak(fresh)) {
            reject(R.string.password_weak, "password too weak");
            return;
        }
        if (fresh.equals(current)) {
            reject(R.string.password_same, "same as current password");
            return;
        }

        record("passed checks, not available on this network");

        currentInput.setText("");
        newInput.setText("");
        confirmInput.setText("");
        message.setVisibility(View.GONE);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.password_unavailable_title)
                .setMessage(R.string.password_unavailable_body)
                .setPositiveButton(R.string.action_ok, null)
                .show();
    }

    private static String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    /** Shorter than 8 characters, or missing a letter or a number. */
    private static boolean isWeak(String password) {
        boolean letter = false;
        boolean digit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        return password.length() < 8 || !letter || !digit;
    }

    /** A failed check: recorded with its reason, then shown under the fields. */
    private void reject(int messageRes, String reason) {
        record(reason);
        message.setText(messageRes);
        message.setVisibility(View.VISIBLE);
    }

    /**
     * Every submit is recorded once, with the outcome in the detail and the
     * account it was made from. The typed passwords are never sent. It is a
     * page_view, never a login_attempt, so it is not checked against the
     * decoy accounts and does not count as a failed login.
     */
    private void record(String outcome) {
        ApiClient.logActivity(this, "page_view", session.getUsername(), null,
                "Change password attempt: " + outcome, null);
    }
}
