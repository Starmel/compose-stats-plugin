package com.github.starmel.composestatsplugin

import com.github.starmel.composestatsplugin.fly.OnFlyMethodStabilityInspection
import com.github.starmel.composestatsplugin.fly.unstableArgumentError
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.intellij.lang.annotations.Language

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class FunctionInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(OnFlyMethodStabilityInspection::class.java)
    }

    fun `test mutable property cause unstable class`() {
        highlight(
            """
        class UnstableClass {
            var info = ""
        }
        
        @Composable
        fun Test(unstableClass: UnstableClass) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 1)
    }

    fun `test mutable constructor parameter cause unstable class`() {
        highlight(
            """
        class UnstableClass(
            var info: String
        )
        
        @Composable
        fun Test(unstableClass: UnstableClass) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 1)
    }

    fun `test unstable constructor parameter cause unstable class`() {

        highlight(
            """
        class UnstableClass(
            var info: String
        )
        class ImplicitlyUnstableClass(
            val info: UnstableClass
        )
        @Composable
        fun Test(unstableClass: ImplicitlyUnstableClass) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 1)
    }

//    @Ignore("Due complexity of inspection do not handle this case")
//    fun `test method call cause unstable class`() {
//        highlight(
//            """
//        class UnstableClass {
//            var info = ""
//        }
//
//        fun createUnstableClass(): UnstableClass = UnstableClass()
//
//        class ImplicitlyUnstableClass {
//            var info = createUnstableClass()
//        }
//
//        @Composable
//        fun Test(unstableClass: ImplicitlyUnstableClass) { }
//        """.trimIndent()
//        )
//            .has(unstableClassError, 2)
//            .has(mutablePropertyError, 1)
//    }

    fun `test implicit property declaration with callable expression`() {
        highlight(
            """
        class Info {
            var item: String = ""
        }
        class ImplicitPropertyDeclaration {
            val info = Info()
        }
        @Composable
        fun Test(implicitPropertyDeclaration: ImplicitPropertyDeclaration) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 1)
    }

    fun `test implicit property declaration with callable expression and type`() {
        highlight(
            """
        class Info {
            var item: String = ""
        }
        class ImplicitPropertyDeclaration {
            val info: Info = Info()
        }
        @Composable
        fun Test(implicitPropertyDeclaration: ImplicitPropertyDeclaration) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 1)
    }

    fun `test class depends on unstable type marked as unstable`() {
        highlight(
            """
        class UnstableClass {
            var info = ""
        }
        class ImplicitlyUnstableClass(
            val info: UnstableClass
        )
        @Composable
        fun Test(unstableClass: ImplicitlyUnstableClass) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 1)
    }

    fun `test class depends on unstable type marked as unstable recursive`() {
        highlight(
            """
        class UnstableClass {
            var info = ""
        }
        class ImplicitlyUnstableClass1Level(
            val info: UnstableClass
        )
        class ImplicitlyUnstableClass2Level(
            val info: ImplicitlyUnstableClass1Level
        )
        @Composable
        fun Test(unstableClass: ImplicitlyUnstableClass2Level) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 1)
    }

    fun `test function argument with no params is stable`() {
        highlight(
            """
        @Composable
        fun Test(function: () -> Unit) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 0)
    }

    fun `test function argument of String is stable`() {
        highlight(
            """
        @Composable
        fun Test(function: (String) -> Unit) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 0)
    }

    fun `test function argument of List is unstable`() {
        highlight(
            """
        @Composable
        fun Test(function: (List<String>) -> Unit) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 1)
    }

    fun `test function argument of two String is stable`() {
        highlight(
            """
        @Composable
        fun Test(function: (String, String) -> Unit) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 0)
    }

    fun `test function argument of String and List is unstable`() {
        highlight(
            """
        @Composable
        fun Test(function: (String, List<String>) -> Unit) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 1)
    }

    fun `test List cause unstable class as constructor argument`() {
        highlight(
            """
        class UnstableClass(
            val info: List<String>
        )
        @Composable
        fun Test(unstableClass: UnstableClass) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 1)
    }

    fun `test List cause unstable class as property`() {
        highlight(
            """
        class UnstableClass {
            val info: List<String> = listOf()
        }
        @Composable
        fun Test(unstableClass: UnstableClass) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 1)
    }

    fun `test Set cause unstable class`() {
        highlight(
            """
        class UnstableClass(
            val info: Set<String>
        )
        @Composable
        fun Test(unstableClass: UnstableClass) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 1)
    }

    fun `test Map cause unstable class`() {
        highlight(
            """
        class UnstableClass(
            val info: Map<String, String>
        )
        @Composable
        fun Test(unstableClass: UnstableClass) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 1)
    }

    fun `test Suppress if Stable annotation is present and constructor parameter is mutable`() {
        highlight(
            """
        @Stable
        class UnstableClass(
            var info: String
        )
        @Composable
        fun Test(unstableClass: UnstableClass) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 0)
    }

    fun `test Suppress if Stable annotation is present and property is mutable`() {
        highlight(
            """
        @Stable
        class UnstableClass {
            var info: String = ""
        }
        @Composable
        fun Test(unstableClass: UnstableClass) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 0)
    }

    fun `test none if Suppressed type used as constructor parameter`() {
        highlight(
            """@Stable
        class UnstableClass(
            var info: String
        )
        class ImplicitlyUnstableClass(
            val info: String
        )
        @Composable
        fun Test(unstableClass: ImplicitlyUnstableClass) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 0)
    }

    fun `test none if Suppressed type used as property`() {
        highlight(
            """
        @Stable
        class UnstableClass {
            var info: String = ""
        }
        class ImplicitlyUnstableClass {
            val info: UnstableClass = UnstableClass()
        }
        @Composable
        fun Test(unstableClass: ImplicitlyUnstableClass) { }
        """.trimIndent()
        )
            .has(unstableArgumentError, 0)
    }

    private fun highlight(@Language("kotlin") text: String): List<HighlightInfo> {
        myFixture.configureByText(
            "Composable.kt", """
            package androidx.compose.runtime
           
            annotation class Composable
            annotation class Immutable
            annotation class Stable
            $text 
        """.trimIndent()
        )
        myFixture.configureByText(
            "TestFile.kt", """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.Stable
            import androidx.compose.runtime.Immutable
           
            $text 
        """.trimIndent()
        )
        return myFixture.doHighlighting()
    }

    // TODO: ImmutableList, ImmutableSet, ImmutableMap

    private fun List<HighlightInfo>.has(description: String, count: Int = 1) = apply {
        count { it.description == description }.let { findingCount ->
            if (findingCount != count) {
                TestCase.assertEquals(
                    "Expected to find $description $count times in ${this.joinToString("\n")}",
                    count,
                    findingCount
                )
            }
        }
    }

    override fun getTestDataPath() = "src/test/testData/rename"
}

