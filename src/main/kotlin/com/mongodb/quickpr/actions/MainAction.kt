package com.mongodb.quickpr.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.mongodb.quickpr.config.JiraConfig
import com.mongodb.quickpr.config.SettingsManager
import com.mongodb.quickpr.core.Err
import com.mongodb.quickpr.core.Ok
import com.mongodb.quickpr.core.SafeError
import com.mongodb.quickpr.core.andThen
import com.mongodb.quickpr.core.mapError
import com.mongodb.quickpr.core.runResultTry
import com.mongodb.quickpr.github.GitError
import com.mongodb.quickpr.github.GitUtils
import com.mongodb.quickpr.jira.JIRA_HOME
import com.mongodb.quickpr.jira.JiraClient
import com.mongodb.quickpr.jira.JiraError
import com.mongodb.quickpr.models.PRModel
import com.mongodb.quickpr.ui.MainDialogWrapper
import org.kohsuke.github.GHRepository
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
        val settingValidationError = SettingsManager.validateSettings(settings)

        if (settingValidationError != null) {
            SettingsAction.invokeAction()
        }

        populateAndShowUi(event.project?.basePath!!)
    }

    private fun populateAndShowUi(projectPath: String) {
        val result = runResultTry<Any, SafeError> {
            val settings = SettingsManager.loadSettings()

            val repoPath = GitUtils.getRepoPath(projectPath).abortOnError()
            val repo = GitUtils.getRepo(settings.githubToken, repoPath)
                .abortOnError()

            val branchName = GitUtils.getBranchName(projectPath).abortOnError()
            GitUtils.getRemoteBranch(repo, branchName).abortOnError()

            val jiraConfig =
                JiraConfig.loadConfigFile(SettingsManager.loadSettings().jiraConfigPath)
                    .abortOnError()
            val jiraIssue = JiraClient(jiraConfig).getIssue(branchName).abortOnError()

            val existingPrResult = GitUtils.getPrByBranch(branchName)
            if (existingPrResult is Ok) {
                BrowserUtil.browse(existingPrResult.value.htmlUrl)
                existingPrResult.andThen { Err(GitError.PR_ALREADY_EXISTS) }.abortOnError()
            }

            val prModel = PRModel(
                "$branchName: ${jiraIssue.summary}",
                JIRA_HOME + "/browse/" + branchName + "\n\n" + jiraIssue.description!!
            )
            showUi(repo, prModel, branchName)

            Ok("")
        }.mapError {
            when (it) {
                GitError.NOT_GIT_REPO -> "Project directory is not a Git repo"
                GitError.BAD_CREDENTIALS -> "Bad GitHub credentials"
                GitError.INVALID_CREDENTIALS -> "Invalid GitHub credentials"
                GitError.NO_WRITE_PERMISSION_TO_REPO -> "Write permission to the remote repo is needed"
                GitError.REMOTE_BRANCH_NOT_FOUND -> "Error accessing remote branch, did you push your commits?"
                GitError.PR_ALREADY_EXISTS -> "There's already an open pull request"
                JiraError.ISSUE_NOT_FOUND ->
                    "Error fetching JIRA issue, invalid ticket number or issue with credentials?"
                else -> "An error occurred"
            }
        }

        if (result is Err) {
            Messages.showErrorDialog(result.error, "Error")
        }
    }

    private fun showUi(
        repo: GHRepository,
        prModel: PRModel,
        gitBranch: String
    ) {
        val action = fun(): Boolean {
            val prResult = GitUtils.createPr(repo, prModel, gitBranch).mapError {
                when (it) {
                    GitError.PR_ALREADY_EXISTS -> "There's already an open pull request"
                    else -> "There was an error creating PR"
                }
            }

            return when (prResult) {
                is Ok -> {
                    BrowserUtil.browse(prResult.value.htmlUrl)
                    true
                }
                is Err -> {
                    Messages.showErrorDialog(prResult.error, "Error")
                    false
                }
            }
        }

        MainDialogWrapper(prModel, action).show()
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
}
