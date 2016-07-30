package com.zyw.zhaochuan.entity;

import cn.bmob.v3.BmobObject;

/**
 * Created by zyw on 2016/7/24.
 */
public class FileSessionList extends BmobObject {


    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    private String url;
    private  String fileName;
    private  String fileSize;
    private  String fileKey;

}
