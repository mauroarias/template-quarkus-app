package datadog.lib.datadog.trace.bootstrap.instrumentation.api.ci;

class AzurePipelinesInfo extends CIProviderInfo {
    public static final String AZURE = "TF_BUILD";
    public static final String AZURE_PROVIDER_NAME = "azurepipelines";
    public static final String AZURE_PIPELINE_NAME = "BUILD_DEFINITIONNAME";
    public static final String AZURE_SYSTEM_TEAMFOUNDATIONSERVERURI = "SYSTEM_TEAMFOUNDATIONSERVERURI";
    public static final String AZURE_SYSTEM_TEAMPROJECT = "SYSTEM_TEAMPROJECT";
    public static final String AZURE_BUILD_BUILDID = "BUILD_BUILDID";
    public static final String AZURE_SYSTEM_JOBID = "SYSTEM_JOBID";
    public static final String AZURE_SYSTEM_TASKINSTANCEID = "SYSTEM_TASKINSTANCEID";
    public static final String AZURE_WORKSPACE_PATH = "BUILD_SOURCESDIRECTORY";
    public static final String AZURE_SYSTEM_PULLREQUEST_SOURCEREPOSITORYURI = "SYSTEM_PULLREQUEST_SOURCEREPOSITORYURI";
    public static final String AZURE_BUILD_REPOSITORY_URI = "BUILD_REPOSITORY_URI";
    public static final String AZURE_SYSTEM_PULLREQUEST_SOURCECOMMITID = "SYSTEM_PULLREQUEST_SOURCECOMMITID";
    public static final String AZURE_BUILD_SOURCEVERSION = "BUILD_SOURCEVERSION";
    public static final String AZURE_SYSTEM_PULLREQUEST_SOURCEBRANCH = "SYSTEM_PULLREQUEST_SOURCEBRANCH";
    public static final String AZURE_BUILD_SOURCEBRANCH = "BUILD_SOURCEBRANCH";
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

    AzurePipelinesInfo() {
        String uri = System.getenv("SYSTEM_TEAMFOUNDATIONSERVERURI");
        String project = System.getenv("SYSTEM_TEAMPROJECT");
        String buildId = System.getenv("BUILD_BUILDID");
        String jobId = System.getenv("SYSTEM_JOBID");
        String taskId = System.getenv("SYSTEM_TASKINSTANCEID");
        this.ciProviderName = "azurepipelines";
        this.ciPipelineId = System.getenv("BUILD_BUILDID");
        this.ciPipelineName = System.getenv("BUILD_DEFINITIONNAME");
        this.ciPipelineNumber = System.getenv("BUILD_BUILDID");
        this.ciWorkspacePath = this.expandTilde(System.getenv("BUILD_SOURCESDIRECTORY"));
        this.ciPipelineUrl = this.buildCiPipelineUrl(uri, project, buildId);
        this.ciJobUrl = this.buildCiJobUrl(uri, project, buildId, jobId, taskId);
        this.gitRepositoryUrl = this.buildGitRepositoryUrl();
        this.gitCommit = this.buildGitCommit();
        this.gitBranch = this.buildGitBranch();
        this.gitTag = this.buildGitTag();
    }

    private String buildGitTag() {
        String branchOrTag = System.getenv("SYSTEM_PULLREQUEST_SOURCEBRANCH");
        if (branchOrTag == null || branchOrTag.isEmpty()) {
            branchOrTag = System.getenv("BUILD_SOURCEBRANCH");
        }

        return branchOrTag != null && branchOrTag.contains("tags") ? this.normalizeRef(branchOrTag) : null;
    }

    private String buildGitBranch() {
        String branchOrTag = System.getenv("SYSTEM_PULLREQUEST_SOURCEBRANCH");
        if (branchOrTag == null || branchOrTag.isEmpty()) {
            branchOrTag = System.getenv("BUILD_SOURCEBRANCH");
        }

        return branchOrTag != null && !branchOrTag.contains("tags") ? this.normalizeRef(branchOrTag) : null;
    }

    private String buildGitCommit() {
        String commit = System.getenv("SYSTEM_PULLREQUEST_SOURCECOMMITID");
        if (commit == null || commit.isEmpty()) {
            commit = System.getenv("BUILD_SOURCEVERSION");
        }

        return commit;
    }

    private String buildGitRepositoryUrl() {
        String repoUrl = System.getenv("SYSTEM_PULLREQUEST_SOURCEREPOSITORYURI");
        if (repoUrl == null || repoUrl.isEmpty()) {
            repoUrl = System.getenv("BUILD_REPOSITORY_URI");
        }

        return this.filterSensitiveInfo(repoUrl);
    }

    private String buildCiJobUrl(String uri, String project, String buildId, String jobId, String taskId) {
        return String.format("%s%s/_build/results?buildId=%s&view=logs&j=%s&t=%s", uri, project, buildId, jobId, taskId);
    }

    private String buildCiPipelineUrl(String uri, String project, String buildId) {
        return String.format("%s%s/_build/results?buildId=%s&_a=summary", uri, project, buildId);
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
