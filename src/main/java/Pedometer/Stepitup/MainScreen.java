package Pedometer.Stepitup;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.BarModel;
import org.eazegraph.lib.models.PieModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import stepitup.BuildConfig;
import stepitup.R;

public class MainScreen extends Fragment implements SensorEventListener {

    private int After_start_Total, After_boot, todayOffset, total_days, goal;

    private PieChart pg;

    private PieModel sG, sC;

    private TextView todaysteps, totalsteps, averagesteps;

    public final static NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());

    private boolean showSteps = true;

    @Override
    public void onCreate(final Bundle stateSavedInstance) {
        super.onCreate(stateSavedInstance);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater li, final ViewGroup vG,
                             final Bundle B) {
        final View view = li.inflate(R.layout.fragment_overview, null);
        todaysteps = (TextView) view.findViewById(R.id.steps);
        totalsteps = (TextView) view.findViewById(R.id.total);
        averagesteps = (TextView) view.findViewById(R.id.average);

        pg = (PieChart) view.findViewById(R.id.graph);

        // slice for the steps taken today
        sC = new PieModel("", 0, Color.parseColor("#00cc81"));
        pg.addPieSlice(sC);

        // slice for the "missing" steps until reaching the goal
        sG = new PieModel("", Settings.DEFAULT_GOAL, Color.parseColor("#ccbb00"));
        pg.addPieSlice(sG);

        pg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                showSteps = !showSteps;
                distanceChangedForSteps();
            }
        });

        pg.setDrawValueInPie(false);
        pg.setUsePieRotation(false);
        pg.startAnimation();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);

        DataBase db = DataBase.getInstance(getActivity());

        if (BuildConfig.DEBUG) db.logTheState();
        // read todays offset
        todayOffset = db.getSteps(calender_information.getToday());

        SharedPreferences prefs =
                getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);

        goal = prefs.getInt("goal", Settings.DEFAULT_GOAL);
        After_boot = db.getCurrentSteps(); // do not use the value from the sharedPreferences
        int pauseDifference = After_boot - prefs.getInt("pauseCount", After_boot);

        // register a sensorlistener to live update the UI if a step is taken
        if (!prefs.contains("pauseCount")) {
            SensorManager sm =
                    (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (sensor == null) {
                new AlertDialog.Builder(getActivity()).setTitle(R.string.no_sensor)
                        .setMessage(R.string.no_sensor_explain)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(final DialogInterface dialogInterface) {
                                getActivity().finish();
                            }
                        }).setNeutralButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).create().show();
            } else {
                sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 0);
            }
        }

        After_boot -= pauseDifference;

        After_start_Total = db.getTotalExcludingToday();
        total_days = db.getDaysExcludToday();

        db.close();

        distanceChangedForSteps();
    }

    /**
     * Call this method if the Fragment should update the "steps"/"km" text in
     * the pie graph as well as the pie and the bars graphs.
     */
    private void distanceChangedForSteps() {//stepsDistanceChanged to distanceChangedForSteps
        if (showSteps) {
            ((TextView) getView().findViewById(R.id.unit)).setText(getString(R.string.steps));
        } else {
            String unit = getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                    .getString("stepsize_unit", Settings.DEFAULT_STEP_UNIT);
            if (unit.equals("cm")) {
                unit = "km";
            } else {
                unit = "mi";
            }
            ((TextView) getView().findViewById(R.id.unit)).setText(unit);
        }

        updateThePie();
        updateBars();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            SensorManager sm =
                    (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            sm.unregisterListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        DataBase db = DataBase.getInstance(getActivity());
        db.storeCurrentSteps(After_boot);
        db.close();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
        MenuItem pause = menu.getItem(0);
        Drawable d;
        if (getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                .contains("pauseCount")) { // currently paused
            pause.setTitle(R.string.resume);
            d = getResources().getDrawable(R.drawable.ic_resume);
        } else {
            pause.setTitle(R.string.pause);
            d = getResources().getDrawable(R.drawable.ic_pause);
        }
        d.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        pause.setIcon(d);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_split_count:
                SplitCountSteps.ShowDialog(getActivity(),
                        After_start_Total + Math.max(todayOffset + After_boot, 0)).show();
                return true;
            case R.id.action_pause:
                SensorManager sm =
                        (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
                Drawable d;
                if (getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                        .contains("pauseCount")) { // currently paused -> now resumed
                    sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                            SensorManager.SENSOR_DELAY_UI, 0);
                    item.setTitle(R.string.pause);
                    d = getResources().getDrawable(R.drawable.ic_pause);
                } else {
                    sm.unregisterListener(this);
                    item.setTitle(R.string.resume);
                    d = getResources().getDrawable(R.drawable.ic_resume);
                }
                d.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                item.setIcon(d);
                getActivity().startService(new Intent(getActivity(), UpdateSensor.class)
                        .putExtra("action", UpdateSensor.ACTION_PAUSE));
                return true;
            default:
                return ((Addfragment) getActivity()).ItemSelectedoptions(item);
        }
    }

    @Override
    public void onAccuracyChanged(final Sensor sensor, int accuracy) {
        // won't happen
    }

    /**
     * Updates the bar graph to show the steps/distance of the last week. Should
     * be called when switching from step count to distance.
     */
    private void updateBars() {
        SimpleDateFormat df = new SimpleDateFormat("E", Locale.getDefault());
        BarChart barChart = (BarChart) getView().findViewById(R.id.bargraph);
        if (barChart.getData().size() > 0) barChart.clearChart();
        int steps;
        float distance, stepsize = Settings.DEFAULT_STEP_SIZE;
        boolean stepsize_cm = true;
        if (!showSteps) {
            // load some more settings if distance is needed
            SharedPreferences prefs =
                    getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
            stepsize = prefs.getFloat("stepsize_value", Settings.DEFAULT_STEP_SIZE);
            stepsize_cm = prefs.getString("stepsize_unit", Settings.DEFAULT_STEP_UNIT)
                    .equals("cm");
        }
        barChart.setShowDecimal(!showSteps); // show decimal in distance view only
        BarModel bm;
        DataBase db = DataBase.getInstance(getActivity());
        List<Pair<Long, Integer>> last = db.getLastEntries(8);
        db.close();
        for (int i = last.size() - 1; i > 0; i--) {
            Pair<Long, Integer> current = last.get(i);
            steps = current.second;
            if (steps > 0) {
                bm = new BarModel(df.format(new Date(current.first)), 0,
                        steps > goal ? Color.parseColor("#ccbb00") : Color.parseColor("#00cc81"));
                if (showSteps) {
                    bm.setValue(steps);
                } else {
                    distance = steps * stepsize;
                    if (stepsize_cm) {
                        distance /= 100000;
                    } else {
                        distance /= 5280;
                    }
                    distance = Math.round(distance * 1000) / 1000f; // 3 decimals
                    bm.setValue(distance);
                }
                barChart.addBar(bm);
            }
        }
        if (barChart.getData().size() > 0) {
            barChart.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View v) {
                    StatisticsInfo.ShowDialog(getActivity(), After_boot).show();
                }
            });
            barChart.startAnimation();
        } else {
            barChart.setVisibility(View.GONE);
        }
    }


    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (BuildConfig.DEBUG)
            UpdateLog.log("UI - sensorChanged | todayOffset: " + todayOffset + " since boot: " +
                    event.values[0]);
        if (event.values[0] > Integer.MAX_VALUE || event.values[0] == 0) {
            return;
        }
        if (todayOffset == Integer.MIN_VALUE) {
            // no values for today
            // we dont know when the reboot was, so set todays steps to 0 by
            // initializing them with -STEPS_SINCE_BOOT
            todayOffset = -(int) event.values[0];
            DataBase db = DataBase.getInstance(getActivity());
            db.NewDayinsert(calender_information.getToday(), (int) event.values[0]);
            db.close();
        }
        After_boot = (int) event.values[0];
        updateThePie();
    }

    /**
     * Updates the pie graph to show todays steps/distance as well as the
     * yesterday and total values. Should be called when switching from step
     * count to distance.
     */
    private void updateThePie() {
        if (BuildConfig.DEBUG) UpdateLog.log("UI - update steps: " + After_boot);
        // todayOffset might still be Integer.MIN_VALUE on first start
        int steps_today = Math.max(todayOffset + After_boot, 0);
        sC.setValue(steps_today);
        if (goal - steps_today > 0) {
            // goal not reached yet
            if (pg.getData().size() == 1) {
                // can happen if the goal value was changed: old goal value was
                // reached but now there are some steps missing for the new goal
                pg.addPieSlice(sG);
            }
            sG.setValue(goal - steps_today);
        } else {
            // goal reached
            pg.clearChart();
            pg.addPieSlice(sC);
        }
        pg.update();
        if (showSteps) {
            todaysteps.setText(formatter.format(steps_today));
            totalsteps.setText(formatter.format(After_start_Total + steps_today));
            averagesteps.setText(formatter.format((After_start_Total + steps_today) / total_days));
        } else {
            // update only every 10 steps when displaying distance
            SharedPreferences prefs =
                    getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
            float stepsize = prefs.getFloat("stepsize_value", Settings.DEFAULT_STEP_SIZE);
            float walked_dist_today = steps_today * stepsize;
            float walked_dist_total = (After_start_Total + steps_today) * stepsize;
            if (prefs.getString("stepsize_unit", Settings.DEFAULT_STEP_UNIT)
                    .equals("cm")) {
                walked_dist_today /= 100000;
                walked_dist_total /= 100000;
            } else {
                walked_dist_today /= 5280;
                walked_dist_total /= 5280;
            }
            todaysteps.setText(formatter.format(walked_dist_today));
            totalsteps.setText(formatter.format(walked_dist_total));
            averagesteps.setText(formatter.format(walked_dist_total / total_days));
        }
    }

}
