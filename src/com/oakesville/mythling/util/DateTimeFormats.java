package com.oakesville.mythling.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class DateTimeFormats
{
  public static final DateFormat SERVICE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  
  public static final DateFormat SERVICE_DATE_TIME_RAW_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  static
  {
    SERVICE_DATE_TIME_RAW_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public static DateFormat DATE_FORMAT = new SimpleDateFormat("MMM d yyyy");
  public static DateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a");
  public static DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MMM d  h:mm a");
  public static DateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy");
}
