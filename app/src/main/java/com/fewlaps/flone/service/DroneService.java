package com.fewlaps.flone.service;

import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.fewlaps.flone.communication.Bluetooth;
import com.fewlaps.flone.communication.Communication;
import com.fewlaps.flone.communication.bean.DroneConnectionStatusChanged;
import com.fewlaps.flone.communication.bean.DroneSensorInformation;
import com.fewlaps.flone.communication.protocol.MultiWii230;
import com.fewlaps.flone.communication.protocol.MultirotorData;
import com.fewlaps.flone.data.Database;
import com.fewlaps.flone.data.bean.Drone;
import com.fewlaps.flone.util.NotificationUtil;

/**
 * This Sercive is the responsable of maintaing a connection with the Drone
 *
 * @author Roc Boronat (roc@fewlaps.com)
 * @date 15/02/2015
 */
public class DroneService extends BaseService {
    private static final int BAUD_RATE = 115200;

    private static final int DELAY_RECONNECT = 2000;

    public Communication communication;
    public MultirotorData protocol;

    public boolean running = false;

    public static String ACTION_CONNECT = "connect";
    public static String ACTION_DISCONNECT = "disconnect";

    private Handler connectTask = new Handler();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("SERVICE", "onStartCommand");

        communication = new Bluetooth(getApplicationContext());
        protocol = new MultiWii230(communication);

        startForeground(NotificationUtil.KEY_FOREGROUND_NOTIFICATION, NotificationUtil.getForegroundServiceNotification(this));

        onEventMainThread(ACTION_CONNECT);

        return START_NOT_STICKY;
    }

    public void onEventMainThread(String action) {
        Log.d("SERVICE", "Action received: " + action);

        if (action.equals(ACTION_CONNECT)) {
            running = true;
            reconnectRunnable.run();
        } else if (action.equals(ACTION_DISCONNECT)) {
            communication.Close();
            running = false;
            stopForeground(true);
            stopSelf();
        }
    }

    public void onEventMainThread(DroneConnectionStatusChanged status) {
        if (status.isConnected()) {
            protocol.SendRequestMSP_ATTITUDE();
        }
    }

    public void onEventMainThread(DroneSensorInformation sensorInformation) {
        protocol.SendRequestMSP_ATTITUDE();
    }

    private final Runnable reconnectRunnable = new Runnable() {
        public void run() {
            if (running) {
                if (!communication.Connected) {
                    Drone selectedDrone = Database.getSelectedDrone(DroneService.this);
                    if (selectedDrone != null) {
                        protocol.Connect(selectedDrone.address, BAUD_RATE, 0);
                    }
                }
                connectTask.postDelayed(reconnectRunnable, DELAY_RECONNECT);
            }
        }
    };
}
