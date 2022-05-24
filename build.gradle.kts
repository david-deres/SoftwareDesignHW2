import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

import java.time.Duration
import java.awt.Desktop

plugins {
  kotlin("jvm") version "1.4.30"
  id("org.jetbrains.dokka") version "1.4.30"
  `maven-publish`
}

allprojects {
  repositories {
    mavenCentral()
    jcenter()
    maven {
      url = uri("https://jitpack.io")
    }
  }

  extra.apply {
    set("kotlinVersion", "1.4.30")
    set("junitVersion", "5.6.1")
    set("hamkrestVersion", "1.7.0.3")
    set("guiceVersion", "4.2.3")
    set("kotlinGuiceVersion", "1.4.1")
    set("mockkVersion", "1.9.3")
    set("externalLibraryVersion", "1.2.1")
  }
}

subprojects {
  apply(plugin = "kotlin")
  apply(plugin = "org.jetbrains.dokka")

  val junitVersion: String? by extra

  dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("com.github.Dor-B:sd-loan-service:1.0.3")

    testImplementation("org.junit.jupiter", "junit-jupiter-engine", junitVersion)
  }

  tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }

  tasks.withType<Test> {
    useJUnitPlatform()

    // Make sure tests don't take over 10 minutes
    timeout.set(Duration.ofMinutes(10))
  }
}

tasks.dokkaHtmlMultiModule.configure {
  outputDirectory.set(project.buildDir.resolve("docs"))
}

task("documentation") {
  dependsOn("dokkaHtmlMultiModule")
  doLast {
    Desktop.getDesktop().browse(project.buildDir.resolve("docs/index.html").toURI())
  }
}

task<Zip>("submission") {
  val taskname = "submission"
  val base = project.rootDir.name
  archiveBaseName.set(taskname)
  from(project.rootDir.parentFile) {
    include("$base/**")
    exclude(
      "$base/**/*.iml", "$base/*/build", "$base/**/.gradle", "$base/**/.idea", "$base/*/out",
      "$base/**/.git", "$base/**/.DS_Store", "$base/build", "$base/out"
    )
    exclude("$base/$taskname.zip")
  }
  destinationDirectory.set(project.rootDir)
}
