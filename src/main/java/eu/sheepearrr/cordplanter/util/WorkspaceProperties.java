package eu.sheepearrr.cordplanter.util;

public class WorkspaceProperties {
    public final int compileVersion;
    public final String format;
    public final String displayName;
    public final String displayVersion;

    public WorkspaceProperties(int compileVersion, String format, String displayName, String displayVersion) {
        this.compileVersion = compileVersion;
        this.format = format;
        this.displayName = displayName;
        this.displayVersion = displayVersion;
    }
}
