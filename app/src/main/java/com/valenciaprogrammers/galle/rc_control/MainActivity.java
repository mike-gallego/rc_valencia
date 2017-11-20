package com.valenciaprogrammers.galle.rc_control;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;

import static java.lang.Float.parseFloat;
import android.webkit.WebSettings.ZoomDensity;

public class MainActivity extends AppCompatActivity implements SensorEventListener{


    Handler h = new Handler();
    int delay = 200; //15 seconds
    Runnable runnable;


    private Sensor mySensor;
    private SensorManager sensorManager;

    private Thread thread;
    public static boolean going = false;
    private int appRunning = 1;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();

    private final static String IP_ADDRESS = "http://192.168.43.244:8081/";
    WebView webView;

    Button forward;
    Button backward;
    Button exit;
    Button talk; // for now we are going to disable talk

    ArrayList<Float> speedLeft = new ArrayList<Float>();
    ArrayList<Float> speedRight = new ArrayList<Float>();
    float tempLeft;
    float tempRight;
    private static float turnLeftMultiplierForward;
    private static float turnRightMultiplierForward;

    private static float turnRightMultiplierBackward;
    private static float turnLeftMultiplierBackward;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appRunning = 1;
        myRef.child("running").setValue(appRunning);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mySensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);



        webView = (WebView) findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        webView.loadUrl(IP_ADDRESS);


        forward = (Button) findViewById(R.id.forwardButton);
        backward = (Button) findViewById(R.id.backwardButton);
        exit = (Button) findViewById(R.id.stopButton);
        talk = (Button) findViewById(R.id.talkButton);
        talk.setAlpha(.5f);
        talk.setClickable(false);







    }




    @Override
    protected void onResume() {
        super.onResume();

        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                runnable = this;


                if (!going) {
                    speedLeft.add(1500f);
                    speedRight.add(1500f);
                    myRef.child("speedLeft").setValue(speedLeft);
                    myRef.child("speedRight").setValue(speedRight);


                }

                if (speedLeft.size() > 50) {
                    tempLeft = speedLeft.get(speedLeft.size() - 1);
                    tempRight = speedRight.get(speedRight.size() - 1);
                    speedLeft.clear();
                    speedRight.clear();
                    speedLeft.add(tempLeft);
                    speedRight.add(tempRight);

                }
                h.postDelayed(runnable, delay);
            }
        }, delay);



        forward.setOnTouchListener(new RepeatListener(400, 100, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (going) {

                    speedRight.add(1409.5f * turnRightMultiplierForward);
                    speedLeft.add(1444.5f * turnLeftMultiplierForward);
                    myRef.child("speedLeft").setValue(speedLeft);
                    myRef.child("speedRight").setValue(speedRight);
                }
            }


        }));


        backward.setOnTouchListener(new RepeatListener(400, 100, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (going) {

                    speedRight.add(1625f * turnRightMultiplierBackward);
                    speedLeft.add(1625f * turnLeftMultiplierBackward);
                    myRef.child("speedLeft").setValue(speedLeft);
                    myRef.child("speedRight").setValue(speedRight);
                }
            }


        }));

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appRunning = 0;
                myRef.child("running").setValue(appRunning);
                System.exit(0);
            }
        });


    }

    @Override
    protected void onPause() {
        h.removeCallbacks(runnable);
        super.onPause();


    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.out.println(event.values[1]);

            if (event.values[1] > 0.7f && event.values[1] < 3.5f) {
                turnLeftMultiplierForward = 0.98f;
                turnRightMultiplierForward = 1.05f;
                turnLeftMultiplierBackward = 1.08f;
                turnRightMultiplierBackward = 1f;
            } else if (event.values[1] >= 3.5f && event.values[1] < 7f) {
                turnLeftMultiplierForward = 0.955f;
                turnRightMultiplierForward = 1.05f;
                turnLeftMultiplierBackward = 1.10f;
                turnRightMultiplierBackward = 1f;
            } else if (event.values[1] >= 7f) {
                turnLeftMultiplierForward = 0.80f;
                turnRightMultiplierForward = 1.117f;
                turnLeftMultiplierBackward = 1.17f;
                turnRightMultiplierBackward = 0.935f;
            } else if (event.values[1] < -0.7f && event.values[1] > -3.5f) {
                turnLeftMultiplierForward = 1.05f;
                turnRightMultiplierForward = 0.98f;
                turnLeftMultiplierBackward = 1f;
                turnRightMultiplierBackward = 1.08f;
            } else if (event.values[1] <= -3.5f && event.values[1] > -7f) {
                turnLeftMultiplierForward = 1.05f;
                turnRightMultiplierForward = 0.955f;
                turnLeftMultiplierBackward = 1f;
                turnRightMultiplierBackward = 1.10f;
            } else if (event.values[1] <= -7f) {
                turnLeftMultiplierForward = 1.117f;
                turnRightMultiplierForward = 0.80f;
                turnRightMultiplierBackward = 1.17f;
                turnLeftMultiplierBackward = 0.935f;
            } else if (event.values[1] <= 0.7f && event.values[1] >= -0.7f) {
                turnRightMultiplierForward = 1f;
                turnLeftMultiplierForward = 1f;
                turnRightMultiplierBackward = 1f;
                turnLeftMultiplierBackward = 1f;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myRef.child("running").setValue(0);
    }


}
