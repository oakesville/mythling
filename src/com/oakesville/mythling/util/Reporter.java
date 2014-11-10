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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.app.AppSettings;

public class Reporter
{
  private static final String TAG = Reporter.class.getSimpleName();
  public static final String ERROR_REPORTING_URL = "http://54.187.163.194/oakesville/report";  
  
  private Throwable throwable;
  private String message;
  
  public Reporter(Throwable t)
  {
    this.throwable = t;
    this.message = t.getMessage();
  }
  
  public Reporter(String message)
  {
    this.message = message;
  }
  
  /**
   * Report the error in a background thread.
   */
  public void send()
  {
    new Thread(new Runnable()
    {
      public void run()
      {
        try
        {
          JSONObject json = buildJson();
          HttpHelper helper = new HttpHelper(new URL(ERROR_REPORTING_URL));
          String response = new String(helper.post(json.toString(2).getBytes()));
          JSONObject responseJson = new JSONObject(response);
          JSONObject reportResponse = responseJson.getJSONObject("reportResponse");
          String status = reportResponse.getString("status");
          if (!"success".equals(status))
            throw new IOException("Error response from reporting site: " + reportResponse.getString("message"));
        }
        catch (Exception ex)
        {
          if (BuildConfig.DEBUG)
            Log.e(TAG, ex.getMessage(), ex);
        }    
      }
    }).start();    
  }
  
  private JSONObject buildJson() throws JSONException
  {
    JSONObject json = new JSONObject();
    JSONObject report = new JSONObject();
    json.put("report", report);
    report.put("source", "Mythling v" + AppSettings.getMythlingVersion());
    report.put("message", message == null ? "No message" : message);
    if (throwable != null)
    {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      throwable.printStackTrace(new PrintStream(out));
      report.put("stackTrace", new String(out.toByteArray()));
    }
    return json;
  }  
}
