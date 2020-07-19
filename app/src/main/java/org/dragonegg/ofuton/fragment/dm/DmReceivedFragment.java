package org.dragonegg.ofuton.fragment.dm;

import org.dragonegg.ofuton.util.TwitterUtils;

import java.util.ArrayList;
import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.DirectMessageList;
import twitter4j.TwitterException;

public class DmReceivedFragment extends AbstractDmFragment {

	@Override
	protected List<DirectMessage> fetchMessages(long maxId, int counts) {
		List<DirectMessage> list = new ArrayList<>();
		try {
			DirectMessageList allMessages = mTwitter.getDirectMessages(counts);
			for (DirectMessage message : allMessages) {
				if (message.getRecipientId() == TwitterUtils.getCurrentAccount().getUserId()) {
					list.add(message);
				}
			}
			return list;
		} catch (TwitterException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected List<DirectMessage> newMessages(long sinceId, int counts) {
		try {
			return mTwitter.getDirectMessages(counts);
		} catch (TwitterException e) {
			e.printStackTrace();
		}
		return null;
	}
}
