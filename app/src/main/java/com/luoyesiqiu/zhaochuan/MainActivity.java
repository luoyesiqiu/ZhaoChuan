package com.luoyesiqiu.zhaochuan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.transition.Explode;
import android.view.View;
import android.view.MenuItem;
import android.widget.Button;


import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTabHost;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.luoyesiqiu.zhaochuan.activity.CaptureConnectQRActivity;
import com.luoyesiqiu.zhaochuan.activity.SessionActivity;
import com.luoyesiqiu.zhaochuan.activity.ShareActivity;
import com.luoyesiqiu.zhaochuan.util.Utils;
import com.luoyesiqiu.zhaochuan.wifi.WifiAP;

import java.util.ArrayList;

import rebus.permissionutils.AskAgainCallback;
import rebus.permissionutils.FullCallback;
import rebus.permissionutils.PermissionEnum;
import rebus.permissionutils.PermissionManager;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private WifiManager wifiManager;
    private WifiAP wifiAP;
    private Button bn_create;
    private  Button bn_connect;
    NavigationView navigationView;
    private boolean isServer;//标记是否是服务端
    public  static  MainActivity thiz;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        reqPermissions();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        bn_connect=(Button)findViewById(R.id.main_bn_connect);
        bn_create=(Button)findViewById(R.id.main_bn_create);
        bn_create.setOnClickListener(new ButtonOnClick());
        bn_connect.setOnClickListener(new ButtonOnClick());
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiAP =new WifiAP(wifiManager);
        setupWindowExitAnimation();

        thiz=this;
    }

    private void reqPermissions(){
        PermissionManager.Builder()
                .permission(PermissionEnum.WRITE_EXTERNAL_STORAGE,PermissionEnum.CAMERA)
                .askAgain(true)
                .askAgainCallback(new AskAgainCallback() {
                    @Override
                    public void showRequestPermission(UserResponse response) {

                    }
                })
                .callback(new FullCallback() {
                    @Override
                    public void result(ArrayList<PermissionEnum> permissionsGranted, ArrayList<PermissionEnum> permissionsDenied, ArrayList<PermissionEnum> permissionsDeniedForever, ArrayList<PermissionEnum> permissionsAsked) {
                    }
                })
                .ask(this);
    }
    private  void setupWindowExitAnimation()
    {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Explode explode=new Explode();
            explode.setDuration(2000);
            getWindow().setExitTransition(explode);
        }

    }
    /**
     * 按钮事件
     */
    private final class ButtonOnClick implements View.OnClickListener {
        Intent intent;
        private  boolean isOpen=false;
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                //连接其他
                case R.id.main_bn_connect:
                    //是否打开wifi
                    if(wifiManager.isWifiEnabled()){
                     intent=new Intent(MainActivity.this, CaptureConnectQRActivity.class);
                    startActivity(intent);
                    }
                    else{
                        //Snackbar.make(navigationView,"请先打开并连接WIFI。",Snackbar.LENGTH_SHORT).show();
                        //打开wifi
                        wifiManager.setWifiEnabled(true);
                        intent=new Intent(MainActivity.this,CaptureConnectQRActivity.class);
                        startActivity(intent);
                       // wifiAP.CreateWifiInfo(Utils.getRandomSSID(),Utils.getRandomKey(), WifiAP.WifiCipherType.WIFICIPHER_INVALID);

                    }
                    break;
                //创建连接
                case R.id.main_bn_create:
                    if(wifiManager.isWifiEnabled()){
                        //如果wifi是开着的
                        isServer=true;
                        intent=new Intent(MainActivity.this,SessionActivity.class);
                        intent.setAction(SessionActivity.ACTION_SHOW_QR);
                        intent.putExtra("isServer",isServer);
                        intent.putExtra("local_ip",Utils.getIpAddress(wifiManager));
                        intent.putExtra("isAsAP",false);
                        startActivity(intent);
                    }
                else{
                        //如果wifi是关的,打开热点
                        String ssid= Utils.getRandomSSID();
                        String key=Utils.getRandomKey();
                        wifiAP.startWifiAp(ssid,key);
                        isServer=true;
                        intent=new Intent(MainActivity.this,SessionActivity.class);
                        intent.setAction(SessionActivity.ACTION_SHOW_QR);
                        intent.putExtra("isServer",isServer);
                        intent.putExtra("local_ip",Utils.getIpAddress(wifiManager));
                        intent.putExtra("isAsAP",true);
                        intent.putExtra("ap_ssid",ssid);
                        intent.putExtra("ap_key",key);
                        startActivity(intent);
                        Snackbar.make(navigationView,"WIFI热点已打开。",Snackbar.LENGTH_SHORT).show();
                  }

                    break;
            }
        }
    }

    /**
     * 返回键按下时
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            wifiAP.closeWifiAp();
            super.onBackPressed();

        }
    }



    /**
     * 导航菜单被点击后
     * @param item
     * @return
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        switch (id){
//            //设置
//            case R.id.nav_setting:
//                 intent=new Intent(MainActivity.this, SettingActivity.class);
//                startActivity(intent);
//                break;
            //分享
            case R.id.nav_share:
                intent=new Intent(MainActivity.this, ShareActivity.class);
                startActivity(intent);
                break;
            //关于
            case  R.id.nav_about:
                AlertDialog alertDialog=new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.app_name))
                        .setMessage(getResources().getString(R.string.about_dialog))
                        .setPositiveButton("我知道了",null)
                        .create();

                alertDialog.show();
                break;

        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.handleResult(this, requestCode, permissions, grantResults);
    }
}

