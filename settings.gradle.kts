pluginManagement {
   resolutionStrategy {
      plugins {
         kotlin("multiplatform") version(extra["kotlin.version"] as String)
      }
   }
}

dependencyResolutionManagement {
   repositories {
      mavenCentral()
   }
}

with(rootProject) {
   include(":kotlin:compiler")
   project(":kotlin:compiler").apply {
      projectDir = file("modules/${path.substring(1).replace(":", "/")}")
   }
   
   include(":typescript:compiler")
   project(":typescript:compiler").apply {
      projectDir = file("modules/${path.substring(1).replace(":", "/")}")
   }
}
