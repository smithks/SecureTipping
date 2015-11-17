package com.example.keegan.securetipping.data;

import android.provider.BaseColumns;

/**
 * Contract for history entry in the history database.
 * Created by Keegan on 11/9/2015.
 */
public final class HistoryContract {

    public static final class HistoryEntry implements BaseColumns{
        public static final String TABLE_NAME = "history";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_METHOD = "method";
        public static final String COLUMN_BILL = "bill";
        public static final String COLUMN_TIP_PERCENT = "percent";
        public static final String COLUMN_TOTAL = "total";
        public static final String COLUMN_PEOPLE = "people";
        public static final String COLUMN_EACH_PAYS = "each_pays";
    }
}
