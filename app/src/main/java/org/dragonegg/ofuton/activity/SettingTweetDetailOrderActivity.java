package org.dragonegg.ofuton.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jmedeisis.draglinearlayout.DragLinearLayout;

import org.dragonegg.ofuton.R;
import org.dragonegg.ofuton.fragment.dialog.StatusDialogFragment;
import org.dragonegg.ofuton.util.PrefUtil;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;

public class SettingTweetDetailOrderActivity extends FinishableActionbarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tweet_detail_order);
        DragLinearLayout dragLinearLayout = findViewById(R.id.drag_container);
        ScrollView scrollView = findViewById(R.id.drag_view);

        StatusDialogFragment.checkDialogDetails();
        Gson gson = new Gson();
        String viewOrder = PrefUtil.getString(R.string.tweet_detail_setting_order, gson.toJson(new LinkedHashMap(StatusDialogFragment.initialDetails)));
        Type type = new TypeToken<LinkedHashMap<String, Boolean>>(){}.getType();
        LinkedHashMap<String, Boolean> savedHashMap = new LinkedHashMap<>(gson.fromJson(viewOrder, type));
        for (String key: savedHashMap.keySet().toArray(new String[0])) {
            LinearLayout linearLayout = new LinearLayout(this);
            // 画像表示
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(R.drawable.ic_drag_indicator);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            LinearLayout.LayoutParams imageLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            imageLayoutParams.leftMargin = (int)(8 * this.getResources().getDisplayMetrics().density); // 8dp
            imageView.setLayoutParams(imageLayoutParams);
            linearLayout.addView(imageView);
            // アクション名表示
            TextView textView = new TextView(this, null, 0, R.style.TextViewStyle);
            textView.setText(getString(getResources().getIdentifier(key, "string", getPackageName())));
            linearLayout.addView(textView);
            dragLinearLayout.addView(linearLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        for(int i = 0; i < dragLinearLayout.getChildCount(); i++){
            LinearLayout child = (LinearLayout)dragLinearLayout.getChildAt(i);
            dragLinearLayout.setViewDraggable(child, child.getChildAt(0));
        }
        dragLinearLayout.setContainerScrollView(scrollView);
        dragLinearLayout.setOnViewSwapListener(new DragLinearLayout.OnViewSwapListener() {
            @Override
            public void onSwap(View firstView, int firstPosition,
                               View secondView, int secondPosition) {
                StatusDialogFragment.checkDialogDetails();
                Gson gson = new Gson();
                String viewOrder = PrefUtil.getString(R.string.tweet_detail_setting_order, gson.toJson(new LinkedHashMap(StatusDialogFragment.initialDetails)));
                Type type = new TypeToken<LinkedHashMap<String, Boolean>>(){}.getType();
                LinkedHashMap<String, Boolean> savedHashMap = new LinkedHashMap<>(gson.fromJson(viewOrder, type));
                LinkedHashMap<String, Boolean> after = new LinkedHashMap<>();
                String[] keys = savedHashMap.keySet().toArray(new String[0]);
                for (int i = 0; i < savedHashMap.size(); i++) {
                    if (i == firstPosition) {
                        after.put(keys[secondPosition], savedHashMap.get(keys[secondPosition]));
                    } else  if (i == secondPosition) {
                        after.put(keys[firstPosition], savedHashMap.get(keys[firstPosition]));
                    } else {
                        after.put(keys[i], savedHashMap.get(keys[i]));
                    }
                }
                String saveData = gson.toJson(after);
                PrefUtil.putString(R.string.tweet_detail_setting_order, saveData);
            }
        });
    }

}


