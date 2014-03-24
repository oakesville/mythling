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

import java.io.IOException;

public class TunerInUseException extends IOException
{
  public TunerInUseException(String msg)
  {
    super(msg);
  }
  
  public TunerInUseException(String msg, Throwable cause)
  {
    super(msg, cause);
  }
  
  private Recording recording;
  public Recording getRecording() { return recording; }
  public void setRecording(Recording rec) { this.recording = rec; }
}
