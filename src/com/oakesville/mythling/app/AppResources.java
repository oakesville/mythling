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
package com.oakesville.mythling.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.res.Resources;

public class AppResources {

    private Context appContext;

    public AppResources(Context appContext) {
        this.appContext = appContext;
    }

    /**
     * Reads a json file resource from res/raw, stripping out whole-line comments.
     */
    public String readJsonString(int resourceId) throws IOException {
        Resources res = appContext.getResources();
        InputStream stream = res.openRawResource(resourceId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        try {
            StringBuffer str = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.matches("^\\s*//.*$"))
                    str.append(line).append("\n");
            }
            return str.toString();
        }
        finally {
            reader.close();
        }

    }
}
