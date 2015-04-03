/**
 * Copyright 2015 Donald Oakes
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
package com.oakesville.mythling.app;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

import com.oakesville.mythling.BuildConfig;
import com.oakesville.mythling.R;
import com.oakesville.mythling.util.Reporter;

/**
 * Note: initialize() had better have been run before accessing any static methods.
 */
public class Localizer {

    private static final String TAG = Localizer.class.getSimpleName();

    private static String[] leadingArticles = new String[] { "A", "An", "The" };

    private static AppSettings appSettings;

    private static Context getAppContext() {
        return appSettings.getAppContext();
    }

    public static void initialize(AppSettings appSettings) {
        Localizer.appSettings = appSettings;
        try {
            leadingArticles = getAppContext().getResources().getStringArray(R.array.leading_articles);
        } catch (NotFoundException ex) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, ex.getMessage(), ex);
            if (appSettings.isErrorReportingEnabled())
                new Reporter(ex).send();
        }
    }

    public static String stripLeadingArticle(String in) {
        for (String leadingArticle : leadingArticles) {
            if (in.startsWith(leadingArticle + " "))
                return in.substring(leadingArticle.length() + 1);
        }
        return in;
    }

    public static String getStringRes(int resId, String... substs) {
        String str = getAppContext().getString(resId);
        for (int i = 0; i < substs.length; i++) {
            str = str.replaceAll("%" + i + "%", substs[i]);
        }
        return str;
    }

    public static String getStringRes(int resId) {
        return getAppContext().getString(resId);
    }
}
