package com.example.keegan.securetipping;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
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
import android.widget.Toast;

import com.example.keegan.securetipping.data.HistoryContract.HistoryEntry;
import com.example.keegan.securetipping.data.HistoryDbHelper;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
//TODO edit button appearance, make purple color?
//TODO fix database leaking, close on pause and such
//TODO include about page describing each method, indicate why methods may not be the best for lower/larger amounts (large descrepancy in tip percent vs actual tip percent)
//TODO make interface nice, make total larger.
//TODO improve history section, make searchable? Pop up item on click, show detailed view. Allow delete.
/**
 * Calculator fragment displayed within viewpager.
 * @author Keegan Smith
 * @version  10/31/2015.
 */
public class CalculatorFragment extends Fragment {

    private SQLiteDatabase db;

    private static final String APPTAG = "Secure Tipping";
    private String DEFAULT_TIP;
    private String TIP_METHOD;
    private String[] TIPPING_METHODS;
    private DecimalFormat mDecimalFormat = new DecimalFormat("#0.00");

    private RelativeLayout mSplitCheckLayout;
    private EditText mBillAmountEdit;
    private EditText mTipPercentEdit;
    private EditText mTipAmountEdit;
    private EditText mTotalAmountEdit;
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

        mToggleSplitButton = (ImageButton) rootView.findViewById(R.id.split_toggle_button);
        mClearButton = (Button) rootView.findViewById(R.id.clear_button);
        mSaveButton = (Button) rootView.findViewById(R.id.save_button);

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

        //EditorInfo.ACTION

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
        resetTextFields();
        if (mMethodChanged) {
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
        view.setFocusable(false);
    }

    /**
     * Enables an editText View for editing
     * @param view View to enable
     */
    private void enableView(View view){
        view.setEnabled(true);
        view.setFocusable(true);
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
        mSaveButton.setEnabled(false);
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
        //TODO make this better?
        double bill = 0;
        double tipPercent = 0;
        double tipAmount = 0;
        double total = 0;
        double people = 1;
        double eachPays = 0;

        mIgnoreTextChange = true; //Disable text change listeners while updating programmatically.

        String valStr;
        //Collect current values stored in fields
        if (mBillAmountEdit.getText().length() > 0){
            valStr = mBillAmountEdit.getText().toString();
            if(!valStr.equals("."))
                bill = Double.parseDouble(valStr);
        }
        if (mTipPercentEdit.getText().length() > 0 ){
            String tipRaw = mTipPercentEdit.getText().toString();
            if (tipRaw.length() == 1) //TODO Convert tip amount to percent. Could use work. only works on whole numbers
                tipRaw = ".0"+tipRaw;
            else if (tipRaw.length() == 2) //10-99 percent
                tipRaw = "."+tipRaw;
            else if(tipRaw.length() == 3) //100 percent
                tipRaw = "1";
            tipPercent = Double.parseDouble(tipRaw);
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

        //TODO change this to each pays, whatever is being stored into database
        if(total > 0)
            mSaveButton.setEnabled(true);
        else
            mSaveButton.setEnabled(false);

        mIgnoreTextChange = false; //Enable the text change listeners again
    }

    /**
     * Updates tip amount using bill and tip percent.
     * @return the tip amount
     */
    private double updateTipAmount(double bill, double tipPercent){
        double tipAmount = bill * tipPercent;
        mTipAmountEdit.setText(mDecimalFormat.format(tipAmount));
        return tipAmount;
    }

    /**
     * Updates tip amount using bill and total.
     * @return the tip amount
     */
    private double updateTipAmountFromTotal(double bill, double total){
        double tipAmount = total - bill;
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
                String mirrorAmount = Double.toString(total).split("\\.")[0];
                String newTotal = mirrorAmount;
                if(Integer.parseInt(mirrorAmount)>0){ //Don't mirror a dollar value of 0
                    char[] chars = mirrorAmount.toCharArray();
                    //TODO this may be too hacky
                    if (chars.length == 1){
                        newTotal = newTotal+"."+chars[0];
                    }else{
                        newTotal = newTotal+"."+chars[chars.length-1]+chars[chars.length-2];
                    }
                    total = Double.parseDouble(newTotal);
                }
                //TODO display actual tip?
                double tip = calculateTipPercent(bill,(bill * tipPercent));
                double difference = Math.abs(tipPercent - tip);
            }
        }
        updateEachPays(total, people);
        mTotalAmountEdit.setText(mDecimalFormat.format(total));
        return total;
    }

    /**
     * Updates each pays field using total and number of people.
     * @return each pays value
     */
    //TODO round up so the total is met or exceeded not less than
    private double updateEachPays(double total, double people){
        double eachPays = total / people;
        mEachPaysEdit.setText(mDecimalFormat.format(eachPays));
        return eachPays;
    }

    /**
     * Updates tip percent using bill and tip amount.
     * @return the tip percent
     */
    private double updateTipPercent(double bill, double tipAmount){
        double tipPercent = calculateTipPercent(bill, tipAmount);
        String tip = Double.toString(tipPercent);
        mTipPercentEdit.setText(tip);
        return tipPercent;
    }

    /**
     * Calculates the tip percent using the bill and the tipAmount
     * @return tipPercent as a decimal (25% == .25)
     */
    private double calculateTipPercent(double bill, double tipAmount){
        double tipPercent = 0;
        if (bill > 0) //Don't divide by zero
            tipPercent= tipAmount / bill;
        double tipPercentRead = tipPercent * 100;
        String tip = Double.toString(tipPercentRead);
        if (tip.contains(".")) {
            int index = tip.indexOf(".");
            tip = tip.substring(0, index);
        }
        return tipPercent;
    }

    /**
     * Displays a dialog alert when the user attempts to open the check split section when not using
     * normal tip calculation method.
     */
    private void showNormalTipAlertDialog(){
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle("Check splitting is unavailable"); //TODO add these to strings xml
        dialog.setMessage("We cannot maintain the secure palindrome pattern for each persons check without altering the original total. To split the check, temporarily switch to " +
                "normal calculation by pressing the corresponding button below or change your default calculation method in settings.");
        dialog.setNeutralButton("Use normal calculation", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TIP_METHOD = TIPPING_METHODS[0]; //Set tipping method to normal for duration of this activity
                mOverrideTipMethod = true;
                updateFields(mBillAmountEdit); //Recalculate fields
                updateFieldProperties();
                mSplitCheckLayout.setVisibility(View.VISIBLE);
                mToggleSplitButton.setImageResource(R.drawable.ic_remove_circle_black_24dp);
                mSplitCheckDisplayed = true;
                dialog.dismiss();
            }
        });

        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
            dialog.setTitle("Tip percentage"); //TODO pull strings from strings xml
        else
            dialog.setTitle("Number of people");
        dialog.setContentView(R.layout.number_picker_dialog);
        final NumberPicker picker = (NumberPicker) dialog.findViewById(R.id.dialog_number_picker);
        Button okButton = (Button) dialog.findViewById(R.id.dialog_ok_button);
        Button cancelButton = (Button) dialog.findViewById(R.id.dialog_cancel_button);

        picker.setMaxValue(100);
        //TODO BUG: open app, insert value for tip amount, touch tip percent, crash
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
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        /**
         * Update all fields every time any field is modified.
         */
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!mIgnoreTextChange)
                fragment.updateFields(host);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
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
                        mSaveButton.setEnabled(false);
                    }
                }
                Toast.makeText(getContext(),R.string.toast_store_successful,Toast.LENGTH_SHORT).show();

            }else{
                Toast.makeText(getContext(),R.string.toast_store_failed,Toast.LENGTH_SHORT).show();
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
            //TODO need error checking
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
            //SimpleDateFormat dateFormat = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
            //return dateFormat.format(date);
        }


    }
}
