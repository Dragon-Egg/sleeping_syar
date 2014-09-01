package com.crakac.ofuton.user;

import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.TwitterException;

import com.crakac.ofuton.timeline.AbstractTimelineFragment;
import com.crakac.ofuton.util.AppUtil;

public class UserTimelineFragment extends AbstractTimelineFragment {

	@Override
	protected List<Status> newStatuses(long sinceId, int count) {
		try {
			return mTwitter.getUserTimeline(mUserId,
					new Paging().sinceId(sinceId).count(count));
		} catch (TwitterException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected List<Status> previousStatuses(long maxId, int count) {
		try {
			return mTwitter.getUserTimeline(mUserId,
					new Paging().maxId(maxId - 1).count(count));
		} catch (TwitterException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void failToGetStatuses() {
		AppUtil.showToast("ツイートの取得に失敗しました");
	}

	@Override
	public String getTimelineName() {
		return "ツイート";
	}
}