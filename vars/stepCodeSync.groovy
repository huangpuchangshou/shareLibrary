import com.gy.utils.Utils
import com.gy.utils.Constants

def call(stageConfig)
{
    def utils = new Utils()

    def prodGitServer = stageConfig.prod_git_server
    def prodGitOrg = stageConfig.prod_git_org
    def prodGitRepo = stageConfig.prod_git_repo
    def prodGitCredentials = stageConfig.prod_git_credentials

    if (prodGitServer == null || prodGitOrg == null || prodGitRepo == null || prodGitCredentials == null) {
        echo 'Please configure the PROD environment git information with prod_git_server, prod_git_org, prod_git_repo and prod_git_credentials'
    }

    def githubSourceInfo = utils.getGitSourceInfo()
    withCredentials([usernamePassword(credentialsId: githubSourceInfo.credentialsId, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
        def orginUrl = githubSourceInfo.endpointUrl
        orginUrl = orginUrl.replace('://', "://${GIT_USERNAME}:${GIT_PASSWORD}@")
        orginUrl = orginUrl.replace('/api/v3', "/${githubSourceInfo.repoOwner}/${githubSourceInfo.repository}.git")

        sh("git remote set-url origin ${orginUrl}")
        sh('git checkout master')
        sh('git pull')
    }
    def sync = sh(returnStdout: true, script: 'git remote')
    withCredentials([usernamePassword(credentialsId: prodGitCredentials, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
        echo "sync repository = ${sync}"
        if (!sync.contains('sync')) {
            echo "create new sync remote"
            sh("git remote add sync https://${GIT_USERNAME}:${GIT_PASSWORD}@${prodGitServer}/${prodGitOrg}/${prodGitRepo}.git")
        } else {
            sh("git remote remove sync")
            sh("git remote add sync https://${GIT_USERNAME}:${GIT_PASSWORD}@${prodGitServer}/${prodGitOrg}/${prodGitRepo}.git")
            echo "remove and recreate sync remote"
        }
        //TODO need think how to keep two side not conflict
        //sh("git pull sync master --allow-unrelated-histories")
        sh("git fetch origin")
        sh('git push sync master')
    }
}
