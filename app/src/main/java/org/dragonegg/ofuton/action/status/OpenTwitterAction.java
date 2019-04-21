package org.dragonegg.ofuton.action.status;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.dragonegg.ofuton.R;
import org.dragonegg.ofuton.util.AppUtil;

public class OpenTwitterAction extends ClickAction {
    private twitter4j.Status status;
    public OpenTwitterAction(Context context, twitter4j.Status status) {
        super(context, R.string.open_twitter_app, R.drawable.twitter_bird);
        this.status = status;
    }

    @Override
    public void doAction() {
        Intent intent = null;
        long tweetId = 0;

        if (status.isRetweet()) {
            tweetId = status.getRetweetedStatus().getId();
        } else {
            tweetId = status.getId();
        }

        try {
            mContext.getPackageManager().getPackageInfo("com.twitter.android", 0);
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("twitter://status?id=" + tweetId));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        } catch (Exception e) {
            AppUtil.showToast(R.string.no_twitter_app);
        }
    }
}
