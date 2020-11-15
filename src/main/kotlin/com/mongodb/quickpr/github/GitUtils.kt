package com.mongodb.quickpr.github

import com.intellij.openapi.diagnostic.Logger
import com.mongodb.quickpr.core.CommonError
import com.mongodb.quickpr.core.Err
import com.mongodb.quickpr.core.Ok
import com.mongodb.quickpr.core.SafeError
import com.mongodb.quickpr.core.SafeResult
import com.mongodb.quickpr.core.andThen
import com.mongodb.quickpr.models.PRModel
import org.kohsuke.github.GHBranch
import org.kohsuke.github.GHPermissionType
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

private val logger = Logger.getInstance(GitUtils.javaClass)

object GitUtils {
    fun getGithub(token: String): SafeResult<GitHub, SafeError> {
        val github: GitHub = try {
            GitHubBuilder().withOAuthToken(
                token
            ).build()
        } catch (e: IOException) {
            return if (e.message != null && e.message!!.contains("Bad credentials")) {
                Err(GitError.BAD_CREDENTIALS)
            } else {
                logger.error(e)
                Err(CommonError.UNKNOWN)
            }
        }

        return if (github.isCredentialValid) {
            Ok(github)
        } else {
            Err(GitError.INVALID_CREDENTIALS)
        }
    }

    private fun getRepo(github: GitHub, repoPath: String): SafeResult<GHRepository, SafeError> {
        try {
            val repo =
                github.getRepository(repoPath)

            val permission =
                repo.getPermission(github.myself)

            return if (listOf(GHPermissionType.ADMIN, GHPermissionType.WRITE).contains(permission)) {
                Ok(repo)
            } else {
                Err(GitError.NO_WRITE_PERMISSION_TO_REPO)
            }
        } catch (e: IOException) {
            logger.error(e)
            return Err(CommonError.UNKNOWN)
        }
    }

    fun getRepo(token: String, repoPath: String): SafeResult<GHRepository, SafeError> {
        return getGithub(token).andThen { getRepo(it, repoPath) }
    }

    fun getRemoteBranch(repo: GHRepository, branch: String): SafeResult<GHBranch, SafeError> {
        return try {
            Ok(repo.getBranch(branch))
        } catch (e: IOException) {
            // TODO: add check
//            logger.error(e)
            Err(GitError.REMOTE_BRANCH_NOT_FOUND)
        }
    }

    fun getBranchName(workingDir: String): SafeResult<String, SafeError> {
        val process =
            Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD", null, File(workingDir))
        process.waitFor()

        BufferedReader(
            InputStreamReader(process.inputStream)
        ).use {
            val line = it.readLine()

            return when {
                line.isBlank() -> Err(CommonError.UNKNOWN)
                line.contains("not a git repository") -> Err(GitError.NOT_GIT_REPO)
                else -> Ok(line)
            }
        }
    }

    fun getRepoPath(workingDir: String): SafeResult<String, SafeError> {
        val process =
            Runtime.getRuntime().exec("git remote -v", null, File(workingDir))
        process.waitFor()

        BufferedReader(
            InputStreamReader(process.inputStream)
        ).use {
            val line = it.readLine()

            if (line.contains("not a git repository")) {
                return Err(GitError.NOT_GIT_REPO)
            }
            val regex = Regex(""".*:(.*)\.git.*""")
            val matchResult = regex.matchEntire(line)

            return if (matchResult?.groups?.size == 2) {
                Ok(matchResult.groupValues[1])
            } else {
                Err(CommonError.UNKNOWN)
            }
        }
    }

    fun getPrByBranch(branchName: String): SafeResult<GHPullRequest, SafeError> {
        // TODO: find a more efficient way of getting existing PR
        // val existingPr = repo.getPullRequests(GHIssueState.OPEN)
        //      .firstOrNull { pr -> pr.head.ref == currentGitBranch && pr.state == GHIssueState.OPEN }
        return Err(GitError.PR_NOT_FOUND)
    }

    @Suppress("ReturnCount")
    fun createPr(
        repo: GHRepository,
        content: PRModel,
        branchName: String
    ): SafeResult<GHPullRequest, SafeError> {
        try {
            return Ok(
                repo.createPullRequest(
                    content.title,
                    branchName,
                    "master",
                    content.description
                )
            )
        } catch (e: IOException) {
            if (e.message != null && e.message!!.contains("A pull request already exists")) {
                return Err(GitError.PR_ALREADY_EXISTS)
            } else {
                return Err(CommonError.UNKNOWN)
            }
        }
    }
}

enum class GitError : SafeError {
    BAD_CREDENTIALS,
    INVALID_CREDENTIALS,
    NO_WRITE_PERMISSION_TO_REPO,
    NOT_GIT_REPO,
    REMOTE_BRANCH_NOT_FOUND,
    PR_ALREADY_EXISTS,
    PR_NOT_FOUND,
}
