package com.crakac.ofuton.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.astuetz.PagerSlidingTabStrip;
import com.crakac.ofuton.C;
import com.crakac.ofuton.R;
import com.crakac.ofuton.util.RelativeTimeUpdater;
import com.crakac.ofuton.fragment.search.LocalSearchFragment;
import com.crakac.ofuton.fragment.adapter.SearchFragmentPagerAdapter;
import com.crakac.ofuton.fragment.search.TweetSearchFragment;
import com.crakac.ofuton.fragment.search.UserSearchFragment;
import com.crakac.ofuton.util.AppUtil;

import twitter4j.Query;

/**
 * Created by kosukeshirakashi on 2014/09/05.
 */
public class SearchActivity extends ActionBarActivity {

    private PagerSlidingTabStrip mTab;
    private ViewPager mPager;
    private SearchFragmentPagerAdapter mAdapter;
    private SearchView mSearchView;
    private String mQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_tab);
        mQuery = getIntent().getStringExtra(C.QUERY);
        mTab = (PagerSlidingTabStrip) findViewById(R.id.tab);
        mTab.setTabPaddingLeftRight(AppUtil.dpToPx(8));
        mPager = (ViewPager) findViewById(R.id.pager);
        if(mAdapter == null){
            mAdapter = new SearchFragmentPagerAdapter(getSupportFragmentManager());
        }
        if(mAdapter.isEmpty()){
            setFragments();
        }
        mPager.setAdapter(mAdapter);
        mPager.setOffscreenPageLimit(mAdapter.getCount());
        mTab.setViewPager(mPager);
        mTab.setOnPageChangeListener(new RelativeTimeUpdater(mAdapter));
    }

    private void setArgs(Fragment f, String query) {
        Bundle b = new Bundle(1);
        b.putString(C.QUERY, query);
        f.setArguments(b);
    }

    private void setArgs(Fragment f, String query, Query.ResultType resultType) {
        Bundle b = new Bundle(2);
        b.putString(C.QUERY, query);
        b.putSerializable(C.TYPE, resultType);
        f.setArguments(b);
    }

    private void setFragments(){
        TweetSearchFragment local = new LocalSearchFragment();
        TweetSearchFragment tweet = new TweetSearchFragment();
        TweetSearchFragment pics = new TweetSearchFragment();
        UserSearchFragment user = new UserSearchFragment();
        setArgs(local, mQuery);
        setArgs(tweet, buildQuery(mQuery));
        setArgs(pics, buildQuery(mQuery + " pic.twitter.com"));
        setArgs(user, buildQuery(mQuery));
        mAdapter.add(local);
        mAdapter.add(tweet);
        mAdapter.add(user);
        mAdapter.add(pics);
    }

    private String buildQuery(String query){
        return query + " exclude:retweets";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search, menu);
        MenuItem search = menu.findItem(R.id.search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(search);
        mSearchView.setOnQueryTextListener(mOnQueryTextListener);
        mSearchView.setIconified(false);
        mSearchView.clearFocus();
        mSearchView.setQuery(mQuery, false);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mSearchView != null && !mSearchView.isIconified()) {
            AppUtil.closeSearchView(mSearchView);
        } else {
            super.onBackPressed();
        }
    }

    private final SearchView.OnQueryTextListener mOnQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextChange(String newText) {
            return true;
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            search(query);
            mSearchView.clearFocus();// clear focus and hide software keyboard
            return true;
        }

    };

    private void search(String query) {
        for(Fragment f : mAdapter.getFragments()){
            if(f instanceof Searchable){
                ((Searchable) f).search(query);
            }
        }
    }

    public static interface Searchable{
        void search(String query);
    }
}