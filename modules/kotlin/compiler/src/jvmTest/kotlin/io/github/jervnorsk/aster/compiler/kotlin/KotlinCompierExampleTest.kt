package io.github.jervnorsk.aster.compiler.kotlin

import kotlin.io.path.absolute
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test

class KotlinCompierExampleTest {
   
   @Test
   fun test() {
      val tmpDir = createTempDirectory()
   
      val sourceFile = tmpDir.resolve("source.kt")
      sourceFile.writeText(
         """
         val A: String = "Hello World"
      """.trimIndent()
      )
   
      println(sourceFile.absolute())
   }
}
