package com.guaiyihu.flymeota;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.RecoverySystem;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private String currentVersion;//获取当前flyme版本
    private String OTAMessage;//获取更新信息
    private String uri;//获取下载链接
    private String newVersion;//获取服务器上面的新版本号
    private String s = null;
    private Button check;
    private TextView newMessage;
    private TextView version;
    private File otaZip = null;
    private int mode = 0;//0.检查更新,1.下载更新,2.正在下载,3.立即安装,4.正在检查更新
    private DownloadReceiver receiver;
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    check.setText("立即下载");
                    newMessage.setText(OTAMessage);
                    mode = 1;
                    break;
                case 2:
                    check.setText("正在下载");
                    mode = 2;
                    break;
                case 3:
                    check.setText("立即安装");
                    break;
                case 4:
                    check.setText("正在检查更新...");
                    mode = 4;
                    break;
                case 5:
                    check.setText("检查更新");
                    mode = 0;
                    break;
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentVersion = SystemProperties.get("ro.build.display.id");
        check = (Button)findViewById(R.id.check);
        version = (TextView)findViewById(R.id.version);
        newMessage = (TextView)findViewById(R.id.message);
        receiver = new DownloadReceiver();

        version.setText("当前版本" + "\n" + currentVersion);

        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mode == 0) {//0模式点击后检查更新
                    check();
                    Message message = handler.obtainMessage();//更新ui中的按钮,其中按钮没有任何绑定事件
                    message.what = 4;
                    handler.sendMessage(message);
               } else if(mode == 1) {//1模式监听下载事件然后
                    download(uri);
                    Message message = handler.obtainMessage();
                    message.what = 2;
                    handler.sendMessage(message);
                }else if(mode == 3) {//3模式执行安装
                    otaZip = Environment.getExternalStoragePublicDirectory("/sdcard/Download/" + newVersion + ".zip");
                    try {
                        RecoverySystem.installPackage(MainActivity.this, otaZip);
                    } catch (IOException e) {
                        Log.e("", e.getMessage());
                    }
                }


            }
        });

    }

    private void check() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpGet get = new HttpGet("https://raw.githubusercontent.com/GuaiYiHu/FlymeOTASite/master/update.json");
                HttpClient httpClient = new DefaultHttpClient();
                try {
                    HttpResponse httpResponse = httpClient.execute(get);
                    s = EntityUtils.toString(httpResponse.getEntity());
                    parseJSONWithJSONObject(s);
                    if(newVersion.equals(currentVersion)){
                        Message message = handler.obtainMessage();//检查更新完了更改ui
                        message.what = 5;
                        handler.sendMessage(message);
                        newMessage.setText("当前无可用更新");
                    }else {
                        Message message = handler.obtainMessage();//检查更新完了更改ui
                        message.what = 1;
                        handler.sendMessage(message);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        }).start();

    }

    private void parseJSONWithJSONObject(String jsonData) {

        try {
            JSONArray jsonArray = new JSONArray(jsonData);
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            try {
                newVersion = jsonObject.getString("version");
                uri = jsonObject.getString("Uri");
                OTAMessage = jsonObject.getString("message");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void download(String ri) {
        DownloadManager dManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(ri);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setDestinationInExternalPublicDir("download", newVersion + ".zip");
        request.setDescription(newVersion + "下载");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setVisibleInDownloadsUi(true);
        dManager.enqueue(request);
    }

    class DownloadReceiver extends BroadcastReceiver {

        @SuppressLint("NewApi")
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                Message message = handler.obtainMessage();
                message.what = 3;
                handler.sendMessage(message);
                mode = 3;
            }
        }
    }

    @Override
    protected void onResume() {
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if(receiver != null)unregisterReceiver(receiver);
        super.onDestroy();
    }

}
