<?php
// Assign values for these constants and drop this file somewhere
// under your Apache DocumentRoot directory.
$MYTHDB_HOST = "localhost";
$MYTHDB_DATABASE = "mythconverg";
$MYTHDB_USER = "mythtv";
$MYTHDB_PASSWORD = "mythtv";

// MythTV settings
$VIDEO_STORAGE_GROUP = "Videos";
$VIDEO_DIR_SETTING = "VideoStartupDir";
$MUSIC_DIR_SETTING = "MusicLocation";
$ARTWORK_STORAGE_GROUP = "Coverart";  // or Fanart or Screenshots or Banners
$ARTWORK_DIR_SETTING = "VideoArtworkDir";
$RECORDINGS_STORAGE_GROUP = "Default";

$type = new Type($_REQUEST['type']);
if (!$type->isSpecified())
  die("Missing request parameter: type");
if (!$type->isValid())
  die("Unsupported query type: " . $type->type);
if ($type->isSearch())
{
  $searchQuery = $_REQUEST['query'];
  if ($searchQuery == null)
    die("Missing search parameter: query");
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

$hostname = gethostname();
date_default_timezone_set("UTC");
$dt = date("m-d-Y H:i:s") . " UTC";

mysql_connect($MYTHDB_HOST, $MYTHDB_USER, $MYTHDB_PASSWORD) or die("Could not connect: " . mysql_error());
mysql_select_db($MYTHDB_DATABASE) or die("Unable to select database");

$isVideoStorageGroup = false;
$castMap = null;

if ($type->isSearch())
{
  $videoBase = getStorageGroupDir($VIDEO_STORAGE_GROUP);
  if ($videoBase == null)
    $videoBase = getSettingDir($VIDEO_DIR_SETTING);
  else
    $isVideoStorageGroup = true;
  $musicBase = getSettingDir($MUSIC_DIR_SETTING);
  $recordingsBase = getStorageGroupDir($RECORDINGS_STORAGE_GROUP);
  $artworkBase = getBaseDir($ARTWORK_STORAGE_GROUP, $ARTWORK_DIR_SETTING);
  
  header("Content-type:application/json");
  echo "{\n";
  echo '  "summary": ' . "\n";
  echo '  { "date": "' . $dt . '", "query": "' . $searchQuery . '", "videoBase": "' . $videoBase . '", "musicBase": "' . $musicBase . '", "recordingsBase": "' . $recordingsBase . '", "moviesBase": "' . $videoBase . '", "tvSeriesBase": "' . $videoBase . '", "artworkStorageGroup": "' . $ARTWORK_STORAGE_GROUP . '"';  
  echo ' },'	. "\n";

  if ($videoBase != null)
  {
    // videos
    $vWhere = "where 1=1";
    if ($categorizeUsingDirs)
      $vWhere = $vWhere . " and (" . notLike(($isVideoStorageGroup ? null : $videoBase), array_merge(array_merge($videoExcludeDirs,$movieDirs),$tvSeriesDirs)) . ")";
    else if ($categorizeUsingMetadata)
      $vWhere = $vWhere . " and (inetref is null or inetref = '00000000') and (season is null or season = '0') and (episode is null or episode = '0')";
    $vWhere = $vWhere . " and filename like '%" . $searchQuery . "%'"; 
    $vQuery = "select intid as id, filename from videometadata " . $vWhere . " order by filename";
    // echo "\n vQuery:\n" . $vQuery . "\n";
    $vRes = mysql_query($vQuery) or die("Query failed: " . mysql_error());
    $vNum = mysql_numrows($vRes);
    echo '  "videos": ' . "\n  [\n";
    $i = 0;
    while ($i < $vNum)
    {
      $id = mysql_result($vRes, $i, "id");
      $full = mysql_result($vRes, $i, "filename");
      if ($isVideoStorageGroup)
        $full = $videoBase . "/" . $full;
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
        $mWhere = $mWhere . " and (" . like(($isVideoStorageGroup ? null : $videoBase), $movieDirs) . ")";
      else
        $mWhere = $mWhere . " and ((inetref is not null and inetref != '00000000') and (episode is null or episode = '0') and (season is null or season = '0'))"; 
      $mWhere = $mWhere . " and (filename like '%" . $searchQuery . "%' or year like '%" . $searchQuery . "%' or director like '%" . $searchQuery . "%' or plot like '%" . $searchQuery . "%')";
      $mQuery = "select intid as id, title, filename, inetref, homepage, year, userrating, director, plot as summary, coverfile, fanart, screenshot, banner from videometadata " . $mWhere . " order by filename";
      // echo "\n mQuery:\n" . $mQuery . "\n";
      $mRes = mysql_query($mQuery) or die("Query failed: " . mysql_error());
      $mNum = mysql_numrows($mRes);
      echo '  "movies": ' . "\n  [\n";
      $i = 0;
      while ($i < $mNum)
      {
        $id = mysql_result($mRes, $i, "id");
        $title = mysql_result($mRes, $i, "title");
        $full = mysql_result($mRes, $i, "filename");
        if ($isVideoStorageGroup)
          $full = $videoBase . "/" . $full;
        $part = substr($full, strlen($videoBase) + 1);
        $lastSlash = strrpos($part, "/");
        $path = substr($part, 0, $lastSlash);
        $file = substr($part, $lastSlash + 1);
        $inetref = mysql_result($mRes, $i, "inetref");
        $homepage = mysql_result($mRes, $i, "homepage");
        $year = mysql_result($mRes, $i, "year");
        $rating = mysql_result($mRes, $i, "userrating");
        $director = mysql_result($mRes, $i, "director");
        $summary = mysql_result($mRes, $i, "summary");
        $art = mysql_result($mRes, $i, "coverfile");
        if ($art == null || (strcmp($ARTWORK_STORAGE_GROUP, 'Fanart') == 0))
          $art = mysql_result($mRes, $i, "fanart");
        if ($art == null || (strcmp($ARTWORK_STORAGE_GROUP, 'Screenshots') == 0))
          $art = mysql_result($mRes, $i, "screenshot");
        if ($art == null || (strcmp($ARTWORK_STORAGE_GROUP, 'Banners') == 0))
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
        $tsWhere = $tsWhere . " and (" . like(($isVideoStorageGroup ? null : $videoBase), $tvSeriesDirs) . ")";
      else
        $tsWhere = $tsWhere . " and ((season is not null and season != '0') or (episode is not null and episode != '0'))";
      $tsWhere = $tsWhere . " and (filename like '%" . $searchQuery . "%' or subtitle like '%" . $searchQuery . "%' or director like '%" . $searchQuery . "%' or plot like '%" . $searchQuery . "%')";
      $tsQuery = "select intid as id, title, subtitle, season, episode, filename, inetref, homepage, releasedate, userrating, director, plot as summary, coverfile, fanart, screenshot, banner from videometadata " . $tsWhere . " order by filename";
      // echo "\n tsQuery:\n" . $tsQuery . "\n";
      $tsRes = mysql_query($tsQuery) or die("Query failed: " . mysql_error());
      $tsNum = mysql_numrows($tsRes);
      echo '  "tvSeries": ' . "\n  [\n";
      $i = 0;
      while ($i < $tsNum)
      {
        $id = mysql_result($tsRes, $i, "id");
        $title = mysql_result($tsRes, $i, "title");
        $subTitle = mysql_result($tsRes, $i, "subtitle");
        $season = mysql_result($tsRes, $i, "season");
        $episode = mysql_result($tsRes, $i, "episode");
        $full = mysql_result($tsRes, $i, "filename");
        if ($isVideoStorageGroup)
          $full = $videoBase . "/" . $full;
        $part = substr($full, strlen($videoBase) + 1);
        $lastSlash = strrpos($part, "/");
        $path = substr($part, 0, $lastSlash);
        $file = substr($part, $lastSlash + 1);
        $inetref = mysql_result($tsRes, $i, "inetref");
        $homepage = mysql_result($tsRes, $i, "homepage");
        $aired = mysql_result($tsRes, $i, "releasedate");
        $rating = mysql_result($tsRes, $i, "userrating");
        $director = mysql_result($tsRes, $i, "director");
        $summary = mysql_result($tsRes, $i, "summary");
        $art = mysql_result($tsRes, $i, "coverfile");
        if ($art == null || (strcmp($ARTWORK_STORAGE_GROUP, 'Fanart') == 0))
          $art = mysql_result($tsRes, $i, "fanart");
        if ($art == null || (strcmp($ARTWORK_STORAGE_GROUP, 'Screenshots') == 0))
          $art = mysql_result($tsRes, $i, "screenshot");
        if ($art == null || (strcmp($ARTWORK_STORAGE_GROUP, 'Banners') == 0))
          $art = mysql_result($tsRes, $i, "banner");
        $artwork = $art == null || $artworkBase == null ? null : (startsWith($art, $artworkBase) ? substr($art, strlen($artworkBase) + 1) : $art);
        $actors = array_key_exists($id, $castMap) ? $castMap[$id] : null;
        printSearchResultMovieOrTvSeries($id, $title, $subTitle, $season, $episode, $path, $file, $inetref, $homepage, null, $aired, $rating, $director, $actors, $summary, $artwork, $i < $tsNum - 1);
        $i++;
      }
    }
     
    echo "  ],\n";
  }

  if ($musicBase != null)
  {
    $sQuery = "select s.song_id as id, concat(concat(d.path,'/'),s.filename) as filename from music_directories d, music_songs s where d.directory_id = s.directory_id and (d.path like '%" . $searchQuery . "%' or s.filename like '%" . $searchQuery . "%') order by filename";
    $sRes = mysql_query($sQuery) or die("Query failed: " . mysql_error());
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

  $tQuery = "select concat(concat(p.chanid,'~'),p.starttime) as id, c.callsign, p.endtime, p.title, p.subtitle, p.description, convert(p.originalairdate using utf8) as oad from program p, channel c where p.chanid = c.chanid and starttime <= utc_timestamp() and endtime >= utc_timestamp() and (p.title like '%" . $searchQuery . "%' or p.subtitle like '%" . $searchQuery . "%' or p.description like '%" . $searchQuery . "%') order by p.chanid";
  $tRes = mysql_query($tQuery) or die("Query failed: " . mysql_error());
  $tNum = mysql_numrows($tRes);
  echo '  "liveTv": ' . "\n  [\n";
  $i = 0;
  while ($i < $tNum)
  {
    $id = mysql_result($tRes, $i, "id");
    $progstart = null; // always identifed by id
    $callsign = mysql_result($tRes, $i, "callsign");
    $title = mysql_result($tRes, $i, "title");
    $subtitle = mysql_result($tRes, $i, "subtitle");
    $description = mysql_result($tRes, $i, "description");
    $oads = mysql_result($tRes, $i, "oad");
    if (strcmp($oads, "0000-00-00") != 0)
      $airdate = $oads;
    $endtime = mysql_result($tRes, $i, "endtime");
    printSearchResultRecordingOrLiveTv($id, $progstart, $callsign, $title, null, $subtitle, $description, $airdate, $endtime, $i < $tNum - 1);
    $i++;
  }
  echo "  ],\n";

  $rQuery = "select concat(concat(r.chanid,'~'),r.starttime) as id, r.progstart, c.callsign, trim(leading 'A ' from trim(leading 'An ' from trim(leading 'The ' from r.title))) as title, r.basename, r.subtitle, r.description, convert(r.originalairdate using utf8) as oad, r.endtime from recorded r, channel c where r.chanid = c.chanid and (r.title like '%" . $searchQuery . "%' or r.subtitle like '%" . $searchQuery . "%' or r.description like '%" . $searchQuery . "%') order by trim(leading 'A ' from trim(leading 'An ' from trim(leading 'The ' from r.title))), r.starttime desc";
  $rRes = mysql_query($rQuery) or die("Query failed: " . mysql_error());
  $rNum = mysql_numrows($rRes);
  echo '  "recordings": ' . "\n  [\n";
  $i = 0;
  while ($i < $rNum)
  {
    $id = mysql_result($rRes, $i, "id");
    $progstart = mysql_result($rRes, $i, "progstart");
    $callsign = mysql_result($rRes, $i, "callsign");
    $title = mysql_result($rRes, $i, "title");
    $basename = mysql_result($rRes, $i, "basename");
    $subtitle = mysql_result($rRes, $i, "subtitle");
    $description = mysql_result($rRes, $i, "description");
    $oads = mysql_result($rRes, $i, "oad");
    if (strcmp($oads, "0000-00-00") != 0)
      $airdate = $oads;
    $endtime = mysql_result($rRes, $i, "endtime");
    printSearchResultRecordingOrLiveTv($id, $progstart, $callsign, $title, $basename, $subtitle, $description, $airdate, $endtime, $i < $rNum - 1);
    $i++;
  }
  echo "  ]\n}";

  mysql_close();
}
else
{
  header("Content-type:application/json");
  $orderBy = "filename";
  if (array_key_exists('orderBy', $_REQUEST))  // only for isVideos(), isMusic(), isMovies(), isTvSeries()
    $orderBy = $_REQUEST['orderBy'];
  if (strcmp($orderBy, "filename") != 0)
    $orderBy = $orderBy . ", filename";
  if ($type->isVideos())
  {
    $base = getStorageGroupDir($VIDEO_STORAGE_GROUP);
    if ($base == null)
      $base = getSettingDir($VIDEO_DIR_SETTING);
    else
      $isVideoStorageGroup = true;
    if ($base == null)
      die("Cannot determine base directory for videos");
    $where = "";
    if ($categorizeUsingDirs)
      $where = "where (" . notLike(($isVideoStorageGroup ? null : $base), array_merge(array_merge($videoExcludeDirs,$movieDirs),$tvSeriesDirs)) . ")";
    else if ($categorizeUsingMetadata)
      $where = "where (inetref is null or inetref = '00000000') and ((season is null or season = '0') and (episode is null or episode = '0'))";
    $orderBy = "order by " . $orderBy;
    $query = "select intid as id, filename from videometadata " . $where . " " . $orderBy;
  }
  else if ($type->isMusic())
  {
    $base = getSettingDir($MUSIC_DIR_SETTING);
    if ($base == null)
      die("Cannot determine base directory for music");
    $where = "where d.directory_id = s.directory_id";
    $orderBy = "order by " . $orderBy;
    $query = "select s.song_id as id, concat(concat(d.path,'/'),s.filename) as filename from music_directories d, music_songs s " . $where . " " . $orderBy;
  }
  else if ($type->isMovies() || $type->isTvSeries())
  {
    $castMap = getCastMap();
    $base = getStorageGroupDir($VIDEO_STORAGE_GROUP);
    if ($base == null)
      $base = getSettingDir($VIDEO_DIR_SETTING);
    else
      $isVideoStorageGroup = true;
    if ($base == null)
      die("Cannot determine base directory for categorized videos");
    if ($categorizeUsingDirs)
    {
    	if ($type->isMovies())
        $where = "where (" . like(($isVideoStorageGroup ? null : $base), $movieDirs) . ")";
    	else if ($type->isTvSeries())
    		$where = "where (" . like(($isVideoStorageGroup ? null : $base), $tvSeriesDirs) . ")";
    }
    else
    {
      if ($type->isMovies())
        $where = "where (inetref is not null and inetref != '00000000') and (season is null or season = '0') and (episode is null or episode = '0')";
      else if ($type->isTvSeries())
        $where = "where (season is not null and season != '0') or (episode is not null and episode != '0')";
    }
    $orderBy = "order by " . $orderBy;
    $query = "select intid as id, title, subtitle, filename, inetref, homepage, season, episode, year, releasedate, userrating, director, plot as summary, coverfile, fanart, screenshot, banner from videometadata " . $where . " " . $orderBy;
  }
  else if ($type->isLiveTv())
  {
    $base = getStorageGroupDir($RECORDINGS_STORAGE_GROUP);
    $where = "where p.chanid = c.chanid and starttime <= utc_timestamp() and endtime >= utc_timestamp()";
    $orderBy = "order by p.chanid";
    $query = "select concat(concat(p.chanid,'~'),p.starttime) as id, c.callsign, p.endtime, p.title, p.subtitle, p.description, convert(p.originalairdate using utf8) as oad from program p, channel c " . $where . " " . $orderBy;
  }
  else if ($type->isRecordings())
  {
    $base = getStorageGroupDir($RECORDINGS_STORAGE_GROUP);
    $where = "inner join channel c on (r.chanid = c.chanid) left outer join record rr on (rr.chanid = r.chanid and rr.programid = r.programid)";
    $orderBy = "order by trim(leading 'A ' from trim(leading 'An ' from trim(leading 'The ' from r.title))), r.starttime desc";
    $query = "select concat(concat(r.chanid,'~'),r.starttime) as id, r.progstart, c.callsign, r.endtime, trim(leading 'A ' from trim(leading 'An ' from trim(leading 'The ' from r.title))) as title, r.basename, r.subtitle, r.description, convert(r.originalairdate using utf8) as oad, rr.recordid from recorded r " . $where . " " . $orderBy;
  }

  //echo $query . "\n\n";

  $result = mysql_query($query) or die("Query failed: " . mysql_error());
  $num = mysql_numrows($result);
  $catPaths = array();
  $fileIds = array();
  if ($type->isRecordings() || $type->isLiveTv())
  {
    $progstarts = array();
    $callsigns = array();
    $recordings = array();
    $basenames = array();
    $subtitles = array();
    $descriptions = array();
    $airdates = array();
    $endtimes = array();
    $recordids = array();
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
  $prevPath = "";
  $i = 0;
  while ($i < $num)
  {
    $id = mysql_result($result, $i, "id");
    if ($type->isMovies() || $type->isTvSeries())
    {
      $ttl = mysql_result($result, $i, "title");
      $stl = mysql_result($result, $i, "subtitle");
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
      $art = mysql_result($result, $i, "coverfile");
      if ($art == null || (strcmp($ARTWORK_STORAGE_GROUP, 'Fanart') == 0))
        $art = mysql_result($result, $i, "fanart");
      if ($art == null || (strcmp($ARTWORK_STORAGE_GROUP, 'Screenshots') == 0))
        $art = mysql_result($result, $i, "screenshot");
      if ($art == null || (strcmp($ARTWORK_STORAGE_GROUP, 'Banners') == 0))
        $art = mysql_result($result, $i, "banner");
    }
    if ($type->isRecordings() || $type->isLiveTv())
    {
      if ($type->isRecordings())
        $progst = mysql_result($result, $i, "progstart");
      else
        $progst = null;
      $cs = mysql_result($result, $i, "callsign");
      $tit = mysql_result($result, $i, "title");
      $tit = str_replace("/", "--", $tit);
      if ($type->isRecordings())
        $full = $tit . "/" . $tit;
      else
        $full = $tit;
      if ($type->isRecordings())
        $bn = mysql_result($result, $i, "basename");
      else
        $bn = null;
      $subtit = mysql_result($result, $i, "subtitle");
      $descrip = mysql_result($result, $i, "description");
      $oads = mysql_result($result, $i, "oad");
      if (strcmp($oads, "0000-00-00") != 0)
        $oad = $oads;
      $et = mysql_result($result, $i, "endtime");
      $rid = mysql_result($result, $i, "recordid");
    }
    else
    {
      $full = mysql_result($result, $i, "filename");
      if (!$type->isMusic() && $isVideoStorageGroup)
        $full = $base . "/" . $full;
    }
    if ($type->isMusic() || $type->isRecordings() || $type->isLiveTv())
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
      $callsigns[$id] = $cs;
      $recordings[$id] = $tit;
      $basenames[$id] = $bn;
      $subtitles[$id] = $subtit;
      $descriptions[$id] = $descrip;
      $airdates[$id] = $oad;
      $endtimes[$id] = $et;
      $recordids[$id] = $rid;
    }
    else if ($type->isMovies() || $type->isTvSeries())
    {
      $artworkBase = getBaseDir($ARTWORK_STORAGE_GROUP, $ARTWORK_DIR_SETTING);
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
  
    $i++;
  }

  mysql_close();
  

  echo "{\n";
  echo '  "summary": ' . "\n";
  echo '  { "type": "' . $type->type . '", "date": "' . $dt . '", "count": "' . $num . '", "base": "' . $base . '", "artworkStorageGroup": "' . $ARTWORK_STORAGE_GROUP . '" ';
  if ($num > 0)
    echo " },\n";
  else
  	echo " }\n";  
  $i = 0;
  $depth = 0;
  $prevPath = "";
  $prevSize = 0;
  $prevHadItems = false;
  $keys = array_keys($catPaths);
  $keyCt = count($keys);
  // top-level items
  if (array_key_exists("", $catPaths))
  {
    printItems("", $catPaths[""], 0);
    echo ",\n";
  }
  while ($i < $keyCt)
  {
    $path = $keys[$i];
    $files = $catPaths[$path];
    $fileCt = count($files);
    //echo $path . "($fileCt)\n";
    $pieces = explode("/", $path);
    $size = count($pieces);
  
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
  
    $prevPath = $path;
    $prevSize = $size;
    $prevHadItems = $hasItems;
    $i++;
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
  global $type, $fileIds, $progstarts, $callsigns, $recordings, $basenames, $subtitles, $descriptions, $airdates, $endtimes, $recordids, $titles, $subtitles, $inetrefs, $homepages, $airdates, $seasons, $episodes, $years, $ratings, $directors, $actors, $summaries, $artworks;

  echo indent($depth * 4 + 2);

  if ($type->isRecordings() || $type->isLiveTv())
    $id = $file; // file is actually id
  else
    $id = $fileIds[$path . '/' . $file];

  if ($type->isMusic())
  {
    $lastdot = strrpos($file, ".");
    $title = substr($file, 0, $lastdot);
    $artist = null;
    $extra = null;
    $filetype = substr($file, $lastdot + 1);
  }
  else if ($type->isRecordings() || $type->isLiveTv())
  {
    if ($type->isRecordings())
      $title = $path;
    else
      $title = $recordings[$file];
    $artist = null;
    $extra = null;
    $filetype = null;
  }
  else
  {
    $lastDash = strrpos($file, "- ");
    if ($lastDash)
    {
      $parta = substr($file, 0, $lastDash - 1);
      $openParen = strpos($parta, "(");
      if ($openParen)
      {
        $closeParen = strrpos($parta, ")");
        $title = substr($parta, 0, $openParen - 1);
        $extra = substr($parta, $openParen +1, $closeParen - $openParen - 1);
      }
      else
      {
        $title = $parta;
        $extra = null;
      }
      $partb = substr($file, $lastDash + 2);
      $lastdot = strrpos($partb, ".");
      $artist = substr($partb, 0, $lastdot);
      $filetype = substr($partb, $lastdot + 1);
    }
    else
    {
      // no artist
      $lastdot = strpos($file, ".");
      $title = substr($file, 0, $lastdot);
      $filetype = substr($file, $lastdot + 1);
      $artist = null;
      $extra = null;
    }
    if ($type->isMovies() || $type->isTvSeries())
      $title = $titles[$id];
  }

  echo "{ ";
  echo '"id": "' . $id . '"';
  echo ', "title": "' . $title . '"';
  if ($artist != null)
    echo ', "artist": "' . $artist . '"';
  if ($filetype != null)
    echo ', "format": "' . $filetype . '"';
  if ($extra != null)
    echo ', "extra": "' . $extra . '"';
  if ($type->isRecordings() || $type->isLiveTv())
  {
    if ($progstarts[$id] != null)
      echo ', "programStart": "' . $progstarts[$id] . '"';
    if ($callsigns[$id] != null)
      echo ', "callsign": "' . $callsigns[$id] . '"';
    if ($basenames[$id] != null)
    {
      $basename = $basenames[$id];
      $lastdot = strpos($basename, ".");
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
    if ($recordids[$id] != null)
      echo ', "recordid": ' . $recordids[$id];
  }
  else if ($type->isMovies() || $type->isTvSeries())
  {
  	echo ', "file": "' . substr($file, 0, strpos($file, ".")) . '"';
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

function printSearchResultRecordingOrLiveTv($id, $progstart, $callsign, $title, $basename, $subtitle, $description, $airdate, $endtime, $more)
{
  echo "    { ";
  echo '"id": "' . $id . '"';
  if ($progstart != null)
    echo ', "programStart": "' . $progstart . '"';
  echo ', "title": "' . $title . '"';
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
  if ($airdate)
    echo ', "airdate": "' . $airdate . '"';
  if ($endtime)
    echo ', "endtime": "' . $endtime . '"';
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

function getSettingDir($setting)
{
  global $hostname;
  $query = "select data from settings where hostname = '" . $hostname . "' and value = '" . $setting . "'";
  $result = mysql_query($query) or die("Query failed: " . mysql_error());
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
  $res = mysql_query($query) or die("Query failed: " . mysql_error());
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

class Type
{
  const VIDEOS = "videos";
  const MOVIES = "movies";
  const TV_SERIES = "tvSeries";
  const MUSIC = "music";
  const LIVE_TV = "liveTv";
  const RECORDINGS = "recordings";
  const SEARCH = "search";

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
  function isValid()
  {
    return $this->isVideos()
        || $this->isMovies()
        || $this->isTvSeries()
        || $this->isMusic()
        || $this->isLiveTv()
        || $this->isRecordings()
        || $this->isSearch();
  }
  function isSpecified()
  {
    return $this->type;
  }
}

?>
