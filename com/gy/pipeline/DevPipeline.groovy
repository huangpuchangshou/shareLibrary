package com.gy.pipeline

/**
 * This pipeline is for DEV environment, running on feature* branch.
 * It will checkout source code, do the build, run the unit test and run the system test if required.
 *
 * System Test can be skipped, configured in the JenkinsConfig.yml
 */
class DevPipeline extends Pipeline
{
    /**
     * constructor
     *
     * @param script
     */
    DevPipeline(script)
    {
        super(script, 'dev')
    }

    /**
     * run the detail pipeline steps
     *
     * @param config
     * @return
     */
    def process(config)
    {
        def stageConfig

        //run on configured node
        steps.node(config.node)
        {
            try
            {
                script.githubStatus 'Pipeline/DEV', 'PENDING', 'Running DEV Pipeline...'
                
                //checkout source code
                stageCheckout(config)

                //do the build
                stageConfig = configParser.getStageConfig(config, 'build')
                stageBuild(stageConfig)

                //do the unit test
                stageConfig = configParser.getStageConfig(config, 'unit test')
                stageUnitTest(stageConfig)

                //do the system test
                stageConfig = configParser.getStageConfig(config, 'system test')
                if (stageConfig == null || stageConfig.skip)
                {
                    steps.echo 'System test stage is skipped'
                    script.githubStatus 'Pipeline/DEV', 'SUCCESS', 'System Test skipped. DEV Pipeline Succeed!'
                    return
                }
                else
                {
                    stageSystemTest(stageConfig)
                    script.githubStatus 'Pipeline/DEV', 'SUCCESS', 'DEV Pipeline Succeed!'
                }
            } catch (Exception rethrow)
            {
                script.githubStatus 'Pipeline/DEV', 'FAILURE', 'DEV Pipeline Fail!'
                throw rethrow
            }
        }
    }
}

