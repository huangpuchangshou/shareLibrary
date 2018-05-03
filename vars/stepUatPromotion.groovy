import com.gy.utils.Utils
import com.gy.utils.ArtifactoryUtil
def call(config)
{
    def utils = new Utils()
    //TODO tag the branch ?
    def buildInfo = config.buildInfo
    def artifactoryConfig = config.artifactory ?: config.globalConfig.artifactory
    ArtifactoryUtil artifactoryUtil = new ArtifactoryUtil()
    def server = artifactoryUtil.newArtifactoryServer()

    /*
    def promotionConfig = [
            'buildName'          : buildInfo.name,
            'buildNumber'        : buildInfo.number,
            'targetRepo'         : artifactoryConfig.release_repository,
            'comment'            : 'promote to PROD repo',
            'sourceRepo'         : artifactoryConfig.stage_repository,
            'status'             : 'Released',
            'includeDependencies': false,
            'copy'               : true,
            'failFast'           : true
    ]
    server.promote promotionConfig
   */
    // Promotion with -RC suffix removed using snapshotToRelease plugin in in the
    def resp = utils.promoteBuildInfo(server, buildInfo.name, buildInfo.number, artifactoryConfig.release_repository)

    echo "UAT release response = ${resp}"
    // TODO check the status , might failed because there is no promotion plugin not install in the artifactory server
/*    if (resp.statusCode != 200) {
        currentBuild.result = 'FAILURE'
        new RuntimeException("can't promote the artifacts")
    } */
}
