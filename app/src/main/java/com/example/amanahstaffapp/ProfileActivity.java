package com.example.amanahstaffapp;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/**
 * The nurse's own staff record, read-only: masked name, staff ID, position,
 * block and masked IC. There are no phone numbers and nothing can be edited -
 * the screen says personal details are managed by HR. Fake data.
 */
public class ProfileActivity extends BaseActivity {

    @Override
    protected int drawerItem() {
        return R.id.drawer_profile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setupToolbar("My Profile");

        if (savedInstanceState == null) {
            ApiClient.track(this, "page_view", "Viewed My Profile");
        }

        StaffData.Profile profile = StaffData.forUser(session.getUsername());

        ((TextView) findViewById(R.id.text_profile_initial))
                .setText(Masking.firstNameInitial(profile.displayName));
        ((TextView) findViewById(R.id.text_profile_name))
                .setText(Masking.name(profile.displayName));
        ((TextView) findViewById(R.id.text_profile_position)).setText(profile.position);

        LinearLayout container = findViewById(R.id.profile_container);
        addRow(container, "Staff ID", profile.staffId, false);
        addRow(container, "Position", profile.position, true);
        addRow(container, "Block", Masking.workArea(profile.block), true);
        addRow(container, "IC number", profile.maskedIc, true);
    }

    private void addRow(LinearLayout container, String label, String value, boolean divider) {
        if (divider) {
            View line = new View(this);
            line.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Cards.dp(this, 1)));
            line.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
            container.addView(line);
        }
        Cards.addText(container, label, 12, R.color.text_muted, false, 12);
        Cards.addText(container, value, 15, R.color.text_primary, false, 2)
                .setPadding(0, Cards.dp(this, 2), 0, Cards.dp(this, 12));
    }
}
