'use strict';

var calendarOpen = false;
var calendarBtnClick = function(event) {
  calendarOpen = !calendarOpen;
  console.log('calendarOpen: ' + calendarOpen);
  setPopupOpen();
};

var searchOpen = false;
var searchBtnClick = function(event) {
  searchOpen = !searchOpen;
  console.log('searchOpen: ' + searchOpen);
  setPopupOpen();
};

var menuOpen = false;
var menuProgId = null;
function programKey(event) {
  if (event.keyCode == 13) {  // enter
    event.preventDefault();
    event.stopPropagation();
    menuOpen = !menuOpen;
    console.log('menuOpen: ' + menuOpen);
    setPopupOpen();
    var progElem = event.target;
    if (menuOpen)
      menuProgId = progElem.id;
    progElem.parentElement.click();
  }
}

function setPopupOpen() {
  jsHandler.setPopupOpen(searchOpen || calendarOpen || menuOpen);  
}

document.addEventListener('DOMContentLoaded', function(event) {
  document.getElementById('calendarBtn').focus();
  document.getElementById('calendarBtn').addEventListener("click", calendarBtnClick);
  document.getElementById('searchBtn').addEventListener("click", searchBtnClick);
});

document.addEventListener('epgAction', function(event) {
  console.log('popupsClosed');
  if (menuOpen && menuProgId !== null) {
    var progElem = document.getElementById(menuProgId);
    if (event.name == 'close')
      progElem.focus();
  }
  searchOpen = calendarOpen = menuOpen = false;
  setPopupOpen();
});

var offset = 0;

function getOffset(progElem) {
  if (!progElem || !progElem.parentElement)
    return 0;
  return parseInt(progElem.dataset.offset);
}

function getSiblingElementBySeq(progElem, seq) {
  var chanRowElem = progElem.parentElement.parentElement.parentElement; 
  return chanRowElem.querySelector('div[data-seq="' + seq + '"]');
}

function getChanProgElementForOffset(chanIdx, offset) {
  console.log('offset: ' + offset);
  console.log('querySel: ' + 'div[data-seq="ch' + chanIdx + 'pr1"]');
  var firstProgElem = document.querySelector('div[data-seq="ch' + chanIdx + 'pr1"]');
  console.log('firstProgElem: ' + firstProgElem);
  if (!firstProgElem)
    return null;
  console.log('firstProgElem.id: ' + firstProgElem.id);
  var chanRowElem = firstProgElem.parentElement.parentElement.parentElement;
  var chanProgElems = chanRowElem.querySelectorAll('div[data-seq]');
  var progElem = null;
  for (var i = 0; i < chanProgElems.length; i++) {
    progElem = chanProgElems[i];
    var w = progElem.parentElement.style.width;
    var progOffset = parseInt(progElem.dataset.offset) + parseInt(w.substring(0, w.length - 2))/2;
    console.log('progOffset: ' + progOffset);
    if (progOffset >= offset)
      break;
  }
  console.log('found: ' + progElem.id);
  return progElem;
}

var focused = null;

function webViewKey(key) {
  console.log('webViewKey(): ' + key);
  
  var foc = document.activeElement;
  if (foc && foc.id) {
    var oldFocused = focused;
    
    if (foc.id == 'calendarBtn') {
      if (key == 'right')
        focused = document.getElementById('searchBtn');
      else if (key == 'down') {
        focused = document.querySelector('div[data-seq="ch1pr1"]');
        offset = getOffset(focused);
      }
    }
    else if (foc.id == 'searchBtn') {
      if (key == 'left')
        focused = document.getElementById('calendarBtn');
      else if (key == 'right' || key == 'down') {
        focused = document.querySelector('div[data-seq="ch1pr1"]');
        offset = getOffset(focused);
      }
    }
    else if (foc.id && foc.id.startsWith('ch')) {
      if ((foc.dataset.seq == 'ch1pr1' && key == 'left') ||
            (foc.dataset.seq.startsWith('ch1pr') && key == 'up')) {
          focused = document.getElementById('searchBtn');
      }
      else {
        var seq = foc.dataset.seq;
        if (seq) {
          var prIdx = seq.indexOf('pr');
          if (prIdx > 0) {
            var focChan = parseInt(seq.substring(2, prIdx));
            var focProg = parseInt(seq.substring(prIdx + 2));
            if ('right' == key) {
              focused = getSiblingElementBySeq(foc, 'ch' + focChan + 'pr' + (focProg + 1));
              offset = getOffset(focused);
            }
            else if ('left' == key) {
              focused = getSiblingElementBySeq(foc, 'ch' + focChan + 'pr' + (focProg - 1));
              offset = getOffset(focused);
              if (offset === 0)
                document.body.scrollLeft = 0; 
            }
            else if ('up' == key) {
              if (focChan === 0) {
                focused = document.getElementById('searchBtn');
              }
              else {
                focused = getChanProgElementForOffset(focChan - 1, offset);
              }
            }
            else if ('down' == key) {
              focused = getChanProgElementForOffset(focChan + 1, offset);
            }
          }
        }
      }
    }
    
    if (oldFocused !== null && oldFocused.id && oldFocused.id.startsWith('ch')) {
      oldFocused.removeEventListener('keypress', programKey);
      
    }
    if (focused.id && focused.id.startsWith('ch')) {
      focused.addEventListener('keypress', programKey);      
    }
  }

  if (focused !== null) {
    focused.focus();
    console.log('new focused id: ' + focused.id);
  }
}

function closePopups() {
  if (calendarOpen) {
    var calBtn = document.getElementById('calendarBtn');
    calBtn.click();
    calBtn.focus();
  }
  if (searchOpen) {
    document.getElementById('searchBtn').click();
  }
  if (menuOpen && menuProgId !== null) {
    var progElem = document.getElementById(menuProgId);
    progElem.focus();
    progElem.parentElement.click();
    menuOpen = false;
    console.log('menuOpen: ' + menuOpen);
  }
    
}
