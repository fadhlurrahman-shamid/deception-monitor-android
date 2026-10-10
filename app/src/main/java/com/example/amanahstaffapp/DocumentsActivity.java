package com.example.amanahstaffapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

/**
 * Company documents. Lists only the files a staff nurse can open; every card
 * opens a read-only in-app preview (DocumentViewerActivity), which records the
 * 'file_interaction' event.
 */
public class DocumentsActivity extends BaseActivity {

    // filename, size, meta
    private static final String[][] DOCUMENTS = {
            {DocumentViewerActivity.DOC_HANDBOOK, "2.4 MB", "Updated 03 Feb 2026"},
            {DocumentViewerActivity.DOC_PAYSLIP, "312 KB", "Generated 28 Aug 2026"},
            {DocumentViewerActivity.DOC_SOP, "3.1 MB", "Updated 12 Jun 2026"},
    };

    @Override
    protected int drawerItem() {
        return R.id.drawer_documents;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);
        setupToolbar("Documents");

        if (savedInstanceState == null) {
            ApiClient.track(this, "page_view", "Viewed company Documents");
        }

        LinearLayout container = findViewById(R.id.documents_container);
        for (String[] doc : DOCUMENTS) {
            container.addView(buildDocCard(doc));
        }
    }

    private MaterialCardView buildDocCard(String[] doc) {
        final String filename = doc[0];
        final String size = doc[1];
        final String meta = doc[2];

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

        // PDF badge
        TextView badge = new TextView(this);
        badge.setText("PDF");
        badge.setGravity(Gravity.CENTER);
        badge.setTextColor(ContextCompat.getColor(this, R.color.white));
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        badge.setTypeface(badge.getTypeface(), Typeface.BOLD);
        badge.setBackgroundResource(R.drawable.bg_pdf_badge);
        LinearLayout.LayoutParams badgeParams =
                new LinearLayout.LayoutParams(dp(44), dp(44));
        badgeParams.rightMargin = dp(14);
        badge.setLayoutParams(badgeParams);
        inner.addView(badge);

        // Details
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView nameView = new TextView(this);
        nameView.setText(filename);
        nameView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        nameView.setTypeface(nameView.getTypeface(), Typeface.BOLD);
        details.addView(nameView);

        TextView metaView = new TextView(this);
        metaView.setText(size + "  ·  " + meta);
        metaView.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        metaView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        metaView.setPadding(0, dp(3), 0, 0);
        details.addView(metaView);

        inner.addView(details);
        card.addView(inner);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, DocumentViewerActivity.class);
            intent.putExtra(DocumentViewerActivity.EXTRA_FILENAME, filename);
            startActivity(intent);
        });

        return card;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
