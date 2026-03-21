plugins {
  `java-library`
  `maven-publish`
}

group = "dev.optimistic.chatattestation"
version = "1.0.0-SNAPSHOT"

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

java {
  toolchain.languageVersion = JavaLanguageVersion.of(libs.findVersion("java").get().toString())
}

repositories {
  maven("https://code.optmstc.dev/api/packages/kso/maven") {
    content {
      includeGroupAndSubgroups("land.chipmunk.code")
    }
  }
  maven("https://code.chipmunk.land/api/packages/kaboomstandardsorganization/maven") {
    content {
      includeGroupAndSubgroups("land.chipmunk.code")
    }
  }
  mavenCentral()
}
