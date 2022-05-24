import java.net.URI

rootProject.name = "base"

pluginManagement {
    repositories {
        gradlePluginPortal()
        jcenter()
        maven(url="https://dl.bintray.com/kotlin/dokka")
    }
}

sourceControl {
    gitRepository(URI("https://github.com/mikimn/sd-primitive-storage-layer.git")) {
        producesModule("il.ac.technion.cs.softwaredesign:primitive-storage-layer")
    }
}

include("library")
include("techwm-app")
include("techwm-test")