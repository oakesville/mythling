package com.oakesville.mythling.media;

import java.util.Comparator;

import com.oakesville.mythling.media.MediaSettings.MediaType;

/**
 * A TV episode from MythVideo.
 */
public class TvEpisode extends Video
{
  private int season;
  public int getSeason() { return season; }
  public void setSeason(int season) { this.season = season; }
  
  private int episode;
  public int getEpisode() { return episode; }
  public void setEpisode(int episode) { this.episode = episode; }
  
  public TvEpisode(String id, String title)
  {
    super(id, title);
  }
  
  public MediaType getType()
  {
    return MediaType.tvSeries;
  }
  
  public String getTypeTitle()
  {
    return "TV Episode";
  }
  
  @Override
  public String getText()
  {
    StringBuffer buf = new StringBuffer(PREFIX + getTitle());
    buf.append(" (s").append(getSeason()).append("e").append(getEpisode()).append(")");
    if (getRating() > 0)
      buf.append(" ").append(getRatingString(getRating()));
    return buf.toString();
  }

  public String getSearchResultText()
  {
    StringBuffer buf = new StringBuffer(PREFIX);
    buf.append("(").append(getTypeTitle()).append(") ");
    if (getPath() != null && getPath().length() > 0)
      buf.append(getPath()).append("/");
    buf.append(getTitle());
    buf.append(" (s").append(getSeason()).append("e").append(getEpisode()).append(")");
    return buf.toString();
  }
  
  @Override
  protected Comparator<Item> getDateComparator()
  {
    return new Comparator<Item>()
    {
      public int compare(Item item1, Item item2)
      {
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
