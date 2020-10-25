package datadog.lib.datadog.trace.bootstrap.instrumentation.api.ci;

class BitBucketInfo extends CIProviderInfo {
    public static final String BITBUCKET = "BITBUCKET_COMMIT";
    public static final String BITBUCKET_PROVIDER_NAME = "bitbucket";
    public static final String BITBUCKET_PIPELINE_ID = "BITBUCKET_PIPELINE_UUID";
    public static final String BITBUCKET_REPO_FULL_NAME = "BITBUCKET_REPO_FULL_NAME";
    public static final String BITBUCKET_BUILD_NUMBER = "BITBUCKET_BUILD_NUMBER";
    public static final String BITBUCKET_WORKSPACE_PATH = "BITBUCKET_CLONE_DIR";
    public static final String BITBUCKET_GIT_REPOSITORY_URL = "BITBUCKET_GIT_SSH_ORIGIN";
    public static final String BITBUCKET_GIT_COMMIT = "BITBUCKET_COMMIT";
    public static final String BITBUCKET_GIT_BRANCH = "BITBUCKET_BRANCH";
    public static final String BITBUCKET_GIT_TAG = "BITBUCKET_TAG";
    private final String ciProviderName;
    private final String ciPipelineId;
    private final String ciPipelineName;
    private final String ciPipelineNumber;
    private final String ciPipelineUrl;
    private final String ciJobUrl;
    private final String ciWorkspacePath;
    private final String gitRepositoryUrl;
    private final String gitCommit;
    private final String gitBranch;
    private final String gitTag;

    BitBucketInfo() {
        String repo = System.getenv("BITBUCKET_REPO_FULL_NAME");
        String number = System.getenv("BITBUCKET_BUILD_NUMBER");
        String url = this.buildPipelineUrl(repo, number);
        this.ciProviderName = "bitbucket";
        this.ciPipelineId = this.buildPipelineId();
        this.ciPipelineName = repo;
        this.ciPipelineNumber = number;
        this.ciPipelineUrl = url;
        this.ciJobUrl = url;
        this.ciWorkspacePath = this.expandTilde(System.getenv("BITBUCKET_CLONE_DIR"));
        this.gitRepositoryUrl = this.filterSensitiveInfo(System.getenv("BITBUCKET_GIT_SSH_ORIGIN"));
        this.gitCommit = System.getenv("BITBUCKET_COMMIT");
        this.gitBranch = this.normalizeRef(System.getenv("BITBUCKET_BRANCH"));
        this.gitTag = this.normalizeRef(System.getenv("BITBUCKET_TAG"));
    }

    private String buildPipelineUrl(String repo, String number) {
        return String.format("https://bitbucket.org/%s/addon/pipelines/home#!/results/%s", repo, number);
    }

    private String buildPipelineId() {
        String id = System.getenv("BITBUCKET_PIPELINE_UUID");
        if (id != null) {
            id = id.replaceAll("}", "").replaceAll("\\{", "");
        }

        return id;
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
