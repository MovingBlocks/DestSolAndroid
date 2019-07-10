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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import org.destinationsol.assets.audio.OggSoundData;
import org.terasology.assets.ResourceUrn;
import org.terasology.assets.format.AbstractAssetFileFormat;
import org.terasology.assets.format.AssetDataFile;
import org.terasology.assets.module.annotations.RegisterAssetFileFormat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RegisterAssetFileFormat
public class AndroidOggSoundFileFormat extends AbstractAssetFileFormat<OggSoundData> {
    public AndroidOggSoundFileFormat() {
        super("ogg");
    }

    @Override
    public OggSoundData load(ResourceUrn urn, List<AssetDataFile> inputs) throws IOException {
        // HACK: LibGDX will only accept an AndroidFileHandle (it casts to one internally). The class has a private
        //       constructor, so we cannot use the same workaround as with AssetDataFileHandle. The only plausible way
        //        to do this appears to be to save the data out to a file and point LibGDX at that.
        AssetDataFile asset = inputs.get(0);
        InputStream fileStream = asset.openStream();
        FileHandle outFile = Gdx.files.local("sound/" + asset.getFilename() + asset.getFileExtension());
        if (!outFile.exists()) {
            byte[] buffer = new byte[512];
            while (fileStream.read(buffer) != -1) {
                outFile.writeBytes(buffer, true);
            }
        }

        return new OggSoundData(Gdx.audio.newSound(outFile));
    }
}
