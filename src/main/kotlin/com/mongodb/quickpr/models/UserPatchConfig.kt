package com.mongodb.quickpr.models

import com.google.api.client.util.Key
import com.google.gson.annotations.SerializedName

data class UserPatchConfig(
    var aliases: Set<String> = mutableSetOf(),
    var tasks: Set<PatchVariantTasks> = mutableSetOf(),
)

data class PatchVariantTasks(
    @Key("id") @SerializedName("id") var variant: String = "",
    @Key var tasks: Set<String> = mutableSetOf()
)
