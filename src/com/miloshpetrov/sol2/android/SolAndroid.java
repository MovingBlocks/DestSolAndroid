package com.miloshpetrov.sol2.android;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import org.destinationsol.Const;
import org.destinationsol.SolApplication;
import org.destinationsol.modules.FacadeModuleConfig;
import org.terasology.context.Lifetime;
import org.terasology.gestalt.android.AndroidModuleClassLoader;
import org.terasology.gestalt.di.ServiceRegistry;
import org.terasology.gestalt.module.ModuleEnvironment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class SolAndroid extends AndroidApplication {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();

        File filesDir = getFilesDir();
        copyModulesToDataDir(filesDir);

        try {
            initialize(new SolApplication(60.0f, new AndroidServices(filesDir, getCodeCacheDir())), config);
        } catch (Exception e) {
            Log.e("DESTINATION_SOL", "FATAL ERROR: Forced abort!", e);
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

    private void copyModulesToDataDir(File dataDir) {
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

        copyModules(dataDir, "modules", refreshCache);

        File musicFolder = new File(dataDir, "music");
        File soundFolder = new File(dataDir, "sound");
        if (!musicFolder.exists()) {
            musicFolder.mkdir();
        }

        if (!soundFolder.exists()) {
            soundFolder.mkdir();
        }
    }

    private void copyModules(File dataDir, String rootDir, boolean replaceFiles) {
        AssetManager assets = getAssets();
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
                    copyModules(dataDir, filePath, replaceFiles);
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

    private static class AndroidServices extends ServiceRegistry {
        public AndroidServices(File filesDir, File codeCacheDir) {
            this.with(FacadeModuleConfig.class).lifetime(Lifetime.Singleton).use(() -> new AndroidModuleConfig(filesDir, codeCacheDir));
        }
    }

    private static class AndroidModuleConfig implements FacadeModuleConfig {
        private final File filesDir;
        private final File codeCacheDir;

        public AndroidModuleConfig(File filesDir, File codeCacheDir) {
            this.filesDir = filesDir;
            this.codeCacheDir = codeCacheDir;
        }

        @Override
        public File getModulesPath() {
            return new File(filesDir, "modules");
        }

        // Android does not allow changing the system security policy.
        // Modules should still be restricted via classpath filtering though.
        @Override
        public boolean useSecurityManager() {
            return false;
        }

        @Override
        public ModuleEnvironment.ClassLoaderSupplier getClassLoaderSupplier() {
            return (module, parent, permissionProvider) -> AndroidModuleClassLoader.create(module, parent, permissionProvider, codeCacheDir);
        }

        @Override
        public Class<?>[] getAPIClasses() {
            return new Class<?>[0];
        }
    }
}
