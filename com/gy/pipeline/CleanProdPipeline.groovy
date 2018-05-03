package com.gy.pipeline
import com.gy.utils.ArtifactoryUtil

class CleanProdPipeline extends Pipeline
{
    def artifactoryServer
    CleanProdPipeline(script)
    {
        super(script, 'clean prod')
    }

    def process(config)
    {
        try
        {
            def stageConfig
            //build & test & publish
            def buildInfo
            def releaseVersion
            def deployCredentialMap = [:]
            steps.node(config.node) {
                // stage1 check out
                script.githubStatus 'Pipeline/PROD', 'PENDING', 'Checking out...'
                stageCheckout(config)
                releaseVersion = script.stepUpdateVersion()
                script.githubStatus 'Pipeline/PROD', 'PENDING', 'Building...'
                stageConfig = configParser.getStageConfig(config, 'build')
                buildInfo = stageBuildWithArtifact(stageConfig)
                script.githubStatus 'Pipeline/PROD', 'SUCCESS', 'Building'

                def publishConfig = configParser.getStageConfig(config, 'publish')
                if (buildInfo)
                {
                    publishConfig.buildInfo = buildInfo
                }
                stageCodePublish(publishConfig)

                stageConfig = configParser.getStageConfig(config, 'stage')
                // put the releaseVersion into extra-vars
                def extraArgs = stageConfig.extra_args
                if (extraArgs == null) {
                    extraArgs = [:]
                    stageConfig.extra_args = extraArgs
                }
                extraArgs.put('releaseVersion',releaseVersion)
                ArtifactoryUtil artifactoryUtil = new ArtifactoryUtil()
                extraArgs.put('artifactoryServer', artifactoryUtil.getArtifacotryUrl(com.ssc.dpe.utils.Constants.DEFAULT_ARTIFACTORY_PROD_SERVER))

                def stackName = stageConfig.prod_profile_name
                if (stackName == null) {
                    stackName = configParser.getPropertyFromGlobalConfig(stageConfig, 'prod_profile_name')
                }
                stageConfig.stackName = stackName
                stageConfig.deployCredentialMap = deployCredentialMap
                stageBlueGreenDeploy(stageConfig)

                stageConfig = configParser.getStageConfig(config, 'go alive')
            }
            // Manual stage have to be running outside of node
            stageProdSignOff(stageConfig)
            // deploy & automatic test
            steps.node(config.node) {
                stageConfig = configParser.getStageConfig(config, 'production')
                // put the releaseVersion into extra-vars
                def extraArgs = stageConfig.extra_args
                if (extraArgs == null) {
                    extraArgs = [:]
                    stageConfig.extra_args = extraArgs
                }
                extraArgs.put('releaseVersion',releaseVersion)
                stageConfig.deployCredentialMap = deployCredentialMap
                stageProdGoAlive(stageConfig)

                script.githubStatus 'Pipeline/PROD', 'SUCCESS', 'Go Production Succeed'
            }

        } catch (Exception rethrow)
        {
            steps.node(config.node) {
                script.githubStatus 'Pipeline/PROD', 'FAILURE', 'Pipeline is failed'
            }
            throw rethrow
        }
    }
}
