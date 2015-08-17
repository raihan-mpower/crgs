package com.mpower.clientcollection.utilities;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import android.util.Log;


public class DateUtils {
	

	public static long midnightTimestamp() {
		
		Date date = new Date(); // timestamp now
		Calendar cal = Calendar.getInstance(); // get calendar instance
		cal.setTime(date); // set cal to date
		cal.set(Calendar.HOUR_OF_DAY, 0); // set hour to midnight
		cal.set(Calendar.MINUTE, 0); // set minute in hour
		cal.set(Calendar.SECOND, 0); // set second in minute
		cal.set(Calendar.MILLISECOND, 0); // set millis in second	
		long timstamp = cal.getTimeInMillis(); // actually computes the new Date

		return timstamp;
	}

	public static String formattedDateFromTimestamp(long timestamp, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);

		Date resultdate = new Date(timestamp);		
		String retVal = sdf.format(resultdate);
		return retVal;

	}
	
	public static long getTimestamp(){
		Date date = new Date(); // timestamp now
		Calendar cal = Calendar.getInstance(); // get calendar instance
		cal.setTime(date); // set cal to date
		long timstamp = cal.getTimeInMillis(); // actually computes the new Date
		
		return timstamp;
	}
	public static long getCurrentDateTimesStamp(){
		
		DateTime jodaTime = new DateTime();
		DateTimeFormatter formatter = DateTimeFormat.forPattern("YYYY-MM-dd");
		String currentDate = formatter.print(jodaTime);
		long timstamp = getTimestampFromString(currentDate);		
		
		return timstamp;
	}
	
	public static String getCurrentDate(){
		//String mydate = DateFormat.getDateInstance().format(new Date());
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
		String currentDate = dateFormat.format(new Date());		
		return currentDate;
	}

	public static long getTimestampFromString(String date){
		 SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		    Date convertedDate = new Date();
		    try {
		        convertedDate = dateFormat.parse(date);
		    } catch (ParseException e) {		       
		        e.printStackTrace();
		    }
		    Calendar cal = Calendar.getInstance();
			cal.setTime(convertedDate); 
			long timstamp = cal.getTimeInMillis();
			
			return timstamp;
		
	}
	public static String getCurrentDateInString() {
		DateTime jodaTime = new DateTime();
		DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/YYYY");
		String currentDate = formatter.print(jodaTime);
		Log.d("DateUtils", "DateUtils = " + currentDate);
		return currentDate;
	}  
}
