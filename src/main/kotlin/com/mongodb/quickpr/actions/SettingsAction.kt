package com.mongodb.quickpr.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.mongodb.quickpr.config.JiraConfig
import com.mongodb.quickpr.config.SettingsManager.loadSettings
import com.mongodb.quickpr.config.SettingsManager.saveSettings
import com.mongodb.quickpr.models.SettingsModel
import com.mongodb.quickpr.ui.SettingsDialogWrapper
import org.apache.commons.io.FilenameUtils
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
            newSettings.jiraConfigPath =
                FilenameUtils.concat(System.getProperty("user.home"), ".mdbutils/config.yaml")
        }

        val validateSettings = fun(settings: SettingsModel): String? {
            if (JiraConfig.loadConfigFile(settings.jiraConfigPath) == null) {
                return "JIRA config file not found or invalid"
            }
            if (settings.githubToken.isBlank()) {
                return "GitHub token is empty"
            }
            return null
        }

        if (SettingsDialogWrapper(newSettings, validateSettings).showAndGet()) {
            // user pressed OK
            if (savedSettings != newSettings) {
                saveSettings(newSettings)
            }
        }
    }
}
