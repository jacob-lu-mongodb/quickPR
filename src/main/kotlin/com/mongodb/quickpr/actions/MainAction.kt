package com.mongodb.quickpr.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.mongodb.quickpr.config.JiraConfig
import com.mongodb.quickpr.config.SettingsManager
import com.mongodb.quickpr.jira.JIRA_HOME
import com.mongodb.quickpr.jira.JiraClient
import com.mongodb.quickpr.models.PRModel
import com.mongodb.quickpr.ui.MainDialogWrapper
import org.jetbrains.annotations.Nullable
import org.jetbrains.annotations.SystemIndependent
import org.kohsuke.github.GHPermissionType
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import javax.swing.Icon

class MainAction : AnAction {
    /**
     * This default constructor is used by the IntelliJ Platform framework to instantiate this class based on plugin.xml
     * declarations. Only needed in [MainAction] class because a second constructor is overridden.
     */
    constructor() : super()

    /**
     * This constructor is used to support dynamically added menu actions.
     * It sets the text, description to be displayed for the menu item.
     * Otherwise, the default AnAction constructor is used by the IntelliJ Platform.
     *
     * @param text        The text to be displayed as a menu item.
     * @param description The description of the menu item.
     * @param icon        The icon to be used with the menu item.
     */
    constructor(text: String?, description: String?, icon: Icon?) : super(
        text,
        description,
        icon
    )

    override fun actionPerformed(event: AnActionEvent) {
        val settings = SettingsManager.loadSettings()
        val jiraConfig = JiraConfig.loadConfigFile(settings.jiraConfigPath)

        if (settings.githubToken.isBlank() || jiraConfig == null) {
            SettingsAction("QuickPR Settings", null, null).actionPerformed(event)
        }

        populateUi(event)
    }

    private fun populateUi(event: AnActionEvent) {
        val currentGitBranch = getCurrentGitBranch(event.project?.basePath)

        if (currentGitBranch == null) {
            Messages.showErrorDialog("No Git repo detected", "Error")
            return
        }

        val jiraConfig = JiraConfig.loadConfigFile(SettingsManager.loadSettings().jiraConfigPath)

        var prModel = PRModel(
            "$currentGitBranch:",
            "$JIRA_HOME/browse/$currentGitBranch"
        )

        val issue = try {
            JiraClient(jiraConfig!!).getIssue(currentGitBranch)
        } catch (e: Exception) {
            Messages.showErrorDialog(
                "There was an error contacting JIRA, issue with credentials?",
                "Error"
            )
            showUi(prModel, event, currentGitBranch)
            return
        }

        if (issue == null) {
            Messages.showWarningDialog("No JIRA issue found for $currentGitBranch", "Warning")
            showUi(prModel, event, currentGitBranch)
            return
        }

        val existingPr = getExistingPr()
        if (existingPr != null) {
            Messages.showWarningDialog(
                "There's already an open pull request: " + existingPr.id,
                "Existing PR"
            )
            BrowserUtil.browse(existingPr.htmlUrl)
            return
        }

        prModel = PRModel(
            "$currentGitBranch: ${issue.summary}",
            JIRA_HOME + "/browse/" + currentGitBranch + "\n\n" + issue.description!!
        )
        showUi(prModel, event, currentGitBranch)
    }

    private fun showUi(
        model: PRModel,
        event: AnActionEvent,
        currentGitBranch: String?
    ) {
        val action = fun(): Boolean {
            val github: GitHub? = try {
                GitHubBuilder().withOAuthToken(
                    SettingsManager.loadSettings().githubToken
                ).build()
            } catch (e: Exception) {
                var errorMsg: String? = null
                if (e.message != null && e.message!!.contains("Bad credentials")) {
                    errorMsg = "Bad GitHub credentials"
                }

                if (errorMsg == null) {
                    null
                } else {
                    Messages.showErrorDialog(
                        errorMsg,
                        "Error"
                    )
                    return false
                }
            }

            if (github == null) {
                Messages.showErrorDialog(
                    "There was an error contacting GitHub",
                    "Error"
                )
                return false
            }

            if (!github.isCredentialValid) {
                Messages.showErrorDialog(
                    "Invalid GitHub credentials",
                    "Error"
                )
                return false
            }

            val gitHubRepo: GHRepository? = try {
                github.getRepository("10gen/mms")
            } catch (e: Exception) {
                var errorMsg: String? = null

                if (errorMsg == null) {
                    null
                } else {
                    Messages.showErrorDialog(
                        errorMsg,
                        "Error"
                    )
                    return false
                }
            }

            if (gitHubRepo == null) {
                Messages.showErrorDialog(
                    "There was an error accessing the GitHub repo",
                    "Error"
                )
                return false
            }

            val permission = gitHubRepo.getPermission(github.myself)
            if (!listOf(GHPermissionType.ADMIN, GHPermissionType.WRITE).contains(permission)) {
                Messages.showErrorDialog(
                    "Write permission is needed for the GitHub repo",
                    "Error"
                )
                return false
            }

            val pr: GHPullRequest? = try {
                gitHubRepo.createPullRequest(
                    model.title,
                    currentGitBranch,
                    "master",
                    model.description
                )
            } catch (e: Exception) {
                var errorMsg: String? = null
                if (e.message != null && e.message!!.contains("A pull request already exists")) {
                    errorMsg = "There's an open PR already"
                }

                if (errorMsg == null) {
                    null
                } else {
                    Messages.showErrorDialog(
                        errorMsg,
                        "Error"
                    )
                    return false
                }
            }

            if (pr == null) {
                Messages.showErrorDialog(
                    "There was an error creating PR",
                    "Error"
                )
                return false
            }

            BrowserUtil.browse(pr.htmlUrl)
            return true
        }

        MainDialogWrapper(model, action, event).show()
    }

    @Throws(IOException::class, InterruptedException::class)
    fun getCurrentGitBranch(basePath: @Nullable @SystemIndependent String?): String? {
        val process =
            Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD", null, File(basePath))
        process.waitFor()
        val reader = BufferedReader(
            InputStreamReader(process.inputStream)
        )
        return reader.readLine()
    }

    /**
     * Determines whether this menu item is available for the current context.
     * Requires a project to be open.
     *
     * @param e Event received when the associated group-id menu is chosen.
     */
    override fun update(e: AnActionEvent) {
        // Set the availability based on whether a project is open
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
    }

    private fun getExistingPr(): GHPullRequest? {
        // TODO: find a more efficient way of getting existing PR
        // val existingPr = repo.getPullRequests(GHIssueState.OPEN)
        //      .firstOrNull { pr -> pr.head.ref == currentGitBranch && pr.state == GHIssueState.OPEN }
        return null
    }
}
