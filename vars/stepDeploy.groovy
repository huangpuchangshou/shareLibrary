import com.gy.utils.YamlParser

def call(config)
{
    def yamlParser = new YamlParser()

    def awsCredentialId = config.aws_credential_id
    echo("awsCredentialId=${awsCredentialId}")
    if (awsCredentialId == null)
    {
        awsCredentialId = yamlParser.getPropertyFromGlobalConfig(config, 'aws_credential_id')
    }

    def awsSshCredentialId = config.aws_ssh_credential_id
    echo("awsSshCredentialId=${awsSshCredentialId}")
    if (awsSshCredentialId == null)
    {
        awsSshCredentialId = yamlParser.getPropertyFromGlobalConfig(config, 'aws_ssh_credential_id')
    }
    def ansibleDeployScript = config.ansible_script
    echo "config.extra_args=${config.extra_args}"
    withCredentials([[$class           : 'AmazonWebServicesCredentialsBinding',
                      credentialsId    : "${awsCredentialId}",
                      accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                      secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
        env.ANSIBLE_HOST_KEY_CHECKING = 'FALSE'

        ansiblePlaybook(playbook: "${ansibleDeployScript}",
                credentialsId: "${awsSshCredentialId}",
                extraVars: config.extra_args)
    }
}
