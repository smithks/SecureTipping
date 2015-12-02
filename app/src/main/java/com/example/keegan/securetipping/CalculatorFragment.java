package com.example.keegan.securetipping;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.keegan.securetipping.data.HistoryContract.HistoryEntry;
import com.example.keegan.securetipping.data.HistoryDbHelper;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
//TODO edit button appearance, make purple color?
//TODO include about page describing each method, indicate why methods may not be the best for lower/larger amounts (large descrepancy in tip percent vs actual tip percent)
/**
 * Calculator fragment displayed within viewpager.
 * @author Keegan Smith
 * @version  10/31/2015.
 */
public class CalculatorFragment extends Fragment {

    private SQLiteDatabase db;

    private String DEFAULT_TIP;
    private String TIP_METHOD;
    private String[] TIPPING_METHODS;
    private DecimalFormat mDecimalFormat = new DecimalFormat("#0.00");

    private RelativeLayout mSplitCheckLayout;
    private EditText mBillAmountEdit;
    private EditText mTipPercentEdit;
    private EditText mTipAmountEdit;
    private EditText mTotalAmountEdit;
    private TextView mPercentWarningText;
    private EditText mNumberPeopleEdit;
    private EditText mEachPaysEdit;
    private ImageButton mToggleSplitButton;
    private Button mClearButton;
    private Button mSaveButton;
    private Boolean mSplitCheckDisplayed; //Denotes if the split check layout is being displayed
    private Boolean mIgnoreTextChange;  //Flags the textChange listener to not update (used when a field is set programmatically)
    private Boolean mMethodChanged; //If tip method changes on screen load clear fields and lock fields appropriately.
    private Boolean mOverrideTipMethod; //Called if user temporarily changes tip method

    /**
     *
     * @return the rootView, the calculator fragment
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.calculator_fragment, container, false);

        initializeFields(rootView);
        db = HistoryDbHelper.getInstance(getContext()).getWritableDatabase();
        return rootView;
    }

    /**
     * Initializes all member variables and sets listeners.
     * @param rootView the rootView for this fragment
     */
    private void initializeFields(final View rootView){
        mSplitCheckLayout = (RelativeLayout) rootView.findViewById(R.id.split_check_layout);

        mBillAmountEdit = (EditText) rootView.findViewById(R.id.bill_amount_edit);
        mTipPercentEdit = (EditText) rootView.findViewById(R.id.tip_percent_edit);
        mTipAmountEdit = (EditText) rootView.findViewById(R.id.tip_amount_edit);
        mTotalAmountEdit = (EditText) rootView.findViewById(R.id.total_amount_edit);
        mNumberPeopleEdit = (EditText) rootView.findViewById(R.id.number_people_edit);
        mEachPaysEdit = (EditText) rootView.findViewById(R.id.each_pays_edit);

        mPercentWarningText = (TextView) rootView.findViewById(R.id.rounding_warning_text);

        mToggleSplitButton = (ImageButton) rootView.findViewById(R.id.split_toggle_button);
        mClearButton = (Button) rootView.findViewById(R.id.clear_button);
        mSaveButton = (Button) rootView.findViewById(R.id.save_button);
        mSaveButton.getBackground().setColorFilter(Color.parseColor("#F6511D"), PorterDuff.Mode.MULTIPLY);
        mClearButton.getBackground().setColorFilter(Color.parseColor("#F6511D"), PorterDuff.Mode.MULTIPLY);
        mSaveButton.setTextColor(Color.WHITE);
        mClearButton.setTextColor(Color.WHITE);

        mSplitCheckDisplayed = false;
        TIPPING_METHODS = getResources().getStringArray(R.array.tipping_method_array_vaues);
        mOverrideTipMethod = false;
        pullPreferenceValues();

        mBillAmountEdit.addTextChangedListener(new TextChangeListener(this, mBillAmountEdit));
        mTipPercentEdit.addTextChangedListener(new TextChangeListener(this, mTipPercentEdit));
        mTipAmountEdit.addTextChangedListener(new TextChangeListener(this, mTipAmountEdit));
        mTotalAmountEdit.addTextChangedListener(new TextChangeListener(this, mTotalAmountEdit));
        mNumberPeopleEdit.addTextChangedListener(new TextChangeListener(this, mNumberPeopleEdit));
        disableView(mEachPaysEdit); //Don't allow users to edit the each pays field

        mDecimalFormat.setRoundingMode(RoundingMode.CEILING);

        mTipPercentEdit.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                clearViewFocus(); //Remove focus from other editText views that may have it
                if (event.getAction() == MotionEvent.ACTION_UP)
                    showPickerDialog(mTipPercentEdit);
                return true;
            }
        });

        mNumberPeopleEdit.setOnTouchListener(new View.OnTouchListener(){

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                clearViewFocus(); //Remove focus from other editText views that may have it
                if (event.getAction() == MotionEvent.ACTION_UP)
                    showPickerDialog(mNumberPeopleEdit);
                return true;
            }
        });


        //TODO remove database viewer
        Button b = (Button)rootView.findViewById(R.id.delete_db);
        b.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //getContext().deleteDatabase(HistoryDbHelper.DATABASE_NAME);
                Intent dbManager = new Intent(getActivity(), AndroidDatabaseManager.class);
                startActivity(dbManager);
            }
        });

        //Zero out fields on click
        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTextFields();
                clearViewFocus();
            }
        });

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] params = {TIP_METHOD, mBillAmountEdit.getText().toString(), mTipPercentEdit.getText().toString(), mTotalAmountEdit.getText().toString(), mNumberPeopleEdit.getText().toString(), mEachPaysEdit.getText().toString()};
                new StoreTransactionTask().execute(params);
            }
        });

        mToggleSplitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(TIP_METHOD.equals(TIPPING_METHODS[1])){
                    showNormalTipAlertDialog();
                }else {
                    if (mSplitCheckDisplayed) {
                        mSplitCheckLayout.setVisibility(View.INVISIBLE);
                        mToggleSplitButton.setImageResource(R.drawable.ic_add_circle_black_24dp);
                        mSplitCheckDisplayed = false;

                    } else {
                        mSplitCheckLayout.setVisibility(View.VISIBLE);
                        mToggleSplitButton.setImageResource(R.drawable.ic_remove_circle_black_24dp);
                        mSplitCheckDisplayed = true;
                    }
                }
            }
        });

        mIgnoreTextChange = false;
        mMethodChanged = false;
        resetTextFields(); //Set fields to default values
        updateFieldProperties();
    }

    /**
     * Resets calculator and pulls preference values.
     */
    @Override
    public void onResume(){
        super.onResume();
        mOverrideTipMethod = false;
        if(mSplitCheckDisplayed){
            mSplitCheckLayout.setVisibility(View.INVISIBLE);
            mToggleSplitButton.setImageResource(R.drawable.ic_add_circle_black_24dp);
            mSplitCheckDisplayed = false;
        }
        refreshCalculator();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        db.close();
    }

    /**
     * Refreshes values of fields. Called when the screen is scrolled to in page viewer or return from settings.
     */
    public void refreshCalculator(){
        pullPreferenceValues();
        if (mMethodChanged) {
            resetTextFields();
            updateFieldProperties();
            mMethodChanged = false;
        }
    }

    /**
     * Enables or disables fields based on current tipping method. Some fields should not be
     *  modified in order to maintain proper usage.
     */
    private void updateFieldProperties(){
        if (TIP_METHOD.equals(TIPPING_METHODS[0])){ //normal
            enableView(mTipAmountEdit);
            enableView(mTotalAmountEdit);
        } else if(TIP_METHOD.equals(TIPPING_METHODS[1])){ //palindrome
            disableView(mTipAmountEdit);
            disableView(mTotalAmountEdit);
        }
    }

    /**
     * Disables and edit text view.
     * @param view view to disable
     */
    private void disableView(View view){
        view.setEnabled(false);
    }

    /**
     * Enables an editText View for editing
     * @param view View to enable
     */
    private void enableView(View view){
        view.setEnabled(true);
    }

    /**
     * Disables a button
     * @param b button to disable
     */
    private void disableButton(Button b){
        b.setAlpha(.5f); //Make button transparent to display disabled status
        b.setEnabled(false);
    }

    /**
     * Enables a button
     * @param b button to enable
     */
    private void enableButton(Button b){
        b.setAlpha(1f); //Make button opaque
        b.setEnabled(true);
    }

    /**
     * Clears the focus on any view that currently holds it. Used to remove focus from edit text fields when switching
     * screens or using other controls.
     */
    private void clearViewFocus(){
        RelativeLayout rootView = ((RelativeLayout)getActivity().findViewById(R.id.calculator_layout));
        if(rootView != null) {
            View focusChild = rootView.getFocusedChild();
            if (focusChild != null)
                focusChild.clearFocus();
        }
    }

    /**
     *Resets edit text fields to default values.
     */
    private void resetTextFields() {
        mIgnoreTextChange = true;   //Ignore text field text change listeners when setting default values
        clearViewFocus();
        String formattedZero = mDecimalFormat.format(0);
        mBillAmountEdit.setText(formattedZero);
        mTipPercentEdit.setText(DEFAULT_TIP);
        mTipAmountEdit.setText(formattedZero);
        mTotalAmountEdit.setText(formattedZero);
        mNumberPeopleEdit.setText("1");
        mEachPaysEdit.setText(formattedZero);
        disableButton(mSaveButton);
        mIgnoreTextChange = false;
    }

    /**
     * Pulls current value of preferences, the default tip amount and the tip method.
     */
    private void pullPreferenceValues(){
        SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        DEFAULT_TIP = Integer.toString(sPref.getInt(getString(R.string.pref_tip_key), R.integer.pref_tip_default));
        if (!mOverrideTipMethod) {
            String newMethod = sPref.getString(getString(R.string.pref_method_key), getString(R.string.pref_method_default));
            if (TIP_METHOD != null) //If this is not the initial preference load
                if (!TIP_METHOD.equals(newMethod)) //If the new method is different than previous
                    mMethodChanged = true;
            TIP_METHOD = newMethod;
        }
    }

    /**
     * Updates editText fields whenever a key is pressed to make sure all fields display proper value at all times.
     * @param caller The edit text that was modified
     */
    private void updateFields(EditText caller){
        double bill = 0;
        double tipPercent = 0;
        double tipAmount = 0;
        double total = 0;
        double people = 1;
        double eachPays = 0;

        mIgnoreTextChange = true; //Disable text change listeners while updating programmatically.
        mPercentWarningText.setVisibility(View.INVISIBLE);

        String valStr;
        //Collect current values stored in fields
        if (mBillAmountEdit.getText().length() > 0){
            valStr = mBillAmountEdit.getText().toString();
            if(!valStr.equals("."))
                bill = Double.parseDouble(valStr);
        }
        if (mTipPercentEdit.getText().length() > 0 ){
            String tipRaw = mTipPercentEdit.getText().toString();
            tipPercent = Double.parseDouble(tipRaw)/100;
        }
        if (mTipAmountEdit.getText().length() > 0){
            valStr = mTipAmountEdit.getText().toString();
            if(!valStr.equals("."))
                tipAmount = Double.parseDouble(valStr);
        }
        if (mTotalAmountEdit.getText().length()>0){
            valStr = mTotalAmountEdit.getText().toString();
            if(!valStr.equals("."))
                total = Double.parseDouble(valStr);
        }
        if (mNumberPeopleEdit.getText().length() > 0) {
            people = Double.parseDouble(mNumberPeopleEdit.getText().toString());
        }
        if (mEachPaysEdit.getText().length() > 0){
            valStr = mEachPaysEdit.getText().toString();
            if (!valStr.equals("."))
                eachPays = Double.parseDouble(valStr);
        }

        //Update fields
        if (caller.getId() == mBillAmountEdit.getId()){
            total = updateTotalAmounts(bill, tipPercent, people);
            tipAmount = updateTipAmountFromTotal(bill, total);
        } else if (caller.getId() == mTipPercentEdit.getId()){
            total = updateTotalAmounts(bill, tipPercent, people);
            tipAmount = updateTipAmountFromTotal(bill, total);
        } else if (caller.getId() == mTipAmountEdit.getId()){
            tipPercent = updateTipPercent(bill, tipAmount);
            total = updateTotalAmounts(bill, tipPercent, people);
        } else if (caller.getId() == mTotalAmountEdit.getId()){
            tipAmount = updateTipAmountFromTotal(bill, total);
            tipPercent = updateTipPercent(bill, tipAmount);
            eachPays = updateEachPays(total, people);
        } else if (caller.getId() == mNumberPeopleEdit.getId()){
            eachPays = updateEachPays(total, people);
        }

        if(eachPays > 0)
            enableButton(mSaveButton);
        else
            disableButton(mSaveButton);

        mIgnoreTextChange = false; //Enable the text change listeners again
    }

    /**
     * Updates tip amount using bill and tip percent.
     * @return the tip amount
     */
    private double updateTipAmount(double bill, double tipPercent){
        double tipAmount = bill * tipPercent;
        if (tipAmount < 0) //Do not display negative tip amounts
            tipAmount = 0;
        mTipAmountEdit.setText(mDecimalFormat.format(tipAmount));
        return tipAmount;
    }

    /**
     * Updates tip amount using bill and total.
     * @return the tip amount
     */
    private double updateTipAmountFromTotal(double bill, double total){
        double tipAmount = total - bill;
        if (tipAmount < 0) //Do not display negative tipAmounts
            tipAmount = 0;
        mTipAmountEdit.setText(mDecimalFormat.format(tipAmount));
        return tipAmount;
    }

    /**
     * Calculates the total field based on the currently selected tipping method.
     * @param bill The current value of the bill
     * @param tipPercent the current value of the tip percent
     * @param people the current value for number of people
     * @return the new value of the total
     */
    private double updateTotalAmounts(double bill, double tipPercent, double people){
        double total = bill + (bill * tipPercent);;
        if(TIP_METHOD.equals(TIPPING_METHODS[1])) {//palindrome
            if(total > 0) {
                String choppedValue = Double.toString(total).split("\\.")[0]; //Cuts off amount following decimal (24.90 becomes 24)
                if(Integer.parseInt(choppedValue)>0){ //Don't mirror a dollar value of 0
                    double newTotal = mirrorAmount(choppedValue);
                    if (newTotal < bill){ //In the case where new total is smaller than the original bill ($2.5 bill + .20 tip = $2.20 total)
                        choppedValue = Double.toString(total+1).split("\\.")[0]; //Cuts off amount following decimal (24.90 becomes 24)
                        newTotal = mirrorAmount(choppedValue);
                    }
                    total = newTotal;

                    double newTipPercentage = (total - bill) / bill;
                    if (Math.abs(newTipPercentage - tipPercent)>.30){ //If the difference is over 50 percent
                        String newTipStr = getTipPercentString(newTipPercentage*100);
                        mPercentWarningText.setText(getResources().getString(R.string.rounding_warning,newTipStr));
                        mPercentWarningText.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
        updateEachPays(total, people);
        mTotalAmountEdit.setText(mDecimalFormat.format(total));
        return total;
    }

    private double mirrorAmount(String amount){
        String newTotalStr = amount;
        char[] chars = amount.toCharArray();
        if (chars.length == 1){
            newTotalStr = newTotalStr+"."+chars[0];
        }else{
            newTotalStr = newTotalStr+"."+chars[chars.length-1]+chars[chars.length-2];
        }
        return Double.parseDouble(newTotalStr);
    }

    /**
     * Updates each pays field using total and number of people.
     * @return each pays value
     */
    private double updateEachPays(double total, double people){
        double eachPays = total / people;
        mEachPaysEdit.setText(mDecimalFormat.format(eachPays));
        return eachPays;
    }

    /**
     * Updates tip percent using bill and tip amount.
     * @return the tip percent as decimal (25% = .25)
     */
    private double updateTipPercent(double bill, double tipAmount){
        double tipPercent = 0;
        if (bill > 0) //Don't divide by zero
            tipPercent= tipAmount / bill;
        double tipPercentRead = tipPercent * 100;
        mTipPercentEdit.setText(getTipPercentString(tipPercentRead));
        return tipPercent; //Return original value of tip before concatenation
    }

    /**
     * Returns string representation of tip percentage. Trims off any
     * decimal values.
     * @param tipPercentRead the tip percentage (25.02% = 25.02)
     * @return The tip with trailing decimals trimmed off
     */
    private String getTipPercentString(double tipPercentRead){
        String tip = Double.toString(tipPercentRead);
        if (tipPercentRead < 0) //Display 0 if negative tip
            tip = "0";
        if (tip.contains(".")) {
            int index = tip.indexOf(".");
            tip = tip.substring(0, index); //Display value concatenated at decimal
        }
        return tip;
    }

    /**
     * Displays a dialog alert when the user attempts to open the check split section when not using
     * normal tip calculation method.
     */
    private void showNormalTipAlertDialog(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(getResources().getString(R.string.alert_dialog_toggle_title));
        dialog.setMessage(getResources().getString(R.string.alert_dialog_toggle_message));
        dialog.setNeutralButton(getResources().getString(R.string.alert_dialog_toggle_neutral), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TIP_METHOD = TIPPING_METHODS[0]; //Set tipping method to normal for duration of this activity
                mOverrideTipMethod = true;
                updateFields(mBillAmountEdit); //Recalculate fields based on bill amount
                updateFieldProperties();
                mSplitCheckLayout.setVisibility(View.VISIBLE);
                mToggleSplitButton.setImageResource(R.drawable.ic_remove_circle_black_24dp);
                mSplitCheckDisplayed = true;
                dialog.dismiss();
            }
        });

        dialog.setPositiveButton(getResources().getString(R.string.alert_dialog_toggle_positive), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /**
     * Displays a number picker dialog that will be used when the user presses the tip percent or number of people edit texts.
     * @param caller The calling editText object.
     */
    private void showPickerDialog(final EditText caller){

        final Dialog dialog = new Dialog(getContext());
        if (caller.getId() == mTipPercentEdit.getId())
            dialog.setTitle(getResources().getString(R.string.picker_dialog_percent_title));
        else
            dialog.setTitle(getResources().getString(R.string.picker_dialog_people_title));
        dialog.setContentView(R.layout.number_picker_dialog);
        final NumberPicker picker = (NumberPicker) dialog.findViewById(R.id.dialog_number_picker);
        Button okButton = (Button) dialog.findViewById(R.id.dialog_ok_button);
        Button cancelButton = (Button) dialog.findViewById(R.id.dialog_cancel_button);

        picker.setMaxValue(100);
        picker.setMinValue(caller.getId() == mTipPercentEdit.getId() ? 0 : 1);
        picker.setValue(Integer.parseInt(caller.getText().toString()));
        picker.setWrapSelectorWheel(false);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                picker.clearFocus();
                caller.setText(Integer.toString(picker.getValue()));
                dialog.dismiss();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
    /**
     * Text Change Listener that will listen for any key presses within the editText fields of the calculator.
     * Will dynamically update other fields whenever a change is detected.
     * @author Keegan Smith
     * @version 10/31/2015
     */
    private class TextChangeListener implements TextWatcher{

        CalculatorFragment fragment;
        EditText host;

        public TextChangeListener(CalculatorFragment fragment, EditText host){
            this.fragment = fragment;
            this.host = host;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        /**
         * Update all fields every time any field is modified.
         */
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!mIgnoreTextChange)
                fragment.updateFields(host);
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }

    /**
     * This class handles storing the current values of the fields into the history database.
     * Used whenever the save transaction button is pressed.
     */
    private class StoreTransactionTask extends AsyncTask<String, Void, Long>{

        double eachPaysBefore;

        /**
         * Called after data has been inserted into database. Disables save button if data in fields has not changed
         * and presents a toast message on the status of the insert.
         * @param result result of the insert, -1 if insert failed.
         */
        @Override
        protected void onPostExecute(Long result){
            if(result != -1){
                //Race condition depending on how long insert took, before disabling button check if each pays has changed
                if (mEachPaysEdit.getText().length() > 0){
                    if(eachPaysBefore == Double.parseDouble(mEachPaysEdit.getText().toString())){ //No change, no race condition, disable button so same data cannot be saved
                        //mSaveButton.setEnabled(false);
                        disableButton(mSaveButton);
                    }
                }
                Toast.makeText(getContext(),getResources().getString(R.string.toast_store_successful),Toast.LENGTH_SHORT).show();

            }else{
                Toast.makeText(getContext(),getResources().getString(R.string.toast_store_failed),Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Parses values passed in through params and stores them into the history database.
         * @param params The values of text fields at the time of button press
         * @return Index of new value in database
         */
        @Override
        protected Long doInBackground(String... params) {

            eachPaysBefore = Double.parseDouble(params[5]); //Save eachPaysBefore value for comparison once insert is complete
            ContentValues values = new ContentValues();
            values.put(HistoryEntry.COLUMN_DATE,getTimestamp());
            values.put(HistoryEntry.COLUMN_METHOD,params[0]);
            values.put(HistoryEntry.COLUMN_BILL,params[1]);
            values.put(HistoryEntry.COLUMN_TIP_PERCENT,params[2]);
            values.put(HistoryEntry.COLUMN_TOTAL,params[3]);
            values.put(HistoryEntry.COLUMN_PEOPLE, params[4]);
            values.put(HistoryEntry.COLUMN_EACH_PAYS, params[5]);

            return db.insert(HistoryEntry.TABLE_NAME,null,values);
        }

        /**
         * Gets the current timestamp for this locale
         * @return timestamp converted through default locale as a string
         */
        public String getTimestamp(){
            Date date = Calendar.getInstance().getTime();
            return Long.toString(date.getTime());
        }
    }
}