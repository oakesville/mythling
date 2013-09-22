<?php
// Assign values for these constants and drop this file 
// somewhere under your Apache DocumentRoot directory.
$MYTHDB_HOST = "localhost";
$MYTHDB_DATABASE = "mythconverg";
$MYTHDB_USER = "mythtv";
$MYTHDB_PASSWORD = "mythtv";

$VIDEO_STORAGE_GROUP = "Videos";
$VIDEO_DIR_SETTING = "VideoStartupDir";
$MUSIC_DIR_SETTING = "MusicLocation";
$ARTWORK_STORAGE_GROUP = "Coverart";
$ARTWORK_DIR_SETTING = "VideoArtworkDir";
$RECORDINGS_STORAGE_GROUP = "Default";

$VIDEO_MOVIE_DIRS = array("Horror", "Pre-Code");
$VIDEO_EXCLUDE_DIRS = array("To Watch");
$PAGE_LINK_TITLE = "IMDB";

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

$hostname = gethostname();
date_default_timezone_set("UTC");
$dt = date("m-d-Y H:i:s") . " UTC";

mysql_connect($MYTHDB_HOST, $MYTHDB_USER, $MYTHDB_PASSWORD) or die("Could not connect: " . mysql_error());
mysql_select_db($MYTHDB_DATABASE) or die("Unable to select database");

$isVideoStorageGroup = false;
$pageLinkTitle = null;

if ($type->isSearch())
{
	$videoBase = getStorageGroupDir($VIDEO_STORAGE_GROUP);
	if ($videoBase == null)
		$videoBase = getSettingDir($VIDEO_DIR_SETTING);
	else
		$isVideoStorageGroup = true;
	$musicBase = getSettingDir($MUSIC_DIR_SETTING);
	$recordingsBase = getStorageGroupDir($RECORDINGS_STORAGE_GROUP);
	$posterBase = getBaseDir($ARTWORK_STORAGE_GROUP, $ARTWORK_DIR_SETTING);
	
  header("Content-type:text/plain");
  echo "{\n";
  echo '  "summary": ' . "\n";
  echo '  { "date": "' . $dt . '", "query": "' . $searchQuery . '", "videoBase": "' . $videoBase . '", "musicBase": "' . $musicBase . '", "recordingsBase": "' . $recordingsBase . '", "moviesBase": "' . $videoBase . '" },' . "\n";

  if ($videoBase != null)
  {
  	// videos
	  $vQuery = "select intid as id, filename from videometadata where (" . notLike(($isVideoStorageGroup ? null : $videoBase), array_merge($VIDEO_EXCLUDE_DIRS, $VIDEO_MOVIE_DIRS)) . ") and filename like '%" . $searchQuery . "%' order by filename";
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
	    $file = substr($part, $lastSlash + 1);
	    printSearchResult($id, $path, $file, $i < $vNum - 1);
	    $i++;
	  }
	  
	  // movies
	  echo "  ],\n";
	  $mQuery = "select intid as id, filename, year, userrating, director, '' as actors, plot as summary, coverfile, homepage from videometadata where (" . like(($isVideoStorageGroup ? null : $videoBase), $VIDEO_MOVIE_DIRS) . ") and (filename like '%" . $searchQuery . "%' or year like '%" . $searchQuery . "%' or userrating like '%" . $searchQuery . "%' or director like '%" . $searchQuery . "%' or plot like '%" . $searchQuery . "%') order by filename";
	  $mRes = mysql_query($mQuery) or die("Query failed: " . mysql_error());
	  $mNum = mysql_numrows($mRes);
	  echo '  "movies": ' . "\n  [\n";
	  $i = 0;
	  while ($i < $mNum)
	  {
	    $id = mysql_result($mRes, $i, "id");
	    $full = mysql_result($mRes, $i, "filename");
	    if ($isVideoStorageGroup)
	    	$full = $videoBase . "/" . $full;
	    $part = substr($full, strlen($videoBase) + 1);
	    $lastSlash = strrpos($part, "/");
	    $path = substr($part, 0, $lastSlash);
	    $file = substr($part, $lastSlash + 1);
	    $year = mysql_result($mRes, $i, "year");
	    $rating = mysql_result($mRes, $i, "userrating");
	    $director = mysql_result($mRes, $i, "director");
	    $actors = mysql_result($mRes, $i, "actors");
	    $summary = mysql_result($mRes, $i, "summary");
	    $pst = mysql_result($mRes, $i, "coverfile");
	    $poster = $pst == null || $posterBase == null ? null : substr($pst, strlen($posterBase) + 1);
	    $hp = mysql_result($mRes, $i, "homepage");
	    printSearchResultMovie($id, $path, $file, $year, $rating, $director, $actors, $summary, $poster, $hp, $i < $mNum - 1);
	    $i++;
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
	    printSearchResult($id, $path, $file, $i < $sNum - 1);
	    $i++;
	  }
	  echo "  ],\n";
  }

  $tQuery = "select concat(concat(p.chanid,'~'),p.starttime) as id, c.callsign, p.endtime, p.title, p.subtitle, p.description, convert(p.originalairdate using utf8) as oad from program p, channel c where p.chanid = c.chanid and starttime <= utc_timestamp() and endtime >= utc_timestamp() and (p.title like '%" . $searchQuery . "%' or p.subtitle like '%" . $searchQuery . "%' or p.description like '%" . $searchQuery . "%') order by p.chanid";
  $tRes = mysql_query($tQuery) or die("Query failed: " . mysql_error());
  $tNum = mysql_numrows($tRes);
  echo '  "tv": ' . "\n  [\n";
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
    printSearchResultRecordingOrTv($id, $progstart, $callsign, $title, null, $subtitle, $description, $airdate, $endtime, $i < $tNum - 1);
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
    printSearchResultRecordingOrTv($id, $progstart, $callsign, $title, $basename, $subtitle, $description, $airdate, $endtime, $i < $rNum - 1);
    $i++;
  }
  echo "  ]\n}";

  mysql_close();
}
else
{
  header("Content-type:text/plain");
  $orderBy = $_REQUEST['orderBy'];  // only for isVideos(), isMusic(), isMovies()
  if ($orderBy == null)
    $orderBy = "filename";
  else if (strcmp($orderBy, "filename") != 0)
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
    $query = "select intid as id, filename from videometadata where (" . notLike(($isVideoStorageGroup ? null : $base), array_merge($VIDEO_EXCLUDE_DIRS, $VIDEO_MOVIE_DIRS)) . ") order by " . $orderBy;
  }
  else if ($type->isMusic())
  {
    $base = getSettingDir($MUSIC_DIR_SETTING);
    if ($base == null)
    	die("Cannot determine base directory for music");
    $query = "select s.song_id as id, concat(concat(d.path,'/'),s.filename) as filename from music_directories d, music_songs s where d.directory_id = s.directory_id order by " . $orderBy;
  }
  else if ($type->isMovies())
  {
  	$pageLinkTitle = $PAGE_LINK_TITLE;
    $base = getStorageGroupDir($VIDEO_STORAGE_GROUP);
    if ($base == null)
    	$base = getSettingDir($VIDEO_DIR_SETTING);
    else
    	$isVideoStorageGroup = true;
    if ($base == null)
    	die("Cannot determine base directory for movies");
    $query = "select intid as id, title, filename, year, userrating, director, '' as actors, plot as summary, coverfile, homepage from videometadata where (" . like(($isVideoStorageGroup ? null : $base), $VIDEO_MOVIE_DIRS) . ") order by " . $orderBy;
  }
  else if ($type->isTv())
  {
    $base = getStorageGroupDir($RECORDINGS_STORAGE_GROUP);
    $query = "select concat(concat(p.chanid,'~'),p.starttime) as id, c.callsign, p.endtime, p.title, p.subtitle, p.description, convert(p.originalairdate using utf8) as oad from program p, channel c where p.chanid = c.chanid and starttime <= utc_timestamp() and endtime >= utc_timestamp() order by p.chanid";
  }
  else if ($type->isRecordings())
  {
    $base = getStorageGroupDir($RECORDINGS_STORAGE_GROUP);
    $query = "select concat(concat(r.chanid,'~'),r.starttime) as id, r.progstart, c.callsign, r.endtime, trim(leading 'A ' from trim(leading 'An ' from trim(leading 'The ' from r.title))) as title, r.basename, r.subtitle, r.description, convert(r.originalairdate using utf8) as oad, rr.recordid from recorded r inner join channel c on (r.chanid = c.chanid) left outer join record rr on (rr.chanid = r.chanid and rr.programid = r.programid) order by trim(leading 'A ' from trim(leading 'An ' from trim(leading 'The ' from r.title))), r.starttime desc";
  }

  //echo $query . "\n\n";

  $result = mysql_query($query) or die("Query failed: " . mysql_error());
  $num = mysql_numrows($result);
  $catPaths = array();
  $fileIds = array();
  if ($type->isRecordings() || $type->isTv())
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
  else if ($type->isMovies())
  {
  	$movieTitles = array();
    $years = array();
    $ratings = array();
    $directors = array();
    $actors = array();
    $summaries = array();
    $posters = array();
    $hps = array();
  }
  $prevPath = "";
  $i = 0;
  while ($i < $num)
  {
    $id = mysql_result($result, $i, "id");
    if ($type->isMovies())
    {
    	$ttl = mysql_result($result, $i, "title");
      $yr = mysql_result($result, $i, "year");
      $rt = mysql_result($result, $i, "userrating");
      $dir = mysql_result($result, $i, "director");
      $act = mysql_result($result, $i, "actors");
      $sum = mysql_result($result, $i, "summary");
      $pst = mysql_result($result, $i, "coverfile");
      $hp = mysql_result($result, $i, "homepage");
    }
    if ($type->isRecordings() || $type->isTv())
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
        $full = "TV/" . $tit;
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
    if ($type->isMusic() || $type->isRecordings() || $type->isTv())
      $part = $full;
    else
      $part = substr($full, strlen($base) + 1);
    // echo $part . "\n";
    $lastSlash = strrpos($part, "/");
    $path = substr($part, 0, $lastSlash);

    $file = substr($part, $lastSlash + 1);
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
    if ($type->isRecordings() || $type->isTv())
      $files[] = $id;  // $files actually contains ids
    else
      $files[] = $file;
    $catPaths[$path] = $files;
    $prevPath = $path;
    $fileIds[$path . '/' . $file] = $id;
    if ($type->isRecordings() || $type->isTv())
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
    else if ($type->isMovies())
    {
    	$posterBase = getBaseDir($ARTWORK_STORAGE_GROUP, $ARTWORK_DIR_SETTING);
    	$movieTitles[$id] = $ttl; 
      $years[$id] = $yr;
      $ratings[$id] = $rt;
      $directors[$id] = $dir;
      $actors[$id] = $act;
      $summaries[$id] = $sum;
      $posters[$id] = $pst == null || $posterBase == null ? null : substr($pst, strlen($posterBase) + 1);
      $hps[$id] = $hp;
    }
  
    $i++;
  }

  mysql_close();
  

  echo "{\n";
  echo '  "summary": ' . "\n";
  echo '  { "type": "' . $type->type . '", "date": "' . $dt . '", "count": "' . $num . '", "base": "' . $base . '"';
  if ($pageLinkTitle != null)
  	echo ', "pageLinkTitle": "' . $pageLinkTitle . '"';
  echo " },\n";  
  $i = 0;
  $depth = 0;
  $prevPath = "";
  $prevSize = 0;
  $prevHadItems = false;
  $keys = array_keys($catPaths);
  $keyCt = count($keys);
  while ($i < $keyCt)
  {
    $path = $keys[$i];
    $files = $catPaths[$path];
    $fileCt = count($files);
    // echo $path . "($fileCt)\n";
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
    if ($size == $prevSize)
    {
      printCatEnd($depth, true);
    }

    printCatBegin($piece, $depth);
    if ($fileCt > 0)
      printItems($path, $files, $depth);
  
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
  global $type, $fileIds, $progstarts, $callsigns, $recordings, $basenames, $subtitles, $descriptions, $airdates, $endtimes, $recordids, $movieTitles, $years, $ratings, $directors, $actors, $summaries, $posters, $hps;

  echo indent($depth * 4 + 2);

  if ($type->isRecordings() || $type->isTv())
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
  else if ($type->isRecordings() || $type->isTv())
  {
    if ($type->isRecordings())
      $title = $path;
    else
      $title = $recordings[$file];
    $artist = null;
    $extra = null;
    $filetype = null;
  }
  else if ($type->isMovies())
  {
  	$title = $movieTitles[$id];
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
  if ($type->isRecordings() || $type->isTv())
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
  else if ($type->isMovies())
  {
    if ($years[$id] != null)
      echo ', "year": "' . $years[$id] . '"';
    if ($ratings[$id] != null)
      echo ', "rating": "' . $ratings[$id] . '"';
    if ($directors[$id] != null)
      echo ', "director": "' . $directors[$id] . '"';
    if ($actors[$id] != null)
      echo ', "actors": "' . $actors[$id] . '"';
    if ($summaries[$id] != null)
      echo ', "summary": "' . str_replace('"', '\"', $summaries[$id]) . '"';
    if ($posters[$id] != null)
      echo ', "poster": "' . $posters[$id] . '"';
    if ($hps[$id] != null)
      echo ', "pageUrl": "' . $hps[$id] . '"';
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

function printSearchResultRecordingOrTv($id, $progstart, $callsign, $title, $basename, $subtitle, $description, $airdate, $endtime, $more)
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

function printSearchResultMovie($id, $path, $file, $year, $rating, $director, $actors, $summary, $poster, $hp, $more)
{
  $lastdot = strrpos($file, ".");
  $title = substr($file, 0, $lastdot);
  $filetype = substr($file, $lastdot + 1);

  echo "    { ";
  echo '"id": "' . $id . '", ';
  echo '"path": "' . $path . '", ';
  echo '"title": "' . $title . '", ';
  echo '"format": "' . $filetype . '"';
  if ($year)
    echo ', "year": "' . $year . '"';
  if ($rating)
    echo ', "rating": "' . $rating . '"';
  if ($director)
    echo ', "director": "' . $director . '"';
  if ($actors)
    echo ', "actors": "' . $actors . '"';
  if ($summary)
    echo ', "summary": "' . str_replace('"','\"',$summary) . '"';
  if ($poster && $poster != null)
    echo ', "poster": "' . $poster . '"';
  if ($hp && $hp != null)
    echo ', "pageUrl": "' . $hp . '"';
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
	$dir = mysql_result($result, 0, "dirname");
	if ($dir != null)
		return trimTrailingSlash($dir);
}

function getSettingDir($setting)
{
	global $hostname;
	$query = "select data from settings where hostname = '" . $hostname . "' and value = '" . $setting . "'";
	$result = mysql_query($query) or die("Query failed: " . mysql_error());
	$dir = mysql_result($result, 0, "data");
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

class Type
{
  const VIDEOS = "videos";
  const MOVIES = "movies";
  const MUSIC = "music";
  const TV = "tv";
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
  function isMusic()
  {
    return $this->type == Type::MUSIC;
  }
  function isTv()
  {
    return $this->type == Type::TV;
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
        || $this->isMusic()
        || $this->isTv()
        || $this->isRecordings()
        || $this->isSearch();
  }
  function isSpecified()
  {
    return $this->type;
  }
}

?>
