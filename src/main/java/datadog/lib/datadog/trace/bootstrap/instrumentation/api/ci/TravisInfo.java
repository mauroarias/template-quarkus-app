package datadog.lib.datadog.trace.bootstrap.instrumentation.api.ci;

class TravisInfo extends CIProviderInfo {
    public static final String TRAVIS = "TRAVIS";
    public static final String TRAVIS_PROVIDER_NAME = "travisci";
    public static final String TRAVIS_PIPELINE_ID = "TRAVIS_BUILD_ID";
    public static final String TRAVIS_PIPELINE_NUMBER = "TRAVIS_BUILD_NUMBER";
    public static final String TRAVIS_PIPELINE_URL = "TRAVIS_BUILD_WEB_URL";
    public static final String TRAVIS_JOB_URL = "TRAVIS_JOB_WEB_URL";
    public static final String TRAVIS_WORKSPACE_PATH = "TRAVIS_BUILD_DIR";
    public static final String TRAVIS_REPOSITORY_SLUG = "TRAVIS_REPO_SLUG";
    public static final String TRAVIS_PR_REPOSITORY_SLUG = "TRAVIS_PULL_REQUEST_SLUG";
    public static final String TRAVIS_GIT_COMMIT = "TRAVIS_COMMIT";
    public static final String TRAVIS_GIT_PR_BRANCH = "TRAVIS_PULL_REQUEST_BRANCH";
    public static final String TRAVIS_GIT_BRANCH = "TRAVIS_BRANCH";
    public static final String TRAVIS_GIT_TAG = "TRAVIS_TAG";
    private final String ciProviderName = "travisci";
    private final String ciPipelineId = System.getenv("TRAVIS_BUILD_ID");
    private final String ciPipelineName = this.buildCiPipelineName();
    private final String ciPipelineNumber = System.getenv("TRAVIS_BUILD_NUMBER");
    private final String ciPipelineUrl = System.getenv("TRAVIS_BUILD_WEB_URL");
    private final String ciJobUrl = System.getenv("TRAVIS_JOB_WEB_URL");
    private final String ciWorkspacePath = this.expandTilde(System.getenv("TRAVIS_BUILD_DIR"));
    private final String gitRepositoryUrl = this.buildGitRepositoryUrl();
    private final String gitCommit = System.getenv("TRAVIS_COMMIT");
    private final String gitBranch;
    private final String gitTag = this.normalizeRef(System.getenv("TRAVIS_TAG"));

    TravisInfo() {
        this.gitBranch = this.buildGitBranch(this.gitTag);
    }

    private String buildGitBranch(String gitTag) {
        if (gitTag != null) {
            return null;
        } else {
            String fromBranch = System.getenv("TRAVIS_PULL_REQUEST_BRANCH");
            return fromBranch != null && !fromBranch.isEmpty() ? this.normalizeRef(fromBranch) : this.normalizeRef(System.getenv("TRAVIS_BRANCH"));
        }
    }

    private String buildGitRepositoryUrl() {
        String repoSlug = System.getenv("TRAVIS_PULL_REQUEST_SLUG");
        if (repoSlug == null || repoSlug.isEmpty()) {
            repoSlug = System.getenv("TRAVIS_REPO_SLUG");
        }

        return String.format("https://github.com/%s.git", repoSlug);
    }

    private String buildCiPipelineName() {
        String repoSlug = System.getenv("TRAVIS_PULL_REQUEST_SLUG");
        if (repoSlug == null || repoSlug.isEmpty()) {
            repoSlug = System.getenv("TRAVIS_REPO_SLUG");
        }

        return repoSlug;
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
