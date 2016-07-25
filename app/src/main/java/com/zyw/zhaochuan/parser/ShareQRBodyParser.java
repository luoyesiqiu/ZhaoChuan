package com.zyw.zhaochuan.parser;


import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by zyw on 2016/7/24.
 */
public class ShareQRBodyParser  {


    public ShareQRBodyParser(String url, String key) throws JSONException {
        this.url = url;
        this.key = key;
        JSONObject obj=new JSONObject();
        obj.put("url",url);
        obj.put("key",key);
        json=obj.toString();
    }

    @Override
    public String toString() {
        return  json;
    }

    private String url;
    private  String key;
    private  String json;
}
