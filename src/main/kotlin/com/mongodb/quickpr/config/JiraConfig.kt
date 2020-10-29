package com.mongodb.quickpr.config

import org.yaml.snakeyaml.Yaml
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path

class JiraConfig {
    var consumerKey: String = ""
    var accessToken: String = ""
    var accessTokenSecret: String = ""
    var privateKey: String = ""

    companion object {
        fun loadConfigFile(path: String): JiraConfig? {
            if (Files.notExists(Path.of(path))) {
                return null
            }

            try {
                FileInputStream(path).use {
                    val obj: Map<String, Map<String, String>> = Yaml().load(it)

                    val jira = obj["jira"] ?: return null

                    val jiraConfig = JiraConfig()
                    jiraConfig.accessToken = jira["access_token"]!!
                    jiraConfig.accessTokenSecret = jira["access_token_secret"]!!
                    jiraConfig.consumerKey = jira["consumer_key"]!!
                    jiraConfig.privateKey = jira["key_cert"]!!

                    return jiraConfig
                }
            } catch (e: Exception) {
                return null
            }
        }
    }
}
