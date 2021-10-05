package com.nju.map;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;

/**
 * Created by YSL on 2017/6/18.
 */

public class GuidePopupWindow extends PopupWindow {
    public static View popupView;
    private LayoutInflater mInflater;
    private Button confirmButton;

    public GuidePopupWindow(Activity context, final GuideClickListener mlistener) {
        super(context);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        popupView = mInflater.inflate(R.layout.guide_popup_window, null);
        confirmButton=(Button)popupView.findViewById(R.id.endButton);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mlistener.onEnd();
            }
        });
        setContentView(popupView);
        setWidth(ActionBar.LayoutParams.MATCH_PARENT);
        setHeight(ActionBar.LayoutParams.WRAP_CONTENT);
        setTouchable(true);
        setOutsideTouchable(false);
        setFocusable(true);
        setBackgroundDrawable(new BitmapDrawable(context.getResources()));
        update();
        setAnimationStyle(R.style.DialogShowStyle);
    }
}
