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

import java.util.Comparator;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.Localizer;
import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.util.TextBuilder;

/**
 * A TV episode from MythVideo.
 */
public class TvEpisode extends Video {

    private int season;
    public int getSeason() { return season; }
    public void setSeason(int season) { this.season = season; }

    private int episode;
    public int getEpisode() { return episode; }
    public void setEpisode(int episode) { this.episode = episode; }

    public TvEpisode(String id, String title) {
        super(id, title);
    }

    public MediaType getType() {
        return MediaType.tvSeries;
    }

    @Override
    public String getListSubText() {
        return new TextBuilder(getSeasonEpisodeInfo()).appendQuotedLine(getSubTitle()).toString();
    }

    public String getSummary() {
        if (getSeason() != 0) {
            TextBuilder tb = new TextBuilder();
            tb.append(Localizer.getStringRes(R.string.season)).append(getSeason()).append(",");
            tb.append(Localizer.getStringRes(R.string.episode)).append(getEpisode());
            tb.appendLine(super.getSummary());
            return tb.toString();
        } else {
            return super.getSummary();
        }
    }

    private String getSeasonEpisodeInfo() {
        return new TextBuilder().appendParen((getYear() > 0 ? getYear() + " " : "") + "s" + getSeason() + "e" + getEpisode()).toString();
    }

    @Override
    protected Comparator<Item> getDateComparator() {
        return new Comparator<Item>() {
            public int compare(Item item1, Item item2) {
                TvEpisode episode1 = (TvEpisode) item1;
                TvEpisode episode2 = (TvEpisode) item2;
                if (episode1.getSeason() == episode2.getSeason())
                    return episode1.getEpisode() - episode2.getEpisode();
                else
                    return episode1.getSeason() - episode2.getSeason();
            }
        };
    }
}
