package com.mongodb.quickpr.config

import com.intellij.ide.util.PropertiesComponent
import com.mongodb.quickpr.models.SettingsModel

private const val PREFIX = "QUICKPR_"
private const val GITHUB_TOKEN_SETTING = PREFIX + "GITHUB_TOKEN"
private const val JIRA_CONFIG_SETTING = PREFIX + "JIRA_CONFIG"

object SettingsManager {
    fun loadSettings(): SettingsModel {
        return SettingsModel(
            PropertiesComponent.getInstance().getValue(JIRA_CONFIG_SETTING, ""),
            PropertiesComponent.getInstance().getValue(GITHUB_TOKEN_SETTING, "")
        )
    }

    fun saveSettings(model: SettingsModel) {
        PropertiesComponent.getInstance().setValue(GITHUB_TOKEN_SETTING, model.githubToken)
        PropertiesComponent.getInstance().setValue(JIRA_CONFIG_SETTING, model.jiraConfigPath)
    }
}
