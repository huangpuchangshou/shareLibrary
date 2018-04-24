package com.gy.pipeline

import com.gy.utils.Constants

/**
 * SysPipeline is used for System/Integration Environment, run on integration* branches
 *
 * It will do the checkout, run the build, unit test and then system test
 *
 */
class SysPipeline extends Pipeline
{

    /**
     * constructor
     *
     * @param script
     */
    SysPipeline(script)
    {
        super(script, 'sys')
    }

    /**
     * run the pipeline details
     *
     * @param config
     * @return
     */
    def process(config)
    {
        //run on configured node
        steps.node(config.node)
        {
            try
            {
                script.githubStatus 'Pipeline/SYS', 'PENDING', 'Running SYS Pipeline...'
                
                //do the checkout
                stageCheckout(config)

                //do the build
                def stageConfig = configParser.getStageConfig(config, 'build')
                stageBuild(stageConfig)

                //run the unit test
                stageConfig = configParser.getStageConfig(config, 'unit test')
                stageUnitTest(stageConfig)

                //run the system test
                stageConfig = configParser.getStageConfig(config, 'system test')
                stageSystemTest(stageConfig)

                script.githubStatus 'Pipeline/SYS', 'PENDING', 'Code Scanning....'
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
                script.githubStatus 'Pipeline/SYS', 'SUCCESS', 'SYS Pipeline succeed!'

            } catch (Exception rethrow)
            {
                script.githubStatus 'Pipeline/SYS', 'FAILURE', 'SYS Pipeline Fail!'
                throw rethrow
            }
        }
    }
}
