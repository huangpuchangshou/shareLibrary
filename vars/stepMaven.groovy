def call(config)
{
    mavenSettings = config.maven_settings
    mavenRepository = config.maven_repository
    mavenVersion = config.maven_version ?: 'M3'
    withMaven(
            // Maven installation declared in the Jenkins "Global Tool Configuration"
            maven: mavenVersion,
            // Maven settings.xml file defined with the Jenkins Config File Provider Plugin
            // Maven settings and global settings can also be defined in Jenkins Global Tools Configuration
            mavenSettingsConfig: mavenSettings,
            mavenLocalRepo: mavenRepository) {
        // maven build
        sh config.maven_script
    }
}
