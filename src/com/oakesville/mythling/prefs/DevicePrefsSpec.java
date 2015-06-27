/**
 * Copyright 2015 Donald Oakes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oakesville.mythling.prefs;

import java.util.Map;

public interface DevicePrefsSpec {
    
    /**
     * android.os.Build.MANUFACTURER, android.os.Build.MODEL
     */
    public boolean appliesToDevice(String manufacturer, String model);
    
    /**
     * resource id of preference headers
     */
    public int getPrefsHeadersResource();
    
    /**
     * map of specialized default prefs values
     */
    public Map<String,Object> getDefaultValues();
    
    /**
     * whether web links can be opened in an external browser
     */
    public boolean supportsWebLinks();
    
}
