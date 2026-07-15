import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.jvm.isAccessible

plugins {
  common

  alias(libs.plugins.fabric.loom)
  alias(libs.plugins.minotaur)
}

val shade: Configuration by configurations.creating

loom {
  runs {
    removeIf {
      it.name == "server"
    }
  }

  runs.configureEach { ideConfigGenerated(true) }
}

base.archivesName = rootProject.name

configurations {
  compileClasspath {
    extendsFrom(shade)
  }

  runtimeClasspath {
    extendsFrom(shade)
  }
}

repositories {
  maven("https://central.sonatype.com/repository/maven-snapshots") {
    mavenContent {
      snapshotsOnly()

      includeGroupAndSubgroups("net.kyori")
    }
  }
  maven("https://central.sonatype.com/repository/maven-releases") {
    mavenContent {
      includeGroupAndSubgroups("net.kyori")
    }
  }

  maven("https://maven.shedaniel.me/")
  maven("https://maven.terraformersmc.com/releases/")
}

dependencies {
  minecraft(libs.minecraft)

  implementation(libs.fabric.loader)
  implementation(include(libs.adventure.platform.fabric.get())!!)
  implementation(libs.messaginglib.fabric)

  api(libs.clothconfig)
  api(libs.modmenu)

  implementation(include(libs.expiringmap.get())!!)

  shade(libs.jtoml.configurate)
  shade(libs.bcprov)
  shade(libs.zstd.jni)
}

fun createConformantName(name: String): String {
  val out = StringBuilder()
  name.removePrefix("get").forEach { ch ->
    if (ch.isUpperCase() && !out.isEmpty()) out.append("_")
    out.append(ch.lowercase())
  }
  return out.toString()
}

fun getProvidersWithNames(ins: Any, cls: KClass<*>): Map<String, Provider<String>> {
  val map = mutableMapOf<String, Provider<String>>()

  cls.declaredMembers
    .filterNot { it.name.contains("vacc") }
    .forEach {
      it.isAccessible = true
      val value = it.call(ins)
      val conformantName = createConformantName(it.name)

      if (value is Provider<*>) {
        @Suppress("UNCHECKED_CAST")
        map[conformantName] = value as Provider<String>
      } else if (value is AbstractExternalDependencyFactory.VersionFactory) {
        map.putAll(
          getProvidersWithNames(value, value::class).mapKeys { subName ->
            "${conformantName}_${subName.key}"
          }
        )
      }
    }

  return map
}

fun createVersionMap(): Map<String, String> =
  getProvidersWithNames(libs.versions, libs.versions::class).mapValues { it.value.get() }

tasks {
  processResources {
    dependsOn(shade)

    from("./LICENSE")

    inputs.properties(createVersionMap())
    inputs.property("version", project.version)
    inputs.property("name", rootProject.name)

    filesMatching("**/*.json") {
      expand(inputs.properties)
    }

    shade.files.forEach {
      from(zipTree(it)) {
        exclude("META-INF/MANIFEST.MF")
        exclude("META-INF/versions/*/module-info.class")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.SF")
        exclude("META-INF/*.RSA")
        exclude("**/LICENSE*")
        exclude("**/NOTICE*")

        duplicatesStrategy = DuplicatesStrategy.FAIL
      }
    }
  }
}


modrinth {
  token.set(System.getenv("MODRINTH_TOKEN"))
  projectId.set("chat-attestation")
  uploadFile.set(tasks.jar)
  if (project.version.toString().endsWith("-SNAPSHOT")) versionType.set("beta") else versionType.set("release")
  additionalFiles.addAll(tasks.sourcesJar)
  gameVersions.addAll(libs.versions.minecraft.get())
  loaders.addAll("fabric")

  dependencies {
    required.project("messaging-lib")
  }
}
