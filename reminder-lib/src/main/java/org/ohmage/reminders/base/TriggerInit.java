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

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import com.google.gson.GsonBuilder;

import org.ohmage.reminders.config.NotifConfig;
import org.ohmage.reminders.notif.NotifDesc;
import org.ohmage.reminders.notif.Notifier;
import org.ohmage.reminders.types.location.LocTrigDesc;
import org.ohmage.reminders.types.location.LocTrigMapsActivity;
import org.ohmage.reminders.types.time.TimeTrigDesc;
import org.ohmage.reminders.utils.TrigPrefManager;

/*
 * Boot listener. Starts all the active triggers. 
 * Also restores the pending notifications if any
 */
public class TriggerInit {

    private static final String TAG = "TriggerFramework";

    public static void initTriggers(Context context, String campaignUrn, String campaignName) {

        Log.v(TAG, "TriggerInit: Initializing triggers for " + campaignUrn);

        TriggerTypeMap trigMap = new TriggerTypeMap();

        TriggerDB db = new TriggerDB(context);
        db.open();

        Cursor c = db.getAllTriggers(campaignUrn);

        if (c.moveToFirst()) {
            do {
                int trigId = c.getInt(
                        c.getColumnIndexOrThrow(TriggerDB.KEY_ID));

                String trigDesc = c.getString(
                        c.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_DESCRIPT));

                String notifDesc = c.getString(
                        c.getColumnIndexOrThrow(TriggerDB.KEY_NOTIF_DESCRIPT));

                String trigType = c.getString(
                        c.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_TYPE));

                String rtDesc = c.getString(
                        c.getColumnIndexOrThrow(TriggerDB.KEY_RUNTIME_DESCRIPT));

                String actDesc = c.getString(
                        c.getColumnIndexOrThrow(TriggerDB.KEY_TRIG_ACTION_DESCRIPT));

                Log.v(TAG, "TriggerInit: Read from db: " + trigId +
                        ", " + trigDesc + ", " + actDesc);

                TriggerBase trig = trigMap.getTrigger(trigType);
                if (trig != null) {

                    //Start the trigger
                    TriggerActionDesc aDesc = new TriggerActionDesc();
                    //Start only if it has a positive number of surveys
                    if (aDesc.loadString(actDesc) && aDesc.getCount() > 0) {
                        Log.v(TAG, "TriggerInit: Starting trigger: " + trigId +
                                ", " + trigDesc);

                        trig.startTrigger(context, trigId, trigDesc);
                    }

                    //Restore the notification states for this trigger
                    TriggerRunTimeDesc desc = new TriggerRunTimeDesc();
                    if (desc.loadString(rtDesc) && desc.hasTriggerTimeStamp()) {
                        Log.v(TAG, "TriggerInit: Restoring notifications for " + trigId);

                        Notifier.restorePastNotificationStates(context, trigId, notifDesc,
                                desc.getTriggerTimeStamp());
                    }
                }

            } while (c.moveToNext());
        }

        c.close();
        db.close();

        //Refresh the notification display
        Notifier.refreshNotification(context, true);
    }


    /*
     * Resets all triggers, settings and preferences to its default.
     * Removes all triggers from the database after stopping them.
     */
    public static boolean resetTriggersAndSettings(Context context) {
        Log.v(TAG, "TriggerInit: Resetting all triggers");

        TriggerTypeMap trigMap = new TriggerTypeMap();

        TriggerDB db = new TriggerDB(context);
        db.open();

        Cursor c = db.getAllTriggers();

        //Stop and delete all triggers
        if (c.moveToFirst()) {
            do {
                int trigId = c.getInt(
                        c.getColumnIndexOrThrow(TriggerDB.KEY_ID));

                TriggerBase trig = trigMap.getTrigger(
                        db.getTriggerType(trigId));
                if (trig != null) {
                    //delete the trigger
                    trig.deleteTrigger(context, trigId);
                }
            } while (c.moveToNext());
        }

        c.close();
        db.close();

        //Refresh the notification display
        Notifier.refreshNotification(context, true);

        return true;
    }

    public static boolean resetAllTriggersAndSettings(Context context) {
        Log.v(TAG, "TriggerInit: Resetting all triggers");

        resetTriggersAndSettings(context);

        TrigPrefManager.clearPreferenceFiles(context);

        TriggerTypeMap trigMap = new TriggerTypeMap();

        //Reset the settings of all trigger types
        for (TriggerBase trig : trigMap.getAllTriggers()) {

            if (trig.hasSettings()) {
                trig.resetSettings(context);
            }
        }

        SharedPreferences pref = context.getSharedPreferences(LocTrigMapsActivity.TOOL_TIP_PREF_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(LocTrigMapsActivity.KEY_TOOL_TIP_DO_NT_SHOW, false);
        editor.commit();

        NotifDesc.setGlobalDesc(context, NotifConfig.defaultConfig);

        return true;
    }

    public static void injectDeserializers(GsonBuilder builder) {
        builder.registerTypeAdapter(TimeTrigDesc.class, new TimeTrigDesc.TimeTrigDescDeserializer());
        builder.registerTypeAdapter(LocTrigDesc.class, new LocTrigDesc.LocTrigDescDeserializer());
        builder.registerTypeAdapter(TriggerBase.class, new TriggerBase.TriggerDeserializer());
    }
}
