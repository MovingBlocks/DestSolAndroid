package org.destinationsol.android;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import org.destinationsol.Const;
import org.destinationsol.assets.AssetHelper;
import org.destinationsol.assets.Assets;
import org.destinationsol.assets.music.OggMusic;
import org.destinationsol.assets.sound.OggSound;
import org.destinationsol.assets.emitters.Emitter;
import org.destinationsol.assets.json.Json;
import org.destinationsol.assets.textures.DSTexture;
import org.destinationsol.modules.ModuleManager;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.module.Module;
import org.terasology.gestalt.module.ModuleEnvironment;
import org.terasology.gestalt.module.ModuleMetadata;
import org.terasology.gestalt.module.ModuleMetadataJsonAdapter;
import org.terasology.gestalt.module.ModulePathScanner;
import org.terasology.gestalt.module.TableModuleRegistry;
import org.terasology.gestalt.module.sandbox.APIScanner;
import org.terasology.gestalt.module.sandbox.StandardPermissionProviderFactory;
import org.terasology.gestalt.android.AndroidModuleClassLoader;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.terasology.gestalt.naming.Name;
import org.terasology.gestalt.naming.Version;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.ReflectPermission;
import java.util.Set;

public class AndroidModuleManager extends ModuleManager {
    private Context context;
    private Module engineCodeModule;

    public AndroidModuleManager(Context context) {
        this.context = context;
    }

    @Override
    public void init() throws Exception {
        File filesPath = context.getFilesDir();
        AssetManager assets = context.getAssets();

        try {
            Reader engineModuleReader = new InputStreamReader(assets.open("modules/engine/module.json"), Charsets.UTF_8);
            ModuleMetadata engineMetadata = new ModuleMetadataJsonAdapter().read(engineModuleReader);
            engineModuleReader.close();

            registry = new TableModuleRegistry();
            Set<Module> requiredModules = Sets.newHashSet();

            // The "assets" directory only exists within the APK, which makes it inefficient to traverse
            copyModulesToDataDir(filesPath, assets);

            ModulePathScanner scanner = new ModulePathScanner();
            scanner.scan(registry, new File(filesPath, "modules"));

            InputStream engineReflectionsStream = assets.open("modules/engine/reflections.cache");
            Reflections engineReflections = new ConfigurationBuilder().getSerializer().read(engineReflectionsStream);
            engineReflectionsStream.close();

            Module engineDataModule = registry.getModule(new Name("engine"), new Version(Const.VERSION));
            registry.remove(engineDataModule);
            engineModule = new Module(engineMetadata, engineDataModule.getResources(), engineDataModule.getClasspaths(), engineReflections, x -> true);

            registry.add(engineModule);
            requiredModules.addAll(registry);
            loadEnvironment(requiredModules);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void clearCachedModules(File modulesDir) {
        File[] files = modulesDir.listFiles();
        if (files != null) {
            for (File file : modulesDir.listFiles()) {
                clearCachedModules(file);
            }
        }

        modulesDir.delete();
    }

    private void copyModulesToDataDir(File dataDir, AssetManager assets) {
        File assetVersionFile = new File(dataDir, "version.txt");
        String versionString = "";
        if (assetVersionFile.exists()) {
            try (FileInputStream inputStream = new FileInputStream(assetVersionFile)) {
                try (InputStreamReader streamReader = new InputStreamReader(inputStream, Charsets.UTF_8)) {
                    StringBuilder builder = new StringBuilder();
                    char[] buffer = new char[32];
                    while (streamReader.read(buffer) != -1) {
                        builder.append(buffer);
                    }
                    versionString = builder.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        boolean refreshCache = (!versionString.equals(Const.VERSION) || com.miloshpetrov.sol2.android.BuildConfig.DEBUG);

        if (refreshCache) {
            try (FileOutputStream stream = new FileOutputStream(assetVersionFile)) {
                try (OutputStreamWriter writer = new OutputStreamWriter(stream)) {
                    writer.write(Const.VERSION);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            clearCachedModules(new File(dataDir, "modules"));
        }

        copyModules(dataDir, assets, "modules", refreshCache);

        File musicFolder = new File(dataDir, "music");
        File soundFolder = new File(dataDir, "sound");
        if (!musicFolder.exists()) {
            musicFolder.mkdir();
        }

        if (!soundFolder.exists()) {
            soundFolder.mkdir();
        }
    }

    private void copyModules(File dataDir, AssetManager assets, String rootDir, boolean replaceFiles) {
        try {
            String[] filesToCopy = assets.list(rootDir);
            for (String fileToCopy : filesToCopy) {
                String filePath = rootDir + "/" + fileToCopy;
                File file = new File(dataDir + "/" + rootDir, fileToCopy);
                file.mkdirs();
                if (assets.list(filePath).length > 0) {
                    // File is a directory
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    copyModules(dataDir, assets, filePath, replaceFiles);
                } else {
                    if (file.exists() && replaceFiles) {
                        // Replace old copies with newer ones
                        file.delete();
                    }

                    file.createNewFile();

                    byte[] buffer = new byte[512];
                    try (InputStream inputStream = assets.open(filePath)) {
                        try (FileOutputStream outputStream = new FileOutputStream(file)) {
                            ByteStreams.copy(inputStream, outputStream);
                        } catch (Exception e) {
                            Log.e("DESTINATION_SOL", "", e);
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        Log.e("DESTINATION_SOL", "", e);
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            Log.e("DESTINATION_SOL", "", e);
            e.printStackTrace();
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

        APIScanner scanner = new APIScanner(permissionFactory);
        scanner.scan(reflections);

        // TODO: Android does not appear to allow changing the system security policy. Modules will run without a sandbox (though Android's sandbox should be enough).
        //Policy.setPolicy(new ModuleSecurityPolicy());
        //System.setSecurityManager(new ModuleSecurityManager());

        environment = new ModuleEnvironment(registry, permissionFactory, (module, parent, permissionProvider) -> AndroidModuleClassLoader.create(module, parent, permissionProvider, context.getCodeCacheDir()));
        AssetHelper helper = new AndroidAssetHelper();
        helper.init(environment);
        Assets.initialize(helper);
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
