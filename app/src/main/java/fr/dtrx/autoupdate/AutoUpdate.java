package fr.dtrx.autoupdate;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import fr.dtrx.androidcore.utils.Permission;
import fr.dtrx.autoupdate.Utils.UpdateUtils;

/**
 * Add this line into onCreate into your ApplicationController:
 * AutoUpdate.getInstance().enable(this, <your app id>);
 */
public class AutoUpdate implements Application.ActivityLifecycleCallbacks, ActivityCompat.OnRequestPermissionsResultCallback {

    private static AutoUpdate instance = new AutoUpdate();

    public static AutoUpdate getInstance() {
        return instance;
    }

    private Application application;
    private String applicationId;
    private boolean enabled = false;

    private SparseArray<Activity> activities = new SparseArray<>();

    // Getters

    public String getApplicationId() {
        return applicationId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // Setters

//    public static void setApplicationId(String applicationId) {
//        applicationId = applicationId;
//    }

    // Functions

    public void enable(Application application, String appId) {
        application.registerActivityLifecycleCallbacks(instance);
        this.application = application;
        this.applicationId = appId;
        this.enabled = true;
    }

    // Override

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == Permission.UPDATE_PERMISSION) {
            if (Permission.INTERNET(application) && Permission.WRITE_EXTERNAL_STORAGE(application)) {
                checkForUpdate(activities.get(Permission.UPDATE_PERMISSION));
            }
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        // Vérifie s'il y a une mise à jour de l'application
        if (Permission.INTERNET(activity) && Permission.WRITE_EXTERNAL_STORAGE(activity)) {
            checkForUpdate(activity);
        }
        else {
            activities.put(Permission.UPDATE_PERMISSION, activity);
            Permission.requestPermissions(activity, Permission.UPDATE_PERMISSION, Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    private void checkForUpdate(Activity activity) {
        if (instance.isEnabled()) {
            UpdateUtils.checkForUpdate(activity);
        }
    }
}
