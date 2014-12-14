/**
 * Copyright 2014 Donald Oakes
 *
 * This file is part of Mythling.
 *
 * Mythling is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mythling is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mythling.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.oakesville.mythling.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class DateTimeFormats {
    public static final DateFormat SERVICE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static final DateFormat SERVICE_DATE_TIME_RAW_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        SERVICE_DATE_TIME_RAW_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static DateFormat DATE_FORMAT = new SimpleDateFormat("MMM d yyyy");
    public static DateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a");
    public static DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MMM d  h:mm a");
    public static DateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy");
}
