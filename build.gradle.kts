import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.tasks.TaskExecutionException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()

        // Needed for the Android Gradle Plugin to work
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.3.1")
    }
}

plugins {
    id("destination-sol-constants")
}

apply(plugin = "com.android.application")

repositories {
    mavenLocal()

    // Good ole Maven central
    mavenCentral()

    // For the Android libraries
    google()

    // Repos for LibGDX
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }

    // Terasology Artifactory for any shared libs
    maven {
        url = uri("https://artifactory.terasology.io/artifactory/virtual-repo-live")
    }

    // everit-org JSON schema dependency
    maven { url = uri("https://jitpack.io") }

    // Needed for Jsemver, which is a gestalt dependency
    maven { url = uri("https://heisluft.de/maven/") }

    maven { url = uri("https://maven.google.com") }
}

val natives: Configuration = configurations.create("natives")

val gdxVersion = extra["gdxVersion"] as String
val gestaltVersion = extra["gestaltVersion"] as String

dependencies {
    // TODO: Maybe exclude the commons-logging dependency from main (or set it as 'provided' here)
    "api"("com.google.guava:guava:27.0.1-android")
    "implementation"("com.google.code.gson:gson:2.8.5")
    "implementation"("org.apache.commons:commons-vfs2:2.2")
    "implementation"("com.android.support:support-annotations:28.0.0")
    "implementation"("net.jcip:jcip-annotations:1.0")
    "implementation"("net.sf.trove4j:trove4j:3.0.3")
    "implementation"("com.google.protobuf:protobuf-java:3.4.0")
    "implementation"("com.googlecode.gentyref:gentyref:1.2.0")
    "implementation"("com.github.everit-org.json-schema:org.everit.json.schema:1.9.2")

    "implementation"(project(":engine")) {
        // Resolves duplicate class errors
        exclude(group = "org.reflections")
        // The default JOML package doesn't compile with the Android tooling, so we use a jdk3 variant
        exclude(group = "org.joml")
    }
    // Android-compatible JOML variant
    "implementation"("org.joml:joml-jdk3:1.9.25")
    "implementation"("org.terasology.gestalt:gestalt-android:$gestaltVersion")
    // TODO: Needed for gestalt because of an internal dependency but since that dependency is never
    //       exposed in a public API, I have no idea why it's needed for compilation.
    "implementation"("com.github.zafarkhaja:java-semver:0.10.0")

    "implementation"("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
    "implementation"("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-x86_64")

    // Add reflections purely for NUI. NUI uses the ReflectionUtils class from it.
    "implementation"("org.terasology:reflections:0.9.12-MB")

    // Android-compatible logging
    "implementation"("org.slf4j:slf4j-android:1.7.25")

    // Backport some Java 8 APIs like java.time for gestalt
    "coreLibraryDesugaring"("com.android.tools:desugar_jdk_libs:2.0.2")
}

// Set broken defaults just so Gradle won't complain about non-existent properties. Supply real values externally
var keyStoreToUse = "notset"
var storePassToUse = "notset"
var keyAliasToUse = "notset"
var keyPassToUse = "notset"

// Named to avoid colliding with CommonExtension's own (nullable) compileSdk property, which
// would otherwise shadow this inside the configure<ApplicationExtension> {} block below.
val compileSdkNumber = 36
// Named to avoid colliding with AppExtension.defaultConfig's own (nullable) minSdk/targetSdk
// properties, which would otherwise shadow these inside the defaultConfig {} block below.
val minSdkNumber = 24
val targetSdkNumber = 36

// Load values from properties passed to the project, such as via gradle.properties in the user's home .gradle dir
if (project.hasProperty("signingKeystore")) {
    keyStoreToUse = project.property("signingKeystore") as String
}

if (project.hasProperty("signingStorePass")) {
    storePassToUse = project.property("signingStorePass") as String
}

if (project.hasProperty("signingKeyAlias")) {
    keyAliasToUse = project.property("signingKeyAlias") as String
}

if (project.hasProperty("signingKeyPass")) {
    keyPassToUse = project.property("signingKeyPass") as String
}

// com.android.application is applied via apply(plugin = ...) rather than the plugins{} block
// (its version comes from the buildscript classpath, not the plugin portal), so the type-safe
// android{} accessor was never generated - look the extension up explicitly instead. AGP 9.x
// defaults to android.newDsl=true, under which only the modern ApplicationExtension gets
// registered - the legacy AppExtension isn't available as a project extension at all anymore.
configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "com.miloshpetrov.sol2.android"
    compileSdkVersion(compileSdkNumber)

    // Make it clear we're compiling for Java 8
    compileOptions {
        // Backport some Java 8 APIs like java.time for gestalt
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.directories.clear()
            java.directories.add("src")
            //aidl.srcDirs = ['src']
            renderscript.directories.clear()
            renderscript.directories.add("src")
            res.directories.clear()
            res.directories.add("res")
            assets.directories.clear()
            assets.directories.add("assets")
            jniLibs.directories.clear()
            jniLibs.directories.add("libs")
        }
    }

    packaging {
        resources.excludes.add("org/destinationsol/module.json")
        resources.excludes.add("org/destinationsol/reflections.cache")
        resources.excludes.add("org/destinationsol/assets/**")
    }

    lint {
        abortOnError = false
    }

    packaging {
        resources.merges.add("META-INF/annotations/*")
        resources.merges.add("META-INF/subtypes/*")
        resources.merges.add("META-INF/services/*")

        // The contents of the "gestalt-indexes-present" file is never read, so just pick any one of them
        resources.pickFirsts.add("META-INF/gestalt-indexes-present")
    }

    defaultConfig {
        targetSdk = targetSdkNumber
        minSdk = minSdkNumber
        multiDexEnabled = true
    }

    signingConfigs {
        create("release") {
            storeFile = file(keyStoreToUse)
            storePassword = storePassToUse
            keyAlias = keyAliasToUse
            keyPassword = keyPassToUse
        }
    }

    buildTypes {
        getByName("release") {
            isDebuggable = false
            isJniDebuggable = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

// called every time gradle gets executed, takes the native dependencies of
// the natives configuration, and extracts them to the proper libs/ folders
// so they get packed with the APK.
val copyAndroidNatives = tasks.register("copyAndroidNatives") {
    doFirst {
        file("libs/armeabi-v7a/").mkdirs()
        file("libs/arm64-v8a/").mkdirs()
        file("libs/x86_64/").mkdirs()
        file("libs/x86/").mkdirs()

        natives.copy().files.forEach { jar ->
            var outputDir: File? = null
            if (jar.name.endsWith("natives-arm64-v8a.jar")) outputDir = file("libs/arm64-v8a")
            if (jar.name.endsWith("natives-armeabi-v7a.jar")) outputDir = file("libs/armeabi-v7a")
            if (jar.name.endsWith("natives-x86_64.jar")) outputDir = file("libs/x86_64")
            if (jar.name.endsWith("natives-x86.jar")) outputDir = file("libs/x86")
            if (outputDir != null) {
                copy {
                    from(zipTree(jar))
                    into(outputDir)
                    include("*.so")
                }
            }
        }
    }
}

tasks.whenTaskAdded {
    if (name.contains("package")) {
        dependsOn(copyAndroidNatives)
    }
}

fun deleteDir(dir: File) {
    if (!Files.isSymbolicLink(dir.toPath())) {
        val files = dir.listFiles()
        if (files != null) {
            for (file in files) {
                deleteDir(file)
            }
        }
    }

    dir.delete()
}

abstract class PropertyBasedSync : Sync() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    override fun getDestinationDir(): File = outputDirectory.get().asFile

    override fun setDestinationDir(destination: File) {
        outputDirectory.set(destination)
    }
}

abstract class PropertyBasedGenericTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
}

fun androidSdkPath(): String {
    val localProperties = project.file("../local.properties")
    return if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { instr ->
            properties.load(instr)
        }
        val sdkDir = properties.getProperty("sdk.dir")
        sdkDir ?: (System.getenv("ANDROID_HOME") ?: "")
    } else {
        System.getenv("ANDROID_HOME") ?: ""
    }
}

configure<ApplicationAndroidComponentsExtension> {
    var canCreateSymlinks = false

    try {
        val tempDir = layout.buildDirectory.dir("tmp").get()
        tempDir.asFile.mkdir()
        val testSymlink = tempDir.file("test_symlink").asFile
        Files.createSymbolicLink(testSymlink.toPath(), Paths.get("$rootDir/engine/build/resources/main/org/destinationsol"))
        testSymlink.delete()
        canCreateSymlinks = true
    } catch (ignore: Exception) {
        // Copy the files instead
        canCreateSymlinks = false
    }

    val exportEngineModuleByCopy = tasks.register<PropertyBasedSync>("exportModuleEngineByCopy") {
        dependsOn(":engine:processResources")
        from("$rootDir/engine/build/resources/main/org/destinationsol") {
            include("assets/**")
            include("module.json")
        }
        includeEmptyDirs = false
        eachFile {
            relativePath = relativePath.prepend("engine")
        }
    }
    val exportEngineModuleBySymlink = tasks.register<PropertyBasedGenericTask>("exportModuleEngineBySymlink") {
        dependsOn(":engine:processResources")
        doLast {
            outputDirectory.dir("engine").get().asFile.mkdir()
            listOf("assets", "overrides", "deltas", "module.json").forEach { symlinkPath ->
                val sourcePath = "$rootDir/engine/build/resources/main/org/destinationsol/$symlinkPath"
                if (file(sourcePath).exists()) {
                    val destinationPath = outputDirectory.dir("engine").get().dir(symlinkPath).asFile.toPath()
                    if (Files.exists(destinationPath)) {
                        Files.delete(destinationPath)
                    }
                    Files.createSymbolicLink(destinationPath, Paths.get(sourcePath))
                }
            }
        }
    }

    onVariants(selector().all()) { variant ->
        if (canCreateSymlinks) {
            variant.sources.assets?.addGeneratedSourceDirectory(exportEngineModuleBySymlink, PropertyBasedGenericTask::outputDirectory)
        } else {
            variant.sources.assets?.addGeneratedSourceDirectory(exportEngineModuleByCopy, PropertyBasedSync::outputDirectory)
        }
    }

    val sdkPath = androidSdkPath()
    val dexCommand = if (System.getProperty("os.name").lowercase().contains("windows")) "d8.bat" else "d8"
    val buildToolsVersions = File(sdkPath, "build-tools").listFiles { file ->
        val versionParts = file.name.split(".")
        if (versionParts.size < 3) {
            // This is not an SDK build-tools directory
            false
        } else {
            file.isDirectory && versionParts[0].toInt() >= compileSdkNumber
        }
    } ?: emptyArray()

    if (buildToolsVersions.isEmpty()) {
        throw GradleException("An Android SDK build tools version >= $compileSdkNumber could not be found.")
    }

    @Suppress("UNCHECKED_CAST")
    val destinationSolModules = rootProject.extra["destinationSolModules"] as () -> List<Project>
    destinationSolModules().forEach { module ->
        val moduleBuildDir = file("${rootProject.projectDir}/modules/${module.name}/build")
        val moduleClassesDir = file("$moduleBuildDir/classes/")
        val moduleDexesDir = "${rootProject.projectDir}/modules/${module.name}/build/dexes/"
        val codePresent = !fileTree("${rootProject.projectDir}/modules/${module.name}/src/").isEmpty
        var thisDexedModuleJar: TaskProvider<Zip>? = null
        if (codePresent) {
            val thisModuleDexes = tasks.register<Exec>("moduleDexes${module.name}") {
                dependsOn(":modules:${module.name}:classes")
                outputs.dir(moduleDexesDir)
                val dex = "${buildToolsVersions[0]}/$dexCommand"
                val moduleClassesFiles = fileTree(moduleClassesDir).filter { it.isFile && it.name.endsWith(".class") }.files
                val classesRootPath = moduleClassesDir.toPath()
                val moduleClasses = moduleClassesFiles.map { classesRootPath.relativize(it.toPath()) }

                workingDir(moduleClassesDir)
                commandLine(listOf(dex) + moduleClasses.map { it.toString() } + listOf(
                    "--classpath", "$rootDir/engine/build/classes",
                    "--lib", "$sdkPath/platforms/android-$compileSdkNumber/android.jar",
                    "--min-api", "$minSdkNumber",
                    "--output", moduleDexesDir
                ))
            }
            thisDexedModuleJar = tasks.register<Zip>("dexedModuleJar${module.name}") {
                val outputDir = layout.buildDirectory.dir("moduleAssetRoots/${module.name}/assets/modules/${module.name}/build/dexes/")
                outputs.dir(outputDir)
                dependsOn(thisModuleDexes)
                from("${rootProject.projectDir}/modules/${module.name}/build/dexes/") {
                    include("**/*.dex")
                }
                from("$moduleBuildDir/classes/") {
                    exclude("assets/**")
                    exclude("dexes/**")
                    exclude("**/*.class")
                }
                destinationDirectory.set(outputDir)
                archiveFileName.set("${module.name}.jar")
            }
        }

        val exportModulesByCopy = tasks.register<PropertyBasedSync>("exportModule${module.name}ByCopy") {
            if (codePresent) {
                dependsOn(thisDexedModuleJar!!)
            }
            from(rootDir) {
                include("modules/${module.name}/module.json")
                include("modules/${module.name}/assets/**")
                include("modules/${module.name}/overrides/**")
                include("modules/${module.name}/deltas/**")
            }
            from(layout.buildDirectory.dir("moduleAssetRoots/${module.name}/assets/")) {
                include("modules/${module.name}/build/dexes/${module.name}.jar")
            }
            into("modules/${module.name}/")
            preserve {
                include("build/dexes/**")
            }
        }

        val exportModulesBySymlink = tasks.register<PropertyBasedGenericTask>("exportModule${module.name}BySymlink") {
            if (codePresent) {
                dependsOn(thisDexedModuleJar!!)
            }

            doLast {
                val moduleRoot = outputDirectory.file("modules/${module.name}").get().asFile
                moduleRoot.mkdirs()
                listOf("assets", "overrides", "deltas", "module.json").forEach { symlinkPath ->
                    val sourcePath = "$rootDir/modules/${module.name}/$symlinkPath"
                    if (file(sourcePath).exists()) {
                        val destinationPath = moduleRoot.toPath().resolve(symlinkPath)
                        if (Files.exists(destinationPath)) {
                            if (Files.readSymbolicLink(destinationPath) == Paths.get(sourcePath)) {
                                return@forEach
                            }
                            Files.delete(destinationPath)
                        }
                        Files.createSymbolicLink(destinationPath, Paths.get(sourcePath))
                    }
                }
                if (codePresent) {
                    val codeJarPath = moduleRoot.toPath().resolve("build/dexes/${module.name}.jar")
                    val codeJarSourcePath = layout.buildDirectory.dir("moduleAssetRoots/${module.name}/assets/modules/${module.name}/build/dexes/").get().file("${module.name}.jar").asFile.toPath()
                    if (Files.exists(codeJarPath)) {
                        if (Files.readSymbolicLink(codeJarPath) == codeJarSourcePath) {
                            return@doLast
                        }
                        Files.delete(codeJarPath)
                    }
                    codeJarPath.toFile().parentFile.mkdirs()
                    Files.createSymbolicLink(codeJarPath, codeJarSourcePath)
                }
            }
        }

        onVariants(selector().all()) { variant ->
            if (canCreateSymlinks) {
                variant.sources.assets?.addGeneratedSourceDirectory(exportModulesBySymlink, PropertyBasedGenericTask::outputDirectory)
            } else {
                variant.sources.assets?.addGeneratedSourceDirectory(exportModulesByCopy, PropertyBasedSync::outputDirectory)
            }
        }
    }
}

tasks.register<Exec>("android") {
    val path = androidSdkPath()
    val adb = "$path/platform-tools/adb"
    commandLine(adb, "shell", "am", "start", "-n", "com.miloshpetrov.sol2.android/com.miloshpetrov.sol2.android.SolAndroid")
}
