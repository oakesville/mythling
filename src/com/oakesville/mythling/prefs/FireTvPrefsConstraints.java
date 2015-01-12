package com.oakesville.mythling.prefs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.preference.PreferenceFragment;

import com.oakesville.mythling.R;
import com.oakesville.mythling.app.AppSettings;

public class FireTvPrefsConstraints implements DevicePrefsConstraints {

    @Override
    public boolean appliesToDevice(String manufacturer, String model) {
        return "Amazon".equals(manufacturer) && model != null && model.startsWith("AFT");
    }

    public Collection<Long> getOmittedHeaders() {
        List<Long> omitted = new ArrayList<Long>();
        omitted.add(Long.valueOf(R.id.prefs_header_playback));
        return omitted;
    }

    public int getPrefsResource(Class<? extends PreferenceFragment> prefsClass) {
        if (prefsClass.equals(NetworkPrefs.class))
            return R.xml.network_prefs_no_ext;
        else if (prefsClass.equals(ConnectionsPrefs.class))
            return R.xml.connect_prefs_no_mythweb;
        return 0;
    }

    public Map<String,Object> getHardWiredPrefs() {
        Map<String,Object> hardwired = new HashMap<String,Object>();
        hardwired.put(AppSettings.INTERNAL_VIDEO_PLAYER, true);
        return hardwired;
    }

}
