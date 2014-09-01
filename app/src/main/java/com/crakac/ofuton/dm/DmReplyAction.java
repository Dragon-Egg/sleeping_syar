package com.crakac.ofuton.dm;

import android.content.Context;
import android.content.Intent;

import com.crakac.ofuton.R;
import com.crakac.ofuton.C;
import com.crakac.ofuton.status.action.ClickAction;

public class DmReplyAction extends ClickAction {
	private twitter4j.DirectMessage dm;
	public DmReplyAction(Context context, twitter4j.DirectMessage dm) {
		super(context, R.string.reply, R.drawable.ic_dm);
		this.dm = dm;
	}
	@Override
	public void doAction() {
		Intent intent = new Intent(mContext, ComposeDmActivity.class);
		intent.putExtra(C.DM, dm);
		intent.putExtra(C.USER, dm.getSender());
		mContext.startActivity(intent);
	}
}