package com.mongodb.quickpr.config

import com.intellij.ide.util.PropertiesComponent
import com.mongodb.quickpr.core.Err
import com.mongodb.quickpr.core.andThen
import com.mongodb.quickpr.core.mapError
import com.mongodb.quickpr.git.GitError
import com.mongodb.quickpr.git.GitUtils
import com.mongodb.quickpr.models.SettingsModel
import com.mongodb.quickpr.services.JiraClient
import com.mongodb.quickpr.services.JiraError
import org.apache.commons.io.FilenameUtils

private const val PREFIX = "QUICKPR_"
private const val GITHUB_TOKEN_SETTING = PREFIX + "GITHUB_TOKEN"
private const val JIRA_CONFIG_SETTING = PREFIX + "JIRA_CONFIG"
private const val EVG_CONFIG_SETTING = PREFIX + "EVG_CONFIG"

object SettingsManager {
    fun loadSettings(): SettingsModel {
        return SettingsModel(
            PropertiesComponent.getInstance().getValue(JIRA_CONFIG_SETTING, ""),
            PropertiesComponent.getInstance().getValue(EVG_CONFIG_SETTING, ""),
            PropertiesComponent.getInstance().getValue(GITHUB_TOKEN_SETTING, "")
        )
    }

    // TODO: https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html
    fun saveSettings(model: SettingsModel) {
        PropertiesComponent.getInstance().setValue(GITHUB_TOKEN_SETTING, model.githubToken)
        PropertiesComponent.getInstance().setValue(JIRA_CONFIG_SETTING, model.jiraConfigPath)
        PropertiesComponent.getInstance().setValue(EVG_CONFIG_SETTING, model.evgConfigPath)
    }

    @Suppress("ReturnCount")
    fun validateSettings(model: SettingsModel): String? {
        if (model.githubToken.isBlank()) {
            return "GitHub token is required"
        }
        if (model.jiraConfigPath.isBlank()) {
            return "JIRA config is required"
        }
        if (model.evgConfigPath.isBlank()) {
            return "Evergreen config is required"
        }

        val githubResult = GitUtils.getGithub(model.githubToken).mapError {
            when (it) {
                GitError.BAD_CREDENTIALS, GitError.INVALID_CREDENTIALS -> "Bad GitHub credentials"
                else -> null
            }
        }

        if (githubResult is Err && githubResult.error != null) {
            return githubResult.error
        }

        val jiraConfigLoadResult = JiraConfig.loadConfigFile(model.jiraConfigPath)
            .andThen { JiraClient(it).getIssue("CLOUDP-10000") }
            .mapError {
                when (it) {
                    JiraConfigError.FILE_NOT_EXIST -> "JIRA config file does not exist"
                    JiraConfigError.FILE_CANNOT_BE_OPENED -> "JIRA config file cannot be opened"
                    JiraConfigError.INVALID_CONTENT -> "JIRA config file has invalid content"
                    JiraError.AUTHENTICATION_ERROR, JiraError.AUTHORIZATION_ERROR -> "Bad JIRA credentials"
                    else -> null
                }
            }

        if (jiraConfigLoadResult is Err && jiraConfigLoadResult.error != null) {
            return jiraConfigLoadResult.error
        }

        return null
    }

    fun getDefaultJiraConfigFilePath(): String {
        return FilenameUtils.concat(System.getProperty("user.home"), ".mdbutils/config.yaml")
    }

    fun getDefaultEvgConfigFilePath(): String {
        return FilenameUtils.concat(System.getProperty("user.home"), ".evergreen.yml")
    }
}
