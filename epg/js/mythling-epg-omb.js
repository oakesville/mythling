/*! mythling-epg v1.2.0
 *  Copyright 2015 Donald Oakes
 *  License: Apache-2.0 */
'use strict';

var epgApp = angular.module('epgApp', ['epgSearch', 'epgCalendar', 'infinite-scroll', 'stay', 'ui.bootstrap']);

epgApp.constant('ERROR_TAG', 'ERROR: ');

epgApp.config(['$compileProvider', function($compileProvider) {
  var debugInfo = 'true' == urlParams().angularDebug;
  $compileProvider.debugInfoEnabled(debugInfo);
}]);

epgApp.config(['$httpProvider', function($httpProvider) {
  $httpProvider.defaults.headers.get = { 'Accept' : 'application/json' };
  $httpProvider.interceptors.push(function() {
    return {
      'request': function(config) {
        config.requestTimestamp = new Date().getTime();
        return config;
      },
      'response': function(response) {
        response.config.responseTime = new Date().getTime() - response.config.requestTimestamp;
        return response;
      }
    };
  });
}]);

epgApp.config(['$tooltipProvider', function($tooltipProvider) {
  $tooltipProvider.setTriggers({'popShow': 'popHide'});
}]);

epgApp.controller('EpgController', 
    ['$scope', '$window', '$http', '$timeout', '$location', '$modal', '$filter', 'GuideData', 
    function($scope, $window, $http, $timeout, $location, $modal, $filter, GuideData) {

  console.log('userAgent: ' + $window.navigator.userAgent);
  console.log('location: ' + $window.location);
  
  // layout dimensions
  for (var i = 0; i < $window.document.styleSheets.length; i++) {
    var sheet = $window.document.styleSheets[i];
    if (sheet.href !== null && sheet.href.endsWith('mythling.css'))  { // allows override (my-mythling.css)
      for (var j = 0; j < sheet.cssRules.length; j++) {
        var rule = sheet.cssRules[j];
        if (rule.selectorText === '.slot-width')
          $scope.slotWidth = parseCssDim(rule.style.width);
        else if (rule.selectorText === '.header-height')
          $scope.headerHeight = parseCssDim(rule.style.height);
        else if (rule.selectorText === '.label-width')
          $scope.labelWidth = parseCssDim(rule.style.width);
        else if (rule.selectorText === '.row-height')
          $scope.rowHeight = parseCssDim(rule.style.height);
      }
    }
  }

  var params = urlParams();
  // supportedParams
  var startTime = params.startTime ? new Date(params.startTime) : new Date();
  startTime = params.StartTime ? new Date(params.StartTime) : startTime; // support MythTV param name
  console.log('startTime: ' + startTime);
  var guideInterval = params.guideInterval ? parseInt(params.guideInterval) : 12; // hours
  var guideHistory = params.guideHistory ? parseInt(params.guideHistory) : 0; // hours (must be less than interval)
  $scope.bufferSize = params.bufferSize ? parseInt(params.bufferSize) : 6; // screen widths (say, around 2 hours per)
  var awaitPrime = params.awaitPrime ? params.awaitPrime == 'true' : false; // whether to disable mobile scroll until loaded
  var channelGroupId = params.channelGroupId ? parseInt(params.channelGroupId) : 0;
  var mythlingServices = params.mythlingServices ? params.mythlingServices == 'true' : false;
  var demoMode = params.demoMode ? params.demoMode == 'true' : false;
  
  $scope.guideData = new GuideData(startTime, $scope.slotWidth, guideInterval, awaitPrime, guideHistory, channelGroupId, mythlingServices, demoMode);
  
  $scope.setPosition = function(offset) {
    var slots = Math.floor(offset / $scope.slotWidth);
    var newCurDate = new Date($scope.guideData.zeroTime.getTime() + slots * 1800000);
    if (newCurDate.getTime() != $scope.guideData.curDate.getTime()) {
      $scope.guideData.curDate = newCurDate;
      if (!$scope.guideData.busy)
        angular.element(document.getElementById('calendarInput')).val($filter('date')($scope.guideData.curDate, 'EEE MMM d'));
    }
  };
  setPosition = $scope.setPosition;
  
  $scope.popElem = null;
  $scope.setPopElem = function(elem) {
    $scope.popElem = elem;
    elem.children(0).addClass('program-select');
  };
  $scope.popHide = function() {
    if ($scope.popElem !== null) {
      $scope.popElem.triggerHandler('popHide');
      $scope.popElem.children(0).removeClass('program-select');
      $scope.popElem = null;
    }
  };
  
  $scope.popPlace = "top";
  $scope.setPopPlace = function(place) {
    $scope.popPlace = place;
  };
  
  var win = angular.element($window); 
  win.bind('click', $scope.popHide);
  win.bind('scroll', $scope.popHide);
  win.bind('resize', $scope.popHide);
  
  // TODO make details a service
  $scope.details = function(program) {
    // TODO: better way to keep element showing selected
    $timeout(function() {
      angular.element(document.getElementById(program.id)).addClass('program-select');
    }, 0);
    
    if ($scope.guideData.demoMode || !$scope.guideData.isMyth28) {
      $scope.program = program;
      var modalInstance = $modal.open({
        animation: false,
        templateUrl: 'views/details.html',
        controller: 'EpgModalController',
        scope: $scope
      });
      return;
    }
    
    var url = '/Guide/GetProgramDetails?ChanId=' + program.channel.ChanId + '&StartTime=' + program.StartTime;
    console.log('details url: ' + url);
    $http.get(url).success(function(data) {
      var Program = data.Program;
      if (Program.Description)
        program.description = Program.Description.replace('``', '"');
      if (Program.Airdate && "true" === Program.Repeat) {
        var oad = Program.Airdate;
        var d = new Date();
        d.setFullYear(parseInt(oad.substring(0, oad.indexOf('-'))));
        d.setMonth(parseInt(oad.substring(oad.indexOf('-') + 1, oad.lastIndexOf('-'))) - 1);
        d.setDate(parseInt(oad.substring(oad.lastIndexOf('-') + 1)));
        d.setHours(0);
        d.setMinutes(0);
        d.setSeconds(0);
        d.setMilliseconds(0);
        program.originalAirDate = d;
      }
      if (Program.Category)
        program.category = Program.Category;
      if (Program.Season && Program.Season != '0')
        program.season = parseInt(Program.Season);
      if (Program.Episode && Program.Episode != '0')
        program.episode = parseInt(Program.Episode);
      if (Program.Stars && Program.Stars != '0')
        program.rating = Math.round(parseFloat(Program.Stars)*5);
        
      if (Program.Cast && Program.Cast.CastMembers) {
        var directors = '';
        var actors = '';
        for (var i = 0; i < Program.Cast.CastMembers.length; i++) {
          var castMember = Program.Cast.CastMembers[i];
          if (castMember.Role == 'director')
            directors += castMember.Name + ', ';
          else if (castMember.Role == 'actor' || castMember.Role == 'guest_star')
            actors += castMember.Name + ', ';
        }
        if (directors !== '')
          program.directors = directors.substring(0, directors.length - 2);
        if (actors !== '')
          program.actors = actors.substring(0, actors.length - 2);
      }

      $scope.program = program;
      var modalInstance = $modal.open({
        animation: false,
        templateUrl: 'views/details.html',
        controller: 'EpgModalController',
        scope: $scope
      });
    });
  };
  
  $scope.fireEpgAction = function(name) {
    if (typeof CustomEvent == 'function')
      document.dispatchEvent(new CustomEvent('epgAction', {'name': name})); // enable outside listeners
  };
  
  if ($scope.bufferSize === 0)
    $scope.guideData.nextPage();
  
}]);

epgApp.controller('EpgModalController', ['$scope', '$timeout', '$modalInstance', function($scope, $timeout, $modalInstance) {
  $scope.close = function(program) {
    // TODO: better way
    $timeout(function() {
      var progElem = document.getElementById(program.id);
      angular.element(progElem).removeClass('program-select');
      progElem.focus();
    }, 0);    
    $modalInstance.dismiss('close');
  };
}]);

epgApp.directive('popClick', ['$timeout', function($timeout) {
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs) {
      elem.bind('click', function() {
        if (scope.popElem != elem) {
          // calculate placement
          var viewportOffset = elem[0].getBoundingClientRect();
          var topRoom = viewportOffset.top;
          var btmRoom = document.documentElement.clientHeight - viewportOffset.bottom;
          var leftRoom = viewportOffset.left;
          var rightRoom = document.documentElement.clientWidth - viewportOffset.right;
          var topClose = topRoom < 115;
          var btmClose = btmRoom < 115;
          var leftClose = leftRoom < 155;
          var rightClose = rightRoom < 155;
          // leftVClose and rightVClose mean there's no room to display menu on top or bottom
          var leftVClose = leftRoom + elem[0].offsetWidth/2 < 80;
          var rightVClose = document.documentElement.clientWidth - viewportOffset.left - elem[0].offsetWidth/2 < 80;
          var leftXorRightClose = (leftVClose || rightVClose) && !(leftVClose && rightVClose);
          var place = 'top';
          if (topClose || leftXorRightClose) {
            if (btmRoom > topRoom)
              place = 'bottom';
            if (btmClose || leftXorRightClose) {
              if (leftClose && !rightClose)
                place = 'right';
              else if (rightClose && !leftClose)
                place = 'left';
            }
          }
          
          scope.setPopPlace(place);
          scope.$apply();
          $timeout(function() {
            if (scope.popElem === null) {
              elem.triggerHandler('popShow');
              scope.setPopElem(elem);
            }
          }, 0);
        }
      });
    }
  };
}]);

epgApp.directive('onEnter', function() {
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs) {
      elem.bind('keypress', function(event) {
        if (event.which === 13) {
          scope.$apply(function() {
            scope.$eval(attrs.onEnter);
          });
          event.preventDefault();
        }
      });
    }
  };
});

epgApp.directive('focusMe', function() {
  var focusMeLaters = {};
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs) {
      if (attrs.focusMe == 'later') {
        if (focusMeLaters[elem[0].id])
          elem[0].focus();
        else
          focusMeLaters[elem[0].id] = true;
      }
      else {
        elem[0].focus();
      }
    }
  };
});

epgApp.directive('blurMe', function() {
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs) {
      elem.bind('focus', function() {
        elem[0].blur();
      });
    }
  };
});

epgApp.directive('epgRecord', ['$http', function($http) {
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs) {
      elem.bind('click', function() {
        var action = attrs.epgRecord;
        // TODO use a controller
        var url = '/Dvr/';
        if (action == 'single' || action == 'transcode' || action == 'all') {
          url += 'AddRecordSchedule?' + 
              'ChanId=' + scope.program.channel.ChanId + '&' +
              'Station=' + scope.program.channel.CallSign + '&' + 
              'StartTime=' + scope.program.StartTime + '&' + 
              'EndTime=' + scope.program.EndTime + '&' + 
              'Type=' + (action == 'transcode' ? 'single' : action) + '&' + 
              'Title=' + encodeURI(scope.program.Title) + '&' + 
              'FindDay=0&FindTime=00:00:00';
        }
        else if (action == 'dont' || action == 'never') {
          url += 'AddDontRecordSchedule?' + 
              'ChanId=' + scope.program.channel.ChanId + '&' + 
              'StartTime=' + scope.program.StartTime + '&' + 
              'NeverRecord=' + (action == 'never');
        }
        
        if (action == 'transcode')
          url += '&AutoTranscode=true';

        scope.fireEpgAction('record');
        console.log('record action url: ' + url);
        $http.post(url).success(function(data, status, headers, config) {
          scope.program.willRecord = (action == 'single' || action == 'transcode' || action == 'all');
        });
      });
    }
  };
}]);

// GuideData constructor function to encapsulate HTTP and pagination logic
epgApp.factory('GuideData', ['$http', '$timeout', '$window', '$filter', 'ERROR_TAG', function($http, $timeout, $window, $filter, ERROR_TAG) {
  var GuideData = function(startDate, slotWidth, intervalHours, awaitPrime, historyHours, channelGroupId, mythlingServices, demoMode) {
    
    this.slotWidth = slotWidth; // width of 1/2 hour
    
    this.interval = intervalHours * 3600000; // how much data to retrieve at a time
    this.history = historyHours * 3600000;
    
    if (channelGroupId)
      this.channelGroupId = channelGroupId;

    this.busy = false;
    this.prescrolled = false;

    this.preventMobileScroll = function(e) {
      e.preventDefault();
    };
    
    this.setStartTime(startDate);
    this.awaitPrime = awaitPrime;
    this.mythlingServices = mythlingServices;
    this.demoMode = demoMode;
  };

  GuideData.prototype.setStartTime = function(startTime) {

    console.log('startTime: ' + startTime.toISOString());
    
    this.curDate = new Date(startTime);
    
    this.channels = {};

    this.startTime = new Date(startTime);
    this.startTime.setTime(this.startTime.getTime() - this.history);
    // nearest half-hour
    this.startTime.setMinutes(this.startTime.getMinutes() >= 30 ? 30 : 0);
    this.startTime.setSeconds(0);
    this.startTime.setMilliseconds(0);
    this.endTime = new Date(this.startTime.getTime() + this.interval);
    this.zeroTime = new Date(this.startTime);
    this.timeslots = [];
    
    this.index = 10;
  };
  
  // programId = to select, scrollTime = scroll to this time (should be in same day as this.startTime)
  GuideData.prototype.nextPage = function(programId, scrollTime) {
    if (this.busy)
      return;
    this.busy = true;
    
    if (scrollTime && scrollTime !== null)
      this.endTime = new Date(this.startTime.getTime() + 24*3600000); // cover one day
    
    var start = this.startTime.toISOString();
    var end = this.endTime.toISOString();
    
    for (var t = this.startTime.getTime(); t < this.endTime.getTime(); t = t + 1800000) {
      var d = new Date(t);
      var h = d.getHours();
      var ts = h >= 12 ? ' pm' : ' am';
      if (h === 0)
        h = 12;
      else if (h > 12)
        h -= 12;
      ts = h + ':' + (d.getMinutes() === 0 ? '00' : '30') + ts;
      this.timeslots.push(ts);
    }
    
    var url;
    if (this.demoMode)
      url = 'demo/guide-data/';
    else if (this.mythlingServices)
      url = '/mythling/media.php?type=guide&';
    else
      url = '/Guide/GetProgramGuide?';
    var path = 'StartTime=' + start + '&EndTime=' + end;
    if (this.channelGroupId && this.channelGroupId > 0)
      path += "&ChannelGroupId=" + this.channelGroupId;
    url += this.demoMode ? path.replace(/:/g, '-') + '.json' : path;
    console.log('retrieving from: ' + url);
    if (this.awaitPrime)
      angular.element(document.body).bind('touchmove', this.preventMobileScroll);
    
    // going through angular model screws up bootstrap calendar directive
    var cal = angular.element(document.getElementById('calendarInput'));
    cal.addClass('input-warn');
    cal.val('Loading...');
    $timeout(function() {
      cal.val('Loading...');
    }, 75);
    
    $http.get(url).error(function(data, status) {
      console.log(ERROR_TAG + 'HTTP ' + status + ': ' + url);
      this.busy = false;
    }).success(function(data, status, headers, config) {
      console.log('guide data response time: ' + config.responseTime);
      if (typeof this.isMyth28 === 'undefined') {
        this.mythVersion = data.ProgramGuide.Version ? data.ProgramGuide.Version : '0.27';
        console.log('MythTV version ' + this.mythVersion);
        this.isMyth28 = !this.mythVersion.startsWith('0.27');
      }
      var chans = data.ProgramGuide.Channels;
      var chanIdx = 0;
      for (var i = 0; i < chans.length; i++) {
        var chan = chans[i];
        if (chan.Programs.length > 0) {
          chanIdx++;
          var chanNum = chan.ChanNum;
          // pad to 4 digits to ensure proper sorting by chanNum
          while (chanNum.length < 4)
            chanNum = '0' + chanNum;
          if (!(chanNum in this.channels)) {
            chan.programs = {};
            chan.progSize = 0;
            chan.progOffset = 0;
            this.channels[chanNum] = chan;
          }          
          var channel = this.channels[chanNum];
          for (var j = 0; j < chan.Programs.length; j++) {
            var prog = chan.Programs[j];
            var end = new Date(prog.EndTime);
            if (end.getTime() > this.startTime.getTime() - this.history) { // ignore programs that already ended
              var startTime = prog.StartTime;
              if (!(startTime in channel.programs)) {
                channel.programs[startTime] = prog;
                channel.progSize++;
                var start = new Date(prog.StartTime);
                var slotsStartTime = start.getTime() < this.startTime.getTime() ? this.startTime.getTime() : start.getTime();
                var slots = (end.getTime() - slotsStartTime) / 1800000;
                prog.start = start;
                prog.end = end;
                prog.offset = channel.progOffset;
                prog.width = Math.round(this.slotWidth * slots);
                prog.subTitle = prog.SubTitle ? '"' + prog.SubTitle + '"' : '';
                prog.willRecord = prog.Recording.Status == '-1' || prog.Recording.Status == '-2' || prog.Recording.Status == '-3' || 
                    prog.Recording.Status == 'WillRecord' || prog.Recording.Status == 'Recording' || prog.Recording.Status == 'Recorded'; // futureproofing
                prog.channel = channel;
                prog.id = 'ch' + channel.ChanId + 'pr' + prog.StartTime;
                prog.seq = 'ch' + chanIdx + 'pr' + channel.progSize;
                prog.index = this.index++;
                channel.progOffset += prog.width;
              }
            }
          }
        }
      }
      
      if (scrollTime && scrollTime !== null) {
        var scrollTo = (scrollTime.getTime() - this.startTime.getTime()) * this.slotWidth / 1800000;
        $timeout(function() {
          $window.scrollTo(scrollTo, 0);
        }, 0);
      }

      this.startTime.setTime(this.endTime.getTime());
      this.endTime.setTime(this.endTime.getTime() + this.interval);

      if (!this.prescrolled) {
        this.prescrolled = true;
        // scroll programmatically to adjust for history
        // (also reduces initial lag on mobile browsers)
        var toScroll = (this.history * this.slotWidth) / 1800000;
        $timeout(function() {
          $window.scrollBy(toScroll, 0);
        }, 0);
      }
      
      if (programId && programId !== null) {
        $timeout(function() {
          console.log('selecting program: ' + programId);
          document.getElementById(programId).focus();
        }, 0);
      }
      
      if (this.awaitPrime) {
        var pms = this.preventMobileScroll;
        $timeout(function() {
          angular.element(document.body).unbind('touchmove', pms);
        }, 0);
      }
      
      cal.removeClass('input-warn');
      cal.val($filter('date')(this.curDate, 'EEE MMM d'));
      this.busy = false;
      
    }.bind(this));
  };
  
  return GuideData;
}]);

function urlParams() {
  var params = {};
  var search = /([^&=]+)=?([^&]*)/g;
  var match;
  while ((match = search.exec(window.location.search.substring(1))) !== null)
   params[decodeURIComponent(match[1])] = decodeURIComponent(match[2]);
  return params;
}

function parseCssDim(dim) {
  if (dim.endsWith('px'))
    dim = dim.substring(0, dim.lastIndexOf('px'));
  return parseInt(dim);
}

// hack: function for outside access from stay-omb
var setPosition;

// in case js string does not supply startsWith() and endsWith()
if (typeof String.prototype.startsWith != 'function') {
  String.prototype.startsWith = function(prefix) {
    return this.indexOf(prefix) === 0;
  };
}
if (typeof String.prototype.endsWith !== 'function') {
  String.prototype.endsWith = function(suffix) {
      return this.indexOf(suffix, this.length - suffix.length) !== -1;
  };
};
'use strict';

//in case js string does not supply startsWith() and endsWith()
if (typeof String.prototype.startsWith != 'function') {
  String.prototype.startsWith = function(prefix) {
    return this.indexOf(prefix) === 0;
  };
}
if (typeof String.prototype.endsWith !== 'function') {
  String.prototype.endsWith = function(suffix) {
      return this.indexOf(suffix, this.length - suffix.length) !== -1;
  };
}

function parseCssDim(dim) {
  if (dim.endsWith('px'))
    dim = dim.substring(0, dim.lastIndexOf('px'));
  return parseInt(dim);
}

// for old mobile browsers
var stayOmbMod = angular.module('stay', []);

//layout parameters
var labelWidth = 105; 
var headerHeight = 50;
for (var i = 0; i < document.styleSheets.length; i++) {
  var sheet = document.styleSheets[i];
  if (sheet.href !== null && sheet.href.endsWith('mythling.css'))  { // allows override (my-mythling.css)
    for (var j = 0; j < sheet.cssRules.length; j++) {
      var rule = sheet.cssRules[j];
      if (rule.selectorText === '.header-height')
        headerHeight = parseCssDim(rule.style.height);
      else if (rule.selectorText === '.label-width')
        labelWidth = parseCssDim(rule.style.width);
    }
  }
}

var timeslotRowA, channelColA, timeslotRowF, channelColF;

var sl,oldSl = 0;
var st,oldSt = 0;

document.addEventListener('DOMContentLoaded', function(event) { 
  timeslotRowA = document.getElementById('timeslot-row-a');
  channelColA = document.getElementById('channel-column-a');
  timeslotRowF = document.getElementById('timeslot-row-f');
  channelColF = document.getElementById('channel-column-f');
});

function scrollHandler() {
  sl = window.pageXOffset;
  if (sl != oldSl) {
    timeslotRowF.style.visibility = 'hidden';
    timeslotRowA.style.visibility = 'visible';
    channelColA.style.visibility = 'hidden';
    channelColF.style.visibility = 'visible';
    
    timeslotRowF.style.left = (labelWidth - sl) + 'px';
    channelColA.style.left = sl + 'px';
    
    oldSl = sl;
    setPosition(sl);
  }

  st = window.pageYOffset;
  if (st != oldSt) {
    channelColF.style.visibility = 'hidden';
    channelColA.style.visibility = 'visible';
    timeslotRowA.style.visibility = 'hidden';
    timeslotRowF.style.visibility = 'visible';
    
    channelColF.style.top = (headerHeight - st) + 'px';
    
    timeslotRowA.style.top = st + 'px';
    
    oldSt = st;
  }
}

window.onscroll = function() {
  scrollHandler();
};;
'use strict';

var epgCalendar = angular.module('epgCalendar', []);

epgCalendar.controller('EpgCalController', ['$scope', '$timeout', function($scope, $timeout) {
  
  $scope.minDate = new Date();
  $scope.maxDate = new Date($scope.minDate.getTime() + 1209600000); // two weeks
  
  $scope.openCalendar = function($event) {
    $event.preventDefault();
    $event.stopPropagation();
    if ($scope.calendarOpened)
      $scope.calendarOpened = false;
    else
      $scope.calendarOpened = true;
    
    if ($scope.popElem !== null) {
      $timeout(function() {
        $scope.popHide();
      }, 0);
    }
  };
  
  $scope.currentDate = function(newValue) {
    if (newValue) {
      var newTime = new Date(newValue);
      var startTime = new Date(newTime);
      startTime.setHours(0);
      startTime.setMinutes(0);
      $scope.guideData.setStartTime(startTime);
      $scope.fireEpgAction('calendar');
      $scope.guideData.nextPage(null, newTime);
    }
    return $scope.guideData.curDate;
  };

}]);
;
'use strict';

var epgSearch = angular.module('epgSearch', []);

epgSearch.controller('EpgSearchController', ['$scope', '$timeout', 'search', function($scope, $timeout, search) {

  $scope.filterVal = "";
  
  $scope.searchOpened = false;
  $scope.searchClick = function($event) {
    $scope.searchOpened = !$scope.searchOpened;
  };
  
  $scope.searchForward = function() {
    $scope.resultsSummary = null;
    search.forward($scope.filterVal, $scope.guideData.curDate, $scope.showResult);
  };
  
  $scope.searchBackward = function() {
    $scope.resultsSummary = null;
    search.backward($scope.filterVal, $scope.guideData.curDate, $scope.showResult);
  };
  
  $scope.showResult = function(results) {
    $scope.resultsSummary = (results.index + 1) + ' / ' + results.programs.length;
    if (results.programs.length > 0) {
      var program = results.programs[results.index];
      var startTime = new Date(program.start);
      startTime.setMinutes(0);
      $scope.guideData.setStartTime(startTime);
      $scope.guideData.nextPage(program.id);
    }
  };
  
  $scope.isSearching = function() {
    return search.isBusy();
  };
  
  $scope.closeSearch = function() {
    $timeout(function() {
      $scope.fireEpgAction('search');
      document.getElementById('searchBtn').click();
    }, 5);
  };

  $scope.searchFilter = function(newValue) {
    if (newValue || newValue === '') {
      $scope.filterVal = newValue;
    }
    else {
      if (!$scope.guideData.isMyth28)
        return 'Search requires MythTV 0.28';
      else if ($scope.guideData.demoMode)
        return 'Search not available in demo';
    }
    return $scope.filterVal;
  };
}]);

epgSearch.factory('search', ['$http', '$q', function($http, $q) {
  
  var busy = false;
  var encodedFilter;
  var results = {
    index: -1,
    programs: [],
    addPrograms: function(progs) {
      for (var i = 0; i < progs.length; i++) {
        var prog = progs[i];
        if (!this.hasProgram(prog)) {
          prog.start = new Date(prog.StartTime);
          prog.id = 'ch' + prog.Channel.ChanId + 'pr' + prog.StartTime;
          this.programs.push(prog);
        }
      }
    },
    hasProgram: function(prog) {
      for (var i = 0; i < this.programs.length; i++) {
        var program = this.programs[i];
        if (program.ChanId == prog.ChanId && program.StartTime == prog.StartTime)
          return true;
      }
    }
  };
  
  function doSearch(filter, curDate, callback) {
    busy = true;
    results.index = -1;
    results.programs = [];
    encodedFilter = filter;

    var startTime = new Date();
    var baseUrl = '/Guide/GetProgramList?StartTime=' + startTime.toISOString();
    console.log('search base url: ' + baseUrl);
    
    $q.all({
      // title search redundant with keyword search?
      // titleSearch: $http.get(baseUrl + '&TitleFilter=' + encodedFilter),
      personSearch: $http.get(baseUrl + '&PersonFilter=' + encodedFilter),
      keywordSearch: $http.get(baseUrl + '&KeywordFilter=' + encodedFilter)
    }).then(function(res) {

      busy = false;

      // console.log('titleSearch:\n============\n' + JSON.stringify(res.titleSearch.data, null, 2));
      // console.log('\npersonSearch:\n============\n' + JSON.stringify(res.personSearch.data, null, 2));
      // console.log('\nkeywordSearch:\n============\n' + JSON.stringify(res.personSearch.data, null, 2));
      
      // results.addPrograms(res.titleSearch.data.ProgramList.Programs);
      results.addPrograms(res.personSearch.data.ProgramList.Programs);
      results.addPrograms(res.keywordSearch.data.ProgramList.Programs);
      
      // initialize the index
      if (results.programs.length > 0) {
        results.index = 0;
        var prog = results.programs[results.index];
        var compareCurDate = new Date(curDate.getTime());
        compareCurDate.setMinutes(0);
        compareCurDate.setSeconds(0);
        compareCurDate.setMilliseconds(0);
        while (prog.start.getTime() < compareCurDate.getTime()) {
          results.index++;
          prog = results.programs[results.index];
        }
      }
      callback(results);
    });
  }
  
  return {
    forward: function(filter, curDate, callback) {
      if (!filter || filter === '')
        return;
      var newFilter = encodeURIComponent(filter);
      if (newFilter != encodedFilter) {
        doSearch(newFilter, curDate, callback);
      }
      if (results.programs.length > 0) {
        results.index++;
        if (results.index == results.programs.length)
          results.index = 0;
        callback(results);
      }
    },
    backward: function(filter, curDate, callback) {
      if (!filter || filter === '')
        return;
      var newFilter = encodeURIComponent(filter);
      if (newFilter != encodedFilter) {
        doSearch(newFilter, curDate, callback);
      }
      if (results.programs.length > 0) {
        results.index--;
        if (results.index == -1)
          results.index = results.programs.length - 1;
        callback(results);
      }
    },
    isBusy: function() {
      return busy;
    },
    getResults: function() {
      return results;
    } 
  };
}]);

epgSearch.directive('searchPreRender', ['$timeout', function($timeout) {
  return {
    restrict: 'A',
    link: function link(scope, elem, attrs) {
      $timeout(function() {
        elem[0].click();
        elem[0].click();
      }, 5);  
    }
  };
}]);
