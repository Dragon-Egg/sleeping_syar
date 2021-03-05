package org.dragonegg.ofuton.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import org.dragonegg.ofuton.R;
import org.dragonegg.ofuton.util.PrefUtil;
import org.dragonegg.ofuton.util.ReloadChecker;
import org.dragonegg.ofuton.util.TweetButtonPosition;

public class SettingActivity extends FinishableActionbarActivity {

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

    public static class PrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            setButtonPosition();
            setFontSize();
            setIconSize();
            setDateDisplayMode();
            setInlinePreview();
            setImageSize();
            displayLicenseInfo();
            displayVersionInfo();
            requestSoftReloadOnClick(R.string.show_image_in_timeline, R.string.show_source, R.string.date_display_mode);
            requestHardReloadOnChange(R.string.enable_fast_scroll, PrefUtil.getBoolean(R.string.enable_fast_scroll));
            requestHardReloadOnChange(R.string.remember_star, PrefUtil.getBoolean(R.string.remember_star));
            Preference tweetDetailPreference = findPreference(getString(R.string.tweet_detail_setting));
            tweetDetailPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(), SettingTweetDetailActivity.class));
                    return true;
                }
            });
            Preference tweetDetailOrderPreference = findPreference(getString(R.string.tweet_detail_setting_order));
            tweetDetailOrderPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(), SettingTweetDetailOrderActivity.class));
                    return true;
                }
            });
        }

        private void setButtonPosition() {
            ListPreference positionPref;
            final String[] displayStrings = getResources().getStringArray(R.array.tweet_button_position_selectable);
            positionPref = (ListPreference) findPreference(getString(R.string.tweet_button_position));
            TweetButtonPosition buttonPosition = TweetButtonPosition.strToEnum(positionPref.getValue());
            if(buttonPosition == TweetButtonPosition.Unknown){
                buttonPosition = TweetButtonPosition.Right;
            }
            positionPref.setSummary(displayStrings[buttonPosition.toInt()]);
            positionPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(displayStrings[TweetButtonPosition.strToEnum((String)newValue).toInt()]);
                    ReloadChecker.requestHardReload(true);
                    return true;
                }
            });
        }

        private void setInlinePreview() {
            CheckBoxPreference inlinePref = (CheckBoxPreference) findPreference(getString(R.string.show_image_in_timeline));
            inlinePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    ReloadChecker.requestSoftReload(true);
                    return true;
                }
            });
        }

        private void setFontSize() {
            ListPreference fontPref;
            fontPref = (ListPreference) findPreference(getString(R.string.font_size));
            fontPref.setSummary(fontPref.getValue());
            fontPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary((CharSequence) newValue);
                    ReloadChecker.requestSoftReload(true);
                    return true;
                }
            });
        }

        private void setIconSize() {
            ListPreference iconPref;
            iconPref = (ListPreference) findPreference(getString(R.string.icon_size));
            iconPref.setSummary(iconPref.getValue());
            iconPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary((CharSequence) newValue);
                    ReloadChecker.requestSoftReload(true);
                    return true;
                }
            });
        }

        private void setDateDisplayMode() {
            ListPreference datePref;
            datePref = (ListPreference) findPreference(getString(R.string.date_display_mode));
            datePref.setSummary(datePref.getValue());
            datePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary((CharSequence) newValue);
                    return true;
                }
            });
        }

        private void setImageSize() {
            ListPreference imagePref;
            ListPreference wifiImagePref;
            imagePref = (ListPreference) findPreference(getString(R.string.image_size));
            wifiImagePref = (ListPreference) findPreference(getString(R.string.image_size_is_wifi));
            imagePref.setSummary(imagePref.getValue());
            wifiImagePref.setSummary(wifiImagePref.getValue());
            imagePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary((CharSequence) newValue);
                    return true;
                }
            });
            wifiImagePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary((CharSequence) newValue);
                    return true;
                }
            });
        }

        /**
         * バージョン情報を表示する
         */
        private void displayVersionInfo() {
            String versionName = null;
            PackageManager packageManager = getActivity().getPackageManager();
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(getActivity().getPackageName(),
                        PackageManager.GET_ACTIVITIES);
                versionName = packageInfo.versionName;
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            PreferenceScreen versionInfo;
            versionInfo = (PreferenceScreen) findPreference(getString(R.string.version_info));
            versionInfo.setSummary(versionName != null ? versionName : "取得に失敗しました");
        }

        /**
         * PreferenceScreenにIntentを仕込めなくもないが，パッケージ名を返るといちいち面倒なのでJavaで飛ばす
         */
        private void displayLicenseInfo() {
            PreferenceScreen license = (PreferenceScreen) findPreference(getString(R.string.license_info));
            license.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(), LicenseActivity.class));
                    return true;
                }
            });
        }

        private void requestSoftReloadOnClick(int... ids) {
            for (int id : ids) {
                findPreference(getString(id)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        ReloadChecker.requestSoftReload(true);
                        return true;
                    }
                });
            }
        }

        private void requestHardReloadOnChange(int id, final Object before){
            findPreference(getString(id)).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    ReloadChecker.requestHardReload(newValue != before);
                    return true;
                }
            });
        }
    }
}