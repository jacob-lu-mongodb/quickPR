package com.mongodb.quickpr.services

import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.http.json.JsonHttpContent
import com.google.api.client.json.gson.GsonFactory
import com.intellij.openapi.diagnostic.Logger
import com.mongodb.quickpr.models.PatchVariantTasks
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*
import java.util.stream.Collectors

private val logger = Logger.getInstance(EvergreenUtils.javaClass)

const val API_BASE = "https://evergreen.mongodb.com/api/rest/v2/"
const val LEGACY_API_BASE = "https://evergreen.mongodb.com/api/"

object EvergreenUtils {
    fun getAliasTasks(proj: String, alias: String) {
    }
}

class EvergreenClient(private val apiUser: String, private val apiKey: String) {

    fun getAliasTasks(proj: String, alias: String, versionId: String): List<PatchVariantTasks> {
        val requestFactory = NetHttpTransport().createRequestFactory()
        val request = requestFactory.buildGetRequest(
            GenericUrl(API_BASE + "projects/test_alias?&include_deps=true&version=" + versionId + "&alias=" + alias)
        )

        val headers = request.headers
        headers.set("Api-User", apiUser)
        headers.set("Api-Key", apiKey)

        val rawResponse = request.execute().parseAsString()
        val json = Json.parseToJsonElement(rawResponse)

        return json.jsonArray.stream().map {
            val obj = it.jsonObject
            val variant = obj["Variant"]!!.jsonPrimitive.content
            val tasks = obj["Tasks"]!!.jsonArray.stream().map { it.jsonPrimitive.content }.collect(
                Collectors.toSet()
            )
            PatchVariantTasks(variant, tasks)
        }.collect(Collectors.toList())
    }

    fun getLatestVersionId(proj: String): String {
        val requestFactory = NetHttpTransport().createRequestFactory()
        val request = requestFactory.buildGetRequest(
            GenericUrl(API_BASE + "projects/" + proj + "/versions")
        )

        val headers = request.headers
        headers.set("Api-User", apiUser)
        headers.set("Api-Key", apiKey)

        val rawResponse = request.execute().parseAsString()
        val json = Json.parseToJsonElement(rawResponse)

        return json.jsonArray[0].jsonObject["version_id"]!!.jsonPrimitive.content
    }

    fun createPatch(
        project: String,
        gitHash: String,
        gitDiff: String
    ): String {
        val requestFactory = NetHttpTransport().createRequestFactory()

        val data = LinkedHashMap<String, Any>()
        data["project"] = project
        data["githash"] = gitHash
        data["patch_bytes"] = Base64.getEncoder().encodeToString(gitDiff.toByteArray())

        val content = JsonHttpContent(GsonFactory.getDefaultInstance(), data)

        val request = requestFactory.buildPutRequest(
            GenericUrl(LEGACY_API_BASE + "patches/"), content
        )

        val headers = request.headers
        headers.set("Api-User", apiUser)
        headers.set("Api-Key", apiKey)

        val rawResponse = request.execute().parseAsString()
        val json = Json.parseToJsonElement(rawResponse)

        return json.jsonObject["patch"]!!.jsonObject["Id"]!!.jsonPrimitive.content
    }

    fun finalizePatch(patchId: String): String {
        val requestFactory = NetHttpTransport().createRequestFactory()

        val data = LinkedHashMap<String, String>()
        data["patch_id"] = patchId
        data["action"] = "finalize"
        val content = JsonHttpContent(GsonFactory.getDefaultInstance(), data)

        val request = requestFactory.buildPostRequest(
            GenericUrl(LEGACY_API_BASE + "patches/" + patchId), content
        )

        val headers = request.headers
        headers.set("Api-User", apiUser)
        headers.set("Api-Key", apiKey)

        val rawResponse = request.execute().parseAsString()

        if (rawResponse != "patch finalized") {
            throw IllegalStateException()
        }
        return rawResponse
    }

    fun configurePatch(
        patchId: String,
        tasks: Collection<PatchVariantTasks>,
    ): String {
        val requestFactory = NetHttpTransport().createRequestFactory()

        val data = LinkedHashMap<String, Any>()
        data["variants"] = tasks
//        data["project"] = project
//        data["githash"] = gitHash
//        data["patch_bytes"] = Base64.getEncoder().encodeToString(gitDiff.toByteArray())

        val content = JsonHttpContent(GsonFactory.getDefaultInstance(), data)

        val request = requestFactory.buildPostRequest(
            GenericUrl(API_BASE + "patches/" + patchId + "/configure"), content
        )

        val headers = request.headers
        headers.set("Api-User", apiUser)
        headers.set("Api-Key", apiKey)

        val rawResponse = request.execute().parseAsString()
        val json = Json.parseToJsonElement(rawResponse)

        return json.jsonObject["version_id"]!!.jsonPrimitive.content
    }
}
