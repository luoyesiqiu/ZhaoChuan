package com.zyw.zhaochuan.activity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.support.v4.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.transition.Explode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.zyw.zhaochuan.R;
import com.zyw.zhaochuan.ThisApplication;
import com.zyw.zhaochuan.fragment.SessionFragment;
import com.zyw.zhaochuan.fragment.ShowConnectQRFragment;
import com.zyw.zhaochuan.interfaces.OnTransProgressChangeListener;
import com.zyw.zhaochuan.services.TcpService;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by zyw on 2016/5/14.
 * 要做的：屏蔽返返回键。
 * 文件从远程到本地时，本地刷新列表
 */
public class SessionActivity extends AppCompatActivity implements OnTransProgressChangeListener {

    public static  final  String ACTION_SHOW_QR="show_qr";
    public static  final  String ACTION_SHOW_SESSION="show_session";
    public static  final String NOTICE_BACKKEY_PRESS="back_key_press";
    public static SessionActivity thiz;
    private  boolean isServer;
    public static Intent intent;
    private FragmentTransaction transaction;
    public  static TcpService tcpService;
    private  final String TAG="SessionActivity";
    //private ProgressDialog progressDialog;


    private Notification.Builder notiBuilder;
    private NotificationManager notificationManager;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container);
        thiz=this;
        intent=getIntent();
        //加载资源

        isServer=intent.getBooleanExtra("isServer",false);
        String remote_ip=intent.getStringExtra("remote_ip");//这里得到的是对方的ip,如果是创建连接的那一方他是得不到对方ip的
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent servIntent = new Intent(SessionActivity.this, TcpService.class);
        servIntent.putExtra("start_action", TcpService.START_ACTION_LISTEN);
        servIntent.putExtra("remote_ip",remote_ip);
        servIntent.putExtra("remote_port", ((ThisApplication)getApplication()).getAppPort());
        bindService(servIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        //注册广播
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(TcpService.NOTICE_TYPE_CLIENT_CONTENTED);//收到客户连接的
       intentFilter.addAction(TcpService.NOTICE_TYPE_GETTED_MSG);//测试用
        intentFilter.addAction(TcpService.NOTICE_TYPE_FILE_CLOSE_ACTIVITY);//关闭Activity
        registerReceiver(broadcastReceiver,intentFilter);

        //fragment管理
        transaction = getSupportFragmentManager().beginTransaction();
        if(intent.getAction().equals(ACTION_SHOW_QR)) {

            transaction.replace(R.id.frag_container, new ShowConnectQRFragment());
        }
        else
        {
            transaction.replace(R.id.frag_container, new SessionFragment());

        }
        transaction.commit();
        setupWindowEnterAnimation();
        notificationManager=(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notiBuilder=new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher).setTicker("文件开始传输");
    }

    /**
     * 设置动画
     */
    private  void setupWindowEnterAnimation()
    {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Explode explode=new Explode();
            explode.setDuration(2000);
            getWindow().setExitTransition(explode);
        }

    }


    /**
     * 广播接收者
     */
    BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(TcpService.NOTICE_TYPE_CLIENT_CONTENTED)) {
                //当收到客户连接的广播
                transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.frag_container, new SessionFragment());
                transaction.commit();
            }
            //关闭Activity
            else if(intent.getAction().equals(TcpService.NOTICE_TYPE_FILE_CLOSE_ACTIVITY)){
                finish();
            }
            //收到目录，测试用
            else if(intent.getAction().equals(TcpService.NOTICE_TYPE_GETTED_MSG)) {
                String json=intent.getStringExtra("json");
                try {
                    FileWriter fw =new FileWriter("/sdcard/zhaochuan.txt",true);
                    fw.write("\n\n");
                    fw.write(getFormatedDate(System.currentTimeMillis())+"\n");
                    fw.write(json);
                    if(fw!=null)
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    /***
     *格式化时间
     * @param time
     * @return
     */
    public String getFormatedDate(long time)
    {
        //String nowStr;
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Date curDate = new Date(time);//获取当前时间
        return  sdf.format(curDate);//转换时间格式

    }
    /**
     * 监听Activity与Service关联情况
     */
      ServiceConnection serviceConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG,service.toString());
            TcpService.SimpleBinder simpleBinder=(TcpService.SimpleBinder) service;
            tcpService=simpleBinder.getService();
            //设置进度条更新
            tcpService.setOnTransProgressChangeListener(SessionActivity.this);
            if(!isServer){
                try {
                    SessionActivity.tcpService.sendConnectedMsg();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //不连接

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        if(serviceConnection!=null)
        unbindService(serviceConnection);

    }
    private  Toast toast;
    public  void showToast(CharSequence msg)
    {
        if(toast==null)
        {
            toast= Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT);
        }else
        {
            toast.setText(msg);
        }
        toast.show();
    }

    private MenuItem.OnMenuItemClickListener onMenuItemClickListener;

    public  void setMenuItemSelectedListener(MenuItem.OnMenuItemClickListener listener){
        this.onMenuItemClickListener=listener;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
       // menu.add("粘贴");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //按下左上角的返回键
        if(item.getItemId()==android.R.id.home)
        {
            new AlertDialog.Builder(this)
                    .setTitle("提示")
                    .setMessage("确定将断开连接吗？")
                    .setPositiveButton("确定",new DialogInterface.OnClickListener(){

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("取消",null)
                    .create()
                    .show();

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        //这里目的为了通知fragment，activity已经按下返回键，让她们自己处理
        Intent intent=new Intent(NOTICE_BACKKEY_PRESS);
        sendBroadcast(intent);
    }

    /**
     * 更新进度条，用回调函数
     * @param current
     * @param max
     */
    @Override
    public void onProgressChange(final long current, final long max) {
        int cur=(int)((double)current/max*100);

        notiBuilder.setProgress(100,cur,false).setContentTitle(String.format("文件传输中...(%s%%)",cur));
        if(current>=max)
        {
            //notificationManager.cancel(1);
            notiBuilder=new Notification.Builder(this);
            notiBuilder.setSmallIcon(R.mipmap.ic_launcher);
            notiBuilder.setContentTitle("传输完成").setTicker("传输完成");
        }
        notificationManager.notify(1,notiBuilder.getNotification());

    }
}
