package org.dragonegg.ofuton.fragment.dialog;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import org.dragonegg.ofuton.C;
import org.dragonegg.ofuton.R;
import org.dragonegg.ofuton.action.ClickActionAdapter;
import org.dragonegg.ofuton.action.dm.DmReplyAction;
import org.dragonegg.ofuton.action.status.ClickAction;
import org.dragonegg.ofuton.action.status.LinkAction;
import org.dragonegg.ofuton.action.status.UserDetailAction;
import org.dragonegg.ofuton.adapter.DmAdapter;
import org.dragonegg.ofuton.util.AppUtil;
import org.dragonegg.ofuton.util.ParallelTask;
import org.dragonegg.ofuton.util.TwitterUtils;

import java.util.TreeSet;

import twitter4j.DirectMessage;
import twitter4j.MediaEntity;
import twitter4j.TwitterException;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;

/**
 * DMをタップした時に出てくるダイアログ
 *
 * @author Kosuke
 *
 */
public class DmDialogFragment extends DialogFragment {

	private ClickActionAdapter mActionAdapter;
	private Dialog dialog;
	private DirectMessage dm;

	public DmDialogFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.dialog_actions, container);
		// 各種アクションをアダプタに追加して表示
		mActionAdapter = new ClickActionAdapter(getActivity());

		ListView lvActions = view
				.findViewById(R.id.action_list);

		// DM表示部分を作成

		View dmView = new DmAdapter.ViewConstructor(getActivity()).createView(dm, null);
		dmView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		// HeaderViewをadd. setAdapterより先にしないと落ちる
		lvActions.addHeaderView(dmView);
		// アダプタをセット
		lvActions.setAdapter(mActionAdapter);
		SetActionsTask actionsTask = new SetActionsTask(getActivity(), dm);
		actionsTask.executeParallel();

		//アクションタップ次の処理
		lvActions.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ListView lv = (ListView) parent;
				ClickAction item = (ClickAction) lv
						.getItemAtPosition(position);
				dialog.dismiss();
				item.doAction();
			}
		});
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		dialog = getDialog();

		WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();

		DisplayMetrics metrics = getResources().getDisplayMetrics();

		// 縦幅はwrap contentで，横幅は92%で．
        int dialogWidth = (int) Math.min((metrics.widthPixels * 0.85), AppUtil.dpToPx(480));
        int dialogHeight = WindowManager.LayoutParams.WRAP_CONTENT;

		lp.width = dialogWidth;
		lp.height = dialogHeight;
		dialog.getWindow().setAttributes(lp);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		dialog = new Dialog(getActivity());
		dm = (DirectMessage)getArguments().getSerializable(C.DM);
		// タイトル部分を消す．消さないとダイアログの表示位置が下にずれる
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

		// 全画面化
		dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

		// 背景を透明に
		dialog.getWindow().setBackgroundDrawable(
				new ColorDrawable(Color.TRANSPARENT));

		return dialog;
	}
	private class SetActionsTask extends ParallelTask<Void, Void> {
		FragmentActivity activity;
		DirectMessage item;
		User senderUser;
		User recipientUser;

		SetActionsTask(FragmentActivity activity, DirectMessage item) {
			this.activity = activity;
			this.item = item;
		}

		@Override
		protected Void doInBackground() {
			try {
				senderUser = TwitterUtils.getTwitterInstance().showUser(item.getSenderId());
				recipientUser = TwitterUtils.getTwitterInstance().showUser(item.getRecipientId());
			} catch (TwitterException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void param) {
			//返信する
			if(senderUser.getId() != TwitterUtils.getCurrentAccount().getUserId()) {
				mActionAdapter.add(new DmReplyAction(getActivity(), dm, senderUser));
			}
			//ユーザー詳細
			mActionAdapter.add(new UserDetailAction(getActivity(), senderUser));
			if(dm.getSenderId() != dm.getRecipientId()){
				mActionAdapter.add(new UserDetailAction(getActivity(), recipientUser));
			}
			TreeSet<Long> mentionedUserIds = new TreeSet<>();
			mentionedUserIds.add(dm.getSenderId());
			mentionedUserIds.add(dm.getRecipientId());

			for(UserMentionEntity e : dm.getUserMentionEntities()){
				if(!mentionedUserIds.contains(e.getId()))
					mActionAdapter.add(new UserDetailAction(getActivity(), e.getScreenName()));
			}
			for(URLEntity urlEntity : dm.getURLEntities()){
				mActionAdapter.add(new LinkAction(getActivity(), urlEntity.getExpandedURL()));
			}
			for(MediaEntity mediaEntity : dm.getMediaEntities()){
				mActionAdapter.add(new LinkAction(getActivity(), mediaEntity.getExpandedURL()));
			}
		}
	}
}
