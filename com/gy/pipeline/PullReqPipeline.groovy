package com.gy.pipeline
import com.gy.utils.Constants

/**
 * This pipeline is used for the Pull Request Status check.
 *
 * It will run on the Pull Request, do the checkout, build, unit test and also code scan.
 */
class PullReqPipeline extends Pipeline
{
    def pullRequestId
    /**
     * constructor
     * @param script
     */
    PullReqPipeline(script)
    {
        super(script, 'pr')
        pullRequestId = env.CHANGE_ID
    }

    /**
     * run the detail pipeline steps
     *
     * @param config
     * @return
     */
    def process(config)
    {
        //run on configured node
        steps.node(config.node)
        {
            def stageConfig

            //checking out source code
            try
            {
                script.githubStatus 'PR Check/Build', 'PENDING', 'Checking out...'
                stageCheckout(config)
            } catch (Exception r)
            {
                script.githubStatus 'PR Check/Build', 'FAILURE', 'Checkout Code Fail!'
                throw r
            }

            //do the build
            try
            {
                script.githubStatus 'PR Check/Build', 'PENDING', 'Building...'
                stageConfig = configParser.getStageConfig(config, 'build')
                stageBuild(stageConfig)
                script.githubStatus 'PR Check/Build', 'SUCCESS', 'Build Succeed!'
            } catch (Exception r)
            {
                script.githubStatus 'PR Check/Build', 'FAILURE', 'Build Fail!'
                throw r
            }

            //run the unit test
            try
            {
                script.githubStatus 'PR Check/Unit Test', 'PENDING', 'Unit Testing...'
                stageConfig = configParser.getStageConfig(config, 'unit test')
                stageUnitTest(stageConfig)
                def testStatus = utils.getUnitTestStatus()
                script.echo "testStus=${testStatus}"
                if (script.currentBuild.result == 'UNSTABLE')
                {
                    script.currentBuild.result = 'FAILURE'
                    script.githubStatus 'PR Check/Unit Test', 'FAILURE', "${testStatus.failedCount} of ${testStatus.totalCount} Unit Tests Fail!"

                } else
                {
                    script.githubStatus 'PR Check/Unit Test', 'SUCCESS', "${testStatus.totalCount} Unit Tests All Succeed!"
                }
            } catch (Exception r)
            {
                script.githubStatus 'PR Check/Unit Test', 'FAILURE', 'Unit Test Fail!'
                throw r
            }

            //run the code scan if required
            try
            {
                stageConfig = configParser.getStageConfig(config, 'code scan')
                if (stageConfig == null || stageConfig.skip)
                {
                    script.githubStatus 'PR Check/Code Scan', 'SUCCESS', 'Code Scan skipped!'
                    return
                }
                else
                {
                    script.githubStatus 'PR Check/Code Scan', 'PENDING', 'Code Scanning....'
                    def codeScanConfig = [:]
                    def githubSourceInfo = utils.getGitSourceInfo()
                    // For Pull request, we only run the sonar code scan as preview mode
                    codeScanConfig.put('sonar.analysis.mode', 'preview')
                    codeScanConfig.put('sonar.github.pullRequest', pullRequestId)
                    codeScanConfig.put('sonar.github.endpoint', githubSourceInfo.endpointUrl)
                    codeScanConfig.put('sonar.github.repository', "${githubSourceInfo.repoOwner}/${githubSourceInfo.repository}")

                    def gitHubToken = configParser.getProperty(config, 'code scan', 'github.credentialsId')
                    gitHubToken = gitHubToken ?: Constants.DEFAULT_GITHUB_TOKEN
                    script.withCredentials([[$class: 'StringBinding', credentialsId: gitHubToken, variable: 'GITHUB_TOKEN']]) {
                        codeScanConfig.put('sonar.github.oauth', env.GITHUB_TOKEN)
                        stageConfig.codeScanConfig = codeScanConfig
                        stageCodeScan(stageConfig)
                    }
                    script.githubStatus 'PR Check/Code Scan', 'SUCCESS', 'Code Scan Succeed'
                }
            } catch (Exception r)
            {
                script.githubStatus 'PR Check/Code Scan', 'FAILURE', 'Code Scan Fail!'
                throw r
            }
        }
    }
}

