package com.mongodb.quickpr.jira

import com.google.api.client.auth.oauth.OAuthGetAccessToken
import com.google.api.client.auth.oauth.OAuthRsaSigner
import com.google.api.client.http.apache.v2.ApacheHttpTransport
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.StringReader
import java.security.KeyPair
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.Security
import java.security.spec.InvalidKeySpecException

// Modified from https://bitbucket.org/atlassianlabs/atlassian-oauth-examples/src/master/java/
class JiraOAuthTokenFactory(jiraBaseUrl: String) {
    private val accessTokenUrl: String = "$jiraBaseUrl/plugins/servlet/oauth/access-token"

    /**
     * Initialize JiraOAuthGetAccessToken
     * by setting it to use POST method, secret, request token
     * and setting consumer and private keys.
     *
     * @param tmpToken    request token
     * @param secret      secret (verification code provided by JIRA after request token authorization)
     * @param consumerKey consumer ey
     * @param privateKey  private key in PKCS1 format
     * @return JiraOAuthGetAccessToken request
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun getJiraOAuthGetAccessToken(
        tmpToken: String?,
        secret: String?,
        consumerKey: String?,
        privateKey: String
    ): OAuthGetAccessToken {
        val accessToken = object : OAuthGetAccessToken(accessTokenUrl) {
            init {
                usePost = true
            }
        }

        accessToken.consumerKey = consumerKey
        accessToken.signer = getOAuthRsaSigner(privateKey)
        accessToken.transport = ApacheHttpTransport()
        accessToken.verifier = secret
        accessToken.temporaryToken = tmpToken
        return accessToken
    }

    /**
     * @param privateKey private key in PKCS1 format
     * @return OAuthRsaSigner
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    private fun getOAuthRsaSigner(privateKey: String): OAuthRsaSigner {
        val oAuthRsaSigner = OAuthRsaSigner()
        oAuthRsaSigner.privateKey = getPrivateKey(privateKey)
        return oAuthRsaSigner
    }

    /**
     * Creates PrivateKey from string
     *
     * @param privateKey private key in PKCS1 format
     * @return private key
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    private fun getPrivateKey(privateKey: String): PrivateKey {
        Security.addProvider(BouncyCastleProvider())
        val pemParser = PEMParser(StringReader(privateKey))
        val converter = JcaPEMKeyConverter().setProvider("BC")
        val keyObject = pemParser.readObject()
        val kp: KeyPair = converter.getKeyPair(keyObject as PEMKeyPair)

        return kp.private
    }
}
