package datadog.lib.datadog.trace.bootstrap.instrumentation.api.ci;

import java.util.HashMap;
import java.util.Map;

class JenkinsInfo extends CIProviderInfo {
    public static final String JENKINS = "JENKINS_URL";
    public static final String JENKINS_PROVIDER_NAME = "jenkins";
    public static final String JENKINS_PIPELINE_ID = "BUILD_TAG";
    public static final String JENKINS_PIPELINE_NUMBER = "BUILD_NUMBER";
    public static final String JENKINS_PIPELINE_URL = "BUILD_URL";
    public static final String JENKINS_PIPELINE_NAME = "JOB_NAME";
    public static final String JENKINS_JOB_URL = "JOB_URL";
    public static final String JENKINS_WORKSPACE_PATH = "WORKSPACE";
    public static final String JENKINS_GIT_REPOSITORY_URL = "GIT_URL";
    public static final String JENKINS_GIT_COMMIT = "GIT_COMMIT";
    public static final String JENKINS_GIT_BRANCH = "GIT_BRANCH";
    private final String ciProviderName = "jenkins";
    private final String ciPipelineId = System.getenv("BUILD_TAG");
    private final String ciPipelineName;
    private final String ciPipelineNumber = System.getenv("BUILD_NUMBER");
    private final String ciPipelineUrl = System.getenv("BUILD_URL");
    private final String ciJobUrl = System.getenv("JOB_URL");
    private final String ciWorkspacePath = this.expandTilde(System.getenv("WORKSPACE"));
    private final String gitRepositoryUrl = this.filterSensitiveInfo(System.getenv("GIT_URL"));
    private final String gitCommit = System.getenv("GIT_COMMIT");
    private final String gitBranch = this.buildGitBranch();
    private final String gitTag = this.buildGitTag();

    JenkinsInfo() {
        this.ciPipelineName = this.buildCiPipelineName(this.gitBranch);
    }

    private String buildGitBranch() {
        String gitBranchOrTag = System.getenv("GIT_BRANCH");
        return gitBranchOrTag != null && !gitBranchOrTag.contains("tags") ? this.normalizeRef(gitBranchOrTag) : null;
    }

    private String buildGitTag() {
        String gitBranchOrTag = System.getenv("GIT_BRANCH");
        return gitBranchOrTag != null && gitBranchOrTag.contains("tags") ? this.normalizeRef(gitBranchOrTag) : null;
    }

    private String buildCiPipelineName(String branch) {
        String jobName = System.getenv("JOB_NAME");
        return this.filterJenkinsJobName(jobName, branch);
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

    private String filterJenkinsJobName(String jobName, String gitBranch) {
        if (jobName == null) {
            return null;
        } else {
            String jobNameNoBranch;
            if (gitBranch != null) {
                jobNameNoBranch = jobName.trim().replace("/" + gitBranch, "");
            } else {
                jobNameNoBranch = jobName;
            }

            Map<String, String> configurations = new HashMap();
            String[] jobNameParts = jobNameNoBranch.split("/");
            if (jobNameParts.length > 1 && jobNameParts[1].contains("=")) {
                String configsStr = jobNameParts[1].toLowerCase().trim();
                String[] configsKeyValue = configsStr.split(",");
                String[] var8 = configsKeyValue;
                int var9 = configsKeyValue.length;

                for(int var10 = 0; var10 < var9; ++var10) {
                    String configKeyValue = var8[var10];
                    String[] keyValue = configKeyValue.trim().split("=");
                    configurations.put(keyValue[0], keyValue[1]);
                }
            }

            return configurations.isEmpty() ? jobNameNoBranch : jobNameParts[0];
        }
    }
}
