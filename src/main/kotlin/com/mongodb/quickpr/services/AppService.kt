package com.mongodb.quickpr.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.mongodb.quickpr.models.PatchPreset
import com.mongodb.quickpr.models.UserPatchConfig

@State(name = "quickPRAppService", storages = [Storage("quickPRAppService.xml")])
class AppService : PersistentStateComponent<AppService.State> {
    private var state: State = State()

    data class PatchPresetMap(var map: MutableMap<String, MutableList<PatchPreset>> = mutableMapOf()) {
        fun getPresets(repo: String): List<PatchPreset> {
            return map[repo] ?: mutableListOf()
        }

        fun savePreset(repo: String, preset: PatchPreset) {
            map.putIfAbsent(repo, mutableListOf())
            map[repo]!!.removeIf { it.name == preset.name }
            map[repo]!!.add(preset)
        }

        fun deletePreset(repo: String, name: String) {
            map[repo]?.removeIf { it.name == name }
        }
    }

    data class PatchInfo(
        var patchId: String = "",
        var patchConfig: UserPatchConfig = UserPatchConfig()
    )

    data class PRState(var prNumber: Int = 0, var lastPatchInfo: PatchInfo? = null) {
    }

    data class PRStateMap(var map: MutableMap<String, MutableMap<String, PRState>> = mutableMapOf()) {

//        override fun equals(other: Any?): Boolean {
//            return other != null && other is PRStateMap && other.map == map
//        }
//
//        override fun hashCode(): Int {
//            return map.hashCode()
//        }

        fun getPRState(repo: String, branch: String): PRState? {
            return map[repo]?.get(branch)
//            return null;
        }

        fun setPRState(repo: String, branch: String, state: PRState) {
            map.putIfAbsent(repo, mutableMapOf())
            map[repo]!![branch] = state
//            map.put(repo, branch)
        }
    }

//    class RepoPRStateMap {
//        private val map: Map<String, PRState> = mutableMapOf()
//
//        override fun equals(other: Any?): Boolean {
//            return other != null && other is RepoPRStateMap && other.map == map
//        }
//
//        override fun hashCode(): Int {
//            return map.hashCode()
//        }
//
//        fun getPRState(branch: String): PRState?{
//            return map[branch]
//        }
//    }

    data class State(
        var prStates: PRStateMap = PRStateMap(),
        var patchPresets: PatchPresetMap = PatchPresetMap()
    )

//    data class State(var a: String = "") {
//        fun doo(aa: String) {
//            a = aa
//        }
//    }

    override fun getState(): State? {
        return state;
    }

    override fun loadState(state: State) {
        this.state = state
    }
}

fun getAppState(): AppService.State {
    return service<AppService>().state!!
}