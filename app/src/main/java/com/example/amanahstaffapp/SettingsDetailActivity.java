package com.example.amanahstaffapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * One sub-screen of Settings with four uses: Notifications (two switches kept
 * on the phone only), Help & Support, Privacy Notice and About. Each opening
 * is recorded as a 'page_view', and so is each flick of a switch.
 */
public class SettingsDetailActivity extends BaseActivity {

    static final String MODE_NOTIFICATIONS = "notifications";
    static final String MODE_HELP = "help";
    static final String MODE_PRIVACY = "privacy";
    static final String MODE_ABOUT = "about";

    private static final String EXTRA_MODE = "extra_mode";

    static void open(Context context, String mode) {
        context.startActivity(new Intent(context, SettingsDetailActivity.class)
                .putExtra(EXTRA_MODE, mode));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_list);

        String mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) mode = MODE_ABOUT;

        String title = title(mode);
        setupToolbar(title);
        ((TextView) findViewById(R.id.text_list_title)).setText(title);

        if (savedInstanceState == null) {
            ApiClient.track(this, "page_view", "Viewed Settings: " + title);
        }

        TextView sub = findViewById(R.id.text_list_sub);
        LinearLayout card = Cards.addCard(findViewById(R.id.list_container));

        switch (mode) {
            case MODE_NOTIFICATIONS:
                sub.setText(R.string.settings_notifications_note);
                addSwitch(card, R.string.settings_shift_reminders, session.getShiftReminders(),
                        session::setShiftReminders);
                addSwitch(card, R.string.settings_handover_alerts, session.getHandoverAlerts(),
                        session::setHandoverAlerts);
                break;
            case MODE_HELP:
                sub.setText(R.string.settings_help_note);
                Cards.addText(card, getString(R.string.settings_help_contact), 16,
                        R.color.text_primary, true, 0);
                break;
            case MODE_PRIVACY:
                sub.setText(R.string.settings_privacy_summary);
                Cards.addText(card, getString(R.string.settings_privacy_body), 14,
                        R.color.text_primary, false, 0).setLineSpacing(0, 1.2f);
                break;
            default:
                sub.setText(R.string.portal_name);
                Cards.addText(card, getString(R.string.app_name), 16,
                        R.color.text_primary, true, 0);
                Cards.addText(card, getString(R.string.settings_version,
                        BuildConfig.VERSION_NAME), 14, R.color.text_primary, false, 4);
                Cards.addText(card, "© 2026 " + getString(R.string.company_full), 12,
                        R.color.text_muted, false, 8);
                break;
        }
    }

    private String title(String mode) {
        switch (mode) {
            case MODE_NOTIFICATIONS: return getString(R.string.settings_notifications);
            case MODE_HELP:          return getString(R.string.settings_help);
            case MODE_PRIVACY:       return getString(R.string.settings_privacy);
            default:                 return getString(R.string.settings_about);
        }
    }

    private interface Setter {
        void set(boolean on);
    }

    /** A labelled switch whose state is kept on the phone and nowhere else. */
    private void addSwitch(LinearLayout card, int labelRes, boolean checked, Setter setter) {
        String label = getString(labelRes);
        MaterialSwitch toggle = new MaterialSwitch(this);
        toggle.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        toggle.setText(label);
        toggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        toggle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        // The saved setting is applied before the listener is attached, so
        // only a tap is recorded.
        toggle.setChecked(checked);
        toggle.setOnCheckedChangeListener((button, on) -> {
            setter.set(on);
            ApiClient.track(this, "page_view",
                    "Settings: " + label + " turned " + (on ? "on" : "off"));
        });
        card.addView(toggle);
    }
}
