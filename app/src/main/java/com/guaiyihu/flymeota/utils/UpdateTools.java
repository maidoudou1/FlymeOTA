package com.guaiyihu.flymeota.utils;

import android.app.DownloadManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

/**
 * Created by GuaiYiHu on 16/6/13.
 */
public class UpdateTools {

    public void download(String ri, String newVersion, DownloadManager downloadManager) {

        Uri uri = Uri.parse(ri);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setDestinationInExternalPublicDir("download", newVersion + ".zip");
        request.setDescription(newVersion + "下载");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setVisibleInDownloadsUi(true);
        downloadManager.enqueue(request);
    }

    public void sendUpdateMessage(int i, Handler handler){
        Message message = handler.obtainMessage();
        message.what = i;
        handler.sendMessage(message);
    }

}
