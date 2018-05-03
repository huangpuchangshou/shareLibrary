package com.gy.pipeline

import com.gy.utils.YamlParser
import com.gy.utils.Utils
import com.gy.utils.ToolExecutor
import com.gy.utils.Constants
import com.gy.utils.exception.*

/***
 * Base class for all the Pipelines*/
abstract class Pipeline
{
    protected def steps
    protected def env
    protected def script
    protected def currentPipelineName
    protected def configParser = new YamlParser()
    protected def utils = new Utils()
    protected def toolExecutor
    /**
     *
     * @param script The global function script clojure
     */
    Pipeline(script, currentPipelineName)
    {
        this.script = script;
        this.steps = this.script.steps
        this.env = this.script.env
        this.currentPipelineName = currentPipelineName
        toolExecutor = new ToolExecutor(script, steps, env)
    }

    /**
     * Abstract function for all child classes to implement specific pipeline
     * @param config
     * @return
     */
    abstract def process(config)

    /**
     * The entry point for the pipeline to invoke
     * @param config
     * @return
     */
    def execute(config)
    {
        // use the currentPipeline to find the configuration for current Pipeline
        config.put("currentPipelineName", currentPipelineName)
        try
        {
            process(config)
        } catch (PipelineException pipelineException)
        {
            script.mailNotification(config, "FAILURE: Pipeline '${env.JOB_NAME}' (${env.BUILD_NUMBER}) failed at ${pipelineException.getStageName()} stage!",
                    "Your job failed, please review it ${env.BUILD_URL}.\n\n${pipelineException.getMessage()}")
            script.echo pipelineException.getMessage()
            script.currentBuild.result = 'FAILURE'
        } catch (Exception rethrow)
        {
            def failureDetailMsg = failureDetail rethrow
            script.mailNotification(config, "FAILURE: Pipeline '${env.JOB_NAME}' (${env.BUILD_NUMBER}) failed!",
                    "Your job failed, please review it ${env.BUILD_URL}.\n\n${failureDetailMsg}")
            throw rethrow
        }
    }

    protected def failureDetail(exception)
    {
        def w = new StringWriter()
        exception.printStackTrace(new PrintWriter(w))
        return w.toString();
    }

    /**
     * Print out detail stack trace of exception
     * @param exception
     * @return
     */
    def stageCheckout(configForAll)
    {
        def timeOut = configForAll.checkout_timeout ?: Constants.DEFAULT_CHECKOUT_TIMEOUT
        steps.stage('Checkout') {
            // provide another option to control the checkout process in finer level.
            //script.stepCheckout configForAll
            steps.timeout(time: timeOut, unit: 'HOURS') {
                try
                {
                    script.checkout script.scm
                } catch (err)
                {
                    throw err
                }
            }
        }
    }


    def stageBuild(stageConfig)
    {
        def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_BUILD_TIMEOUT
        steps.stage('Build') {
            steps.timeout(time: timeOut, unit: 'SECONDS') {
                toolExecutor.build stageConfig
            }
        }
    }

    def stageBuildWithArtifact(stageConfig)
    {
        def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_BUILD_TIMEOUT
        steps.stage('Build') {
            steps.timeout(time: timeOut, unit: 'SECONDS') {
                toolExecutor.buildWithArtifact stageConfig
            }
        }
        return stageConfig.buildInfo
    }

    def stageUnitTest(stageConfig)
    {
        def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_UNIT_TEST_TIMEOUT
        steps.stage('Unit Test') {
            steps.timeout(time: timeOut, unit: 'SECONDS') {
                toolExecutor.test stageConfig
            }
        }
    }

    def stageSystemTest(stageConfig)
    {
        def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_SYSTEM_TEST_TIMEOUT
        def retryTimes = stageConfig.retry ?: Constants.DEFAULT_SYSTEM_TEST_RETRY
        steps.stage('System Test') {
            steps.timeout(time: timeOut, unit: 'SECONDS') {
                steps.retry(retryTimes) {
                    toolExecutor.systemTest stageConfig
                }
            }
        }
    }

    def stageCodeScan(stageConfig)
    {
        def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_CODE_SCAN_TIMEOUT
        steps.stage('Code Scan') {
            steps.timeout(time: timeOut, unit: 'SECONDS') {
                toolExecutor.codeScan stageConfig
            }
        }
    }

    def stageCodePublish(stageConfig)
    {
        steps.stage('Publish') {
            script.stepArtifactoryPublish stageConfig
        }
    }

    def stageEnvReady(stageConfig)
    {
        def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_APPROVAL_TIMEOUT_IN_HOURS
        steps.stage('Env Ready') {
            steps.timeout(time: timeOut, unit: 'HOURS') {
                try
                {
                    steps.input id: 'envReady', message: "\nIs the UAT environment ready?"
                } catch (err)
                {
                    throw err
                }
            }
        }
    }

    def stageAppDeploy(stageConfig)
    {
        def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_DEPLOY_TIMEOUT
        steps.stage('Deploy') {
            steps.timeout(time: timeOut, unit: 'SECONDS') {
                script.stepDeploy stageConfig
            }
        }
    }

    def stageUATDeploy(stageConfig)
    {
        def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_DEPLOY_TIMEOUT
        steps.stage('Deploy') {
            steps.timeout(time: timeOut, unit: 'SECONDS') {
                script.stepBlueGreenDeploy stageConfig
            }
        }
    }



    def stageBlueGreenDeploy(stageConfig)
    {
        def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_DEPLOY_TIMEOUT
        steps.stage('Stage') {
            steps.timeout(time: timeOut, unit: 'SECONDS') {
                script.stepBlueGreenDeploy stageConfig
            }
        }
    }

    def stageAppSignOff(stageConfig)
    {
        def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_SIGNOFF_TIMEOUT_IN_HOURS
        steps.stage('Sign Off') {
            steps.timeout(time: timeOut, unit: 'HOURS') {
                try
                {
                    steps.input id: 'appSignOff', message: "\nDo you sign off current release?"
                } catch (err)
                {
                    throw err
                }
            }
        }
    }

    def stageSanityTest(stageConfig)
    {
        def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_SANITY_TEST_TIMEOUT
        def retryTimes = stageConfig.retry ?: Constants.DEFAULT_SANITY_TEST_RETRY
        steps.stage('Smoke Test') {
            steps.timeout(time: timeOut, unit: 'SECONDS') {
                steps.retry(retryTimes) {
                    toolExecutor.sanityTest stageConfig
                }
            }
        }
    }

    def stageFunctionalTest(stageConfig)
    {
        def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_FUNCTIONAL_TEST_TIMEOUT
        def retryTimes = stageConfig.retry ?: Constants.DEFAULT_FUNCTIONAL_TEST_RETRY
        steps.stage('Functional Test') {
            steps.timeout(time: timeOut, unit: 'SECONDS') {
                steps.retry(retryTimes) {
                    toolExecutor.functionalTest stageConfig
                }
            }
        }
    }


    def stageRelease(stageConfig)
    {
        steps.stage('Release') {
            script.stepUatPromotion stageConfig
        }
    }

    def stageCodeSync(stageConfig)
    {
        steps.stage('Code Sync') {
            script.stepCodeSync stageConfig
        }
    }

    def stageGoAlive(stageConfig)
    {
        def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_APPROVAL_TIMEOUT_IN_HOURS
        steps.stage('Go Alive') {
            steps.timeout(time: timeOut, unit: 'HOURS') {
                try
                {
                    steps.input id: 'prodGoAlive', message: "\nIs the PROD ready go alive?"
                } catch (err)
                {
                    throw err
                }
            }
        }
    }

    def stageHotfixEnvReady(stageConfig)
    {
        def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_APPROVAL_TIMEOUT_IN_HOURS
        steps.stage('Env Ready') {
            steps.timeout(time: timeOut, unit: 'HOURS') {
                try
                {
                    steps.input id: 'hotfixEnvReady', message: "\nIs Environment ready?"
                } catch (err)
                {
                    throw err
                }
            }
        }
    }

    def stageRegressionlTest(stageConfig)
    {
        def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_REGRESSION_TEST_TIMEOUT
        def retryTimes = stageConfig.retry ?: Constants.DEFAULT_REGRESSION_TEST_RETRY
        steps.stage('Regression Test') {
            steps.timeout(time: timeOut, unit: 'SECONDS') {
                steps.retry(retryTimes) {
                    toolExecutor.regrssionTest stageConfig
                }
            }
        }
    }

    def stageProdSignOff(stageConfig)
    {
        def timeOut = stageConfig.timeOut ?: Constants.DEFAULT_SIGNOFF_TIMEOUT_IN_HOURS
        steps.stage('Go Alive') {
            steps.timeout(time: timeOut, unit: 'HOURS') {
                try
                {
                    steps.input id: 'prodGoAlive', message: "\nDo you want Prod go alive?"
                } catch (err)
                {
                    throw err
                }
            }
        }
    }

    def stageProdGoAlive(stageConfig)
    {
        steps.stage('Production') {
            script.stepBlueGreenGoAlive stageConfig
        }
    }

    def throwPropertyRequiredExcpeiotn(propertyName)
    {
        PipelineException exception = new PropertyRequiredException(this.env.STAGE_NAME, propertyName);
        exception.setStageName(this.env.STAGE_NAME)
        throw exception
    }
}
