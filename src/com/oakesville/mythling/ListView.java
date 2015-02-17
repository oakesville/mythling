package com.oakesville.mythling;

import android.content.Context;
import android.util.AttributeSet;

public class ListView extends android.widget.ListView {

    public ListView(Context context) {
        this(context, null);
    }

    public ListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0 /*android.R.attr.listViewStyle*/);
    }

    public ListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // test
    }
}
