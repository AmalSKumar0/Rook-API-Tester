package com.example.rook;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/**
 * Shared app chrome wiring for the reusable header and bottom navigation.
 */
public class NavigationUtils {

    public static void setupAppHeader(Activity activity, String title, boolean showBack) {
        TextView headerTitle = activity.findViewById(R.id.headerTitle);
        TextView headerSubtitle = activity.findViewById(R.id.headerSubtitle);
        ImageView headerBack = activity.findViewById(R.id.headerBack);
        ImageView headerCollections = activity.findViewById(R.id.headerCollections);
        ImageView headerReports = activity.findViewById(R.id.headerReports);

        if (headerTitle == null) return;

        headerTitle.setText(title != null && !title.isEmpty() ? title : activity.getString(R.string.app_name));
        if (headerSubtitle != null) {
            headerSubtitle.setText("API testing workspace");
        }

        if (headerBack != null) {
            headerBack.setVisibility(showBack ? View.VISIBLE : View.GONE);
            headerBack.setOnClickListener(v -> activity.finish());
        }

        if (headerCollections != null) {
            headerCollections.setOnClickListener(v -> {
                if (!(activity instanceof MyApisActivity)) {
                    activity.startActivity(new Intent(activity, MyApisActivity.class));
                    activity.overridePendingTransition(0, 0);
                }
            });
        }

        if (headerReports != null) {
            headerReports.setOnClickListener(v -> {
                if (!(activity instanceof ReportsActivity)) {
                    activity.startActivity(new Intent(activity, ReportsActivity.class));
                    activity.overridePendingTransition(0, 0);
                }
            });
        }
    }

    public static void setupAppChrome(Activity activity, String title, boolean showBack) {
        setupAppHeader(activity, title, showBack);
        setupBottomNav(activity);
    }

    public static void setupBottomNav(Activity activity) {
        LinearLayout navHome = activity.findViewById(R.id.navHome);
        LinearLayout navSearch = activity.findViewById(R.id.navSearch);
        LinearLayout navAdd = activity.findViewById(R.id.navAdd);
        LinearLayout navAnalytics = activity.findViewById(R.id.navAnalytics);
        LinearLayout navProfile = activity.findViewById(R.id.navProfile);

        if (navHome == null) return;

        navHome.setOnClickListener(v -> {
            if (!(activity instanceof MainActivity)) {
                activity.startActivity(new Intent(activity, MainActivity.class));
                activity.overridePendingTransition(0, 0);
            }
        });

        navSearch.setOnClickListener(v -> {
            if (!(activity instanceof MyApisActivity)) {
                activity.startActivity(new Intent(activity, MyApisActivity.class));
                activity.overridePendingTransition(0, 0);
            }
        });

        navAdd.setOnClickListener(v -> {
            if (!(activity instanceof ApiLabActivity)) {
                activity.startActivity(new Intent(activity, ApiLabActivity.class));
                activity.overridePendingTransition(0, 0);
            }
        });

        navAnalytics.setOnClickListener(v -> {
            if (!(activity instanceof ReportsActivity)) {
                activity.startActivity(new Intent(activity, ReportsActivity.class));
                activity.overridePendingTransition(0, 0);
            }
        });

        navProfile.setOnClickListener(v -> {
            if (!(activity instanceof UserProfileActivity)) {
                activity.startActivity(new Intent(activity, UserProfileActivity.class));
                activity.overridePendingTransition(0, 0);
            }
        });

        int primaryColor = ContextCompat.getColor(activity, R.color.primary);
        int inactiveColor = ContextCompat.getColor(activity, R.color.on_surface_variant);

        ImageView iconHome = activity.findViewById(R.id.iconHome);
        ImageView iconSearch = activity.findViewById(R.id.iconSearch);
        ImageView iconAdd = activity.findViewById(R.id.iconAdd);
        ImageView iconAnalytics = activity.findViewById(R.id.iconAnalytics);
        ImageView iconProfile = activity.findViewById(R.id.iconProfile);

        if (iconHome != null) iconHome.setColorFilter(inactiveColor);
        if (iconSearch != null) iconSearch.setColorFilter(inactiveColor);
        if (iconAdd != null) iconAdd.setColorFilter(inactiveColor);
        if (iconAnalytics != null) iconAnalytics.setColorFilter(inactiveColor);
        if (iconProfile != null) iconProfile.setColorFilter(inactiveColor);

        if (activity instanceof MainActivity && iconHome != null) {
            iconHome.setColorFilter(primaryColor);
        } else if (activity instanceof MyApisActivity && iconSearch != null) {
            iconSearch.setColorFilter(primaryColor);
        } else if (activity instanceof ApiLabActivity && iconAdd != null) {
            iconAdd.setColorFilter(primaryColor);
        } else if (activity instanceof ReportsActivity && iconAnalytics != null) {
            iconAnalytics.setColorFilter(primaryColor);
        } else if (activity instanceof UserProfileActivity && iconProfile != null) {
            iconProfile.setColorFilter(primaryColor);
        }
    }
}
