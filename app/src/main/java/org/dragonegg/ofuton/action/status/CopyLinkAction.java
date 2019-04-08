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
		String url = "https://twitter.com/" + status.getUser().getScreenName() + "/status/" + status.getId();
		clipboardManager.setPrimaryClip(ClipData.newPlainText("", url));
        AppUtil.showToast(mContext.getString(R.string.link_copied));
	}
}