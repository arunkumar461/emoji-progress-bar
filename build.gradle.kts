    plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.16.1"
}

group = "com.madesha.emoji"
version = "0.1.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.3")
    type.set("IC")
    plugins.set(listOf("java"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("242.*")
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    compileJava {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    runIde {
        autoReloadPlugins.set(true)
    }
}

