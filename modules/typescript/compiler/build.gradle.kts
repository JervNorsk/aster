plugins {
   kotlin("multiplatform")
}

kotlin {
   js {
      nodejs {
         testTask {
            useMocha()
            testLogging {
               showStandardStreams = true
            }
         }
      }
   }
   sourceSets {
      val commonMain by getting {
         dependencies {
            api("org.jetbrains.kotlin-wrappers:kotlin-extensions:latest.release")
            api("org.jetbrains.kotlin-wrappers:kotlin-typescript:latest.release")
         }
      }
      val jsTest by getting {
         resources.srcDir("src/jsTest/typescript")
         dependencies {
            implementation(kotlin("test-js"))
         }
      }
   }
}
