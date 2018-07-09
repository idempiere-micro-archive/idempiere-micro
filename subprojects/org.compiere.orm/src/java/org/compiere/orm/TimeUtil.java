package org.compiere.orm;

import org.idempiere.common.util.Language;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class TimeUtil {
    /**
     * 	Get earliest time of a day (truncate)
     *  @param dayTime day and time
     *  @return day with 00:00
     */
    static public Timestamp getDay (Timestamp dayTime)
    {
        if (dayTime == null)
            return getDay(System.currentTimeMillis());
        return getDay(dayTime.getTime());
    }	//	getDay
    /**
     * 	Get earliest time of a day (truncate)
     *  @param time day and time
     *  @return day with 00:00
     */
    static public Timestamp getDay (long time)
    {
        if (time == 0)
            time = System.currentTimeMillis();
        GregorianCalendar cal = new GregorianCalendar(Language.getLoginLanguage().getLocale());
        cal.setTimeInMillis(time);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Timestamp (cal.getTimeInMillis());
    }	//	getDay

    /**
     * Returns the day border by combining the date part from dateTime and time part form timeSlot.
     * If timeSlot is null, then first milli of the day will be used (if end == false)
     * or last milli of the day (if end == true).
     *
     * @param dateTime
     * @param timeSlot
     * @param end
     * @return
     */
    public static Timestamp getDayBorder(Timestamp dateTime, Timestamp timeSlot, boolean end)
    {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(dateTime.getTime());
        dateTime.setNanos(0);

        if(timeSlot != null)
        {
            timeSlot.setNanos(0);
            GregorianCalendar gcTS = new GregorianCalendar();
            gcTS.setTimeInMillis(timeSlot.getTime());

            gc.set(Calendar.HOUR_OF_DAY, gcTS.get(Calendar.HOUR_OF_DAY));
            gc.set(Calendar.MINUTE, gcTS.get(Calendar.MINUTE));
            gc.set(Calendar.SECOND, gcTS.get(Calendar.SECOND));
            gc.set(Calendar.MILLISECOND, gcTS.get(Calendar.MILLISECOND));
        }
        else if(end)
        {
            gc.set(Calendar.HOUR_OF_DAY, 23);
            gc.set(Calendar.MINUTE, 59);
            gc.set(Calendar.SECOND, 59);
            gc.set(Calendar.MILLISECOND, 999);
        }
        else
        {
            gc.set(Calendar.MILLISECOND, 0);
            gc.set(Calendar.SECOND, 0);
            gc.set(Calendar.MINUTE, 0);
            gc.set(Calendar.HOUR_OF_DAY, 0);
        }
        return new Timestamp(gc.getTimeInMillis());
    }

    /**
     * 	Return Day + offset (truncates)
     * 	@param day Day
     * 	@param offset day offset
     * 	@return Day + offset at 00:00
     */
    static public Timestamp addDays (Timestamp day, int offset)
    {
        if (offset == 0)
        {
            return day;
        }
        if (day == null)
        {
            day = new Timestamp(System.currentTimeMillis());
        }
        //
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (offset == 0)
            return new Timestamp (cal.getTimeInMillis());
        cal.add(Calendar.DAY_OF_YEAR, offset);			//	may have a problem with negative (before 1/1)
        return new Timestamp (cal.getTimeInMillis());
    }	//	addDays


    /**
     * 	Get earliest time of a day (truncate)
     *	@param day day 1..31
     *	@param month month 1..12
     *	@param year year (if two diguts: < 50 is 2000; > 50 is 1900)
     *	@return timestamp ** not too reliable
     */
    static public Timestamp getDay (int year, int month, int day)
    {
        if (year < 50)
            year += 2000;
        else if (year < 100)
            year += 1900;
        if (month < 1 || month > 12)
            throw new IllegalArgumentException("Invalid Month: " + month);
        if (day < 1 || day > 31)
            throw new IllegalArgumentException("Invalid Day: " + month);
        GregorianCalendar cal = new GregorianCalendar (year, month-1, day);
        return new Timestamp (cal.getTimeInMillis());
    }	//	getDay

    /**
     * 	Get Minimum of 2 digits
     *	@param no number
     *	@return String
     */
    private static String get2digits (long no)
    {
        String s = String.valueOf(no);
        if (s.length() > 1)
            return s;
        return "0" + s;
    }	//	get2digits


    /**
     * 	Format Elapsed Time
     *	@param elapsedMS time in ms
     *	@return formatted time string 1'23:59:59.999 - d'hh:mm:ss.xxx
     */
    public static String formatElapsed (long elapsedMS)
    {
        if (elapsedMS == 0)
            return "0";
        StringBuilder sb = new StringBuilder();
        if (elapsedMS < 0)
        {
            elapsedMS = - elapsedMS;
            sb.append("-");
        }
        //
        long miliSeconds = elapsedMS%1000;
        elapsedMS = elapsedMS / 1000;
        long seconds = elapsedMS%60;
        elapsedMS = elapsedMS / 60;
        long minutes = elapsedMS%60;
        elapsedMS = elapsedMS / 60;
        long hours = elapsedMS%24;
        long days = elapsedMS / 24;
        //
        if (days != 0)
            sb.append(days).append("'");
        //	hh
        if (hours != 0)
            sb.append(get2digits(hours)).append(":");
        else if (days != 0)
            sb.append("00:");
        //	mm
        if (minutes != 0)
            sb.append(get2digits(minutes)).append(":");
        else if (hours != 0 || days != 0)
            sb.append("00:");
        //	ss
        sb.append(get2digits(seconds))
            .append(".").append(miliSeconds);
        return sb.toString();
    }	//	formatElapsed

    /**
     * 	Format Elapsed Time until now
     * 	@param start start time
     *	@return formatted time string 1'23:59:59.999
     */
    public static String formatElapsed (Timestamp start)
    {
        if (start == null)
            return "NoStartTime";
        long startTime = start.getTime();
        long endTime = System.currentTimeMillis();
        return formatElapsed(endTime-startTime);
    }	//	formatElapsed



    /**************************************************************************
     * 	Format Elapsed Time
     * 	@param start start time or null for now
     * 	@param end end time or null for now
     * 	@return formatted time string 1'23:59:59.999
     */
    public static String formatElapsed (Timestamp start, Timestamp end)
    {
        long startTime = 0;
        if (start == null)
            startTime = System.currentTimeMillis();
        else
            startTime = start.getTime();
        //
        long endTime = 0;
        if (end == null)
            endTime = System.currentTimeMillis();
        else
            endTime = end.getTime();
        return formatElapsed(endTime-startTime);
    }	//	formatElapsed

    /**
     * 	Is it valid today?
     *	@param validFrom valid from
     *	@param validTo valid to
     *	@return true if walid
     */
    public static boolean isValid (Timestamp validFrom, Timestamp validTo)
    {
        return isValid (validFrom, validTo, new Timestamp (System.currentTimeMillis()));
    }	//	isValid

    /**
     * 	Is it valid on test date
     *	@param validFrom valid from
     *	@param validTo valid to
     *	@param testDate Date
     @return true if walid
     */
    public static boolean isValid (Timestamp validFrom, Timestamp validTo, Timestamp testDate)
    {
        if (testDate == null)
            return true;
        if (validFrom == null && validTo == null)
            return true;
        //	(validFrom)	ok
        if (validFrom != null && validFrom.after(testDate))
            return false;
        //	ok	(validTo)
        if (validTo != null && validTo.before(testDate))
            return false;
        return true;
    }	//	isValid
}
