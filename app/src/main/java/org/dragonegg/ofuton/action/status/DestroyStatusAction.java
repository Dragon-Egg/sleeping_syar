package org.dragonegg.ofuton.action.status;

import android.content.Context;

import org.dragonegg.ofuton.R;
import org.dragonegg.ofuton.adapter.TweetStatusAdapter;
import org.dragonegg.ofuton.util.AppUtil;
import org.dragonegg.ofuton.util.ParallelTask;
import org.dragonegg.ofuton.util.TwitterUtils;

import twitter4j.Twitter;
import twitter4j.TwitterException;

public class DestroyStatusAction extends ClickAction {
	twitter4j.Status selectedStatus;
	public DestroyStatusAction(Context context, twitter4j.Status status) {
		super(context, R.string.destroy_status, R.drawable.ic_delete);
		selectedStatus = status;
	}
	@Override
	public void doAction() {
		ParallelTask<Void, twitter4j.Status> task = new ParallelTask<Void, twitter4j.Status>() {
			@Override
			protected twitter4j.Status doInBackground() {
				Twitter mTwitter = TwitterUtils.getTwitterInstance();
				try {
					if(!selectedStatus.isRetweet()){
						return mTwitter.destroyStatus(selectedStatus.getId());
					} else {
						return mTwitter.destroyStatus(selectedStatus.getRetweetedStatus().getId());
					}
				} catch (TwitterException e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(twitter4j.Status result) {
				if (result == null) {
					AppUtil.showToast("無理でした");
				} else {
					AppUtil.showToast("ツイートを削除しました");
					TweetStatusAdapter.removeItem(selectedStatus);
				}
			}
		};
		task.executeParallel();
	}
}
