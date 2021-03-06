package org.dragonegg.ofuton.action.status;

import android.content.Context;
import android.os.Handler;

import org.dragonegg.ofuton.R;
import org.dragonegg.ofuton.adapter.TweetStatusAdapter;
import org.dragonegg.ofuton.util.AppUtil;
import org.dragonegg.ofuton.util.PrefUtil;
import org.dragonegg.ofuton.util.TwitterUtils;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

/**
 * Created by Kosuke on 2017/12/18.
 */

public class FavAndRetweeAction extends ClickAction {
    private final Status selectedStatus;

    public FavAndRetweeAction(Context context, Status status) {
        super(context, R.string.fav_and_retweet, (PrefUtil.getBoolean(R.string.remember_star, false)) ? R.drawable.ic_star_and_retweet : R.drawable.ic_fav_and_retweet);
        selectedStatus = status;
    }

    @Override
    public void doAction() {
        final Handler handler = new Handler(mContext.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                Twitter twitter = TwitterUtils.getTwitterInstance();
                Status result = null;
                Status original = null;
                try {
                    twitter.createFavorite(selectedStatus.getId());
                } catch (TwitterException e) {
                    AppUtil.showToast(R.string.something_wrong);
                    return;
                }

                try {
                    twitter.retweetStatus(selectedStatus.getId());
                } catch (TwitterException e) {
                    AppUtil.showToast(R.string.something_wrong);
                    return;
                }

                try {
                    result = twitter.showStatus(selectedStatus.getId());
                } catch (TwitterException e) {
                    AppUtil.showToast(R.string.something_wrong);
                }

                handler.post(new OnActionResult(result));
            }
        }).start();
    }

    class OnActionResult implements Runnable {
        Status result;

        OnActionResult(Status result) {
            this.result = result;
        }

        @Override
        public void run() {
            if (result == null){
                AppUtil.showToast(R.string.something_wrong);
                return;
            }
            TweetStatusAdapter.updateItem(selectedStatus, result);
            AppUtil.showToast(R.string.fav_and_retweet_succeess);
        }
    }
}
