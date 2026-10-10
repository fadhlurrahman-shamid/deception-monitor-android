package com.example.amanahstaffapp;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

/**
 * Shared toolbar, navigation drawer, scroll-inset, menu (bell, Log Out) and
 * logout behaviour for every inner staff screen.
 *
 * A main screen names its drawer entry in {@link #drawerItem()} and gets the
 * hamburger button and the drawer; a sub-screen leaves it at 0 and gets a back
 * arrow instead.
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected SessionManager session;

    private DrawerLayout drawer;

    /** The drawer entry this screen stands for, or 0 for a sub-screen. */
    protected int drawerItem() {
        return 0;
    }

    /** A main screen's layout is placed inside the shared drawer shell. */
    @Override
    public void setContentView(int layoutResID) {
        if (drawerItem() == 0) {
            super.setContentView(layoutResID);
            return;
        }
        super.setContentView(R.layout.activity_drawer_shell);
        getLayoutInflater().inflate(layoutResID, findViewById(R.id.drawer_content), true);
    }

    protected void setupToolbar(String title) {
        session = SessionManager.get(this);
        boolean hasDrawer = drawerItem() != 0;
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
                getSupportActionBar().setSubtitle("Takahashi Legend Care");
                getSupportActionBar().setDisplayHomeAsUpEnabled(!hasDrawer);
            }
            if (hasDrawer) setupDrawer(toolbar);
        }
        applyScrollInsets();
    }

    /** Hamburger button, drawer header (first name, staff ID, block) and entries. */
    private void setupDrawer(Toolbar toolbar) {
        drawer = findViewById(R.id.drawer_layout);

        toolbar.setNavigationIcon(R.drawable.ic_menu);
        toolbar.setNavigationContentDescription(R.string.nav_open_menu);
        toolbar.setNavigationOnClickListener(v -> drawer.openDrawer(GravityCompat.START));

        NavigationView nav = findViewById(R.id.drawer_nav);
        nav.setCheckedItem(drawerItem());
        nav.setNavigationItemSelectedListener(item -> {
            openDrawerItem(item.getItemId());
            return false; // the highlight stays on the screen being shown
        });

        StaffData.Profile profile = StaffData.forUser(session.getUsername());
        View header = nav.getHeaderView(0);
        ((TextView) header.findViewById(R.id.text_drawer_name))
                .setText(Masking.firstName(profile.displayName));
        ((TextView) header.findViewById(R.id.text_drawer_staff))
                .setText(profile.staffId + "  ·  " + Masking.workArea(profile.block));

        // The drawer runs under the status bar, so push the header's content
        // below the clock and battery icons.
        final int baseTop = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(drawer, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            header.setPadding(header.getPaddingLeft(), baseTop + bars.top,
                    header.getPaddingRight(), header.getPaddingBottom());
            return insets;
        });

        // Back closes an open drawer before it leaves the screen.
        OnBackPressedCallback closeDrawer = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                drawer.closeDrawer(GravityCompat.START);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, closeDrawer);
        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                closeDrawer.setEnabled(true);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                closeDrawer.setEnabled(false);
            }
        });
    }

    /**
     * Home stays at the bottom of the stack and the other main screens replace
     * one another above it, so Back from any of them returns to Home.
     */
    private void openDrawerItem(int id) {
        drawer.closeDrawer(GravityCompat.START);
        if (id == drawerItem()) return;

        Intent intent;
        if (id == R.id.drawer_logout) {
            logout();
            return;
        } else if (id == R.id.drawer_home) {
            intent = new Intent(this, DashboardActivity.class).addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else if (id == R.id.drawer_schedule) {
            intent = new Intent(this, ScheduleActivity.class);
        } else if (id == R.id.drawer_patients) {
            intent = new Intent(this, PatientsActivity.class);
        } else if (id == R.id.drawer_handover) {
            intent = InfoListActivity.intent(this, InfoListActivity.MODE_HANDOVER);
        } else if (id == R.id.drawer_tasks) {
            intent = InfoListActivity.intent(this, InfoListActivity.MODE_TASKS);
        } else if (id == R.id.drawer_documents) {
            intent = new Intent(this, DocumentsActivity.class);
        } else if (id == R.id.drawer_announcements) {
            intent = InfoListActivity.intent(this, InfoListActivity.MODE_ANNOUNCEMENTS);
        } else if (id == R.id.drawer_profile) {
            intent = new Intent(this, ProfileActivity.class);
        } else if (id == R.id.drawer_settings) {
            intent = new Intent(this, SettingsActivity.class);
        } else {
            return;
        }
        startActivity(intent);
        if (!(this instanceof DashboardActivity)) finish();
    }

    /**
     * targetSdk 36 draws edge-to-edge, so the navigation bar overlaps the bottom
     * of the window. Pad the screen's scroll container by that inset (it has
     * clipToPadding=false) so content still scrolls behind the bar but the last
     * item always comes to rest fully above it.
     */
    private void applyScrollInsets() {
        View scroll = findViewById(R.id.scroll_content);
        if (scroll == null) return;
        final int baseLeft = scroll.getPaddingLeft();
        final int baseRight = scroll.getPaddingRight();
        final int baseBottom = scroll.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(baseLeft + bars.left, v.getPaddingTop(),
                    baseRight + bars.right, baseBottom + bars.bottom);
            return insets;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // The bell uses a custom action view (icon + count badge), so its click
        // is wired here rather than arriving through onOptionsItemSelected.
        MenuItem bell = menu.findItem(R.id.action_notifications);
        bell.setVisible(showsNotificationBell());
        View bellView = bell.getActionView();
        if (bellView != null && bell.isVisible()) {
            // Items posted since Notifications was last opened; no badge at 0.
            int count = CareData.newNotificationCount(
                    StaffData.forUser(session.getUsername()), StaffData.nowInMalaysia(),
                    session.getNotificationsOpenedAt());
            TextView badge = bellView.findViewById(R.id.text_bell_count);
            badge.setText(String.valueOf(count));
            badge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            bellView.setOnClickListener(v ->
                    InfoListActivity.open(this, InfoListActivity.MODE_NOTIFICATIONS));
        }
        return true;
    }

    /** Rebuilds the menu on every return to the screen so the bell count is current. */
    @Override
    protected void onResume() {
        super.onResume();
        if (showsNotificationBell()) invalidateOptionsMenu();
    }

    /** Whether this screen shows the notification bell in its top bar. */
    protected boolean showsNotificationBell() {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(@Nullable MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            logout();
            return true;
        }
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void logout() {
        // The 'logout' event captures the current token synchronously (ApiClient
        // reads it before dispatching to the background thread), so it still
        // belongs to this session. Only AFTER that do we clear the token, so the
        // next visit begins a fresh session.
        ApiClient.track(this, "logout", "Staff logged out from " + getClass().getSimpleName());
        SessionManager.get(this).clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
