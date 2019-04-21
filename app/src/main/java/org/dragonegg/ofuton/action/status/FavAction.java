package org.dragonegg.ofuton.action.status;

import android.content.Context;

import org.dragonegg.ofuton.R;
import org.dragonegg.ofuton.adapter.TweetStatusAdapter;
import org.dragonegg.ofuton.util.AppUtil;
import org.dragonegg.ofuton.util.ParallelTask;
import org.dragonegg.ofuton.util.PrefUtil;
import org.dragonegg.ofuton.util.TwitterUtils;

import twitter4j.Twitter;
import twitter4j.TwitterException;

public class FavAction extends ClickAction {

    private twitter4j.Status selectedStatus;

    public FavAction(Context context, twitter4j.Status status) {
        super(context, R.string.favorite, (PrefUtil.getBoolean(R.string.remember_star, false)) ? R.drawable.ic_star : R.drawable.ic_fav);
        mContext = context;
        selectedStatus = status;
        if (selectedStatus.isFavorited()) {
            stringId = R.string.unfavorite;
            iconId = (PrefUtil.getBoolean(R.string.remember_star, false)) ? R.drawable.ic_star_border : R.drawable.ic_fav_border;
        } else {
            stringId = R.string.favorite;
            iconId = (PrefUtil.getBoolean(R.string.remember_star, false)) ? R.drawable.ic_star : R.drawable.ic_fav;
        }
    }

    @Override
    public void doAction() {
        ParallelTask<Void, twitter4j.Status> task = new ParallelTask<Void, twitter4j.Status>() {
            @Override
            protected twitter4j.Status doInBackground() {
                Twitter mTwitter = TwitterUtils.getTwitterInstance();
                try {
                    if (selectedStatus.isFavorited()) {
                        return mTwitter.destroyFavorite(selectedStatus.getId());
                    } else {
                        return mTwitter.createFavorite(selectedStatus.getId());
                    }
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(twitter4j.Status result) {
                if (result != null) {
                    if (selectedStatus.isFavorited()) {
                        AppUtil.showToast("お気に入りから削除しました");
                    } else {
                        AppUtil.showToast("お気に入りに追加しました");
                    }
                    TweetStatusAdapter.updateItem(selectedStatus, result);
                    if(selectedStatus.isRetweet() && result.isRetweet()){
                        TweetStatusAdapter.updateItem(selectedStatus.getRetweetedStatus(), result.getRetweetedStatus());
                    }
                } else {
                    AppUtil.showToast(mContext
                            .getString(R.string.something_wrong));
                }
            }
        };
        task.executeParallel();
    }
}
