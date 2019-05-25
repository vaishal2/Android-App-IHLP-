package Pedometer.Stepitup;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

import stepitup.BuildConfig;
import stepitup.R;

/**
 * Background service which keeps the step-sensor listener alive to always get
 * the number of steps since boot.
 * <p/>
 * This service won't be needed any more if there is a way to read the
 * step-value without waiting for a sensor event
 */
public class UpdateSensor extends Service implements SensorEventListener {
    private static int steps;

    private static int lastSaveSteps;

    private static long lastSaveTime;

    private final static long SAVE_OFFSET_TIME = AlarmManager.INTERVAL_HOUR;

    private final static int SAVE_OFFSET_STEPS = 500;

    private final static int NOTIFICATION_ID = 1;

    private final static long MICROSECONDS_IN_ONE_MINUTE = 60000000;

    public final static String ACTION_PAUSE = "pause";

    public final static String ACTION_UPDATE_NOTIFICATION = "updateNotificationState";

    @Override
    public void onAccuracyChanged(final Sensor sensor, int accuracy) {
        if (BuildConfig.DEBUG)
            UpdateLog.log(sensor.getName() + " accuracy changed value: " + accuracy);
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (event.values[0] > Integer.MAX_VALUE) {
            if (BuildConfig.DEBUG) UpdateLog.log("probably value is not real: " + event.values[0]);
        } else {
            steps = (int) event.values[0];
            updateIfNecessary();
        }
    }

    private void updateIfNecessary() {
        if (steps > lastSaveSteps + SAVE_OFFSET_STEPS ||
                (steps > 0 && System.currentTimeMillis() > lastSaveTime + SAVE_OFFSET_TIME)) {
            if (BuildConfig.DEBUG) UpdateLog.log(
                    "total saving steps: steps=" + steps + " lastSavedStep=" + lastSaveSteps +
                            " lastSavedTimeStep=" + new Date(lastSaveTime));
            DataBase db = DataBase.getInstance(this);
            if (db.getSteps(calender_information.getToday()) == Integer.MIN_VALUE) {
                int pauseDifference = steps -
                        getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                                .getInt("pauseTheCount", steps);
                db.NewDayinsert(calender_information.getToday(), steps - pauseDifference);
                if (pauseDifference > 0) {
                    // update pauseCount for the new day
                    getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                            .putInt("pauseTheCount", steps).commit();

                }
            }
            db.storeCurrentSteps(steps);
            db.close();
            lastSaveSteps = steps;
            lastSaveTime = System.currentTimeMillis();
            updateNotification();
        }
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent != null && ACTION_PAUSE.equals(intent.getStringExtra("action"))) {
            if (BuildConfig.DEBUG)
                UpdateLog.log("onStartCommand action: " + intent.getStringExtra("action"));
            if (steps == 0) {
                DataBase db = DataBase.getInstance(this);
                steps = db.getCurrentSteps();
                db.close();
            }
            SharedPreferences prefs = getSharedPreferences("pedometer", Context.MODE_PRIVATE);
            //while clicking on pause, the steps are counted here and added with the total steps after it resumes
            if (prefs.contains("pauseCount")) {
                int difference = steps -
                        prefs.getInt("pauseCount", steps);
                DataBase db = DataBase.getInstance(this);
                db.addStepsInLastEntry(-difference);
                db.close();
                prefs.edit().remove("pauseCount").commit();
                updateNotification();
            } else { // pause counting
                // cancel restart
                ((AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE))
                        .cancel(PendingIntent.getService(getApplicationContext(), 2,
                                new Intent(this, UpdateSensor.class),
                                PendingIntent.FLAG_UPDATE_CURRENT));
                prefs.edit().putInt("pauseCount", steps).commit();
                updateNotification();
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        if (intent != null && intent.getBooleanExtra(ACTION_UPDATE_NOTIFICATION, false)) {
            updateNotification();
        } else {
            updateIfNecessary();
        }

        // restart service every hour to save the current step count
        ((AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE))
                .set(AlarmManager.RTC, Math.min(calender_information.getTomorrow(),
                        System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR), PendingIntent
                        .getService(getApplicationContext(), 2,
                                new Intent(this, UpdateSensor.class),
                                PendingIntent.FLAG_UPDATE_CURRENT));

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) UpdateLog.log("UpdateSensor onCreate");
        reRegisterSensor();
        updateNotification();
    }

    @Override
    public void onTaskRemoved(final Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (BuildConfig.DEBUG) UpdateLog.log("sensor service task removed");
        // Restart service in 500 ms
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                .set(AlarmManager.RTC, System.currentTimeMillis() + 500, PendingIntent
                        .getService(this, 3, new Intent(this, UpdateSensor.class), 0));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (BuildConfig.DEBUG) UpdateLog.log("UpdateSensor onDestroy");
        try {
            SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
            sm.unregisterListener(this);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) UpdateLog.log(e);
            e.printStackTrace();
        }
    }

    private void updateNotification() {
        if (BuildConfig.DEBUG) UpdateLog.log("UpdateSensor updateNotificationState");
        SharedPreferences prefs = getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (prefs.getBoolean("notification", true)) {
            int goal = prefs.getInt("goal", 10000);
            DataBase db = DataBase.getInstance(this);
            int today_offset = db.getSteps(calender_information.getToday());
            if (steps == 0)
                steps = db.getCurrentSteps(); // use saved value if we haven't anything better
            db.close();
            Notification.Builder notificationBuilder = new Notification.Builder(this);
            if (steps > 0) {
                if (today_offset == Integer.MIN_VALUE) today_offset = -steps;
                notificationBuilder.setProgress(goal, today_offset + steps, false).setContentText(
                        today_offset + steps >= goal ? getString(R.string.goal_reached_notification,
                                NumberFormat.getInstance(Locale.getDefault())
                                        .format((today_offset + steps))) :
                                getString(R.string.notification_text,
                                        NumberFormat.getInstance(Locale.getDefault())
                                                .format((goal - today_offset - steps))));
            } else { // still no step value?
                notificationBuilder
                        .setContentText(getString(R.string.your_progress_will_be_shown_here_soon));
            }
            boolean isPaused = prefs.contains("pauseCount");
            notificationBuilder.setPriority(Notification.PRIORITY_MIN).setShowWhen(false)
                    .setContentTitle(isPaused ? getString(R.string.ispaused) :
                            getString(R.string.notification_title)).setContentIntent(PendingIntent
                    .getActivity(this, 0, new Intent(this, Addfragment.class),
                            PendingIntent.FLAG_UPDATE_CURRENT))
                    .setSmallIcon(R.drawable.ic_notification)
                    .addAction(isPaused ? R.drawable.ic_resume : R.drawable.ic_pause,
                            isPaused ? getString(R.string.resume) : getString(R.string.pause),
                            PendingIntent.getService(this, 4, new Intent(this, UpdateSensor.class)
                                            .putExtra("action", ACTION_PAUSE),
                                    PendingIntent.FLAG_UPDATE_CURRENT)).setOngoing(true);
            nm.notify(NOTIFICATION_ID, notificationBuilder.build());
        } else {
            nm.cancel(NOTIFICATION_ID);
        }
    }

    private void reRegisterSensor() {
        if (BuildConfig.DEBUG) UpdateLog.log("re-register sensor listener");
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        try {
            sm.unregisterListener(this);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) UpdateLog.log(e);
            e.printStackTrace();
        }

        if (BuildConfig.DEBUG) {
            UpdateLog.log("step sensors: " + sm.getSensorList(Sensor.TYPE_STEP_COUNTER).size());
            if (sm.getSensorList(Sensor.TYPE_STEP_COUNTER).size() < 1) return; // emulator
            UpdateLog.log("default: " + sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER).getName());
        }

        // enable batching with delay of max 5 min
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                SensorManager.SENSOR_DELAY_NORMAL, (int) (5 * MICROSECONDS_IN_ONE_MINUTE));
    }
}
