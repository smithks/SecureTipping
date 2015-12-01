package com.example.keegan.securetipping;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.keegan.securetipping.data.HistoryContract.HistoryEntry;
import com.example.keegan.securetipping.data.HistoryDbHelper;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Fragment displayed in history section.
 * Created by Keegan on 10/30/2015.
 */
public class HistoryFragment extends Fragment {

    private SimpleCursorAdapter adapter;
    private ListView listView;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    private DecimalFormat decimalFormat;
    private SQLiteDatabase db;
    private Context mContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.history_fragment, container, false);
        db = HistoryDbHelper.getInstance(getContext()).getReadableDatabase();
        listView = (ListView)rootView.findViewById(R.id.history_listview);
        dateFormat = (SimpleDateFormat)SimpleDateFormat.getDateInstance();  //TODO do not show year if current year?
        timeFormat = (SimpleDateFormat)SimpleDateFormat.getTimeInstance(DateFormat.SHORT);
        decimalFormat = new DecimalFormat("#0.00");
        refreshHistory();
        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        if (db==null){
            db = HistoryDbHelper.getInstance(getContext()).getReadableDatabase();
        }
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        db.close();
        mContext = null;
    }

    /**
     * Refreshes the listView in the history fragment with data from the history database.
     */
    public void refreshHistory(){
        if (db==null){
            db = HistoryDbHelper.getInstance(getContext()).getReadableDatabase();
        }
        new FetchHistoryEntries(getActivity()).execute();
    }

    /**
     * This method will parse the date string value to display in the UI
     * @param sDate raw string from database
     * @return formatted date string
     */
    public String parseDate(String sDate){
        SimpleDateFormat format = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
        Date newDate = format.parse(sDate,new ParsePosition(0));
        return format.format(newDate);
    }

    /**
     * Fetches history entries and populates list view from SimpleCursorAdapter.
     */
    private class FetchHistoryEntries extends AsyncTask<Void,Void,Cursor>{

        Context context;

        public FetchHistoryEntries(Context context){
            this.context = context;
        }

        @Override
        protected Cursor doInBackground(Void... params) {
            //TODO modify query to get restricted results, drop boxes for date range/price/etc.. send as params to asynctask
            String[] column = new String[] {HistoryEntry._ID,HistoryEntry.COLUMN_DATE,HistoryEntry.COLUMN_EACH_PAYS};
            String orderBy = HistoryEntry.COLUMN_DATE +" DESC";
            return db.query(HistoryEntry.TABLE_NAME,column,null,null,null,null,orderBy,null);
        }

        /**
         * Populates listItem views with data from cursor through setting adapter.
         * @param result
         */
        @Override
        protected void onPostExecute(final Cursor result){
            //if (result == null || context == null)
                //return;
            String[] fromColumns = new String[] {HistoryEntry._ID, HistoryEntry.COLUMN_DATE, HistoryEntry.COLUMN_EACH_PAYS};
            int[] toViews = new int[]{R.id.clear_button,R.id.date_layout,R.id.paid_textView};
            adapter = new SimpleCursorAdapter(context,R.layout.history_listview_item,result,fromColumns,toViews,SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            //Set custom handling of views through viewBinder
            adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

                /**
                 * Custom handling of textviews being populated from adapter
                 * @param view The view corresponding to this cursor entry
                 * @param cursor The cursor
                 * @param columnIndex Current column index
                 * @return true if this method was used, false otherwise
                 */
                @Override
                public boolean setViewValue(View view, final Cursor cursor, int columnIndex) {
                    if (columnIndex == cursor.getColumnIndex(HistoryEntry.COLUMN_DATE)){ //Format date using two text views
                        TextView date = (TextView) view.findViewById(R.id.date_textView);
                        TextView time = (TextView) view.findViewById(R.id.time_textView);
                        String dateAndTime = cursor.getString(cursor.getColumnIndex(HistoryEntry.COLUMN_DATE));
                        try {
                            Date fullDate = new Date(Long.parseLong(dateAndTime));
                            date.setText(dateFormat.format(fullDate));
                            time.setText(timeFormat.format(fullDate));

                        } catch (Exception e){
                            Log.e("Date format exception", e.getMessage());
                        }
                        return true;
                    }else if (columnIndex == cursor.getColumnIndex(HistoryEntry.COLUMN_EACH_PAYS)){ //Format paid total
                        TextView paid = (TextView) view;
                        String paidStr = cursor.getString(cursor.getColumnIndex(HistoryEntry.COLUMN_EACH_PAYS));
                        paidStr = "$"+decimalFormat.format(Double.parseDouble(paidStr));
                        paid.setText(paidStr);
                        return true;
                    }else if (columnIndex == cursor.getColumnIndex(HistoryEntry._ID)){
                        ImageButton deleteButton = (ImageButton) view;
                        deleteButton.setOnClickListener(new DeleteButtonListener(cursor.getString(cursor.getColumnIndex(HistoryEntry._ID))));
                        return true;
                    }
                    return false;
                }
            });
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(getActivity(), DetailActivity.class);
                    intent.putExtra("HISTORY_ENTRY_ITEM_ID", id);
                    startActivity(intent);
                }
            });
        }

    }

    /**
     * Click listener for delete buttons used in the list viewer.
     */
    private class DeleteButtonListener implements View.OnClickListener{

        String mEntryID;

        public DeleteButtonListener(String id){
            mEntryID = id;
        }
        @Override

        public void onClick(View v) {
            final AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
            alert.setTitle(getResources().getString(R.string.alert_dialog_delete_title));
            alert.setMessage(getResources().getString(R.string.alert_dialog_delete_message));
            alert.setIcon(R.drawable.ic_warning_black_24dp);
            alert.setPositiveButton(getResources().getString(R.string.alert_dialog_delete_positive), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    new DeleteHistoryEntry().execute(mEntryID);
                }
            });
            alert.setNegativeButton(getResources().getString(R.string.alert_dialog_delete_negative), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            alert.show();
        }
    }

    /**
     * Async task that handles deleting an entry in the history table. Called
     * by clicking the delete button from an item in the listView.
     */
    private class DeleteHistoryEntry extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... params) {
            String where = "_id = " +params[0];
            db.delete(HistoryEntry.TABLE_NAME,where,null);
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            refreshHistory();
            Toast.makeText(getContext(),getResources().getString(R.string.toast_delete_successful),Toast.LENGTH_SHORT).show();
        }


    }
}
