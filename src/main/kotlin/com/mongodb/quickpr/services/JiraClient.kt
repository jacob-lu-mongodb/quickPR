package com.mongodb.quickpr.services

import com.atlassian.httpclient.apache.httpcomponents.DefaultRequest
import com.atlassian.httpclient.api.Request
import com.atlassian.jira.rest.client.api.RestClientException
import com.atlassian.jira.rest.client.api.domain.Issue
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory
import com.google.api.client.auth.oauth.OAuthParameters
import com.google.api.client.http.GenericUrl
import com.intellij.openapi.diagnostic.Logger
import com.mongodb.quickpr.config.JiraConfig
import com.mongodb.quickpr.core.CommonError
import com.mongodb.quickpr.core.Err
import com.mongodb.quickpr.core.Ok
import com.mongodb.quickpr.core.SafeError
import com.mongodb.quickpr.core.SafeResult
import java.net.URI
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException

const val JIRA_HOME = "https://jira.mongodb.org"

private val logger = Logger.getInstance(JiraClient::class.java)

class JiraClient(private val jiraConfig: JiraConfig) {
    @Suppress("ReturnCount")
    fun getIssue(issueNumber: String): SafeResult<Issue, SafeError> {

        val factory = AsynchronousJiraRestClientFactory()

        // https://bitbucket.org/atlassian/jira-rest-java-client/src/master/test/src/test/java/samples/
        // https://developer.atlassian.com/cloud/jira/platform/jira-rest-api-oauth-authentication/

        val parameters = getOAuthParameters(
            jiraConfig.accessToken,
            jiraConfig.accessTokenSecret,
            jiraConfig.consumerKey,
            jiraConfig.privateKey
        )

        val restClient = factory.create(
            URI(JIRA_HOME)
        ) { builder ->
            val methodField =
                DefaultRequest.DefaultRequestBuilder::class.java.getDeclaredField("method")
            methodField.isAccessible = true
            val method = methodField.get(builder) as Request.Method

            val uriField =
                DefaultRequest.DefaultRequestBuilder::class.java.getDeclaredField("uri")
            uriField.isAccessible = true
            val uri = uriField.get(builder) as URI

            parameters.computeNonce()
            parameters.computeTimestamp()
            parameters.computeSignature(method.name, GenericUrl(uri))

            builder.setHeader("Authorization", parameters.authorizationHeader)
        }
        try {
            restClient.use { client ->
                return Ok(client.issueClient.getIssue(issueNumber).claim())
            }
        } catch (e: RestClientException) {
            if (e.statusCode.isPresent) {
                @Suppress("MagicNumber")
                val error: SafeError = when (e.statusCode.get()) {
                    401 -> JiraError.AUTHENTICATION_ERROR
                    403 -> JiraError.AUTHORIZATION_ERROR
                    404 -> JiraError.ISSUE_NOT_FOUND
                    else -> {
                        logger.error(e)
                        CommonError.UNKNOWN
                    }
                }
                return Err(error)
            }
            logger.error(e)
            return Err(CommonError.UNKNOWN)
        }
    }

    /**
     * Creates OAuthParameters used to make authorized request to JIRA
     *
     * @param tmpToken
     * @param secret
     * @param consumerKey
     * @param privateKey
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    private fun getOAuthParameters(
        tmpToken: String?,
        secret: String?,
        consumerKey: String?,
        privateKey: String?
    ): OAuthParameters {
        val oAuthGetAccessTokenFactory = JiraOAuthTokenFactory(
            JIRA_HOME
        )

        val oAuthAccessToken = oAuthGetAccessTokenFactory.getJiraOAuthGetAccessToken(
            tmpToken,
            secret,
            consumerKey,
            privateKey!!
        )
        oAuthAccessToken.verifier = secret
        return oAuthAccessToken.createParameters()
    }
}

enum class JiraError : SafeError {
    ISSUE_NOT_FOUND,
    AUTHENTICATION_ERROR,
    AUTHORIZATION_ERROR,
}
