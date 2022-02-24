plugins {
   kotlin("multiplatform")
}

kotlin {
   jvm {
      tasks.named<Test>("jvmTest") {
         useJUnitPlatform()
         testLogging {
            events = setOf(
               org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
               org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
            )
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            
            showExceptions = true
            showStandardStreams = true
         }
      }
   }
   sourceSets {
      val commonMain by getting {
         dependencies {
            api(kotlin("compiler"))
         }
      }
      val jvmTest by getting {
         dependencies {
            api(kotlin("test-junit"))
         }
      }
   }
}
