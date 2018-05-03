package com.gy.pipeline
import com.gy.utils.Constants

class HotfixPipeline extends Pipeline
{
    def artifactoryServer
    HotfixPipeline(script)
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
                script.githubStatus 'Pipeline/Hotfix', 'PENDING', 'Checking out...'
                stageCheckout(config)
                releaseVersion = script.stepUpdateVersion()
                script.githubStatus 'Pipeline/Hotfix', 'PENDING', 'Building...'
                stageConfig = configParser.getStageConfig(config, 'build')
                buildInfo = stageBuildWithArtifact(stageConfig)
                script.githubStatus 'Pipeline/Hotfix', 'SUCCESS', 'Building'

                //TODO add logic of static code and Vulnerability scan
                script.githubStatus 'Pipeline/Hotfix', 'PENDING', 'Code Scanning....'
                steps.stage('Scan') {
                    steps.parallel     'Code Scan': {
                        steps.stage('Code Scan') {
                            stageConfig = configParser.getStageConfig(config, 'code scan')
                            def codeScanConfig = [:]
                            def githubSourceInfo = utils.getGitSourceInfo()
                            codeScanConfig.put('sonar.github.endpoint', githubSourceInfo.endpointUrl)
                            codeScanConfig.put('sonar.github.repository', "${githubSourceInfo.repoOwner}/${githubSourceInfo.repository}")

                            def gitHubToken = configParser.getProperty(config, 'code scan', 'github.credentialsId')
                            gitHubToken = gitHubToken ?: Constants.DEFAULT_GITHUB_TOKEN
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
                script.githubStatus 'Pipeline/Hotfix', 'SUCCESS', 'Code Scan Succeed'

                def publishConfig = configParser.getStageConfig(config, 'publish')
                if (buildInfo)
                {
                    publishConfig.buildInfo = buildInfo
                }
                stageCodePublish(publishConfig)

                stageConfig = configParser.getStageConfig(config, 'env ready')
            }
            // Manual stage have to be running outside of node
            stageHotfixEnvReady(stageConfig)

            steps.node(config.node) {
                stageConfig = configParser.getStageConfig(config, 'stage')
                // put the releaseVersion into extra-vars
                def extraArgs = stageConfig.extra_args
                if (extraArgs == null)
                {
                    extraArgs = [:]
                    stageConfig.extra_args = extraArgs
                }
                extraArgs.put('releaseVersion', releaseVersion)
                def stackName = stageConfig.prod_profile_name
                if (stackName == null)
                {
                    stackName = configParser.getPropertyFromGlobalConfig(stageConfig, 'prod_profile_name')
                }
                stageConfig.stackName = stackName
                stageConfig.deployCredentialMap = deployCredentialMap
                stageBlueGreenDeploy(stageConfig)

                //TODO add regression test
                stageConfig = configParser.getStageConfig(config, 'regression test')
                if (stageConfig == null || stageConfig.skip)
                {
                    steps.echo 'Regression test stage is skipped'
                } else
                {
                    stageFunctionalTest(stageConfig)
                }


                stageConfig = configParser.getStageConfig(config, 'sign off')
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

                script.githubStatus 'Pipeline/Hotfix', 'SUCCESS', 'Go Production Succeed'
            }

        } catch (Exception rethrow)
        {
            steps.node(config.node) {
                script.githubStatus 'Pipeline/Hotfix', 'FAILURE', 'Pipeline is failed'
            }
            throw rethrow
        }
    }
}
