package com.example.amanahstaffapp;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

/**
 * Residents in the block this nurse covers. Each resident is shown only by
 * resident ID and masked name (see {@link Masking}); no room numbers are shown.
 * Fake names, ages, conditions and medication schedules. Tapping a resident
 * records a 'file_interaction' with the name.
 */
public class PatientsActivity extends BaseActivity {

    @Override
    protected int drawerItem() {
        return R.id.drawer_patients;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patients);
        setupToolbar("My Patients");

        StaffData.Profile profile = StaffData.forUser(session.getUsername());

        ((TextView) findViewById(R.id.text_patients_sub))
                .setText(Masking.occupancy(profile));

        if (savedInstanceState == null) {
            ApiClient.track(this, "page_view", "Viewed assigned Patients list");
        }

        LinearLayout container = findViewById(R.id.patients_container);
        for (String[] p : profile.residents) {
            container.addView(buildPatientCard(p[1], p[0], p[2], p[4], p[5]));
        }
    }

    private MaterialCardView buildPatientCard(String name, String residentId, String demo,
                                              String condition, String medication) {
        String maskedName = Masking.name(name);

        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(14));
        card.setCardElevation(dp(1));
        card.setClickable(true);
        card.setFocusable(true);
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_bg));
        card.setRippleColorResource(R.color.brand_primary_light);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Initial avatar: first letter of the (unmasked) first name
        TextView avatar = new TextView(this);
        avatar.setText(Masking.firstNameInitial(name));
        avatar.setGravity(Gravity.CENTER);
        avatar.setTextColor(ContextCompat.getColor(this, R.color.white));
        avatar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        avatar.setTypeface(avatar.getTypeface(), Typeface.BOLD);
        avatar.setBackgroundResource(R.drawable.bg_avatar_circle);
        LinearLayout.LayoutParams avatarParams =
                new LinearLayout.LayoutParams(dp(44), dp(44));
        avatarParams.rightMargin = dp(14);
        avatar.setLayoutParams(avatarParams);
        inner.addView(avatar);

        // Details
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView nameView = new TextView(this);
        nameView.setText(maskedName);
        nameView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        nameView.setTypeface(nameView.getTypeface(), Typeface.BOLD);
        details.addView(nameView);

        TextView idView = new TextView(this);
        idView.setText(residentId + "  ·  " + demo);
        idView.setTextColor(ContextCompat.getColor(this, R.color.brand_secondary));
        idView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        idView.setTypeface(idView.getTypeface(), Typeface.BOLD);
        idView.setPadding(0, dp(3), 0, 0);
        details.addView(idView);

        TextView condView = new TextView(this);
        condView.setText(condition);
        condView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        condView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        condView.setPadding(0, dp(3), 0, 0);
        details.addView(condView);

        TextView medView = new TextView(this);
        medView.setText("Meds · " + medication);
        medView.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        medView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        medView.setPadding(0, dp(3), 0, 0);
        details.addView(medView);

        inner.addView(details);
        card.addView(inner);

        card.setOnClickListener(v -> {
            ApiClient.track(this, "file_interaction", "Opened patient record: " + name);
            Toast.makeText(this,
                    "Opening care record for " + maskedName + "…",
                    Toast.LENGTH_SHORT).show();
        });

        return card;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
