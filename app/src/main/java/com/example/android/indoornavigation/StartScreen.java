package com.example.android.indoornavigation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class StartScreen extends AppCompatActivity implements SensorEventListener {
    String fileName = "dump.txt";
    double distance = 0;
    double stepFactor = 0.43;
    double height = 0;
    double stepThreshold = 101.0;
    int stepTaken = 0;
    private SensorManager mSensorManager;
    private Sensor mSensoraccelerometer;
    private Sensor mSensormagnetic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File file;
        // read the input height from the user

        setContentView(R.layout.activity_start_screen);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //mSensoraccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //mSensorManager.registerListener(this, mSensoraccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        // delete the file if it is present for the latest readings
        if (isExternalStorageWritable()) {

            if (isStoragePermissionGranted()) {

                file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName);
                if (file.exists()) {
                    file.delete();
                }

            }
        }


    }

    /**
     * This function is called when the start button is pressed
     */
    public void startNavigating(View view) {
        String s = "hello_x";
        EditText textEdit = (EditText) findViewById(R.id.height_input);
        String stringHeight = textEdit.getText().toString();
        height = Double.parseDouble(stringHeight);
        height = height / 100;
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensoraccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensoraccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        // start the magnetic field listener
        mSensormagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, mSensormagnetic, SensorManager.SENSOR_DELAY_NORMAL);

    }

    public void onSensorChanged(SensorEvent event) {
        /**here we will calculate the change in accelaretion using the accelrometer and will print in the text box*/
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            getMagnetometer(event);

        }
    }

    public void getMagnetometer(SensorEvent event) {
        float[] geomag = {0, 0, 0};
        float[] linear_acceleration = {0, 0, 0};
        float[] gravity = {(float) 0, (float) 0, (float) 9.8};
        final float alpha = (float) 0.8;
        geomag[0] = event.values[0];
        geomag[1] = event.values[1];
        geomag[2] = event.values[2];
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
            gravity[0] = event.values[0];
            gravity[1] = event.values[1];
            gravity[2] = event.values[2];
        }
        float[] rotationMatrix = new float[9];
        float[] vals = {0, 0, 0};
        SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomag);
        SensorManager.getOrientation(rotationMatrix, vals);
        double Heading = vals[0] * (180 / Math.PI);
        //logic to convert the heading which is supplied to the directions
        //heading means how much we are differing from the magnetic north
        // so 0 reading means we are facing the actual magnetic north
        //90 means we are facing east

        String text = Double.toString(Heading);
        TextView directionTextView = (TextView) findViewById(R.id.direction_Textview);
        directionTextView.setText(text);
        directionTextView = (TextView) findViewById(R.id.pitch_Textview);
        text = Double.toString(vals[1]);// for updating the pitch
        directionTextView.setText(text);
        directionTextView = (TextView) findViewById(R.id.Roll_Textview);
        text = Double.toString(vals[2]);// for updating the pitch
        directionTextView.setText(text);

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void getAccelerometer(SensorEvent event) {
        float[] linear_acceleration = {0, 0, 0};
        float[] gravity = {(float) 0, (float) 0, (float) 9.8};
        final float alpha = (float) 0.8;
        /**isolate the force of gravity from the readingds using the low pass filter*/
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0]; //- gravity[0];
        linear_acceleration[1] = event.values[1];// - gravity[1];
        linear_acceleration[2] = event.values[2];// - gravity[2];
        /**now the display function which will print the linear accelertion in the respective fields*/
        //displayAcceleration(linear_acceleration[0], linear_acceleration[1], linear_acceleration[2]);
        float totalAcceloeration = ((linear_acceleration[0] * linear_acceleration[0]) + (linear_acceleration[1] * linear_acceleration[1]) + (linear_acceleration[2] * linear_acceleration[2]));
        //dumpTheAccelerationToFile(totalAcceloeration);
        // now the logic to find when the step is taken
        /**When ever the total acceleration value exceeds a constant threshold
         * which is calculated previously then it will be assumed that
         * the step is taken and the calculate distance function will be called
         * which will update the total distance travelled along with the total steps taken
         */
        if (totalAcceloeration >= stepThreshold) {
            // then we assume that the step has been taken and we will the update the distance travelled
            calculateDistanceTravelled();
            // update the number of steps taken
            stepTaken++;
            TextView steptaken = (TextView) findViewById(R.id.total_steps_textview);
            String step = Float.toString(stepTaken);
            steptaken.setText(step);

        }
    }

    public void dumpTheAccelerationToFile(float acc) {

        String content = Float.toString(acc);
        content = content + "\n";
        FileOutputStream outputStream;
        File file;
        if (isExternalStorageWritable()) {

            if (isStoragePermissionGranted()) {
                try {
                    file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName);
                    // this part ofthe code will check if the file already present if not then it will create the file and then we will write the input
                    FileWriter filewriter;
                    if (!file.exists()) {
                        file.createNewFile();
                        /**outputStream = new FileOutputStream(file);
                         outputStream.write(content.getBytes());
                         outputStream.close();*/
                        filewriter = new FileWriter(file, true);
                        filewriter.append(content);
                        filewriter.flush();
                        filewriter.close();
                    } else {
                        /**outputStream = new FileOutputStream(file);
                         outputStream.write(content.getBytes());
                         outputStream.close();*/
                        filewriter = new FileWriter(file, true);
                        filewriter.append(content);
                        filewriter.flush();
                        filewriter.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public File getTextStorageDirectory(String appName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), appName);
        if (!file.mkdirs()) {

        }
        return file;
    }


    // to check if the permission is granted by the user
    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                //Log.v(TAG,"Permission is granted");
                return true;
            } else {

                // Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            //Log.v(TAG,"Permission is granted");
            return true;
        }

    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Toast.makeText(this, "External SD card mounted", Toast.LENGTH_LONG).show();
            return true;
        }
        Toast.makeText(this, "External SD card not mounted", Toast.LENGTH_LONG).show();
        return false;
    }


    /**
     * public void displayAcceleration(float x, float y, float z) {
     * TextView xTextView = (TextView) findViewById(R.id.x_acceleration_textview);
     * String x_acceleration = Float.toString(x);
     * xTextView.setText(x_acceleration);
     * TextView yTextView = (TextView) findViewById(R.id.y_acceleration_textview);
     * String y_acceleration = Float.toString(y);
     * yTextView.setText(y_acceleration);
     * TextView zTextView = (TextView) findViewById(R.id.z_acceleration_textview);
     * String z_acceleration = Float.toString(z);
     * zTextView.setText(z_acceleration);
     * }
     */

    public void stopNavigating(View view) {
        String s = "hello_y";
        /**TextView yTextView = (TextView)findViewById(R.id.y_acceleration_textview);
         yTextView.setText(s);
         */
        //onPause();
        mSensorManager.unregisterListener(this);

    }

    /**
     * protected void onResume() {
     * super.onResume();
     * mSensorManager.registerListener(this, mSensoraccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
     * }
     */

    protected void onPause() {
        super.onPause();
        //mSensorManager.unregisterListener(this);
    }

    public void calculateDistanceTravelled() {
        distance = distance + stepFactor * height;
        TextView zTextView = (TextView) findViewById(R.id.total_distance_textview);
        String dist = Double.toString(distance);
        zTextView.setText(dist);
    }

}
