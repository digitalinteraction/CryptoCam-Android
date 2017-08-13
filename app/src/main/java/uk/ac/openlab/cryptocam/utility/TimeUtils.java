package uk.ac.openlab.cryptocam.utility;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Kyle Montague on 05/08/2017.
 */

public class TimeUtils extends android.text.format.DateUtils {

    public static boolean isWithinWeek(final long millis) {
        return System.currentTimeMillis() - millis <= (WEEK_IN_MILLIS - DAY_IN_MILLIS);
    }

    public static boolean isWithinYear(final long millis) {
        return System.currentTimeMillis() - millis <= YEAR_IN_MILLIS;
    }


    public final static String todayFormat = "h:mm:ss a";
    public final static String weekFormat = "EEEE, h:mm a";
    public final static String yearFormat = "DD MMM, h:mm a";
    public final static String oldFormat = "DD MMM yyyy";
    public final static String fullFormat = "yyyy-MM-DD hh:mm:ss";


    public static String getRelativeTimeSpanString(final long millis) {
        String prefix = "";
        final String dateFormat;
        if (isToday(millis)) {
            prefix = "Today, ";
            dateFormat = todayFormat;
        } else if (isWithinWeek(millis)) {
            dateFormat = weekFormat;
        } else if (isWithinYear(millis)) {
            dateFormat = yearFormat;
        } else {
            dateFormat = oldFormat;
        }
        return String.format(Locale.ENGLISH,"%s%s",prefix,formatDate(millis, dateFormat));
    }

    /**
     * To format the Current time
     *
     * @param dateFormat
     * @return formated current time value.
     */
    public static String formatCurrentTime(String dateFormat) {
        return formatDate(System.currentTimeMillis(), dateFormat);
    }

    /**
     * To format the time in a readable given format.
     *
     * @param milliSeconds - Time in milliseconds
     * @param dateFormat - Format date
     * @return formated time value.
     */
    public static String formatDate(long milliSeconds, String dateFormat) {
        if (dateFormat == null) {
            dateFormat = fullFormat;
        }
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat, Locale.getDefault());



        /**
         * we are creating the calendar instance and setting the time value.
         */
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);

        return formatter.format(calendar.getTime());
    }
}