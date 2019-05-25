package Pedometer.Stepitup;

import android.app.Dialog;
import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;

import stepitup.R;

abstract class StatisticsInfo {

    public static Dialog ShowDialog(final Context c, int since_boot) {//ShowDialog method was getDialog
        final Dialog dialog = new Dialog(c);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.statistics);
        dialog.findViewById(R.id.close).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        DataBase db = DataBase.getInstance(c);

        Pair<Date, Integer> record = db.getRecordData();

        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(calender_information.getToday());
        int daysThisMonth = date.get(Calendar.DAY_OF_MONTH);

        date.add(Calendar.DATE, -6);

        int thisWeek = db.getSteps(date.getTimeInMillis(), System.currentTimeMillis()) + since_boot;

        date.setTimeInMillis(calender_information.getToday());
        date.set(Calendar.DAY_OF_MONTH, 1);
        int thisMonth = db.getSteps(date.getTimeInMillis(), System.currentTimeMillis()) + since_boot;

        ((TextView) dialog.findViewById(R.id.record)).setText(
                MainScreen.formatter.format(record.second) + " @ "
                        + java.text.DateFormat.getDateInstance().format(record.first));

        ((TextView) dialog.findViewById(R.id.totalthisweek)).setText(MainScreen.formatter.format(thisWeek));
        ((TextView) dialog.findViewById(R.id.totalthismonth)).setText(MainScreen.formatter.format(thisMonth));

        ((TextView) dialog.findViewById(R.id.averagethisweek)).setText(MainScreen.formatter.format(thisWeek / 7));
        ((TextView) dialog.findViewById(R.id.averagethismonth)).setText(MainScreen.formatter.format(thisMonth / daysThisMonth));

        db.close();

        return dialog;
    }

}
