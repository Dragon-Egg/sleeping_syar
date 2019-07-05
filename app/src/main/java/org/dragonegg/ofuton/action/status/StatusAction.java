package org.dragonegg.ofuton.action.status;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import org.dragonegg.ofuton.C;
import org.dragonegg.ofuton.R;
import org.dragonegg.ofuton.fragment.dialog.StatusDialogFragment;
import org.dragonegg.ofuton.util.AppUtil;
import org.dragonegg.ofuton.util.ParallelTask;
import org.dragonegg.ofuton.util.TwitterUtils;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class StatusAction extends ClickAction {
	private Long statusId;
	private FragmentActivity mActivity;
	private String url;
	public StatusAction(FragmentActivity activity, Long statusId, String url) {
		super(activity, 0, R.drawable.ic_open_in_browser);
		this.statusId = statusId;
		this.mActivity = activity;
		this.url = url;
	}

	public String getText() {
		return url;
	}

	@Override
	public void doAction() {
		ParallelTask<Void, Status> task = new ParallelTask<Void, twitter4j.Status>() {
			Twitter mTwitter = TwitterUtils.getTwitterInstance();
			private twitter4j.Status status;
			@Override
			protected twitter4j.Status doInBackground() {
				try {
					status = mTwitter.showStatus(statusId);
				} catch (TwitterException e) {
					if (e.getErrorCode() != 404) {
						e.printStackTrace();
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(twitter4j.Status result) {
				if (status == null) {
					AppUtil.showToast(mContext.getString(R.string.something_wrong));
					return;
				}
				StatusDialogFragment dialog = new StatusDialogFragment();
				Bundle b = new Bundle();
				b.putSerializable(C.STATUS, status);
				dialog.setArguments(b);
				dialog.show(mActivity.getSupportFragmentManager(), "dialog");
			}
		};
		task.executeParallel();
	}
}