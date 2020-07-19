package org.dragonegg.ofuton.action.dm;

import android.content.Context;
import android.content.Intent;

import org.dragonegg.ofuton.C;
import org.dragonegg.ofuton.R;
import org.dragonegg.ofuton.action.status.ClickAction;
import org.dragonegg.ofuton.activity.ComposeDmActivity;

public class DmReplyAction extends ClickAction {
	private twitter4j.DirectMessage dm;
	private twitter4j.User senderUser;
	public DmReplyAction(Context context, twitter4j.DirectMessage dm, twitter4j.User senderUser) {
		super(context, R.string.reply, R.drawable.ic_email);
		this.dm = dm;
		this.senderUser = senderUser;
	}
	@Override
	public void doAction() {
		Intent intent = new Intent(mContext, ComposeDmActivity.class);
		intent.putExtra(C.DM, dm);
		intent.putExtra(C.USER, senderUser);
		mContext.startActivity(intent);
	}
}