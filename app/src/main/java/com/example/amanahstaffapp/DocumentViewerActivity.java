package com.example.amanahstaffapp;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.Locale;

/**
 * Read-only, in-app preview of a company document. All content is built into
 * the app — nothing is downloaded, no file is read and no WebView is used.
 * Opening a preview records a 'file_interaction' with the filename.
 */
public class DocumentViewerActivity extends BaseActivity {

    public static final String EXTRA_FILENAME = "extra_filename";

    public static final String DOC_HANDBOOK = "Staff_Handbook.pdf";
    public static final String DOC_PAYSLIP = "Payslip_Aug2026.pdf";
    public static final String DOC_SOP = "Infection_Control_SOP.pdf";

    // Payslip employee details: { staff ID, full name, masked IC }. Unknown
    // usernames fall back to nurse01, matching StaffData#forUser.
    private static final String[] PAYSLIP_NURSE01 =
            {"TLC-N01", "Farah Aiman binti Rahim", "900318-14-****"};
    private static final String[] PAYSLIP_NURSE02 =
            {"TLC-N02", "Nurul Izzah binti Kamal", "880725-10-****"};
    private static final String[] PAYSLIP_SITI =
            {"TLC-N03", "Siti Aminah binti Osman", "920214-03-****"};
    private static final String[] PAYSLIP_KAVITHA =
            {"TLC-N04", "Kavitha a/p Rajendran", "940607-07-****"};

    private LinearLayout body;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_viewer);

        String filename = getIntent().getStringExtra(EXTRA_FILENAME);
        if (!DOC_HANDBOOK.equals(filename) && !DOC_PAYSLIP.equals(filename)
                && !DOC_SOP.equals(filename)) {
            finish();
            return;
        }

        setupToolbar(filename);
        body = findViewById(R.id.document_body);

        // Record once per opening, not again on rotation/recreation.
        if (savedInstanceState == null) {
            ApiClient.track(this, "file_interaction", "Viewed " + filename);
        }

        if (DOC_PAYSLIP.equals(filename)) {
            buildPayslip();
        } else if (DOC_HANDBOOK.equals(filename)) {
            buildHandbook();
        } else {
            buildInfectionControlSop();
        }
    }

    // ---- Payslip -------------------------------------------------------------

    private void buildPayslip() {
        String[] employee = payslipEmployee(session.getUsername());

        addTitle("Takahashi Legend Care");
        addMuted("PAYSLIP");
        addSpacer(12);

        addRow("Pay period", "August 2026", false);
        addRow("Pay date", "28 Aug 2026", false);
        addDivider();

        addRow("Name", Masking.name(employee[1]), false);
        addRow("Staff ID", employee[0], false);
        addRow("IC No.", employee[2], false);
        addRow("Position", "Staff Nurse", false);
        addDivider();

        addHeading("Earnings (RM)");
        addRow("Basic Salary", "3,800.00", false);
        addRow("Shift Allowance", "300.00", false);
        addRow("Gross Pay", "4,100.00", true);
        addDivider();

        addHeading("Deductions (RM)");
        addRow("EPF", "451.00", false);
        addRow("SOCSO", "20.50", false);
        addRow("EIS", "8.20", false);
        addDivider();

        addRow("Net Pay (RM)", "3,620.30", true);
        addSpacer(16);
        addMuted("This is a computer-generated payslip. No signature is required.");
    }

    private static String[] payslipEmployee(String username) {
        String key = TextUtils.isEmpty(username) ? "" : username.trim().toLowerCase(Locale.ROOT);
        switch (key) {
            case "nurse02":      return PAYSLIP_NURSE02;
            case "siti.aminah":  return PAYSLIP_SITI;
            case "kavitha.raj":  return PAYSLIP_KAVITHA;
            case "nurse01":
            default:             return PAYSLIP_NURSE01;
        }
    }

    // ---- Staff Handbook ------------------------------------------------------

    private void buildHandbook() {
        addTitle("Staff Handbook");
        addMuted("Takahashi Legend Care · Nursing Staff · Updated 03 Feb 2026");
        addSpacer(8);

        addHeading("1. Welcome");
        addParagraph("Takahashi Legend Care is a residential care home providing long-term "
                + "nursing care for older adults. This handbook sets out what is expected of "
                + "every member of the nursing team. Read it alongside the clinical SOPs "
                + "listed in the staff portal.");

        addHeading("2. Shifts");
        addParagraph("Nursing cover runs 24 hours a day in three shifts:");
        addBullet("Morning: 07:00-15:00");
        addBullet("Afternoon: 15:00-23:00");
        addBullet("Night: 23:00-07:00");
        addParagraph("A night shift belongs to the day it starts. Each nurse works five "
                + "shifts a week with two rest days, as shown in My Schedule. Arrive in "
                + "time for handover and do not leave until your relief has taken over.");

        addHeading("3. Room blocks");
        addParagraph("Each nurse is assigned a fixed block (Block A, B, C or D) and remains "
                + "responsible for it on every shift. Complete room-to-room rounds at the "
                + "start and end of each shift and record them before handover.");

        addHeading("4. Handover");
        addParagraph("Give a verbal and written handover for every occupied room in your "
                + "block: changes in condition, medication given or withheld, falls, "
                + "appetite and family visits. Vacant rooms are checked and noted as ready "
                + "or not ready.");

        addHeading("5. Attendance and leave");
        addParagraph("Apply for annual leave at least two weeks in advance through the "
                + "Nurse Manager. If you are unwell and cannot attend, inform the Nurse "
                + "Manager on duty before your shift starts so cover can be arranged. "
                + "Shift swaps must be approved before they take effect.");

        addHeading("6. Uniform and conduct");
        addParagraph("Wear the issued uniform and staff ID badge at all times on duty. "
                + "Keep nails short and free of polish, and hair tied back. Treat "
                + "residents and their families with dignity, patience and respect.");

        addHeading("7. Confidentiality");
        addParagraph("Resident information is confidential and must be handled in line "
                + "with the Personal Data Protection Act 2010. Do not photograph "
                + "residents, discuss them outside work or share your staff portal "
                + "login with anyone.");

        addHeading("8. Pay");
        addParagraph("Salaries are paid monthly on the 28th. Your payslip is available "
                + "under Documents in the staff portal. Raise any pay queries with HR "
                + "within 14 days of the pay date.");
    }

    // ---- Infection Control SOP -----------------------------------------------

    private void buildInfectionControlSop() {
        addTitle("Infection Control SOP");
        addMuted("Doc. TLC-SOP-IC-03 · Rev. 4 · Effective 12 Jun 2026");
        addSpacer(8);

        addHeading("1. Purpose");
        addParagraph("To prevent the spread of infection between residents, staff and "
                + "visitors at Takahashi Legend Care. Applies to all nursing and care "
                + "staff on every shift.");

        addHeading("2. Hand hygiene");
        addParagraph("Clean your hands with alcohol rub, or soap and water when hands are "
                + "visibly soiled:");
        addBullet("Before touching a resident");
        addBullet("Before a clean or aseptic procedure");
        addBullet("After exposure to body fluids");
        addBullet("After touching a resident");
        addBullet("After touching a resident's surroundings");

        addHeading("3. Personal protective equipment");
        addParagraph("Wear gloves and a disposable apron for personal care, continence "
                + "care and wound dressing. Add a surgical mask and eye protection when "
                + "there is a risk of splashing. Change PPE between rooms — never carry "
                + "used PPE from one room to the next during rounds.");

        addHeading("4. Rounds within your room block");
        addParagraph("Where possible, visit residents with suspected or confirmed "
                + "infection last on your round. Keep a dedicated thermometer and blood "
                + "pressure cuff in isolation rooms and wipe shared equipment with "
                + "disinfectant wipes after each use.");

        addHeading("5. Isolation");
        addParagraph("A resident with diarrhoea and vomiting, a suspected respiratory "
                + "infection or a known multi-drug-resistant organism is cared for in "
                + "their own room with the door closed. Place the isolation sign on the "
                + "door and inform the Nurse Manager and the incoming shift at handover.");

        addHeading("6. Sharps and clinical waste");
        addParagraph("Dispose of sharps immediately into the sharps bin at the point of "
                + "use and never re-cap needles. Clinical waste goes in yellow bags; "
                + "general waste in black bags. Close sharps bins when three-quarters full.");

        addHeading("7. Linen");
        addParagraph("Place soiled linen in the red alginate bag inside the room. Do not "
                + "shake linen or place it on the floor.");

        addHeading("8. Reporting");
        addParagraph("Record any new fever, cough, diarrhoea, vomiting or wound infection "
                + "in the resident's notes and report it to the Nurse Manager before the "
                + "end of your shift. Two or more residents with similar symptoms within "
                + "48 hours must be reported as a possible outbreak.");
    }

    // ---- View helpers --------------------------------------------------------

    private void addTitle(String text) {
        TextView v = text(text, 20, R.color.brand_primary_dark, true);
        body.addView(v);
    }

    private void addHeading(String text) {
        TextView v = text(text, 15, R.color.brand_primary, true);
        v.setPadding(0, dp(14), 0, dp(4));
        body.addView(v);
    }

    private void addParagraph(String text) {
        TextView v = text(text, 14, R.color.text_primary, false);
        v.setLineSpacing(0, 1.2f);
        v.setPadding(0, dp(2), 0, dp(4));
        body.addView(v);
    }

    private void addBullet(String text) {
        TextView v = text("•  " + text, 14, R.color.text_primary, false);
        v.setPadding(dp(8), dp(2), 0, dp(2));
        body.addView(v);
    }

    private void addMuted(String text) {
        TextView v = text(text, 12, R.color.text_muted, false);
        v.setPadding(0, dp(2), 0, 0);
        body.addView(v);
    }

    private void addRow(String label, String value, boolean bold) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView labelView = text(label, 14,
                bold ? R.color.text_primary : R.color.text_secondary, bold);
        labelView.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(labelView);

        TextView valueView = text(value, 14, R.color.text_primary, bold);
        valueView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        row.addView(valueView);

        body.addView(row);
    }

    private void addDivider() {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
        params.topMargin = dp(8);
        params.bottomMargin = dp(8);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.divider));
        body.addView(divider);
    }

    private void addSpacer(int heightDp) {
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)));
        body.addView(spacer);
    }

    private TextView text(String text, int sp, int colorRes, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(ContextCompat.getColor(this, colorRes));
        if (bold) v.setTypeface(v.getTypeface(), Typeface.BOLD);
        return v;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
