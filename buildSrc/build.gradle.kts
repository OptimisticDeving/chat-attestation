plugins {
  `kotlin-dsl`
}

kotlin {
  jvmToolchain(Integer.parseInt(libs.versions.java.get()))
}

repositories {
  gradlePluginPortal()
}
