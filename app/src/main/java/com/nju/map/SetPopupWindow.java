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
 * Created by YSL on 2017/10/6.
 */

public class SetPopupWindow extends PopupWindow {
    public static View popupView;
    private LayoutInflater mInflater;
    private Button setInitButton;
    private Button setDestButton;
    private Button introButton;

    public SetPopupWindow(Activity context, final SetClickListener mlistener) {
        super(context);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        popupView = mInflater.inflate(R.layout.set_popup_window, null);
        setInitButton=(Button)popupView.findViewById(R.id.setInitButton);
        setInitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mlistener.onSetInit();
            }
        });
        setDestButton=(Button)popupView.findViewById(R.id.setDestButton);
        setDestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mlistener.onSetDest();
            }
        });
        introButton=(Button)popupView.findViewById(R.id.introButton);
        introButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mlistener.onIntro();
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
