package com.crakac.ofuton.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.astuetz.PagerSlidingTabStrip;
import com.crakac.ofuton.C;
import com.crakac.ofuton.R;
import com.crakac.ofuton.util.RelativeTimeUpdater;
import com.crakac.ofuton.fragment.adapter.UserFragmentPagerAdapter;
import com.crakac.ofuton.fragment.timeline.FavoriteTimelineFragment;
import com.crakac.ofuton.fragment.dialog.ListSelectDialogFragment;
import com.crakac.ofuton.adapter.TwitterListAdapter;
import com.crakac.ofuton.fragment.timeline.UserTimelineFragment;
import com.crakac.ofuton.fragment.FollowersOfUserFragment;
import com.crakac.ofuton.fragment.FriendsOfUserFragment;
import com.crakac.ofuton.util.AppUtil;
import com.crakac.ofuton.util.NetUtil;
import com.crakac.ofuton.util.ParallelTask;
import com.crakac.ofuton.util.ProgressDialogFragment;
import com.crakac.ofuton.util.TwitterList;
import com.crakac.ofuton.util.TwitterUtils;
import com.crakac.ofuton.widget.ColorOverlayOnTouch;

import java.util.List;

import twitter4j.Relationship;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserList;

public class UserDetailActivity extends FinishableActionbarActivity {
    enum Relation {
        NotLoaded, Mutal, Following, Followed, Blocking, Unrelated, Myself,
    }

    protected static final String TAG = UserDetailActivity.class.getSimpleName();
    private ViewPager mPager;
    private View mProfileContentView, mProfileView;
    private PagerSlidingTabStrip mTab;
    private UserFragmentPagerAdapter mPagerAdapter;
    private ImageView mCollapseMark;
    private ParallelTask<Long, Void, Relationship> mloadRelationTask;
    private ParallelTask<String, Void, User> mLoadUserTask;
    private static ProgressDialogFragment mDialog;
    private Twitter mTwitter;
    private User mTargetUser;
    private Relation mRelation;
    private Relationship mT4jRelation;
    private RelationTask mChangeRelationTask;
    private ListSelectDialogFragment mListSelectDgFragment;
    private TwitterListAdapter mTwitterListAdapter;
    private ParallelTask<Void, Void, List<UserList>> mLoadListTask;
    private TextView mBioText, mLocationText, mUrlText, mRelationText;
    private NetworkImageView mIconImage;
    private ImageView mlockMark;
    private ProgressBar mloadingSpinner;
    private int mShortAnimeDuration;
    private ActionBar mActionbar;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("relation", mT4jRelation);
        outState.putSerializable(C.USER, mTargetUser);
        outState.putSerializable("rel", mRelation);
        Log.d(TAG, "onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // t4jRelationShip = (Relationship)
        // savedInstanceState.getSerializable("relation");
        // targetUser = (User) savedInstanceState.getSerializable(C.USER);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);
        mActionbar = getSupportActionBar();
        mActionbar.setDisplayShowHomeEnabled(false);

        mTargetUser = (User) getIntent().getSerializableExtra(C.USER);
        mRelation = Relation.NotLoaded;
        if (savedInstanceState != null) {
            mT4jRelation = (Relationship) savedInstanceState.getSerializable("relation");
            mTargetUser = (User) savedInstanceState.getSerializable(C.USER);
            mRelation = (Relation) savedInstanceState.getSerializable("rel");
        }
        mTwitter = TwitterUtils.getTwitterInstance();

        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new UserFragmentPagerAdapter(this, mPager);
        mTab = (PagerSlidingTabStrip) findViewById(R.id.tab);

        findAndInitViews();

        if (mTargetUser == null) {
            loadUserAndSetRelationship(getIntent().getStringExtra(C.SCREEN_NAME));
        } else {
            setContent(mTargetUser);
            loadRelationship(mTargetUser);
        }
    }

    private void loadUserAndSetRelationship(String screenName) {
        if (mLoadUserTask != null && mLoadUserTask.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }
        mLoadUserTask = new ParallelTask<String, Void, User>() {

            @Override
            protected User doInBackground(String... params) {
                try {
                    return mTwitter.showUser(params[0]);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(User result) {
                if (result == null) {
                    AppUtil.showToast(getString(R.string.something_wrong));
                    finish();
                    return;
                }
                mTargetUser = result;
                setContent(mTargetUser);
                loadRelationship(mTargetUser);
            }

        };
        mLoadUserTask.executeParallel(screenName);
    }

    /**
     * 終了時にはタスクを止める
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTask(mloadRelationTask);
        cancelTask(mLoadUserTask);
        cancelTask(mLoadListTask);
    }

    private void findAndInitViews() {
        mProfileView = (View) findViewById(R.id.profile);
        mProfileContentView = (View) findViewById(R.id.profile_contents);
        mProfileContentView.setVisibility(View.INVISIBLE);
        mRelationText = (TextView) findViewById(R.id.relationText);
        mIconImage = (NetworkImageView) findViewById(R.id.icon);
        mIconImage.setOnTouchListener(new ColorOverlayOnTouch());
        mlockMark = (ImageView) findViewById(R.id.lockedIcon);
        mBioText = (TextView) findViewById(R.id.bioText);
        mLocationText = (TextView) findViewById(R.id.locationText);
        mUrlText = (TextView) findViewById(R.id.urlText);

        mloadingSpinner = (ProgressBar) findViewById(R.id.loading_spinner);
        mloadingSpinner.setVisibility(View.VISIBLE);

        mRelationText.setText("読み込み中");
        mlockMark.setVisibility(View.GONE);
        mBioText.setText("");
        mLocationText.setText("");
        mUrlText.setText("");
        // 折りたたみボタン
        mCollapseMark = (ImageView) findViewById(R.id.collapse);
        mCollapseMark.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mProfileView.getVisibility() == View.VISIBLE) {
                    mProfileView.setVisibility(View.GONE);
                    mProfileContentView.setVisibility(View.GONE);
                    mCollapseMark.setImageResource(R.drawable.ic_expand_more_white_36dp);
                } else {
                    mProfileView.setVisibility(View.VISIBLE);
                    mProfileContentView.setVisibility(View.VISIBLE);
                    mCollapseMark.setImageResource(R.drawable.ic_expand_less_white_36dp);
                }
            }
        });
    }

    private void cancelTask(AsyncTask<?, ?, ?> task) {
        if (task != null && task.getStatus() == AsyncTask.Status.RUNNING) {
            task.cancel(true);
            task = null;
        }
    }

    private void setContent(final User user) {
        // ユーザーの情報
        mActionbar.setTitle(user.getName());
        mActionbar.setSubtitle("@" + user.getScreenName());
        mIconImage.setImageUrl(user.getOriginalProfileImageURLHttps(), NetUtil.PREVIEW_LOADER);
        mIconImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(UserDetailActivity.this, WebImagePreviewActivity.class);
                i.setData(Uri.parse(user.getOriginalProfileImageURL()));
                startActivity(i);
                overridePendingTransition(R.anim.fade_in, 0);
            }
        });
        if (user.isProtected()) {
            mlockMark.setVisibility(View.VISIBLE);
        } else {
            mlockMark.setVisibility(View.GONE);
        }

        URLEntity[] urls = user.getDescriptionURLEntities();
        String description = user.getDescription();
        for (URLEntity url : urls) {
            description = description.replace(url.getURL(),
                    "<a href=\"" + url.getExpandedURL() + "\">" + url.getDisplayURL() + "</a>");
        }
        description = description.replace("\n", "<br/>");
        mBioText.setText(Html.fromHtml(description));
        // URLをタップしてリンク先を開けるようにする
        MovementMethod mMethod = LinkMovementMethod.getInstance();
        mBioText.setMovementMethod(mMethod);

        mLocationText.setText(user.getLocation());
        mUrlText.setText(user.getURLEntity().getExpandedURL());

        crossFade();

        int statusCounts = user.getStatusesCount();
        int friends = user.getFriendsCount();
        int followers = user.getFollowersCount();
        int favs = user.getFavouritesCount();
        mPagerAdapter.setCounts(statusCounts, friends, followers, favs);
        if(mPagerAdapter.isEmpty()) {
            Bundle args = createArgs();
            mPagerAdapter.add(UserTimelineFragment.class, args, 0);
            mPagerAdapter.add(FriendsOfUserFragment.class, args, 1);
            mPagerAdapter.add(FollowersOfUserFragment.class, args, 2);
            mPagerAdapter.add(FavoriteTimelineFragment.class, args, 3);
            mPagerAdapter.notifyDataSetChanged();
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            mPager.setOffscreenPageLimit(mPagerAdapter.getCount());// 全Fragmentを保持（onCreateViewが複数呼ばれるのを抑止）
        }
        mTab.setOnPageChangeListener(new RelativeTimeUpdater(mPagerAdapter));
        mTab.setViewPager(mPager);
    }

    private Bundle createArgs() {
        Bundle args = new Bundle();
        args.putSerializable(C.USER, mTargetUser);
        args.putSerializable(C.USER_ID, mTargetUser.getId());
        return args;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private void crossFade() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            mProfileContentView.setAlpha(0f);
            mProfileContentView.setVisibility(View.VISIBLE);
            mShortAnimeDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
            mProfileContentView.animate().alpha(1f).setDuration(mShortAnimeDuration).setListener(null);
            mloadingSpinner.animate().alpha(0f).setDuration(mShortAnimeDuration)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mloadingSpinner.setVisibility(View.GONE);
                        }
                    });
        } else {
            mProfileContentView.setVisibility(View.VISIBLE);
            mloadingSpinner.setVisibility(View.GONE);
        }
    }

    private void loadRelationship(User user) {
        // すでに一度リレーションシップを読み込んでいる場合
        if (mT4jRelation != null) {
            setRelationship(mT4jRelation);
            return;
        }
        // 現在読み込み中の場合
        if (mloadRelationTask != null && mloadRelationTask.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }
        mloadRelationTask = new ParallelTask<Long, Void, Relationship>() {
            @Override
            protected Relationship doInBackground(Long... params) {
                try {
                    return mTwitter.showFriendship(params[0], params[1]);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Relationship result) {
                if (result != null) {
                    setRelationship(result);
                } else {
                    AppUtil.showToast("何かがおかしいよ");
                    Log.d(TAG, "getRelaitionship(), something wrong");
                }
            }
        };
        long currentUserId = TwitterUtils.getCurrentAccountId();
        mloadRelationTask.executeParallel(currentUserId, user.getId());
    }

    private void setRelationship(Relationship relationShip) {
        if (relationShip.isSourceBlockingTarget()) {
            mRelation = Relation.Blocking;
        } else if (relationShip.isSourceFollowedByTarget() && relationShip.isSourceFollowingTarget()) {
            mRelation = Relation.Mutal;
        } else if (relationShip.isSourceFollowingTarget()) {
            mRelation = Relation.Following;
        } else if (relationShip.isSourceFollowedByTarget()) {
            mRelation = Relation.Followed;
        } else {
            mRelation = Relation.Unrelated;
        }
        if (mTargetUser.getId() == TwitterUtils.getCurrentAccountId()) {
            mRelation = Relation.Myself;
        }
        setRelationText(mRelation);
    }

    private void setRelationText(Relation r) {
        switch (r) {
        case Blocking:
            mRelationText.setText("ブロック中");
            break;
        case Mutal:
            mRelationText.setText("相互フォロー");
            break;
        case Followed:
            mRelationText.setText("ファン");
            break;
        case Following:
            mRelationText.setText("片思い");
            break;
        case Unrelated:
            mRelationText.setText("無関心");
            break;
        case Myself:
            mRelationText.setText("あなたです！");
            break;
        default:
            mRelationText.setText("何かおかしいよ");
            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem changeRelationMenu = menu.findItem(R.id.menu_relation);
        switch (mRelation) {
        case Myself:
            changeRelationMenu.setVisible(false);
        case NotLoaded:
            changeRelationMenu.setEnabled(false);
            changeRelationMenu.setTitle(R.string.now_loading);
            break;
        case Blocking:
            changeRelationMenu.setEnabled(false);
            changeRelationMenu.setTitle("ブロック中");
            break;
        case Following:
        case Mutal:
            changeRelationMenu.setEnabled(true);
            changeRelationMenu.setTitle(R.string.remove);
            break;
        case Followed:
        case Unrelated:
            changeRelationMenu.setEnabled(true);
            changeRelationMenu.setTitle(R.string.follow);
            break;
        default:
            break;
        }

        MenuItem blockMenu = menu.findItem(R.id.menu_block);
        if (mRelation == Relation.Blocking) {
            blockMenu.setTitle(R.string.unblock);
        } else {
            blockMenu.setTitle(R.string.block);
        }

        MenuItem dmMenu = menu.findItem(R.id.menu_dm);
        switch (mRelation) {
        case Followed:
        case Mutal:
        case Myself:
            dmMenu.setEnabled(true);
            break;
        default:
            dmMenu.setEnabled(false);
            break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
        case R.id.menu_list:
            addToList();
            break;
        case R.id.menu_relation:
            changeRelation();
            break;
        case R.id.menu_block:
            blockUser();
            break;
        case R.id.menu_r4s:
            reportUser();
            break;
        case R.id.menu_mention:
            i = new Intent(this, TweetActivity.class);
            i.putExtra(C.SCREEN_NAME, mTargetUser.getScreenName());
            i.putExtra(C.USER, mTargetUser);
            startActivity(i);
            break;
        case R.id.menu_dm:
            i = new Intent(this, ComposeDmActivity.class);
            i.putExtra(C.USER, mTargetUser);
            startActivity(i);
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void reportUser() {
        RelationTaskListener listner = new RelationTaskListener() {
            @Override
            public void end() {
                AppUtil.showToast("スパムとして報告しました");
            }

            @Override
            public User action() throws TwitterException {
                return mTwitter.reportSpam(mTargetUser.getId());
            }
        };
        mChangeRelationTask = new RelationTask(listner);
        mChangeRelationTask.executeParallel();
    }

    private void blockUser() {
        RelationTaskListener listner = new RelationTaskListener() {
            @Override
            public void end() {
                if (mRelation == Relation.Blocking) {
                    AppUtil.showToast("ブロックを解除しました");
                } else {
                    AppUtil.showToast("ブロックしました");
                }
            }

            @Override
            public User action() throws TwitterException {
                if (mRelation == Relation.Blocking) {
                    return mTwitter.destroyBlock(mTargetUser.getId());
                } else {
                    return mTwitter.createBlock(mTargetUser.getId());
                }
            }
        };
        mChangeRelationTask = new RelationTask(listner);
        mChangeRelationTask.executeParallel();
    }

    private void changeRelation() {
        RelationTaskListener listner = new RelationTaskListener() {
            @Override
            public void end() {
                if (mRelation == Relation.Following || mRelation == Relation.Mutal) {
                    AppUtil.showToast("リムーブしました");
                } else {
                    AppUtil.showToast("フォローしました");
                }
            }

            @Override
            public User action() throws TwitterException {
                if (mRelation == Relation.Following || mRelation == Relation.Mutal) {
                    return mTwitter.destroyFriendship(mTargetUser.getId());
                } else if (mRelation == Relation.Followed || mRelation == Relation.Unrelated) {
                    return mTwitter.createFriendship(mTargetUser.getId());
                } else {
                    return null;
                }
            }
        };
        mChangeRelationTask = new RelationTask(listner);
        mChangeRelationTask.executeParallel();
    }

    private interface RelationTaskListener {
        User action() throws TwitterException;

        void end();
    }

    private class RelationTask extends ParallelTask<Void, Void, User> {
        RelationTaskListener listener;

        public RelationTask(RelationTaskListener listener) {
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            mDialog = ProgressDialogFragment.newInstance(getString(R.string.now_executing));
            mDialog.show(getSupportFragmentManager(), "reporting");
        }

        @Override
        protected User doInBackground(Void... params) {
            try {
                return listener.action();
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(User result) {
            mDialog.getDialog().dismiss();
            if (result != null) {
                listener.end();
                loadRelationship(result);
            } else {
                AppUtil.showToast(getString(R.string.something_wrong));
            }
        }
    }

    /**
     * ユーザーをリストに加えたるやつ．
     */
    private void addToList() {
        mListSelectDgFragment = new ListSelectDialogFragment();
        mTwitterListAdapter = new TwitterListAdapter(UserDetailActivity.this, mTargetUser.getId());
        mListSelectDgFragment.setAdapter(mTwitterListAdapter);
        Bundle b = new Bundle();
        b.putLong("userId", mTargetUser.getId());
        mListSelectDgFragment.setArguments(b);
        loadList();
    }

    /**
     * リストを読み込み．キャッシュとかしないから毎回読み込む．15回/15min.(API1.1)
     */
    void loadList() {
        mLoadListTask = new ParallelTask<Void, Void, List<UserList>>() {
            TwitterException te = null;
            ProgressDialogFragment pgDialog;

            @Override
            protected void onPreExecute() {
                pgDialog = ProgressDialogFragment.newInstance("リストを読み込んでいます…");
                pgDialog.show(getSupportFragmentManager(), "loading list");
            }

            @Override
            protected List<UserList> doInBackground(Void... params) {
                try {
                    return mTwitter.getUserLists(TwitterUtils.getCurrentAccountId());
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (TwitterException e) {
                    te = e;
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(List<UserList> lists) {
                pgDialog.getDialog().dismiss();
                if (te != null) {
                    if (te.getStatusCode() == TwitterException.TOO_MANY_REQUESTS) {
                        AppUtil.showToast("リストを取得できませんでした．\nしばらくしてからもう一度試してみてください．");
                    }
                }
                if (lists != null) {
                    long userId = TwitterUtils.getCurrentAccountId();// リスト選ぶんだから現在のユーザでおｋ
                    for (UserList list : lists) {
                        TwitterList tList = new TwitterList(userId, list.getId(), list.getName(), list.getFullName());
                        mTwitterListAdapter.add(tList);
                    }
                    mListSelectDgFragment.show(getSupportFragmentManager(), "ListDialog");
                }
            }
        };
        mLoadListTask.executeParallel();
    }
}