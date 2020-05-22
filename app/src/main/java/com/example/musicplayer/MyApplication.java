package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

/**
 * 获取全局context，因为有些工具类中不好取得context
 */

public class MyApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContext(){
        return context;
    }
}
