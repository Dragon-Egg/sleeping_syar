package org.dragonegg.ofuton.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.view.MenuItem;

import androidx.recyclerview.widget.RecyclerView;

import org.dragonegg.ofuton.R;

import java.util.ArrayList;

/**
 * Created by Kosuke on 2017/12/23.
 */

public class SettingTweetDetailActivity extends FinishableActionbarActivity {
    private Menu mMenu;
    private ArrayList<RecyclerView> editViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        FragmentManager fm = getFragmentManager();
        Fragment prefsFragment = fm.findFragmentByTag("prefs");
        if (prefsFragment == null) {
            prefsFragment = new PrefsFragment();
            fm.beginTransaction().replace(R.id.content, prefsFragment, "prefs").commit();
        }
    }

    public static class PrefsFragment extends PreferenceFragment{
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.tweet_detail);
        }
    }

}
