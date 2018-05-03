package com.gy.utils

def parseYaml(fileName)
{
    def config = readYaml file: fileName
    return config
}

@NonCPS
def getProperty(configInAll, stageName, properName)
{
    def currentPipelineName = configInAll.currentPipelineName
    def pipelineConfig
    def stageConfig = configInAll.stage.find { it.name.equals stageName }
    if (currentPipelineName != null)
    {
        pipelineConfig = configInAll.pipeline.find { it.name.equals currentPipelineName }
        if (pipelineConfig != null)
        {
            tempStageConfig = pipelineConfig.stage.find { it.name.equals stageName }
            if (tempStageConfig != null)
            {
                stageConfig = tempStageConfig
            }
        }
    }
    def property
    if (stageConfig != null)
    {
        property = stageConfig.get(properName)
        if (property != null)
        {
            return property
        }
    } else if (pipelineConfig != null)
    {
        property = pipelineConfig.get(properName)
        if (property != null)
        {
            return property
        }
    }
    return configInAll.get(properName)
}

@NonCPS
def getStageConfig(configInAll, stageName)
{
    def currentPipelineName = configInAll.currentPipelineName
    def pipelineConfig
    def stageConfig = configInAll.stage.find { it.name.equals stageName }
    if (currentPipelineName != null)
    {
        pipelineConfig = configInAll.pipeline.find { it.name.equals currentPipelineName }
        if (pipelineConfig != null)
        {
            tempStageConfig = pipelineConfig.stage.find { it.name.equals stageName }
            if (tempStageConfig != null)
            {
                stageConfig = tempStageConfig
            }
        }
    }
    if (stageConfig == null) {
        stageConfig = [:]
        stageConfig.skip = true
    }
    stageConfig.currentStageName = stageName
    stageConfig.globalConfig = configInAll
    stageConfig.pipelineConfig = pipelineConfig
    return stageConfig
}

@NonCPS
def getPropertyFromGlobalConfig(stageConfig, propertyName)
{
   if(stageConfig == null) {
       return null
   }
   configInAll = stageConfig.globalConfig
   if (configInAll == null) {
       configInAll = [:]
   }
    def currentPipelineName = configInAll.currentPipelineName

    def property
    property = configInAll.get(propertyName)

    def pipelineConfig
    if (currentPipelineName != null)
    {
        pipelineConfig = configInAll.pipeline.find { it.name.equals currentPipelineName }
        if (pipelineConfig != null)
        {
            def tempProperty = pipelineConfig.get(propertyName)
            if (tempProperty != null) {
                property = tempProperty
            }
        }
    }
    return property
}
