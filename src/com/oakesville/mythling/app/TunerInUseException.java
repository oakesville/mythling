package com.oakesville.mythling.app;

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
  
  private Item recording;
  public Item getRecording() { return recording; }
  public void setRecording(Item rec) { this.recording = rec; }
}
