package com.luoyesiqiu.zhaochuan.fragment;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.luoyesiqiu.zhaochuan.R;
import com.luoyesiqiu.zhaochuan.ThisApplication;
import com.luoyesiqiu.zhaochuan.activity.CaptureShareQRActivity;
import com.luoyesiqiu.zhaochuan.activity.ShareActivity;
import com.luoyesiqiu.zhaochuan.adapter.FileListAdapter;
import com.luoyesiqiu.zhaochuan.entity.FileListItem;
import com.luoyesiqiu.zhaochuan.entity.FileSessionList;
import com.luoyesiqiu.zhaochuan.interfaces.CaptureCompleteCallback;
import com.luoyesiqiu.zhaochuan.interfaces.FileListInterface;
import com.luoyesiqiu.zhaochuan.interfaces.OnTransProgressChangeListener;
import com.luoyesiqiu.zhaochuan.util.FileNameSort;
import com.luoyesiqiu.zhaochuan.util.IntentTool;
import com.luoyesiqiu.zhaochuan.util.Utils;
import com.luoyesiqiu.zhaochuan.view.RecycleViewDivider;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by zyw on 2016/7/23.
 */
public class ShareFileListFragment extends Fragment implements FileListInterface, OnTransProgressChangeListener, CaptureCompleteCallback {
    private RecyclerView recyclerView;
    private Context context;
    private FileListAdapter fileListAdapter;
    private List<FileListItem> fileListItems;
    final String SD_PATH= Environment.getExternalStorageDirectory().getAbsolutePath();
    private File[] files;
    private View rootView =null;
    private File curPath;
    private Toast toast;
    static List<FileListItem> fileListItemsCache;
    final String TAG="LocalListFragment";
    public static File willSendFilePath;
    private FloatingActionButton pasteFloatButton;
    private ProgressDialog uploadProgressDialog;
    private PackageManager pm = null;
    private ShareActivity rootAct;
    private ThisApplication application;
    private boolean isCanDownload=true;
    private ProgressDialog copyProgressDialog;
    public static final String SHARE_CAPTURE_COMPLELED="share_capture_completed";
    private NotificationManager notificationManager;
    private  Notification.Builder downloadNotiBuilder;

    boolean isUpdateSuccess=false;
    private  String objId=null;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=getContext();
        if(rootView ==null) {
            rootView = inflater.inflate(R.layout.file_list_layout, null);
            recyclerView = (RecyclerView) rootView.findViewById(R.id.filelist_reclyclerview);
            pasteFloatButton=(FloatingActionButton)rootView.findViewById(R.id.float_button_paste);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            //添加分割线
            recyclerView.addItemDecoration(new RecycleViewDivider(context, LinearLayoutManager.VERTICAL));
            fileListItems = new ArrayList<FileListItem>();
            curPath=new File(SD_PATH);
            //loadList前先加载资源
            pm=context.getPackageManager();

            rootAct=ShareActivity.thiz;
            loadList(curPath,true);
            fileListAdapter = new FileListAdapter(context, fileListItems);
            //列表项单击事件
            fileListAdapter.setOnItemClickListener(new RecyclerViewEvents());
            //列表长按事件
            fileListAdapter.setOnItemLongClickListener(new RecyclerViewEvents());
            recyclerView.setAdapter(fileListAdapter);
            //粘贴文件
            pasteFloatButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(application.isCopyFromLocal()) {
                            //从本地进行复制，开启线程复制
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        boolean notSame=Utils.copyFile(willSendFilePath, new File(curPath + File.separator + willSendFilePath.getName()), ShareFileListFragment.this);
                                        application.setCopyFromLocal(false);
                                        if(!notSame){
                                            showToast("文件复制失败");
                                        }
                                    } catch (IOException e) {
                                        rootAct.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                showToast("文件复制失败");
                                            }
                                        });
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                    }
                }
            });
            application=(ThisApplication)rootAct.getApplication();
            application.setCopyFromLocal(false);//初始设置为没有复制
            copyProgressDialog =new ProgressDialog(rootAct);
            copyProgressDialog.setTitle("文件复制中...");
            copyProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            copyProgressDialog.setMax(100);
            copyProgressDialog.setCancelable(false);
            notificationManager=(NotificationManager)rootAct.getSystemService(Context.NOTIFICATION_SERVICE);
            downloadNotiBuilder=new Notification.Builder(rootAct)
                    .setTicker("文件开始下载")
                    .setSmallIcon(R.mipmap.ic_launcher);
            //设置扫码回调
            CaptureShareQRActivity.setCaptureCompleteCallback(this);


            IntentFilter intentFilter=new IntentFilter();
            intentFilter.addAction(rootAct.NOTICE_BACKKEY_PRESS);
            rootAct.registerReceiver(broadcastReceiver,intentFilter);
        }
        showToast("长按可以分享文件噢~");
        rootAct.getSupportActionBar().show();//显示ActionBar
        rootAct.getSupportActionBar().setTitle(getShortPath(curPath.toString()));
        return rootView;
    }


    BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final  String action=intent.getAction();
            if(action.equals(rootAct.NOTICE_BACKKEY_PRESS))
            {
                goBack();
            }
        }
    };
    /**
     * 复制本地文件的进度
     * @param current
     * @param max
     */
    @Override
    public void onProgressChange(final long current,final long max) {

        rootAct.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                copyProgressDialog.setProgress((int)(((double)current/max)*100));
                copyProgressDialog.show();

                if(current>=max) {
                    copyProgressDialog.hide();
                    loadList(curPath,false);
                    fileListAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    /**
     * 扫码回调
     * @param data
     */
    @Override
    public void onCaptureComplete(Map<String, String> data) {
        String key=data.get("key");
        String url=data.get("url");
        String fileName=data.get("fileName");

    }

    /**
     * 添加数据到表
     * @param file
     * @param key
     */
    private  void addFileInfoToServer(File file,String key)
    {

    }

    private class RecyclerViewEvents implements FileListAdapter.OnRecyclerViewItemClickListener,FileListAdapter.OnRecyclerViewItemLongClickListener
    {
        /**
         * 列表单击事件
         * @param view
         * @param pos
         */
        @Override
        public void onItemClick(View view, int pos) {

            if (pos == 0 && !curPath.toString().equals("/"))
            {
                goBack();
            }
            //点击文件夹时,这里要注意数组的范围
            else if ((pos == 0 && curPath.toString().equals("/")) || files[pos-1].isDirectory())
            {
                try
                {
                    fileListItemsCache=new ArrayList<FileListItem>();
                    //深复制
                    Utils.listCopy(fileListItemsCache,fileListItems);
                    //对根目录的处理
                    if (curPath.toString().equals("/"))
                    {
                        curPath =files[pos];
                    }
                    else
                    {
                        curPath =files[pos-1];
                    }

                    loadList(curPath,true);
                    fileListAdapter.notifyDataSetChanged();
                }
                catch (Exception e)
                {
                    //发生异常时，当前路径不变，并重新载入列表
                    showToast("抱歉，发生了错误！" + "\n" + e.toString());
                    goBack();
                }
            }

            //点击文件时
            else if (files[pos-1].isFile())
            {
                Intent intent= IntentTool.openFile(files[pos-1].getAbsolutePath());
                startActivity(intent);
            }
        }


        /**
         * 列表长按事件
         * @param view
         * @param pos
         */
        @Override
        public void onItemLongClick(View view, final int pos) {

            boolean isFileItem=false;
            File selectedPath=null;
            if (pos == 0 && !curPath.toString().equals("/"))
            {
                return;
            }
            else if ((pos == 0 && curPath.toString().equals("/")) || files[pos-1].isDirectory()) {
                if (curPath.toString().equals("/")) {
                    selectedPath = files[pos];
                } else {
                    selectedPath = files[pos - 1];
                }
                isFileItem=false;
            }else if(files[pos-1].isFile()) {
                selectedPath = files[pos - 1];
                isFileItem=true;
            }
            final File finalSelectedPath = selectedPath;
            final String[] menuItem= isFileItem?getResources().getStringArray(R.array.share_context_menu_item_file):getResources().getStringArray(R.array.share_context_menu_item_folder);
            Dialog dialog = new AlertDialog.Builder(rootAct)
                    .setItems(menuItem, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (menuItem[which]) {
                                case "分享":
                                    if(isCanDownload) {
                                        isCanDownload=false;
                                        final File tempFile = files[pos - 1];

                                    }

                                break ;

                                case "复制":
                                    //复制本地的了，远程的路径清空
                                  //  RemoteListFragment.willSendFilePath=null;
                                    willSendFilePath= finalSelectedPath;
                                    Toast.makeText(rootAct,willSendFilePath.getAbsolutePath(),Toast.LENGTH_LONG).show();
                                    application.setCopyFromLocal(true);
                                    break;
                                case "重命名":
                                    //重命名
                                    final EditText editText=new EditText(context);
                                    editText.setText(finalSelectedPath.getName());
                                    AlertDialog alertDialog=new AlertDialog.Builder(context)
                                            .setTitle("重命名")
                                            .setView(editText)
                                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    String text=editText.getText().toString();
                                                    if(!text.equals(""))
                                                    {
                                                        finalSelectedPath.renameTo(new File(finalSelectedPath.getParent()+File.separator+text));
                                                        loadList(curPath,false);
                                                        fileListAdapter.notifyDataSetChanged();
                                                    }else
                                                    {
                                                        showToast("重命名失败");
                                                    }
                                                }
                                            })
                                            .create();
                                    alertDialog.show();
                                    break;

                                case "删除":
                                    //删除
                                    Utils.deleteFile(finalSelectedPath);
                                    loadList(curPath,false);
                                    fileListAdapter.notifyDataSetChanged();
                                    break;
                            }
                        }
                    })
                    .create();
            dialog.show();
        }
    }//

    /**
     * 返回上一级目录，有ui操作
     */
    public void goBack() {
        try
        {
            if(curPath.getAbsolutePath().equals("/"))
                return;
            curPath = curPath.getParentFile();
            //缓存都不为空时才能使用缓存
            if (fileListItemsCache!=null)
            {
                //深复制
                Utils.listCopy(fileListItems,fileListItemsCache);
                //使用后清空缓存
                fileListItemsCache = null;

                fileListAdapter.notifyDataSetChanged();

                files=curPath.listFiles();
                Arrays.sort(files,new FileNameSort());
            }
            //没有缓存就重新获取列表
            else
            {
                loadList(curPath,true);

                fileListAdapter.notifyDataSetChanged();
            }

            rootAct.getSupportActionBar().setTitle(getShortPath(curPath.toString()));
        }
        catch (Exception e)
        {
            showToast("抱歉，发生了错误！" + "\n" + e.toString());
        }
    }

    /**
     * 当销毁视图时发生
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((ViewGroup) rootView.getParent()).removeView(rootView);

    }

    /**
     * 显示toast
     * @param msg
     */
    public  void showToast(CharSequence msg)
    {
        if(toast==null)
        {
            toast=Toast.makeText(getContext(),msg,Toast.LENGTH_SHORT);
        }else
        {
            toast.setText(msg);
        }
        toast.show();
    }

    /**
     * 加载列表，有ui操作
     * @param path
     */
    @Override
    public void loadList(final File path,boolean isToTop) {
        fileListItems.clear();
        files =path.listFiles();
        Arrays.sort(files,new FileNameSort());

        if(!curPath.getAbsolutePath().equals("/"))
        {
            //添加返回上级目录项
            fileListItems.add(new FileListItem(application.folderBmp
                    , ".."
                    ,""
                    ,""));
        }
        for(int i = 0; i< files.length; i++)
        {
            String tempName=(files[i].getName()).toLowerCase();
            if(files[i].isDirectory()) {
                fileListItems.add(new FileListItem(application.folderBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,""));
            }
            else if(tempName.matches(".*.jpg|.*.png|.*.bmp|.*.gif$")) {
                fileListItems.add(new FileListItem(application.imageFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            } else if(tempName.matches(".*.mp4|.*.3gp|.*.flv|.*.wmv|.*.rmvb|.*.avi$")) {
                fileListItems.add(new FileListItem(application.videoFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            }else if(tempName.matches(".*.mp3|.*.amr|.*.ape|.*.flac|.*.ogg$")) {
                fileListItems.add(new FileListItem(application.audioFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            }else if(tempName.matches(".*.zip|.*.rar|.*.7z$")) {
                fileListItems.add(new FileListItem(application.achieveFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
                //--------------------------------------------------------
            }else if(tempName.matches(".*.xls|.*.xlsx$")) {
                fileListItems.add(new FileListItem(application.excelFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            }else if(tempName.matches(".*.doc|.*.docx$")) {
                fileListItems.add(new FileListItem(application.wordFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            }else if(tempName.matches(".*.ppt|.*.pptx$")) {
                fileListItems.add(new FileListItem(application.pptFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            }else if(tempName.matches(".*.apk$")) {
                Drawable icon = null;
                PackageInfo info = pm.getPackageArchiveInfo(files[i].getAbsolutePath(),
                        PackageManager.GET_ACTIVITIES);
                if (info != null) {
                    ApplicationInfo appInfo = info.applicationInfo;
                    appInfo.sourceDir = files[i].getAbsolutePath();
                    appInfo.publicSourceDir = files[i].getAbsolutePath();
                    try {
                        icon= appInfo.loadIcon(pm);
                    } catch (OutOfMemoryError e) {
                        Log.e("ApkIconLoader", e.toString());
                    }
                }
                Bitmap apkIcon=null;

                if(icon==null)
                {
                    apkIcon=application.apkFileBmp;
                }else {
                    apkIcon = ((BitmapDrawable) icon).getBitmap();
                }
                fileListItems.add(new FileListItem(apkIcon
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            }
            else
            {
                fileListItems.add(new FileListItem(application.commonFileBmp
                        , files[i].getName()
                        , getFormatedDate(files[i].lastModified())
                        ,formetFileSize(files[i].length())));
            }
        }

        rootAct.getSupportActionBar().setTitle(getShortPath(curPath.toString()));
        if(isToTop)
        {
            recyclerView.scrollToPosition(0);//跳到第一项显示
        }
    }

    /**
     * 获取缩略路径
     *
     * @param path
     * @return
     */
    public static String getShortPath (String path)
    {
        int n=0,p=0;
        for (int i=0;i < path.length();i++)
            if (path.charAt(i) == '/')
                ++n;

        if (n >= 3)
        {
            for (int i=0;i < path.length();i++)
            {
                if (path.charAt(i) == '/')
                    ++p;
                if (p == n - 1)
                {
                    String newPath = "..." + path.substring(i, path.length());
                    return newPath;
                }
            }
        }

        return path;
    }

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
     * 格式化文件大小
     * @param fileS
     * @return
     */
    public static  String formetFileSize (long fileS)
    {
        //转换文件大小
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileS < 1024)
        {
            fileSizeString = df.format((double) fileS) + "b";
            if (fileS == 0)
                fileSizeString = fileS + ".0b";
        }
        else if (fileS < 1048576)
        {
            fileSizeString = df.format((double) fileS / 1024) + "Kb";
        }
        else if (fileS < 1073741824)
        {
            fileSizeString = df.format((double) fileS / 1048576) + "Mb";
        }
        else
        {
            fileSizeString = df.format((double) fileS / 1073741824) + "Gb";
        }
        return fileSizeString;
    }
}
