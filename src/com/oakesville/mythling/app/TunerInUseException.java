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
  
  private Work recording;
  public Work getRecording() { return recording; }
  public void setRecording(Work rec) { this.recording = rec; }
}
