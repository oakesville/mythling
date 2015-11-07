/*! mythling-epg v1.2.1
 *  Copyright 2015 Donald Oakes
 *  License: Apache-2.0 */
'use strict';

var calendarOpen = false;
var menuOpen = false;
var detailsOpen = false;
var searchOpen = false;

document.addEventListener('epgAction', function(event) {
  if (event.detail.startsWith('open.'))
    setPopup(event.detail.substring(5), true);
  else if (event.detail.startsWith('close.'))
    setPopup(event.detail.substring(6), false);
});

function setPopup(popup, open) {
  
  if (popup == 'calendar')
    calendarOpen = open;
  else if (popup == 'menu')
    menuOpen = open;
  else if (popup == 'details')
    detailsOpen = open;
  else if (popup == 'search')
    searchOpen = open;
  
  if (open)
    jsHandler.popupOpened(popup);
  else
    jsHandler.popupClosed(popup);
}

function closePopup() {
  if (calendarOpen) {
    document.getElementById('calendarBtn').click();
    calendarOpen = false;
  }
  else if (menuOpen) {
    popHide();
    menuOpen = false;
  }
  else if (detailsOpen) {
    document.getElementById('detailsCloseBtn').click();
    detailsOpen = false;
  }
  else if (searchOpen) {
    document.getElementById('searchBtn').click();
    searchOpen = false;
  }
}
