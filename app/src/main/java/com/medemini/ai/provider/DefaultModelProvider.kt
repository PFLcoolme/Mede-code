package com.medemini.ai.provider

import android.content.Context
import android.content.SharedPreferences
import com.medemini.ai.model.BuiltInModel
import com.medemini.ai.model.BuiltInModelProvider
import com.medemini.ai.native.CryptoBridge
import java.security.MessageDigest
import java.util.*

class DefaultModelProvider(private val context: Context) : BuiltInModelProvider {

    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences("medemini_model_limits", Context.MODE_PRIVATE)
    }

    private val models = mutableListOf<BuiltInModel>()

    init {
        loadBuiltinModels()
    }

    private fun loadBuiltinModels() {
        try {
            val apiKey = CryptoBridge.getBuiltinKey()
            models.add(
                BuiltInModel(
                    name = "Qwen3.5-397B-A17B",
                    endpoint = "https://api.iamhc.cn/v1/",
                    apiKey = apiKey,
                    dailyLimit = 30,
                    enabled = true,
                    description = "阿里通义千问3.5系列旗舰模型"
                )
            )
        } catch (_: Exception) {
        }
    }

    override fun getModels(): List<BuiltInModel> {
        return models.filter { it.enabled }
    }

    override fun getModelByName(name: String): BuiltInModel? {
        return models.find { it.name == name }
    }

    override fun addModel(model: BuiltInModel) {
        if (!models.any { it.name == model.name }) {
            models.add(model)
        }
    }

    override fun removeModel(name: String): Boolean {
        return models.removeIf { it.name == name }
    }

    override fun canUseModel(modelName: String): Boolean {
        val model = getModelByName(modelName) ?: return false
        if (!model.enabled) return false
        return getRemainingUses(modelName) > 0
    }

    override fun recordUsage(modelName: String) {
        val today = getTodayKey()
        val current = preferences.getInt("usage_${modelName}_$today", 0)
        preferences.edit().putInt("usage_${modelName}_$today", current + 1).apply()
    }

    override fun getRemainingUses(modelName: String): Int {
        val model = getModelByName(modelName) ?: return 0
        val today = getTodayKey()
        val used = preferences.getInt("usage_${modelName}_$today", 0)
        return maxOf(0, model.dailyLimit - used)
    }

    private fun getTodayKey(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}${calendar.get(Calendar.MONTH) + 1}${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    fun getDailyLimitForModel(modelName: String): Int {
        return getModelByName(modelName)?.dailyLimit ?: 0
    }
}
