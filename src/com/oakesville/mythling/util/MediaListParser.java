package com.oakesville.mythling.util;

import java.text.ParseException;

import org.json.JSONException;

import com.oakesville.mythling.media.MediaList;
import com.oakesville.mythling.media.MediaSettings.MediaType;

public interface MediaListParser
{
  public abstract MediaList parseMediaList(MediaType mediaType) throws JSONException, ParseException;
}
