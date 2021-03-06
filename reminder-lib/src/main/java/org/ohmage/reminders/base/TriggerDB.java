/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.reminders.base;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

/*
 * The database to store all triggers in the system. Each row 
 * is as follows:
 * (id, trigger type, trigger desc, action desc, notif desc, 
 * runtime desc)
 * 
 * where each of the above expect the id is a String. 
 * 
 *  - trigger type: String to uniquely identify the type of trigger
 *  - trigger desc: The description of the trigger itself
 *  - action desc: The action to be taken when the trigger goes off
 *  - notif desc: The manner in which the notification is to be done
 *                when the trigger goes off
 *  - run time desc: A collection of run time info related to the trigger  
 */
public class TriggerDB {

    private static final String TAG = "TriggerFramework";

    private static final String DATABASE_NAME = "trigger_framework";
    private static final int DATABASE_VERSION = 3;

    /* Table name */
    private static final String TABLE_TRIGGERS = "triggers";

    /* Columns */
    public static final String KEY_ID = "_id";
    public static final String KEY_UUID = "uuid";
    public static final String KEY_CAMPAIGN_URN = "campaign_urn";
    public static final String KEY_CAMPAIGN_NAME = "campaign_name";
    public static final String KEY_TRIG_TYPE = "trigger_type";
    public static final String KEY_TRIG_DESCRIPT = "trig_descript";
    public static final String KEY_TRIG_ACTION_DESCRIPT = "trig_action_descript";
    public static final String KEY_NOTIF_DESCRIPT = "notif_descript";
    public static final String KEY_RUNTIME_DESCRIPT = "runtime_descript";

    private final Context mContext;
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    public TriggerDB(Context context) {
        this.mContext = context;
    }

    /* Open the database */
    public boolean open() {
        Log.v(TAG, "DB: open");


        mDbHelper = new DatabaseHelper(mContext);

        try {
            mDb = mDbHelper.getWritableDatabase();
        } catch (SQLException e) {
            Log.e(TAG, "Error opening trigger db", e);
            return false;
        }
        return true;
    }

    /* Close the database */
    public void close() {
        Log.v(TAG, "DB: close");

        if (mDbHelper != null) {
            mDbHelper.close();
        }
    }

    /*
     * Add a new trigger to the db
     */
    public long addTrigger(String uuid, String campaignUrn, String campaignName, String trigType,
                           String trigDescript,
                           String trigActDesc,
                           String notifDescript,
                           String rtDescript) {

        Log.v(TAG, "DB: addTrigger(" + campaignUrn +
                ", " + trigType +
                ", " + trigDescript +
                ", " + trigActDesc +
                ", " + notifDescript +
                ", " + rtDescript + ")");

        ContentValues values = new ContentValues();
        values.put(KEY_UUID, uuid);
        values.put(KEY_CAMPAIGN_URN, campaignUrn);
        values.put(KEY_CAMPAIGN_NAME, campaignName);
        values.put(KEY_TRIG_TYPE, trigType);
        values.put(KEY_TRIG_DESCRIPT, trigDescript);
        values.put(KEY_TRIG_ACTION_DESCRIPT, trigActDesc);
        values.put(KEY_NOTIF_DESCRIPT, notifDescript);
        values.put(KEY_RUNTIME_DESCRIPT, rtDescript);

        return mDb.insertWithOnConflict(TABLE_TRIGGERS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    /*
     * Get the row corresponding to a trigger id
     */
    public Cursor getTrigger(int trigId) {
        Log.v(TAG, "DB: getTrigger(" + trigId + ")");

        return mDb.query(TABLE_TRIGGERS, null,
                KEY_ID + "=?", new String[]{String.valueOf(trigId)},
                null, null, null);
    }

    /*
     * Get all the triggers for a survey for a campaign
     */
    public Cursor getSurveyTriggers(String campaignUrn, String surveyTitle) {
        return mDb.query(TABLE_TRIGGERS, null,
                KEY_CAMPAIGN_URN + "=? AND " + KEY_TRIG_ACTION_DESCRIPT + " LIKE " + DatabaseUtils.sqlEscapeString("%" + surveyTitle + "%"),
                new String[]{campaignUrn},
                null, null, null);
    }

    /*
     * Get all the triggers corresponding to a type for a campaign, or for all triggers of a type
     * if campaignUrn is null, or for all triggers if trigType is also null
     */
    public Cursor getTriggers(String campaignUrn, String trigType) {
        Log.v(TAG, "DB: getTriggers(" + trigType + ")");

        StringBuilder selectBuilder = new StringBuilder();
        ArrayList<String> selectArgs = new ArrayList<String>();
        if (campaignUrn != null) {
            selectBuilder.append(KEY_CAMPAIGN_URN + "=?");
            selectArgs.add(campaignUrn);
        }

        if (trigType != null) {
            if (selectBuilder.length() != 0) {
                selectBuilder.append(" AND ");
            }
            selectBuilder.append(KEY_TRIG_TYPE + "=?");
            selectArgs.add(trigType);
        }

        return mDb.query(TABLE_TRIGGERS, null, selectBuilder.toString(),
                selectArgs.toArray(new String[]{}), null, null, null);
    }

    /*
     * Get all triggers in the system for a campaign
     */
    public Cursor getAllTriggers(String campaignUrn) {
        return getTriggers(campaignUrn, null);
    }

    /*
     * Get all triggers in the system
     */
    public Cursor getAllTriggers() {
        return mDb.query(TABLE_TRIGGERS, null, null, null, null, null, null);
    }

    public ArrayList<Campaign> getAllCampaigns() {
        Cursor c = mDb.query(TABLE_TRIGGERS, new String[]{KEY_CAMPAIGN_URN, KEY_CAMPAIGN_NAME}, null, null,
                KEY_CAMPAIGN_URN, null, null);
        ArrayList<Campaign> ret = new ArrayList<Campaign>(c.getCount());
        int i = 0;
        while (c.moveToNext()) {
            Campaign campaign = new Campaign();
            campaign.urn = c.getString(0);
            campaign.name = c.getString(1);
            ret.add(campaign);
        }
        c.close();
        return ret;
    }

//    /*
//     * Get all the triggers corresponding to a type
//     */
//    public Cursor getTriggers(String trigType) {
//        Log.v(DEBUG_TAG, "DB: getTriggers(" + trigType + ")");
//
//        return mDb.query(TABLE_TRIGGERS, null,
//                         KEY_TRIG_TYPE + "=?",
//                         new String[] {String.valueOf(trigType)},
//                         null, null, null);
//    }
//
//    /*
//     * Get all triggers in the system
//     */
//    public Cursor getAllTriggers() {
//        Log.v(DEBUG_TAG, "DB: getAllTriggers");
//
//        return mDb.query(TABLE_TRIGGERS, null, null,
//                null, null, null, null);
//    }

    /*
     * Get the notification description for a trigger
     */
    public String getNotifDescription(int trigId) {
        Log.v(TAG, "DB: getNotifDescription(" + trigId + ")");

        Cursor c = mDb.query(TABLE_TRIGGERS, new String[]{KEY_NOTIF_DESCRIPT},
                KEY_ID + "=?", new String[]{String.valueOf(trigId)},
                null, null, null);

        String notifDesc = null;
        if (c.moveToFirst()) {
            notifDesc = c.getString(
                    c.getColumnIndexOrThrow(KEY_NOTIF_DESCRIPT));
        }
        c.close();
        return notifDesc;
    }

    /*
     * Get the type of a trigger
     */
    public String getTriggerType(int trigId) {
        Log.v(TAG, "DB: getTriggerType(" + trigId + ")");

        Cursor c = mDb.query(TABLE_TRIGGERS, new String[]{KEY_TRIG_TYPE},
                KEY_ID + "=?", new String[]{String.valueOf(trigId)},
                null, null, null);

        String trigType = null;
        if (c.moveToFirst()) {
            trigType = c.getString(
                    c.getColumnIndexOrThrow(KEY_TRIG_TYPE));
        }
        c.close();
        return trigType;
    }

    public static class Campaign {
        public String urn;
        public String name;
    }

    /*
     * Get the campaignUrn of a trigger
     */
    public Campaign getCampaignInfo(int trigId) {
        Log.v(TAG, "DB: getCampaignUrn(" + trigId + ")");

        Cursor c = mDb.query(TABLE_TRIGGERS, new String[]{KEY_CAMPAIGN_URN, KEY_CAMPAIGN_NAME},
                KEY_ID + "=?", new String[]{String.valueOf(trigId)},
                null, null, null);

        Campaign campaign = null;
        if (c.moveToFirst()) {
            campaign = new Campaign();
            campaign.urn = c.getString(c.getColumnIndexOrThrow(KEY_CAMPAIGN_URN));
            campaign.name = c.getString(c.getColumnIndexOrThrow(KEY_CAMPAIGN_NAME));
        }
        c.close();
        return campaign;
    }

    /*
     * Get the description of a trigger
     */
    public String getTriggerDescription(int trigId) {
        Log.v(TAG, "DB: getTriggerDescription(" + trigId + ")");

        Cursor c = mDb.query(TABLE_TRIGGERS, new String[]{KEY_TRIG_DESCRIPT},
                KEY_ID + "=?", new String[]{String.valueOf(trigId)},
                null, null, null);

        String trigDesc = null;
        if (c.moveToFirst()) {
            trigDesc = c.getString(
                    c.getColumnIndexOrThrow(KEY_TRIG_DESCRIPT));
        }
        c.close();
        return trigDesc;
    }

    /*
     * Get the action description of a trigger
     */
    public String getActionDescription(int trigId) {
        Log.v(TAG, "DB: getActionDescription(" + trigId + ")");

        Cursor c = mDb.query(TABLE_TRIGGERS, new String[]{KEY_TRIG_ACTION_DESCRIPT},
                KEY_ID + "=?", new String[]{String.valueOf(trigId)},
                null, null, null);

        String actDesc = null;
        if (c.moveToFirst()) {
            actDesc = c.getString(
                    c.getColumnIndexOrThrow(KEY_TRIG_ACTION_DESCRIPT));
        }
        c.close();
        return actDesc;
    }

    /*
     * Get the run time description of a trigger
     */
    public String getRunTimeDescription(int trigId) {
        Log.v(TAG, "DB: getRunTimeDescription(" + trigId + ")");

        Cursor c = mDb.query(TABLE_TRIGGERS, new String[]{KEY_RUNTIME_DESCRIPT},
                KEY_ID + "=?", new String[]{String.valueOf(trigId)},
                null, null, null);

        String rtDesc = null;
        if (c.moveToFirst()) {
            rtDesc = c.getString(
                    c.getColumnIndexOrThrow(KEY_RUNTIME_DESCRIPT));
        }
        c.close();
        return rtDesc;
    }

    /*
     * Update the trigger description of an existing trigger
     */
    public boolean updateTriggerDescription(int trigId, String newDesc) {
        Log.v(TAG, "DB: updateTriggerDescription(" + trigId +
                ", " + newDesc + ")");

        ContentValues values = new ContentValues();
        values.put(KEY_TRIG_DESCRIPT, newDesc);

        if (mDb.update(TABLE_TRIGGERS, values,
                KEY_ID + "=?",
                new String[]{String.valueOf(trigId)}) != 1) {
            return false;
        }

        return true;
    }

    /*
     * Update the action description of an existing trigger
     */
    public boolean updateActionDescription(int trigId, String newDesc) {
        Log.v(TAG, "DB: updateActionDescription(" + trigId +
                ", " + newDesc + ")");

        ContentValues values = new ContentValues();
        values.put(KEY_TRIG_ACTION_DESCRIPT, newDesc);

        if (mDb.update(TABLE_TRIGGERS, values,
                KEY_ID + "=?",
                new String[]{String.valueOf(trigId)}) != 1) {
            return false;
        }

        return true;
    }

    /*
     * Update the run time description of an existing trigger
     */
    public boolean updateRunTimeDescription(int trigId, String newDesc) {
        Log.v(TAG, "DB: updateRunTimeDescription(" + trigId +
                ", " + newDesc + ")");

        ContentValues values = new ContentValues();
        values.put(KEY_RUNTIME_DESCRIPT, newDesc);

        if (mDb.update(TABLE_TRIGGERS, values,
                KEY_ID + "=?",
                new String[]{String.valueOf(trigId)}) != 1) {
            return false;
        }

        return true;
    }

//    /*
//     * Update the notification descriptions of all triggers with
//     * a new one
//     */
//    public boolean updateAllNotificationDescriptions(String newDesc) {
//        Log.v(DEBUG_TAG, "DB: updateAllNotificationDescriptions(" + newDesc + ")");
//
//        ContentValues values = new ContentValues();
//        values.put(KEY_NOTIF_DESCRIPT, newDesc);
//
//        mDb.update(TABLE_TRIGGERS, values, null, null);
//        return true;
//    }

    /*
     * Update the notification descriptions of all triggers
     * a new one
     */
    public boolean updateAllNotificationDescriptions(String newDesc) {
        Log.v(TAG, "DB: updateAllNotificationDescriptions(" + newDesc + ")");

        ContentValues values = new ContentValues();
        values.put(KEY_NOTIF_DESCRIPT, newDesc);

        mDb.update(TABLE_TRIGGERS, values, null, null);
        return true;
    }

    /*
     * Delete a specific trigger
     */
    public boolean deleteTrigger(int trigId) {
        Log.v(TAG, "DB: deleteTrigger(" + trigId + ")");

        mDb.delete(TABLE_TRIGGERS, KEY_ID + "=?",
                new String[]{String.valueOf(trigId)});

        return true;
    }


    /* Database helper inner class */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase mDb) {
            Log.v(TAG, "DB: SQLiteOpenHelper.onCreate");

            final String QUERY_CREATE_TRIGGERS_TB =
                    "create table " + TABLE_TRIGGERS + " ("
                            + KEY_ID + " integer primary key autoincrement, "
                            + KEY_UUID + " string unique, "
                            + KEY_CAMPAIGN_URN + " text, "
                            + KEY_CAMPAIGN_NAME + " text, "
                            + KEY_TRIG_TYPE + " text not null, "
                            + KEY_TRIG_DESCRIPT + " text, "
                            + KEY_TRIG_ACTION_DESCRIPT + " text, "
                            + KEY_NOTIF_DESCRIPT + " text, "
                            + KEY_RUNTIME_DESCRIPT + " text)";


            //Create the table
            mDb.execSQL(QUERY_CREATE_TRIGGERS_TB);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIGGERS);
            onCreate(db);
        }
    }
}
