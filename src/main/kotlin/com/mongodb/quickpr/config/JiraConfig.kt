package com.mongodb.quickpr.config

import com.mongodb.quickpr.core.Err
import com.mongodb.quickpr.core.Ok
import com.mongodb.quickpr.core.SafeError
import com.mongodb.quickpr.core.SafeResult
import org.yaml.snakeyaml.Yaml
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class JiraConfig {
    var consumerKey: String = ""
    var accessToken: String = ""
    var accessTokenSecret: String = ""
    var privateKey: String = ""

    companion object {
        fun loadConfigFile(path: String): SafeResult<JiraConfig, JiraConfigError> {
            val filePath = Path.of(path)
            if (Files.notExists(filePath) || !Files.isRegularFile(filePath)) {
                return Err(JiraConfigError.FILE_NOT_EXIST)
            }

            try {
                FileInputStream(path).use {
                    val obj: Map<String, Map<String, String>> = Yaml().load(it)
                    val jira = obj["jira"]!!

                    val jiraConfig = JiraConfig()
                    jiraConfig.accessToken = jira["access_token"]!!
                    jiraConfig.accessTokenSecret = jira["access_token_secret"]!!
                    jiraConfig.consumerKey = jira["consumer_key"]!!
                    jiraConfig.privateKey = jira["key_cert"]!!

                    return Ok(jiraConfig)
                }
            } catch (e: IOException) {
                return Err(JiraConfigError.FILE_CANNOT_BE_OPENED)
            } catch (e: Exception) {
                return Err(JiraConfigError.FILED_CANNOT_BE_PARSED)
            }
        }
    }
}

enum class JiraConfigError : SafeError {
    FILE_NOT_EXIST,
    FILE_CANNOT_BE_OPENED,
    FILED_CANNOT_BE_PARSED
}
