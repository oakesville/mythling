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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.oakesville.mythling.media.MediaSettings.MediaType;
import com.oakesville.mythling.util.TextBuilder;

public class Song extends Item {
    public static final String ARTWORK_LEVEL_SONG = "songArtwork";

    public Song(String id, String title) {
        super(id, title);
    }

    public MediaType getType() {
        return MediaType.music;
    }

    private String albumArt;  // pathless filename
    public String getAlbumArt() { return albumArt; }
    public void setAlbumArt(String art) { this.albumArt = art; }

    /**
     * Storage group is null to indicate no artwork.
     */
    @Override
    public ArtworkDescriptor getArtworkDescriptor(String storageGroup) {
        if (storageGroup == null)
            return null;

        // ARTWORK_LEVEL_SONG = bogus storage group to indicate use GetAlbumArt with songId
        final boolean songLevelArt = ARTWORK_LEVEL_SONG.equals(storageGroup);

        return new ArtworkDescriptor(storageGroup) {
            public String getArtworkPath() {
                // cache at album level if it makes sense
                return getStorageGroup() + (songLevelArt ? ("/" + getId()) : "");
            }

            public String getArtworkContentServicePath() throws UnsupportedEncodingException {
                if (songLevelArt)
                    return "GetAlbumArt?Id=" + getId();
                else
                    return "GetImageFile?StorageGroup=Music&FileName=" + URLEncoder.encode(getPath() + "/" + albumArt, "UTF-8");
            }
        };
    }

    public String getSearchResultText() {
        TextBuilder tb = new TextBuilder();
        tb.appendParen(getTypeLabel());
        tb.append(getSearchPath());
        tb.appendLine(getTitle());
        return tb.toString();
    }

}
