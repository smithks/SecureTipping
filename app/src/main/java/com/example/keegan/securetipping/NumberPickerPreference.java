package com.example.keegan.securetipping;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

/**
 * Created by Keegan on 11/13/2015.
 */
//TODO finish  number picker
public class NumberPickerPreference extends DialogPreference {

    int currentSetting;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.number_picker_preference);
        setPositiveButtonText(R.string.ok);
        setNegativeButtonText(R.string.cancel);

        //NumberPicker picker = (NumberPicker)getView(null,null);

        setDialogIcon(null);

    }

    @Override
    public void onBindView(View view){
        super.onBindView(view);
        NumberPicker mypicker = (NumberPicker) view.findViewById(R.id.percent_number_picker);
    }

    public String getEntry(){
        return Integer.toString(currentSetting);
    }
}
