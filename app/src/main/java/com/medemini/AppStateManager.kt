package com.medemini

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.medemini.ai.model.AIConfig

class AppStateManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("MedeMiniState", Context.MODE_PRIVATE)
    private val gson = Gson()

    var projectPath: String
        get() = prefs.getString("projectPath", "") ?: ""
        set(value) = prefs.edit().putString("projectPath", value).apply()

    var activeFilePath: String
        get() = prefs.getString("activeFilePath", "") ?: ""
        set(value) = prefs.edit().putString("activeFilePath", value).apply()

    var activeFileName: String
        get() = prefs.getString("activeFileName", "") ?: ""
        set(value) = prefs.edit().putString("activeFileName", value).apply()

    var activeFileContent: String
        get() = prefs.getString("activeFileContent", "") ?: ""
        set(value) = prefs.edit().putString("activeFileContent", value).apply()

    var apiBaseUrl: String
        get() = prefs.getString("apiBaseUrl", "") ?: ""
        set(value) = prefs.edit().putString("apiBaseUrl", value).apply()

    var apiKey: String
        get() = prefs.getString("apiKey", "") ?: ""
        set(value) = prefs.edit().putString("apiKey", value).apply()

    var model: String
        get() = prefs.getString("model", "") ?: ""
        set(value) = prefs.edit().putString("model", value).apply()

    var temperature: Float
        get() = prefs.getFloat("temperature", 0.7f)
        set(value) = prefs.edit().putFloat("temperature", value).apply()

    var maxTokens: Int
        get() = prefs.getInt("maxTokens", 4096)
        set(value) = prefs.edit().putInt("maxTokens", value).apply()

    fun getRecentProjects(): List<RecentProject> {
        val json = prefs.getString("recentProjects", "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<RecentProject>>() {}.type)
    }

    fun addRecentProject(project: RecentProject) {
        val projects = getRecentProjects().toMutableList()
        projects.removeAll { it.path == project.path }
        projects.add(0, project)
        if (projects.size > 10) {
            projects.removeLast()
        }
        val json = gson.toJson(projects)
        prefs.edit().putString("recentProjects", json).apply()
    }

    fun getRecentFiles(): List<RecentFile> {
        val json = prefs.getString("recentFiles", "[]") ?: "[]"
        return gson.fromJson(json, object : TypeToken<List<RecentFile>>() {}.type)
    }

    fun addRecentFile(file: RecentFile) {
        val files = getRecentFiles().toMutableList()
        files.removeAll { it.path == file.path }
        files.add(0, file)
        if (files.size > 10) {
            files.removeLast()
        }
        val json = gson.toJson(files)
        prefs.edit().putString("recentFiles", json).apply()
    }

    fun saveAIConfig(config: AIConfig) {
        apiBaseUrl = config.apiBaseUrl
        apiKey = config.apiKey
        model = config.model
        temperature = config.temperature
        maxTokens = config.maxTokens
    }

    fun loadAIConfig(): AIConfig {
        return AIConfig(
            apiBaseUrl = apiBaseUrl,
            apiKey = apiKey,
            model = model,
            temperature = temperature,
            maxTokens = maxTokens
        )
    }

    fun saveEditorState(projectPath: String, filePath: String, fileName: String, content: String) {
        this.projectPath = projectPath
        this.activeFilePath = filePath
        this.activeFileName = fileName
        this.activeFileContent = content
        addRecentProject(RecentProject(projectPath, projectPath.substringAfterLast('/')))
        addRecentFile(RecentFile(filePath, fileName))
    }

    fun hasSavedState(): Boolean {
        return projectPath.isNotEmpty()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

data class RecentProject(val path: String, val name: String)

data class RecentFile(val path: String, val name: String)