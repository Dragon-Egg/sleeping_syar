package org.dragonegg.ofuton.action.status;

import android.content.Context;
import android.content.Intent;

import org.dragonegg.ofuton.C;
import org.dragonegg.ofuton.R;
import org.dragonegg.ofuton.activity.SearchActivity;
import org.dragonegg.ofuton.activity.TweetActivity;

public class HashtagSearchAction extends ClickAction {
	private String tag;
	public HashtagSearchAction(Context context, String tag) {
		super(context, 0, R.drawable.ic_search);
		this.tag = tag;
	}

	@Override
	public String getText() {
		return "#" + tag;
	}

	@Override
	public void doAction() {
		Intent intent = new Intent(mContext, SearchActivity.class);
		intent.putExtra(C.QUERY, "#" + tag);
		mContext.startActivity(intent);
	}
}
