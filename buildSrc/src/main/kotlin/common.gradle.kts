plugins {
  `java-library`
  `maven-publish`
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

group = "dev.optimistic.chatattestation"
version = libs.findVersion("self").get()

java {
  toolchain.languageVersion = JavaLanguageVersion.of(libs.findVersion("java").get().toString())
  withSourcesJar()
}

repositories {
  for (forge in arrayOf("code.optmstc.dev", "code.hcesaropz.dev", "code.chipmunk.land", "codeberg.org")) {
    maven("https://$forge/api/packages/${if (forge == "codeberg.org" || forge == "code.chipmunk.land") "kaboomstandardsorganization" else "kso"}/maven") {
      mavenContent {
        includeGroupAndSubgroups("land.chipmunk.code.kaboomstandardsorganization")
      }
    }
  }

  mavenCentral()
}
