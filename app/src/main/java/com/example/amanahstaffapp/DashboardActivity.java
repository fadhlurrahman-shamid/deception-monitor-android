package com.example.amanahstaffapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Fake staff home screen, a glance view that fits on one phone screen: a
 * greeting, today's shift with the next task, three count tiles (patients,
 * tasks left, new handover notes) and, when one is near, an announcement
 * banner. It shows no resident names; the full lists are on their own screens.
 */
public class DashboardActivity extends BaseActivity {

    public static final String EXTRA_USERNAME = "extra_username";

    @Override
    protected int drawerItem() {
        return R.id.drawer_home;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        setupToolbar("Staff Portal");

        String username = getIntent().getStringExtra(EXTRA_USERNAME);
        if (TextUtils.isEmpty(username)) {
            username = session.getUsername();
        }

        // Recorded once per opening, not again when the screen is rebuilt
        // (e.g. on rotation).
        if (savedInstanceState == null) {
            ApiClient.track(this, "page_view", "Viewed Dashboard as '" + username + "'");
        }

        findViewById(R.id.tile_patients).setOnClickListener(v ->
                startActivity(new Intent(this, PatientsActivity.class)));
        findViewById(R.id.tile_tasks).setOnClickListener(v ->
                InfoListActivity.open(this, InfoListActivity.MODE_TASKS));
        findViewById(R.id.tile_handover).setOnClickListener(v ->
                InfoListActivity.open(this, InfoListActivity.MODE_HANDOVER));
        findViewById(R.id.card_banner).setOnClickListener(v ->
                InfoListActivity.open(this, InfoListActivity.MODE_ANNOUNCEMENTS));
    }

    /** Choosing Home in the drawer of another screen returns here: one more view. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        ApiClient.track(this, "page_view",
                "Viewed Dashboard as '" + session.getUsername() + "'");
    }

    /**
     * Everything on Home follows the clock, so it is refilled on every return
     * to the screen, not only when it is first opened: the greeting, the date,
     * today's shift (computed live from the roster in Malaysia time, see
     * StaffData.Profile#currentShift), the tiles and the banner.
     */
    @Override
    protected void onResume() {
        super.onResume();
        StaffData.Profile profile = StaffData.forUser(session.getUsername());
        Calendar now = StaffData.nowInMalaysia();

        ((TextView) findViewById(R.id.text_greeting)).setText(
                greeting(now) + ", " + Masking.firstName(profile.displayName));

        // Today's date in Malaysia (Asia/Kuala_Lumpur), in English, so the
        // header day matches the timezone the shift is calculated in.
        SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH);
        dateFmt.setTimeZone(TimeZone.getTimeZone(StaffData.ZONE_ID));
        ((TextView) findViewById(R.id.text_date)).setText(dateFmt.format(now.getTime()));

        ((TextView) findViewById(R.id.text_shift)).setText(profile.currentShift(now));
        ((TextView) findViewById(R.id.text_shift_rooms)).setText(Masking.workArea(profile.block));

        List<CareData.Round> rounds = CareData.todaysRounds(profile, now);
        ((TextView) findViewById(R.id.text_next_task)).setText(nextTask(profile, now, rounds));

        int tasksLeft = 0;
        for (CareData.Round round : rounds) {
            if (!round.done) tasksLeft++;
        }
        ((TextView) findViewById(R.id.text_tile_patients))
                .setText(String.valueOf(profile.residentCount));
        ((TextView) findViewById(R.id.text_tile_tasks)).setText(String.valueOf(tasksLeft));
        ((TextView) findViewById(R.id.text_tile_handover)).setText(String.valueOf(
                CareData.newHandoverCount(profile, now, session.getHandoverOpenedAt())));

        String banner = CareData.upcomingBanner(now);
        findViewById(R.id.card_banner).setVisibility(banner == null ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.text_banner)).setText(banner == null ? "" : banner);
    }

    @Override
    protected boolean showsNotificationBell() {
        return true;
    }

    private static String greeting(Calendar now) {
        int hour = now.get(Calendar.HOUR_OF_DAY);
        return hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
    }

    /** The last line of the shift card: the next round, or why there is none. */
    private static String nextTask(StaffData.Profile profile, Calendar now,
                                   List<CareData.Round> rounds) {
        char shift = profile.currentShiftCode(now);
        if (shift == StaffData.REST) {
            return "Rest day. Next shift: " + profile.nextShift(now);
        }
        if (shift == StaffData.NIGHT && rounds.isEmpty()) {
            return CareData.noTasksMessage(profile, now);
        }
        for (CareData.Round round : rounds) {
            if (!round.done) {
                return "Next: " + round.time + " " + round.name + " · "
                        + CareData.residentCount(round);
            }
        }
        return "All tasks completed for this shift";
    }
}
