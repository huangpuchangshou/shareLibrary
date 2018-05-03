import com.gy.utils.YamlParser
import com.gy.dpe.utils.Utils

def call(config)
{
    def yamlParser = new YamlParser()
    def utils = new Utils();

    def awsCredentialId = config.aws_credential_id
    if (awsCredentialId == null)
    {
        awsCredentialId = yamlParser.getPropertyFromGlobalConfig(config, 'aws_credential_id')
    }
    echo("awsCredentialId=${awsCredentialId}")

    def awsSshCredentialId = config.aws_ssh_credential_id
    if (awsSshCredentialId == null)
    {
        awsSshCredentialId = yamlParser.getPropertyFromGlobalConfig(config, 'aws_ssh_credential_id')
    }
    echo("awsSshCredentialId=${awsSshCredentialId}")

    def ansibleDeployScript = config.ansible_script
    echo "ansibleDeployScript=${ansibleDeployScript}"

    echo "config.extra_args=${config.extra_args}"

    def releaseVersion = config.extra_args.releaseVersion
    def artifactoryServer = config.extra_args.artifactoryServer

    def versionMap = [:]
    versionMap["Code1"] = releaseVersion
    versionMap["Code2"] = releaseVersion

    def stackName = config.stackName
    config.deployCredentialMap['aws_credential_id'] = awsCredentialId
    config.deployCredentialMap['aws_ssh_credential_id'] = awsSshCredentialId
    config.deployCredentialMap['stackName'] = stackName

    echo "stackName=${stackName}"
    withCredentials([[$class           : 'AmazonWebServicesCredentialsBinding',
                      credentialsId    : "${awsCredentialId}",
                      accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                      secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
        env.ANSIBLE_HOST_KEY_CHECKING = 'FALSE'

        boolean update = false;
        def stackInfo
        try
        {
            stackInfo = utils.descibeStack(stackName)
            echo "stackInfo=${stackInfo}"
            versionMap[stackInfo.betaIdentifier]= releaseVersion
            versionMap[stackInfo.liveIdentifier]= stackInfo.liveReleaseVersion
            update = true

        } catch (Exception ex)
        {
            echo "stack is not existing"
        }

        ansiblePlaybook(playbook: "${ansibleDeployScript}",
                credentialsId: "${awsSshCredentialId}",
                extraVars: ["ArtifacotryServer":"${artifactoryServer}", "StackName": "${stackName}", "ReleaseVersion": "${versionMap.Code1}", "NewReleaseVersion": "${versionMap.Code2}"])
        if (update) {
            stackInfo = utils.descibeStack(stackName)
            utils.bringUpBeta(stackInfo)
            echo "stackInfo=${stackInfo}"
        }
    }
}
