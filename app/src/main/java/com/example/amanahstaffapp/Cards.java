package com.example.amanahstaffapp;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

/**
 * Small view builders shared by the read-only information screens, in the same
 * card style as the schedule and patient lists.
 */
final class Cards {

    /** Adds a white rounded card to the container and returns its content column. */
    static LinearLayout addCard(LinearLayout container) {
        Context context = container.getContext();

        MaterialCardView card = new MaterialCardView(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(context, 10);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(context, 14));
        card.setCardElevation(dp(context, 1));
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_bg));

        LinearLayout inner = new LinearLayout(context);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16));
        card.addView(inner);

        container.addView(card);
        return inner;
    }

    /** Adds one line of text to a column and returns it. */
    static TextView addText(LinearLayout column, String text, int sizeSp, int colorRes,
                            boolean bold, int topPaddingDp) {
        Context context = column.getContext();
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(context, colorRes));
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        if (bold) view.setTypeface(view.getTypeface(), Typeface.BOLD);
        view.setPadding(0, dp(context, topPaddingDp), 0, 0);
        column.addView(view);
        return view;
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private Cards() {}
}
