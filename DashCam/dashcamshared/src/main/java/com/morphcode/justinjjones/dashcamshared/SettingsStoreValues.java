package com.mcd.hkyj.dashcamshared;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by jones on 11/05/2015.
 */
public class SettingsStoreValues extends Activity {

    private String prefStoreName = "";
    private Context context;

    public SettingsStoreValues(Context ctx, String storeName)
    {
        context = context;
        prefStoreName = storeName;
        fill();
    }

    public boolean isSaveOnSdCard;

    public boolean isAppPurchased;

    //TODO: Implement this fully in future versions.
    public LocationPreference locationPreference;

    public boolean isStopOnShockEnabled;
    public float shockLevel;

    public int maxSpaceToUseOnSdCardInBytes;

    public int saveRecordingTimeInSeconds;

    public int numberOfTimesAppUsed;



    private void fill(){

        try {
            SharedPreferences settings = context.getSharedPreferences(prefStoreName, 0);

            isSaveOnSdCard = settings.getBoolean("isSaveOnSdCard", true);

            isAppPurchased = settings.getBoolean("isAppPurchased", false);

            String tmpPref = settings.getString("locationPref", LocationPreference.OFF.toString());
            locationPreference = LocationPreference.valueOf(tmpPref);

            isStopOnShockEnabled = settings.getBoolean("isStopOnShockEnabled", true);

            float gshock = (float)(2.5);
            shockLevel = settings.getFloat("shockLevel", gshock); //TODO: find a suitable value!

            maxSpaceToUseOnSdCardInBytes = settings.getInt("maxSpaceToUseOnSdCardInBytes", 200000000); //TODO: check 500mb suitable!
            saveRecordingTimeInSeconds = settings.getInt("saveRecordingTimeInSeconds", 120);
            numberOfTimesAppUsed = settings.getInt("numberOfTimesAppUsed", 0);
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }


    public void Save()
    {
        try {
            SharedPreferences settings = context.getSharedPreferences(prefStoreName, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("isSaveOnSdCard", isSaveOnSdCard);
            editor.putBoolean("isAppPurchased", isAppPurchased);
            editor.putString("locationPref", locationPreference.toString());
            editor.putBoolean("isStopOnShockEnabled", isStopOnShockEnabled);
            editor.putFloat("shockLevel", shockLevel);
            editor.putInt("maxSpaceToUseOnSdCardInBytes", maxSpaceToUseOnSdCardInBytes);
            editor.putInt("saveRecordingTimeInSeconds", saveRecordingTimeInSeconds);
            editor.putInt("numberOfTimesAppUsed", numberOfTimesAppUsed);
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
