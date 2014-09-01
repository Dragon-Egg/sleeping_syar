package com.crakac.ofuton.status.action;

import twitter4j.User;
import android.content.Context;
import android.content.Intent;

import com.crakac.ofuton.R;
import com.crakac.ofuton.C;
import com.crakac.ofuton.user.UserDetailActivity;

public class UserDetailAction extends ClickAction {
	private User user;
	private String screenName;
	public UserDetailAction(Context context, twitter4j.Status status) {
		super(context, 0, R.drawable.ic_menu_user);
		user = status.getUser();
		screenName = user.getScreenName();
	}

	public UserDetailAction(Context context, String screenName) {
		super(context, 0, R.drawable.ic_menu_user);
		this.screenName = screenName;
	}
	
	public UserDetailAction(Context context, User user) {
		super(context, 0, R.drawable.ic_menu_user);
		this.user = user;
		screenName = user.getScreenName();
	}

	@Override
	public String getText() {
		return "@" + screenName;
	}

	@Override
	public void doAction() {
		Intent intent = new Intent(mContext, UserDetailActivity.class);
		intent.putExtra(C.USER, user);
		intent.putExtra(C.SCREEN_NAME, screenName);
		mContext.startActivity(intent);
	}
}