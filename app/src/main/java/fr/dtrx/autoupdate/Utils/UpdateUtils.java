package fr.dtrx.autoupdate.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.webkit.MimeTypeMap;

import com.google.gson.JsonElement;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import fr.dtrx.autoupdate.AutoUpdate;
import fr.dtrx.autoupdate.R;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateUtils {

    public final static String ACTION_UPDATE_OVER = "action_update_canceled_or_over";
    private final static int cancelTime = -1;

    private static boolean canceledUpdate = false;

    /**
     * for checkForUpdate
     **/
    private static final String P_UPDATE_TEXT = "Une nouvelle version (%s) est disponible pour cette application";
    private static final String P_UPDATE_ERROR_TEXT = "Un problème est survenu lors du téléchargement de la mise à jour (%s)";
    private static final String P_UPDATING_TEXT = "Téléchargement en cours";
    private static final String P_UPDATE_BUTTON_UPDATE_LABEL = "Mettre à jour";
    private static final String P_UPDATE_BUTTON_LATER_LABEL = "Plus tard";
    private static final String P_UPDATE_BUTTON_OK_LABEL = "OK";
    private static AlertDialog updateDialog;

    public static void checkForUpdate(final Activity activity) {
        if (!NetworkUtils.isNetworkAvailable(activity)) {
            activity.sendBroadcast(new Intent(ACTION_UPDATE_OVER));
        }

        VersionService.service.getVersion(AutoUpdate.getInstance().getApplicationId()).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(@NonNull Call<JsonElement> call, @NonNull Response<JsonElement> response) {
                JsonElement json = response.body();

                int buildNumber = -1;
                int newBuildNumber = -1;
                String version = null;
                String buildLocation = null;
                int lastForceUpdateBuildNumber = 0;

                try {
                    buildNumber = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                if (json != null) {
                    newBuildNumber = json.getAsJsonObject().get("build_number").getAsInt();
                    version = json.getAsJsonObject().get("version").getAsString();
                    buildLocation = json.getAsJsonObject().get("build_location").getAsString();
                    lastForceUpdateBuildNumber = json.getAsJsonObject().get("last_force_update_build_number").getAsInt();
                }

                boolean shouldForceUpdate = buildNumber < lastForceUpdateBuildNumber;

                if (buildNumber > -1 && newBuildNumber > -1 && buildNumber < newBuildNumber && !canceledUpdate) {
                    alertForUpdate(activity, version, buildLocation, shouldForceUpdate);
                }
                else {
                    if (updateDialog != null) updateDialog.dismiss();
                    onUpdateOver(activity);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonElement> call, @NonNull Throwable t) {
            }
        });
    }

    /**
     * Alert the user that an update is available
     *
     * @param activity Activity that call the alert
     **/
    private static void alertForUpdate(final Activity activity, final String version, final String buildLocation, boolean shouldForceUpdate) {
        AlertDialog.Builder updateDialogBuilder = new AlertDialog.Builder(activity);

        updateDialogBuilder.setMessage(String.format(P_UPDATE_TEXT, version));

        updateDialogBuilder.setPositiveButton(P_UPDATE_BUTTON_UPDATE_LABEL, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                final ProgressDialog progressDialog = ProgressDialog.show(activity, "", P_UPDATING_TEXT, true, false);
                String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + activity.getResources().getString(R.string.app_name) + ".apk";

                Ion.with(activity)
                        .load(buildLocation)
                        .progressDialog(progressDialog)
                        .write(new File(destination))
                        .setCallback(new FutureCallback<File>() {
                            @Override
                            public void onCompleted(Exception e, File file) {
                                progressDialog.dismiss();
                                if (e == null) {
                                    // Start the installation
                                    Uri fileUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".utils.GenericFileProvider", file);

                                    Intent install = new Intent(Intent.ACTION_VIEW);
                                    install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    install.setDataAndType(fileUri, getMimeType(buildLocation));
                                    activity.startActivity(install);
                                }
                                else {
                                    // Display a popup with the error message
                                    AlertDialog.Builder errorBuilder = new AlertDialog.Builder(activity);
                                    errorBuilder.setMessage(String.format(P_UPDATE_ERROR_TEXT, e.getMessage()));
                                    errorBuilder.setPositiveButton(P_UPDATE_BUTTON_OK_LABEL, null);

                                    AlertDialog errorDialog = errorBuilder.create();
                                    errorDialog.setCancelable(true);
                                    errorDialog.setCanceledOnTouchOutside(true);
                                    errorDialog.show();
                                }
                            }
                        });
            }
        });

        if (!shouldForceUpdate) {
            updateDialogBuilder.setNegativeButton(P_UPDATE_BUTTON_LATER_LABEL, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    canceledUpdate = true;
                    onUpdateOver(activity);
                }
            });
        }

        if (updateDialog != null) updateDialog.dismiss();
        updateDialog = updateDialogBuilder.create();
        updateDialog.setCancelable(false);
        updateDialog.setCanceledOnTouchOutside(false);
        updateDialog.show();

        if (cancelTime >= 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateDialog.cancel();
                }
            }, cancelTime);
        }
    }

    private static void onUpdateOver(Activity activity) {
        activity.sendBroadcast(new Intent(ACTION_UPDATE_OVER));
    }

    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

}
