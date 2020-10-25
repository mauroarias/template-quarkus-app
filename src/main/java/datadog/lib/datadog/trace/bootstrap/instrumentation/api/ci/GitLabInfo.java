package datadog.lib.datadog.trace.bootstrap.instrumentation.api.ci;

class GitLabInfo extends CIProviderInfo {
    public static final String GITLAB = "GITLAB_CI";
    public static final String GITLAB_PROVIDER_NAME = "gitlab";
    public static final String GITLAB_PIPELINE_ID = "CI_PIPELINE_ID";
    public static final String GITLAB_PIPELINE_NAME = "CI_PROJECT_PATH";
    public static final String GITLAB_PIPELINE_NUMBER = "CI_PIPELINE_IID";
    public static final String GITLAB_PIPELINE_URL = "CI_PIPELINE_URL";
    public static final String GITLAB_JOB_URL = "CI_JOB_URL";
    public static final String GITLAB_WORKSPACE_PATH = "CI_PROJECT_DIR";
    public static final String GITLAB_GIT_REPOSITORY_URL = "CI_REPOSITORY_URL";
    public static final String GITLAB_GIT_COMMIT = "CI_COMMIT_SHA";
    public static final String GITLAB_GIT_BRANCH = "CI_COMMIT_BRANCH";
    public static final String GITLAB_GIT_TAG = "CI_COMMIT_TAG";
    private final String ciProviderName = "gitlab";
    private final String ciPipelineId = System.getenv("CI_PIPELINE_ID");
    private final String ciPipelineName = System.getenv("CI_PROJECT_PATH");
    private final String ciPipelineNumber = System.getenv("CI_PIPELINE_IID");
    private final String ciPipelineUrl = this.buildPipelineUrl();
    private final String ciJobUrl = System.getenv("CI_JOB_URL");
    private final String ciWorkspacePath = this.expandTilde(System.getenv("CI_PROJECT_DIR"));
    private final String gitRepositoryUrl = this.filterSensitiveInfo(System.getenv("CI_REPOSITORY_URL"));
    private final String gitCommit = System.getenv("CI_COMMIT_SHA");
    private final String gitBranch = this.normalizeRef(System.getenv("CI_COMMIT_BRANCH"));
    private final String gitTag = this.normalizeRef(System.getenv("CI_COMMIT_TAG"));

    GitLabInfo() {
    }

    private String buildPipelineUrl() {
        String pipelineUrl = System.getenv("CI_PIPELINE_URL");
        return pipelineUrl != null && !pipelineUrl.isEmpty() ? pipelineUrl.replace("/-/pipelines/", "/pipelines/") : null;
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
