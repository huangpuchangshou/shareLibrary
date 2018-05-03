import com.gy.utils.YamlParser
import com.gy.utils.Utils

def call(config)
{
    def yamlParser = new YamlParser()
    def utils = new Utils();

    def awsSshCredentialId = config.deployCredentialMap['aws_ssh_credential_id']
    def awsCredentialId = config.deployCredentialMap['aws_credential_id']
    def stackName = config.deployCredentialMap['stackName']
    echo("awsCredentialId=${awsCredentialId}")
    echo("awsSshCredentialId=${awsSshCredentialId}")
    echo "stackName=${stackName}"

    withCredentials([[$class           : 'AmazonWebServicesCredentialsBinding',
                      credentialsId    : "${awsCredentialId}",
                      accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                      secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
        env.ANSIBLE_HOST_KEY_CHECKING = 'FALSE'
        def stackInfo = utils.descibeStack(stackName)
        utils.swapBetaToLive(stackInfo)

        stackInfo = utils.descibeStack(stackName)
        echo "stackInfo=${stackInfo}"
    }
}
