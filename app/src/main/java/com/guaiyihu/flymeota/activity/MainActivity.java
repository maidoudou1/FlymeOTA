package com.guaiyihu.flymeota.activity;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.RecoverySystem;
import android.widget.Toast;

import com.guaiyihu.flymeota.R;
import com.guaiyihu.flymeota.utils.UpdateTools;

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
    private String OTASite = "https://raw.githubusercontent.com/GuaiYiHu/FlymeOTASite/master/update.json";
    private Button check;
    private TextView newMessage;
    private TextView version;
    private File otaZip = null;
    private IntentFilter intentFilter;
    private int netState = 0;//0.Wi-Fi,1.移动数据,2.无网络
    private int mode = 0;//0.检查更新,1.下载更新,2.正在下载,3.立即安装,4.正在检查更新
    private NetworkChangeReceiver networkChangeReceiver;
    private DownloadReceiver receiver;
    private AlertDialog.Builder dialog;
    private DownloadManager dManager;
    private UpdateTools updateTools;
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
                    mode = 3;
                    break;
                case 4:
                    check.setText("正在检查更新...");
                    mode = 4;
                    break;
                case 5:
                    check.setText("检查更新");
                    newMessage.setText("当前无可用更新");
                    mode = 0;
                    break;
                case 6:
                    check.setText("检查更新");
                    newMessage.setText("无法检查更新");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        updateTools = new UpdateTools();
        currentVersion = SystemProperties.get("ro.build.display.id");
        check = (Button)findViewById(R.id.check);
        version = (TextView)findViewById(R.id.version);
        newMessage = (TextView)findViewById(R.id.message);
        receiver = new DownloadReceiver();
        intentFilter=new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        networkChangeReceiver=new NetworkChangeReceiver();
        registerReceiver(networkChangeReceiver,intentFilter);
        dialog =new AlertDialog.Builder(MainActivity.this);
        dManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        version.setText("当前版本 " + currentVersion);

        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(netState == 2){
                    updateTools.sendUpdateMessage(6, handler);
                    Toast.makeText(MainActivity.this, "当前没有网络", Toast.LENGTH_SHORT).show();
                }else {
                    if(mode == 0) {//0模式点击后检查更新
                        check(OTASite);
                        updateTools.sendUpdateMessage(4, handler);
                    } else if(mode == 1) {//1模式监听下载事件然后
                        updateTools.download(uri, newVersion, dManager);
                        updateTools.sendUpdateMessage(2, handler);
                    }else if(mode == 3) {//3模式执行安装
                        otaZip = Environment.getExternalStoragePublicDirectory("/sdcard/Download/" + newVersion + ".zip");
                        try {
                            RecoverySystem.installPackage(MainActivity.this, otaZip);
                        } catch (IOException e) {
                            Log.e("", e.getMessage());
                        }
                    }
                }


            }
        });

    }

    private void check(final String site) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpGet get = new HttpGet(site);
                HttpClient httpClient = new DefaultHttpClient();
                try {
                    HttpResponse httpResponse = httpClient.execute(get);
                    String s = EntityUtils.toString(httpResponse.getEntity());
                    parseJSONWithJSONObject(s);
                    if(newVersion.equals(currentVersion)){
                        updateTools.sendUpdateMessage(5, handler);

                    }else {
                        updateTools.sendUpdateMessage(1, handler);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        }).start();

    }

    class DownloadReceiver extends BroadcastReceiver {

        @SuppressLint("NewApi")
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                updateTools.sendUpdateMessage(3, handler);
            }
        }
    }

    class NetworkChangeReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context,Intent intent){
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mMobile = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (mWifi.isConnected()) {
                netState = 0;

            }else if(mMobile.isConnected()){
                netState = 1;
                Toast.makeText(context, "注意：你在使用手机流量哟~", Toast.LENGTH_SHORT).show();
            }else{
                netState = 2;
                dialog.setTitle("当前没有网络！");
                dialog.setMessage("请检查你的网络连接！");
                dialog.setCancelable(true);
                dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                dialog.setNegativeButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent =  new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                        startActivity(intent);
                    }
                });
                dialog.show();
            }
        }
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

    @Override
    protected void onResume() {
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(networkChangeReceiver,intentFilter);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if(receiver != null){
            unregisterReceiver(receiver);
        }
        if(networkChangeReceiver != null){
            unregisterReceiver(networkChangeReceiver);
        }
        super.onDestroy();
    }

}
