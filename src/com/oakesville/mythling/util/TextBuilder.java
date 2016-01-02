/**
 * Copyright 2015 Donald Oakes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oakesville.mythling.util;

/**
 * Utility for building labes for displaying in TextViews.
 * All append methods check for null and don't append in that case.
 */
public class TextBuilder {

    public static final char STAR = 0x2605;
    public static final char HALF_STAR = 0x00BD;
    public static final char DASH = 0x2013;

    private StringBuilder stringBuilder;

    public TextBuilder() {
        stringBuilder = new StringBuilder();
    }

    public TextBuilder(String s) {
        stringBuilder = s == null ? new StringBuilder() : new StringBuilder(s);
    }

    public TextBuilder(StringBuilder sb) {
        this.stringBuilder = sb;
    }

    public TextBuilder append(String s) {
        if (s != null) {
            if (stringBuilder.length() > 0)
                stringBuilder.append(" ");
            stringBuilder.append(s);
        }
        return this;
    }

    public TextBuilder append(int i) {
        return append(String.valueOf(i));
    }

    public TextBuilder appendYear(int year) {
        if (year > 0)
            appendParen(String.valueOf(year));
        return this;
    }

    public TextBuilder appendLine() {
        return appendLine(""); // just the \n
    }

    public TextBuilder appendLine(String s) {
        if (s != null) {
            if (stringBuilder.length() > 0)
                stringBuilder.append("\n");
            stringBuilder.append(s);
        }
        return this;
    }

    public TextBuilder appendQuoted(String s) {
        if (s != null) {
            if (stringBuilder.length() > 0)
                stringBuilder.append(" ");
            stringBuilder.append("\"").append(s).append("\"'");
        }
        return this;
    }

    public TextBuilder appendQuotedLine(String s) {
        if (s != null) {
            if (stringBuilder.length() > 0)
                stringBuilder.append("\n");
            stringBuilder.append("\"").append(s).append("\"");
        }
        return this;
    }

    public TextBuilder appendParen(String s) {
        if (s != null) {
            if (stringBuilder.length() > 0)
                stringBuilder.append(" ");
            stringBuilder.append("(").append(s).append(")");
        }
        return this;
    }

    public TextBuilder appendParenLine(String s) {
        if (s != null) {
            if (stringBuilder.length() > 0)
                stringBuilder.append("\n");
            stringBuilder.append("(").append(s).append(")");
        }
        return this;
    }

    public TextBuilder appendRating(float stars) {
        if (stars > 0 && stringBuilder.length() > 0)
            stringBuilder.append(" ");
        for (int i = 0; i < stars; i++) {
            if (i <= stars - 1)
                stringBuilder.append(String.valueOf(STAR));
            else
                stringBuilder.append(String.valueOf(HALF_STAR));
        }
        return this;
    }

    public TextBuilder appendDashed(String s) {
        if (s != null) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(" ");
                stringBuilder.append(String.valueOf(DASH));
                stringBuilder.append(" ");
            }
            stringBuilder.append(s);
        }
        return this;
    }

    public TextBuilder appendDuration(int seconds) {
        int mins = 0;
        int hrs = 0;

        if (seconds >= 60) {
          mins = seconds / 60;
          seconds = seconds % 60;
        }
        if (mins >= 60) {
            hrs = mins / 60;
            mins = mins % 60;
        }

        if (hrs > 0)
            stringBuilder.append(hrs).append(":");
        appendPadTwo(mins);
        stringBuilder.append(":");
        appendPadTwo(seconds);

        return this;
    }

    public TextBuilder appendSeasonEpisode(int season, int episode) {
        if (season > 0 && episode > 0)
            stringBuilder.append("S").append(season).append("E").append(episode);
        return this;
    }

    public TextBuilder appendPadTwo(long l) {
        String s = String.valueOf(l);
        if (s.length() == 1)
            stringBuilder.append("0").append(s);
        else
            stringBuilder.append(s);
        return this;
    }

    /**
     * Returns null if empty.
     */
    public String toString() {
        return stringBuilder.length() == 0 ? null : stringBuilder.toString();
    }
}
