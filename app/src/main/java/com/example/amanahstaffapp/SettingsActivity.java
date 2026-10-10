package com.example.amanahstaffapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

/**
 * Settings, as a list: Account (Change Password), Notifications, Help &
 * Support, Privacy Notice, About and Log Out. Every row but Log Out opens a
 * sub-screen; nothing here changes anything outside the phone.
 */
public class SettingsActivity extends BaseActivity {

    @Override
    protected int drawerItem() {
        return R.id.drawer_settings;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupToolbar("Settings");

        if (savedInstanceState == null) {
            ApiClient.track(this, "page_view", "Viewed Settings");
        }

        LinearLayout container = findViewById(R.id.settings_container);
        addRow(container, getString(R.string.settings_account),
                getString(R.string.action_change_password),
                () -> startActivity(new Intent(this, ChangePasswordActivity.class)));
        addRow(container, getString(R.string.settings_notifications),
                getString(R.string.settings_notifications_summary),
                () -> SettingsDetailActivity.open(this, SettingsDetailActivity.MODE_NOTIFICATIONS));
        addRow(container, getString(R.string.settings_help),
                getString(R.string.settings_help_contact),
                () -> SettingsDetailActivity.open(this, SettingsDetailActivity.MODE_HELP));
        addRow(container, getString(R.string.settings_privacy),
                getString(R.string.settings_privacy_summary),
                () -> SettingsDetailActivity.open(this, SettingsDetailActivity.MODE_PRIVACY));
        addRow(container, getString(R.string.settings_about),
                getString(R.string.settings_version, BuildConfig.VERSION_NAME),
                () -> SettingsDetailActivity.open(this, SettingsDetailActivity.MODE_ABOUT));
        addRow(container, getString(R.string.action_logout), null, this::logout);
    }

    /** One tappable row: a title, an optional line under it, a divider above. */
    private void addRow(LinearLayout container, String title, String detail, Runnable action) {
        if (container.getChildCount() > 0) {
            View line = new View(this);
            line.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Cards.dp(this, 1)));
            line.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
            container.addView(line);
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(Cards.dp(this, 18), Cards.dp(this, 14), Cards.dp(this, 18),
                Cards.dp(this, 14));
        TypedValue ripple = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        row.setBackgroundResource(ripple.resourceId);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> action.run());

        Cards.addText(row, title, 16, detail == null ? R.color.status_error : R.color.text_primary,
                true, 0);
        if (detail != null) {
            Cards.addText(row, detail, 13, R.color.text_muted, false, 2);
        }
        container.addView(row);
    }
}
