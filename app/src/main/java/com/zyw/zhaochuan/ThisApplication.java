package com.zyw.zhaochuan;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Created by zyw on 2016/7/23.
 */
public class ThisApplication extends Application {
    public static Bitmap commonFileBmp,folderBmp ,remoteFolderBmp,imageFileBmp,videoFileBmp,audioFileBmp,achieveFileBmp,pptFileBmp, excelFileBmp,wordFileBmp,apkFileBmp;
    private  String fileRoot;
    private  int appPort;

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
        fileRoot="";
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
    }
    public String getFileRoot()
    {
        return fileRoot;
    }

    public void setFileRoot(String path)
    {
        fileRoot=path;
    }


    public int getAppPort()
    {
        return appPort;
    }
}
