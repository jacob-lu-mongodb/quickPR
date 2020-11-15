package com.mongodb.quickpr.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.panel
import com.mongodb.quickpr.models.SettingsModel
import javax.swing.JComponent

class SettingsDialogWrapper(private val model: SettingsModel, private val validateSettings: (SettingsModel) -> String?) :
    DialogWrapper(true) {
    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label("Please follow the steps")
                link("here") {
                    BrowserUtil.browse("https://wiki.corp.mongodb.com/display/MMS/Setup+the+MMS+Python+Environment#SetuptheMMSPythonEnvironment-GetOAuthAccessforJIRAScripts")
                }
                label("to set up your JIRA config file")
            }
            row {
                label("JIRA Config File")
                eagerBoundTextField(model::jiraConfigPath)
            }
            row {
                label("GitHub Token")
                eagerBoundPasswordField(model::githubToken)
            }
        }
    }

    override fun doValidate(): ValidationInfo? {
        return validateSettings(model)?.let { ValidationInfo(it) }
    }

    init {
        init()
        startTrackingValidation()
        title = "QuickPR Settings"
    }
}
