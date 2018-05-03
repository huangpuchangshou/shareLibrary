package com.gy.pipeline
import com.gy.utils.Constants
import com.gy.utils.ArtifactoryUtil

class UatPipeline extends Pipeline
{
    def artifactoryServer

    UatPipeline(script)
    {
        super(script, 'uat')
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
                stageCheckout(config)
                releaseVersion = script.stepUpdateVersion()
                stageConfig = configParser.getStageConfig(config, 'build')
                buildInfo = stageBuildWithArtifact(stageConfig)

                script.githubStatus 'Pipeline/UAT', 'PENDING', 'Code Scanning....'
                steps.stage('Scan') {
                    steps.parallel 'Code Scan': {
                        steps.stage('Code Scan') {
                            stageConfig = configParser.getStageConfig(config, 'code scan')
                            def codeScanConfig = [:]
                            def githubSourceInfo = utils.getGitSourceInfo()
                            codeScanConfig.put('sonar.github.endpoint', githubSourceInfo.endpointUrl)
                            codeScanConfig.put('sonar.github.repository', "${githubSourceInfo.repoOwner}/${githubSourceInfo.repository}")

                            def gitHubToken = configParser.getProperty(config, 'code scan', 'github.credentialsId')
                            gitHubToken = gitHubToken ?: Constants.DEFAULT_GITHUB_TOKEN

                            if(gitHubToken == null) {
                                throwPropertyRequiredExcpeiotn('github.credentialsId')
                            }

                            script.withCredentials([[$class: 'StringBinding', credentialsId: gitHubToken, variable: 'GITHUB_TOKEN']]) {
                                codeScanConfig.put('sonar.github.oauth', env.GITHUB_TOKEN)
                                stageConfig.codeScanConfig = codeScanConfig
                            }

                            def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_CODE_SCAN_TIMEOUT
                            steps.timeout(time: timeOut, unit: 'SECONDS') {
                                toolExecutor.codeScan stageConfig
                            }
                        }
                    },
                            'Vulnerability Scan': {
                                steps.stage('Vulnerability Scan') {
                                    def vulnStageConfig = configParser.getStageConfig(config, 'vulnerability scan')
                                    def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_CODE_SCAN_TIMEOUT
                                    steps.timeout(time: timeOut, unit: 'SECONDS') {
                                        toolExecutor.codeScan vulnStageConfig
                                    }
                                }
                            }
                }
                script.githubStatus 'Pipeline/UAT', 'SUCCESS', 'Code Scan Succeed'

                def publishConfig = configParser.getStageConfig(config, 'publish')
                if (buildInfo)
                {
                    publishConfig.buildInfo = buildInfo
                }
                stageCodePublish(publishConfig)
                stageConfig = configParser.getStageConfig(config, 'env ready')
            }
            // Manual stage have to be running outside of node
            stageEnvReady(stageConfig)
            // deploy & automatic test
            steps.node(config.node) {
                stageConfig = configParser.getStageConfig(config, 'deploy')
                stageConfig.deployCredentialMap = deployCredentialMap
                // put the releaseVersion into extra-vars
                def extraArgs = stageConfig.extra_args
                if (extraArgs == null)
                {
                    extraArgs = [:]
                    stageConfig.extra_args = extraArgs
                }
                extraArgs.put('releaseVersion', releaseVersion)
                ArtifactoryUtil artifactoryUtil = new ArtifactoryUtil()
                extraArgs.put('artifactoryServer', artifactoryUtil.getArtifacotryUrl())

                def stackName = stageConfig.uat_profile_name
                if (stackName == null) {
                    stackName = configParser.getPropertyFromGlobalConfig(stageConfig, 'uat_profile_name')
                }
                stageConfig.stackName = stackName
                stageUATDeploy(stageConfig)

                stageConfig = configParser.getStageConfig(config, 'smoke test')
                if (stageConfig == null || stageConfig.skip)
                {
                    steps.echo 'Smoke test stage is skipped'
                } else
                {
                    stageSanityTest(stageConfig)
                }

                stageConfig = configParser.getStageConfig(config, 'functional test')
                if (stageConfig == null || stageConfig.skip)
                {
                    steps.echo 'Functional test stage is skipped'
                } else
                {
                    stageFunctionalTest(stageConfig)
                }

                stageConfig = configParser.getStageConfig(config, 'sign off')
            }
            stageAppSignOff(stageConfig)

            steps.node(config.node) {
                stageConfig = configParser.getStageConfig(config, 'release')
                stageConfig.buildInfo = buildInfo
                stageRelease(stageConfig)

                stageConfig.deployCredentialMap = deployCredentialMap
                script.stepBlueGreenGoAlive stageConfig
                script.githubStatus 'Pipeline/UAT', 'SUCCESS', 'Pipeline is done successfully'
            }
        } catch (Exception rethrow)
        {
            steps.node(config.node) {
                script.githubStatus 'Pipeline/UAT', 'FAILURE', 'Pipeline is failed'
            }
            throw rethrow
        }
    }


}

