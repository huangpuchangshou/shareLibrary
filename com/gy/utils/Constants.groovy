package com.gy.utils

/**
 * Define the default values which will be used in the pipelines
 */
public class Constants {
    public static DEFAULT_CHECKOUT_TIMEOUT          = 1800
    public static DEFAULT_BUILD_TIMEOUT             = 3600
    public static DEFAULT_UNIT_TEST_TIMEOUT         = 3600
    public static DEFAULT_SYSTEM_TEST_TIMEOUT       = 3600
    public static DEFAULT_CODE_SCAN_TIMEOUT         = 3600
    public static DEFAULT_DEPLOY_TIMEOUT            = 3600
    public static DEFAULT_SANITY_TEST_TIMEOUT       = 3600
    public static DEFAULT_SANITY_TEST_RETRY         = 3
    public static DEFAULT_FUNCTIONAL_TEST_TIMEOUT   = 3600
    public static DEFAULT_FUNCTIONAL_TEST_RETRY     = 3
    public static DEFAULT_REGRESSION_TEST_TIMEOUT   = 3600
    public static DEFAULT_REGRESSION_TEST_RETRY     = 3

    public static DEFAULT_SYSTEM_TEST_RETRY         = 3
    public static DEFAULT_APPROVAL_TIMEOUT_IN_HOURS = 24
    public static DEFAULT_SIGNOFF_TIMEOUT_IN_HOURS  = 24

    public static DEFAULT_GITHUB_TOKEN = 'GITHUB_TOKEN'
    public static DEFAULT_ARTIFACTORY_SERVER = 'DEFAULT_ARTIFACTORY'
    public static DEFAULT_ARTIFACTORY_PROD_SERVER = 'DEFAULT_ARTIFACTORY_PROD'

    public static DEFAULT_PROD_GITHUB_SERVER = 'DEFAULT_PROD_GITHUB_SERVER'

    public static ARTIFACTORY_PROMOTION_PLUG = 'snapshotToRelease'

    public static SELF_SERVICE_SERVER_URL = 'http://10.56.248.107:8888'
}
