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
    NumberPicker myPicker;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.number_picker_preference);
        setPositiveButtonText(R.string.ok);
        setNegativeButtonText(R.string.cancel);

        setDialogIcon(null);

    }

    @Override
    public void onBindView(View view){
        super.onBindView(view);
        myPicker = (NumberPicker) view.findViewById(R.id.percent_number_picker);
        //https://github.com/CyanogenMod/android_packages_apps_Trebuchet/blob/cm-10.2/src/com/cyanogenmod/trebuchet/preference/NumberPickerPreference.java
        //https://github.com/CyanogenMod/android_packages_apps_Trebuchet/blob/cm-10.2/res/layout/number_picker_dialog.xml
    }



    public String getEntry(){
        return Integer.toString(currentSetting);
    }
}
