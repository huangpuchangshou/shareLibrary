import com.gy.pipeline.*
import com.gy.utils.YamlParser
import com.gy.utils.Utils

def call()
{
    def customConfig
    echo "branch name is $env.BRANCH_NAME"
    def branchName = env.BRANCH_NAME ?: ''
    def pipeLine
    def envName = 'DEV'
    def yamlParser = new YamlParser()
    switch (branchName)
    {
        case ~/^(?i)feature.*/:
            pipeLine = new DevPipeline(this)
            envName = 'DEV'
            break
        case ~/^(?i)PR.*/:
            pipeLine = new PullReqPipeline(this)
            envName = 'PR'
            break
        case ~/^(?i)integration.*/:
            pipeLine = new SysPipeline(this)
            envName = 'SYS'

            break
        case ~/^(?i)release.*/:
            pipeLine = new UatPipeline(this)
            envName = 'UAT'

            break

        case ~/^(?i)hotfix.*/:
            pipeLine = new HotfixPipeline(this)
            envName = 'hotfix'
            break
        case 'master':
            envName = 'PROD'
            break
        default:
            echo "no pipeline define for crrent branch ${branchName}"
            break
    }
    node {
        try
        {
            checkout scm
            customConfig = yamlParser.parseYaml 'PipelineConfig.yml'

            if (envName.equals('PROD'))
            {
                def utils = new Utils()
                customConfig.currentPipelineName = 'prod'
                def prodGitServer = yamlParser.getProperty(customConfig, 'code sync', 'prod_git_server')
                def prodGitOrg = yamlParser.getProperty(customConfig, 'code sync', 'prod_git_org')
                def prodGitRepo = yamlParser.getProperty(customConfig, 'code sync', 'prod_git_repo')

                def sourceInfo = utils.getGitSourceInfo()
                echo "sourceInfo=${sourceInfo}, prodGitServer=${prodGitServer},prodGitOrg=${prodGitOrg}, prodGitRepo=${prodGitRepo}"
                if (sourceInfo.endpointUrl.contains(prodGitServer) && sourceInfo.repoOwner.equals(prodGitOrg) && sourceInfo.repository.equals(prodGitRepo))
                {
                    pipeLine = new CleanProdPipeline(this)
                } else
                {
                    pipeLine = new ProdPipeline(this)
                }
            }
        } catch (Exception rethrowEx)
        {
            githubStatus "Pipeline/${envName}", 'FAILURE', "${envName} is failed to parse the pipeline configuration!"
            throw rethrowEx
        }
    }

    echo "config=${customConfig}"

    if (pipeLine)
    {
        pipeLine.execute(customConfig)
    }
}
