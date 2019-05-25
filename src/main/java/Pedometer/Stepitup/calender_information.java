package Pedometer.Stepitup;

import java.util.Calendar;

public abstract class calender_information {


    public static long getToday() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }


    public static long getTomorrow() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 1);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, 1);
        return cal.getTimeInMillis();
    }
}
