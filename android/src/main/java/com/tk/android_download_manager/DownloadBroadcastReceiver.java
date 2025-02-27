package com.tk.android_download_manager;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.tk.android_download_manager.models.DownloadAction;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;

public class DownloadBroadcastReceiver extends BroadcastReceiver implements EventChannel.StreamHandler {
    private final Context context;
    private EventChannel.EventSink events;
    private boolean isReceiverRegistered = false;

    public DownloadBroadcastReceiver(Context context) {
        this.context = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (events == null || intent.getAction() == null) return;

        long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        if (downloadId == -1) return;

        Map<String, String> result = new HashMap<>();
        result.put("id", String.valueOf(downloadId));

        switch (intent.getAction()) {
            case DownloadManager.ACTION_DOWNLOAD_COMPLETE:
                result.put("action", String.valueOf(DownloadAction.Downloaded.ordinal()));
                break;
            case DownloadManager.ACTION_NOTIFICATION_CLICKED:
                result.put("action", String.valueOf(DownloadAction.NotificationClicked.ordinal()));
                break;
            default:
                return;
        }

        if (events != null) {
            events.success(result);
        }
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        this.events = events;

        if (!isReceiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            filter.addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED);
            context.registerReceiver(this, filter);
            isReceiverRegistered = true;
        }
    }

    @Override
    public void onCancel(Object arguments) {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(this);
                isReceiverRegistered = false;
            } catch (IllegalArgumentException e) {
                // Receiver was not registered, ignore
            }
        }
        this.events = null;
    }
}
