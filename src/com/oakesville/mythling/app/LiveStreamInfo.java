/**
 * Copyright 2013 Donald Oakes
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

public class LiveStreamInfo
{
  private long id;
  public long getId() { return id; }
  public void setId(long id) { this.id = id; }
  
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
  
}
