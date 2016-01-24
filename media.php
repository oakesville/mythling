 <?php
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

// Assign values for these constants and drop this file somewhere
// under your Apache DocumentRoot directory.
$MYTHTV_VERSION = "0.28";
$MYTHDB_HOST = "localhost";
$MYTHDB_DATABASE = "mythconverg";
$MYTHDB_USER = "mythtv";
$MYTHDB_PASSWORD = "mythtv";

// MythTV settings
$VIDEO_STORAGE_GROUP = "Videos";
$VIDEO_DIR_SETTING = "VideoStartupDir";
$MUSIC_DIR_SETTING = "MusicLocation";
$ARTWORK_DIR_SETTING = "VideoArtworkDir";

$type = new Type($_REQUEST['type']);
if (!$type->isSpecified())
  die(error("Missing request parameter: type"));
if (!$type->isValid())
  die(error("Unsupported query type: " . $type->type));
if ($type->isSearch())
{
  $searchQuery = $_REQUEST['query'];
  if ($searchQuery == null)
    die(error("Missing search parameter: query"));
}
if ($type->isGuide())
{
  $startTime = $_REQUEST['StartTime'];
  if ($startTime == null)
    die(error("Missing guide parameter: StartTime"));
  if (!strpos($startTime, 'Z', strlen($startTime) - 1))
    $startTime .= 'Z';
  if (isset($_REQUEST['EndTime']))
  {
    $endTime = $_REQUEST['EndTime'];
    if (!strpos($endTime, 'Z', strlen($endTime) - 1))
      $endTime .= 'Z';
  }
  else
  {
    $endTime = null;
  }
  if (isset($_REQUEST['listingsSearch']))
    $listingsSearch = $_REQUEST['listingsSearch'];
  else
    $listingsSearch = null;
  if ($endTime == null && $listingsSearch == null)
    die(error("Missing guide parameter: EndTime or listingsSearch"));
  if (isset($_REQUEST['ChannelGroupId']))
    $channelGroupId = $_REQUEST['ChannelGroupId'];
  else
    $channelGroupId = null;
}
if ($type->isCutList())
{
  $chanId = $_REQUEST['ChanId'];
  if ($chanId == null)
    die(error("Missing cutList parameter: ChanId"));
  $startTime = $_REQUEST['StartTime'];
  if ($startTime == null)
    die(error("Missing cutList parameter: StartTime"));
}

$categorizeUsingDirs = false;
$categorizeUsingMetadata = false;

if (isset($_REQUEST['movieDirs']))
{
  $movieDirs = explode(",", $_REQUEST['movieDirs']);
  $categorizeUsingDirs = true;
}
else
{
  $movieDirs = array();
}

if (isset($_REQUEST['tvSeriesDirs']))
{
  $tvSeriesDirs = explode(",", $_REQUEST['tvSeriesDirs']);
  $categorizeUsingDirs = true;
}
else
{
  $tvSeriesDirs = array();
}

if (isset($_REQUEST['videoExcludeDirs']))
{
  $videoExcludeDirs = explode(",", $_REQUEST['videoExcludeDirs']);
}
else
{
  $videoExcludeDirs = array();
}

if (isset($_REQUEST['categorizeUsingMetadata']))
{
  if (strtoupper($_REQUEST['categorizeUsingMetadata']) == 'TRUE')
    $categorizeUsingMetadata = true;
}

$artworkStorageGroup = "Coverart";  // or Fanart or Screenshots or Banners
if (isset($_REQUEST['artworkStorageGroup']))
{
  $artworkStorageGroup = $_REQUEST['artworkStorageGroup'];
}

$albumArtAlbumLevel = true;  // otherwise at individual song level (prob. embedded)
if (isset($_REQUEST['albumArtSongLevel']))
{
  if (strtoupper($_REQUEST['albumArtSongLevel']) == 'TRUE')
    $albumArtAlbumLevel = false;
}

$flatten = false;  // currently only affects recordings
if (isset($_REQUEST['flatten']))
{
  if (strtoupper($_REQUEST['flatten']) == 'TRUE')
    $flatten = true;
}

if (isset($_REQUEST['flatten']))
{
  if (strtoupper($_REQUEST['flatten']) == 'TRUE')
    $flatten = true;
}

$hostname = gethostname();
date_default_timezone_set("UTC");
$dt = date("m-d-Y H:i:s");

mysql_connect($MYTHDB_HOST, $MYTHDB_USER, $MYTHDB_PASSWORD) or die(error("Could not connect: " . mysql_error()));
mysql_select_db($MYTHDB_DATABASE) or die(error("Unable to select database"));

$isVideoStorageGroup = false;
$castMap = null;

if ($type->isGuide())
{
  $where =  "inner join channel c on c.chanid = p.chanid" . "\n";
  if ($channelGroupId != null)
    $where .= "  and c.chanid in (select chanid from channelgroup where grpid = " . $channelGroupId . ")" . "\n";
  $where .= "left outer join oldrecorded r on c.callsign = r.station" . "\n";
  $where .= "  and p.starttime = r.starttime" . "\n";
  if ($listingsSearch != null)
  {
    $where .= "left outer join credits cr on p.chanid = cr.chanid and p.starttime = cr.starttime" . "\n";
    $where .= "left outer join people peeps on peeps.person = cr.person" . "\n";
    $where .= "where p.endtime >= '" . $startTime . "'" . "\n";
    $where .= "and (p.title like '%" . $listingsSearch . "%' or p.subtitle like '%" . $listingsSearch . "%' or p.description like '%" . $listingsSearch . "%'";
    $where .= " or peeps.name like '%" . $listingsSearch . "%')";
  }
  else
  {
    $where .= "where p.starttime < '" . $endTime . "'" . "\n";
    $where .= "and p.endtime >= '" . $startTime . "'" . "\n";
    $where .= "and c.visible = true";
  }

  if ($listingsSearch != null)
    $orderBy = "order by p.starttime, cast(c.channum as unsigned)";
  else
    $orderBy = "order by cast(c.channum as unsigned), p.starttime";
  
  $query = $listingsSearch == null ? "select " : "select distinct ";
  $query .= "c.chanid, c.callsign, c.channum, c.icon, p.category_type, p.category, p.starttime, p.endtime, p.title, p.subtitle, p.previouslyshown, r.recstatus" . "\n";
  $query .= "from program p" . "\n";
  
  $query .= $where . "\n" . $orderBy;
  
  if (isShowQuery())
    echo "query:\n" . $query . "\n\n";

  mysql_set_charset("utf8");
  $res = mysql_query($query) or die(error("Query failed: " . mysql_error()));
  $num = mysql_numrows($res);
  
  header("Content-type:application/json");
  if ($listingsSearch != null)
  {
    echo '{' . "\n";
    echo '  "ProgramList": {';
    echo ' "AsOf": "' . substr(date(DATE_ISO8601), 0, -5) . 'Z", "Version": "' . $MYTHTV_VERSION . '",' . "\n";
    echo ' "Count": "' . $num . '",' . "\n";
    echo '    "Programs":' . "\n" . '    [' . "\n";
    $i = 0;
    while ($i < $num)
    {
      $chanId = mysql_result($res, $i, "chanid");
      $callSign = mysql_result($res, $i, "callsign");
      $chanNum = mysql_result($res, $i, "channum");
      $type = mysql_result($res, $i, "category_type");
      $category = mysql_result($res, $i, "category");
      $startTime = mysql_result($res, $i, "starttime");
      $startTime = str_replace(' ', 'T', $startTime) . 'Z';
      $endTime = mysql_result($res, $i, "endtime");
      $endTime = str_replace(' ', 'T', $endTime) . 'Z';
      $title = cleanup(mysql_result($res, $i, "title"));
      $subTitle = cleanup(mysql_result($res, $i, "subtitle"));
      $repeat = mysql_result($res, $i, "previouslyshown");
      $recStatus = mysql_result($res, $i, "recstatus");
      if ($recStatus == null)
        $recStatus = 0;
      printProgram($type, $category, $startTime, $endTime, $title, $subTitle, $repeat, $recStatus, $chanId, $callSign, $chanNum);
      if ($i < $num - 1)
        echo ', ' . "\n";
      $i++;
    }
    echo '    ]' . "\n";
    echo '  }' . "\n";
    echo '}';
  }
  else
  {
    echo '{' . "\n";
    echo '  "ProgramGuide": {';
    echo ' "AsOf": "' . substr(date(DATE_ISO8601), 0, -5) . 'Z", "Version": "' . $MYTHTV_VERSION . '",' . "\n";
    echo '    "Channels":' . "\n" . '    [' . "\n";
    $curChan = null;
    $i = 0;
    while ($i < $num)
    {
      $chanId = mysql_result($res, $i, "chanid");
      if ($chanId == $curChan)
      {
        echo ',' . "\n"; // sep programs in array
      }
      else
      {
        if ($curChan != null)
          printChannelEnd(false);
        $callSign = mysql_result($res, $i, "callsign");
        $chanNum = mysql_result($res, $i, "channum");
        $icon = mysql_result($res, $i, "icon");
        printChannelBegin($chanId, $chanNum, $callSign, $icon);
        $curChan = $chanId;
      }
      $type = mysql_result($res, $i, "category_type");
      $category = mysql_result($res, $i, "category");
      $startTime = mysql_result($res, $i, "starttime");
      $startTime = str_replace(' ', 'T', $startTime) . 'Z';
      $endTime = mysql_result($res, $i, "endtime");
      $endTime = str_replace(' ', 'T', $endTime) . 'Z';
      $title = cleanup(mysql_result($res, $i, "title"));
      $subTitle = cleanup(mysql_result($res, $i, "subtitle"));
      $repeat = mysql_result($res, $i, "previouslyshown");
      $recStatus = mysql_result($res, $i, "recstatus");
      if ($recStatus == null)
        $recStatus = 0;
      printProgram($type, $category, $startTime, $endTime, $title, $subTitle, $repeat, $recStatus, null, null, null);
      $i++;
    }
    
    if ($num > 0)
      printChannelEnd(true);
    echo '    ]' . "\n";
    echo '  }' . "\n";
    echo '}';
  }

  mysql_close();
  
}
else if ($type->isCutList())
{
  // currently only comm flags supported
  $baseWhere = "where chanid = " . $chanId . "\n";
  $baseWhere .= "and starttime = '" . $startTime . "'\n";
  $where = $baseWhere . "and type in (4, 5)";
  
  $orderBy = "order by mark, type";
  
  $query = "select type, mark from recordedmarkup" . "\n";
  $query .= $where . "\n" . $orderBy;
  
  if (isShowQuery())
    echo "query:\n" . $query . "\n\n";
  mysql_set_charset("utf8");
  $res = mysql_query($query) or die(error("Query failed: " . mysql_error()));
  $num = mysql_numrows($res);
  
  header("Content-type:application/json");
  echo '{' . "\n";
  echo '  "CutList": {';
  echo ' "AsOf": "' . substr(date(DATE_ISO8601), 0, -5) . 'Z", "Version": "' . $MYTHTV_VERSION . '", ';
  echo ' "Count": "' . $num . '",' . "\n";
  echo '    "Cuttings":' . "\n" . '    [' . "\n";
    
  $i = 0;
  while ($i < $num)
  {
    $markType = mysql_result($res, $i, "type"); // 4 or 5
    $frame = mysql_result($res, $i, "mark");
    
    $innerWhere = $baseWhere . "and type = 33 and mark <= " . $frame;
    $innerOrderBy = "order by chanid desc, starttime desc, type desc, mark desc LIMIT 1";
    $innerQuery = "select offset from recordedseek" . "\n" . $innerWhere . "\n" . $innerOrderBy;
    if (isShowQuery())
      echo "inner query:\n" . $innerQuery . "\n\n";
    $innerRes = mysql_query($innerQuery) or die(error("Query failed: " . mysql_error()));
    if (mysql_numrows($innerRes) == 1)
    {
      $offset = mysql_result($innerRes, 0, "offset");
      printCutting($markType, $offset);
    if ($i < $num - 1)
      echo ', ' . "\n";
    }
    $i++;
  }
  
  echo '    ]' . "\n";
  echo '  }' . "\n";
  echo '}';
  
  mysql_close();
}
else if ($type->isSearch())
{
  // search request
  $videoBase = null;
  if (hasStorageGroup($VIDEO_STORAGE_GROUP))
    $isVideoStorageGroup = true;
  else
    $videoBase = getSettingDir($VIDEO_DIR_SETTING);
  
  $musicBase = getSettingDir($MUSIC_DIR_SETTING);
  $artworkBase = getBaseDir($artworkStorageGroup, $ARTWORK_DIR_SETTING);
  
  header("Content-type:application/json");
  echo "{\n";
  echo '  "summary": ' . "\n";
  echo '  { "date": "' . $dt . ' UTC", "query": "' . $searchQuery . '"';
  if ($videoBase != null)
    echo ', "videoBase": "' . $videoBase . '"';
  if ($musicBase != null)
    echo ', "musicBase": "' . $musicBase . '"';
  echo ' },' . "\n";
  
  if ($isVideoStorageGroup || $videoBase !== null)
  {
    // videos
    $vWhere = "where 1=1";
    if ($categorizeUsingDirs)
      $vWhere = $vWhere . " and (" . notLike($videoBase, array_merge(array_merge($videoExcludeDirs,$movieDirs),$tvSeriesDirs)) . ")";
    else if ($categorizeUsingMetadata)
      $vWhere = $vWhere . " and (inetref is null or inetref = '00000000') and (season is null or season = '0') and (episode is null or episode = '0')";
    $vWhere = $vWhere . " and filename like '%" . $searchQuery . "%'";
    $vQuery = "select intid as id, filename from videometadata " . $vWhere . " order by filename";
    if (isShowQuery())
      echo "vQuery: " . $vQuery . "\n\n";
    mysql_set_charset("utf8");
    $vRes = mysql_query($vQuery) or die(error("Query failed: " . mysql_error()));
    $vNum = mysql_numrows($vRes);
    echo '  "videos": ' . "\n  [\n";
    $i = 0;
    while ($i < $vNum)
    {
      $id = mysql_result($vRes, $i, "id");
      $full = mysql_result($vRes, $i, "filename");
      if ($isVideoStorageGroup)
        $part = $full;
      else
        $part = substr($full, strlen($videoBase) + 1);
      $lastSlash = strrpos($part, "/");
      $path = substr($part, 0, $lastSlash);
      $file = empty($path) ? $part : substr($part, $lastSlash + 1);
      printSearchResult($id, $path, $file, $i < $vNum - 1);
      $i++;
    }
  
    if ($categorizeUsingDirs || $categorizeUsingMetadata)
    {
      // movies
      $castMap = getCastMap();
      echo "  ],\n";
      $mWhere = "where 1=1";
      if ($categorizeUsingDirs)
        $mWhere = $mWhere . " and (" . like($videoBase, $movieDirs) . ")";
      else
        $mWhere = $mWhere . " and ((inetref is not null and inetref != '00000000') and (episode is null or episode = '0') and (season is null or season = '0'))";
      $mWhere = $mWhere . " and (filename like '%" . $searchQuery . "%' or year like '%" . $searchQuery . "%' or director like '%" . $searchQuery . "%' or plot like '%" . $searchQuery . "%')";
      $mQuery = "select intid as id, title, filename, inetref, homepage, year, userrating, director, plot as summary, coverfile, fanart, screenshot, banner from videometadata " . $mWhere . " order by filename";
      if (isShowQuery())
        echo "mQuery: " . $mQuery . "\n\n";
      $mRes = mysql_query($mQuery) or die(error("Query failed: " . mysql_error()));
      $mNum = mysql_numrows($mRes);
      echo '  "movies": ' . "\n  [\n";
      $i = 0;
      while ($i < $mNum)
      {
        $id = mysql_result($mRes, $i, "id");
        $title = cleanup(mysql_result($mRes, $i, "title"));
        $full = mysql_result($mRes, $i, "filename");
        if ($isVideoStorageGroup)
          $part = $full;
        else
          $part = substr($full, strlen($videoBase) + 1);
        $lastSlash = strrpos($part, "/");
        $path = substr($part, 0, $lastSlash);
        $file = substr($part, $lastSlash + 1);
        $inetref = mysql_result($mRes, $i, "inetref");
        $homepage = mysql_result($mRes, $i, "homepage");
        $year = mysql_result($mRes, $i, "year");
        $rating = mysql_result($mRes, $i, "userrating");
        $director = mysql_result($mRes, $i, "director");
        $summary = cleanup(mysql_result($mRes, $i, "summary"));
        $art = mysql_result($mRes, $i, "coverfile");
        if ($art == null || (strcmp($artworkStorageGroup, 'Fanart') == 0))
          $art = mysql_result($mRes, $i, "fanart");
        if ($art == null || (strcmp($artworkStorageGroup, 'Screenshots') == 0))
          $art = mysql_result($mRes, $i, "screenshot");
        if ($art == null || (strcmp($artworkStorageGroup, 'Banners') == 0))
          $art = mysql_result($mRes, $i, "banner");
        $artwork = $art == null || $artworkBase == null ? null : (startsWith($art, $artworkBase) ? substr($art, strlen($artworkBase) + 1) : $art);
        $actors = array_key_exists($id, $castMap) ? $castMap[$id] : null;
        printSearchResultMovieOrTvSeries($id, $title, null, null, null, $path, $file, $inetref, $homepage, $year, null, $rating, $director, $actors, $summary, $artwork, $i < $mNum - 1);
        $i++;
      }
  
      // tv series'
      echo "  ],\n";
      $tsWhere = "where 1=1";
      if ($categorizeUsingDirs)
        $tsWhere = $tsWhere . " and (" . like($videoBase, $tvSeriesDirs) . ")";
      else
        $tsWhere = $tsWhere . " and ((season is not null and season != '0') or (episode is not null and episode != '0'))";
      $tsWhere = $tsWhere . " and (filename like '%" . $searchQuery . "%' or subtitle like '%" . $searchQuery . "%' or director like '%" . $searchQuery . "%' or plot like '%" . $searchQuery . "%')";
      $tsQuery = "select intid as id, title, subtitle, season, episode, filename, inetref, homepage, releasedate, userrating, director, plot as summary, coverfile, fanart, screenshot, banner from videometadata " . $tsWhere . " order by filename";
      if (isShowQuery())
        echo "tsQuery: " . $tsQuery . "\n\n";
      $tsRes = mysql_query($tsQuery) or die(error("Query failed: " . mysql_error()));
      $tsNum = mysql_numrows($tsRes);
      echo '  "tvSeries": ' . "\n  [\n";
      $i = 0;
      while ($i < $tsNum)
      {
        $id = mysql_result($tsRes, $i, "id");
        $title = cleanup(mysql_result($tsRes, $i, "title"));
        $subTitle = cleanup(mysql_result($tsRes, $i, "subtitle"));
        $season = mysql_result($tsRes, $i, "season");
        $episode = mysql_result($tsRes, $i, "episode");
        $full = mysql_result($tsRes, $i, "filename");
        if ($isVideoStorageGroup)
          $part = $full;
        else
          $part = substr($full, strlen($videoBase) + 1);
        $lastSlash = strrpos($part, "/");
        $path = substr($part, 0, $lastSlash);
        $file = substr($part, $lastSlash + 1);
        $inetref = mysql_result($tsRes, $i, "inetref");
        $homepage = mysql_result($tsRes, $i, "homepage");
        $aired = mysql_result($tsRes, $i, "releasedate");
        $rating = mysql_result($tsRes, $i, "userrating");
        $director = mysql_result($tsRes, $i, "director");
        $summary = cleanup(mysql_result($tsRes, $i, "summary"));
        $art = mysql_result($tsRes, $i, "coverfile");
        if ($art == null || (strcmp($artworkStorageGroup, 'Fanart') == 0))
          $art = mysql_result($tsRes, $i, "fanart");
        if ($art == null || (strcmp($artworkStorageGroup, 'Screenshots') == 0))
          $art = mysql_result($tsRes, $i, "screenshot");
        if ($art == null || (strcmp($artworkStorageGroup, 'Banners') == 0))
          $art = mysql_result($tsRes, $i, "banner");
        $artwork = $art == null || $artworkBase == null ? null : (startsWith($art, $artworkBase) ? substr($art, strlen($artworkBase) + 1) : $art);
        $actors = array_key_exists($id, $castMap) ? $castMap[$id] : null;
        printSearchResultMovieOrTvSeries($id, $title, $subTitle, $season, $episode, $path, $file, $inetref, $homepage, null, $aired, $rating, $director, $actors, $summary, $artwork, $i < $tsNum - 1);
        $i++;
      }
    }
     
    echo "  ],\n";
  }
  
  // music
  if ($musicBase != null)
  {
    $sQuery = "select s.song_id as id, concat(concat(d.path,'/'),s.filename) as filename from music_directories d, music_songs s where d.directory_id = s.directory_id and (d.path like '%" . $searchQuery . "%' or s.filename like '%" . $searchQuery . "%') order by filename";
    if (isShowQuery())
      echo "sQuery: " . $sQuery . "\n\n";
    $sRes = mysql_query($sQuery) or die(error("Query failed: " . mysql_error()));
    $sNum = mysql_numrows($sRes);
    echo '  "songs": ' . "\n  [\n";
    $i = 0;
    while ($i < $sNum)
    {
      $id = mysql_result($sRes, $i, "id");
      $full = mysql_result($sRes, $i, "filename");
      $lastSlash = strrpos($full, "/");
      $path = substr($full, 0, $lastSlash);
      $file = substr($full, $lastSlash + 1);
      if (strcmp($path, $musicBase) == 0)
        $path = "";
      printSearchResult($id, $path, $file, $i < $sNum - 1);
      $i++;
    }
    echo "  ],\n";
  }
  
  // liveTv
  $tQuery = "select concat(concat(p.chanid,'~'),p.starttime) as id, c.channum, c.callsign, p.endtime, p.title, p.subtitle, p.description, p.stars, convert(p.originalairdate using utf8) as oad, p.airdate from program p, channel c where p.chanid = c.chanid and starttime <= utc_timestamp() and endtime >= utc_timestamp() and (p.title like '%" . $searchQuery . "%' or p.subtitle like '%" . $searchQuery . "%' or p.description like '%" . $searchQuery . "%') group by p.programid order by p.chanid";
  if (isShowQuery())
    echo "tQuery: " . $tQuery . "\n\n";
  $tRes = mysql_query($tQuery) or die(error("Query failed: " . mysql_error()));
  $tNum = mysql_numrows($tRes);
  echo '  "liveTv": ' . "\n  [\n";
  $i = 0;
  while ($i < $tNum)
  {
    $id = mysql_result($tRes, $i, "id");
    $progstart = null; // always identifed by id
    $channum = mysql_result($tRes, $i, "channum");
    $callsign = mysql_result($tRes, $i, "callsign");
    $title = cleanup(mysql_result($tRes, $i, "title"));
    $subtitle = cleanup(mysql_result($tRes, $i, "subtitle"));
    $description = cleanup(mysql_result($tRes, $i, "description"));
    $rating = mysql_result($tRes, $i, "stars");
    $oads = mysql_result($tRes, $i, "oad");
    if ($oads != null && strcmp("0000-00-00", $oads) != 0)
    {
      $airdate = $oads;
    }
    else
    {
      $oads = mysql_result($tRes, $i, "airdate");
      if (strcmp("0000", $oads) != 0)
        $airdate = $oads;
      else
        $airdate = null;
    }
    $endtime = mysql_result($tRes, $i, "endtime");
    printSearchResultRecordingOrLiveTv($id, $progstart, $channum, $callsign, $title, null, $subtitle, $description, $rating, null, $airdate, null, null, $endtime, null, $i < $tNum - 1);
    $i++;
  }
  echo "  ],\n";
  
  // recordings
  $rQuery = "select distinct concat(concat(r.chanid,'~'),r.starttime) as id, r.progstart, c.channum, c.callsign, r.title, r.basename, r.subtitle, r.description, r.stars, r.storagegroup, convert(r.originalairdate using utf8) as oad, rp.airdate, r.season, r.episode, r.endtime, orec.recstatus";
  $rQuery .= " from channel c, recordedprogram rp, recorded r left outer join oldrecorded orec on (orec.chanid = r.chanid and orec.starttime = r.progstart) where r.recgroup != 'Deleted' and r.recgroup != 'LiveTV' and r.chanid = c.chanid and r.programid = rp.programid";
  $rQuery .= " and (r.title like '%" . $searchQuery . "%' or r.subtitle like '%" . $searchQuery . "%' or r.description like '%" . $searchQuery . "%') order by trim(leading 'A ' from trim(leading 'An ' from trim(leading 'The ' from r.title))), r.starttime desc";
  if (isShowQuery())
    echo "rQuery: " . $rQuery . "\n\n";
  $rRes = mysql_query($rQuery) or die(error("Query failed: " . mysql_error()));
  $rNum = mysql_numrows($rRes);
  echo '  "recordings": ' . "\n  [\n";
  $i = 0;
  while ($i < $rNum)
  {
    $id = mysql_result($rRes, $i, "id");
    $progstart = mysql_result($rRes, $i, "progstart");
    $channum = mysql_result($rRes, $i, "channum");
    $callsign = mysql_result($rRes, $i, "callsign");
    $title = cleanup(mysql_result($rRes, $i, "title"));
    $basename = mysql_result($rRes, $i, "basename");
    $subtitle = cleanup(mysql_result($rRes, $i, "subtitle"));
    $description = cleanup(mysql_result($rRes, $i, "description"));
    $rating = mysql_result($rRes, $i, "stars");
    $storagegroup = mysql_result($rRes, $i, "storagegroup");
    $oads = mysql_result($rRes, $i, "oad");
    if ($oads != null && strcmp("0000-00-00", $oads) != 0)
    {
      $airdate = $oads;
    }
    else
    {
      $oads = mysql_result($rRes, $i, "airdate");
      if (strcmp("0000", $oads) != 0)
        $airdate = $oads;
      else
        $airdate = null;
    }
    $season = mysql_result($rRes, $i, "season");
    $episode = mysql_result($rRes, $i, "episode");
    $endtime = mysql_result($rRes, $i, "endtime");
    $recstatus = mysql_result($rRes, $i, "recstatus");
    printSearchResultRecordingOrLiveTv($id, $progstart, $channum, $callsign, $title, $basename, $subtitle, $description, $rating, $storagegroup, $airdate, $season, $episode, $endtime, $recstatus, $i < $rNum - 1);
    $i++;
  }
  echo "  ]\n}";
  
  mysql_close();
}
else
{
  // non-search request
  header("Content-type:application/json");
  $base = null;
  $sort = 'title';
  if (array_key_exists('sort', $_REQUEST))
    $sort = $_REQUEST['sort'];
  if ($type->isVideos() || $type->isMovies() || $type->isTvSeries())
  {
    if (hasStorageGroup($VIDEO_STORAGE_GROUP))
      $isVideoStorageGroup = true;
    else
      $base = getSettingDir($VIDEO_DIR_SETTING);
  
    if (!$isVideoStorageGroup && $base == null)
      die(error("No Videos storage group and no " . $VIDEO_DIR_SETTING . " setting for videos"));
  
    $where = "";
  
    if ($type->isVideos())
    {
      if ($categorizeUsingDirs)
        $where = "where (" . notLike($base, array_merge(array_merge($videoExcludeDirs,$movieDirs),$tvSeriesDirs)) . ")";
      else if ($categorizeUsingMetadata)
        $where = "where (inetref is null or inetref = '00000000') and ((season is null or season = '0') and (episode is null or episode = '0'))";
      // one option
      $orderBy = "order by replace(replace(replace(trim(leading 'A ' from trim(leading 'An ' from trim(leading 'The ' from filename))), '/A ', '/'), '/An ', '/'), '/The ', '/')";
      $query = "select intid as id, filename from videometadata " . $where . " " . $orderBy;
    }
    else
    {
      $castMap = getCastMap();
      if ($categorizeUsingDirs)
      {
        if ($type->isMovies())
          $where = "where (" . like($base, $movieDirs) . ")";
        else if ($type->isTvSeries())
          $where = "where (" . like($base, $tvSeriesDirs) . ")";
      }
      else if ($categorizeUsingMetadata)
      {
        if ($type->isMovies())
          $where = "where (inetref is not null and inetref != '00000000') and (season is null or season = '0') and (episode is null or episode = '0')";
        else if ($type->isTvSeries())
          $where = "where (season is not null and season != '0') or (episode is not null and episode != '0')";
      }
      else
      {
        $where = "where 1 = 0"; // can't determine movies or tv series
      }
      if ($sort == "date")
      {
        if ($type->isMovies())
          $orderBy = "order by year, replace(replace(replace(trim(leading 'A ' from trim(leading 'An ' from trim(leading 'The ' from filename))), '/A ', '/'), '/An ', '/'), '/The ', '/')";
        else if ($type->isTvSeries())
          $orderBy = "order by season, episode";
      }
      else if ($sort == "rating")
      {
        if ($type->isMovies())
          $orderBy = "order by userrating desc, replace(replace(replace(trim(leading 'A ' from trim(leading 'An ' from trim(leading 'The ' from filename))), '/A ', '/'), '/An ', '/'), '/The ', '/')";
        else if ($type->isTvSeries())
          $orderBy = "order by userrating desc, replace(replace(replace(trim(leading 'A ' from trim(leading 'An ' from trim(leading 'The ' from filename))), '/A ', '/'), '/An ', '/'), '/The ', '/')";
      }
      else
      {
        $orderBy = "order by replace(replace(replace(trim(leading 'A ' from trim(leading 'An ' from trim(leading 'The ' from filename))), '/A ', '/'), '/An ', '/'), '/The ', '/')";
      }
  
      $query = "select intid as id, title, subtitle, filename, inetref, homepage, season, episode, year, releasedate, userrating, director, plot as summary, coverfile, fanart, screenshot, banner from videometadata " . $where . " " . $orderBy;
    }
  }
  else if ($type->isMusic())
  {
    $base = getSettingDir($MUSIC_DIR_SETTING);
    if ($base == null)
      die(error("No " . $MUSIC_DIR_SETTING . " setting for music"));
    $albumArtMap = getAlbumArtMap();
    $where = "where d.directory_id = s.directory_id";
    // one option
    $orderBy = "order by replace(replace(replace(trim(leading 'A ' from trim(leading 'An ' from trim(leading 'The ' from concat(concat(d.path,'/'),s.filename)))), '/A ', '/'), '/An ', '/'), '/The ', '/')";
    $query = "select s.song_id as id, s.directory_id, concat(concat(d.path,'/'),s.filename) as filename from music_directories d, music_songs s " . $where . " " . $orderBy;
  }
  else if ($type->isLiveTv())
  {
    $where = "where p.chanid = c.chanid and starttime <= utc_timestamp() and endtime >= utc_timestamp()";
    $groupBy = "group by p.programid"; // avoid dups when multiple recording sources
    if ($sort == "callsign")
      $orderBy = "order by c.callsign";
    else
      $orderBy = "order by cast(c.channum as unsigned)";
    $query = "select concat(concat(p.chanid,'~'),p.starttime) as id, c.channum, c.callsign, p.endtime, p.title, p.subtitle, p.description, p.stars, convert(p.originalairdate using utf8) as oad, p.airdate from program p, channel c " . $where . " " . $groupBy . " " . $orderBy;
  }
  else if ($type->isRecordings())
  {
    $where = "left outer join channel c on (r.chanid = c.chanid) inner join recordedprogram rp on (r.programid = rp.programid) left outer join record rr on (rr.chanid = r.chanid and rr.programid = r.programid)";
    $where .= " left outer join oldrecorded orec on (orec.chanid = r.chanid and orec.starttime = r.progstart) where r.recgroup != 'Deleted' and r.recgroup != 'LiveTV'";
    if ($sort == "date")
    {
      $orderBy = "order by r.starttime desc, trim(leading 'A ' from trim(leading 'An ' from trim(leading 'The ' from r.title)))";
      $groupRecordingsByTitle = false;
    }
    else if ($sort == "rating")
    {
      $orderBy = "order by r.stars desc, trim(leading 'A ' from trim(leading 'An ' from trim(leading 'The ' from r.title)))";
      $groupRecordingsByTitle = false;
    }
    else
    {
      $orderBy = "order by trim(leading 'A ' from trim(leading 'An ' from trim(leading 'The ' from r.title))), r.starttime desc";
      $groupRecordingsByTitle = !$flatten;
    }
    $query = "select distinct concat(concat(r.chanid,'~'),r.starttime) as id, r.progstart, c.channum, c.callsign, r.endtime, r.title, r.basename, r.subtitle, r.description, r.stars, r.inetref, convert(r.originalairdate using utf8) as oad, rp.airdate, r.season, r.episode, r.recordid, r.storagegroup, r.recgroup, orec.recstatus";
    $query .= " from recorded r " . $where . " " . $orderBy;
  }
  
  if (isShowQuery())
    echo "query: " . $query . "\n\n";
  
  mysql_set_charset("utf8");
  $result = mysql_query($query) or die(error("Query failed: " . mysql_error()));
  $num = mysql_numrows($result);
  $catPaths = array();
  $fileIds = array();
  if ($type->isRecordings() || $type->isLiveTv())
  {
    $progstarts = array();
    $titles = array();
    $channums = array();
    $callsigns = array();
    $basenames = array();
    $subtitles = array();
    $descriptions = array();
    $airdates = array();
    $endtimes = array();
    $ratings = array();
    if ($type->isRecordings())
    {
      $inetrefs = array();
      $recordids = array();
      $storagegroups = array();
      $recgroups = array();
      $recstatuses = array();
      $seasons = array();
      $episodes = array();
    }
  }
  else if ($type->isMovies() || $type->isTvSeries())
  {
    $titles = array();
    $subtitles = array();
    $inetrefs = array();
    $homepages = array();
    $seasons = array();
    $episodes = array();
    $airedDates = array();
    $years = array();
    $ratings = array();
    $directors = array();
    $actors = array();
    $summaries = array();
    $artworks = array();
    $hps = array();
  }
  else if ($type->isMusic())
  {
    $artworks = array();
  }
  $prevPath = "";
  $i = 0;
  while ($i < $num)
  {
    $id = mysql_result($result, $i, "id");
    if ($type->isMovies() || $type->isTvSeries())
    {
      $ttl = cleanup(mysql_result($result, $i, "title"));
      $stl = cleanup(mysql_result($result, $i, "subtitle"));
      $inr = mysql_result($result, $i, "inetref");
      $hp = mysql_result($result, $i, "homepage");
      $seas = mysql_result($result, $i, "season");
      $ep = mysql_result($result, $i, "episode");
      $yr = mysql_result($result, $i, "year");
      $aired = mysql_result($result, $i, "releasedate");
      $rt = mysql_result($result, $i, "userrating");
      $dir = mysql_result($result, $i, "director");
      $act = array_key_exists($id, $castMap) ? $castMap[$id] : null;
      $sum = mysql_result($result, $i, "summary");
      if (strcmp('None', $sum) == 0)
        $sum = null;
      else
        $sum = cleanup($sum);
      $art = mysql_result($result, $i, "coverfile");
      if ($art == null || (strcmp($artworkStorageGroup, 'Fanart') == 0))
        $art = mysql_result($result, $i, "fanart");
      if ($art == null || (strcmp($artworkStorageGroup, 'Screenshots') == 0))
        $art = mysql_result($result, $i, "screenshot");
      if ($art == null || (strcmp($artworkStorageGroup, 'Banners') == 0))
        $art = mysql_result($result, $i, "banner");
    }
  
    if ($type->isRecordings() || $type->isLiveTv())
    {
      if ($type->isRecordings())
        $progst = mysql_result($result, $i, "progstart");
      else
        $progst = null;
      $cn = mysql_result($result, $i, "channum");
      $cs = mysql_result($result, $i, "callsign");
      $tit = cleanup(mysql_result($result, $i, "title"));
      if ($type->isRecordings() && $groupRecordingsByTitle)
        $full = $tit . "/" . $tit;
      else
        $full = $tit;
      if ($type->isRecordings())
        $bn = mysql_result($result, $i, "basename");
      else
        $bn = null;
      $subtit = cleanup(mysql_result($result, $i, "subtitle"));
      $descrip = cleanup(mysql_result($result, $i, "description"));
      $oads = mysql_result($result, $i, "oad");
      if ($oads != null && strcmp("0000-00-00", $oads) != 0)
      {
        $oad = $oads;
      }
      else
      {
        $oads = mysql_result($result, $i, "airdate");
        if (strcmp("0000", $oads) != 0)
          $oad = $oads;
        else
          $oad = null;
      }
      $et = mysql_result($result, $i, "endtime");
      $rat = mysql_result($result, $i, "stars");
      if ($type->isRecordings())
      {
        $rid = mysql_result($result, $i, "recordid");
        $inr = mysql_result($result, $i, "inetref");
        $sg = mysql_result($result, $i, "storagegroup");
        $rg = mysql_result($result, $i, "recgroup");
        $rstat = mysql_result($result, $i, "recstatus");
        $seas = mysql_result($result, $i, "season");
        $ep = mysql_result($result, $i, "episode");
      }
    }
    else
    {
      $full = mysql_result($result, $i, "filename");
    }
  
    if ($type->isMusic())
    {
      $dirId = mysql_result($result, $i, "directory_id");
      if ($albumArtAlbumLevel)
        $art = array_key_exists($dirId, $albumArtMap) ? $albumArtMap[$dirId] : null;
      else
        $art = array_key_exists($id, $albumArtMap) ? $albumArtMap[$id] : null;
    }
  
    if ($type->isMusic() || $type->isRecordings() || $type->isLiveTv() || $isVideoStorageGroup)
      $part = $full;
    else
      $part = substr($full, strlen($base) + 1);
  
    // echo $part . "\n";
    $lastSlash = strrpos($part, "/");
    $path = substr($part, 0, $lastSlash);
    $file = $lastSlash ? substr($part, $lastSlash + 1) : $part;
    if ($type->isMusic() && strcmp($path, $base) == 0)
      $path = "";
    if (strcmp($prevPath, $path) != 0)
    {
      // make sure shorter segments are added first
      $pieces = explode("/", $path);
      $j = 0;
      $seg = "";
      while ($j < count($pieces))
      {
        $seg = $seg . $pieces[$j];
        if (!array_key_exists($seg, $catPaths))
          $catPaths[$seg] = array();
        $seg = $seg . "/";
        $j++;
      }
    }
  
    if (array_key_exists($path, $catPaths))
      $files = $catPaths[$path];
    else
      $files = array();
    if ($type->isRecordings() || $type->isLiveTv())
      $files[] = $id;  // $files actually contains ids
    else
      $files[] = $file;
    $catPaths[$path] = $files;
    $prevPath = $path;
    $fileIds[$path . '/' . $file] = $id;
    if ($type->isRecordings() || $type->isLiveTv())
    {
      $progstarts[$id] = $progst;
      $titles[$id] = $tit;
      $channums[$id] = $cn;
      $callsigns[$id] = $cs;
      $basenames[$id] = $bn;
      $subtitles[$id] = $subtit;
      $descriptions[$id] = $descrip;
      $airdates[$id] = $oad;
      $endtimes[$id] = $et;
      $ratings[$id] = $rat;
      if ($type->isRecordings())
      {
        $inetrefs[$id] = $inr;
        $recordids[$id] = $rid;
        $storagegroups[$id] = $sg;
        $recgroups[$id] = $rg;
        $recstatuses[$id] = $rstat;
        $seasons[$id] = $seas;
        $episodes[$id] = $ep;
      }
    }
    else if ($type->isMovies() || $type->isTvSeries())
    {
      $artworkBase = getBaseDir($artworkStorageGroup, $ARTWORK_DIR_SETTING);
      $titles[$id] = $ttl;
      $subtitles[$id] = $stl;
      $years[$id] = $yr;
      $airedDates[$id] = $aired;
      $inetrefs[$id] = $inr;
      $homepages[$id] = $hp;
      $seasons[$id] = $seas;
      $episodes[$id] = $ep;
      $ratings[$id] = $rt;
      $directors[$id] = $dir;
      $actors[$id] = $act;
      $summaries[$id] = $sum;
      $artworks[$id] = $art == null || $artworkBase == null ? null : (startsWith($art, $artworkBase) ? substr($art, strlen($artworkBase) + 1) : $art);
      $hps[$id] = $hp;
    }
    else if ($type->isMusic())
    {
      $artworks[$id] = $art;
    }
  
    $i++;
  }
  
  mysql_close();
  
  echo "{\n";
  echo '  "summary": ' . "\n";
  echo '  { "type": "' . $type->type . '", "date": "' . $dt . ' UTC", "count": "' . $num . '"';
  if ($base != null)
    echo ', "base": "' . $base . '" ';
  if ($num > 0)
    echo " },\n";
  else
    echo " }\n";
  
  // echo print_r($catPaths) . "\n";
  
  $i = 0;
  $depth = 0;
  $prevPath = "";
  $size = 0;
  $prevSize = 0;
  $prevHadItems = false;
  $keys = array_keys($catPaths);
  $keyCt = count($keys);
  $hasTopLevelItems = array_key_exists("", $catPaths);
  $hasCats = $hasTopLevelItems ? $keyCt > 1 : $keyCt > 0;
  // top-level items
  if ($hasTopLevelItems)
  {
    printItems("", $catPaths[""], 0);
    if ($hasCats)
      echo ",";
    echo "\n";
  }
  // categories
  if ($hasCats)
  {
    while ($i < $keyCt)
    {
      $path = $keys[$i];
      if ($path != "")
      {
        $files = $catPaths[$path];
        $fileCt = count($files);
  
        // echo $path . "($fileCt)\n";
        $pieces = explode("/", $path);
        $size = count($pieces);
        // echo "size: " . $size . "  prevSize: " . $prevSize . "\n";
  
        $piece = $pieces[$size - 1];
        $hasItems = $fileCt > 0;
  
        if ($size > $prevSize)
        {
          printSubCatsBegin($depth, $prevHadItems);
          $depth = $depth + ($size - $prevSize);
        }
        if ($size < $prevSize)
        {
          $j = 0;
          while ($j < ($prevSize - $size))
          {
            printCatEnd($depth, false);
            $depth--;
            printSubCatsEnd($depth, $prevHadItems);
            $j++;
          }
          printCatEnd($depth, true);
        }
        if ($size == $prevSize && $prevPath)
        {
          printCatEnd($depth, true);
        }
  
        if ($path)
        {
          printCatBegin($piece, $depth);
          if ($fileCt > 0)
            printItems($path, $files, $depth);
        }
  
        $prevSize = $size;
      }
      else
      {
        $prevSize = $size;
        $size = 0;
        $hasItems = false;
      }
  
      $prevPath = $path;
      $prevHadItems = $hasItems;
      $i++;
    }
  }
  
  while ($depth > 0)
  {
    printCatEnd($depth, false);
    $depth--;
    printSubCatsEnd($depth, $prevHadItems);
    $prevHadItems = false;
  }
  echo "}\n";
}

function isShowQuery()
{
  return isset($_REQUEST['showQuery']) && strtoupper($_REQUEST['showQuery']) == 'TRUE';  
}

function printCatBegin($name, $depth)
{
  echo indent(2 + ($depth - 1) * 4);
  echo "{\n";
  echo indent($depth * 4);
  echo '"name": "' . $name . '"';
  echo ",\n";
}

function printSubCatsBegin($depth, $hasItems)
{
  if ($hasItems)
    echo ",\n";
  echo indent($depth * 4);
  echo '"categories":' . "\n";
  echo indent($depth * 4);
  echo "[\n";
}

function printSubCatsEnd($depth, $hasItems)
{
  echo indent($depth * 4);
  echo "]";
  if (!$hasItems)
    echo "\n";
}

function printCatEnd($depth, $more)
{
  echo "\n";
  echo indent(2 + ($depth - 1) * 4);
  echo "}";
  if ($more)
    echo ",";
  echo "\n";
}

function printItems($path, $files, $depth)
{
  $fileCt = count($files);
  printItemsBegin($depth);
  $j = 0;
  while ($j < $fileCt)
  {
    printItem($path, $files[$j], $depth, $j < $fileCt - 1);
    $j++;
  }
  printItemsEnd($depth);
}

function printItemsBegin($depth)
{
  echo indent($depth * 4);
  echo '"items":';
  echo "\n";
  echo indent($depth * 4);
  echo "[\n";
}

function printItem($path, $file, $depth, $more)
{
  global $type, $fileIds, $progstarts, $channums, $callsigns, $basenames, $titles, $subtitles, $descriptions, $airdates, $endtimes, $recordids, $storagegroups, $recgroups, $recstatuses, $subtitles, $inetrefs, $homepages, $seasons, $episodes, $years, $ratings, $directors, $actors, $summaries, $artworks;

  echo indent($depth * 4 + 2);

  if ($type->isRecordings() || $type->isLiveTv())
    $id = $file; // file is actually id
  else
    $id = $fileIds[$path . '/' . $file];

  if ($type->isMusic())
  {
    $lastdot = strrpos($file, ".");
    $title = substr($file, 0, $lastdot);
    $filetype = substr($file, $lastdot + 1);
  }
  else if ($type->isRecordings() || $type->isLiveTv())
  {
    $title = $titles[$id];
    $filetype = null;
  }
  else
  {
    $lastdot = strrpos($file, ".");
    $title = substr($file, 0, $lastdot);
    $filetype = substr($file, $lastdot + 1);
    if (!($type->isVideos()))
      $title = $titles[$id];
  }

  echo "{ ";
  echo '"id": "' . $id . '"';
  echo ', "title": "' . $title . '"';
  if ($filetype != null)
    echo ', "format": "' . $filetype . '"';
  if ($type->isRecordings() || $type->isLiveTv())
  {
    if ($progstarts[$id] != null)
      echo ', "programStart": "' . $progstarts[$id] . '"';
    if ($channums[$id] != null)
      echo ', "channel": "' . $channums[$id] . '"';
    if ($callsigns[$id] != null)
      echo ', "callsign": "' . $callsigns[$id] . '"';
    if ($basenames[$id] != null)
    {
      $basename = $basenames[$id];
      $lastdot = strrpos($basename, ".");
      $filename = substr($basename, 0, $lastdot);
      $format = substr($basename, $lastdot + 1);
      echo ', "file": "' . $filename . '", "format": "' . $format . '"';
    }
    if ($subtitles[$id] != null)
      echo ', "subtitle": "' . $subtitles[$id] . '"';
    if ($descriptions[$id] != null)
      echo ', "description": "' . $descriptions[$id] . '"';
    if ($airdates[$id] != null)
      echo ', "airdate": "' . $airdates[$id] . '"';
    if ($endtimes[$id] != null)
      echo ', "endtime": "' . $endtimes[$id] . '"';
    if ($ratings[$id] != null && $ratings[$id] != '0')
      echo ', "rating": "' . $ratings[$id] . '"';
    if ($type->isRecordings())
    {
      if ($inetrefs[$id] != null && $inetrefs[$id] != '00000000')
        echo ', "internetRef": "' . $inetrefs[$id] . '"';
      if ($recordids[$id] != null)
        echo ', "recordId": ' . $recordids[$id];
      if ($storagegroups[$id] != null)
        echo ', "storageGroup": "' . $storagegroups[$id] . '"';
      if ($recgroups[$id] != null)
        echo ', "recGroup": "' . $recgroups[$id] . '"';
      if ($seasons[$id] != null && $seasons[$id] != '0')
        echo ', "season": "' . $seasons[$id] . '"';
      if ($episodes[$id] != null && $episodes[$id] != '0')
        echo ', "episode": "' . $episodes[$id] . '"';
      if ($recgroups[$id] != null)
        echo ', "recStatus": "' . $recstatuses[$id] . '"';
    }
  }
  else if ($type->isMovies() || $type->isTvSeries())
  {
    echo ', "file": "' . substr($file, 0, strrpos($file, ".")) . '"';
    if ($subtitles[$id] != null)
      echo ', "subtitle": "' . $subtitles[$id] . '"';
    if ($inetrefs[$id] != null && $inetrefs[$id] != '00000000')
      echo ', "internetRef": "' . $inetrefs[$id] . '"';
    if ($homepages[$id] != null)
      echo ', "pageUrl": "' . $homepages[$id] . '"';
    if ($seasons[$id] != null && $seasons[$id] != '0')
      echo ', "season": "' . $seasons[$id] . '"';
    if ($episodes[$id] != null && $episodes[$id] != '0')
      echo ', "episode": "' . $episodes[$id] . '"';
    if ($years[$id] != null && $years[$id] != '1895')
      echo ', "year": "' . $years[$id] . '"';
    if ($airdates[$id] != null && $airdates[$id] != '0000-00-00')
      echo ', "aired": "' . $airdates[$id] . '"';
    if ($ratings[$id] != null && $ratings[$id] != '0')
      echo ', "rating": "' . $ratings[$id] . '"';
    if ($directors[$id] != null && $directors[$id] != 'Unknown')
      echo ', "director": "' . $directors[$id] . '"';
    if ($actors[$id] != null)
      echo ', "actors": "' . $actors[$id] . '"';
    if ($summaries[$id] != null)
      echo ', "summary": "' . str_replace('"', '\"', $summaries[$id]) . '"';
    if ($artworks[$id] != null)
      echo ', "artwork": "' . $artworks[$id] . '"';
  }
  if ($type->isMusic())
  {
    if ($artworks[$id] != null)
      echo ', "albumArtId": "' . $artworks[$id] . '"';
  }
  echo " }";
  if ($more)
    echo ",";
  echo "\n";
}

function printSearchResult($id, $path, $file, $more)
{
  $lastdot = strrpos($file, ".");
  $title = substr($file, 0, $lastdot);
  $filetype = substr($file, $lastdot + 1);

  echo "    { ";
  echo '"id": "' . $id . '", ';
  echo '"path": "' . $path . '", ';
  echo '"title": "' . $title . '", ';
  echo '"format": "' . $filetype . '" } ';
  if ($more)
    echo ",";
  echo "\n";
}

function printSearchResultRecordingOrLiveTv($id, $progstart, $channum, $callsign, $title, $basename, $subtitle, $description, $rating, $storagegroup, $airdate, $season, $episode, $endtime, $recstatus, $more)
{
  echo "    { ";
  echo '"id": "' . $id . '"';
  if ($progstart != null)
    echo ', "programStart": "' . $progstart . '"';
  echo ', "title": "' . $title . '"';
  if ($channum)
    echo ', "channel": "' . $channum . '"'; 
  if ($callsign)
    echo ', "callsign": "' . $callsign . '"'; 
  if ($basename)
  {
    $lastdot = strrpos($basename, ".");
    $filename = substr($basename, 0, $lastdot);
    $format = substr($basename, $lastdot + 1);
    echo ', "file": "' . $filename . '", "format": "' . $format . '"';
  }
  if ($subtitle)
    echo ', "subtitle": "' . $subtitle . '"';
  if ($description)
    echo ', "description": "' . $description . '"';
  if ($rating)
    echo ', "rating": "' . $rating . '"';
  if ($storagegroup)
    echo ', "storageGroup": "' . $storagegroup . '"';
  if ($airdate)
    echo ', "airdate": "' . $airdate . '"';
  if ($season && $season != '0')
    echo ', "season": "' . $season . '"';
  if ($episode && $episode != '0')
    echo ', "episode": "' . $episode . '"';  
  if ($endtime)
    echo ', "endtime": "' . $endtime . '"';
  if ($recstatus)
    echo ', "recStatus": "' . $recstatus . '"';
  echo " }";
  if ($more)
    echo ",";
  echo "\n";
}

function printSearchResultMovieOrTvSeries($id, $title, $subtitle, $season, $episode, $path, $file, $inetref, $pageUrl, $year, $aired, $rating, $director, $actors, $summary, $artwork, $more)
{
  $lastdot = strrpos($file, ".");
  $filetype = substr($file, $lastdot + 1);
  $filename = substr($file, 0, $lastdot);

  echo "    { ";
  echo '"id": "' . $id . '", ';
  echo '"path": "' . $path . '", ';
  echo '"title": "' . $title . '", ';
  echo '"file": "' . $filename . '", ';
  echo '"format": "' . $filetype . '"';
  
  if ($subtitle)
    echo ', "subtitle": "' . $subtitle . '"';
  if ($inetref && $inetref != '00000000')
    echo ', "internetRef": "' . $inetref . '"';
  if ($pageUrl)
    echo ', "pageUrl": "' . $pageUrl . '"';
  if ($season && $season != '0')
    echo ', "season": "' . $season . '"';
  if ($episode && $episode != '0')
    echo ', "episode": "' . $episode . '"';
  if ($year && $year != '1895')
    echo ', "year": "' . $year . '"';
  if ($aired && $aired != '0000-00-00')
    echo ', "aired": "' . $aired . '"';
  if ($rating && $rating != '0')
    echo ', "rating": "' . $rating . '"';
  if ($director && $director != 'Unknown')
    echo ', "director": "' . $director . '"';
  if ($actors)
    echo ', "actors": "' . $actors . '"';
  if ($summary && strcmp('None', $summary) != 0)
    echo ', "summary": "' . str_replace('"','\"',$summary) . '"';
  if ($artwork && $artwork != null)
    echo ', "artwork": "' . $artwork . '"';
  echo " }";
  if ($more)
    echo ",";
  echo "\n";
}

function printItemsEnd($depth)
{
  echo indent($depth * 4);
  echo "]";
}

function printChannelBegin($chanId, $chanNum, $callSign, $icon)
{
  echo '      { "CallSign": "' . $callSign . '"';
  echo ', "ChanId": "' . $chanId . '"';
  echo ', "ChanNum": "' . $chanNum . '"';
  if ($icon)
    echo ', "IconURL": "/Guide/GetChannelIcon?ChanId=' . $chanId . '"';
  echo ",\n";
  echo '        "Programs": ' . "\n";
  echo '        [' . "\n";
}

function printChannelEnd($last)
{
  echo "\n" . '        ]' . "\n"; // program array
  echo '      }';
  if (!$last)
    echo ',';
  echo "\n";
}

function printProgram($type, $category, $startTime, $endTime, $title, $subTitle, $repeat, $recStatus, $chanId, $callSign, $chanNum)
{
  echo '          { ';
  echo '"CatType": "' . $type . '"';
  echo ', "Category": "' . $category . '"';
  echo ', "EndTime": "' . $endTime . '"';
  echo ', "Recording": { "Status": "' . $recStatus . '" }';
  echo ', "Repeat": "' . ($repeat == 0 ? 'false' : 'true') . '"';
  echo ', "StartTime": "' . $startTime . '"';
  echo ', "SubTitle": "' . $subTitle . '"';
  echo ', "Title": "' . $title . '"';
  if ($chanId != null)
    echo ', "Channel": { "ChanId": "' . $chanId . '", "CallSign": "' . $callSign . '", "ChanNum": "' . $chanNum . '" }';
  echo ' }';
}

function printCutting($markType, $offset)
{
  echo '          { ';
  echo '"Mark": "' . $markType . '"';
  echo ', "Offset": "' . $offset . '"';
  echo ' }';
}

function indent($depth)
{
  $indent = "  ";
  for ($i = 0; $i < $depth; $i++)
    $indent = $indent . " ";
  return $indent;
}  

function startsWith($haystack, $needle)
{
  $length = strlen($needle);
  return (substr($haystack, 0, $length) === $needle);
}

function notLike($base, $dirs)
{
  $notLike = "1=1 ";
  $dirsSize = count($dirs);
  for ($i = 0; $i < $dirsSize; $i++)
  {
    $notLike = $notLike . "and ";
    $notLike = $notLike . " filename not like '";
    if ($base != null)
      $notLike = $notLike . $base . "/";
    $notLike = $notLike . $dirs[$i] . "%' ";
  }
  return $notLike;
}

function like($base, $dirs)
{
  $like = "1=0 ";
  $dirsSize = count($dirs);
  for ($i = 0; $i < $dirsSize; $i++)
  {
    $like = $like . "or ";
    $like = $like . " filename like '";
    if ($base != null)
      $like = $like . $base . "/";
    $like = $like . $dirs[$i] . "%' ";
  }
  return $like;
}

function getBaseDir($storageGroup, $setting)
{
  $baseDir = getStorageGroupDir($storageGroup);
  if ($baseDir == null)
    return getSettingDir($setting);
  else
    return $baseDir;
}

function getStorageGroupDir($group)
{
  global $hostname;
  $query = "select dirname from storagegroup where hostname = '" . $hostname . "' and groupname = '" . $group . "'";
  $result = mysql_query($query) or die ("Query failed: " . mysql_error());
  $dir = mysql_numrows($result) > 0 ? mysql_result($result, 0, "dirname") : null;
  if ($dir != null)
    return trimTrailingSlash($dir);
}

function hasStorageGroup($group)
{
  global $hostname;
  $query = "select dirname from storagegroup where hostname = '" . $hostname . "' and groupname = '" . $group . "'";
  $result = mysql_query($query) or die (error("Query failed: " . mysql_error()));
  return mysql_numrows($result) > 0;
}

function getSettingDir($setting)
{
  global $hostname;
  $query = "select data from settings where hostname = '" . $hostname . "' and value = '" . $setting . "'";
  $result = mysql_query($query) or die(error("Query failed: " . mysql_error()));
  $dir = mysql_numrows($result) > 0 ? mysql_result($result, 0, "data") : null;
  if ($dir != null)
    return trimTrailingSlash($dir);
}

function trimTrailingSlash($str)
{
  $len = strlen($str);
  if (strpos($str, '/', $len - 1))
    return substr($str, 0, $len - 1);
  else
    return $str;
}

function getCastMap()
{
  $castMap = array();
  $query = "select vmdc.idvideo, group_concat(t.cast SEPARATOR ', ') as actors from videometadatacast vmdc, (select * from videocast) t where t.intid = vmdc.idcast group by vmdc.idvideo";
  mysql_set_charset("utf8");
  $res = mysql_query($query) or die(error("Query failed: " . mysql_error()));
  $num = mysql_numrows($res);
  $i = 0;
  while ($i < $num)
  {
    $id = mysql_result($res, $i, "idvideo");
    $cast = mysql_result($res, $i, "actors");
    $castMap[$id] = $cast;
    $i++;
  }
  return $castMap;
}

function getAlbumArtMap()
{
  global $albumArtAlbumLevel;
  $albumArtMap = array();
  if ($albumArtAlbumLevel)
  {
    // map relates directory_id to albumart_id
    $query = "select albumart_id, directory_id from music_albumart where song_id = 0";
  }
  else
  {
    $query = "select albumart_id, song_id from music_albumart where song_id != 0";
  }
  mysql_set_charset("utf8");  
  $res = mysql_query($query) or die(error("Query failed: " . mysql_error()));
  $num = mysql_numrows($res);
  $i = 0;
  while ($i < $num)
  {
    $albumArtId = mysql_result($res, $i, "albumart_id");
    $key = $albumArtAlbumLevel ? mysql_result($res, $i, "directory_id") : mysql_result($res, $i, "song_id");
    $albumArtMap[$key] = $albumArtId;
    $i++;
  }
  return $albumArtMap;
}

function error($message)
{
  echo '{ "error": "' . $message . '" }'; 
}

// replace problem characters in sql results
function cleanup($in)
{
  if ($in == null)
    return $in;
  return str_replace('"', "'", str_replace("/", "--", $in));
}

class Type
{
  const VIDEOS = "videos";
  const MOVIES = "movies";
  const TV_SERIES = "tvSeries";
  const MUSIC = "music";
  const LIVE_TV = "liveTv";
  const RECORDINGS = "recordings";
  const SEARCH = "search";
  const GUIDE = "guide";
  const CUT_LIST = "cutList";

  function __construct($type)
  {
    $this->type = $type;
  }

  function isVideos()
  {
    return $this->type == Type::VIDEOS;
  }
  function isMovies()
  {
    return $this->type == Type::MOVIES;
  }
  function isTvSeries()
  {
    return $this->type == Type::TV_SERIES;
  }
  function isMusic()
  {
    return $this->type == Type::MUSIC;
  }
  function isLiveTv()
  {
    return $this->type == Type::LIVE_TV;
  }
  function isRecordings()
  {
    return $this->type == Type::RECORDINGS;
  }
  function isSearch()
  {
    return $this->type == Type::SEARCH;
  }
  function isGuide()
  {
    return $this->type == Type::GUIDE;
  }
  function isCutList()
  {
    return $this->type == Type::CUT_LIST;
  }  
  function isValid()
  {
    return $this->isVideos()
        || $this->isMovies()
        || $this->isTvSeries()
        || $this->isMusic()
        || $this->isLiveTv()
        || $this->isRecordings()
        || $this->isSearch()
        || $this->isGuide()
        || $this->isCutList();
  }
  function isSpecified()
  {
    return $this->type;
  }
}

?>
