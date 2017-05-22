'use strict';

function submitBackendHost() {
  var host = document.getElementById('backendHost').value;
  jsHandler.backendHostSubmitted(host);
  return false;
}

function showMessage(message) {
  document.getElementById('message').innerHTML = message; 
}

function enableSubmit(enable) {
  var submit = document.getElementById('submit');
  submit.disabled = !enable;
    
}