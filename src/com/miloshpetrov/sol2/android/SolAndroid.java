package com.miloshpetrov.sol2.android;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.content.Context;
import android.util.Log;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidAudio;
import com.badlogic.gdx.backends.android.AsynchronousAndroidAudio;
import com.google.common.reflect.Reflection;
import org.destinationsol.SolApplication;
import org.destinationsol.modules.FacadeModuleConfig;
import org.terasology.context.Lifetime;
import org.terasology.gestalt.android.AndroidAssetsFileSource;
import org.terasology.gestalt.android.AndroidModuleClassLoader;
import org.terasology.gestalt.android.AndroidModulePathScanner;
import org.terasology.gestalt.di.ServiceRegistry;
import org.terasology.gestalt.di.index.UrlClassIndex;
import org.terasology.gestalt.module.Module;
import org.terasology.gestalt.module.ModuleEnvironment;
import org.terasology.gestalt.module.ModuleMetadataJsonAdapter;
import org.terasology.gestalt.module.ModulePathScanner;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;

public class SolAndroid extends AndroidApplication {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();

        try {
            initialize(new SolApplication(60.0f, new AndroidServices(getAssets(), getCodeCacheDir())), config);
        } catch (Exception e) {
            Log.e("DESTINATION_SOL", "FATAL ERROR: Forced abort!", e);
        }
    }

    @Override
    public AndroidAudio createAudio(Context context, AndroidApplicationConfiguration config) {
        return new AsynchronousAndroidAudio(context, config);
    }

    private static class AndroidServices extends ServiceRegistry {
        public AndroidServices(AssetManager assets, File codeCacheDir) {
            this.with(FacadeModuleConfig.class).lifetime(Lifetime.Singleton).use(() -> new AndroidModuleConfig(assets, codeCacheDir));
            this.with(ModulePathScanner.class).lifetime(Lifetime.Singleton).use(() -> new AndroidModulePathScanner(assets, codeCacheDir));
        }
    }

    private static class AndroidModuleConfig implements FacadeModuleConfig {
        private final AssetManager assets;
        private final File codeCacheDir;

        public AndroidModuleConfig(AssetManager assets, File codeCacheDir) {
            this.assets = assets;
            this.codeCacheDir = codeCacheDir;
        }

        @Override
        public File getModulesPath() {
            return new File("modules");
        }

        // Android does not allow changing the system security policy.
        // Modules should still be restricted via classpath filtering though.
        @Override
        public boolean useSecurityManager() {
            return false;
        }

        @Override
        public Module createEngineModule() {
            try {
                InputStream engineModuleMetadataStream = assets.open("engine/module.json");
                return new Module(new ModuleMetadataJsonAdapter().read(new InputStreamReader(engineModuleMetadataStream)),
                        new AndroidAssetsFileSource(assets, "engine"),
                        Collections.emptyList(), UrlClassIndex.byClassLoaderPrefix("org.destinationsol"), x -> {
                    String classPackageName = Reflection.getPackageName(x);
                    return "org.destinationsol".equals(classPackageName) || classPackageName.startsWith("org.destinationsol.");
                });
            } catch (Exception e) {
                Log.e("DestinationSol", "Error loading engine module!");
                return null;
            }
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
