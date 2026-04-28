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

val defaultIrohaDir: String = rootProject.projectDir
    .toPath()
    .resolve("../../iroha")
    .normalize()
    .toString()

val irohaRootProperty = providers.gradleProperty("irohaDir")
    .orElse(providers.gradleProperty("i23Dir"))
    .orElse(defaultIrohaDir)
val irohaRoot = file(irohaRootProperty.get()).toPath().normalize()
val irohaAndroidMain = irohaRoot.resolve("java/iroha_android/src/main/java")
val noritoJavaMain = irohaRoot.resolve("java/norito_java/src/main/java")
val irohaAndroidTest = irohaRoot.resolve("java/iroha_android/src/test/java")
val irohaAndroidResources = irohaRoot.resolve("java/iroha_android/src/test/resources")

fun ensureDir(path: java.nio.file.Path, description: String) {
    if (!Files.exists(path)) {
        error("Expected $description at $path. Make sure the iroha repository is checked out next to iroha-demo-android.")
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
