package com.luoyesiqiu.zhaochuan.entity;

import java.io.File;
import java.io.Serializable;


/**
 * Created by zyw on 2016/7/24.
 */
public class FileSessionList  implements Serializable {


    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public String getFileKey() {
        return fileKey;
    }

    private File file;
    private  String fileName;
    private  String fileSize;
    private  String fileKey;

}
