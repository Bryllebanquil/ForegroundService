plugins {
    // Project level plugins
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}