package com.mcd.hkyj.dashcam;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mcd.hkyj.dashcamshared.SettingsStoreValues;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


public class CamRecording extends BaseActivity implements SensorEventListener {

    //Starter Tutorial: http://sandyandroidtutorials.blogspot.co.uk/2013/05/android-video-capture-tutorial.html
    SettingsStoreValues _storeValues;

    private String mySdDirectory = ""; //overwrite oncreate

    private Camera myCamera;
    private MyCameraSurfaceView myCameraSurfaceView;
    private MediaRecorder mediaRecorder;
    public static int orientation;
    Button myButton;
    SurfaceHolder surfaceHolder;
    boolean recording;

    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private long lastUpdate;
    private float last_x, last_y, last_z;
    TextView _overallG;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The users settings
        _storeValues = new SettingsStoreValues(getApplicationContext(), getString(R.string.z_settings_db_name));

        setupDirectory();

        recording = false;

        setContentView(R.layout.activity_cam_recording);

        //Get Camera for preview
        myCamera = getCameraInstance();

        if(myCamera == null){
            Toast.makeText(CamRecording.this,
                    "Fail to get Camera",
                    Toast.LENGTH_LONG).show();
        }

        myCameraSurfaceView = new MyCameraSurfaceView(this, myCamera,this);
        FrameLayout myCameraPreview = (FrameLayout)findViewById(R.id.videoview);
        myCameraPreview.addView(myCameraSurfaceView);

        myButton = (Button)findViewById(R.id.mybutton);
        myButton.setOnClickListener(myButtonOnClickListener);

        //sensor stuff
        if(_storeValues.isStopOnShockEnabled){
            sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
            sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }


    Button.OnClickListener myButtonOnClickListener
            = new Button.OnClickListener(){

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub

            try{
                if(recording){
                    stopRecordingAndRelease();
                }else{
                    startRecording();
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }};


    public void startRecording() {

        try {
            //Release Camera before MediaRecorder start
            releaseCamera();

            if(!prepareMediaRecorder()){
                Toast.makeText(CamRecording.this,
                        "Fail in prepareMediaRecorder()!\n - Ended -",
                        Toast.LENGTH_LONG).show();
                finish();
            }
            mediaRecorder.start();
            recording = true;
            myButton.setText(getString(R.string.video_recording_stop));
            myButton.setBackgroundResource(R.drawable.square_button);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void stopRecordingAndRelease(){
        try {
            // stop recording and release camera
            mediaRecorder.stop();  // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object

            myButton.setText(getString(R.string.video_recording_start));
            myButton.setBackgroundResource(R.drawable.round_button);
            recording = false;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private Camera getCameraInstance(){
        // TODO Auto-generated method stub
        Camera c = null;
        try {
            c = Camera.open(0); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }


    private boolean prepareMediaRecorder(){

        clearOldFiles();

        myCamera = getCameraInstance();
        setCameraDisplayOrientation(this,0,myCamera);

        mediaRecorder = new MediaRecorder();

        mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
                        || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                    Log.v("VIDEOCAPTURE", "Maximum Duration Reached");

                    try {
                        stopRecordingAndRelease();
                        startRecording();
                    } catch (Exception e) {
                        myButton.setText("REC");
                        recording = false;
                        e.printStackTrace();
                    }
                }
            }
        });

        myCamera.unlock();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            myCamera.enableShutterSound(false);
        }
        else{
            AudioManager mgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
        }

        mediaRecorder.setCamera(myCamera);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        String fileNameToSave = getFileToSaveName();
        mediaRecorder.setOutputFile(fileNameToSave);
        //mediaRecorder.setOutputFile("/sdcard/myvideo1.mp4");

        int maxDur = _storeValues.saveRecordingTimeInSeconds;
        int maxFileSize = _storeValues.maxSpaceToUseOnSdCardInBytes;

        mediaRecorder.setMaxDuration(maxDur * 1000); // Set max duration 60 sec.
        mediaRecorder.setMaxFileSize(maxFileSize); // Set max file size 50M - 50000000

        mediaRecorder.setPreviewDisplay(myCameraSurfaceView.getHolder().getSurface());
        mediaRecorder.setOrientationHint(CamRecording.orientation);
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        }
        return true;

    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseMediaRecorder(){
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = new MediaRecorder();
            myCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (myCamera != null){
            myCamera.release();        // release the camera for other applications
            myCamera = null;
        }
    }

    public class MyCameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback{

        private SurfaceHolder mHolder;
        private Camera mCamera;
        private Activity mActivity;

        public MyCameraSurfaceView(Context context, Camera camera,Activity activity) {
            super(context);
            mCamera = camera;
            mActivity=activity;
            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
        {
            try {
                setCameraDisplayOrientation(mActivity,0,mCamera);
                previewCamera();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void previewCamera()
        {
            try
            {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            }
            catch(Exception e)
            {
                //Log.d(APP_CLASS, "Cannot start preview", e);
            }
        }


        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // TODO Auto-generated method stub
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // TODO Auto-generated method stub

        }


    }
    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {

        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();

        android.hardware.Camera.getCameraInfo(cameraId, info);

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        CamRecording.orientation=result;
        camera.setDisplayOrientation(result);
    }



    //region "File Handling"


    /**
     * main method for setting up where to save data to.
     */
    public void setupDirectory(){
        //make sure we have a directory to save to.
        //if(_storeValues.isSaveOnSdCard){
            mySdDirectory = Environment.getExternalStorageDirectory() + "/" + getString(R.string.app_name) + "/";
        ///}else{
         //   mySdDirectory = getFilesDir() + "/" + getString(R.string.app_name) + "/";
       // }




        createDirectoryForThisApp();
        File f = new File(mySdDirectory);

        //settings INTERNAL/EXTERNAL STORAGE

        if(!f.exists()){
            if(!_storeValues.isSaveOnSdCard){
                _storeValues.isSaveOnSdCard = true;
                _storeValues.Save();
            }

            createDirectoryForThisApp();
        }
    }


    /**
     * main method for setting the file name for the video.
     * @return
     */
    private String getFileToSaveName(){
            return  mySdDirectory + getFileName_CustomFormat() + ".mp4";
    }


    private String getFileName_CustomFormat() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }


    private void createDirectoryForThisApp(){

        try {
            File mySdLocation = new File(mySdDirectory);
            mySdLocation.mkdirs();
        }catch (Exception ex){
            ex.printStackTrace();
        }

    }


    /*
    * Looks in the save directory and deletes files if we are over the save limit.
    * */
    protected void clearOldFiles(){

        try{
            long dirSize = getFileSize(new File(mySdDirectory));

            long maxdirectorysize = _storeValues.maxSpaceToUseOnSdCardInBytes;

            if(dirSize > maxdirectorysize){

                File[] currentFiles = getOrderedListOfFiles();

                for(int i=0; i < currentFiles.length; i++){

                    if(dirSize < maxdirectorysize){
                        break;
                    }

                    try{
                        //get file size here, remove it from the filesize and remove it.
                        File oldest = currentFiles[i];

                        long thisFileSize = getFileSize(oldest);

                        dirSize = dirSize - thisFileSize;

                        oldest.delete();
                    }catch(Exception e){
                        e.printStackTrace();
                    }

                }


            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }


    private long getFileSize(final File file) {
        if(file==null||!file.exists())
            return 0;
        if(!file.isDirectory())
            return file.length();
        final List<File> dirs=new LinkedList<File>();
        dirs.add(file);
        long result=0;
        while(!dirs.isEmpty())
        {
            final File dir=dirs.remove(0);
            if(!dir.exists())
                continue;
            final File[] listFiles=dir.listFiles();
            if(listFiles==null||listFiles.length==0)
                continue;
            for(final File child : listFiles)
            {
                result+=child.length();
                if(child.isDirectory())
                    dirs.add(child);
            }
        }
        return result;
    }

    /*
    * Gets a list of files in date order
    * */
    private File[] getOrderedListOfFiles(){

        try{
            File directory = new File(mySdDirectory);

            File[] files = directory.listFiles();

            Arrays.sort(files, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                }
            });

            return  files;
        }catch (Exception ex){
            ex.printStackTrace();
        }

        return new File[0];
    }

    //endregion

//region "Sensor Activity"

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

        if(!_storeValues.isStopOnShockEnabled)
            return;

        if(mySensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 500) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                double overall = Math.abs(x + y + z - last_x - last_y - last_z) / 9.8;

                DecimalFormat df = new DecimalFormat("#.00");

                //_overallG.setText("G" + df.format(overall) + "G");

                last_x = x;
                last_y = y;
                last_z = z;

                if(recording && overall > _storeValues.shockLevel){
                    stopRecordingAndRelease();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //endregion


}
