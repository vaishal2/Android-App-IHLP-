package Pedometer.Stepitup;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import stepitup.R;

abstract class SplitCountSteps {

    private static boolean split_active;

    public static Dialog ShowDialog(final Context c, final int totalSteps) {
        final Dialog dialog = new Dialog(c);
        dialog.setTitle(R.string.split_count);
        dialog.setContentView(R.layout.dialog_split);

        final SharedPreferences prefs =
                c.getSharedPreferences("pedometer", Context.MODE_MULTI_PROCESS);
        int split_steps = prefs.getInt("split_steps", totalSteps);
        long split_date = prefs.getLong("split_date", -1);

        ((TextView) dialog.findViewById(R.id.steps))
                .setText(MainScreen.formatter.format(totalSteps - split_steps));
        float stepsize = prefs.getFloat("Default_stepsize_value", Settings.DEFAULT_STEP_SIZE);
        float distance = (totalSteps - split_steps) * stepsize;
        if (prefs.getString("Default_stepsize_unit", Settings.DEFAULT_STEP_UNIT).equals("cm")) {
            distance /= 100000;
            ((TextView) dialog.findViewById(R.id.distanceunit)).setText("km");
        } else {
            distance /= 5280;
            ((TextView) dialog.findViewById(R.id.distanceunit)).setText("mi");
        }
        ((TextView) dialog.findViewById(R.id.distance))
                .setText(MainScreen.formatter.format(distance));
        ((TextView) dialog.findViewById(R.id.date)).setText(c.getString(R.string.since,
                java.text.DateFormat.getDateTimeInstance().format(split_date)));

        final View start = dialog.findViewById(R.id.started);
        final View stop = dialog.findViewById(R.id.stopped);

        split_active = split_date > 0;

        start.setVisibility(split_active ? View.VISIBLE : View.GONE);
        stop.setVisibility(split_active ? View.GONE : View.VISIBLE);

        final Button startstop = (Button) dialog.findViewById(R.id.start);
        startstop.setText(split_active ? R.string.stop : R.string.start);
        startstop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (!split_active) {
                    prefs.edit().putLong("split_date", System.currentTimeMillis())
                            .putInt("split_steps", totalSteps).commit();
                    split_active = true;
                    dialog.dismiss();
                } else {
                    start.setVisibility(View.GONE);
                    stop.setVisibility(View.VISIBLE);
                    prefs.edit().remove("split_date").remove("split_steps").commit();
                    split_active = false;
                }
                startstop.setText(split_active ? R.string.stop : R.string.start);
            }
        });

        dialog.findViewById(R.id.close).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                dialog.dismiss();
            }
        });

        return dialog;
    }
}
