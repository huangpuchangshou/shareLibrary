import com.ssc.dpe.utils.ArtifactoryUtil
def call(config)
{
    ArtifactoryUtil artifactoryUtil = new ArtifactoryUtil()
    def server = artifactoryUtil.newArtifactoryServer()
    if (config.buildInfo)
    {
        server.publishBuildInfo config.buildInfo

        for (a in config.buildInfo.deployedArtifacts)
        {
            echo "artifiact = ${a.name}"
        }
    } else
    {
        // should think about other possibility of
        def uploadSpec = """{
                            "files": [
                                    "pattern": "${config.publish_artifact_pattern}",
                                    "target":  "${config.publish_artifact_repository}"
                                }
                             ]
                        }"""
        server.upload(uploadSpec)
    }
}
