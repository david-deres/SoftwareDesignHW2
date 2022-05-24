val externalLibraryVersion: String? by extra
val guiceVersion: String? by extra
val junitVersion: String? by extra
val hamkrestVersion: String? by extra
val mockkVersion: String? by extra
val kotlinGuiceVersion: String? by extra

dependencies {
    implementation("il.ac.technion.cs.softwaredesign", "primitive-storage-layer", externalLibraryVersion)
    implementation("com.google.inject", "guice", guiceVersion)
    testImplementation("org.junit.jupiter", "junit-jupiter-api", junitVersion)
    testImplementation("org.junit.jupiter", "junit-jupiter-params", junitVersion)
    testImplementation("com.natpryce", "hamkrest", hamkrestVersion)
    testImplementation("dev.misfitlabs.kotlinguice4", "kotlin-guice", kotlinGuiceVersion)
}