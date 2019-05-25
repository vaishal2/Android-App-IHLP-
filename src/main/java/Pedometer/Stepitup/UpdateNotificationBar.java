package Pedometer.Stepitup;

import android.content.Intent;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import stepitup.R;

/**
 * Class for providing a UpdateNotificationBar (https://code.google.com/p/dashclock)
 * extension
 */
public class UpdateNotificationBar extends DashClockExtension {

    @Override
    protected void onUpdateData(int reason) {
        ExtensionData data = new ExtensionData();
        DataBase db = DataBase.getInstance(this);
        int steps = Math.max(db.getCurrentSteps() + db.getSteps(calender_information.getToday()), 0);
        data.visible(true).status(MainScreen.formatter.format(steps))
                .icon(R.drawable.ic_dashclock)
                .clickIntent(new Intent(UpdateNotificationBar.this, Addfragment.class));
        db.close();
        publishUpdate(data);
    }

}
