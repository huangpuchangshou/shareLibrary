package com.gy.utils

import com.cloudbees.groovy.cps.NonCPS
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import jenkins.scm.api.SCMSource;
import jenkins.plugins.git.AbstractGitSCMSource.SCMRevisionImpl
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMRevision
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource

import org.jfrog.hudson.CredentialsConfig
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import groovy.json.JsonSlurper


@NonCPS
def isStepOverride(customConfig, stepName)
{
    return customConfig.metaClass.respondsTo(customConfig, stepName);
}

@NonCPS
def getGitInfo()
{
    def returnMap = [:]
    def run = getCurrentBuild()
    SCMSource src = SCMSource.SourceByItem.findSource(run.getParent());
    if (src != null)
    {
        if (src instanceof GitHubSCMSource)
        {
            returnMap.put("url", ((GitHubSCMSource) src).getRemote())
        }
        SCMRevision revision = SCMRevisionAction.getRevision(src, run)
        if (revision != null)
        {
            if (revision instanceof SCMRevisionImpl)
            {
                returnMap.put("revision", ((SCMRevisionImpl) revision).getHash())
            } else if (revision instanceof PullRequestSCMRevision)
            {
                returnMap.put("revision", ((PullRequestSCMRevision) revision).getPullHash())
            }
        }
    }
    return returnMap;
}

@NonCPS
def getGitSourceInfo()
{
    def returnMap = [:]
    def run = getCurrentBuild()
    SCMSource src = SCMSource.SourceByItem.findSource(run.getParent());
    if (src != null)
    {
        if (src instanceof GitHubSCMSource)
        {
            returnMap.put("endpointUrl", ((GitHubSCMSource) src).getApiUri())
            returnMap.put("repoOwner", ((GitHubSCMSource) src).getRepoOwner())
            returnMap.put("repository", ((GitHubSCMSource) src).getRepository())
            returnMap.put("credentialsId", ((GitHubSCMSource) src).getCredentialsId())
        }
    }
    return returnMap;
}

@NonCPS
def httpGetRequest(String httpUrl, Map requestProperties)
{
    def apiUrl = new URL(httpUrl)
    HttpURLConnection connection
    try
    {
        connection = apiUrl.openConnection()
        if (apiUrl.getUserInfo() != null)
        {
            String basicAuth = "Basic " + new String(new java.util.Base64().getEncoder().encode((byte[]) (apiUrl.getUserInfo().getBytes("UTF-8"))))
            connection.setRequestProperty("Authorization", basicAuth)
        }

        connection.setRequestMethod("GET")
        connection.setDoOutput(true)
        connection.connect()
        // execute the GET request
        def rs = new JsonSlurper().parse(new InputStreamReader(connection.getInputStream(), "UTF-8"))
        return rs
    } finally
    {
        connection.disconnect()
    }
}

@NonCPS
def httpPostRequest(String httpUrl, String postContent)
{
    HttpURLConnection connection
    try
    {
        URL url = new URL(httpUrl);
        connection = (HttpURLConnection) url.openConnection()
        if (url.getUserInfo() != null)
        {
            String basicAuth = "Basic " + new String(new java.util.Base64().getEncoder().encode((byte[]) (url.getUserInfo().getBytes("UTF-8"))))
            connection.setRequestProperty("Authorization", basicAuth)
        }
        connection.setDoOutput(true)
        connection.setRequestMethod("POST")
        OutputStream os = connection.getOutputStream()
        os.write(postContent.getBytes("UTF-8"))
        os.flush()
        def rs = new JsonSlurper().parse(new InputStreamReader(connection.getInputStream(), "UTF-8"))
        return rs
    } finally
    {
        connection.disconnect()
    }
}

def getCodeScanStatus(reportTaskFile)
{
    try
    {
        def props = readProperties file: reportTaskFile
        echo "properties=${props}"
        def sonarServerUrl = props['serverUrl']
        def ceTaskUrl = props['ceTaskUrl']
        def analysisId
        timeout(time: 1, unit: 'MINUTES') {
            waitUntil {
                def response = httpGetRequest(ceTaskUrl, null)
                echo "${response}"
                analysisId = "${response.task.analysisId}"
                return "SUCCESS".equals(response["task"]["status"])
            }
        }
        def sonarStatusApi = "${sonarServerUrl}/api/qualitygates/project_status?analysisId=${analysisId}"
        def analysisResult = httpGetRequest(sonarStatusApi, null)
        echo "analysisResult=${analysisResult}"
        return analysisResult.projectStatus.status
    } catch (Exception ex)
    {
        throw ex
        return 'ERROR'
    }
}

@NonCPS
def getUnitTestStatus()
{
    def build = getCurrentBuild()
    def testStatus = [:]
    if (build.getAction(hudson.tasks.junit.TestResultAction.class) == null)
    {
        testStatus.totalCount = 0
        return testStatus
    }

    testStatus.totalCount = build.getAction(hudson.tasks.junit.TestResultAction.class).getTotalCount();
    testStatus.failedCount = build.getAction(hudson.tasks.junit.TestResultAction.class).getFailCount()
    return testStatus
}

@NonCPS
def getCurrentBuild()
{
    def activeInstance = Jenkins.getActiveInstance()
    def job = (WorkflowJob) activeInstance.getItemByFullName(env.JOB_NAME)
    return job.getBuildByNumber(Integer.parseInt(env.BUILD_NUMBER))
}


def getProjectVersion()
{
    def file = readFile('pom.xml')
    def project = new XmlSlurper().parseText(file)
    return project.version.text()
}

@NonCPS
def promoteBuildInfo(server, buildName, buildNumber, targeRepository)
{
    def artifactoryServer = org.jfrog.hudson.pipeline.Utils.prepareArtifactoryServer(null, server)
    def build = getCurrentBuild()
    CredentialsConfig deployerConfig = server.createCredentialsConfig()
    ArtifactoryBuildInfoClient client = artifactoryServer.createArtifactoryClient(deployerConfig.provideUsername(build.getParent()),
            deployerConfig.providePassword(build.getParent()), null)
    def postParams = [:]
    postParams.snapExp = 'RC'
    postParams.targetRepository = targeRepository
    def promotionName = Constants.ARTIFACTORY_PROMOTION_PLUG
    def resturnMap = [:]
    try
    {
        HttpResponse response = client.executePromotionUserPlugin(promotionName, buildName, buildNumber, postParams)
        StatusLine status = response.getStatusLine()
        resturnMap.statusCode = status.statusCode
        HttpEntity entity = response.getEntity()
        InputStream is = entity.getContent()
        resturnMap.content = IOUtils.toString(is, "UTF-8")
    } catch (IOException e)
    {
        resturnMap.statusCode = 400
        throw e
    }
    return resturnMap
}

def descibeStack(stackName)
{
    def outputs = sh(returnStdout: true, script: "aws cloudformation describe-stacks --stack-name ${stackName}  --region us-east-1")
    def jsonOut = readJSON(text: outputs)
    // echo "jsonOut=${jsonOut}"
    def loadBalancerId = jsonOut.Stacks[0].Outputs.find { it.OutputKey.equals 'ApplicationLoadBalancer' }['OutputValue']
    // echo "loadBalancerId=${loadBalancerId}"

    def asgOne = jsonOut.Stacks[0].Outputs.find { it.OutputKey.equals 'WebServerGroup' }['OutputValue']
    def asgTwo = jsonOut.Stacks[0].Outputs.find { it.OutputKey.equals 'GreenWebServerGroup' }['OutputValue']
    echo "asgOne=${asgOne}"
    echo "asgTwo=${asgTwo}"

    outputs = sh(returnStdout: true, script: "aws elbv2 describe-listeners --region us-east-1 --load-balancer-arn ${loadBalancerId}")
    jsonOut = readJSON(text: outputs)
    // echo "jsonOut=${jsonOut}"

    def liveListenerArn = jsonOut.Listeners.find { it.Port.equals 80 }['ListenerArn']
    def betaListenerArn = jsonOut.Listeners.find { it.Port.equals 8080 }['ListenerArn']

    outputs = sh(returnStdout: true, script: " aws elbv2 describe-rules --region us-east-1 --listener-arn ${liveListenerArn}")
    jsonOut = readJSON(text: outputs)
    // echo "jsonOut=${jsonOut}"
    def liveRuleArn = jsonOut.Rules.find { it.Priority.equals '1' }['RuleArn']
    def liveTargetGroupArn = jsonOut.Rules.find { it.Priority.equals '1' }['Actions'][0].TargetGroupArn

    outputs = sh(returnStdout: true, script: " aws elbv2 describe-rules --region us-east-1 --listener-arn ${betaListenerArn}")
    jsonOut = readJSON(text: outputs)
    //echo "jsonOut=${jsonOut}"
    def betaRuleArn = jsonOut.Rules.find { it.Priority.equals '1' }['RuleArn']
    def betaTargetGroupArn = jsonOut.Rules.find { it.Priority.equals '1' }['Actions'][0].TargetGroupArn

    outputs = sh(returnStdout: true, script: " aws elbv2 describe-tags --region us-east-1 --resource-arn ${betaTargetGroupArn}")
    jsonOut = readJSON(text: outputs)
    def newReleaseVersion = jsonOut.TagDescriptions[0].Tags.find { it.Key.equals 'ReleaseVersion' }['Value']
    def betaIdentifier = jsonOut.TagDescriptions[0].Tags.find { it.Key.equals 'Identifier' }['Value']

    outputs = sh(returnStdout: true, script: " aws elbv2 describe-tags --region us-east-1 --resource-arn ${liveTargetGroupArn}")
    jsonOut = readJSON(text: outputs)
    def releaseVersion = jsonOut.TagDescriptions[0].Tags.find { it.Key.equals 'ReleaseVersion' }['Value']
    def liveIdentifier = jsonOut.TagDescriptions[0].Tags.find { it.Key.equals 'Identifier' }['Value']


    def result = [:]
    result['liveRuleArn'] = liveRuleArn
    result['liveTargetGroupArn'] = liveTargetGroupArn
    result['liveReleaseVersion'] = releaseVersion
    result['liveIdentifier'] = liveIdentifier

    result['betaRuleArn'] = betaRuleArn
    result['betaTargetGroupArn'] = betaTargetGroupArn
    result['betaReleaseVersion'] = newReleaseVersion
    result['betaIdentifier'] = betaIdentifier

    outputs = sh(returnStdout: true, script: " aws autoscaling describe-auto-scaling-groups --region us-east-1 --auto-scaling-group-names '${asgOne}'")
    jsonOut = readJSON(text: outputs)
    echo "targetGroups=${jsonOut.AutoScalingGroups[0].TargetGroupARNs[0]}"



    if (liveTargetGroupArn.equals(jsonOut.AutoScalingGroups[0].TargetGroupARNs[0]))
    {
        result['liveASG'] = asgOne
        result['betaASG'] = asgTwo
    } else if (betaTargetGroupArn.equals(jsonOut.AutoScalingGroups[0].TargetGroupARNs[0]))
    {
        result['betaASG'] = asgOne
        result['liveASG'] = asgTwo
    }
    return result
}

def bringUpBeta(stackInfo)
{
    // use live to get launch configuration name , and all the instance with same configuration name and 
    
    equals InService
    long timeOut = 600 * 1000
    def outputs = sh(returnStdout: true, script: " aws autoscaling describe-auto-scaling-groups --region us-east-1 --auto-scaling-group-names ${stackInfo.betaASG}")
    def jsonOut = readJSON(text: outputs)
    def launchConfiguration = jsonOut.AutoScalingGroups[0].LaunchConfigurationName
    echo "LatestLaunchConfigurationName=${launchConfiguration}"
    def minSize = jsonOut.AutoScalingGroups[0].MinSize
    def desiredCapacity = jsonOut.AutoScalingGroups[0].DesiredCapacity
    def maxSize = jsonOut.AutoScalingGroups[0].MaxSize
    def asgStable = false
    def existingInstances = [] as Set<String>
    instances = jsonOut.AutoScalingGroups[0].Instances
    for (i in instances)
    {
        existingInstances.add(i.InstanceId)
    }
    // scale up
    sh(script: "aws autoscaling  update-auto-scaling-group --min-size ${minSize * 2} --max-size ${maxSize * 2}  " + "--desired-capacity ${desiredCapacity * 2} --region us-east-1 --auto-scaling-group-name='${stackInfo.betaASG}'")
    System.sleep(30000)
    sh(script: "aws autoscaling  update-auto-scaling-group --min-size ${minSize} --max-size ${maxSize}  " + "--desired-capacity ${desiredCapacity} --region us-east-1 --auto-scaling-group-name='${stackInfo.betaASG}'")

    long start = System.currentTimeMillis()
    while (true)
    {
        outputs = sh(returnStdout: true, script: " aws autoscaling describe-auto-scaling-groups --region us-east-1 --auto-scaling-group-names ${stackInfo.betaASG}")
        jsonOut = readJSON(text: outputs)
        instances = jsonOut.AutoScalingGroups[0].Instances
        int length = instances.size()
        boolean notStable = false
        for (i in instances)
        {
            if (existingInstances.contains(i.InstanceId))
            {
                notStable = true
                echo "Old Instance=[InstanceId=${i.InstanceId},LifecycleState=${i.LifecycleState}] exists]"
            } else if (!i.LifecycleState.equals('InService'))
            {
                echo "Instance=[InstanceId=${i.InstanceId},LifecycleState=${i.LifecycleState}] doesn't working in service]"
                notStable = true
            }
        }

        long duration = System.currentTimeMillis() - start
        if (!notStable || duration >= timeOut)
        {
            break;
        }
        System.sleep(30000)
    }
}


def swapBetaToLive(stackInfo)
{
    sh(script: "aws elbv2 modify-rule --region us-east-1 --rule-arn ${stackInfo.betaRuleArn} --actions Type='forward',TargetGroupArn='${stackInfo.liveTargetGroupArn}'")
    sh(script: "aws elbv2 modify-rule --region us-east-1 --rule-arn ${stackInfo.liveRuleArn} --actions Type='forward',TargetGroupArn='${stackInfo.betaTargetGroupArn}'")

    sh(script: "aws elbv2 add-tags --region us-east-1 --resource-arns ${stackInfo.liveTargetGroupArn} --tags Key='IsProduction',Value='False'")
    sh(script: "aws elbv2 add-tags --region us-east-1 --resource-arns ${stackInfo.betaTargetGroupArn} --tags Key='IsProduction',Value='True'")
}

return this



