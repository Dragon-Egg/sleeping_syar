package org.dragonegg.ofuton.action.status;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import org.dragonegg.ofuton.R;
import org.dragonegg.ofuton.util.AppUtil;

public class CopyLinkAction extends ClickAction {
	private twitter4j.Status status;
	public CopyLinkAction(Context context, twitter4j.Status status) {
		super(context, R.string.show_link_copy, R.drawable.ic_share);
		this.status = status;
	}

	@Override
	public void doAction() {
		ClipboardManager clipboardManager = (ClipboardManager)mContext.getSystemService(Context.CLIPBOARD_SERVICE);
		if (clipboardManager == null) {
			return;
		}
		String screenName;
		long tweetId;
		// リツイートかどうか
		if (status.getRetweetedStatus() != null) {
			twitter4j.Status retweetedStatus = status.getRetweetedStatus();
			screenName = retweetedStatus.getUser().getScreenName();
			tweetId = retweetedStatus.getId();
		} else {
			screenName = status.getUser().getScreenName();
			tweetId = status.getId();
		}
		String url = "https://twitter.com/" + screenName + "/status/" + tweetId;
		clipboardManager.setPrimaryClip(ClipData.newPlainText("", url));
        AppUtil.showToast(mContext.getString(R.string.link_copied));
	}
}