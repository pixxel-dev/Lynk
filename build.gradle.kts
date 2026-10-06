import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization) apply false
}

abstract class CheckLocalizationTask : DefaultTask() {
    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @TaskAction
    fun check() {
        val rootDir = rootDirectory.get().asFile
        val valuesDirs = rootDir.walkTopDown().filter { file ->
            file.isDirectory && file.name == "values" && file.parentFile?.name == "res" &&
                    !file.path.contains("build") && !file.path.contains(".gradle") &&
                    !file.path.contains(".idea") && !file.path.contains("scratch")
        }.toList()

        var hasError = false
        for (vDir in valuesDirs) {
            val enFile = File(vDir, "strings.xml")
            val ruDir = File(vDir.parentFile, "values-ru")
            val ruFile = File(ruDir, "strings.xml")

            if (!enFile.exists()) continue

            val relEn = enFile.relativeTo(rootDir).path
            val relRu = ruFile.relativeTo(rootDir).path
            println("Auditing module strings:\n  EN: $relEn\n  RU: $relRu")

            val enKeys = parseKeys(enFile)
            val ruKeys = if (ruFile.exists()) parseKeys(ruFile) else emptySet()

            val missingInRu = enKeys - ruKeys
            val missingInEn = ruKeys - enKeys

            if (missingInRu.isNotEmpty() || missingInEn.isNotEmpty()) {
                hasError = true
                if (missingInRu.isNotEmpty()) {
                    println("  [ERROR] Missing keys in RU ($relRu):")
                    missingInRu.sorted().forEach { println("    - $it") }
                }
                if (missingInEn.isNotEmpty()) {
                    println("  [ERROR] Missing keys in EN ($relEn):")
                    missingInEn.sorted().forEach { println("    - $it") }
                }
            } else {
                println("  [OK] 100% key parity achieved (${enKeys.size} keys match).\n")
            }
        }

        if (hasError) {
            throw GradleException("Localization audit FAILED: Translation parity discrepancies found!")
        } else {
            println("Localization audit PASSED successfully!")
        }
    }

    private fun parseKeys(file: File): Set<String> {
        val keys = mutableSetOf<String>()
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(file)
        doc.documentElement.normalize()
        val nList = doc.getElementsByTagName("string")
        for (i in 0 until nList.length) {
            val node = nList.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element = node as Element
                val name = element.getAttribute("name")
                if (!name.isNullOrBlank()) {
                    keys.add(name)
                }
            }
        }
        return keys
    }
}

tasks.register<CheckLocalizationTask>("checkLocalization") {
    group = "verification"
    description = "Audits key parity between res/values/strings.xml (EN) and res/values-ru/strings.xml (RU)."
    rootDirectory.set(layout.projectDirectory)
}
