import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.jvm.isAccessible

plugins {
  java

  alias(libs.plugins.fabric.loom)
}

val shade: Configuration by configurations.creating

loom {
  runs {
    removeIf {
      it.name == "server"
    }
  }
}

java {
  toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
}

configurations {
  compileClasspath {
    extendsFrom(shade)
  }

  runtimeClasspath {
    extendsFrom(shade)
  }
}

group = "dev.optimistic"
version = "1.0.0-SNAPSHOT"

repositories {
  maven("https://maven.shedaniel.me/")
  maven("https://maven.terraformersmc.com/releases/")
  maven("https://code.chipmunk.land/api/packages/kaboomstandardsorganization/maven") {
    content {
      includeGroupAndSubgroups("land.chipmunk.code")
    }
  }
}

val expandedFabricVersion = "${libs.versions.fabric.api.get()}+${libs.versions.minecraft.get()}"

dependencies {
  minecraft(libs.minecraft)
  mappings(loom.officialMojangMappings())

  modImplementation(libs.fabric.loader)
  modImplementation(fabricApi.module("fabric-networking-api-v1", expandedFabricVersion))
  modImplementation(include(libs.adventure.platform.fabric.get())!!)
  modImplementation(libs.messaginglib)

  modApi(libs.clothconfig)
  modApi(libs.modmenu)

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
    inputs.property("name", project.name)

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
