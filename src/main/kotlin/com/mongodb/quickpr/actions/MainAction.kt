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
import com.mongodb.quickpr.core.mapError
import com.mongodb.quickpr.core.runResultTry
import com.mongodb.quickpr.git.GitError
import com.mongodb.quickpr.git.GitUtils
import com.mongodb.quickpr.models.PRModel
import com.mongodb.quickpr.services.AppService
import com.mongodb.quickpr.services.JIRA_HOME
import com.mongodb.quickpr.services.JiraClient
import com.mongodb.quickpr.services.JiraError
import com.mongodb.quickpr.services.getAppState
import com.mongodb.quickpr.ui.MainDialogWrapper
import com.mongodb.quickpr.ui.PatchDialogWrapper
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHRepository
import javax.swing.Icon

// TODO: https://intellij-support.jetbrains.com/hc/en-us/community/posts/360000405300-Listen-for-Git-push-events-from-IntelliJ-plugin
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

            val prState = getAppState().prStates.getPRState(repo.name, branchName)
            val pr = if (prState == null) null else repo.getPullRequest(prState.prNumber)

            if (pr == null || pr.state != GHIssueState.OPEN) {
                val prModel = PRModel(
                    "$branchName: ${jiraIssue.summary}",
                    JIRA_HOME + "/browse/" + branchName + "\n\n" + jiraIssue.description!!
                )
                showCreatePRUI(repo, prModel, branchName)
            } else {
                showExistingPRUI(pr, prState!!.lastPatchInfo, projectPath, branchName)
            }
            Ok("")
        }.mapError {
            when (it) {
                GitError.NOT_GIT_REPO -> "Project directory is not a Git repo"
                GitError.BAD_CREDENTIALS -> "Bad GitHub credentials"
                GitError.INVALID_CREDENTIALS -> "Invalid GitHub credentials"
                GitError.NO_WRITE_PERMISSION_TO_REPO -> "Write permission to the remote repo is needed"
                GitError.REMOTE_BRANCH_NOT_FOUND -> "Error accessing remote branch, did you push your commits?"
                GitError.PR_ALREADY_EXISTS -> "There's already an open pull request"
                JiraError.ISSUE_NOT_FOUND -> "JIRA issue not found"
                JiraError.AUTHENTICATION_ERROR, JiraError.AUTHORIZATION_ERROR -> "Bad JIRA credentials"
                else -> "An error occurred"
            }
        }

        if (result is Err) {
            Messages.showErrorDialog(result.error, "Error")
        }
    }

    private fun showExistingPRUI(
        pr: GHPullRequest,
        lastPatchInfo: AppService.PatchInfo?,
        projectPath: String,
        branchName: String
    ) {
        PatchDialogWrapper(pr, lastPatchInfo, projectPath, branchName).show()
    }

    private fun showCreatePRUI(
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
                    getAppState().prStates.setPRState(
                        repo.name,
                        gitBranch,
                        AppService.PRState(prResult.value.number)
                    )
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
