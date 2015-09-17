package com.burfdevelopment.lejosandroidsample;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lejos.hardware.Audio;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.remote.ev3.RemoteRequestEV3;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;

public class MainActivity extends AppCompatActivity {

    //todo Change to your IP
    private final String ev3IP = "192.168.0.29";

    TextView ipTextView, touchSensorTextView, irSensorTextView;
    Button motorOnButton, motorOffButton;
    Button connectButton, disconnectButton;

    // EV3
    private RegulatedMotor motorA;
    private RemoteRequestEV3 ev3Brick;
    private EV3IRSensor irSensor;
    private EV3TouchSensor touchSensor;
    private Audio audio;

    private SampleProvider irSampler;
    private float[] irSample;

    private SampleProvider touchSampler;
    private float[] touchSample;

    private final ExecutorService threads = Executors.newCachedThreadPool();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        touchSensorTextView = (TextView) findViewById(R.id.touchSensorTextView);
        irSensorTextView = (TextView) findViewById(R.id.irSensorTextView);

        ipTextView = (TextView) findViewById(R.id.ipTextView);
        ipTextView.setText("IP For EV3 : " + ev3IP);

        motorOnButton = (Button) findViewById(R.id.motorOnButton);
        motorOnButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                if (ev3Brick != null) {
                    new Control().execute("motorOn", null);
                }
            }
        });

        motorOffButton = (Button) findViewById(R.id.motorOffButton);
        motorOffButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                if (ev3Brick != null) {
                    new Control().execute("motorOff", null);
                }
            }
        });

        connectButton = (Button) findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                if (ev3Brick == null) {
                    new Control().execute("connectBrick", ev3IP);
                }
            }
        });

        disconnectButton = (Button) findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                if (ev3Brick != null) {
                    new Control().execute("disconnect", null);
                }
            }
        });

    }

    private void motorOn() {
        motorA.forward();
        // you can rotate to a certain degree.
        //motorA.rotate(100);
        Log.i("MOTOR", "MOTOR ON");
    }

    private void motorOff() {
        motorA.stop();
        Log.i("MOTOR", "MOTOR OFF");
    }

    private class Control extends AsyncTask<String, Integer, Long> {

        protected Long doInBackground(String... cmd) {

            if (cmd[0].equals("connectBrick")) {
                try {
                    ev3Brick = new RemoteRequestEV3(cmd[1]);
                    motorA = ev3Brick.createRegulatedMotor("A", 'L');
                    motorA.setSpeed(7200);
                    audio = ev3Brick.getAudio();
                    audio.systemSound(3);
                    ev3Brick.getAudio().systemSound(1);

                    monitorSensors();

                    return 0l;
                } catch (IOException e) {
                    return 1l;
                }

            } else if (cmd[0].equals("disconnect")) {
                disconnect();
                return 0l;
            }

            if (cmd[0].equals("motorOn")) {
                motorOn();

            } else if (cmd[0].equals("motorOff")) {
                motorOff();
            }

            return 0l;
        }

        protected void onPostExecute(Long result) {
            if (result == 1l) {
                Toast.makeText(MainActivity.this, "Could not connect to EV3", Toast.LENGTH_LONG).show();
            } else if (result == 2l) {
                Toast.makeText(MainActivity.this, "Not connected", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void monitorSensors() {

        // Each sensor needs to get regularly sampled
        irSensor = new EV3IRSensor(ev3Brick.getPort("S4"));
        final int IR_PROX = 0;
        irSensor.setCurrentMode(IR_PROX);
        irSampler = irSensor.getMode("Distance");
        irSample = new float[irSampler.sampleSize()];

//        touchSensor = new EV3TouchSensor(ev3Brick.getPort("S1"));
//        touchSampler = touchSensor.getTouchMode();
//        touchSample = new float[touchSampler.sampleSize()];

        threads.submit(new Runnable() {
            @Override
            public void run() {

                while (!threads.isShutdown()) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);

                        irSampler.fetchSample(irSample, 0);
                        //touchSampler.fetchSample(touchSample, 0);

                        runOnUiThread(new Runnable() {
                            public void run() {
                                irSensorTextView.setText("IR Distance: " + irSample[0]);
                                //touchSensorTextView.setText("" + touchSample[0]);
                            }
                        });

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (irSensor != null) {
                    irSensor.close();
                }

                if (motorA != null) {
                    motorA.close();
                    motorA = null;
                }

                if (ev3Brick != null) {
                    ev3Brick.disConnect();
                    ev3Brick = null;
                }

//                if (touchSensor != null) {
//                    touchSensor.close();
//                }
            }
        });
    }

    private void disconnect() {

        audio = ev3Brick.getAudio();
        audio.systemSound(4);
        ev3Brick.getAudio().systemSound(1);

        threads.shutdown();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
