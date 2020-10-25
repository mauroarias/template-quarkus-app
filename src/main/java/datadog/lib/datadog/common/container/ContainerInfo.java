package datadog.lib.datadog.common.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContainerInfo {
    private static final Logger log = LoggerFactory.getLogger(ContainerInfo.class);
    private static final Path CGROUP_DEFAULT_PROCFILE = Paths.get("/proc/self/cgroup");
    private static final String UUID_REGEX = "[0-9a-f]{8}[-_][0-9a-f]{4}[-_][0-9a-f]{4}[-_][0-9a-f]{4}[-_][0-9a-f]{12}";
    private static final String CONTAINER_REGEX = "[0-9a-f]{64}";
    private static final Pattern LINE_PATTERN = Pattern.compile("(\\d+):([^:]*):(.+)$");
    private static final Pattern POD_PATTERN = Pattern.compile("(?:.+)?pod([0-9a-f]{8}[-_][0-9a-f]{4}[-_][0-9a-f]{4}[-_][0-9a-f]{4}[-_][0-9a-f]{12})(?:.slice)?$");
    private static final Pattern CONTAINER_PATTERN = Pattern.compile("(?:.+)?([0-9a-f]{8}[-_][0-9a-f]{4}[-_][0-9a-f]{4}[-_][0-9a-f]{4}[-_][0-9a-f]{12}|[0-9a-f]{64})(?:.scope)?$");
    private static final ContainerInfo INSTANCE;
    public String containerId;
    public String podId;
    public List<ContainerInfo.CGroupInfo> cGroups = new ArrayList();

    public ContainerInfo() {
    }

    public static ContainerInfo get() {
        return INSTANCE;
    }

    public static boolean isRunningInContainer() {
        return Files.isReadable(CGROUP_DEFAULT_PROCFILE);
    }

    public static ContainerInfo fromDefaultProcFile() throws IOException, ParseException {
        return fromProcFile(CGROUP_DEFAULT_PROCFILE);
    }

    static ContainerInfo fromProcFile(Path path) throws IOException, ParseException {
        String content = new String(Files.readAllBytes(path));
        if (content.isEmpty()) {
            log.debug("Proc file is empty");
            return new ContainerInfo();
        } else {
            return parse(content);
        }
    }

    public static ContainerInfo parse(String cgroupsContent) throws ParseException {
        ContainerInfo containerInfo = new ContainerInfo();
        String[] lines = cgroupsContent.split("\n");
        String[] var3 = lines;
        int var4 = lines.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String line = var3[var5];
            ContainerInfo.CGroupInfo cGroupInfo = parseLine(line);
            containerInfo.getCGroups().add(cGroupInfo);
            if (cGroupInfo.getPodId() != null) {
                containerInfo.setPodId(cGroupInfo.getPodId());
            }

            if (cGroupInfo.getContainerId() != null) {
                containerInfo.setContainerId(cGroupInfo.getContainerId());
            }
        }

        return containerInfo;
    }

    static ContainerInfo.CGroupInfo parseLine(String line) throws ParseException {
        Matcher matcher = LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new ParseException("Unable to match cgroup", 0);
        } else {
            ContainerInfo.CGroupInfo cGroupInfo = new ContainerInfo.CGroupInfo();
            cGroupInfo.setId(Integer.parseInt(matcher.group(1)));
            cGroupInfo.setControllers(Arrays.asList(matcher.group(2).split(",")));
            String path = matcher.group(3);
            String[] pathParts = path.split("/");
            cGroupInfo.setPath(path);
            Matcher podIdMatcher;
            String podId;
            if (pathParts.length >= 1) {
                podIdMatcher = CONTAINER_PATTERN.matcher(pathParts[pathParts.length - 1]);
                podId = podIdMatcher.matches() ? podIdMatcher.group(1) : null;
                cGroupInfo.setContainerId(podId);
            }

            if (pathParts.length >= 2) {
                podIdMatcher = POD_PATTERN.matcher(pathParts[pathParts.length - 2]);
                podId = podIdMatcher.matches() ? podIdMatcher.group(1) : null;
                cGroupInfo.setPodId(podId);
            }

            return cGroupInfo;
        }
    }

    public String getContainerId() {
        return this.containerId;
    }

    public String getPodId() {
        return this.podId;
    }

    public List<ContainerInfo.CGroupInfo> getCGroups() {
        return this.cGroups;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public void setPodId(String podId) {
        this.podId = podId;
    }

    public void setCGroups(List<ContainerInfo.CGroupInfo> cGroups) {
        this.cGroups = cGroups;
    }

    static {
        ContainerInfo containerInfo = new ContainerInfo();
        if (isRunningInContainer()) {
            try {
                containerInfo = fromDefaultProcFile();
            } catch (ParseException | IOException var2) {
                log.error("Unable to parse proc file");
            }
        }

        INSTANCE = containerInfo;
    }

    public static class CGroupInfo {
        public int id;
        public String path;
        public List<String> controllers;
        public String containerId;
        public String podId;

        public CGroupInfo() {
        }

        public int getId() {
            return this.id;
        }

        public String getPath() {
            return this.path;
        }

        public List<String> getControllers() {
            return this.controllers;
        }

        public String getContainerId() {
            return this.containerId;
        }

        public String getPodId() {
            return this.podId;
        }

        public void setId(int id) {
            this.id = id;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public void setControllers(List<String> controllers) {
            this.controllers = controllers;
        }

        public void setContainerId(String containerId) {
            this.containerId = containerId;
        }

        public void setPodId(String podId) {
            this.podId = podId;
        }
    }
}
