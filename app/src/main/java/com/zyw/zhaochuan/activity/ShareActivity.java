package com.zyw.zhaochuan.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.zyw.zhaochuan.R;
import com.zyw.zhaochuan.fragment.ShareFileListFragment;

/**
 * Created by zyw on 2016/7/20.
 */
public class ShareActivity extends AppCompatActivity {
    public static ShareActivity thiz;
    private  FragmentTransaction transaction;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        thiz=this;
        transaction=getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frag_container,new ShareFileListFragment());
        transaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        //按下左上角的返回键
        if(item.getItemId()==android.R.id.home)
        {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
