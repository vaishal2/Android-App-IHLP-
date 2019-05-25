package Pedometer.Stepitup;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import stepitup.BuildConfig;

public class DataBase extends SQLiteOpenHelper {

    private final static String DatabaseB_Name = "steps";
    private final static int Database_Version = 2;

    private static DataBase instance;
    private static final AtomicInteger Counter = new AtomicInteger();

    private DataBase(final Context ct) {
        super(ct, DatabaseB_Name, null, Database_Version);
    }

    @Override
    public void onCreate(final SQLiteDatabase sqldb) {
        sqldb.execSQL("CREATE TABLE " + DatabaseB_Name + " (date INTEGER, steps INTEGER)");
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1) {
            // drop PRIMARY KEY constraint
            db.execSQL("CREATE TABLE " + DatabaseB_Name + "2 (date INTEGER, steps INTEGER)");
            db.execSQL("INSERT INTO " + DatabaseB_Name + "2 (date, steps) SELECT date, steps FROM " +
                    DatabaseB_Name);
            db.execSQL("DROP TABLE " + DatabaseB_Name);
            db.execSQL("ALTER TABLE " + DatabaseB_Name + "2 RENAME TO " + DatabaseB_Name + "");
        }
    }

    @Override
    public void close() {
        if (Counter.decrementAndGet() == 0) {
            super.close();
        }
    }

    public static synchronized DataBase getInstance(final Context ct) {
        if (instance == null) {
            instance = new DataBase(ct.getApplicationContext());
        }
        Counter.incrementAndGet();
        return instance;
    }


    /**
     * Query the 'steps' table. Remember to close the cursor!
     *
     * @param col       the colums
     * @param sel    the selection
     * @param selaruments the selction arguments
     * @param groupBy       the group by statement
     * @param having        the having statement
     * @param orderBy       the order by statement
     * @return the cursor
     */
    /**
     * Inserts a new entry in the database, if there is no entry for the given
     * date yet. Steps should be the current number of steps and it's negative
     * value will be used as offset for the new date. Also adds 'steps' steps to
     * the previous day, if there is an entry for that date.
     * <p/>
     * This method does nothing if there is already an entry for 'date' - use
     * {@link #updateSteps} in this case.
     * <p/>
     * To restore data from a backup, use {@link #insertDayFromBackup}
     *
     * @param date  the date in ms since 1970
     * @param steps the current step value to be used as negative offset for the
     *              new day; must be >= 0
     */
    public void NewDayinsert(long date, int steps) {//insertNewDay to NewDayinsert
        getWritableDatabase().beginTransaction();
        try {
            Cursor cursor = getReadableDatabase().query(DatabaseB_Name, new String[]{"date"}, "date = ?",
                    new String[]{String.valueOf(date)}, null, null, null);
            if (cursor.getCount() == 0 && steps >= 0) {

                // add 'steps' to yesterdays count
                addStepsInLastEntry(steps);

                // add today
                ContentValues cv = new ContentValues();
                cv.put("date", date);
                // use the negative steps as offset
                cv.put("steps", -steps);
                getWritableDatabase().insert(DatabaseB_Name, null, cv);
            }
            cursor.close();
            if (BuildConfig.DEBUG) {
                UpdateLog.log("insertDay " + date + " / " + steps);
                logTheState();
            }
            getWritableDatabase().setTransactionSuccessful();
        } finally {
            getWritableDatabase().endTransaction();
        }
    }

    /**
     * Adds the given number of steps to the last entry in the database
     *
     * @param steps the number of steps to add. Must be > 0
     */
    public void addStepsInLastEntry(int steps) {//addToLastEntry to addInLastEntry
        if (steps > 0) {
            getWritableDatabase().execSQL("UPDATE " + DatabaseB_Name + " SET steps = steps + " + steps +
                    " WHERE date = (SELECT MAX(date) FROM " + DatabaseB_Name + ")");
        }
    }

    /**
     * Inserts a new entry in the database, overwriting any existing entry for the given date.
     * Use this method for restoring data from a backup.
     *
     * @param date  the date in ms since 1970
     * @param steps the step value for 'date'; must be >= 0
     * @return true if a new entry was created, false if there was already an
     * entry for 'date' (and it was overwritten)
     */


    /**
     * Writes the current steps database to the log
     */
    public void logTheState() {//LogState to logTheState
        if (BuildConfig.DEBUG) {
            Cursor cursor = getReadableDatabase()
                    .query(DatabaseB_Name, null, null, null, null, null, "date DESC", "5");
            UpdateLog.log(cursor);
            cursor.close();
        }
    }

    /**
     * Get the total of steps taken without today's value
     *
     * @return number of steps taken, ignoring today
     */
    public int getTotalExcludingToday() {//getTotalWithoutToday to getTotalExcludingToday
        Cursor cursor = getReadableDatabase()
                .query(DatabaseB_Name, new String[]{"SUM(steps)"}, "steps > 0 AND date > 0 AND date < ?",
                        new String[]{String.valueOf(calender_information.getToday())}, null, null, null);
        cursor.moveToFirst();
        int re = cursor.getInt(0);
        cursor.close();
        return re;
    }

    /**
     * Get the maximum of steps walked in one day
     *
     * @return the maximum number of steps walked in one day
     */

    /**
     * Get the maximum of steps walked in one day and the date that happend
     *
     * @return a pair containing the date (Date) in millis since 1970 and the
     * step value (Integer)
     */
    public Pair<Date, Integer> getRecordData() {
        Cursor cursor = getReadableDatabase()
                .query(DatabaseB_Name, new String[]{"date, steps"}, "date > 0", null, null, null,
                        "steps DESC", "1");
        cursor.moveToFirst();
        Pair<Date, Integer> p = new Pair<Date, Integer>(new Date(cursor.getLong(0)), cursor.getInt(1));
        cursor.close();
        return p;
    }

    /**
     * Get the number of steps taken for a specific date.
     * <p/>
     * If date is calender_information.getToday(), this method returns the offset which needs to
     * be added to the value returned by getCurrentSteps() to get todays steps.
     *
     * @param date the date in millis since 1970
     * @return the steps taken on this date or Integer.MIN_VALUE if date doesn't
     * exist in the database
     */
    public int getSteps(final long date) {
        Cursor cursor = getReadableDatabase().query(DatabaseB_Name, new String[]{"steps"}, "date = ?",
                new String[]{String.valueOf(date)}, null, null, null);
        cursor.moveToFirst();
        int re;
        if (cursor.getCount() == 0) re = Integer.MIN_VALUE;
        else re = cursor.getInt(0);
        cursor.close();
        return re;
    }

    /**
     * Gets the last num entries in descending order of date (newest first)
     *
     * @param num the number of entries to get
     * @return a list of long,integer pair - the first being the date, the second the number of steps
     */
    public List<Pair<Long, Integer>> getLastEntries(int num) {
        Cursor cursor = getReadableDatabase()
                .query(DatabaseB_Name, new String[]{"date", "steps"}, "date > 0", null, null, null,
                        "date DESC", String.valueOf(num));
        int max = cursor.getCount();
        List<Pair<Long, Integer>> result = new ArrayList<>(max);
        if (cursor.moveToFirst()) {
            do {
                result.add(new Pair<>(cursor.getLong(0), cursor.getInt(1)));
            } while (cursor.moveToNext());
        }
        return result;
    }

    /**
     * Get the number of steps taken between 'start' and 'end' date
     * <p/>
     * Note that todays entry might have a negative value, so take care of that
     * if 'end' >= calender_information.getToday()!
     *
     * @param start start date in ms since 1970 (steps for this date included)
     * @param end   end date in ms since 1970 (steps for this date included)
     * @return the number of steps from 'start' to 'end'. Can be < 0 as todays
     * entry might have negative value
     */
    public int getSteps(final long start, final long end) {
        Cursor cursor = getReadableDatabase()
                .query(DatabaseB_Name, new String[]{"SUM(steps)"}, "date >= ? AND date <= ?",
                        new String[]{String.valueOf(start), String.valueOf(end)}, null, null, null);
        int re;
        if (cursor.getCount() == 0) {
            re = 0;
        } else {
            cursor.moveToFirst();
            re = cursor.getInt(0);
        }
        cursor.close();
        return re;
    }

    /**
     * Removes all entries with negative values.
     * <p/>
     * Only call this directly after boot, otherwise it might remove the current
     * day as the current offset is likely to be negative
     */

    /**
     * Removes invalid entries from the database.
     * <p/>
     * Currently, an invalid input is such with steps >= 200,000
     */


    /**
     * Get the number of 'valid' days (= days with a step value > 0).
     * <p/>
     * The current day is not added to this number.
     *
     * @return the number of days with a step value > 0, return will be >= 0
     */
    public int getDaysExcludingToday() {
        Cursor cursor = getReadableDatabase()
                .query(DatabaseB_Name, new String[]{"COUNT(*)"}, "steps > ? AND date < ? AND date > 0",
                        new String[]{String.valueOf(0), String.valueOf(calender_information.getToday())}, null,
                        null, null);
        cursor.moveToFirst();
        int re = cursor.getInt(0);
        cursor.close();
        return re < 0 ? 0 : re;
    }

    /**
     * Get the number of 'valid' days (= days with a step value > 0).
     * <p/>
     * The current day is also added to this number, even if the value in the
     * database might still be < 0.
     * <p/>
     * It is safe to divide by the return value as this will be at least 1 (and
     * not 0).
     *
     * @return the number of days with a step value > 0, return will be >= 1
     */
    public int getDaysExcludToday() {
        // todays is not counted yet
        int daytill = this.getDaysExcludingToday() + 1;
        return daytill;
    }

    /**
     * Saves the current 'steps since boot' sensor value in the database.
     *
     * @param steps since boot
     */
    public void storeCurrentSteps(int steps) {//saveCurrentSteps to storeCurrentSteps
        ContentValues cv = new ContentValues();
        cv.put("steps", steps);
        if (getWritableDatabase().update(DatabaseB_Name, cv, "date = -1", null) == 0) {
            cv.put("date", -1);
            getWritableDatabase().insert(DatabaseB_Name, null, cv);
        }
        if (BuildConfig.DEBUG) {
            UpdateLog.log("saving steps in db: " + steps);
        }
    }

    /**
     * Reads the latest saved value for the 'steps since boot' sensor value.
     *
     * @return the current number of steps saved in the database or 0 if there
     * is no entry
     */
    public int getCurrentSteps() {
        int re = getSteps(-1);
        return re == Integer.MIN_VALUE ? 0 : re;
    }
}
