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
package com.oakesville.mythling.media;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.util.TextBuilder;

import android.util.Log;

/**
 * A live or recorded TV show.
 */
public class TvShow extends Item {
    private static final String TAG = TvShow.class.getSimpleName();

    private String channelNumber;
    public String getChannelNumber() { return channelNumber; }
    public void setChannelNumber(String channum) { this.channelNumber = channum; }

    private String callsign;
    public String getCallsign() { return callsign; }
    public void setCallsign(String callsign) { this.callsign = callsign; }

    private String description;
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    private Date originallyAired;
    public Date getOriginallyAired() { return originallyAired; }
    public void setOriginallyAired(Date aired) { this.originallyAired = aired; }

    private Date startTime;
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    private Date endTime;
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    private String programStart;
    public String getProgramStart() { return programStart; }
    public void setProgramStart(String programStart) { this.programStart = programStart; }

    @Override
    public String getFilePath() {
        return getFileName();  // logical category path should not be used for file access
    }

    public int getChannelId() {
        return Integer.parseInt(getId().substring(0, getId().indexOf('~')));
    }

    public String getStartTimeParam() {
        return getStartTimeRaw().replace(' ', 'T');
    }

    public String getStartTimeRaw() {
        return getId().substring(getId().indexOf('~') + 1);
    }

    public String getStartDateTimeFormatted() throws ParseException {
        Calendar oneYearAgo = Calendar.getInstance();
        oneYearAgo.add(Calendar.YEAR, -1);
        boolean showYear = oneYearAgo.getTime().compareTo(getStartTime()) > 0;
        if (showYear)
            return Localizer.getDateTimeYearAbbrev(getStartTime());
        else
            return Localizer.getDateTimeAbbrev(getStartTime());
    }

    public String getStartTimeFormatted() throws ParseException {
        return Localizer.getTimeAbbrev(getStartTime());
    }

    public String getEndDateTimeFormatted() throws ParseException {
        return Localizer.getDateTimeAbbrev(getEndTime());
    }

    public String getEndTimeFormatted() throws ParseException {
        return Localizer.getTimeAbbrev(getEndTime());
    }

    public String getEndTimeParam() {
        return Localizer.SERVICE_DATE_TIME_RAW_FORMAT.format(getEndTime()).replace(' ', 'T');
    }

    public String getChanIdStartTimeParams() {
        return "ChanId=" + getChannelId() + "&StartTime=" + getStartTimeParam();
    }

    public TvShow(String id, String title) {
        super(id, title);
    }

    public MediaType getType() {
        return MediaType.liveTv;
    }

    public String getShowDateTimeInfo() {
        TextBuilder tb = new TextBuilder();
        try {
            tb.append(getStartDateTimeFormatted()).append("-");
            tb.append(getEndTimeFormatted());
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }

        return tb.toString();
    }

    public String getShowTimeInfo() {
        TextBuilder tb = new TextBuilder();
        try {
            tb.append(getStartTimeFormatted()).append("-");
            tb.append(getEndTimeFormatted());
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage(), ex);
        }

        return tb.toString();
    }

    public String getChannelInfo() {
        return new TextBuilder().append(getChannelNumber()).appendParen(getCallsign()).toString();
    }

    public String getAirDateInfo() {
        TextBuilder tb = new TextBuilder();
        if (isRepeat()) {
            if (isShowMovie())
                tb.appendParen(Localizer.getYearFormat().format(originallyAired));
            else
                tb.appendParen(Localizer.getStringRes(R.string.originally_aired) + " " + Localizer.getDateFormat().format(originallyAired));
        }
        return tb.toString();
    }

    public String getFormat() {
        if (getType() == MediaType.liveTv)
            return "Live TV"; // for remembering stream option
        else
            return super.getFormat();
    }

    public boolean isShowMovie() {
        return getRating() > 0; // proxy for determining the show is a movie
    }

    public int getYear() {
        if (originallyAired == null)
            return 0;
        return Integer.parseInt(Localizer.getYearFormat().format(originallyAired));
    }

    protected boolean isRepeat() {
        if (originallyAired == null)
            return false;

        Calendar origCal = Calendar.getInstance();
        origCal.setTime(originallyAired);
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startTime);
        return !(origCal.get(Calendar.YEAR) == startCal.get(Calendar.YEAR)
                && origCal.get(Calendar.MONTH) == startCal.get(Calendar.MONTH)
                && origCal.get(Calendar.DAY_OF_MONTH) == startCal.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Special display for LiveTV.
     */
    @Override
    public String getListText() {
        if (getType() == MediaType.liveTv) {
            return new TextBuilder(getChannelInfo()).append(getShowTimeInfo()).appendLine(getTitle()).toString();
        } else {
            return super.getListText();
        }
    }

    @Override
    public String getSearchResultText() {
        if (getType() == MediaType.liveTv) {
            TextBuilder tb = new TextBuilder();
            tb.appendParen(getTypeLabel());
            tb.append(getTitle());
            tb.appendLine(getChannelInfo());
            tb.append(getShowTimeInfo());
            return tb.toString();
        } else {
            return super.getSearchResultText();
        }
    }

    @Override
    public String getListSubText() {
        TextBuilder tb = new TextBuilder();
        if (isShowMovie())
            tb.appendYear(getYear());
        tb.appendRating(getRating());
        tb.appendQuotedLine(getSubTitle());
        if (!isShowMovie() && isRepeat())
            tb.appendLine(getAirDateInfo());
        return tb.toString();
    }

    @Override
    public String getDialogTitle() {
        TextBuilder tb = new TextBuilder(getTitle());
        if (isShowMovie())
            tb.appendYear(getYear());
        tb.appendDashed(getSubTitle());
        return tb.toString();
    }

    @Override
    public String getSubLabel() {
        TextBuilder tb = new TextBuilder();
        if (isShowMovie())
            tb.appendYear(getYear());
        tb.appendQuotedLine(getSubTitle());
        return tb.toString();
    }

    @Override
    public String getDialogText() {
        TextBuilder tb = new TextBuilder(getTitle());
        if (isShowMovie())
            tb.appendYear(getYear());
        tb.appendRating(getRating());
        tb.appendLine(getSummary());
        return tb.toString();
    }

    public String getSummary() {
        TextBuilder tb = new TextBuilder();
        tb.appendLine(getChannelInfo()).append(getShowTimeInfo());
        tb.appendQuotedLine(getSubTitle());
        if (!isShowMovie() && isRepeat())
            tb.appendLine(getAirDateInfo());
        if (getDescription() != null)
            tb.appendLine(getDescription());
        return tb.toString();
    }

    @Override
    protected Comparator<Item> getDateComparator() {
        return new Comparator<Item>() {
            public int compare(Item item1, Item item2) {
                TvShow show1 = (TvShow) item1;
                TvShow show2 = (TvShow) item2;
                if (show1.getStartTime() == null) {
                    if (show2.getStartTime() == null)
                        return show1.toString().compareTo(show2.toString());
                    else return 1;
                } else if (show2.getStartTime() == null) {
                    return -1;
                } else {
                    if (show1.getStartTime().equals(show2.getStartTime())) {
                        String t1 = Localizer.stripLeadingArticle(show1.getTitle());
                        String t2 = Localizer.stripLeadingArticle(show2.getTitle());
                        return t1.compareToIgnoreCase(t2);
                    } else {
                        return show2.getStartTime().compareTo(show1.getStartTime());
                    }
                }
            }
        };
    }

    @Override
    protected Comparator<Item> getChannelNumberComparator() {
        return new Comparator<Item>() {
            public int compare(Item item1, Item item2) {
                String chan1 = ((TvShow)item1).getChannelNumber();
                String chan2 = ((TvShow)item2).getChannelNumber();
                // try to sort as numbers
                try {
                    int c1 = Integer.parseInt(chan1);
                    int c2 = Integer.parseInt(chan2);
                    return c1 - c2;
                }
                catch (NumberFormatException ex) {
                    return chan1.compareToIgnoreCase(chan2);
                }
            }
        };
    }

    @Override
    protected Comparator<Item> getCallsignComparator() {
        return new Comparator<Item>() {
            public int compare(Item item1, Item item2) {
                TvShow show1 = (TvShow)item1;
                TvShow show2 = (TvShow)item2;
                return show1.getCallsign().compareToIgnoreCase(show2.getCallsign());
            }
        };
    }
}
