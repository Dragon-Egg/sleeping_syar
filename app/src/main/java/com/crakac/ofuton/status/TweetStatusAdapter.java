package com.crakac.ofuton.status;

import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.UserMentionEntity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.crakac.ofuton.C;
import com.crakac.ofuton.R;
import com.crakac.ofuton.WebImagePreviewActivity;
import com.crakac.ofuton.user.UserDetailActivity;
import com.crakac.ofuton.util.Account;
import com.crakac.ofuton.util.AppUtil;
import com.crakac.ofuton.util.NetUtil;
import com.crakac.ofuton.util.TwitterUtils;
import com.crakac.ofuton.widget.ColorOverlayOnTouch;

public class TweetStatusAdapter extends ArrayAdapter<Status> {
    private static Context sContext;
    private static LayoutInflater sInflater;
    private static Account sUserAccount;
    private static boolean shouldShowPreview = false;
    private static final String TAG = TweetStatusAdapter.class.getSimpleName();

    private static class ViewHolder {
        View base;
        TextView name;
        TextView screenName;
        TextView text;
        TextView postedAt;
        TextView via;
        TextView retweetedBy;
        ImageView icon;
        ImageView smallIcon;
        ImageView favicon;
        ImageView image;
        ImageView lockedIcon;
    }

    @Override
    public void add(Status status) {
        super.add(status);
        StatusPool.put(status.getId(), status);
    }

    @Override
    public void insert(Status status, int index) {
        super.insert(status, index);
        StatusPool.put(status.getId(), status);
    }

    public TweetStatusAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_1);
        Log.d(TAG, "constructor");
        sContext = context;
        sInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        sUserAccount = TwitterUtils.getCurrentAccount();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Status item = getItem(position);
        return createView(item, convertView);
    }

    public void updateDisplayTime(int position, View view) {
        if (view.getTag() == null) {
            return;// フッタービューは弾く．
        }
        setPostedAtTime((ViewHolder) view.getTag(), getItem(position));
    }

    public void updateFontSize(View view) {
        if (view.getTag() == null) {
            return;
        }
        optimizeFontSize((ViewHolder) view.getTag());
    }

    public static View createView(final Status item, View convertView) {
        ViewHolder holder;

        // レイアウトの構築
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = sInflater.inflate(R.layout.list_item_tweet, null);
            setViewHolder(holder, convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        setBasicInfo(holder, item);
        if (item.isRetweet()) {
            setRetweetView(holder, convertView, item);
        } else {
            setNormalTweetView(holder, convertView, item);
        }
        optimizeFontSize(holder);
        setColors(holder, item);
        // アイコンクリック時の挙動を設定．ユーザー詳細に飛ばす．
        holder.icon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(sContext, UserDetailActivity.class);
                intent.putExtra(C.USER, (item.isRetweet()) ? item.getRetweetedStatus().getUser() : item.getUser());
                sContext.startActivity(intent);
            }
        });

        setPostedAtTime(holder, item);

        return convertView;
    }

    private static void optimizeFontSize(ViewHolder holder) {
        // フォントサイズの調整
        int fontSize = AppUtil.getFontSize();
        float subFontSize = AppUtil.getSubFontSize();
        holder.name.setTextSize(fontSize);
        holder.screenName.setTextSize(subFontSize);
        holder.text.setTextSize(fontSize);
        holder.retweetedBy.setTextSize(subFontSize);
        holder.postedAt.setTextSize(subFontSize);
        holder.via.setTextSize(subFontSize);
    }

    private static void setBasicInfo(ViewHolder holder, Status status) {
        if (status.isRetweet())
            status = status.getRetweetedStatus();
        // ユーザー名＋スクリーンネーム
        holder.name.setText(status.getUser().getName());
        holder.screenName.setText("@" + status.getUser().getScreenName());

        holder.text.setText(AppUtil.getColoredText(status.getText(), status));
        // アイコン
        setIcon(holder.icon, status);
        // 鍵アイコン
        setLockIcon(holder.lockedIcon, status);
        // ☆
        setFavIcon(holder.favicon, status);

        setImagePreview(holder.image, status);
    }

    private static void setImagePreview(final ImageView image, Status status) {
        image.setVisibility(View.GONE);
        if (!shouldShowPreview) return;
        if (status.getMediaEntities().length == 0)
            return;
        image.setVisibility(View.VISIBLE);
        final MediaEntity media = status.getMediaEntities()[0];
        image.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(sContext, WebImagePreviewActivity.class);
                intent.setData(Uri.parse(media.getMediaURL()));
                sContext.startActivity(intent);
                ((Activity) sContext).overridePendingTransition(android.R.anim.fade_in, 0);
            }
        });

        ImageContainer container = (ImageContainer) image.getTag();
        if(container != null) {
            container.cancelRequest();
            image.setImageResource(android.R.color.transparent);
        }
        container = NetUtil.fetchNetworkImageAsync(image, media.getMediaURL());
        image.setTag(container);
    }

    /**
     * 背景色とスクリーンネームの色を調整する
     *
     * @param holder
     * @param status
     */
    private static void setColors(ViewHolder holder, Status status) {
        if (status.isRetweet()) {
            // Retweet
            holder.base.setBackgroundResource(R.color.retweet_background);
            // スクリーンネームの色
            holder.name.setTextColor(getColor(R.color.droid_green));
            holder.screenName.setTextColor(getColor(R.color.droid_green));
        } else if (isMention(status)) {
            // mention
            holder.base.setBackgroundResource(R.color.mention_background);
            holder.name.setTextColor(getColor(R.color.droid_red));
            holder.screenName.setTextColor(getColor(R.color.droid_red));
        } else if (status.getUser().getId() == sUserAccount.getUserId()) {
            // user's own tweet
            holder.base.setBackgroundResource(R.color.mytweet_background);
            holder.name.setTextColor(getColor(R.color.droid_blue));
            holder.screenName.setTextColor(getColor(R.color.droid_blue));
        } else {
            // others' tweet
            holder.base.setBackgroundResource(R.color.timeline_background);
            holder.name.setTextColor(getColor(R.color.droid_blue));
            holder.screenName.setTextColor(getColor(R.color.droid_blue));
        }
    }

    private static void setViewHolder(final ViewHolder holder, View convertView) {
        holder.base = convertView;
        holder.name = (TextView) convertView.findViewById(R.id.name);
        holder.screenName = (TextView) convertView.findViewById(R.id.screenName);
        holder.text = (TextView) convertView.findViewById(R.id.text);
        holder.postedAt = (TextView) convertView.findViewById(R.id.postedAt);
        holder.via = (TextView) convertView.findViewById(R.id.via);
        holder.icon = (ImageView) convertView.findViewById(R.id.icon);
        holder.icon.setOnTouchListener(new ColorOverlayOnTouch());
        holder.smallIcon = (ImageView) convertView.findViewById(R.id.smallIcon);
        holder.retweetedBy = (TextView) convertView.findViewById(R.id.retweeted_by);
        holder.image = (ImageView) convertView.findViewById(R.id.image);
        holder.image.setOnTouchListener(new ColorOverlayOnTouch());
        holder.favicon = (ImageView) convertView.findViewById(R.id.favedStar);
        holder.lockedIcon = (ImageView) convertView.findViewById(R.id.lockedIcon);
    }

    private static void setRetweetView(ViewHolder holder, View convertView, Status origStatus) {
        Status status = origStatus.getRetweetedStatus();

        // 不要な部分を非表示に
        holder.via.setVisibility(View.GONE);
        // 必要な部分を表示
        holder.smallIcon.setVisibility(View.VISIBLE);
        holder.retweetedBy.setVisibility(View.VISIBLE);

        setIcon(holder.smallIcon, origStatus);

        holder.retweetedBy.setText(origStatus.getUser().getName() + " @" + origStatus.getUser().getScreenName() + " ("
                + status.getRetweetCount() + ")");

    }

    private static void setNormalTweetView(ViewHolder holder, View convertView, Status status) {
        // 不要な部分を非表示に
        holder.smallIcon.setVisibility(View.GONE);
        holder.retweetedBy.setVisibility(View.GONE);

        // via表示
        String source = status.getSource();
        if (source.indexOf(">") != -1) {
            source = source.substring(source.indexOf(">") + 1, source.indexOf("</"));
        }
        holder.via.setText("via " + source);
        holder.via.setVisibility(View.VISIBLE);

    }

    private static void setPostedAtTime(ViewHolder holder, Status status) {
        // 日付の設定
        if (status.isRetweet())
            status = status.getRetweetedStatus();
        String timeMode = PreferenceManager.getDefaultSharedPreferences(sContext).getString(
                sContext.getString(R.string.date_display_mode), sContext.getString(R.string.relative));
        String time = (timeMode.equals(sContext.getString(R.string.relative))) ? AppUtil.dateToRelativeTime(status
                .getCreatedAt()) : AppUtil.dateToAbsoluteTime(status.getCreatedAt());
        holder.postedAt.setText(time);
    }

    private static boolean isMention(Status status) {
        boolean isMention = false;
        if (sUserAccount.getScreenName() != null) {
            for (UserMentionEntity entity : status.getUserMentionEntities()) {
                if (entity.getId() == sUserAccount.getUserId()
                        || entity.getScreenName().equals(sUserAccount.getScreenName())) {
                    isMention = true;
                    break;
                }
            }
        }
        return isMention;
    }

    private static void setIcon(ImageView icon, Status item) {
        String url = AppUtil.getIconURL(item.getUser());
        ImageContainer container = (ImageContainer) icon.getTag();
        if (container != null) {
            container.cancelRequest();
        }
        container = NetUtil.fetchIconAsync(icon, url);
        icon.setTag(container);
    }

    private static void setLockIcon(ImageView lockedIcon, Status item) {
        if (item.getUser().isProtected()) {
            lockedIcon.setVisibility(View.VISIBLE);
        } else {
            lockedIcon.setVisibility(View.GONE);
        }
    }

    private static void setFavIcon(ImageView favIcon, Status item) {
        // 星マーク
        if (item.isFavorited()) {
            favIcon.setVisibility(View.VISIBLE);
        } else {
            favIcon.setVisibility(View.GONE);
        }
    }

    private static int getColor(int id) {
        return sContext.getResources().getColor(id);
    }

    public void shouldShowInlinePreview(boolean showPreview){
        shouldShowPreview = showPreview;
    }
}