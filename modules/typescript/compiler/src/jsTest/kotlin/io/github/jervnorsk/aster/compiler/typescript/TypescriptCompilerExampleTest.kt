package io.github.jervnorsk.aster.compiler.typescript

import kotlinx.js.jso
import typescript.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class TypescriptCompilerExampleTest {
   
   /**
    * This example is a barebones compiler which takes a list of TypeScript files and compiles them to their corresponding JavaScript.
    * We will need to create a Program, via createProgram - this will create a default CompilerHost which uses the file system to get files.
    */
   @Test
   fun tsc_ex_tc_0() {
      fun compile(vararg fileNames: String, options: CompilerOptions) {
         val program = createProgram(fileNames, options)
         
         val emitResult = program.emit()
         
         val allDiagnostics = getPreEmitDiagnostics(program) + emitResult.diagnostics
         
         allDiagnostics.forEach { diagnostic ->
            val message = flattenDiagnosticMessageText(diagnostic.messageText, "\n")
            if (diagnostic.file != null) {
               getLineAndCharacterOfPosition(diagnostic.file.unsafeCast<SourceFileLike>(), diagnostic.start!!).let {
                  console.log("${diagnostic.file!!.fileName} (${it.line + 1},${it.character + 1}): $message")
               }
            } else {
               console.log(message)
            }
         }
         
         val exitCode = if (emitResult.emitSkipped) 1 else 0
         console.log("Process exiting with code $exitCode")
         assertEquals(0, exitCode)
      }
      
      compile("kotlin/io/github/jervnorsk/aster/compiler/typescript/test/tsc_ex_tc_0.ts", options = jso {
         noEmitOnError = true
         noImplicitAny = true
         target = ScriptTarget.ES5
         module = ModuleKind.CommonJS
      })
   }
   
   /**
    * Creating a compiler is not too many lines of code, but you may want to just get the corresponding JavaScript output given TypeScript sources. For this you can use ts.transpileModule to get a string => string transformation in two lines.
    */
   @Test
   fun tsc_ex_tc_1() {
      val source = """
         let x: string = 'hello world'
      """.trimIndent()
      
      val result = transpileModule(source, jso {
         compilerOptions = jso {
            module = ModuleKind.CommonJS
         }
      })
      
      console.log(result)
      assertTrue(result.outputText.isNotBlank())
   }
   
   /**
    * This will only work in TypeScript 3.7 and above. This example shows how you can take a list of JavaScript files and will show their generated d.ts files in the terminal.
    */
   @Test
   fun tsc_ex_tc_2() {
      fun compile(vararg fileNames: String, options: CompilerOptions) {
         // Create a Program with an in-memory emit
         val createdFiles = mutableMapOf<String, String>()
         val host = createCompilerHost(options)
         
         host.writeFile = { fileName, data, _, _, _ ->
            createdFiles[fileName] = data
         }
         
         // Prepare and emit the d.ts files
         val program = createProgram(fileNames, options, host)
         program.emit()
         
         // Loop through all the input files
         fileNames.forEach { file ->
            console.log("### JavaScript\n")
            console.log(host.readFile(file))
            
            console.log("### Type Definition\n")
            val dts = file.replace(".js", ".d.ts")
            console.log(createdFiles[dts])
         }
         
         assertTrue(createdFiles.isNotEmpty())
      }
      
      compile("kotlin/io/github/jervnorsk/aster/compiler/typescript/test/tsc_ex_tc_2.js", options = jso {
         allowJs = true
         declaration = true
         emitDeclarationOnly = true
      })
   }
   
   /**
    * This example will log out sub-sections of a TypeScript or JavaScript source file, this pattern is useful when you want the code for your app to be the source of truth. For example showcasing exports via their JSDoc comments.
    */
   @Test
   fun tsc_ex_tc_3() {
      /**
       * Prints out particular nodes from a source file
       *
       * @param file a path to a file
       * @param identifiers top level identifiers available
       */
      fun extract(file: String, vararg identifiers: String) {
         // Create a Program to represent the prohect, then pull out the source file to parse its AST.
         val program = createProgram(arrayOf(file), jso {
            allowJs = true
         })
         val sourceFile = program.getSourceFile(file)
         
         // To print the AST we'll use TypeScript's printer
         val printer = createPrinter(jso { newLine = NewLineKind.LineFeed })
         
         // To give constructive error messages, keep track of found and un-found identifiers
         val unfoundNodes = arrayListOf<Pair<String, Node>>();
         val foundNodes = arrayListOf<Pair<String, Node>>()
         
         // Loop through the root AST nodes of the file
         forEachChild(sourceFile.unsafeCast<Node>(), { node ->
            var name = ""
            
            // This is an incomplete set of AST nodes wich could have a top level identifier
            // it's left to you to expand this list, which you can do by using
            // https://ts-ast-viewer.com to see the AST of a file then use the same patterns
            // as below
            when {
               isFunctionDeclaration(node) -> {
                  name = node.name!!.text
                  // Hide the method body when printing
                  js("node.body = undefined")
               }
               isVariableStatement(node) -> {
                  name = js("node.declarationList.declarations[0].name.getText(sourceFile)").unsafeCast<String>()
               }
               isInterfaceDeclaration(node) -> {
                  name = node.name.text
               }
               isClassDeclaration(node) -> {
                  name = node.name!!.text
               }
               else -> assertFails("Not implemented!") {
                  console.log(node)
               }
            }
            
            val container = if (identifiers.contains(name)) foundNodes else unfoundNodes
            container.add(name to node)
         })
         
         // Either print the found nodes, or offer a list of what identifiers were found
         if (foundNodes.isEmpty()) {
            assertFails("Could not find any of ${identifiers.joinToString(", ")} in $file, found: ${
               unfoundNodes.joinToString(", ") { it.first }
            }.") {}
         } else {
            foundNodes.map { (name, node) ->
               console.log("### $name\n")
               console.log("${printer.printNode(EmitHint.Unspecified, node, sourceFile!!)}\n")
            }
         }
      }
      
      try {
         extract("kotlin/io/github/jervnorsk/aster/compiler/typescript/test/tsc_ex_tc_3.js", "B")
      } catch (e: Throwable) {
         e.printStackTrace()
      }
   }
   
   /**
    * The Node interface is the root interface for the TypeScript AST. Generally, we use the forEachChild function in a recursive manner to iterate through the tree. This subsumes the visitor pattern and often gives more flexibility.
    *
    * As an example of how one could traverse a file's AST, consider a minimal linter that does the following:
    *
    * - Checks that all looping construct bodies are enclosed by curly braces.
    * - Checks that all if/else bodies are enclosed by curly braces.
    * - The "stricter" equality operators (===/!==) are used instead of the "loose" ones (==/!=).
    */
   @Test
   fun tsc_ex_tc_4() {
      fun delint(sourceFile: SourceFile) {
         fun report(node: Node, message: String) {
            sourceFile.getLineAndCharacterOfPosition(node.getFullStart()).let {
               assertFails("${sourceFile.fileName} (${it.line + 1},${it.character + 1}): $message") {}
            }
         }
         
         fun delintNode(node: Node) {
            when (node.kind) {
               SyntaxKind.ForStatement, SyntaxKind.ForInStatement, SyntaxKind.WhileStatement, SyntaxKind.DoStatement -> {
                  node.unsafeCast<IterationStatement>().let {
                     if (it.statement.kind !== SyntaxKind.Block) {
                        report(node, "A looping statement's contents should be wrapped in a block body.")
                     }
                  }
               }
               SyntaxKind.IfStatement                                                                                -> {
                  node.unsafeCast<IfStatement>().let {
                     if (it.thenStatement.kind !== SyntaxKind.Block) {
                        report(it.thenStatement, "An if statement's contents should be wrapped in a block body.")
                     }
                     if (it.elseStatement != null && it.elseStatement!!.kind != SyntaxKind.Block && it.elseStatement!!.kind !== SyntaxKind.IfStatement) {
                        report(it.elseStatement!!, "Else statement's contents should be wrapped in a block body.")
                     }
                  }
               }
               SyntaxKind.BinaryExpression                                                                           -> {
                  node.unsafeCast<BinaryExpression>().let {
                     if (it.operatorToken.kind === SyntaxKind.EqualsEqualsToken || it.operatorToken.kind === SyntaxKind.ExclamationEqualsToken) {
                        report(node, "Use '===' and '!=='.")
                     }
                  }
               }
               else                                                                                                  -> {}
            }
            
            forEachChild(node, { delintNode(it) })
         }
         
         forEachChild(sourceFile, { delintNode(it) })
      }
      
      val compiler = createCompilerHost(jso {
      
      })
      
      arrayOf(
         "kotlin/io/github/jervnorsk/aster/compiler/typescript/test/tsc_ex_tc_4.ts"
      ).forEach { fileName ->
         // Parse a file
         val sourceFile = compiler.getSourceFile(
            fileName, ScriptTarget.ES5, {}, false
         )
         
         // delint it
         delint(sourceFile!!)
      }
   }
   
   /**
    * TypeScript has factory functions and a printer API that you can use in conjunction.
    *
    * - The factory allows you to generate new tree nodes in TypeScript's AST format.
    * - The printer can take an existing tree (either one produced by createSourceFile or by factory functions), and produce an output string.
    */
   @Test
   fun tsc_ex_tc_5() {
      fun makeNode(): Node {
         return factory.createVariableStatement(
            null,
            factory.createVariableDeclarationList(
               arrayOf(
                  factory.createVariableDeclaration("A", initializer = factory.createStringLiteral("Hello World")),
               ),
               NodeFlags.Let
            )
         )
      }
      
      val resultFile = createSourceFile("someFileName.ts", "", ScriptTarget.Latest, false, ScriptKind.TS)
      val printer = createPrinter(jso {
         newLine = NewLineKind.LineFeed
         removeComments = true
      })
      
      val result = printer.printNode(EmitHint.Unspecified, makeNode(), resultFile)
      console.log(result)
      
      assertEquals("let A = \"Hello World\";", result)
   }
}
