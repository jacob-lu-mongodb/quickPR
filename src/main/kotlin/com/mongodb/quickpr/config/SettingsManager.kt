package com.mongodb.quickpr.config

import com.intellij.ide.util.PropertiesComponent
import com.mongodb.quickpr.core.Err
import com.mongodb.quickpr.core.andThen
import com.mongodb.quickpr.core.mapError
import com.mongodb.quickpr.github.GitError
import com.mongodb.quickpr.github.GitUtils
import com.mongodb.quickpr.jira.JiraClient
import com.mongodb.quickpr.jira.JiraError
import com.mongodb.quickpr.models.SettingsModel
import org.apache.commons.io.FilenameUtils

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

    fun validateSettings(model: SettingsModel): String? {
        if (model.githubToken.isBlank()) {
            return "GitHub token is required"
        }
        if (model.jiraConfigPath.isBlank()) {
            return "JIRA config is required"
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
                    JiraConfigError.FILED_CANNOT_BE_PARSED -> "JIRA config file cannot be parsed"
                    JiraError.ISSUE_NOT_FOUND -> "JIRA config file cannot be parsed" // TODO: change
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
}
