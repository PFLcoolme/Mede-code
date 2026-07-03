package com.medemini.ai.model

interface BuiltInModelProvider {
    fun getModels(): List<BuiltInModel>
    fun getModelByName(name: String): BuiltInModel?
    fun addModel(model: BuiltInModel)
    fun removeModel(name: String): Boolean
    fun canUseModel(modelName: String): Boolean
    fun recordUsage(modelName: String)
    fun getRemainingUses(modelName: String): Int
}
