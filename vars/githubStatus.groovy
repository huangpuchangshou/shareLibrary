import com.gy.utils.Utils

def call(context, state, statusDescription)
{
    Utils utils = new Utils();
    def gitInfo = utils.getGitInfo();

    repoUrl = gitInfo.get("url")
    commitSha = gitInfo.get("revision")
    step([$class            : 'GitHubCommitStatusSetter',
          reposSource       : [$class: "ManuallyEnteredRepositorySource", url: repoUrl],
          commitShaSource   : [$class: "ManuallyEnteredShaSource", sha: commitSha],
          contextSource     : [$class: 'ManuallyEnteredCommitContextSource', context: context],
          errorHandlers     : [[$class: 'ShallowAnyErrorHandler']],
          statusResultSource: [$class : 'ConditionalStatusResultSource',
                               results: [[$class: 'AnyBuildResult', state: state, message: statusDescription]]]])
}
