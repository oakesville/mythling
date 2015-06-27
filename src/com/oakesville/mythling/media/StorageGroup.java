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
package com.oakesville.mythling.media;

import java.util.ArrayList;
import java.util.List;

public class StorageGroup {

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private String host;
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    private List<String> directories;
    public List<String> getDirectories() { return directories; }
    public void setDirectories(List<String> dirs) { this.directories = dirs; }

    public void addDirectory(String dir) {
        directories.add(dir);
    }

    public StorageGroup(String name) {
        this.name = name;
        this.directories = new ArrayList<String>();
    }
}
