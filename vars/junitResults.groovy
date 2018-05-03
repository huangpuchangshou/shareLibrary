def call(config)
{
    def archiveTestResults = config.archiveTestResults ?: true
    if (archiveTestResults)
    {
        try
        {
            def surefire = findFiles(glob: '**/surefire-reports/*.xml')
            if (surefire)
            {
                step([$class: 'JUnitResultArchiver', testResults: '**/surefire-reports/*.xml', healthScaleFactor: 1.0])
            }

            def failsafe = findFiles(glob: '**/failsafe-reports/*.xml')
            if (failsafe)
            {
                step([$class: 'JUnitResultArchiver', testResults: '**/failsafe-reports/*.xml', healthScaleFactor: 1.0])
            }
        } catch (err)
        {
            echo "Failed to find test results "
        }

    }
}
