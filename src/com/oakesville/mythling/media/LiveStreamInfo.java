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

public class LiveStreamInfo {

    public static final int STATUS_CODE_COMPLETED = 3;
    public static final int STATUS_CODE_STOPPED = 6;

    private long id;
    public long getId() { return id; }
    public void setId(long id) { this.id = id;}

    private int statusCode;
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int code) { this.statusCode = code; }

    private String status;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    private int percentComplete;
    public int getPercentComplete() { return percentComplete; }
    public void setPercentComplete(int pc) { this.percentComplete = pc; }

    public int width;
    public int getWidth() { return width; }
    public void setWidth(int w) { this.width = w; }

    public int height;
    public int getHeight() { return height; }
    public void setHeight(int h) { this.height = h; }

    public int videoBitrate;
    public int getVideoBitrate() { return videoBitrate; }
    public void setVideoBitrate(int br) { this.videoBitrate = br; }

    public int audioBitrate;
    public int getAudioBitrate() { return audioBitrate; }
    public void setAudioBitrate(int abr) { this.audioBitrate = abr; }

    private String message;
    public String getMessage() { return message; }
    public void setMessage(String msg) { this.message = msg; }

    private String relativeUrl;
    public String getRelativeUrl() { return relativeUrl; }
    public void setRelativeUrl(String relUrl) { this.relativeUrl = relUrl; }

    private String file;
    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }

    public boolean isCompleted() {
        return statusCode == STATUS_CODE_COMPLETED;
    }

    public boolean matchesItem(Item item) {
        if (item.getStorageGroup() == null || item.getStorageGroup().getDirectories() == null)
            return false;
        for (String dir : item.getStorageGroup().getDirectories()) {
            if (file.equals(dir + "/" + item.getFilePath()))
                return true;
        }
        return false;
    }
}
