package com.example.keegan.securetipping;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.example.keegan.securetipping.data.HistoryContract.HistoryEntry;
import com.example.keegan.securetipping.data.HistoryDbHelper;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Keegan on 11/20/2015.
 */
public class DetailActivity extends AppCompatActivity{

    private SQLiteDatabase mDb;
    private View mParentLayout;
    private long mEntryID;
    private SimpleDateFormat mDateFormat;
    private SimpleDateFormat mTimeFormat;
    private DecimalFormat mDecimalFormat;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_detail_activity);

        mDb = HistoryDbHelper.getInstance(this).getWritableDatabase();
        mParentLayout = findViewById(R.id.entry_detail_layout);
        mDateFormat = (SimpleDateFormat)SimpleDateFormat.getDateInstance();
        mTimeFormat = (SimpleDateFormat)SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
        mDecimalFormat = new DecimalFormat("#0.00");

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            mEntryID = extras.getLong("HISTORY_ENTRY_ITEM_ID");
            new FetchHistoryEntry().execute(mEntryID);
        }
    }

    /**
     * Uses the passed in id for this entry to retreive all relevant values to populate detail fields.
     */
    private class FetchHistoryEntry extends AsyncTask<Long, Void, Cursor>{

        @Override
        protected Cursor doInBackground(Long... params) {
            String[] columns = new String[] {HistoryEntry._ID, HistoryEntry.COLUMN_DATE, HistoryEntry.COLUMN_METHOD, HistoryEntry.COLUMN_BILL,HistoryEntry.COLUMN_TIP_PERCENT,HistoryEntry.COLUMN_TOTAL,HistoryEntry.COLUMN_PEOPLE,HistoryEntry.COLUMN_EACH_PAYS};
            String where = "_id = "+params[0];
            return mDb.query(HistoryEntry.TABLE_NAME,columns,where,null,null,null,null);
        }

        @Override
        protected void onPostExecute(final Cursor result){
            if(result.moveToFirst()){

                String dateAndTime = result.getString(result.getColumnIndex(HistoryEntry.COLUMN_DATE));
                Date fullDate = new Date(Long.parseLong(dateAndTime));
                String date = mDateFormat.format(fullDate);
                String time = mTimeFormat.format(fullDate);


                String paid = "$" + mDecimalFormat.format(Double.parseDouble(result.getString(result.getColumnIndex(HistoryEntry.COLUMN_EACH_PAYS))));

                Double billDouble = Double.parseDouble(result.getString(result.getColumnIndex(HistoryEntry.COLUMN_BILL)));
                String bill = "$" + mDecimalFormat.format(billDouble);

                String percent = result.getString(result.getColumnIndex(HistoryEntry.COLUMN_TIP_PERCENT))+"%";

                Double totalDouble = Double.parseDouble(result.getString(result.getColumnIndex(HistoryEntry.COLUMN_TOTAL)));
                String total = "$" + mDecimalFormat.format(totalDouble);

                Double tipAmountDouble = totalDouble - billDouble;
                String tipAmount = "$" + mDecimalFormat.format(tipAmountDouble);

                String method = result.getString(result.getColumnIndex(HistoryEntry.COLUMN_METHOD));

                String people = result.getString(result.getColumnIndex(HistoryEntry.COLUMN_PEOPLE));

                ((TextView) mParentLayout.findViewById(R.id.detail_date_textView)).setText(date);
                ((TextView) mParentLayout.findViewById(R.id.detail_time_textView)).setText(time);
                ((TextView) mParentLayout.findViewById(R.id.detail_you_paid_textView)).setText(paid);
                ((TextView) mParentLayout.findViewById(R.id.detail_bill_textView)).setText(bill);
                ((TextView) mParentLayout.findViewById(R.id.detail_tip_percent_textView)).setText(percent);
                ((TextView) mParentLayout.findViewById(R.id.detail_tip_amount_textView)).setText(tipAmount);
                ((TextView) mParentLayout.findViewById(R.id.detail_total_textView)).setText(total);
                ((TextView) mParentLayout.findViewById(R.id.detail_method_textView)).setText(method);
                ((TextView) mParentLayout.findViewById(R.id.detail_number_textView)).setText(people);
            }
        }
    }
}
