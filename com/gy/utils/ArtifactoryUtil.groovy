package com.gy.utils

def getArtifacotryUrl() {
    def server = Artifactory.server "DEFAULT_ARTIFACTORY"
    return server.url
}

def getArtifacotryUrl(serverId) {
    def server = Artifactory.server serverId
    return server.url
}


def getArtifactoryUser() {
    Utils util = new Utils()
    def selfServiceTokenUrl = "${Constants.SELF_SERVICE_SERVER_URL}/jfrog/token"
    def gitSourceInfo = util.getGitSourceInfo()
    def artifactoryUser = util.httpPostRequest(selfServiceTokenUrl, 'org=' + gitSourceInfo.repoOwner + '&repo=' + gitSourceInfo.repository )
    return artifactoryUser
}

def newArtifactoryServer() {
    def server
    server = Artifactory.server "DEFAULT_ARTIFACTORY"
    // comment next line to switch to user with token
    if (server != null) { return server }
    def artiUserToken = getArtifactoryUser();
    echo "artiUserToken=${artiUserToken}"
    def serverUrl = getArtifacotryUrl()
    echo "serverUrl=${serverUrl}"
    server = Artifactory.newServer  url: serverUrl,  username: artiUserToken.username, password: artiUserToken.token
    return server
}
return this
