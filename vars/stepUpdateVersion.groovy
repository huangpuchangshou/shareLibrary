import com.gy.utils.Utils

def call(config)
{
    def utils = new Utils()
    /* get the revsion of current branch
     get the version of the POM.xml with SNAPSHOT
     get the revison of the branch by: git rev-list --count --first-parent HEAD
    */
    def tempVersion = utils.getProjectVersion()
    echo "current SnapshotVersion=${tempVersion}"
    def revision = steps.sh(returnStdout: true, script: 'git rev-list --count --first-parent HEAD').trim()
    def releaseVersion = tempVersion.replace('SNAPSHOT', 'RC' + revision)
    echo "releaseVersion=${releaseVersion}"
    def descriptor = Artifactory.mavenDescriptor()
    descriptor.version = releaseVersion
    descriptor.failOnSnapshot = true
    descriptor.transform()

    return releaseVersion
}
