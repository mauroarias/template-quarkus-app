package datadog.lib.datadog.trace.bootstrap.instrumentation.api.ci;

class NoopCIInfo extends CIProviderInfo {
    NoopCIInfo() {
    }

    public boolean isCI() {
        return false;
    }

    public String getCiProviderName() {
        return null;
    }

    public String getCiPipelineId() {
        return null;
    }

    public String getCiPipelineName() {
        return null;
    }

    public String getCiPipelineNumber() {
        return null;
    }

    public String getCiPipelineUrl() {
        return null;
    }

    public String getCiJobUrl() {
        return null;
    }

    public String getCiWorkspacePath() {
        return null;
    }

    public String getGitRepositoryUrl() {
        return null;
    }

    public String getGitCommit() {
        return null;
    }

    public String getGitBranch() {
        return null;
    }

    public String getGitTag() {
        return null;
    }
}
