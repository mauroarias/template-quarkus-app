package datadog.lib.datadog.common.container;

public class ServerlessInfo {
    private static final String AWS_FUNCTION_VARIABLE = "AWS_LAMBDA_FUNCTION_NAME";
    private static final ServerlessInfo INSTANCE = new ServerlessInfo();
    private final String functionName = System.getenv("AWS_LAMBDA_FUNCTION_NAME");

    public ServerlessInfo() {
    }

    public static ServerlessInfo get() {
        return INSTANCE;
    }

    public boolean isRunningInServerlessEnvironment() {
        return this.functionName != null && !this.functionName.isEmpty();
    }

    public String getFunctionName() {
        return this.functionName;
    }
}
