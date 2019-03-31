package org.dragonegg.ofuton;


import android.app.Application;

import org.dragonegg.ofuton.util.AppUtil;
import org.dragonegg.ofuton.util.PrefUtil;
import org.dragonegg.ofuton.util.TwitterUtils;


public class OfutonApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        TwitterUtils.init(this);
        AppUtil.init(this);
        PrefUtil.init(this);
        AppUtil.checkTofuBuster();
    }
}