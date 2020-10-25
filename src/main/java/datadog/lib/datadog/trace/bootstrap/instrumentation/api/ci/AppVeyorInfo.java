package datadog.lib.datadog.trace.bootstrap.instrumentation.api.ci;

class AppVeyorInfo extends CIProviderInfo {
    public static final String APPVEYOR = "APPVEYOR";
    public static final String APPVEYOR_PROVIDER_NAME = "appveyor";
    public static final String APPVEYOR_BUILD_ID = "APPVEYOR_BUILD_ID";
    public static final String APPVEYOR_REPO_NAME = "APPVEYOR_REPO_NAME";
    public static final String APPVEYOR_PIPELINE_NUMBER = "APPVEYOR_BUILD_NUMBER";
    public static final String APPVEYOR_WORKSPACE_PATH = "APPVEYOR_BUILD_FOLDER";
    public static final String APPVEYOR_REPO_PROVIDER = "APPVEYOR_REPO_PROVIDER";
    public static final String APPVEYOR_REPO_COMMIT = "APPVEYOR_REPO_COMMIT";
    public static final String APPVEYOR_REPO_BRANCH = "APPVEYOR_REPO_BRANCH";
    public static final String APPVEYOR_PULL_REQUEST_HEAD_REPO_BRANCH = "APPVEYOR_PULL_REQUEST_HEAD_REPO_BRANCH";
    public static final String APPVEYOR_REPO_TAG_NAME = "APPVEYOR_REPO_TAG_NAME";
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

    AppVeyorInfo() {
        String buildId = System.getenv("APPVEYOR_BUILD_ID");
        String repoName = System.getenv("APPVEYOR_REPO_NAME");
        String url = this.buildPipelineUrl(repoName, buildId);
        String repoProvider = System.getenv("APPVEYOR_REPO_PROVIDER");
        this.ciProviderName = "appveyor";
        this.ciPipelineId = buildId;
        this.ciPipelineName = repoName;
        this.ciPipelineNumber = System.getenv("APPVEYOR_BUILD_NUMBER");
        this.ciPipelineUrl = url;
        this.ciJobUrl = url;
        this.ciWorkspacePath = this.expandTilde(System.getenv("APPVEYOR_BUILD_FOLDER"));
        this.gitRepositoryUrl = this.buildGitRepositoryUrl(repoProvider, repoName);
        this.gitCommit = this.buildGitCommit(repoProvider);
        this.gitTag = this.buildGitTag(repoProvider);
        this.gitBranch = this.buildGitBranch(repoProvider, this.gitTag);
    }

    private String buildGitTag(String repoProvider) {
        return "github".equals(repoProvider) ? this.normalizeRef(System.getenv("APPVEYOR_REPO_TAG_NAME")) : null;
    }

    private String buildGitBranch(String repoProvider, String gitTag) {
        if (gitTag != null) {
            return null;
        } else if (!"github".equals(repoProvider)) {
            return null;
        } else {
            String branch = System.getenv("APPVEYOR_PULL_REQUEST_HEAD_REPO_BRANCH");
            if (branch == null || branch.isEmpty()) {
                branch = System.getenv("APPVEYOR_REPO_BRANCH");
            }

            return this.normalizeRef(branch);
        }
    }

    private String buildGitCommit(String repoProvider) {
        return "github".equals(repoProvider) ? System.getenv("APPVEYOR_REPO_COMMIT") : null;
    }

    private String buildGitRepositoryUrl(String repoProvider, String repoName) {
        return "github".equals(repoProvider) ? String.format("https://github.com/%s.git", repoName) : null;
    }

    private String buildPipelineUrl(String repoName, String buildId) {
        return String.format("https://ci.appveyor.com/project/%s/builds/%s", repoName, buildId);
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
