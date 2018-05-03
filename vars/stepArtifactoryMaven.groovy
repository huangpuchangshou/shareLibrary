import com.gy.utils.ArtifactoryUtil

def call(config)
{
    def mavenConfig = config.maven_properties
    def artifactoryConfig = config.artifactory ?: config.globalConfig.artifactory
    
    ArtifactoryUtil artifactoryUtil = new ArtifactoryUtil()
    def server = artifactoryUtil.newArtifactoryServer()
    def rtMaven = Artifactory.newMavenBuild()
    rtMaven.tool = mavenConfig.maven_version ?: 'M3'
    
    // Set Artifactory repositories for dependencies resolution and artifacts deployment.
    rtMaven.deployer releaseRepo: artifactoryConfig.stage_repository, snapshotRepo: artifactoryConfig.snapshot_repository, server: server
    rtMaven.resolver releaseRepo:artifactoryConfig.resolve_release_repository, snapshotRepo:artifactoryConfig.resolve_snapshot_repository, server: server

    def maven_script = mavenConfig.maven_script
    if (maven_script && maven_script.trim().startsWith('mvn')) {
        maven_script = maven_script.trim() - 'mvn'
    }
    
    buildInfo = rtMaven.run pom: 'pom.xml', goals: maven_script
    // we send back the build info
    config.buildInfo = buildInfo
}
