package com.mongodb.quickpr.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.panel
import com.mongodb.quickpr.models.SettingsModel
import javax.swing.JComponent

class SettingsDialogWrapper(
    private val model: SettingsModel,
    private val validateSettings: (SettingsModel) -> String?
) :
    DialogWrapper(true) {

    private var lastValidatedModel: SettingsModel? = null
    private var lastValidationResult: String? = null

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label("Please follow the steps")
                link("here") {
                    BrowserUtil.browse(
                        "https://wiki.corp.mongodb.com/display/MMS/" +
                            "Setup+the+MMS+Python+Environment#" +
                            "SetuptheMMSPythonEnvironment-GetOAuthAccessforJIRAScripts"
                    )
                }
                label("to set up your JIRA config file")
            }
            row {
                label("JIRA Config File")
                eagerBoundTextField(model::jiraConfigPath)
            }
            row {
                label("Evergreen Config File")
                eagerBoundTextField(model::evgConfigPath)
            }
            row {
                label("GitHub Token")
                eagerBoundPasswordField(model::githubToken)
            }
        }
    }

    override fun doValidate(): ValidationInfo? {
        var validationResult: String?

        if (lastValidatedModel != model) {
            // cache the validation result to prevent excessive calls to API's
            validationResult = validateSettings(model)
            lastValidatedModel = model.copy()
            lastValidationResult = validationResult
        } else {
            validationResult = lastValidationResult
        }
        return validationResult?.let { ValidationInfo(it) }
    }

    init {
        init()
        startTrackingValidation()
        title = "QuickPR Settings"
    }
}
