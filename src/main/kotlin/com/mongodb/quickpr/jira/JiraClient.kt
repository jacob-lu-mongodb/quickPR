package com.mongodb.quickpr.jira

import com.atlassian.httpclient.apache.httpcomponents.DefaultRequest
import com.atlassian.httpclient.api.Request
import com.atlassian.jira.rest.client.api.domain.Issue
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory
import com.google.api.client.auth.oauth.OAuthParameters
import com.google.api.client.http.GenericUrl
import com.mongodb.quickpr.config.JiraConfig
import org.codehaus.jettison.json.JSONException
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException

const val JIRA_HOME = "https://jira.mongodb.org"

class JiraClient(private val jiraConfig: JiraConfig) {
    @Throws(URISyntaxException::class, JSONException::class, IOException::class)
    fun getIssue(issueNumber: String): Issue? {
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
        restClient.use { restClient ->
            return restClient.issueClient.getIssue(issueNumber).claim()
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
