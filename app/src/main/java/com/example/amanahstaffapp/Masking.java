package com.example.amanahstaffapp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Display-time masking of personal data, matching the web decoy. The source
 * data in {@link StaffData} and the payslip records stays unchanged; screens
 * pass values through here just before showing them.
 *
 * The on-screen notice says full names appear on the TLC-Staff Wi-Fi, but the
 * app deliberately never checks the network: the masked view is always shown,
 * and no network-state or location permission is requested.
 */
final class Masking {

    private static final String[] HONORIFICS = {"Mr.", "Madam", "Encik", "Puan"};

    /** Matches a room range such as "Rooms 1–8" or "Rooms 9-16". */
    private static final Pattern ROOM_RANGE =
            Pattern.compile("Rooms\\s+(\\d+)\\s*[–-]\\s*\\d+");

    private static final int ROOMS_PER_BLOCK = 8;

    /**
     * Keeps an optional honorific and the first name, and reduces every other
     * word to its first letter followed by ***.
     * "Farah Aiman binti Rahim" → "Farah A*** b*** R***".
     */
    static String name(String fullName) {
        if (fullName == null) return "";
        String[] words = fullName.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        int keep = isHonorific(words[0]) ? 2 : 1;
        for (int i = 0; i < words.length; i++) {
            if (out.length() > 0) out.append(' ');
            if (i < keep) {
                out.append(words[i]);
            } else if (!words[i].isEmpty()) {
                out.append(words[i].charAt(0)).append("***");
            }
        }
        return out.toString();
    }

    /** The first name alone (the word after any honorific), e.g. "Farah". */
    static String firstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "";
        String[] words = fullName.trim().split("\\s+");
        return isHonorific(words[0]) && words.length > 1 ? words[1] : words[0];
    }

    /** First letter of the first name. */
    static String firstNameInitial(String fullName) {
        String first = firstName(fullName);
        return first.isEmpty() ? "" : first.substring(0, 1);
    }

    /**
     * Replaces any room range in the text with its block letter:
     * Rooms 1–8 → Block A, 9–16 → Block B, 17–24 → Block C, 25–32 → Block D.
     * Text without a room range (e.g. "Off duty") is returned as-is.
     */
    static String workArea(String text) {
        if (text == null) return "";
        Matcher m = ROOM_RANGE.matcher(text);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            int firstRoom = Integer.parseInt(m.group(1));
            m.appendReplacement(out, "Block " + blockLetter(firstRoom));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** "6 of 8 occupied" style summary for a block, with no room numbers. */
    static String occupancy(StaffData.Profile profile) {
        int total = profile.residentCount + profile.vacantCount;
        return workArea(profile.block) + " · " + profile.residentCount
                + " of " + total + " occupied";
    }

    private static char blockLetter(int firstRoom) {
        return (char) ('A' + (firstRoom - 1) / ROOMS_PER_BLOCK);
    }

    private static boolean isHonorific(String word) {
        for (String h : HONORIFICS) {
            if (h.equalsIgnoreCase(word)) return true;
        }
        return false;
    }

    private Masking() {}
}
