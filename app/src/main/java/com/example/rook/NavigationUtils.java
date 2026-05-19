package com.example.rook;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
        ShapeableImageView iconProfile = activity.findViewById(R.id.iconProfile);

        // Reset all to inactive
        if (iconHome != null) {
            iconHome.setColorFilter(inactiveColor);
            iconHome.setScaleX(1.0f); iconHome.setScaleY(1.0f);
        }
        if (iconSearch != null) {
            iconSearch.setColorFilter(inactiveColor);
            iconSearch.setScaleX(1.0f); iconSearch.setScaleY(1.0f);
        }
        if (iconAdd != null) {
            iconAdd.setColorFilter(inactiveColor);
            iconAdd.setScaleX(1.0f); iconAdd.setScaleY(1.0f);
        }
        if (iconAnalytics != null) {
            iconAnalytics.setColorFilter(inactiveColor);
            iconAnalytics.setScaleX(1.0f); iconAnalytics.setScaleY(1.0f);
        }
        
        // Setup Profile Icon with Glide
        if (iconProfile != null) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && currentUser.getPhotoUrl() != null) {
                iconProfile.clearColorFilter(); // Don't tint the actual photo
                Glide.with(activity)
                        .load(currentUser.getPhotoUrl())
                        .placeholder(R.drawable.ic_nav_profile)
                        .into(iconProfile);
            } else {
                iconProfile.setColorFilter(inactiveColor);
            }
            iconProfile.setStrokeColorResource(R.color.on_surface_variant);
            iconProfile.setScaleX(1.0f); iconProfile.setScaleY(1.0f);
        }

        // Apply active states
        if (activity instanceof MainActivity && iconHome != null) {
            iconHome.setColorFilter(primaryColor);
            iconHome.setScaleX(1.15f); iconHome.setScaleY(1.15f);
        } else if (activity instanceof MyApisActivity && iconSearch != null) {
            iconSearch.setColorFilter(primaryColor);
            iconSearch.setScaleX(1.15f); iconSearch.setScaleY(1.15f);
        } else if (activity instanceof ApiLabActivity && iconAdd != null) {
            iconAdd.setColorFilter(primaryColor);
            iconAdd.setScaleX(1.1f); iconAdd.setScaleY(1.1f);
        } else if (activity instanceof ReportsActivity && iconAnalytics != null) {
            iconAnalytics.setColorFilter(primaryColor);
            iconAnalytics.setScaleX(1.15f); iconAnalytics.setScaleY(1.15f);
        } else if (activity instanceof UserProfileActivity && iconProfile != null) {
            iconProfile.setStrokeColorResource(R.color.primary);
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null || currentUser.getPhotoUrl() == null) {
                iconProfile.setColorFilter(primaryColor);
            }
            iconProfile.setScaleX(1.15f); iconProfile.setScaleY(1.15f);
        }
    }
}
