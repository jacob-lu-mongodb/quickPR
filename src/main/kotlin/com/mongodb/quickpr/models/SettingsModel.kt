package com.mongodb.quickpr.models

data class SettingsModel(
    var jiraConfigPath: String = "",
    var evgConfigPath: String = "",
    var githubToken: String = ""
)
