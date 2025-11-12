import java.nio.file.Files

plugins {
    `java-library`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

val i23RootProperty = providers.gradleProperty("i23Dir").orElse("/Users/takemiyamakoto/dev/i23")
val i23Root = file(i23RootProperty.get()).toPath().normalize()
val irohaAndroidMain = i23Root.resolve("java/iroha_android/src/main/java")
val noritoJavaMain = i23Root.resolve("java/norito_java/src/main/java")
val irohaAndroidTest = i23Root.resolve("java/iroha_android/src/test/java")
val irohaAndroidResources = i23Root.resolve("java/iroha_android/src/test/resources")

fun ensureDir(path: java.nio.file.Path, description: String) {
    if (!Files.exists(path)) {
        error("Expected $description at $path. Make sure the i23 repository is checked out next to iroha-demo-android.")
    }
}

ensureDir(irohaAndroidMain, "Iroha Android main sources")
ensureDir(noritoJavaMain, "Norito Java sources")

sourceSets {
    val main by getting {
        java.srcDir(irohaAndroidMain.toFile())
        java.srcDir(noritoJavaMain.toFile())
    }
    val test by getting {
        if (Files.exists(irohaAndroidTest)) {
            java.srcDir(irohaAndroidTest.toFile())
        }
        if (Files.exists(irohaAndroidResources)) {
            resources.srcDir(irohaAndroidResources.toFile())
        }
    }
}

dependencies {
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")

    testImplementation("junit:junit:4.13.2")
}
