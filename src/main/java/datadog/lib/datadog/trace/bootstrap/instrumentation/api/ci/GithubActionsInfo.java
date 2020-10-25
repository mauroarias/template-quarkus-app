package datadog.lib.datadog.trace.bootstrap.instrumentation.api.ci;

class GithubActionsInfo extends CIProviderInfo {
    public static final String GHACTIONS = "GITHUB_ACTION";
    public static final String GHACTIONS_PROVIDER_NAME = "github";
    public static final String GHACTIONS_PIPELINE_ID = "GITHUB_RUN_ID";
    public static final String GHACTIONS_PIPELINE_NAME = "GITHUB_WORKFLOW";
    public static final String GHACTIONS_PIPELINE_NUMBER = "GITHUB_RUN_NUMBER";
    public static final String GHACTIONS_WORKSPACE_PATH = "GITHUB_WORKSPACE";
    public static final String GHACTIONS_REPOSITORY = "GITHUB_REPOSITORY";
    public static final String GHACTIONS_SHA = "GITHUB_SHA";
    public static final String GHACTIONS_HEAD_REF = "GITHUB_HEAD_REF";
    public static final String GHACTIONS_REF = "GITHUB_REF";
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

    GithubActionsInfo() {
        String repo = System.getenv("GITHUB_REPOSITORY");
        String commit = System.getenv("GITHUB_SHA");
        String url = this.buildPipelineUrl(repo, commit);
        this.ciProviderName = "github";
        this.ciPipelineId = System.getenv("GITHUB_RUN_ID");
        this.ciPipelineName = System.getenv("GITHUB_WORKFLOW");
        this.ciPipelineNumber = System.getenv("GITHUB_RUN_NUMBER");
        this.ciPipelineUrl = url;
        this.ciJobUrl = url;
        this.ciWorkspacePath = this.expandTilde(System.getenv("GITHUB_WORKSPACE"));
        this.gitRepositoryUrl = this.buildGitRepositoryUrl(repo);
        this.gitCommit = commit;
        this.gitBranch = this.buildGitBranch();
        this.gitTag = this.buildGitTag();
    }

    private String buildGitTag() {
        String gitBranchOrTag = System.getenv("GITHUB_HEAD_REF");
        if (gitBranchOrTag == null || gitBranchOrTag.isEmpty()) {
            gitBranchOrTag = System.getenv("GITHUB_REF");
        }

        return gitBranchOrTag != null && gitBranchOrTag.contains("tags") ? this.normalizeRef(gitBranchOrTag) : null;
    }

    private String buildGitBranch() {
        String gitBranchOrTag = System.getenv("GITHUB_HEAD_REF");
        if (gitBranchOrTag == null || gitBranchOrTag.isEmpty()) {
            gitBranchOrTag = System.getenv("GITHUB_REF");
        }

        return gitBranchOrTag != null && !gitBranchOrTag.contains("tags") ? this.normalizeRef(gitBranchOrTag) : null;
    }

    private String buildGitRepositoryUrl(String repo) {
        return String.format("https://github.com/%s.git", repo);
    }

    private String buildPipelineUrl(String repo, String commit) {
        return String.format("https://github.com/%s/commit/%s/checks", repo, commit);
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
