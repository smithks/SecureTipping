package com.example.keegan.securetipping;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.NumberPicker;

/**
 * Created by Keegan on 11/16/2015.
 */
//@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MyNumberPicker extends NumberPicker {

    public MyNumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeValues();
    }

    public MyNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeValues();
    }

    public MyNumberPicker(Context context) {
        super(context);
        initializeValues();
    }

    private void initializeValues(){
        setMaxValue(100);
        setMinValue(0);
    }
}
