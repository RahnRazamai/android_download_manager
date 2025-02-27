package com.tk.android_download_manager;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;

public class AndroidDownloadManagerPlugin implements FlutterPlugin, ActivityAware {
    private MethodChannel methodChannel;
    private EventChannel eventChannel;
    private Activity activity;
    private Context context;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        this.context = binding.getApplicationContext();
        setupChannels(binding.getBinaryMessenger(), this.context);
    }

    private void setupChannels(BinaryMessenger messenger, Context context) {
        methodChannel = new MethodChannel(messenger, "download_manager");
        eventChannel = new EventChannel(messenger, "download_manager/complete");
        
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadMethodChannelHandler downloadMethodChannelHandler = new DownloadMethodChannelHandler(context, manager, activity);
        DownloadBroadcastReceiver receiver = new DownloadBroadcastReceiver(context);
        
        methodChannel.setMethodCallHandler(downloadMethodChannelHandler);
        eventChannel.setStreamHandler(receiver);
    }

    private void teardownChannels() {
        if (methodChannel != null) {
            methodChannel.setMethodCallHandler(null);
            methodChannel = null;
        }
        if (eventChannel != null) {
            eventChannel.setStreamHandler(null);
            eventChannel = null;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        teardownChannels();
        context = null;
    }

    // ActivityAware methods
    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        this.activity = null;
    }
}
