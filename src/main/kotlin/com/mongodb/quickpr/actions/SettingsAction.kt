package com.mongodb.quickpr.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.mongodb.quickpr.config.SettingsManager
import com.mongodb.quickpr.config.SettingsManager.loadSettings
import com.mongodb.quickpr.config.SettingsManager.saveSettings
import com.mongodb.quickpr.ui.SettingsDialogWrapper
import javax.swing.Icon

class SettingsAction : AnAction {
    constructor() : super()

    constructor(text: String?, description: String?, icon: Icon?) : super(
        text,
        description,
        icon
    )

    override fun actionPerformed(event: AnActionEvent) {
        val savedSettings = loadSettings()

        val newSettings = savedSettings.copy()
        if (newSettings.jiraConfigPath.isBlank()) {
            newSettings.jiraConfigPath = SettingsManager.getDefaultJiraConfigFilePath()
        }
        if (newSettings.evgConfigPath.isBlank()) {
            newSettings.evgConfigPath = SettingsManager.getDefaultEvgConfigFilePath()
        }

        if (SettingsDialogWrapper(newSettings, SettingsManager::validateSettings).showAndGet()) {
            // user pressed OK
            if (savedSettings != newSettings) {
                saveSettings(newSettings)
            }
        }
    }

    companion object {
        fun invokeAction() {
            SettingsAction(
                "QuickPR Settings",
                null,
                null
            ).actionPerformed(
                AnActionEvent.createFromDataContext(
                    "code",
                    null,
                    SimpleDataContext.getSimpleContext("dummy", "dummy")
                )
            )
        }
    }
}
