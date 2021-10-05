package com.nju.map;

import android.app.Application;
import android.widget.Toast;

import com.uuzuche.lib_zxing.activity.ZXingLibrary;

/**
 * Created by aaron on 16/9/7.
 */

public class MApplication extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        ZXingLibrary.initDisplayOpinion(this);
    }
}
