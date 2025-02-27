package com.tk.android_download_manager;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class DownloadMethodChannelHandler implements MethodChannel.MethodCallHandler {
    private final Context context;
    private final DownloadManager manager;
    private final Activity activity;

    public DownloadMethodChannelHandler(Context context, DownloadManager manager, Activity activity) {
        this.context = context;
        this.manager = manager;
        this.activity = activity;
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        switch (call.method) {
            case "requestPermission":
                handlePermissionRequest(result);
                break;
            case "enqueue":
                handleEnqueue(call, result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void handlePermissionRequest(@NonNull MethodChannel.Result result) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // No need to request permissions for SDK < 23
            result.success(true);
            return;
        }

        if (activity == null) {
            result.error("ACTIVITY_NULL", "Activity is not attached", null);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33) requires READ_MEDIA_IMAGES & READ_MEDIA_VIDEO instead of WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED) {
                result.success(true);
            } else {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO},
                        1000);
            }
        } else {
            // For Android 6.0 - 12 (API 23 - 32), request WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                result.success(true);
            } else {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1000);
            }
        }
    }

    private void handleEnqueue(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        if (manager == null) {
            result.error("DOWNLOAD_MANAGER_NULL", "DownloadManager is not initialized", null);
            return;
        }

        String downloadUrl = call.argument("downloadUrl");
        String fileName = call.argument("fileName");
        String downloadPath = call.argument("downloadPath");
        String description = call.argument("description");
        Map<String, String> headers = call.argument("headers");
        Boolean allowScanningByMediaScanner = call.argument("allow_scanning_by_media_scanner");
        Integer notificationVisibility = call.argument("notification_visibility");

        if (downloadUrl == null || fileName == null || downloadPath == null) {
            result.error("INVALID_ARGUMENTS", "Download URL, file name, and path cannot be null", null);
            return;
        }

        Long downloadId = enqueue(downloadUrl, fileName, downloadPath, headers, allowScanningByMediaScanner, description, notificationVisibility);
        result.success(downloadId);
    }

    private Long enqueue(String downloadUrl, String fileName, String downloadPath, Map<String, String> headers,
                         Boolean allowScanningByMediaScanner, String description, Integer notificationVisibility) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));

        // Add request headers if available
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.addRequestHeader(entry.getKey(), entry.getValue());
            }
        }

        if (allowScanningByMediaScanner == null || allowScanningByMediaScanner) {
            request.allowScanningByMediaScanner();
        }

        request.setDestinationUri(Uri.fromFile(new File(downloadPath, fileName)));
        request.setTitle(fileName);
        request.setAllowedOverRoaming(true);
        request.setDescription(description != null ? description : "Downloading file...");
        request.setNotificationVisibility(notificationVisibility != null ? notificationVisibility : DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        return manager.enqueue(request);
    }
}
