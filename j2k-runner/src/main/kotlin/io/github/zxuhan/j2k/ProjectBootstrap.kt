package io.github.zxuhan.j2k

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories

/**
 * Bootstraps a real (not light) Project + Module rooted at the user's --input directory,
 * so NJ2K's resolver has something to chew on. We deliberately avoid mutating --input:
 * the .idea/.iml metadata lives in a sibling scratch dir under a temp folder.
 */
internal object ProjectBootstrap {

    data class Bootstrapped(val project: Project, val module: Module, val inputRoot: VirtualFile)

    fun open(inputDir: Path): Bootstrapped {
        val absoluteInput = inputDir.absolute().normalize()
        val scratchDir = Files.createTempDirectory("j2k-runner-").also {
            Runtime.getRuntime().addShutdownHook(Thread { runCatching { it.toFile().deleteRecursively() } })
        }
        val projectDir = scratchDir.resolve("project").createDirectories()

        // Register the JDK in the global table BEFORE creating the project, so the project's
        // initial workspace-model snapshot already contains the SdkEntity. Adding it after
        // project open works for ProjectRootManager.setProjectSdk but the K2 analysis API
        // resolves SDKs via the snapshot and fails with "Could not resolve SdkId" otherwise.
        val sdk = WriteAction.computeAndWait<Sdk, RuntimeException> {
            val table = ProjectJdkTable.getInstance()
            val internal = JavaAwareProjectJdkTableImpl.getInstanceEx().internalJdk
            if (table.findJdk(internal.name) == null) table.addJdk(internal)
            internal
        }

        // Use openProject (not newProject) so StartupActivities + the indexing scanner fire.
        // With newProject the project never schedules indexing tasks, so DumbService.waitForSmartMode
        // hangs forever (the project starts dumb but nothing ever asks it to become smart).
        val task = OpenProjectTask().copy(
            isNewProject = true,
            runConfigurators = true,
            forceOpenInNewFrame = false,
            showWelcomeScreen = false,
        )
        val project = ProjectManagerEx.getInstanceEx().openProject(projectDir, task)
            ?: error("ProjectManagerEx.openProject returned null for $projectDir")

        val inputVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(absoluteInput)
            ?: error("input dir not visible to VFS: $absoluteInput")

        val module = WriteAction.computeAndWait<Module, RuntimeException> {
            ProjectRootManager.getInstance(project).projectSdk = sdk

            val moduleManager = ModuleManager.getInstance(project)
            val moduleModel = moduleManager.getModifiableModel()
            val moduleFile = projectDir.resolve("j2k-input.iml")
            val newModule = moduleModel.newModule(moduleFile, JavaModuleType.getModuleType().id)
            moduleModel.commit()

            val rootModel = ModuleRootManager.getInstance(newModule).modifiableModel
            val contentEntry = rootModel.addContentEntry(inputVf)
            contentEntry.addSourceFolder(inputVf, false)
            rootModel.sdk = sdk
            rootModel.commit()

            newModule
        }

        VfsUtil.markDirtyAndRefresh(false, true, true, inputVf)

        // Conversion uses PSI / resolver, which throws IndexNotReadyException in dumb mode.
        // Indexing kicks off automatically once content roots + SDK are committed.
        DumbService.getInstance(project).waitForSmartMode()

        return Bootstrapped(project, module, inputVf)
    }
}
