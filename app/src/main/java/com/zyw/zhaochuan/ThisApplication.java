package com.zyw.zhaochuan;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.zyw.zhaochuan.util.Utils;

import java.io.File;

/**
 * Created by zyw on 2016/7/23.
 */
public class ThisApplication extends Application {
    public static Bitmap commonFileBmp,folderBmp ,remoteFolderBmp,imageFileBmp,videoFileBmp,audioFileBmp,achieveFileBmp,pptFileBmp, excelFileBmp,wordFileBmp,apkFileBmp;
    private  String fileRoot;
    private  int appPort;

    public String getLocalIP() {
        return localIP;
    }

    public void setLocalIP(String localIP) {
        this.localIP = localIP;
    }

    public String getRemoteIP() {
        return remoteIP;
    }

    public void setRemoteIP(String remoteIP) {
        this.remoteIP = remoteIP;
    }

    private String localIP;
    private String remoteIP;
    public boolean isCopyFromLocal() {
        return isCopyFromLocal;
    }

    public void setCopyFromLocal(boolean copyFromLocal) {
        isCopyFromLocal = copyFromLocal;
    }

    private  boolean isCopyFromLocal=false;
    @Override
    public void onCreate() {
        super.onCreate();
        appPort=3574;
        this.fileRoot="";
        commonFileBmp= BitmapFactory.decodeResource(getResources(),R.mipmap.common_file);
        imageFileBmp=BitmapFactory.decodeResource(getResources(),R.mipmap.image_file);
        audioFileBmp=BitmapFactory.decodeResource(getResources(),R.mipmap.audio_file);
        videoFileBmp=BitmapFactory.decodeResource(getResources(),R.mipmap.video_file);
        achieveFileBmp=BitmapFactory.decodeResource(getResources(),R.mipmap.achieve_file);
        apkFileBmp=BitmapFactory.decodeResource(getResources(),R.mipmap.apk_file);
        folderBmp=BitmapFactory.decodeResource(getResources(),R.mipmap.folder);
        remoteFolderBmp=BitmapFactory.decodeResource(getResources(),R.mipmap.remote_folder);
        pptFileBmp=BitmapFactory.decodeResource(getResources(),R.mipmap.ppt_file);
        excelFileBmp =BitmapFactory.decodeResource(getResources(),R.mipmap.excel_file);
        wordFileBmp=BitmapFactory.decodeResource(getResources(),R.mipmap.word_file);

        File logFile=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"zhaochuan.txt");//删除日志
        System.out.println("日志文件："+logFile);
        if(logFile.exists()&&logFile.length()>1024*100)//当日志文件存在并且大于100k
            logFile.delete();
    }
    public String getFileRoot()
    {        return this.fileRoot;

    }

    public void setFileRoot(String path)
    {
        this.fileRoot=path;
    }


    public int getAppPort()
    {
        return appPort;
    }
}
