package com.example.keegan.securetipping.data;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.keegan.securetipping.data.HistoryContract.HistoryEntry;

import java.util.ArrayList;

/**
 * Provides helper methods for modifying the history database.
 * Created by Keegan on 11/9/2015.
 */
public class HistoryDbHelper extends SQLiteOpenHelper {

    private static HistoryDbHelper instance;

    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "weather.db";

    public static synchronized HistoryDbHelper getInstance(Context context){
        if (instance==null){
            instance = new HistoryDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    public HistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_HISTORY_TABLE="CREATE TABLE " + HistoryEntry.TABLE_NAME + " ( "+
                HistoryEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "+
                HistoryEntry.COLUMN_DATE + " INTEGER NOT NULL, "+
                HistoryEntry.COLUMN_METHOD + " TEXT NOT NULL, "+
                HistoryEntry.COLUMN_BILL+ " REAL NOT NULL, "+
                HistoryEntry.COLUMN_TIP_PERCENT+ " INTEGER NOT NULL, "+
                HistoryEntry.COLUMN_TOTAL+ " REAL NOT NULL, "+
                HistoryEntry.COLUMN_PEOPLE + " INTEGER NOT NULL, "+
                HistoryEntry.COLUMN_EACH_PAYS+ " REAL NOT NULL"+
                " );";

        db.execSQL(SQL_CREATE_HISTORY_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+ HistoryEntry.TABLE_NAME);
        onCreate(db);
    }

    public ArrayList<Cursor> getData(String Query){
        //get writable database
        SQLiteDatabase sqlDB = this.getWritableDatabase();
        String[] columns = new String[] { "mesage" };
        //an array list of cursor to save two cursors one has results from the query
        //other cursor stores error message if any errors are triggered
        ArrayList<Cursor> alc = new ArrayList<Cursor>(2);
        MatrixCursor Cursor2= new MatrixCursor(columns);
        alc.add(null);
        alc.add(null);


        try{
            String maxQuery = Query ;
            //execute the query results will be save in Cursor c
            Cursor c = sqlDB.rawQuery(maxQuery, null);


            //add value to cursor2
            Cursor2.addRow(new Object[] { "Success" });

            alc.set(1,Cursor2);
            if (null != c && c.getCount() > 0) {


                alc.set(0,c);
                c.moveToFirst();

                return alc ;
            }
            return alc;
        } catch(SQLException sqlEx){
            Log.d("printing exception", sqlEx.getMessage());
            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[] { ""+sqlEx.getMessage() });
            alc.set(1,Cursor2);
            return alc;
        } catch(Exception ex){

            Log.d("printing exception", ex.getMessage());

            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[] { ""+ex.getMessage() });
            alc.set(1,Cursor2);
            return alc;
        }


    }
}
