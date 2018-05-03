package com.gy.pipeline

class ProdPipeline extends Pipeline
{
    ProdPipeline(script)
    {
        super(script, 'prod')
    }

    def process(config)
    {
        steps.node(config.node) {
            try
            {
                def stageConfig
                stageCheckout(config)

                stageConfig = configParser.getStageConfig(config, 'code sync')
                stageCodeSync(stageConfig)

                script.githubStatus 'Pipeline/PROD', 'SUCCESS', 'Pipeline PROD is done successfully'
            } catch (Exception rethrow)
            {
                script.githubStatus 'Pipeline/PROD', 'FAILURE', 'Pipeline PROD is failed'
                throw rethrow
            }
        }
    }
}
