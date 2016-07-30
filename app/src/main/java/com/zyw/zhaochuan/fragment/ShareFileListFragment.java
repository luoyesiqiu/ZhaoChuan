package com.zyw.zhaochuan.fragment;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.zyw.zhaochuan.R;
import com.zyw.zhaochuan.ThisApplication;
import com.zyw.zhaochuan.activity.SessionActivity;
import com.zyw.zhaochuan.activity.ShareActivity;
import com.zyw.zhaochuan.adapter.FileListAdapter;
import com.zyw.zhaochuan.entity.FileListItem;
import com.zyw.zhaochuan.entity.FileSessionList;
import com.zyw.zhaochuan.interfaces.FileListInterface;
import com.zyw.zhaochuan.interfaces.OnTransProgressChangeListener;
import com.zyw.zhaochuan.parser.FileListParser;
import com.zyw.zhaochuan.parser.ShareQRBodyParser;
import com.zyw.zhaochuan.util.FileNameSort;
import com.zyw.zhaochuan.util.QRMaker;
import com.zyw.zhaochuan.util.Utils;
import com.zyw.zhaochuan.view.RecycleViewDivider;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;
import cn.bmob.v3.listener.UploadFileListener;

/**
 * Created by zyw on 2016/7/23.
 */
public class ShareFileListFragment extends Fragment implements FileListInterface,OnTransProgressChangeListener{
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
            recyclerView.setFocusable(true);//这个和下面的这个命令必须要设置了，才能监听back事件。
            recyclerView.setFocusableInTouchMode(true);
            recyclerView.setOnKeyListener(backlistener);
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
        }
        showToast("选择一个文件来上传");
        rootAct.getSupportActionBar().show();//显示ActionBar
        rootAct.getSupportActionBar().setTitle(getShortPath(curPath.toString()));
        return rootView;
    }

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

            //点击文件时，上传
            else if (files[pos-1].isFile())
            {
                if(isCanDownload) {
                    isCanDownload=false;
                    final File tempFile = files[pos - 1];
                    final BmobFile f = new BmobFile(tempFile);
                    if (uploadProgressDialog == null)
                        uploadProgressDialog = new ProgressDialog(rootAct);

                    uploadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    uploadProgressDialog.setMessage(String.format("文件“%s”上传中...", tempFile.getName()));
                    uploadProgressDialog.setCancelable(false);
                    uploadProgressDialog.setProgress(0);
                    uploadProgressDialog.setMax(100);
                    uploadProgressDialog.show();
                    f.uploadblock(new UploadFileListener() {
                        @Override
                        public void done(BmobException e) {
                            isCanDownload=true;
                            if(e==null) {
                                showToast(String.format("文件“%s”上传完成", tempFile.getName()));
                                LayoutInflater layoutInflater=LayoutInflater.from(rootAct);
                              View view=  layoutInflater.inflate(R.layout.show_qr_layout,null);
                                TextView keyTextView=(TextView)view.findViewById(R.id.show_ip_tv);
                                TextView displayTextView=(TextView)view.findViewById(R.id.show_display_text) ;
                                ImageView qrImageView=(ImageView)view.findViewById(R.id.show_qr_iv);
                                String key=Utils.getFileKey(4);
                                keyTextView.setText(key);
                                displayTextView.setText("扫描二维码或者输入Key获取文件");
                                ShareQRBodyParser shareQRBodyParser= null;
                                try {
                                    shareQRBodyParser = new ShareQRBodyParser(f.getUrl(),key);
                                } catch (JSONException e1) {
                                    e1.printStackTrace();
                                }
                                qrImageView.setImageBitmap(QRMaker.createQRImage(shareQRBodyParser.toString()));
                               final String objId= addFileInfoToServer(f,key);
                                AlertDialog alertDialog=new AlertDialog.Builder(rootAct)
                                        .setView(view)
                                        .setPositiveButton("取消分享", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                f.delete();
                                                FileSessionList sessionList=new FileSessionList();
                                                sessionList.setObjectId(objId);
                                                sessionList.delete(new UpdateListener() {
                                                    @Override
                                                    public void done(BmobException e) {
                                                        if(e==null)
                                                            showToast("取消分享成功");
                                                    }
                                                });
                                            }
                                        })
                                        .create();
                                alertDialog.show();
                            }
                            else
                            {
                                showToast("文件上传失败");
                                uploadProgressDialog.hide();
                            }
                        }

                        @Override
                        public void onProgress(Integer value) {

                            uploadProgressDialog.setProgress(value);

                            if (value >= 100)
                                uploadProgressDialog.hide();
                        }
                    });
                }
            }
        }

        /**
         * 添加数据到表
         * @param file
         * @param key
         */
        boolean isUpdateSuccess=false;
        private  String addFileInfoToServer(BmobFile file,String key)
        {

            final FileSessionList fileSessionList=new FileSessionList();
            fileSessionList.setFileKey(key);//KEY
            fileSessionList.setFileName(file.getFilename());//文件名
            fileSessionList.setFileSize(file.getLocalFile().length()+"");
            fileSessionList.setUrl(file.getUrl());
            fileSessionList.save(new SaveListener<String>() {
                @Override
                public void done(String s, BmobException e) {
                    if(e==null){
                        isUpdateSuccess=true;
                    }else
                    {
                        isUpdateSuccess=false;
                    }
                }
            });
            return isUpdateSuccess?fileSessionList.getObjectId():null;
        }

        /**
         * 列表长按事件
         * @param view
         * @param pos
         */
        @Override
        public void onItemLongClick(View view, final int pos) {

            File selectedPath=null;
            if (pos == 0 && !curPath.toString().equals("/"))
            {
                return;
            }
            else if ((pos == 0 && curPath.toString().equals("/")) || files[pos-1].isDirectory()|| !files[pos-1].isDirectory()) {
                if (curPath.toString().equals("/"))
                {
                    selectedPath =files[pos];
                }
                else
                {
                    selectedPath =files[pos-1];
                }
                final File finalSelectedPath = selectedPath;
                Dialog dialog = new AlertDialog.Builder(rootAct)
                        .setItems(R.array.menu_item, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        //复制本地的了，远程的路径清空
                                      //  RemoteListFragment.willSendFilePath=null;
                                        willSendFilePath= finalSelectedPath;
                                        Toast.makeText(rootAct,willSendFilePath.getAbsolutePath(),Toast.LENGTH_LONG).show();
                                        application.setCopyFromLocal(true);
                                        break;
                                    case 1:
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

                                    case 2:
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
        }

    }

    /**
     * 返回上一级目录，有ui操作
     */
    public void goBack() {
        try
        {
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

    private View.OnKeyListener backlistener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int code, KeyEvent keyEvent) {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                if (code == KeyEvent.KEYCODE_BACK) {  //表示按返回键 时的操作
                    if(!curPath.getAbsolutePath().equals("/"))
                    {
                        goBack();
                    }
                }
            }
            return false;
        }
    };
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
            String tempName=files[i].getName();
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
