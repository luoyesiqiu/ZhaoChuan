package com.woc.chuan.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.woc.chuan.R;

/**
 * Created by zyw on 2016/6/18.
 */
public class SettingFragment extends PreferenceFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        addPreferencesFromResource(R.xml.setting);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

}
