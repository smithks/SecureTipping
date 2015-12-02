package com.example.keegan.securetipping;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

/**
 * Dialog preference for the Number Picker that maintains the default tip percentage.
 * Created by Keegan on 11/13/2015.
 */
public class NumberPickerPreference extends DialogPreference {

    NumberPicker mPicker;
    int mCurrValue;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    @Override
    protected View onCreateDialogView(){
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.number_picker_preference,null);
        mPicker = (NumberPicker) view.findViewById(R.id.percent_number_picker);

        mPicker.setMinValue(0);
        mPicker.setMaxValue(100);
        mPicker.setValue(mCurrValue);


        return view;
    }

    /**
     * Saves value from picker on dialog close
     * @param positiveResult indicates whether a positive action ("OK" pressed) was taken
     */
    @Override
    protected void onDialogClosed(boolean positiveResult){
        if (positiveResult) {
            mPicker.clearFocus(); //Clear focus if user manually enters value
            mCurrValue = mPicker.getValue();
            persistInt(mCurrValue);
            notifyChanged();
        }
    }

    /**
     * Restores persisted value of the preference or uses passed default value.
     * @param restorePersistedValue denotes whether to use stored preference
     * @param defaultValue default value to use otherwise
     */
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue){
        mCurrValue = restorePersistedValue ? getPersistedInt(R.integer.pref_tip_default) : (int) defaultValue;
    }

    /**
     * Fetches the current value of the dialog preference.
     * @return current value of the preference
     */
    public CharSequence getEntry() {
        return Integer.toString(mCurrValue);
    }
}