package org.destinationsol.android;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import com.google.common.collect.Sets;
import com.google.common.reflect.Reflection;
import org.destinationsol.assets.AssetHelper;
import org.destinationsol.assets.Assets;
import org.destinationsol.assets.music.OggMusic;
import org.destinationsol.assets.sound.OggSound;
import org.destinationsol.assets.emitters.Emitter;
import org.destinationsol.assets.json.Json;
import org.destinationsol.assets.textures.DSTexture;
import org.destinationsol.modules.ModuleManager;
import org.reflections.serializers.XmlSerializer;
import org.terasology.gestalt.android.AndroidAssetsFileSource;
import org.terasology.gestalt.android.AndroidModuleClassLoader;
import org.terasology.gestalt.android.AndroidModulePathScanner;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.module.Module;
import org.terasology.gestalt.module.ModuleEnvironment;
import org.terasology.gestalt.module.ModuleFactory;
import org.terasology.gestalt.module.ModuleMetadata;
import org.terasology.gestalt.module.ModuleMetadataJsonAdapter;
import org.terasology.gestalt.module.TableModuleRegistry;
import org.terasology.gestalt.module.resources.EmptyFileSource;
import org.terasology.gestalt.module.sandbox.APIScanner;
import org.terasology.gestalt.module.sandbox.StandardPermissionProviderFactory;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.terasology.gestalt.naming.Name;
import org.terasology.gestalt.naming.Version;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.ReflectPermission;
import java.util.Collections;
import java.util.Set;

public class AndroidModuleManager extends ModuleManager {
    private Context context;

    public AndroidModuleManager(Context context) {
        this.context = context;
    }

    @Override
    public void init() throws Exception {
        File filesPath = context.getFilesDir();
        File moduleDexesPath = context.getCodeCacheDir();
        AssetManager assets = context.getAssets();

        try {
            ModuleFactory moduleFactory = new ModuleFactory();
            moduleFactory.setManifestFileType("reflections.cache", new XmlSerializer());
            ModuleMetadataJsonAdapter metadataJsonAdapter = new ModuleMetadataJsonAdapter();
            InputStream engineModuleMetadataStream = assets.open("engine/module.json");
            engineModule = new Module(metadataJsonAdapter.read(new InputStreamReader(engineModuleMetadataStream)),
                    new AndroidAssetsFileSource(assets, "engine"),
                    Collections.emptyList(), new Reflections().collect(assets.open("engine/reflections.cache")), x -> {
                String classPackageName = Reflection.getPackageName(x);
                return "org.destinationsol".equals(classPackageName) || classPackageName.startsWith("org.destinationsol.");
            });
            // In order for the NUI widgets to be detected, they first need to be found and cached. The build script
            // reflects over the NUI jar and saves a list of all the widgets within the engine's reflections.cache.
            // TODO: Find a better way to do this.
            Module nuiModule = new Module(new ModuleMetadata(new Name("nui"), new Version("2.0.0")), new EmptyFileSource(),
                    Collections.emptyList(), new Reflections("org.terasology.nui"), x -> {
                String classPackageName = Reflection.getPackageName(x);
                return "org.terasology.nui".equals(classPackageName) || classPackageName.startsWith("org.terasology.nui.");
            });

            registry = new TableModuleRegistry();
            registry.add(engineModule);
            registry.add(nuiModule);
            Set<Module> requiredModules = Sets.newHashSet();

            AndroidModulePathScanner scanner = new AndroidModulePathScanner(assets, moduleDexesPath);
            scanner.scan(registry, new File("modules"));

            requiredModules.addAll(registry);
            loadEnvironment(requiredModules);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void loadEnvironment(Set<Module> modules) {
        StandardPermissionProviderFactory permissionFactory = new StandardPermissionProviderFactory();
        for (String api : API_WHITELIST) {
            permissionFactory.getBasePermissionSet().addAPIPackage(api);
        }

        for (Class<?> apiClass : CLASS_WHITELIST) {
            permissionFactory.getBasePermissionSet().addAPIClass(apiClass);
        }

        // The JSON serializers need to reflect classes to discover what exists
        permissionFactory.getBasePermissionSet().grantPermission("com.google.gson", ReflectPermission.class);
        permissionFactory.getBasePermissionSet().grantPermission("com.google.gson.internal", ReflectPermission.class);
        permissionFactory.getBasePermissionSet().grantPermission("com.google.gson", RuntimePermission.class);
        permissionFactory.getBasePermissionSet().grantPermission("com.google.gson.internal", RuntimePermission.class);

        ConfigurationBuilder config = new ConfigurationBuilder().addClassLoader(ClasspathHelper.contextClassLoader())
                .addUrls(ClasspathHelper.forClassLoader())
                .addScanners(new TypeAnnotationsScanner(), new SubTypesScanner());
        Reflections reflections = new Reflections(config);
        for (Module module : registry) {
            reflections.merge(module.getModuleManifest());
        }

        APIScanner scanner = new APIScanner(permissionFactory, getClass().getClassLoader());
        scanner.scan(reflections);

        // TODO: Android does not appear to allow changing the system security policy. Modules will run without a sandbox (though Android's sandbox should be enough).
        //Policy.setPolicy(new ModuleSecurityPolicy());
        //System.setSecurityManager(new ModuleSecurityManager());

        environment = new ModuleEnvironment(registry, permissionFactory, (module, parent, permissionProvider) -> AndroidModuleClassLoader.create(module, parent, permissionProvider, context.getCodeCacheDir()));
    }

    public void printAvailableModules() {
        AssetHelper assetHelper = Assets.getAssetHelper();
        Set<ResourceUrn> jsonList = assetHelper.list(Json.class);
        Set<ResourceUrn> emitterList = assetHelper.list(Emitter.class);
        Set<ResourceUrn> soundList = assetHelper.list(OggSound.class);
        Set<ResourceUrn> musicList = assetHelper.list(OggMusic.class);
        Set<ResourceUrn> textureList = assetHelper.list(DSTexture.class);

        for (Module module : registry) {
            String moduleName = module.getId().toString();

            Log.d("DESTINATION_SOL_DEBUG", "Module Discovered: " + module.toString());

            int armors = 0;
            int abilityCharges = 0;
            int clips = 0;
            int engines = 0;
            int shields = 0;
            int jsonOthers = 0;
            int emitters = 0;
            int sounds = 0;
            int music = 0;
            int textures = 0;

            for (ResourceUrn assetUrn : jsonList) {
                String assetName = assetUrn.toString();

                if (assetName.startsWith(moduleName + ":")) {
                    if (assetName.endsWith("Armor")) {
                        armors++;
                    } else if (assetName.endsWith("AbilityCharge")) {
                        abilityCharges++;
                    } else if (assetName.endsWith("Clip")) {
                        clips++;
                    } else if (assetName.endsWith("Engine")) {
                        engines++;
                    } else if (assetName.endsWith("Shield")) {
                        shields++;
                    } else {
                        jsonOthers++;
                    }
                }
            }

            for (ResourceUrn assetUrn : emitterList) {
                String assetName = assetUrn.toString();

                if (assetName.startsWith(moduleName + ":")) {
                    emitters++;
                }
            }

            for (ResourceUrn assetUrn : soundList) {
                String assetName = assetUrn.toString();

                if (assetName.startsWith(moduleName + ":")) {
                    sounds++;
                }
            }

            for (ResourceUrn assetUrn : musicList) {
                String assetName = assetUrn.toString();

                if (assetName.startsWith(moduleName + ":")) {
                    music++;
                }
            }

            for (ResourceUrn assetUrn : textureList) {
                String assetName = assetUrn.toString();

                if (assetName.startsWith(moduleName + ":")) {
                    textures++;
                }
            }

            Log.d("DESTINATION_SOL_DEBUG", "\t-Items:");
            Log.d("DESTINATION_SOL_DEBUG", "\t\t-Armors: " + Integer.toString(armors));
            Log.d("DESTINATION_SOL_DEBUG", "\t\t-AbilityCharges: " + Integer.toString(abilityCharges));
            Log.d("DESTINATION_SOL_DEBUG", "\t\t-Clips: " + Integer.toString(clips));
            Log.d("DESTINATION_SOL_DEBUG", "\t\t-Engines: " + Integer.toString(engines));
            Log.d("DESTINATION_SOL_DEBUG", "\t\t-Shields: " + Integer.toString(shields));
            Log.d("DESTINATION_SOL_DEBUG", "\t\t-Others: " + Integer.toString(jsonOthers));

            Log.d("DESTINATION_SOL_DEBUG", "\t-Emitters: " + Integer.toString(emitters));

            Log.d("DESTINATION_SOL_DEBUG", "\t-Sounds: " + Integer.toString(sounds));

            Log.d("DESTINATION_SOL_DEBUG", "\t-Music: " + Integer.toString(music));

            Log.d("DESTINATION_SOL_DEBUG", "\t-Textures: " + Integer.toString(textures));

            Log.d("DESTINATION_SOL_DEBUG", "");
        }
    }
}
