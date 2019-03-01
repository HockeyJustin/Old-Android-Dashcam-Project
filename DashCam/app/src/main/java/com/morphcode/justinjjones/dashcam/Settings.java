package com.mcd.hkyj.dashcam;

import android.os.Environment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.Toast;

import com.mcd.hkyj.dashcamshared.LocationPreference;
import com.mcd.hkyj.dashcamshared.LocationService;
import com.mcd.hkyj.dashcamshared.SettingsStoreValues;

import java.math.RoundingMode;
import java.text.DecimalFormat;


public class Settings extends BaseActivity {

    private Switch switchShock;
    private SeekBar seekShockLevel;
    private SeekBar seekMaxSpace;
    private SeekBar seekSaveInterval;


    private long maxBytesOnSDCard = -1;

    private TableLayout tbLayout;

    private SettingsStoreValues storeValues;
    private LocationService locationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        storeValues = new SettingsStoreValues(getApplicationContext(), getString(R.string.z_settings_db_name));
        locationService = new LocationService(getApplicationContext());

        switchShock = (Switch)findViewById(R.id.settings_switch_shock);
        seekShockLevel = (SeekBar)findViewById(R.id.settings_seekbar_shock);
        seekMaxSpace = (SeekBar)findViewById(R.id.settings_seekbar_max_space);
        seekSaveInterval = (SeekBar)findViewById(R.id.settings_seekbar_save_interval);

        tbLayout = (TableLayout)findViewById(R.id.settings_shock_details_table);

        maxBytesOnSDCard = getAvailableSpaceOnSdCard();

        initialize();
    }


    private void initialize()
    {
        switchShock.setChecked(storeValues.isStopOnShockEnabled);

        int shockSeekVal =  convertGForceToSeekerValue(storeValues.shockLevel);
        seekShockLevel.setProgress(shockSeekVal);

        int maxSpaceVal = convertMaxSpaceBytesToSeeker(storeValues.maxSpaceToUseOnSdCardInBytes);
        seekMaxSpace.setProgress(maxSpaceVal);

        int maxInterval = convertIntervalToTimeSeekerValue(storeValues.saveRecordingTimeInSeconds);
        seekSaveInterval.setProgress(maxInterval);

        initializeClickEvents();

        //--- Visibilities and other settings
        if(!switchShock.isChecked()){
            tbLayout.setVisibility(View.GONE);
        }

        if(!locationService.isLocationEnabled()){
            storeValues.locationPreference = LocationPreference.NOTALLOWED;
            storeValues.Save();
        }
    }

    private void initializeClickEvents()
    {
        switchShock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                storeValues.isStopOnShockEnabled = isChecked;
                storeValues.Save();

                int vis = isChecked ? View.VISIBLE : View.GONE;
                if (tbLayout != null)
                    tbLayout.setVisibility(vis);
            }
        });


        seekShockLevel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0)
                    progress = 10;

                storeValues.shockLevel = convertSeekerToGForce(progress);
                storeValues.Save();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        seekMaxSpace.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0)
                    progress = 1;

                if (storeValues.maxSpaceToUseOnSdCardInBytes < maxBytesOnSDCard) {
                    storeValues.maxSpaceToUseOnSdCardInBytes = convertSeekerToMaxSpaceBytes(progress);
                    storeValues.Save();
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.settings_validation_sdcardsize) + getSpaceOnSdAsString() + "Gb", Toast.LENGTH_LONG);
                    progress = 1;
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        seekSaveInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (progress < 6)
                    progress = 6;

                storeValues.saveRecordingTimeInSeconds = convertTimeSeekerToInterval(progress);
                storeValues.Save();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }


    private String getSpaceOnSdAsString() {

        long ret = getAvailableSpaceOnSdCard() / 1000000000;
        DecimalFormat df = new DecimalFormat("0.00##");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(ret);
    }


    private long getAvailableSpaceOnSdCard()
    {
        try {
            return Environment.getExternalStorageDirectory().getUsableSpace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }


    private float convertSeekerToGForce(int seekerValue){
        float returnValue = (float)0.5;

        if(seekerValue > 0){
            returnValue = (float)((double)seekerValue * 0.5);
        }

        return returnValue;
    }

    private int convertGForceToSeekerValue(float gForce){
        return (int)((gForce - 0.5) / 0.5);
    }


    private int convertTimeSeekerToInterval(int seekerValue) {
        return  seekerValue * 5;
    }

    private int convertIntervalToTimeSeekerValue(int interval) {
        return interval / 5;
    }


    private int convertMaxSpaceBytesToSeeker(int bytes) {
        if(bytes > 0)
            return bytes / (2* 1000000);
        else
            return 0;
    }

    private int convertSeekerToMaxSpaceBytes(int seekMaxSpaceLevel) {
        if(seekMaxSpaceLevel > 0)
            return seekMaxSpaceLevel * (2* 1000000);
        else
            return 0;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
