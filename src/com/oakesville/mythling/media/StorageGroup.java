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
package com.oakesville.mythling.media;

public class StorageGroup
{
  private String name;
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  
  private String host;
  public String getHost() { return host; }
  public void setHost(String host) { this.host = host; }
  
  private String directory;
  public String getDirectory() { return directory; }
  public void setDirectory(String dir) { this.directory = dir; }
}
