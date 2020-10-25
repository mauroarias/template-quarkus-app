package datadog.lib.datadog.trace.bootstrap.instrumentation.api.ci;

import java.net.URI;
import java.net.URISyntaxException;

public abstract class CIProviderInfo {
    public CIProviderInfo() {
    }

    public boolean isCI() {
        return true;
    }

    public abstract String getCiProviderName();

    public abstract String getCiPipelineId();

    public abstract String getCiPipelineName();

    public abstract String getCiPipelineNumber();

    public abstract String getCiPipelineUrl();

    public abstract String getCiJobUrl();

    public abstract String getCiWorkspacePath();

    public abstract String getGitRepositoryUrl();

    public abstract String getGitCommit();

    public abstract String getGitBranch();

    public abstract String getGitTag();

    public static CIProviderInfo selectCI() {
        if (System.getenv("JENKINS_URL") != null) {
            return new JenkinsInfo();
        } else if (System.getenv("GITLAB_CI") != null) {
            return new GitLabInfo();
        } else if (System.getenv("TRAVIS") != null) {
            return new TravisInfo();
        } else if (System.getenv("CIRCLECI") != null) {
            return new CircleCIInfo();
        } else if (System.getenv("APPVEYOR") != null) {
            return new AppVeyorInfo();
        } else if (System.getenv("TF_BUILD") != null) {
            return new AzurePipelinesInfo();
        } else if (System.getenv("BITBUCKET_COMMIT") != null) {
            return new BitBucketInfo();
        } else if (System.getenv("GITHUB_ACTION") != null) {
            return new GithubActionsInfo();
        } else {
            return (CIProviderInfo)(System.getenv("BUILDKITE") != null ? new BuildkiteInfo() : new NoopCIInfo());
        }
    }

    protected String expandTilde(String path) {
        if (path != null && !path.isEmpty() && path.startsWith("~")) {
            return !path.equals("~") && !path.startsWith("~/") ? path : path.replaceFirst("^~", System.getProperty("user.home"));
        } else {
            return path;
        }
    }

    protected String normalizeRef(String rawRef) {
        if (rawRef != null && !rawRef.isEmpty()) {
            String ref = rawRef;
            if (rawRef.startsWith("origin")) {
                ref = rawRef.replace("origin/", "");
            } else if (rawRef.startsWith("refs/heads")) {
                ref = rawRef.replace("refs/heads/", "");
            }

            return ref.startsWith("tags") ? ref.replace("tags/", "") : ref;
        } else {
            return null;
        }
    }

    protected String filterSensitiveInfo(String urlStr) {
        if (urlStr != null && !urlStr.isEmpty()) {
            try {
                URI url = new URI(urlStr);
                String userInfo = url.getRawUserInfo();
                return urlStr.replace(userInfo + "@", "");
            } catch (URISyntaxException var4) {
                return urlStr;
            }
        } else {
            return null;
        }
    }
}
