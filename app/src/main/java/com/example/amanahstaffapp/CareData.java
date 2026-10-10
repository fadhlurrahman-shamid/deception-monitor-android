package com.example.amanahstaffapp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

/**
 * Fake day-to-day content for Home, the information screens and the
 * notification list: handover notes, today's medication rounds and the
 * internal staff announcements.
 *
 * Every clinical fact is copied from the web decoy
 * (admin/includes/decoy_data.php): the four medication rounds
 * ($TLC_MED_ROUNDS), what each condition is treated with ($TLC_MEDICATIONS),
 * which rounds a frequency falls on ($TLC_MED_FREQUENCIES) and the shift
 * windows ($TLC_SHIFTS). A resident's medication is worked out from the
 * condition text in {@link StaffData}, the same way the web chart does it.
 *
 * Everything follows the real clock in Malaysia time and is generated inside
 * the app: the notes change at every shift change, the tasks follow today's
 * roster line and the announcements carry dates worked out from today. The
 * same moment always produces the same content. Nothing here is stored, sent
 * or changed.
 */
final class CareData {

    // ---- Announcements ------------------------------------------------------

    /** One management notice, with the moment it went up on the board. */
    static final class Announcement {
        final String title;
        final String text;
        final long postedAt; // epoch millis
        /** Midnight of the day the notice is about, or 0 when it has no single day. */
        final long dueDay;
        final String dueTime; // "14:00", or "" when the notice has no time

        Announcement(String title, String text, long postedAt, long dueDay, String dueTime) {
            this.title = title;
            this.text = text;
            this.postedAt = postedAt;
            this.dueDay = dueDay;
            this.dueTime = dueTime;
        }
    }

    /**
     * Internal notices for nursing staff, with their dates worked out from
     * today: the timesheet cut-off on the next 25th, the infection control
     * refresher on the coming Wednesday at 14:00 and this week's flu
     * vaccination. They only concern matters inside the care home; nothing
     * here is about the system itself or anything visible from outside.
     */
    static List<Announcement> announcements(Calendar now) {
        List<Announcement> notices = new ArrayList<>();

        // Payroll cut-off: the 25th of this month, or of next month once it
        // has passed. The notice for a cut-off goes up the day after the last.
        Calendar cutOff = startOfDay(now);
        if (cutOff.get(Calendar.DAY_OF_MONTH) > 25) cutOff.add(Calendar.MONTH, 1);
        cutOff.set(Calendar.DAY_OF_MONTH, 25);
        Calendar cutOffPosted = (Calendar) cutOff.clone();
        cutOffPosted.add(Calendar.MONTH, -1);
        cutOffPosted.add(Calendar.DAY_OF_MONTH, 1);
        long daysLeft = daysUntil(now, cutOff.getTimeInMillis());
        String left = daysLeft == 0 ? "today"
                : daysLeft == 1 ? "1 day left" : daysLeft + " days left";
        notices.add(new Announcement("Payroll cut-off",
                "Staff timesheets must be approved by " + format("dd MMMM yyyy", cutOff)
                        + " (" + left + ").",
                cutOffPosted.getTimeInMillis(), cutOff.getTimeInMillis(), ""));

        // Infection control refresher: the coming Wednesday, 14:00. On a
        // Wednesday it is still today's until it starts. The notice for a
        // session goes up when the one before it starts.
        int today = StaffData.mondayIndex(now.get(Calendar.DAY_OF_WEEK));
        Calendar refresher = startOfDay(now);
        refresher.add(Calendar.DAY_OF_MONTH, (2 - today + 7) % 7);
        refresher.set(Calendar.HOUR_OF_DAY, 14);
        if (!now.before(refresher)) refresher.add(Calendar.DAY_OF_MONTH, 7);
        Calendar refresherPosted = (Calendar) refresher.clone();
        refresherPosted.add(Calendar.DAY_OF_MONTH, -7);
        notices.add(new Announcement("Infection control refresher",
                "Refresher session on " + format("EEEE, dd MMMM yyyy", refresher)
                        + ", 14:00, in the training room.",
                refresherPosted.getTimeInMillis(),
                startOfDay(refresher).getTimeInMillis(), "14:00"));

        // Staff flu vaccination: on offer all of this Monday-to-Sunday week,
        // so it has no single day. The notice goes up as the week starts.
        Calendar monday = startOfDay(now);
        monday.add(Calendar.DAY_OF_MONTH, -today);
        Calendar sunday = (Calendar) monday.clone();
        sunday.add(Calendar.DAY_OF_MONTH, 6);
        notices.add(new Announcement("Staff flu vaccination",
                "Available at the nurse station this week (" + format("dd MMM", monday)
                        + " – " + format("dd MMM yyyy", sunday) + ").",
                monday.getTimeInMillis(), 0, ""));

        return notices;
    }

    /**
     * The Home banner: the announcement whose day is today, tomorrow or the
     * day after, e.g. "Infection control refresher · tomorrow, 14:00". Null
     * when no announcement is that near, so Home shows nothing.
     */
    static String upcomingBanner(Calendar now) {
        Announcement soonest = null;
        int near = 0;
        for (Announcement notice : announcements(now)) {
            if (notice.dueDay == 0) continue;
            long days = daysUntil(now, notice.dueDay);
            if (days < 0 || days > 2) continue;
            near++;
            if (soonest == null || notice.dueDay < soonest.dueDay) soonest = notice;
        }
        if (soonest == null) return null;

        long days = daysUntil(now, soonest.dueDay);
        Calendar day = (Calendar) now.clone();
        day.setTimeInMillis(soonest.dueDay);
        String when = days == 0 ? "today" : days == 1 ? "tomorrow" : format("EEEE", day);
        if (!soonest.dueTime.isEmpty()) when += ", " + soonest.dueTime;
        return soonest.title + " · " + when + (near > 1 ? " · +" + (near - 1) + " more" : "");
    }

    /**
     * The bell's count: how many items on the Notifications list (the
     * announcements and the newest handover note) went up after the list was
     * last opened. A last-opened time of 0, or one that is ahead of the clock,
     * means it has not been opened.
     */
    static int newNotificationCount(StaffData.Profile profile, Calendar now, long lastOpenedAt) {
        if (lastOpenedAt > now.getTimeInMillis()) lastOpenedAt = 0;
        int count = 0;
        for (Announcement notice : announcements(now)) {
            if (notice.postedAt > lastOpenedAt) count++;
        }
        if (newestNote(profile, now).postedAt > lastOpenedAt) count++;
        return count;
    }

    // ---- Medication ---------------------------------------------------------

    /** The four medication rounds: { time, name }. None fall on the night shift. */
    private static final String[][] MED_ROUNDS = {
            {"08:00", "Morning round"},
            {"11:00", "Midday round"},
            {"17:00", "Evening round"},
            {"21:00", "Bedtime round"},
    };

    /** Condition → medication lines: { condition, medication, dose, frequency }. */
    private static final String[][] MEDICATIONS = {
            {"type 2 diabetes",      "Metformin",            "500 mg",      "BD"},
            {"hypertension",         "Amlodipine",           "5 mg",        "OD"},
            {"alzheimer's disease",  "Donepezil",            "10 mg",       "ON"},
            {"post-stroke care",     "Aspirin",              "100 mg",      "OD"},
            {"post-stroke care",     "Atorvastatin",         "20 mg",       "ON"},
            {"parkinson's disease",  "Levodopa/Carbidopa",   "100/25 mg",   "TDS"},
            {"heart failure",        "Furosemide",           "40 mg",       "OM"},
            {"heart failure",        "Bisoprolol",           "2.5 mg",      "OD"},
            {"copd",                 "Tiotropium",           "18 mcg",      "OD"},
            {"copd",                 "Salbutamol",           "2 puffs",     "PRN"},
            {"vascular dementia",    "Aspirin",              "100 mg",      "OD"},
            {"vascular dementia",    "Atorvastatin",         "20 mg",       "ON"},
            {"osteoporosis",         "Alendronate",          "70 mg",       "Weekly (Mon)"},
            {"osteoporosis",         "Calcium + Vitamin D3", "1 tab",       "OD"},
            {"dementia",             "Rivastigmine patch",   "4.6 mg/24 h", "OD"},
            {"osteoarthritis",       "Paracetamol",          "1 g",         "PRN"},
            {"neuropathy",           "Pregabalin",           "75 mg",       "BD"},
            {"rheumatoid arthritis", "Methotrexate",         "7.5 mg",      "Weekly (Mon)"},
            {"rheumatoid arthritis", "Folic acid",           "5 mg",        "Weekly (Thu)"},
            {"anxiety",              "Sertraline",           "50 mg",       "OM"},
    };

    /**
     * Is a frequency given on this round, on this day (Mon=0..Sun=6)? PRN has
     * no fixed round, and a weekly dose is only due on its own weekday.
     */
    private static boolean dueOn(String frequency, String roundTime, int dayIndex) {
        switch (frequency) {
            case "OD":
            case "OM":           return roundTime.equals("08:00");
            case "ON":           return roundTime.equals("21:00");
            case "BD":           return roundTime.equals("08:00") || roundTime.equals("17:00");
            case "TDS":          return !roundTime.equals("21:00");
            case "Weekly (Mon)": return dayIndex == 0 && roundTime.equals("08:00");
            case "Weekly (Thu)": return dayIndex == 3 && roundTime.equals("08:00");
            default:             return false;
        }
    }

    /** "Metformin 500 mg BD · Amlodipine 5 mg OD" for a resident's condition text. */
    static String medicationSummary(String conditions) {
        StringBuilder out = new StringBuilder();
        for (String condition : conditions.split(",")) {
            String key = condition.trim().toLowerCase(Locale.ROOT);
            for (String[] med : MEDICATIONS) {
                if (!med[0].equals(key)) continue;
                if (out.length() > 0) out.append(" · ");
                out.append(med[1]).append(' ').append(med[2]).append(' ').append(med[3]);
            }
        }
        return out.toString();
    }

    // ---- Today's tasks ------------------------------------------------------

    /** One medication round on the nurse's shift. */
    static final class Round {
        final String time;   // "08:00"
        final String name;   // "Morning round"
        final boolean done;  // the round's time has already passed
        /** One row per resident with a dose due: { resident ID, full name, doses }. */
        final List<String[]> residents = new ArrayList<>();

        Round(String time, String name, boolean done) {
            this.time = time;
            this.name = name;
            this.done = done;
        }
    }

    /**
     * The medication rounds for the nurse's block on today's date, during the
     * shift the roster gives her today (the one the dashboard is showing).
     * Empty on a rest day, and on a night shift, which has no round (PRN only).
     */
    static List<Round> todaysRounds(StaffData.Profile profile, Calendar now) {
        List<Round> rounds = new ArrayList<>();
        char shift = profile.currentShiftCode(now);
        int dayIndex = StaffData.mondayIndex(now.get(Calendar.DAY_OF_WEEK));
        int nowMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        for (String[] r : MED_ROUNDS) {
            int hour = Integer.parseInt(r[0].substring(0, 2));
            boolean inShift = shift == StaffData.MORNING ? (hour >= 7 && hour < 15)
                    : shift == StaffData.AFTERNOON && (hour >= 15 && hour < 23);
            if (!inShift) continue;

            Round round = new Round(r[0], r[1], nowMinute >= hour * 60);
            for (String[] resident : profile.residents) {
                String doses = dosesDue(resident[4], r[0], dayIndex);
                if (!doses.isEmpty()) {
                    round.residents.add(new String[]{resident[0], resident[1], doses});
                }
            }
            if (!round.residents.isEmpty()) rounds.add(round);
        }
        return rounds;
    }

    /** "5 residents" / "1 resident" for a round. */
    static String residentCount(Round round) {
        int count = round.residents.size();
        return count + (count == 1 ? " resident" : " residents");
    }

    /** What the Today's Tasks card says when there is no round to list. */
    static String noTasksMessage(StaffData.Profile profile, Calendar now) {
        return profile.currentShiftCode(now) == StaffData.NIGHT
                ? "No medication rounds on the night shift - PRN only"
                : "Rest day - no tasks";
    }

    private static String dosesDue(String conditions, String roundTime, int dayIndex) {
        StringBuilder out = new StringBuilder();
        for (String condition : conditions.split(",")) {
            String key = condition.trim().toLowerCase(Locale.ROOT);
            for (String[] med : MEDICATIONS) {
                if (!med[0].equals(key) || !dueOn(med[3], roundTime, dayIndex)) continue;
                if (out.length() > 0) out.append(", ");
                out.append(med[1]).append(' ').append(med[2]);
            }
        }
        return out.toString();
    }

    // ---- Handover notes -----------------------------------------------------

    /** One note left by the previous shift about one resident. */
    static final class Note {
        final String time;        // "06:42", or "02 Oct 22:46" when not today
        final long postedAt;      // the same moment, epoch millis
        final String residentId;
        final String residentName; // full name - mask before showing
        final String conditions;
        final String text;

        Note(String time, long postedAt, String residentId, String residentName,
             String conditions, String text) {
            this.time = time;
            this.postedAt = postedAt;
            this.residentId = residentId;
            this.residentName = residentName;
            this.conditions = conditions;
            this.text = text;
        }
    }

    /** How many different notes each shift can leave about one resident. */
    private static final int NOTE_VARIANTS = 2;

    /**
     * Primary condition → the notes each shift can leave: { condition, then
     * two for the morning shift, two for the afternoon, two for the night }.
     * Each note only mentions medication that condition is treated with, on a
     * round that shift actually gives.
     *
     * A block has six or seven residents, so the pool a block's notes are
     * drawn from is at least twelve notes for every shift (thirty-six a day).
     */
    private static final String[][] NOTE_TEXT = {
            {"type 2 diabetes",
                    "Metformin 500 mg given at 08:00. Pre-breakfast glucose 6.8 mmol/L. Ate a full lunch.",
                    "Metformin 500 mg given at 08:00 with breakfast. Glucose 7.1 mmol/L before lunch. Drinking well.",
                    "Metformin 500 mg given at 17:00. Pre-dinner glucose 7.4 mmol/L. Feet checked, skin intact.",
                    "Metformin 500 mg given at 17:00 with dinner. Ate most of the meal. Glucose 6.9 mmol/L at 21:00.",
                    "Slept through. No signs of low blood sugar at the night check. Next Metformin due 08:00.",
                    "Glucose 6.2 mmol/L at the 06:00 check. Slept well. Metformin due at the 08:00 round."},
            {"alzheimer's disease",
                    "Settled at breakfast and joined the morning activity. No medication due this shift; Donepezil is at 21:00.",
                    "Needed prompting to dress, then calm. Ate well at lunch. Donepezil remains at the 21:00 round.",
                    "Donepezil 10 mg given at 21:00. Restless around dusk, settled with reassurance.",
                    "Donepezil 10 mg given at 21:00. Enjoyed the family visit, in bed by 21:30.",
                    "Woke once around 03:00 and was guided back to bed. No wandering.",
                    "Slept through the night checks. Sensor mat in place. No distress."},
            {"post-stroke care",
                    "Aspirin 100 mg given at 08:00. Assisted with transfer for physiotherapy. Swallowing normal at lunch.",
                    "Aspirin 100 mg given at 08:00. Walked ten metres with the frame at physiotherapy. Speech clear.",
                    "Atorvastatin 20 mg given at 21:00. One-person assist to bed. No new weakness.",
                    "Atorvastatin 20 mg given at 21:00. Ate dinner with supervision, no coughing. Settled by 21:30.",
                    "Repositioned two-hourly, pressure areas intact. Slept well.",
                    "Turned at 01:00 and 04:00, heels clear of the mattress. No complaints of pain."},
            {"parkinson's disease",
                    "Levodopa/Carbidopa 100/25 mg given on time at 08:00 and 11:00. Tremor mild, walked with frame.",
                    "Levodopa/Carbidopa 100/25 mg given at 08:00 and 11:00. Needed help with buttons, fed self at lunch.",
                    "Levodopa/Carbidopa 100/25 mg given at 17:00. Slower after dinner, assisted to bed. No falls.",
                    "Levodopa/Carbidopa 100/25 mg given at 17:00. Short freezing episode at the doorway, no fall.",
                    "Stiff when turning in bed, assisted twice. Next Levodopa/Carbidopa due 08:00 - please give on time.",
                    "Slept well. Assisted to the toilet at 02:00, steady with one helper. Levodopa/Carbidopa due 08:00."},
            {"heart failure",
                    "Furosemide 40 mg and Bisoprolol 2.5 mg given at 08:00. Weight unchanged, ankle swelling reduced.",
                    "Furosemide 40 mg and Bisoprolol 2.5 mg given at 08:00. Pulse 68, BP 118/70. Passing urine well.",
                    "No breathlessness at rest. Fluid chart up to date, legs elevated after dinner. No medication due this shift.",
                    "Comfortable in the chair, no chest pain. Evening fluid total within limit. No medication due this shift.",
                    "Slept on two pillows, no breathlessness overnight. Weigh before the 08:00 round.",
                    "Passed urine twice overnight, no breathlessness lying down. Furosemide due at the 08:00 round."},
            {"copd",
                    "Tiotropium 18 mcg given at 08:00. SpO2 94% on room air. No Salbutamol needed.",
                    "Tiotropium 18 mcg given at 08:00. Slightly breathless after the shower, recovered with rest. SpO2 93%.",
                    "Salbutamol 2 puffs PRN given at 19:30 for wheeze, effective on review. SpO2 93%.",
                    "Breathing easy this evening, SpO2 95%. No Salbutamol needed. No regular medication due this shift.",
                    "Slept propped up. Mild cough around 04:00, no Salbutamol needed. SpO2 94%.",
                    "Salbutamol 2 puffs PRN given at 03:10 for wheeze, settled on review. SpO2 93%."},
            {"vascular dementia",
                    "Aspirin 100 mg given at 08:00. Needed prompting at breakfast, calm and cooperative.",
                    "Aspirin 100 mg given at 08:00. Asked for family twice, reassured. Ate half of lunch.",
                    "Atorvastatin 20 mg given at 21:00. Confused about the time in the evening, reassured.",
                    "Atorvastatin 20 mg given at 21:00. Calm this evening, watched television in the lounge.",
                    "Night check done, sleeping. Woke at 05:30, assisted to the toilet.",
                    "Slept from 22:00. Night checks done, no wandering."},
            {"osteoporosis",
                    "Calcium + Vitamin D3 given at 08:00. Walked to the dining hall with frame. No falls.",
                    "Calcium + Vitamin D3 given at 08:00. Joined the seated exercise group. No pain reported.",
                    "Mild back ache after sitting, eased with repositioning. No medication due this shift.",
                    "Walked to dinner with the frame, steady. Bed lowered and call bell in reach. No medication due this shift.",
                    "Slept well. Call bell within reach, assisted to the toilet once. No falls.",
                    "Slept well. Stiff on first standing at 06:00, eased after a short walk. No falls."},
            {"hypertension",
                    "Amlodipine 5 mg given at 08:00. BP 138/82. Walked in the corridor with stick.",
                    "Amlodipine 5 mg given at 08:00. BP 132/78. No headache or dizziness.",
                    "BP 134/80 at evening vitals. No dizziness. No regular medication due this shift.",
                    "BP 136/80 this evening. Ate well, in good spirits. No regular medication due this shift.",
                    "Slept well, no complaints overnight.",
                    "Slept through. BP 128/76 at the 06:00 check."},
            {"rheumatoid arthritis",
                    "Morning hand stiffness eased by 09:30. Methotrexate stays weekly (Mon), Folic acid weekly (Thu).",
                    "Hands stiff until mid-morning, managed breakfast with adapted cutlery. Methotrexate is weekly (Mon).",
                    "Joints comfortable this evening, managed dinner independently. No medication due this shift.",
                    "Wrists a little swollen this evening, rested with warm packs. No medication due this shift.",
                    "Slept well. Extra pillow under the knees for comfort.",
                    "Woke once with wrist discomfort, settled after repositioning."},
    };

    private static final String[] FALLBACK_NOTE = {
            "Comfortable this shift. Ate and drank well.",
            "Comfortable this evening. Evening vitals within usual range.",
            "Slept well, no complaints overnight.",
    };

    /** The shift that most recently finished: its roster label and end hour. */
    private static final String[] SHIFT_NAMES = {"Morning shift", "Afternoon shift", "Night shift"};
    private static final String[] SHIFT_WINDOWS = {"07:00-15:00", "15:00-23:00", "23:00-07:00"};

    /** When each shift writes up its three notes, just before it hands over. */
    private static final int[][] NOTE_MINUTES = {
            {14 * 60 + 35, 14 * 60 + 42, 14 * 60 + 50},
            {22 * 60 + 40, 22 * 60 + 46, 22 * 60 + 52},
            {6 * 60 + 35, 6 * 60 + 42, 6 * 60 + 50},
    };

    /** 0 = morning, 1 = afternoon, 2 = night: the shift that finished last. */
    private static int previousShift(Calendar now) {
        int hour = now.get(Calendar.HOUR_OF_DAY);
        if (hour >= 7 && hour < 15) return 2;   // morning running → night just ended
        if (hour >= 15 && hour < 23) return 0;  // afternoon running → morning ended
        return 1;                               // night running → afternoon ended
    }

    /** "Night shift · 23:00-07:00" for the shift the notes come from. */
    static String previousShiftLabel(Calendar now) {
        int shift = previousShift(now);
        return SHIFT_NAMES[shift] + " · " + SHIFT_WINDOWS[shift];
    }

    /**
     * Three notes from the shift that finished last, about residents in the
     * nurse's own block, oldest first. They change at every shift change
     * (07:00, 15:00 and 23:00). The shift has already ended, so every note's
     * time is before the current time.
     *
     * Which residents are written about, and which of their notes is used, is
     * drawn from the block's pool by the date and the shift alone, so the same
     * shift on the same day always shows the same three notes.
     */
    static List<Note> handoverNotes(StaffData.Profile profile, Calendar now) {
        int shift = previousShift(now);

        // The afternoon shift ends at 23:00; after midnight that was yesterday.
        Calendar written = startOfDay(now);
        boolean today = !(shift == 1 && now.get(Calendar.HOUR_OF_DAY) < 7);
        if (!today) written.add(Calendar.DAY_OF_MONTH, -1);
        String datePrefix = today ? "" : format("dd MMM", written) + " ";

        long seed = (written.get(Calendar.YEAR) * 366L + written.get(Calendar.DAY_OF_YEAR)) * 3
                + shift;
        // Spread neighbouring shifts far apart before seeding, so consecutive
        // shifts and blocks do not draw look-alike picks.
        Random pick = new Random((seed * 31 + profile.staffId.hashCode()) * 0x9E3779B97F4A7C15L);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < profile.residents.length; i++) order.add(i);
        Collections.shuffle(order, pick);

        List<Note> notes = new ArrayList<>();
        for (int i = 0; i < 3 && i < order.size(); i++) {
            String[] resident = profile.residents[order.get(i)];
            int minute = NOTE_MINUTES[shift][i];
            Calendar at = (Calendar) written.clone();
            at.set(Calendar.HOUR_OF_DAY, minute / 60);
            at.set(Calendar.MINUTE, minute % 60);
            String time = String.format(Locale.ENGLISH, "%02d:%02d", minute / 60, minute % 60);
            notes.add(new Note(datePrefix + time, at.getTimeInMillis(), resident[0], resident[1],
                    resident[4], noteText(resident[4], shift, pick.nextInt(NOTE_VARIANTS))));
        }
        return notes;
    }

    /**
     * The Home tile's count: how many of the previous shift's notes were
     * written after Handover Notes was last opened. A last-opened time of 0,
     * or one that is ahead of the clock, means it has not been opened.
     */
    static int newHandoverCount(StaffData.Profile profile, Calendar now, long lastOpenedAt) {
        if (lastOpenedAt > now.getTimeInMillis()) lastOpenedAt = 0;
        int count = 0;
        for (Note note : handoverNotes(profile, now)) {
            if (note.postedAt > lastOpenedAt) count++;
        }
        return count;
    }

    /** The last note the previous shift wrote - the one the bell announces. */
    static Note newestNote(StaffData.Profile profile, Calendar now) {
        List<Note> notes = handoverNotes(profile, now);
        return notes.get(notes.size() - 1);
    }

    private static String noteText(String conditions, int shift, int variant) {
        String primary = conditions.split(",")[0].trim().toLowerCase(Locale.ROOT);
        for (String[] row : NOTE_TEXT) {
            if (row[0].equals(primary)) return row[1 + shift * NOTE_VARIANTS + variant];
        }
        return FALLBACK_NOTE[shift];
    }

    // ---- Date helpers (Malaysia time) ---------------------------------------

    /** Midnight at the start of the given moment's day. */
    private static Calendar startOfDay(Calendar moment) {
        Calendar day = (Calendar) moment.clone();
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        return day;
    }

    /** Whole days from today to the given midnight (0 = today). Malaysia has no DST. */
    private static long daysUntil(Calendar now, long dayMillis) {
        return Math.round((dayMillis - startOfDay(now).getTimeInMillis()) / 86400000.0);
    }

    private static String format(String pattern, Calendar moment) {
        SimpleDateFormat fmt = new SimpleDateFormat(pattern, Locale.ENGLISH);
        fmt.setTimeZone(TimeZone.getTimeZone(StaffData.ZONE_ID));
        return fmt.format(moment.getTime());
    }

    private CareData() {}
}
