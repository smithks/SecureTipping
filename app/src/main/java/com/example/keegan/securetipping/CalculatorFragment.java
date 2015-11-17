package com.example.keegan.securetipping;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.keegan.securetipping.data.HistoryDbHelper;
import com.example.keegan.securetipping.data.HistoryContract.HistoryEntry;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
//TODO create custom calculator for edit text fields
//TODO edit button appearance, make purple color?
//TODO change tip & # person to slider
//TODO fix database leaking, close on pause and such
//TODO include tip value in UI, have to write that down
/**
 * Calculator fragment displayed within viewpager.
 * @author Keegan Smith
 * @version  10/31/2015.
 */
public class CalculatorFragment extends Fragment {

    SQLiteDatabase db;

    public static final String APPTAG = "Secure Tipping";
    private final int BLANK_FIELD = -1;
    private String DEFAULT_TIP; //TODO make setting
    private String TIP_METHOD;
    private String[] TIPPING_METHODS;

    private EditText billAmount;
    private EditText tipPercent;
    private EditText totalAmount;
    private EditText numberPeople;
    private EditText eachPays;
    private ImageButton toggleSplit;
    private Button clearButton;
    private Button saveButton;
    private Boolean splitCheckDisplayed; //Denotes if the split check layout is being displayed

    /**
     *
     * @return the rootView, the calculator fragment
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.calculator_fragment, container, false);

        initializeFields(rootView);

        return rootView;
    }

    /**
     * Initializes all member variables and sets listiners.
     * @param rootView the rootView for this fragment
     */
    public void initializeFields(View rootView){
        final LinearLayout splitCheckLayout = (LinearLayout) rootView.findViewById(R.id.split_check_layout);

        billAmount = (EditText) rootView.findViewById(R.id.bill_amount_edit);
        tipPercent = (EditText) rootView.findViewById(R.id.tip_percent_edit);
        totalAmount = (EditText) rootView.findViewById(R.id.total_amount_edit);
        numberPeople = (EditText) rootView.findViewById(R.id.number_people_edit);
        eachPays = (EditText) rootView.findViewById(R.id.each_pays_edit);

        toggleSplit = (ImageButton) rootView.findViewById(R.id.split_toggle_button);
        clearButton = (Button) rootView.findViewById(R.id.clear_button);
        saveButton = (Button) rootView.findViewById(R.id.save_button);

        splitCheckDisplayed = false;
        TIPPING_METHODS = getResources().getStringArray(R.array.tipping_method_array_vaues);
        pullPreferenceValues();

        billAmount.addTextChangedListener(new TextChangeListener(this, billAmount));
        tipPercent.addTextChangedListener(new TextChangeListener(this, tipPercent));
        totalAmount.addTextChangedListener(new TextChangeListener(this, totalAmount));
        numberPeople.addTextChangedListener(new TextChangeListener(this, numberPeople));
        eachPays.setEnabled(false);

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
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTextFields();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //TODO pull method from preferences
                String[] params = {"normal",billAmount.getText().toString(),tipPercent.getText().toString(),totalAmount.getText().toString(),numberPeople.getText().toString(),eachPays.getText().toString()};
                new StoreTransactionTask().execute(params);
            }
        });

        toggleSplit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (splitCheckDisplayed){
                    splitCheckLayout.setVisibility(View.INVISIBLE);
                    toggleSplit.setImageResource(R.drawable.ic_add_circle_black_24dp);
                    splitCheckDisplayed = false;

                }else{
                    splitCheckLayout.setVisibility(View.VISIBLE);
                    toggleSplit.setImageResource(R.drawable.ic_remove_circle_black_24dp);
                    splitCheckDisplayed = true;
                }
            }
        });

        resetTextFields(); //Set fields to default values
    }

    /**
     *Resets edit text
     */
    public void resetTextFields() {
        billAmount.setText("0");
        tipPercent.setText(DEFAULT_TIP);
        totalAmount.setText("0");
        numberPeople.setText("1");
        eachPays.setText("0");
        saveButton.setEnabled(false);
    }

    @Override
    public void onResume(){
        super.onResume();
        pullPreferenceValues();
    }

    /**
     * Pulls current value of preferences
     */
    public void pullPreferenceValues(){
        SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        DEFAULT_TIP = "15"; //TODO Pull preference setting here
        TIP_METHOD = sPref.getString(getString(R.string.pref_method_key),getString(R.string.pref_method_default));
    }

    /**
     * Updates editText fields whenever a key is pressed to make sure all fields display proper value at all times.
     * @param caller The edit text that was modified
     */
    public void updateFields(EditText caller){
        //Set amounts to default -1, checks if field has been deleted
//        double bill = BLANK_FIELD;
//        double tip = BLANK_FIELD;
//        double total = BLANK_FIELD;
//        double people = BLANK_FIELD;
        double bill = 0;
        double tip = 0;
        double total = 0;
        double people = 1;
        DecimalFormat df = new DecimalFormat("#0.00");

        //Collect current values stored in fields
        //TODO don't crash if user enters . first
        if (billAmount.getText().length() > 0){
            bill = Double.parseDouble(billAmount.getText().toString());
        }
        if (tipPercent.getText().length() > 0 ){
            String tipRaw = tipPercent.getText().toString();
            if (tipRaw.length() == 1) //TODO Convert tip amount to percent. Could use work. only works on whole numbers
                tipRaw = ".0"+tipRaw;
            else if (tipRaw.length() == 2) //10-99 percent
                tipRaw = "."+tipRaw;
            else if(tipRaw.length() == 3) //100 percent
                tipRaw = "1";
            tip = Double.parseDouble(tipRaw);
        }
        if (totalAmount.getText().length()>0){
            total = Double.parseDouble(totalAmount.getText().toString());
        }
        if (splitCheckDisplayed)
            if (numberPeople.getText().length() > 0)
                people = Double.parseDouble(numberPeople.getText().toString());
        Log.v(APPTAG, "bill " + bill + " tip " + tip + " total " + total);//+tip+" total"+total);

        //Update fields
        if (caller.getId() == billAmount.getId()){
            total = calculateTotal(bill, tip);
            totalAmount.setText(df.format(total));
            eachPays.setText(df.format(calculateEachPays(total,people)));
        } else if(caller.getId() == tipPercent.getId()){
                total = calculateTotal(bill, tip);
                totalAmount.setText(df.format(total));
                if (splitCheckDisplayed){
                    eachPays.setText(df.format(calculateEachPays(total,people)));
                }
        } else if (caller.getId() == totalAmount.getId()) {
            //TODO update tip percent using calculate percent
        } else if(caller.getId() == numberPeople.getId()){
            if(people > 0){
                eachPays.setText(df.format(calculateEachPays(total,people)));
            }
        }

        //TODO change this to each pays, whatever is being stored into database
        if(total > 0)
            saveButton.setEnabled(true);
        else
            saveButton.setEnabled(false);
    }

    /**
     * Calculates the total field based on the currently selected tipping method.
     * @param bill The current value of the bill
     * @param tipPercent the current value of the tip percent
     * @return the new value of the total
     */
    public double calculateTotal(double bill, double tipPercent){
        double total = 0;
        double tip = 1+tipPercent;
        if(TIP_METHOD.equals(TIPPING_METHODS[0])){//normal
            total = bill * tip;
            Log.v(APPTAG,total+"");
        }else if(TIP_METHOD.equals(TIPPING_METHODS[1])) {//palindrome
            total = bill * tip;
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
            }
        }
        return total;

    }

    /**
     * Calculates the each pays field that denotes how much each person would have to pay to add up to the
     * total.
     * @param total the current value of total
     * @param people the current number of people
     * @return the value each pays
     */
    //TODO round up so the total is met or exceeded not less than
    public double calculateEachPays(double total, double people){
        return total/people;
    }

    /**
     * Calculates the tip percentage field based on the currently selected tipping method.
     * @param bill the current value of the bill
     * @param total the current value of the total
     * @return the new value of the tip percent
     */
    public double calculatePercent(double bill, double total){
        return 0;
    }

    /**
     * Text Change Listener that will listen for any key presses within the editText fields of the calculator.
     * Will dynamically update other fields whenever a change is detected.
     * @author Keegan Smith
     * @version 10/31/2015
     */
    public class TextChangeListener implements TextWatcher{

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
    public class StoreTransactionTask extends AsyncTask<String, Void, Long>{

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
                if (eachPays.getText().length() > 0){
                    if(eachPaysBefore == Double.parseDouble(eachPays.getText().toString())){ //No change, no race condition, disable button so same data cannot be saved
                        saveButton.setEnabled(false);
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
            if (db==null) //Instantiate db if not already done
                db = HistoryDbHelper.getInstance(getContext()).getWritableDatabase();

            eachPaysBefore = Double.parseDouble(params[5]); //Save eachPaysBefore value for comparison once insert is complete
            //TODO need error checking
            ContentValues values = new ContentValues();
            values.put(HistoryEntry.COLUMN_DATE,getTimestamp());
            values.put(HistoryEntry.COLUMN_METHOD,params[0]);
            values.put(HistoryEntry.COLUMN_BILL,params[1]);
            values.put(HistoryEntry.COLUMN_TIP_PERCENT,params[2]);
            values.put(HistoryEntry.COLUMN_TOTAL,params[3]);
            values.put(HistoryEntry.COLUMN_PEOPLE,params[4]);
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
