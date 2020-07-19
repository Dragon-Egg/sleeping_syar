package org.dragonegg.ofuton.adapter;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.dragonegg.ofuton.R;
import org.dragonegg.ofuton.util.AppUtil;
import org.dragonegg.ofuton.util.ParallelTask;
import org.dragonegg.ofuton.util.PrefUtil;
import org.dragonegg.ofuton.util.TwitterUtils;

import twitter4j.DirectMessage;
import twitter4j.TwitterException;
import twitter4j.User;

public class DmAdapter extends ArrayAdapter<DirectMessage> {

    private ViewConstructor mViewConstructor;

    public DmAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_1);
        mViewConstructor = new ViewConstructor(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DirectMessage item = getItem(position);
        return mViewConstructor.createView(item, convertView);
    }

    public static class ViewConstructor {
        private LayoutInflater mInflater;
        private Context mContext;

        public ViewConstructor(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public View createView(DirectMessage item, View convertView) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item_dm, null);
                holder = new ViewHolder();
                holder.name = convertView.findViewById(R.id.name);
                holder.text = convertView.findViewById(R.id.text);
                holder.sentTo = convertView.findViewById(R.id.sentTo);
                holder.postedAt = convertView.findViewById(R.id.postedAt);
                holder.icon = convertView.findViewById(R.id.icon);
                holder.smallIcon = convertView.findViewById(R.id.smallIcon);
                holder.lockedIcon = convertView.findViewById(R.id.lockedIcon);
                convertView.setTag(holder);
                convertView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            v.getForeground().setHotspot(event.getX(), event.getY());
                        }
                        return false;
                    }
                });
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // フォントサイズの調整
            int fontSize = PrefUtil.getFontSize();
            float subFontSize = PrefUtil.getSubFontSize();
            holder.name.setTextSize(fontSize);
            holder.text.setTextSize(fontSize);
            holder.postedAt.setTextSize(subFontSize);
            holder.sentTo.setTextSize(fontSize);

            holder.name.setText("");
            holder.sentTo.setText("");

            // 本文
            holder.text.setText(AppUtil.getColoredText(item.getText(), item));

            // 日付の設定
            holder.postedAt.setText(AppUtil.dateToAbsoluteTime(item.getCreatedAt()));
            holder.postedAt.setVisibility(View.VISIBLE);

            GetUserDataTask task = new GetUserDataTask(holder, item);
            task.executeParallel();

            return convertView;
        }

        private static class ViewHolder {
            TextView name;
            TextView text;
            TextView postedAt;
            TextView sentTo;
            ImageView icon;
            ImageView smallIcon;
            ImageView lockedIcon;
        }

        private class GetUserDataTask extends ParallelTask<Void, Void> {
            ViewHolder holder;
            DirectMessage item;
            User senderUser;
            User recipientUser;

            GetUserDataTask(ViewHolder holder, DirectMessage item) {
                this.holder = holder;
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
                AppUtil.setImage(holder.icon, AppUtil.getIconURL(senderUser));
                AppUtil.setImage(holder.smallIcon, AppUtil.getIconURL(recipientUser));
                // ユーザー名＋スクリーンネーム
                holder.name.setText(senderUser.getName() + " @" + senderUser.getScreenName());
                // 送信先
                holder.sentTo.setText(recipientUser.getScreenName());
                // 鍵アイコン
                if (senderUser.isProtected()) {
                    holder.lockedIcon.setVisibility(View.VISIBLE);
                } else {
                    holder.lockedIcon.setVisibility(View.GONE);
                }
            }
        }
    }
}