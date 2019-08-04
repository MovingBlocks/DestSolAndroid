/*
 * Copyright 2018 MovingBlocks
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
package org.destinationsol.android.assets;

import com.badlogic.gdx.files.FileHandle;
import org.destinationsol.assets.json.JsonData;
import org.json.JSONObject;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.format.AbstractAssetFileFormat;
import org.terasology.assets.format.AssetDataFile;
import org.terasology.assets.module.annotations.RegisterAssetFileFormat;

import java.io.IOException;
import java.util.List;

@RegisterAssetFileFormat
public class AndroidJsonFileFormat extends AbstractAssetFileFormat<JsonData> {
    public AndroidJsonFileFormat() {
        super("json");
    }

    @Override
    public JsonData load(ResourceUrn urn, List<AssetDataFile> inputs) throws IOException {
        FileHandle handle = new AndroidAssetDataFileHandle(inputs.get(0));
        JSONObject jsonValue;
        try {
            jsonValue = new JSONObject(handle.readString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException();
        }
        return new JsonData(jsonValue);
    }
}
