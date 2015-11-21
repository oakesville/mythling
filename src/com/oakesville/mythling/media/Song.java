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

import com.oakesville.mythling.media.MediaSettings.MediaType;

public class Song extends Item {
    public static final String ARTWORK_LEVEL_ALBUM = "albumArtwork";
    public static final String ARTWORK_LEVEL_SONG = "songArtwork";

    private int albumArtId;
    public int getAlbumArtId() { return albumArtId;  }
    public void setAlbumArtId(int id) { this.albumArtId = id;  }

    public Song(String id, String title) {
        super(id, title);
    }

    public MediaType getType() {
        return MediaType.music;
    }

    @Override
    public ArtworkDescriptor getArtworkDescriptor(String storageGroup) {
        if (albumArtId == 0)
            return null;

        // actually storageGroup is artwork level (album or song)
        final boolean songLevelArt = ARTWORK_LEVEL_SONG.equals(storageGroup);

        return new ArtworkDescriptor(storageGroup) {
            public String getArtworkPath() {
                // cache at album level
                return getStorageGroup() + (songLevelArt ? ("/" + getId()) : "");
            }

            public String getArtworkContentServicePath() throws UnsupportedEncodingException {
                return "GetAlbumArt?Id=" + getAlbumArtId();
            }
        };
    }

    public String getSearchResultText() {
        StringBuffer buf = new StringBuffer(getPrefix());
        buf.append("(").append(getTypeLabel()).append(") ");
        if (!getSearchPath().isEmpty())
            buf.append(getSearchPath()).append("\n");
        buf.append(getTitle());
        return buf.toString();
    }

}
