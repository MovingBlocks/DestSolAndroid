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

import org.destinationsol.android.assets.AndroidDSTextureFileFormat;
import org.destinationsol.android.assets.AndroidEmitterFileFormat;
import org.destinationsol.android.assets.AndroidJsonFileFormat;
import org.destinationsol.android.assets.AndroidOggMusicFileFormat;
import org.destinationsol.android.assets.AndroidOggSoundFileFormat;
import org.destinationsol.assets.AssetHelper;
import org.destinationsol.assets.emitters.Emitter;
import org.destinationsol.assets.emitters.EmitterData;
import org.destinationsol.assets.json.Json;
import org.destinationsol.assets.json.JsonData;
import org.destinationsol.assets.music.OggMusic;
import org.destinationsol.assets.music.OggMusicData;
import org.destinationsol.assets.sound.OggSound;
import org.destinationsol.assets.sound.OggSoundData;
import org.destinationsol.assets.textures.DSTexture;
import org.destinationsol.assets.textures.DSTextureData;
import org.terasology.gestalt.assets.Asset;
import org.terasology.gestalt.assets.AssetData;
import org.terasology.gestalt.assets.AssetType;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.assets.module.ModuleAwareAssetTypeManagerImpl;
import org.terasology.gestalt.module.ModuleEnvironment;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class AndroidAssetHelper extends AssetHelper {
    @Override
    public void init(ModuleEnvironment environment) {
        assetTypeManager = new ModuleAwareAssetTypeManagerImpl();

        AssetType<OggSound, OggSoundData> soundType = assetTypeManager.createAssetType(OggSound.class, OggSound::new, "sounds");
        AssetType<OggMusic, OggMusicData> musicType = assetTypeManager.createAssetType(OggMusic.class, OggMusic::new, "music");
        AssetType<Emitter, EmitterData> emitterType = assetTypeManager.createAssetType(Emitter.class, Emitter::new, "emitters");
        AssetType<Json, JsonData> jsonType = assetTypeManager.createAssetType(Json.class, Json::new, "collisionMeshes", "ships", "items", "configs", "grounds", "mazes", "asteroids", "schemas");
        AssetType<DSTexture, DSTextureData> textureType = assetTypeManager.createAssetType(DSTexture.class, DSTexture::new, "textures", "ships", "items", "grounds", "mazes", "asteroids", "fonts");

        assetTypeManager.getAssetFileDataProducer(soundType).addAssetFormat(new AndroidOggSoundFileFormat());
        assetTypeManager.getAssetFileDataProducer(musicType).addAssetFormat(new AndroidOggMusicFileFormat());
        assetTypeManager.getAssetFileDataProducer(emitterType).addAssetFormat(new AndroidEmitterFileFormat());
        assetTypeManager.getAssetFileDataProducer(jsonType).addAssetFormat(new AndroidJsonFileFormat());
        assetTypeManager.getAssetFileDataProducer(textureType).addAssetFormat(new AndroidDSTextureFileFormat());
        
        assetTypeManager.switchEnvironment(environment);
    }

    // The following three methods are overrides in order to fix the runtime errors that occur

    @Override
    public <T extends Asset<U>, U extends AssetData> Optional<T> get(ResourceUrn urn, Class<T> type) {
        return assetTypeManager.getAssetManager().getAsset(urn, type);
    }

    @Override
    public Set<ResourceUrn> list(Class<? extends Asset<?>> type) {
        return assetTypeManager.getAssetManager().getAvailableAssets(type);
    }

    @Override
    public Set<ResourceUrn> list(Class<? extends Asset<?>> type, String regex) {
        Set<ResourceUrn> finalList = new HashSet<>();

        Set<ResourceUrn> resourceList = assetTypeManager.getAssetManager().getAvailableAssets(type);
        for (ResourceUrn resourceUrn : resourceList) {
            if (resourceUrn.toString().matches(regex)) {
                finalList.add(resourceUrn);
            }
        }

        return finalList;
    }
}
