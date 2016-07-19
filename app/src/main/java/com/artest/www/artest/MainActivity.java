package com.artest.www.artest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    LocationManager locationManager;
    Button updateAltitudeButton;
    TextView altitudeValue;
    double currentAltitude;
    double pitch;
    double newAltitude;
    double changeInAltitude;
    double thetaSin;
    float[] aValues = new float[3];
    float[] mValues = new float[3];
    HorizonView horizonView;
    SensorManager sensorManager;

    SurfaceView cameraPreview;
    SurfaceHolder previewHolder;
    Camera camera;
    boolean inPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inPreview = false;
        cameraPreview = (SurfaceView) findViewById(R.id.cameraPreview);
        previewHolder = cameraPreview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        altitudeValue = (TextView) findViewById(R.id.altitudeValue);
        updateAltitudeButton = (Button) findViewById(R.id.altitudeUpdateButton);
        updateAltitudeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                updateAltitude();
            }
        });
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 2,
                locationListener);
        horizonView = (HorizonView) this.findViewById(R.id.horizonView);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        updateOrientation(new float[]{0, 0, 0});
    }

    private void updateOrientation(float[] values) {
        if (horizonView != null) {
            horizonView.setBearing(values[0]);
            horizonView.setPitch(values[1]);
            horizonView.setRoll(-values[2]);
            horizonView.invalidate();
        }
    }

    private float[] calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];
        float[] outR = new float[9];
        SensorManager.getRotationMatrix(R, null, aValues, mValues);
        SensorManager.remapCoordinateSystem(R,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                outR);
        SensorManager.getOrientation(outR, values);
        values[0] = (float) Math.toDegrees(values[0]);
        values[1] = (float) Math.toDegrees(values[1]);
        values[2] = (float) Math.toDegrees(values[2]);
        pitch = values[1];
        return values;
    }

    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {

            currentAltitude = location.getAltitude();

        }

        public void onProviderDisabled(String arg0) {

        }

        public void onProviderEnabled(String arg0) {
//Not Used

        }

        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
            //Not Used
        }

    };

    public void updateAltitude() {

        int time = 300;
        float speed = 4.5f;
        double distanceMoved = (speed * time) * 0.3048;

        if (pitch != 0 && currentAltitude != 0) {
            thetaSin = Math.sin(pitch);
            changeInAltitude = thetaSin * distanceMoved;
            newAltitude = currentAltitude + changeInAltitude;
            altitudeValue.setText(String.valueOf(newAltitude));
        } else {

            altitudeValue.setText("Try Again");
        }
    }

    private final SensorEventListener sensorEventListener = new
            SensorEventListener() {
                public void onSensorChanged(SensorEvent event) {
                    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                        aValues = event.values;
                    if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                        mValues = event.values;
                    updateOrientation(calculateOrientation());
                }

                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            };


    private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {

                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;
                    if (newArea > resultArea) {

                        result = size;
                    }
                }

            }
        }
        return (result);
    }


    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                camera.setPreviewDisplay(previewHolder);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = getBestPreviewSize(width, height, parameters);

            if (size != null) {

                parameters.setPreviewSize(size.width, size.height);
                camera.setParameters(parameters);
                camera.startPreview();
                inPreview = true;
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }


    };

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(sensorEventListener, sensorManager
                .getDefaultSensor(SensorManager.SENSOR_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);
        camera=Camera.open();
    }
    @Override
    public void onPause() {
        if (inPreview) {
            camera.stopPreview();
        }
        sensorManager.unregisterListener(sensorEventListener);
        camera.release();
        camera=null;
        inPreview=false;
        super.onPause();
    }

}

