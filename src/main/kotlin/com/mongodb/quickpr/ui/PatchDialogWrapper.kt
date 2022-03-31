package com.mongodb.quickpr.ui

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.toBinding
import com.intellij.util.containers.toArray
import com.mongodb.quickpr.actions.SettingsAction
import com.mongodb.quickpr.config.SettingsManager
import com.mongodb.quickpr.core.Ok
import com.mongodb.quickpr.core.SafeError
import com.mongodb.quickpr.core.runResultTry
import com.mongodb.quickpr.git.GitUtils
import com.mongodb.quickpr.models.PatchModel
import com.mongodb.quickpr.models.PatchPreset
import com.mongodb.quickpr.models.PatchVariantTasks
import com.mongodb.quickpr.models.UserPatchConfig
import com.mongodb.quickpr.services.AppService
import com.mongodb.quickpr.services.EvergreenClient
import com.mongodb.quickpr.services.getAppState
import org.kohsuke.github.GHPullRequest
import org.yaml.snakeyaml.Yaml
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import java.io.FileInputStream
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

// TODO: detect push and remind user
class PatchDialogWrapper(
    private val pr: GHPullRequest,
    private val lastPatchInfo: AppService.PatchInfo?,
    private val projectPath: String,
    private val branchName: String,
) : DialogWrapper(true) {

    private val model = PatchModel()
    private var diff: String = ""
    private var mergeBase: String = ""

    // https://jetbrains.design/intellij/controls/combo_box/
    private var presetCombo: ComboBox<String>? = null
    private var aliasField: JBTextField? = null
    private var taskArea: JBTextArea? = null

    private val TEXT_AREA_WIDTH = 800
    private val TEXT_AREA_HEIGHT = 250

    private val LAST_USED_PRESET = "*Last Used"
    private val UNSAVED_PRESET = "*Unsaved"

    val docListener = object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) {
            update(e)
        }

        override fun removeUpdate(e: DocumentEvent?) {
            update(e)
        }

        override fun changedUpdate(e: DocumentEvent?) {
            update(e)
        }

        private fun update(e: DocumentEvent?) {
//            model.preset = ""
//            presetCombo!!.item = ""
// //            e!!.document.getText(0, e.document.length)
        }
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                cell {
                    label("Current PR: ")
                    link(pr.number.toString()) {
                        BrowserUtil.browse(
                            pr.htmlUrl
                        )
                    }
                }
            }
            row {
                label("")
            }
            row {
                cell {
                    label("Aliases (comma separated list)").constraints(
                        CCFlags.growX,
                        CCFlags.pushX
                    )
                }

                cell {
                    label("Select patch preset:")
                    presetCombo =
                        comboBox(DefaultComboBoxModel(arrayOf()), model::preset).component
                    presetCombo!!.addItemListener {
                        if (it.stateChange == ItemEvent.SELECTED) {
                            val selectedItem = presetCombo!!.selectedItem

                            onPresetSelectionChanged(selectedItem)
                        }
                    }
                    updatePresetComboItems(if (lastPatchInfo == null) UNSAVED_PRESET else LAST_USED_PRESET)
                }
            }
            row {
                aliasField = eagerBoundTextField(model::aliases).component
                aliasField!!.document.addDocumentListener(docListener)
            }
            row {
                label("")
            }
            row {
                label(
                    "Variants and Tasks (JSON with format [{id: <variant>, tasks: [<task>...]}...]." +
                            " Use a single \"*\" entry to select all tasks within a variant)"
                )
            }
            row {
                val textArea = JBTextArea()
                textArea.text = model.tasks
                addEagerBoundTextBinding(textArea, (model::tasks).toBinding(), this::onGlobalReset)
                val scrollPane =
                    scrollPane(textArea).constraints(CCFlags.pushY, CCFlags.growY).component
                scrollPane.minimumSize = Dimension(TEXT_AREA_WIDTH, TEXT_AREA_HEIGHT)
                scrollPane.size = Dimension(TEXT_AREA_WIDTH, TEXT_AREA_HEIGHT)
                scrollPane.maximumSize = Dimension(TEXT_AREA_WIDTH, TEXT_AREA_HEIGHT)

                textArea.lineWrap = true
                textArea.wrapStyleWord = true

                textArea.document.addDocumentListener(docListener)
                taskArea = textArea
            }
            row {
                label("")
            }
            row {
                label(
                    "Diff Summary"
                )
            }
            row {
                val textArea = JBTextArea()
                textArea.text = model.stats
                addEagerBoundTextBinding(textArea, (model::stats).toBinding(), this::onGlobalReset)
                val scrollPane =
                    scrollPane(textArea).constraints(CCFlags.pushY, CCFlags.growY).component
                scrollPane.minimumSize = Dimension(TEXT_AREA_WIDTH, TEXT_AREA_HEIGHT)
                scrollPane.size = Dimension(TEXT_AREA_WIDTH, TEXT_AREA_HEIGHT)
                scrollPane.maximumSize = Dimension(TEXT_AREA_WIDTH, TEXT_AREA_HEIGHT)

                textArea.lineWrap = true
                textArea.wrapStyleWord = true
                textArea.isEnabled = false
            }
        }
    }

    private fun onPresetSelectionChanged(
        selectedItem: Any?,
    ) {
        if (selectedItem == model.preset) return

        if (selectedItem == UNSAVED_PRESET) {
            model.preset = UNSAVED_PRESET
            return
        }

        if (selectedItem == LAST_USED_PRESET && lastPatchInfo != null) {
            updateModelAndUI(lastPatchInfo.patchConfig, LAST_USED_PRESET)
        } else {
            val presets =
                getAppState().patchPresets.getPresets(pr.repository.name)
            val foundPreset = presets.find { it.name == selectedItem }

            if (foundPreset == null) {
                updateModelAndUI(UserPatchConfig(), UNSAVED_PRESET)
            } else {
                updateModelAndUI(foundPreset.patchConfig, foundPreset.name)
            }
        }
    }

    fun getPatchConfig(): UserPatchConfig {
        val aliases = model.aliases.split(",").map { it.trim() }.toSet()

        val type = object : TypeToken<MutableSet<PatchVariantTasks>>() {}.getType()
        val gson = Gson()
        val tasks: MutableSet<PatchVariantTasks> =
            if (model.tasks.isNullOrBlank()) mutableSetOf() else gson.fromJson(model.tasks, type)

        return UserPatchConfig(aliases, tasks)
    }

    fun getPatchPresetOptions(): List<String> {
        val list = getAppState().patchPresets.getPresets(pr.repository.name).map { it.name }
            .toMutableList()
        if (lastPatchInfo != null) {
            list += LAST_USED_PRESET
        }
        list += UNSAVED_PRESET
        return list
    }

    override fun createLeftSideActions(): Array<Action> {
        return super.createLeftSideActions() + createSettingsAction() + createSavePresetAction() + createDeletePresetsAction()
    }

    private fun createSettingsAction(): Action {
        return object : AbstractAction("Settings") {
            override fun actionPerformed(e: ActionEvent?) {
                SettingsAction.invokeAction()
            }
        }
    }

    private fun updatePresetComboItems(selectedItem: String? = null) {
        val options = getPatchPresetOptions()
        presetCombo!!.model = DefaultComboBoxModel(options.toArray(arrayOf()))
        if (selectedItem != null) {
            presetCombo!!.item = selectedItem
        }
        if (!options.contains(presetCombo!!.item)) {
            presetCombo!!.item = UNSAVED_PRESET
        }
    }

    private fun createSavePresetAction(): Action {
        return object : AbstractAction("Save Patch Preset") {
            override fun actionPerformed(e: ActionEvent?) {
                val newPresetName =
                    if (model.preset == LAST_USED_PRESET || model.preset == UNSAVED_PRESET) "preset" else model.preset
                val preset = PatchPreset(newPresetName, getPatchConfig())

                if (SavePatchPresetDialogWrapper(
                        preset
                    ).showAndGet()
                ) {
                    // user pressed OK
                    getAppState().patchPresets.savePreset(pr.repository.name, preset)
                    updatePresetComboItems(preset.name)
                }
            }
        }
    }

    private fun createDeletePresetsAction(): Action {
        return object : AbstractAction("Delete Patch Preset") {
            override fun actionPerformed(e: ActionEvent?) {
                if (model.preset != LAST_USED_PRESET && model.preset != UNSAVED_PRESET) {
                    getAppState().patchPresets.deletePreset(pr.repository.name, model.preset)
                    updatePresetComboItems(UNSAVED_PRESET)
                }
            }

//            override fun isEnabled(): Boolean {
//                return model.preset != LAST_USED_PRESET && model.preset != ""
//            }override fun isEnabled(): Boolean {
//                return model.preset != LAST_USED_PRESET && model.preset != ""
//            }
        }
    }

    private fun getEvgProject(repo: String): String {
        return when (repo) {
            "mms-automation" -> "cloud-automation-master"
            "mms" -> "mms"
            else -> ""
        }
        // TODO: handle others
    }

    fun doAction(): Boolean {
        val patchConfig = getPatchConfig()

        val path = SettingsManager.loadSettings().evgConfigPath

//        val filePath = Path.of(path)
//        if (Files.notExists(filePath) || !Files.isRegularFile(filePath)) {
//            return Err(JiraConfigError.FILE_NOT_EXIST)
//        }

        FileInputStream(path).use {
            val obj: Map<String, String> = Yaml().load(it)

            val apiUser = obj["user"]!!
            val apiKey = obj["api_key"]!!
            val evergreenClient = EvergreenClient(apiUser, apiKey)

            val proj = getEvgProject(pr.repository.name)

            val patchId = evergreenClient.createPatch(proj, mergeBase, diff)

            val latestVersionId =
                evergreenClient.getLatestVersionId(proj)

            for (alias in patchConfig.aliases) {
                val aliasTasks = evergreenClient.getAliasTasks(proj, alias, latestVersionId)

                evergreenClient.configurePatch(patchId, aliasTasks)
            }

            if (patchConfig.tasks.isNotEmpty()) {
                evergreenClient.configurePatch(patchId, patchConfig.tasks)
            }

            getAppState().prStates.getPRState(
                pr.repository.name,
                branchName
            )!!.lastPatchInfo = AppService.PatchInfo(patchId, patchConfig)

            pr.comment(GitUtils.getCommitHash(projectPath) + ": " + getPatchUrl(patchId))
//            BrowserUtil.browse(getPatchUrl(patchId))

            BrowserUtil.browse(pr.htmlUrl)
        }

//        val prResult = GitUtils.createP(lastPatchInfo, prModel, gitBranch).mapError {
//            when (it) {
//                GitError.PR_ALREADY_EXISTS -> "There's already an open pull request"
//                else -> "There was an error creating PR"
//            }
//        }
//
//        return when (prResult) {
//            is Ok -> {

        return true
//            }
//            is Err -> {
//                Messages.showErrorDialog(prResult.error, "Error")
//                false
//            }
//        }
    }

    private fun getPatchUrl(patchId: String): String {
        return "https://spruce.mongodb.com/version/$patchId"
    }

    override fun doOKAction() {
        if (doAction()) {
            super.doOKAction()
        }
    }

    private fun updateModel(patchConfig: UserPatchConfig, preset: String) {
        model.aliases = patchConfig.aliases.joinToString(",")

        val gson = GsonBuilder().setPrettyPrinting().create()
        model.tasks = gson.toJson(patchConfig.tasks)

        model.preset = preset
    }

    private fun updateModelAndUI(patchConfig: UserPatchConfig, preset: String) {
        updateModel(patchConfig, preset)
        presetCombo!!.item = preset
        aliasField!!.text = model.aliases
        taskArea!!.text = model.tasks
    }

    init {
        runResultTry<Any, SafeError> {
            mergeBase =
                GitUtils.getMergeBase(projectPath, "master@{upstream}", "HEAD").abortOnError()
            // TODO: get branch1 from /projects ?

            model.stats = GitUtils.getDiff(projectPath, mergeBase, "--stat").abortOnError()
            diff = GitUtils.getDiff(projectPath, mergeBase).abortOnError()

            if (lastPatchInfo != null) {
                updateModel(lastPatchInfo.patchConfig, LAST_USED_PRESET)
            } else {
                model.preset = UNSAVED_PRESET
            }
            // TODO: use repo's last used config

            Ok("")
        }

        init()
        title = "Create Patch"
    }
}
