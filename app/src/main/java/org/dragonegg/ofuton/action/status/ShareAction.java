package org.dragonegg.ofuton.action.status;

import android.content.Context;
import android.content.Intent;

import org.dragonegg.ofuton.R;

public class ShareAction extends ClickAction {
	private twitter4j.Status status;
	public ShareAction(Context context, twitter4j.Status status) {
		super(context, R.string.share, R.drawable.ic_share);
		this.status = status;
	}

	@Override
	public void doAction() {
		String screenName;
		long tweetId;
		// リツイートかどうか
		if (status.isRetweet()) {
			twitter4j.Status retweetedStatus = status.getRetweetedStatus();
			screenName = retweetedStatus.getUser().getScreenName();
			tweetId = retweetedStatus.getId();
		} else {
			screenName = status.getUser().getScreenName();
			tweetId = status.getId();
		}
		String url = "https://twitter.com/" + screenName + "/status/" + tweetId;
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, url);
		sendIntent.setType("text/plain");

		Intent shareIntent = Intent.createChooser(sendIntent, null);
		mContext.startActivity(shareIntent);
	}
}