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
package org.destinationsol.android;

import org.destinationsol.android.assets.AndroidOggMusicFileFormat;
import org.destinationsol.android.assets.AndroidOggSoundFileFormat;
import org.destinationsol.assets.AssetHelper;
import org.destinationsol.assets.music.OggMusic;
import org.destinationsol.assets.music.OggMusicData;
import org.destinationsol.assets.sound.OggSound;
import org.destinationsol.assets.sound.OggSoundData;
import org.terasology.gestalt.assets.AssetType;
import org.terasology.gestalt.assets.module.ModuleAwareAssetTypeManagerImpl;
import org.terasology.gestalt.module.ModuleEnvironment;

public class AndroidAssetHelper extends AssetHelper {
    @Override
    public void init(ModuleEnvironment environment) {
        assetTypeManager = new ModuleAwareAssetTypeManagerImpl();

        AssetType<OggSound, OggSoundData> soundType = assetTypeManager.createAssetType(OggSound.class, OggSound::new, "sounds");
        AssetType<OggMusic, OggMusicData> musicType = assetTypeManager.createAssetType(OggMusic.class, OggMusic::new, "music");

        assetTypeManager.getAssetFileDataProducer(soundType).addAssetFormat(new AndroidOggSoundFileFormat());
        assetTypeManager.getAssetFileDataProducer(musicType).addAssetFormat(new AndroidOggMusicFileFormat());
        
        assetTypeManager.switchEnvironment(environment);
    }
}
