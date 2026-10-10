package com.example.amanahstaffapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

/**
 * One read-only list screen with four uses: the full Handover Notes, Today's
 * Tasks and Announcements reached from the drawer and from Home, and the
 * Notifications list behind the toolbar bell. Each opening is recorded as a 'page_view'.
 * Fake data from {@link CareData}; nothing on this screen can be changed.
 */
public class InfoListActivity extends BaseActivity {

    static final String MODE_HANDOVER = "handover";
    static final String MODE_TASKS = "tasks";
    static final String MODE_ANNOUNCEMENTS = "announcements";
    static final String MODE_NOTIFICATIONS = "notifications";

    private static final String EXTRA_MODE = "extra_mode";

    static Intent intent(Context context, String mode) {
        return new Intent(context, InfoListActivity.class).putExtra(EXTRA_MODE, mode);
    }

    static void open(Context context, String mode) {
        context.startActivity(intent(context, mode));
    }

    private String mode() {
        String mode = getIntent().getStringExtra(EXTRA_MODE);
        return mode == null ? MODE_NOTIFICATIONS : mode;
    }

    /** The three lists are main screens; Notifications is a sub-screen of the bell. */
    @Override
    protected int drawerItem() {
        switch (mode()) {
            case MODE_HANDOVER:      return R.id.drawer_handover;
            case MODE_TASKS:         return R.id.drawer_tasks;
            case MODE_ANNOUNCEMENTS: return R.id.drawer_announcements;
            default:                 return 0;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_list);

        String mode = mode();

        String title = title(mode);
        setupToolbar(title);
        ((TextView) findViewById(R.id.text_list_title)).setText(title);

        // Recorded once per opening, not again when the screen is rebuilt
        // (e.g. on rotation).
        if (savedInstanceState == null) {
            ApiClient.track(this, "page_view", "Viewed " + title);
        }

        StaffData.Profile profile = StaffData.forUser(session.getUsername());
        Calendar now = StaffData.nowInMalaysia();
        String area = Masking.workArea(profile.block);
        TextView sub = findViewById(R.id.text_list_sub);
        LinearLayout container = findViewById(R.id.list_container);

        switch (mode) {
            case MODE_HANDOVER:
                sub.setText(CareData.previousShiftLabel(now) + "  ·  " + area);
                // Opening the list marks its notes as seen: only this moment
                // is stored, and Home counts what is written after it.
                session.setHandoverOpenedAt(now.getTimeInMillis());
                for (CareData.Note note : CareData.handoverNotes(profile, now)) {
                    addNote(container, note, true);
                }
                break;
            case MODE_TASKS:
                sub.setText(profile.currentShift(now) + "  ·  " + area);
                addRounds(container, profile, now);
                break;
            case MODE_ANNOUNCEMENTS:
                sub.setText("Management notices for nursing staff");
                addAnnouncements(container, now);
                break;
            default:
                // Opening the list is what marks its items as seen: only this
                // moment is stored, and the bell counts what is posted after it.
                int fresh = CareData.newNotificationCount(profile, now,
                        session.getNotificationsOpenedAt());
                sub.setText(fresh > 0 ? fresh + " new" : "No new notifications");
                session.setNotificationsOpenedAt(now.getTimeInMillis());
                addAnnouncements(container, now);
                addNote(container, CareData.newestNote(profile, now), false);
                break;
        }
    }

    private static String title(String mode) {
        switch (mode) {
            case MODE_HANDOVER:      return "Handover Notes";
            case MODE_TASKS:         return "Today's Tasks";
            case MODE_ANNOUNCEMENTS: return "Announcements";
            default:                 return "Notifications";
        }
    }

    /** A handover note: masked name, resident ID and time, the note, then the chart line. */
    private void addNote(LinearLayout container, CareData.Note note, boolean withChart) {
        LinearLayout card = Cards.addCard(container);
        if (!withChart) {
            Cards.addText(card, "Handover note", 12, R.color.text_muted, false, 0);
        }
        Cards.addText(card, Masking.name(note.residentName), 16, R.color.text_primary, true,
                withChart ? 0 : 3);
        Cards.addText(card, note.residentId + "  ·  " + note.time, 13,
                R.color.brand_secondary, true, 3);
        Cards.addText(card, note.text, 13, R.color.text_primary, false, 6);
        if (withChart) {
            Cards.addText(card, note.conditions, 12, R.color.text_secondary, false, 6);
            Cards.addText(card, "Meds · " + CareData.medicationSummary(note.conditions), 12,
                    R.color.text_muted, false, 3);
        }
    }

    /** One card per medication round, listing each resident with a dose due. */
    private void addRounds(LinearLayout container, StaffData.Profile profile, Calendar now) {
        List<CareData.Round> rounds = CareData.todaysRounds(profile, now);
        if (rounds.isEmpty()) {
            LinearLayout card = Cards.addCard(container);
            Cards.addText(card, CareData.noTasksMessage(profile, now), 15,
                    R.color.text_secondary, false, 0);
            return;
        }
        for (CareData.Round round : rounds) {
            LinearLayout card = Cards.addCard(container);
            Cards.addText(card, round.time + "  ·  " + round.name, 16,
                    R.color.text_primary, true, 0);
            Cards.addText(card, CareData.residentCount(round) + "  ·  "
                            + (round.done ? "Completed" : "Upcoming"), 13,
                    R.color.brand_secondary, true, 3);
            for (String[] resident : round.residents) {
                Cards.addText(card, resident[0] + "  ·  " + Masking.name(resident[1]), 13,
                        R.color.text_primary, false, 8);
                Cards.addText(card, resident[2], 12, R.color.text_muted, false, 1);
            }
        }
    }

    private void addAnnouncements(LinearLayout container, Calendar now) {
        for (CareData.Announcement notice : CareData.announcements(now)) {
            LinearLayout card = Cards.addCard(container);
            Cards.addText(card, "Announcement", 12, R.color.text_muted, false, 0);
            Cards.addText(card, notice.title, 16, R.color.text_primary, true, 3);
            Cards.addText(card, notice.text, 13, R.color.text_primary, false, 6);
        }
    }
}
