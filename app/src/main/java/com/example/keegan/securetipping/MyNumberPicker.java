package com.example.keegan.securetipping;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.NumberPicker;

/**
 * Created by Keegan on 11/16/2015.
 */
@TargetApi()
public class MyNumberPicker extends NumberPicker {

    public MyNumberPicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public MyNumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MyNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyNumberPicker(Context context) {
        super(context);
    }
}
