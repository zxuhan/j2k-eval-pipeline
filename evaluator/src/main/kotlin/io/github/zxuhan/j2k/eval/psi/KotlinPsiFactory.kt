package io.github.zxuhan.j2k.eval.psi

import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.nio.file.Path
import kotlin.io.path.readText

class KotlinPsiFactory : AutoCloseable {

    private val disposable: Disposable = Disposer.newDisposable("j2k-eval")

    private val environment: KotlinCoreEnvironment = run {
        val configuration = CompilerConfiguration().apply {
            put(CommonConfigurationKeys.MODULE_NAME, "j2k-eval")
            put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        }
        KotlinCoreEnvironment.createForProduction(
            disposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES,
        )
    }

    private val factory: KtPsiFactory = KtPsiFactory(environment.project, markGenerated = false)

    fun parse(path: Path): KtFile = parse(path.fileName.toString(), path.readText())

    fun parse(fileName: String, source: String): KtFile =
        factory.createFile(fileName, source)

    override fun close() {
        Disposer.dispose(disposable)
    }
}
