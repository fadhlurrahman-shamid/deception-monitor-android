package com.example.amanahstaffapp;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Fake, internally-consistent duty data for the residential care home.
 *
 * Takahashi Legend Care has four staff accounts that share the building's
 * workload, each responsible for their own block of resident rooms. This class
 * maps the logged-in username to that nurse's own weekly roster, resident list
 * and today's shift, so a staff member sees only their own duties.
 *
 * The four nurses' weekly schedules, fixed room blocks and residents are the
 * SAME as the web decoy (admin/includes/decoy_data.php: $TLC_NURSES, $TLC_ROTA
 * and $TLC_RESIDENTS). Resident IDs, ages and condition text are copied from
 * there verbatim. Each nurse works exactly 5 days with 2 rest days, and their
 * room block never rotates:
 *   nurse01 (Farah Aiman binti Rahim) → Rooms 1–8   (Block A)
 *   nurse02 (Nurul Izzah binti Kamal) → Rooms 9–16  (Block B)
 *   siti.aminah (Siti Aminah binti Osman) → Rooms 17–24 (Block C)
 *   kavitha.raj (Kavitha a/p Rajendran)   → Rooms 25–32 (Block D)
 *
 * A week is stored as a 7-character shift pattern, Monday→Sunday, where each
 * character is one of: M = Morning (07:00-15:00), A = Afternoon (15:00-23:00),
 * N = Night (23:00-07:00), R = Rest day. The visible weekly roster and the
 * dashboard's "today's shift" are both derived from that single pattern, so the
 * two can never drift apart.
 *
 * Like a real home, the building is not full: some rooms in each block are
 * vacant (recent discharges, rooms between residents, maintenance). Resident
 * and vacant counts are derived from the lists below so every screen agrees.
 *
 * Everything here is fictional (names, ages, conditions, medications). Nothing
 * here touches tracking, API calls or the login flow.
 */
final class StaffData {

    /** IANA zone the web decoy uses for all shift/date maths (UTC+8). */
    static final String ZONE_ID = "Asia/Kuala_Lumpur";

    // Shift codes used inside a weekly pattern.
    static final char MORNING = 'M';
    static final char AFTERNOON = 'A';
    static final char NIGHT = 'N';
    static final char REST = 'R';

    private static final String[] DAY_NAMES = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    /** One nurse's complete, self-consistent dataset. */
    static final class Profile {
        final String staffId;       // e.g. "TLC-N01"
        final String position;      // e.g. "Staff Nurse"
        final String maskedIc;      // already masked, e.g. "900318-14-****"
        final String displayName;   // e.g. "Farah Aiman binti Rahim"
        final String block;         // e.g. "Rooms 1–8"
        final char[] week;          // 7 shift codes, Monday→Sunday
        final int residentCount;    // occupied rooms in this block
        final int vacantCount;
        final String[][] roster;    // { day, shift/time, room block or "Off duty" }
        final String[][] residents; // { resident ID, name, age/gender, room, condition, medication }
        final String[][] vacant;    // { room, reason }

        Profile(String staffId, String position, String maskedIc,
                String displayName, String block, String weekPattern,
                String[][] residents, String[][] vacant) {
            this.staffId = staffId;
            this.position = position;
            this.maskedIc = maskedIc;
            this.displayName = displayName;
            this.block = block;
            this.week = weekPattern.toCharArray();
            this.residentCount = residents.length;
            this.vacantCount = vacant.length;
            this.residents = residents;
            this.vacant = vacant;
            this.roster = buildRoster(this.week, block);
        }

        /**
         * The nurse's shift right now, computed from the wall clock in Malaysia
         * (Asia/Kuala_Lumpur, UTC+8) so it always matches the web decoy — never
         * the device's local time.
         *
         * Night-shift-after-midnight rule: a Night shift runs 23:00 → 07:00
         * the NEXT calendar day, but it belongs to the day it STARTED. So between
         * midnight and 07:00 we first check whether the PREVIOUS day was a Night
         * shift; if so, that night shift is still running and is shown, even
         * though "today" might be a rest day. Only after 07:00 (or when the
         * previous day was not a night) does today's own shift apply.
         */
        String currentShift() {
            return currentShift(nowInMalaysia());
        }

        /** Testable core of {@link #currentShift()} for an explicit Malaysia-zoned clock. */
        String currentShift(Calendar nowInMalaysia) {
            return todayShiftLabel(currentShiftCode(nowInMalaysia));
        }

        /** The shift code (M/A/N/R) behind {@link #currentShift(Calendar)}. */
        char currentShiftCode(Calendar nowInMalaysia) {
            int hour = nowInMalaysia.get(Calendar.HOUR_OF_DAY);
            int today = mondayIndex(nowInMalaysia.get(Calendar.DAY_OF_WEEK));

            if (hour < 7) {
                int previous = (today + 6) % 7; // yesterday, Monday-indexed
                if (week[previous] == NIGHT) {
                    return NIGHT;
                }
            }
            return week[today];
        }

        /** "Monday, 07:00": the day and start time of the first shift after today. */
        String nextShift(Calendar nowInMalaysia) {
            int today = mondayIndex(nowInMalaysia.get(Calendar.DAY_OF_WEEK));
            for (int i = 1; i <= 7; i++) {
                int day = (today + i) % 7;
                if (week[day] != REST) return DAY_NAMES[day] + ", " + shiftStart(week[day]);
            }
            return "";
        }
    }

    /**
     * Returns the profile for the logged-in nurse. Unknown / decoy-mismatched
     * logins fall back to nurse01's dataset so whoever gets in still sees one
     * consistent, logical set of duties.
     */
    static Profile forUser(String username) {
        String key = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        switch (key) {
            case "nurse02":      return NURSE02;
            case "siti.aminah":  return SITI;
            case "kavitha.raj":  return KAVITHA;
            case "nurse01":
            default:             return NURSE01;
        }
    }

    // ---- Calendar helpers (Malaysia time) -----------------------------------

    /** The current wall-clock time in Malaysia, independent of the device zone. */
    static Calendar nowInMalaysia() {
        return Calendar.getInstance(TimeZone.getTimeZone(ZONE_ID), Locale.ENGLISH);
    }

    /** Today's position in the Monday→Sunday week (Mon=0..Sun=6), Malaysia time. */
    static int todayIndex() {
        return mondayIndex(nowInMalaysia().get(Calendar.DAY_OF_WEEK));
    }

    /** Midnight on the Monday of the current Malaysia-time week. */
    static Calendar currentWeekMonday() {
        Calendar monday = nowInMalaysia();
        monday.add(Calendar.DAY_OF_MONTH, -mondayIndex(monday.get(Calendar.DAY_OF_WEEK)));
        monday.set(Calendar.HOUR_OF_DAY, 0);
        monday.set(Calendar.MINUTE, 0);
        monday.set(Calendar.SECOND, 0);
        monday.set(Calendar.MILLISECOND, 0);
        return monday;
    }

    // ---- Derivation helpers -------------------------------------------------

    /** Maps Calendar.DAY_OF_WEEK (Sun=1..Sat=7) to a Monday-based index (Mon=0..Sun=6). */
    static int mondayIndex(int calendarDayOfWeek) {
        return (calendarDayOfWeek + 5) % 7;
    }

    /** Longer, dashboard-style label for a single shift code. */
    static String todayShiftLabel(char code) {
        switch (code) {
            case MORNING:   return "Morning Shift · 07:00-15:00";
            case AFTERNOON: return "Afternoon Shift · 15:00-23:00";
            case NIGHT:     return "Night Shift · 23:00-07:00";
            case REST:
            default:        return "Rest day";
        }
    }

    /** The time a working shift starts. */
    private static String shiftStart(char code) {
        switch (code) {
            case MORNING:   return "07:00";
            case AFTERNOON: return "15:00";
            default:        return "23:00";
        }
    }

    /** Compact label for a single shift code, as shown on the weekly roster. */
    private static String rosterShiftLabel(char code) {
        switch (code) {
            case MORNING:   return "Morning · 07:00-15:00";
            case AFTERNOON: return "Afternoon · 15:00-23:00";
            case NIGHT:     return "Night · 23:00-07:00";
            case REST:
            default:        return "Rest day";
        }
    }

    /** Builds the visible Monday→Sunday roster rows from a 7-day shift pattern. */
    private static String[][] buildRoster(char[] week, String block) {
        String[][] rows = new String[7][3];
        for (int i = 0; i < 7; i++) {
            rows[i][0] = DAY_NAMES[i];
            rows[i][1] = rosterShiftLabel(week[i]);
            rows[i][2] = week[i] == REST ? "Off duty" : block;
        }
        return rows;
    }

    // ---- nurse01 · Farah Aiman binti Rahim · Rooms 1–8 · 6 of 8 occupied ----
    // Week (Mon→Sun): Morning, Morning, Afternoon, Afternoon, Night, Rest, Rest
    private static final Profile NURSE01 = new Profile(
            "TLC-N01", "Staff Nurse", "900318-14-****",
            "Farah Aiman binti Rahim",
            "Rooms 1–8",
            "MMAANRR",
            new String[][]{
                    {"TLC-P0104", "Mr. Lim Cheng Hock",        "82, Male",   "Room 1", "Type 2 diabetes, hypertension", "Metformin 08:00 & 20:00 · Amlodipine 08:00"},
                    {"TLC-P0118", "Madam Wong Ai Lin",         "78, Female", "Room 2", "Alzheimer's disease",           "Donepezil 20:00 · Memantine 08:00"},
                    {"TLC-P0122", "Encik Ismail bin Abdullah", "85, Male",   "Room 3", "Post-stroke care",              "Aspirin 08:00 · Atorvastatin 20:00"},
                    {"TLC-P0131", "Mr. Raju a/l Krishnan",     "80, Male",   "Room 5", "Parkinson's disease",           "Levodopa 07:00, 12:00 & 17:00"},
                    {"TLC-P0137", "Madam Chan Sow Fun",        "88, Female", "Room 6", "Heart failure",                 "Furosemide 08:00 · Digoxin 08:00"},
                    {"TLC-P0145", "Encik Tan Kok Leong",       "83, Male",   "Room 8", "COPD, hypertension",            "Salbutamol inhaler 08:00 & 20:00 · Perindopril 08:00"},
            },
            new String[][]{
                    {"Room 4", "Discharged home 10 Sep · awaiting deep clean"},
                    {"Room 7", "Under maintenance · bathroom grab rails being refitted"},
            });

    // ---- nurse02 · Nurul Izzah binti Kamal · Rooms 9–16 · 7 of 8 occupied ---
    // Week (Mon→Sun): Rest, Rest, Morning, Morning, Afternoon, Afternoon, Night
    private static final Profile NURSE02 = new Profile(
            "TLC-N02", "Staff Nurse", "880725-10-****",
            "Nurul Izzah binti Kamal",
            "Rooms 9–16",
            "RRMMAAN",
            new String[][]{
                    {"TLC-P0152", "Encik Yusof bin Karim",     "79, Male",   "Room 9",  "Type 2 diabetes",               "Metformin 08:00 & 20:00 · Gliclazide 08:00"},
                    {"TLC-P0158", "Madam Lee Siew Peng",       "84, Female", "Room 10", "Vascular dementia",             "Memantine 08:00 · Aspirin 08:00"},
                    {"TLC-P0163", "Mr. Anthony Fernandez",     "81, Male",   "Room 11", "Post-stroke care",              "Clopidogrel 08:00 · Atorvastatin 20:00"},
                    {"TLC-P0169", "Madam Teoh Guat Hoon",      "86, Female", "Room 13", "Osteoporosis",                  "Alendronate (Mon) 07:00 · Calcium 08:00 & 20:00"},
                    {"TLC-P0174", "Encik Suppiah a/l Govind",  "83, Male",   "Room 14", "Parkinson's disease, dementia", "Levodopa 07:00, 13:00 & 18:00 · Rivastigmine 08:00 & 20:00"},
                    {"TLC-P0180", "Puan Aminah binti Salleh",  "75, Female", "Room 15", "Type 2 diabetes",               "Insulin 07:00 & 18:00"},
                    {"TLC-P0186", "Mr. Goh Beng Huat",         "80, Male",   "Room 16", "COPD",                          "Tiotropium inhaler 08:00 · Salbutamol PRN"},
            },
            new String[][]{
                    {"Room 12", "Between residents · new admission expected 18 Sep"},
            });

    // ---- siti.aminah · Siti Aminah binti Osman · Rooms 17–24 · 6 of 8 occupied
    // Week (Mon→Sun): Afternoon, Night, Rest, Rest, Morning, Morning, Afternoon
    private static final Profile SITI = new Profile(
            "TLC-N03", "Staff Nurse", "920214-03-****",
            "Siti Aminah binti Osman",
            "Rooms 17–24",
            "ANRRMMA",
            new String[][]{
                    {"TLC-P0191", "Puan Rohana binti Ahmad",   "78, Female", "Room 17", "Hypertension, osteoarthritis",  "Amlodipine 08:00 · Paracetamol 08:00 & 20:00"},
                    {"TLC-P0197", "Encik Chong Kah Meng",      "82, Male",   "Room 18", "Type 2 diabetes, neuropathy",   "Metformin 08:00 & 20:00 · Gabapentin 21:00"},
                    {"TLC-P0203", "Mr. Wan Ahmad bin Omar",    "79, Male",   "Room 20", "Post-stroke care",              "Aspirin 08:00 · Atorvastatin 20:00"},
                    {"TLC-P0208", "Puan Grace Lim",            "74, Female", "Room 21", "Rheumatoid arthritis",          "Methotrexate (Tue) 09:00 · Folic acid 09:00"},
                    {"TLC-P0214", "Encik Bala a/l Krishnan",   "81, Male",   "Room 22", "Heart failure",                 "Bisoprolol 08:00 · Furosemide 08:00"},
                    {"TLC-P0219", "Mr. Ong Tiong Seng",        "83, Male",   "Room 24", "COPD, hypertension",            "Salbutamol 08:00 & 20:00 · Perindopril 08:00"},
            },
            new String[][]{
                    {"Room 19", "Discharged to family care 12 Sep · room being prepared"},
                    {"Room 23", "Under maintenance · air-conditioning repair"},
            });

    // ---- kavitha.raj · Kavitha a/p Rajendran · Rooms 25–32 · 7 of 8 occupied -
    // Week (Mon→Sun): Morning, Afternoon, Afternoon, Night, Rest, Rest, Morning
    private static final Profile KAVITHA = new Profile(
            "TLC-N04", "Staff Nurse", "940607-07-****",
            "Kavitha a/p Rajendran",
            "Rooms 25–32",
            "MAANRRM",
            new String[][]{
                    {"TLC-P0225", "Encik Ramli bin Jusoh",       "80, Male",   "Room 25", "Type 2 diabetes, hypertension", "Metformin 08:00 & 20:00 · Amlodipine 08:00"},
                    {"TLC-P0231", "Madam Cheng Bee Lian",        "85, Female", "Room 26", "Alzheimer's disease",           "Donepezil 20:00 · Memantine 08:00"},
                    {"TLC-P0236", "Mr. Joseph Raj",              "82, Male",   "Room 27", "Post-stroke care",              "Clopidogrel 08:00 · Simvastatin 20:00"},
                    {"TLC-P0242", "Madam Kamala a/p Nair",       "86, Female", "Room 29", "Parkinson's disease",           "Levodopa 07:00, 12:00 & 17:00"},
                    {"TLC-P0247", "Encik Foo Weng Kong",         "84, Male",   "Room 30", "Heart failure",                 "Furosemide 08:00 · Digoxin 08:00"},
                    {"TLC-P0253", "Puan Noor Aini binti Zainal", "75, Female", "Room 31", "Type 2 diabetes",               "Insulin 07:00 & 18:00 · Metformin 20:00"},
                    {"TLC-P0258", "Mr. Vijay a/l Suppiah",       "81, Male",   "Room 32", "COPD, anxiety",                 "Tiotropium 08:00 · Sertraline 08:00"},
            },
            new String[][]{
                    {"Room 28", "Between residents · admission assessment pending"},
            });

    private StaffData() {}
}
