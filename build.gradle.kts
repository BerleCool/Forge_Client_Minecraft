plugins {
    java
    idea
    id("gg.essential.loom") version "0.10.0.5"
    id("dev.architectury.architectury-pack200") version "0.1.3"
}

group = "dev.forgeclient"
version = "0.2.0-alpha"
base { archivesName.set("forge-client") }

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net/")
}

loom {
    forge { pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter()) }
    runConfigs {
        "client" {
            if (System.getProperty("os.name").lowercase().contains("mac")) vmArgs.remove("-XstartOnFirstThread")
            vmArgs.add("-Dlog4j2.formatMsgNoLookups=true")
        }
        remove(getByName("server"))
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
}

// Legacy Forge's development discovery expects resources alongside classes.
sourceSets.main { output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory }) }

tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("mcmod.info") { expand("version" to project.version) }
}

tasks.jar {
    archiveClassifier.set("dev")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    from("licenses") { into("META-INF/licenses") }
    from("THIRD_PARTY_NOTICES.md") { into("META-INF") }
    manifest.attributes["Implementation-Title"] = "Forge Client"
    manifest.attributes["Implementation-Version"] = project.version
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    input.set(tasks.jar.get().archiveFile)
}
tasks.assemble { dependsOn(remapJar) }

// Compile the real adapter, then test the core/UI on the actual target Java runtime.
val coreTests by tasks.registering(JavaExec::class) {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("dev.forgeclient.tests.AllTests")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
    enableAssertions = true
    args(layout.buildDirectory.file("reports/core-tests.json").get().asFile.absolutePath)
}
tasks.check { dependsOn(coreTests) }
