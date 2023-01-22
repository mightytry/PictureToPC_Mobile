package com.example.picturetopc;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class DownloadBroadcast extends BroadcastReceiver {
    private static final String TAG = "DownloadBroadcast";

    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    public long downloadID;
    private final DownloadManager downloadManager;

    public DownloadBroadcast(DownloadManager downloadmanager ,long id){
        downloadID = id;
        downloadManager = downloadmanager;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pendingResult = goAsync();

        backgroundExecutor.execute(() -> {
            try {
                Log.d("Download", String.valueOf(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)));
                Log.d("Download", String.valueOf(downloadID));
                if(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0) == downloadID)
                {
                    try {
                        Uri a = downloadManager.getUriForDownloadedFile(downloadID);
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        i.setDataAndType(a, context.getContentResolver().getType(a));
                        context.unregisterReceiver(this);
                        context.startActivity(i);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                // Must call finish() so the BroadcastReceiver can be recycled
                pendingResult.finish();
            }
        });
    }
}