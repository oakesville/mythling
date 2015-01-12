package com.oakesville.mythling.prefs;

import java.util.Collection;
import java.util.Map;

import android.preference.PreferenceFragment;

public interface DevicePrefsConstraints {
    
    /**
     * android.os.Build.MANUFACTURER, android.os.Build.MODEL
     */
    public boolean appliesToDevice(String manufacturer, String model);
    
    /**
     * resource ids of omitted preference headers
     */
    public Collection<Long> getOmittedHeaders();
    
    /**
     * map of hardwired prefs values
     */
    public Map<String,Object> getHardWiredPrefs();
    
    /**
     * custom layout for specific fragment
     */
    public int getPrefsResource(Class<? extends PreferenceFragment> prefsClass);
}
