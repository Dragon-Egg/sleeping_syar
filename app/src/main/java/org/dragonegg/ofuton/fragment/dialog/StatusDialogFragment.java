package org.dragonegg.ofuton.fragment.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.dragonegg.ofuton.C;
import org.dragonegg.ofuton.R;
import org.dragonegg.ofuton.action.ClickActionAdapter;
import org.dragonegg.ofuton.action.status.CancelRetweetAction;
import org.dragonegg.ofuton.action.status.ClickAction;
import org.dragonegg.ofuton.action.status.CopyLinkAction;
import org.dragonegg.ofuton.action.status.DestroyStatusAction;
import org.dragonegg.ofuton.action.status.FavAction;
import org.dragonegg.ofuton.action.status.FavAndRetweeAction;
import org.dragonegg.ofuton.action.status.HashtagAction;
import org.dragonegg.ofuton.action.status.HashtagSearchAction;
import org.dragonegg.ofuton.action.status.LinkAction;
import org.dragonegg.ofuton.action.status.MediaAction;
import org.dragonegg.ofuton.action.status.OpenTwitterAction;
import org.dragonegg.ofuton.action.status.ReplyAction;
import org.dragonegg.ofuton.action.status.ReplyAllAction;
import org.dragonegg.ofuton.action.status.RetweetAction;
import org.dragonegg.ofuton.action.status.ShareAction;
import org.dragonegg.ofuton.action.status.StatusAction;
import org.dragonegg.ofuton.action.status.UserDetailAction;
import org.dragonegg.ofuton.adapter.TweetStatusAdapter;
import org.dragonegg.ofuton.util.AppUtil;
import org.dragonegg.ofuton.util.PrefUtil;
import org.dragonegg.ofuton.util.TwitterUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;

import static org.dragonegg.ofuton.util.TwitterUtils.isMyTweet;

/**
 * ツイートをタップした時に出てくるダイアログ
 *
 * @author Kosuke
 */
@SuppressLint("ValidFragment")
public class StatusDialogFragment extends DialogFragment {

    private ClickActionAdapter mActionAdapter;
    private Dialog mDialog;
    private Status mSelectedStatus;
    private boolean isEnablePreview;
    public static final LinkedHashMap<String, Boolean> initialDetails = new LinkedHashMap<String, Boolean>() {
        {
            put("show_destroy", true);
            put("show_reply", true);
            put("show_reply_all", true);
            put("show_conversation", true);
            put("show_accounts_in_tweet", true);
            put("show_favorite_key", true);
            put("show_retweet_key", true);
            put("show_fav_and_retweet", false);
            put("show_user_entities_order", false);
            put("show_url_entities_order", false);
            put("show_media_entities_order", false);
            put("show_hashtag_tweet", false);
            put("show_hashtag_search", true);
            put("show_link_copy", true);
            put("show_share", true);
            put("open_twitter_app", true);
        }
    };

    public StatusDialogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelectedStatus = (Status) getArguments().getSerializable(C.STATUS);
        isEnablePreview = PrefUtil.getBoolean(R.string.show_image_in_timeline, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_actions, container);
        // 各種アクションをアダプタに追加して表示
        mActionAdapter = new ClickActionAdapter(getActivity());

        // リストビューを作成
        // ツイートが縦に長いと全画面分の領域を使ってしまい，アクションを選択できなくなる
        ListView lvActions = view.findViewById(R.id.action_list);

        // ステータス表示部分を作成．タイムライン中と同じレイアウトなのでTweetStatusAdapter内の処理を使いまわす．
        View statusView = TweetStatusAdapter.createView(
                mSelectedStatus, null);
        statusView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();
            }
        });
        // HeaderView（ツイート表示用）をadd. setAdapterより先にしないと落ちる
        lvActions.addHeaderView(statusView);
        // アダプタをセット
        lvActions.setAdapter(mActionAdapter);
        setActions();

        lvActions.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                ListView lv = (ListView) parent;
                ClickAction item = (ClickAction) lv
                        .getItemAtPosition(position);
                if (mDialog != null) {
                    mDialog.dismiss();
                }
                item.doAction();
            }
        });
        return view;
    }

    private boolean isLockedAccountTweet(Status status) {
        return status.getUser().isProtected();
    }

    private boolean isRetweetable(Status status) {
        return (!isLockedAccountTweet(status) || status.isRetweet()) && !status.isRetweeted();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mDialog = getDialog();

        WindowManager.LayoutParams lp = mDialog.getWindow().getAttributes();

        DisplayMetrics metrics = getResources().getDisplayMetrics();

        // 縦幅はwrap contentで，横幅は85%で．
        int dialogWidth = (int) Math.min((metrics.widthPixels * 0.85), AppUtil.dpToPx(480));
        int dialogHeight = WindowManager.LayoutParams.WRAP_CONTENT;

        lp.width = dialogWidth;
        lp.height = dialogHeight;
        mDialog.getWindow().setAttributes(lp);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mDialog = new Dialog(getActivity());
        // タイトル部分を消す．消さないとダイアログの表示位置が下にずれる
        mDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        // レイアウトはonCreateViewで作られる．ので，dialog.setContentViewはいらない

        // 全画面化
        mDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 背景を透明に
        mDialog.getWindow().setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT));
        return mDialog;
    }

    private void setActions() {
        Status retweetedStatus = mSelectedStatus.getRetweetedStatus();
        checkDialogDetails();
        Gson gson = new Gson();
        String viewOrder = PrefUtil.getString(R.string.tweet_detail_setting_order, gson.toJson(new LinkedHashMap(initialDetails)));
        Type type = new TypeToken<LinkedHashMap<String, Boolean>>(){}.getType();
        LinkedHashMap<String, Boolean> savedHashMap = new LinkedHashMap<>(gson.fromJson(viewOrder, type));

        for (String key: savedHashMap.keySet()) {
            int stringResource = getResources().getIdentifier(key, "string", Objects.requireNonNull(this.getContext()).getPackageName());
            if (stringResource == R.string.show_destroy) {
                // 自分のツイートならdestroyアクションを追加
                if (PrefUtil.getBoolean(R.string.show_destroy, true) &&
                        ((isMyTweet(mSelectedStatus) && !mSelectedStatus.isRetweet()) || isMyTweet(retweetedStatus))) {
                    mActionAdapter.add(new DestroyStatusAction(getActivity(), mSelectedStatus));
                }
            }
            if (stringResource == R.string.show_reply) {
                // reply
                if (PrefUtil.getBoolean(R.string.show_reply, true)) {
                    mActionAdapter.add(new ReplyAction(getActivity(), mSelectedStatus));
                }
            }
            if (stringResource == R.string.show_reply_all) {
                // reply all
                if (PrefUtil.getBoolean(R.string.show_reply_all, true)) {
                    UserMentionEntity[] entities = mSelectedStatus.isRetweet() ? retweetedStatus.getUserMentionEntities() : mSelectedStatus.getUserMentionEntities();

                    if ((!isMyTweet(mSelectedStatus) && entities.length > 0)//自分のツイートでなく，誰かしらへリプライを飛ばしている
                            && (!(entities.length == 1 && (entities[0].getId() == TwitterUtils.getCurrentAccountId() //自分だけへのリプライではない
                            || entities[0].getId() == (mSelectedStatus.isRetweet() ? retweetedStatus.getUser().getId() : mSelectedStatus.getUser().getId())
                    )))) {
                        mActionAdapter.add(new ReplyAllAction(getActivity(), mSelectedStatus));
                    }
                }
            }
            if (stringResource == R.string.show_favorite_key) {
                // favorite
                if(PrefUtil.getBoolean(R.string.show_favorite_key, true)){
                    mActionAdapter.add(new FavAction(getActivity(), mSelectedStatus));
                }
            }
            if (stringResource == R.string.show_retweet_key) {
                //cancel Retweet
                // 自分がリツイートしたやつはリツイートを取り消せる
                if (PrefUtil.getBoolean(R.string.show_retweet_key, true)) {
                    if (mSelectedStatus.isRetweeted() ||
                            (mSelectedStatus.isRetweet() && retweetedStatus.isRetweeted())) {
                        mActionAdapter.add(new CancelRetweetAction(getActivity(), mSelectedStatus));
                    }
                    // retweet //鍵垢のツイートでない(鍵垢のRTは元のツイートをRT出来る)
                    else if (isRetweetable(mSelectedStatus)) {
                        mActionAdapter.add(new RetweetAction(getActivity(), mSelectedStatus));
                    }
                }
            }
            if (stringResource == R.string.show_fav_and_retweet) {
                // Fav & Retweet
                if (PrefUtil.getBoolean(R.string.show_fav_and_retweet, false) && isRetweetable(mSelectedStatus) && !mSelectedStatus.isFavorited()) {
                    mActionAdapter.add(new FavAndRetweeAction(getContext(), mSelectedStatus));
                }
            }
            if (stringResource == R.string.show_user_entities_order) {
                setUserEntities(mSelectedStatus);
            }
            if (stringResource == R.string.show_url_entities_order) {
                setUrlEntities(mSelectedStatus);
            }
            if (stringResource == R.string.show_media_entities_order) {
                // インラインプレビューOFFのときは、ダイアログにURLを表示する
                if (!isEnablePreview) {
                    setMediaEntities(mSelectedStatus);
                }
            }
            if (stringResource == R.string.show_hashtag_tweet) {
                if(PrefUtil.getBoolean(R.string.show_hashtag_tweet, false)){
                    setHashtagEntities(mSelectedStatus);
                }
            }
            if (stringResource == R.string.show_hashtag_search) {
                if(PrefUtil.getBoolean(R.string.show_hashtag_search, true)){
                    setHashtagSearchEntities(mSelectedStatus);
                }
            }
            if (stringResource == R.string.show_link_copy) {
                // URLコピー
                if (PrefUtil.getBoolean(R.string.show_link_copy, true)) {
                    mActionAdapter.add(new CopyLinkAction(getContext(), mSelectedStatus));
                }

            }
            if (stringResource == R.string.show_share) {
                // 共有ウィンドウを開く
                if (PrefUtil.getBoolean(R.string.show_share, true)) {
                    mActionAdapter.add(new ShareAction(getContext(), mSelectedStatus));
                }
            }
            if (stringResource == R.string.open_twitter_app) {
                // Twitterで開く
                if (PrefUtil.getBoolean(R.string.open_twitter_app, true)) {
                    mActionAdapter.add(new OpenTwitterAction(getContext(), mSelectedStatus));
                }
            }
        }
        mActionAdapter.notifyDataSetChanged();
    }

    private void setUserEntities(Status status) {
        List<String> users = new ArrayList<>();// statusに関係あるscreenNameをかたっぱしから突っ込む(@抜き)

        if(PrefUtil.getBoolean(R.string.show_accounts_in_tweet, true)) {
            UserMentionEntity[] userMentionEntities = status.getUserMentionEntities();
            for (UserMentionEntity user : userMentionEntities) {
                if (!users.contains(user.getScreenName())) {
                    users.add(user.getScreenName());
                }
            }

            // リツイートの場合，オリジナルの方でないと省略される可能性があるのでretweetedStatusからも引っ張ってくる
            if (status.isRetweet()) {
                Status rtStatus = status.getRetweetedStatus();
                UserMentionEntity[] umEntities = rtStatus.getUserMentionEntities();
                for (UserMentionEntity user : umEntities) {
                    if (!users.contains(user.getScreenName()))
                        users.add(user.getScreenName());
                }
            }
            if (users.contains(status.getUser().getScreenName())) {
                // リツイート内でリツイートしたユーザーのスクリーンネームが含まれていた場合呼ばれる
                users.remove(status.getUser().getScreenName());
            }
        } else if(status.isRetweet()){
            users.add(status.getRetweetedStatus().getUser().getScreenName());
        }
        users.add(status.getUser().getScreenName());// ツイートまたはリツイートした人は一番下に置きたい

        for (String user : users) {
            if (user.equals(status.getUser().getScreenName())) {
                mActionAdapter.add(new UserDetailAction(getActivity(), status));
            } else {
                mActionAdapter.add(new UserDetailAction(getActivity(), user));
            }
        }
    }

    private void setUrlEntities(Status status) {
        URLEntity[] urlEntities;
        ArrayList<String> urls = new ArrayList<>();
        if (status.isRetweet()) {
            urlEntities = status.getRetweetedStatus().getURLEntities();
        } else {
            urlEntities = status.getURLEntities();
        }
        for (URLEntity url : urlEntities) {
            if (isEnablePreview && url.getDisplayURL().startsWith("pic.twitter.com/")) {
                continue;
            }
            if (PrefUtil.getBoolean(R.string.open_twitter_status)) {
                Matcher matcher = Pattern.compile("twitter\\.com/(.+?|)(/|)status/(\\d+)(|/)").matcher(url.getExpandedURL());
                if (matcher.find()) {
                    mActionAdapter.add(new StatusAction(getActivity(), Long.valueOf(matcher.group(3)), url.getExpandedURL()));
                } else {
                    mActionAdapter.add(new LinkAction(getActivity(), url.getExpandedURL()));
                }
            } else {
                mActionAdapter.add(new LinkAction(getActivity(), url.getExpandedURL()));
            }
            urls.add(url.getExpandedURL());
            Log.d("URLEntity", url.getExpandedURL());
        }
    }

    private void setMediaEntities(Status status) {
        MediaEntity[] mediaEntities;
        if (status.isRetweet()) {
            mediaEntities = status.getRetweetedStatus().getMediaEntities();
        } else {
            mediaEntities = status.getMediaEntities();
        }

        for (MediaEntity media : mediaEntities) {
            mActionAdapter.add(new MediaAction(getActivity(), media
                    .getExpandedURL(), media
                    .getMediaURL()));
        }

    }

    private void setHashtagEntities(Status status) {
        HashtagEntity[] hashtags;
        if (status.isRetweet()) {
            hashtags = status.getRetweetedStatus().getHashtagEntities();
        } else {
            hashtags = status.getHashtagEntities();
        }
        for (HashtagEntity hashtag : hashtags) {
            mActionAdapter.add(new HashtagAction(getActivity(), hashtag
                    .getText()));
        }
    }

    private void setHashtagSearchEntities(Status status) {
        HashtagEntity[] hashtags;
        if (status.isRetweet()) {
            hashtags = status.getRetweetedStatus().getHashtagEntities();
        } else {
            hashtags = status.getHashtagEntities();
        }
        for (HashtagEntity hashtag : hashtags) {
            mActionAdapter.add(new HashtagSearchAction(getActivity(), hashtag
                    .getText()));
        }
    }

    @Override
    public int show(FragmentTransaction transaction, String tag) {
        if (!isResumed()) return 0;
        return super.show(transaction, tag);
    }

    public static void checkDialogDetails() {
        Gson gson = new Gson();
        LinkedHashMap<String, Boolean> details = initialDetails;
        String viewOrder = PrefUtil.getString(R.string.tweet_detail_setting_order, gson.toJson(new LinkedHashMap(details)));
        Type type = new TypeToken<LinkedHashMap<String, Boolean>>(){}.getType();
        LinkedHashMap<String, Boolean> savedHashMap = new LinkedHashMap<>(gson.fromJson(viewOrder, type));
        // 保存されていて、既に使われていない値を削除
        Iterator<Map.Entry<String, Boolean>> it = savedHashMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Boolean> entry = it.next();
            if (details.get(entry.getKey()) == null) {
                it.remove();
            }
        }

        // 保存されていない、新たに追加された値を追加
        for (Map.Entry<String, Boolean> entry : details.entrySet()) {
            if (savedHashMap.get(entry.getKey()) == null) {
                savedHashMap.put(entry.getKey(), entry.getValue());
            }
        }
        String saveData = gson.toJson(savedHashMap);
        PrefUtil.putString(R.string.tweet_detail_setting_order, saveData);
     }

}
