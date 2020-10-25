package datadog.lib.datadog.trace.bootstrap.instrumentation.api.ci;

class BuildkiteInfo extends CIProviderInfo {
    public static final String BUILDKITE = "BUILDKITE";
    public static final String BUILDKITE_PROVIDER_NAME = "buildkite";
    public static final String BUILDKITE_PIPELINE_ID = "BUILDKITE_BUILD_ID";
    public static final String BUILDKITE_PIPELINE_NAME = "BUILDKITE_PIPELINE_SLUG";
    public static final String BUILDKITE_PIPELINE_NUMBER = "BUILDKITE_BUILD_NUMBER";
    public static final String BUILDKITE_BUILD_URL = "BUILDKITE_BUILD_URL";
    public static final String BUILDKITE_JOB_ID = "BUILDKITE_JOB_ID";
    public static final String BUILDKITE_WORKSPACE_PATH = "BUILDKITE_BUILD_CHECKOUT_PATH";
    public static final String BUILDKITE_GIT_REPOSITORY_URL = "BUILDKITE_REPO";
    public static final String BUILDKITE_GIT_COMMIT = "BUILDKITE_COMMIT";
    public static final String BUILDKITE_GIT_BRANCH = "BUILDKITE_BRANCH";
    public static final String BUILDKITE_GIT_TAG = "BUILDKITE_TAG";
    private final String ciProviderName = "buildkite";
    private final String ciPipelineId = System.getenv("BUILDKITE_BUILD_ID");
    private final String ciPipelineName = System.getenv("BUILDKITE_PIPELINE_SLUG");
    private final String ciPipelineNumber = System.getenv("BUILDKITE_BUILD_NUMBER");
    private final String ciPipelineUrl = System.getenv("BUILDKITE_BUILD_URL");
    private final String ciJobUrl;
    private final String ciWorkspacePath;
    private final String gitRepositoryUrl;
    private final String gitCommit;
    private final String gitBranch;
    private final String gitTag;

    BuildkiteInfo() {
        this.ciJobUrl = String.format("%s#%s", this.ciPipelineUrl, System.getenv("BUILDKITE_JOB_ID"));
        this.ciWorkspacePath = this.expandTilde(System.getenv("BUILDKITE_BUILD_CHECKOUT_PATH"));
        this.gitRepositoryUrl = this.filterSensitiveInfo(System.getenv("BUILDKITE_REPO"));
        this.gitCommit = System.getenv("BUILDKITE_COMMIT");
        this.gitBranch = this.normalizeRef(System.getenv("BUILDKITE_BRANCH"));
        this.gitTag = this.normalizeRef(System.getenv("BUILDKITE_TAG"));
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
