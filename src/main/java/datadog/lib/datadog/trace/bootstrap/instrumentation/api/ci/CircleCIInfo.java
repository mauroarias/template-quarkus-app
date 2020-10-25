package datadog.lib.datadog.trace.bootstrap.instrumentation.api.ci;

class CircleCIInfo extends CIProviderInfo {
    public static final String CIRCLECI = "CIRCLECI";
    public static final String CIRCLECI_PROVIDER_NAME = "circleci";
    public static final String CIRCLECI_PIPELINE_ID = "CIRCLE_WORKFLOW_ID";
    public static final String CIRCLECI_PIPELINE_NAME = "CIRCLE_PROJECT_REPONAME";
    public static final String CIRCLECI_PIPELINE_NUMBER = "CIRCLE_BUILD_NUM";
    public static final String CIRCLECI_BUILD_URL = "CIRCLE_BUILD_URL";
    public static final String CIRCLECI_WORKSPACE_PATH = "CIRCLE_WORKING_DIRECTORY";
    public static final String CIRCLECI_GIT_REPOSITORY_URL = "CIRCLE_REPOSITORY_URL";
    public static final String CIRCLECI_GIT_COMMIT = "CIRCLE_SHA1";
    public static final String CIRCLECI_GIT_BRANCH = "CIRCLE_BRANCH";
    public static final String CIRCLECI_GIT_TAG = "CIRCLE_TAG";
    private final String ciProviderName = "circleci";
    private final String ciPipelineId = System.getenv("CIRCLE_WORKFLOW_ID");
    private final String ciPipelineName = System.getenv("CIRCLE_PROJECT_REPONAME");
    private final String ciPipelineNumber = System.getenv("CIRCLE_BUILD_NUM");
    private final String ciPipelineUrl = System.getenv("CIRCLE_BUILD_URL");
    private final String ciJobUrl = System.getenv("CIRCLE_BUILD_URL");
    private final String ciWorkspacePath = this.expandTilde(System.getenv("CIRCLE_WORKING_DIRECTORY"));
    private final String gitRepositoryUrl = this.filterSensitiveInfo(System.getenv("CIRCLE_REPOSITORY_URL"));
    private final String gitCommit = System.getenv("CIRCLE_SHA1");
    private final String gitBranch;
    private final String gitTag = this.normalizeRef(System.getenv("CIRCLE_TAG"));

    CircleCIInfo() {
        this.gitBranch = this.buildGitBranch(this.gitTag);
    }

    private String buildGitBranch(String gitTag) {
        return gitTag != null ? null : this.normalizeRef(System.getenv("CIRCLE_BRANCH"));
    }

    public String getCiProviderName() {
        return this.ciProviderName;
    }

    public String getCiPipelineId() {
        return this.ciPipelineId;
    }

    public String getCiPipelineName() {
        return this.ciPipelineName;
    }

    public String getCiPipelineNumber() {
        return this.ciPipelineNumber;
    }

    public String getCiPipelineUrl() {
        return this.ciPipelineUrl;
    }

    public String getCiJobUrl() {
        return this.ciJobUrl;
    }

    public String getCiWorkspacePath() {
        return this.ciWorkspacePath;
    }

    public String getGitRepositoryUrl() {
        return this.gitRepositoryUrl;
    }

    public String getGitCommit() {
        return this.gitCommit;
    }

    public String getGitBranch() {
        return this.gitBranch;
    }

    public String getGitTag() {
        return this.gitTag;
    }
}
