package com.example.amanahstaffapp;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The nurse's own weekly duty roster at the residential care home, for the
 * current Monday→Sunday week in Malaysia time. Each working day is a shift
 * (morning/afternoon/night) covering one block of the building (Block A–D),
 * shown without room numbers; today's row is highlighted. Fake data.
 */
public class ScheduleActivity extends BaseActivity {

    @Override
    protected int drawerItem() {
        return R.id.drawer_schedule;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        setupToolbar("My Schedule");

        StaffData.Profile profile = StaffData.forUser(session.getUsername());

        Calendar day = StaffData.currentWeekMonday();
        Calendar sunday = (Calendar) day.clone();
        sunday.add(Calendar.DAY_OF_MONTH, 6);

        ((TextView) findViewById(R.id.text_week_range))
                .setText(weekRange(day, sunday) + "  ·  " + Masking.workArea(profile.block));

        if (savedInstanceState == null) {
            ApiClient.track(this, "page_view", "Viewed weekly Schedule");
        }

        SimpleDateFormat dateFmt = format("dd MMM");
        int today = StaffData.todayIndex();
        LinearLayout container = findViewById(R.id.schedule_container);
        for (int i = 0; i < profile.roster.length; i++) {
            String[] row = profile.roster[i];
            container.addView(buildDayCard(row[0], dateFmt.format(day.getTime()),
                    row[1], Masking.workArea(row[2]), i == today));
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    /** "28 Sep – 04 Oct 2026", or "29 Dec 2025 – 04 Jan 2026" across a year end. */
    private static String weekRange(Calendar monday, Calendar sunday) {
        boolean sameYear = monday.get(Calendar.YEAR) == sunday.get(Calendar.YEAR);
        String start = format(sameYear ? "dd MMM" : "dd MMM yyyy").format(monday.getTime());
        return start + " – " + format("dd MMM yyyy").format(sunday.getTime());
    }

    private static SimpleDateFormat format(String pattern) {
        SimpleDateFormat fmt = new SimpleDateFormat(pattern, Locale.ENGLISH);
        fmt.setTimeZone(TimeZone.getTimeZone(StaffData.ZONE_ID));
        return fmt;
    }

    private MaterialCardView buildDayCard(String day, String date, String shift, String area,
                                          boolean isToday) {
        boolean isOff = shift.startsWith("Off") || shift.startsWith("Rest");

        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(10);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(14));
        card.setCardElevation(dp(1));
        if (isToday) {
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.brand_on_primary_soft));
            card.setStrokeColor(ContextCompat.getColor(this, R.color.brand_primary));
            card.setStrokeWidth(dp(2));
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_bg));
        }

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Day badge column: day name, then the date (or "Today · date")
        LinearLayout dayColumn = new LinearLayout(this);
        dayColumn.setOrientation(LinearLayout.VERTICAL);
        dayColumn.setLayoutParams(new LinearLayout.LayoutParams(dp(96),
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView dayView = new TextView(this);
        dayView.setText(day);
        dayView.setTextColor(ContextCompat.getColor(this,
                isOff && !isToday ? R.color.text_muted : R.color.brand_primary));
        dayView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        dayView.setTypeface(dayView.getTypeface(), android.graphics.Typeface.BOLD);
        dayColumn.addView(dayView);

        TextView dateView = new TextView(this);
        dateView.setText(isToday ? "Today · " + date : date);
        dateView.setTextColor(ContextCompat.getColor(this,
                isToday ? R.color.brand_primary : R.color.text_muted));
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        if (isToday) dateView.setTypeface(dateView.getTypeface(), android.graphics.Typeface.BOLD);
        dayColumn.addView(dateView);

        inner.addView(dayColumn);

        // Details column
        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        details.setLayoutParams(detailParams);

        TextView shiftView = new TextView(this);
        shiftView.setText(shift);
        shiftView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        shiftView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        details.addView(shiftView);

        TextView metaView = new TextView(this);
        metaView.setText(area);
        metaView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        metaView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        metaView.setPadding(0, dp(3), 0, 0);
        details.addView(metaView);

        inner.addView(details);
        card.addView(inner);
        return card;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
