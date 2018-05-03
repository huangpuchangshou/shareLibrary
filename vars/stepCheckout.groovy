// This step checks out code along with any clone parameters passed in
def call(config)
{
    clone_options = config.clone_options ?: [:]

    checkout([$class           : 'GitSCM',
              branches         : scm.branches,
              userRemoteConfigs: scm.userRemoteConfigs,
              extensions       : scm.extensions + [[$class     : 'LocalBranch',
                                                    localBranch: "${env.BRANCH_NAME}"]] + [[$class   : 'CloneOption',
                                                                                            reference: clone_options.reference ?: '',
                                                                                            depth    : clone_options.depth ?: 0,
                                                                                            shallow  : clone_options.shallow ?: false,
                                                                                            noTags   : clone_options.noTags ?: false]]])
}
