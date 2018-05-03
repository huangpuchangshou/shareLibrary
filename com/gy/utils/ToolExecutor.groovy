package com.gy.utils
import com.gy.utils.exception.*
public class ToolExecutor
{
    protected def steps
    protected def env
    protected def script

    public ToolExecutor(script, steps, env)
    {
        this.script = script
        this.steps = steps
        this.env = env
    }

    def buildWithArtifact(stageConfig)
    {
        def beforeScript = stageConfig.before_script
        if (beforeScript != null)
        {
            runScript(stageConfig, 'before_script')
        }
        def tool = stageConfig.tool
        switch (tool)
        {
            case 'mvn':
                script.stepArtifactoryMaven stageConfig
                break;
        }
    }


    def build(stageConfig)
    {
        def beforeScript = stageConfig.before_script
        if (beforeScript != null)
        {
            runScript(stageConfig, 'before_script')
        }
        def tool = stageConfig.tool
        switch (tool)
        {
            case 'mvn':
                def mavenConfig = stageConfig.maven_properties
                script.stepMaven mavenConfig
                break;
            case 'shell':
                runScript(stageConfig, 'script')
                break;
            default:
                runScript(stageConfig, 'script')
                break;
        }
    }

    def test(stageConfig)
    {
        def tool = stageConfig.tool
        switch (tool)
        {
            case 'mvn':
                def mavenConfig = stageConfig.maven_properties
                script.stepMaven mavenConfig
                break;
            case 'shell':
                runScript(stageConfig, 'script')
                break;
            default:
                runScript(stageConfig, 'script')
                break;
        }
    }

    def systemTest(stageConfig)
    {
        def tool = stageConfig.tool
        switch (tool)
        {
            case 'mvn':
                def mavenConfig = stageConfig.maven_properties
                script.stepMaven mavenConfig
                break;
            case 'shell':
                runScript(stageConfig, 'script')
                break;
            default:
                runScript(stageConfig, 'script')
                break;
        }
    }

    def automationTest(stageConfig)
    {
        def tool = stageConfig.tool
        switch (tool)
        {
            case 'mvn':
                def mavenConfig = stageConfig.maven_properties
                script.stepMaven mavenConfig
                break;
            case 'shell':
                runScript(stageConfig, 'script')
                break;
            default:
                runScript(stageConfig, 'script')
                break;
        }
    }

    def sanityTest(stageConfig)
    {
        def tool = stageConfig.tool
        switch (tool)
        {
            case 'mvn':
                def mavenConfig = stageConfig.maven_properties
                script.stepMaven mavenConfig
                break;
            case 'shell':
                runScript(stageConfig, 'script')
                break;
            default:
                runScript(stageConfig, 'script')
                break;
        }
    }

    def functionalTest(stageConfig)
    {
        def tool = stageConfig.tool
        switch (tool)
        {
            case 'mvn':
                def mavenConfig = stageConfig.maven_properties
                script.stepMaven mavenConfig
                break;
            case 'shell':
                runScript(stageConfig, 'script')
                break;
            default:
                runScript(stageConfig, 'script')
                break;
        }
    }


    def regrssionTest(stageConfig)
    {
        def tool = stageConfig.tool
        switch (tool)
        {
            case 'mvn':
                def mavenConfig = stageConfig.maven_properties
                script.stepMaven mavenConfig
                break;
            case 'shell':
                runScript(stageConfig, 'script')
                break;
            default:
                runScript(stageConfig, 'script')
                break;
        }
    }

    def codeScan(stageConfig)
    {
        def tool = stageConfig.tool
        switch (tool)
        {
            case 'mvn':
                def mavenConfig = stageConfig.maven_properties
                if (stageConfig.codeScanConfig)
                {
                    def temp = ''
                    for (e in stageConfig.codeScanConfig)
                    {
                        temp = " ${temp} -D${e.key}=${e.value}"
                    }
                    mavenConfig.maven_script = "${mavenConfig.maven_script} ${temp}";
                }
                script.echo "maven_script= ${mavenConfig.maven_script}"
                script.stepMaven mavenConfig
                break;
            case 'shell':
                runScript(stageConfig, 'script')
                break;
            default:
                runScript(stageConfig, 'script')
                break;
        }
    }

    def runScript(stageConfig, propertyName) {
        if (stageConfig[propertyName] == null || stageConfig[propertyName].isAllWhitespace()) {
           PipelineException exception =  new PropertyRequiredException(env.STAGE_NAME, propertyName);
           exception.setStageName(env.STAGE_NAME)
           throw exception
        }
        script.sh stageConfig[propertyName]
    }
}
