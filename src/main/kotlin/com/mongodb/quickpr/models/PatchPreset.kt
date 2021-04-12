package com.mongodb.quickpr.models

data class PatchPreset(
    var name: String = "",
    var patchConfig: UserPatchConfig = UserPatchConfig(),
)
