package eu.sheepearrr.cordplanter.util;

import java.util.List;

public class WorkspaceProperties {
    public final int compileVersion;
    public final String format;
    public String displayName;
    public String displayVersion;
    public List<String> authors;

    public WorkspaceProperties(int compileVersion, String format) {
        this.compileVersion = compileVersion;
        this.format = format;
    }

    public String getDisplayName(String id) {
        return this.displayName != null ? this.displayName : id;
    }

    public String getDisplayVersion() {
        return this.displayVersion != null ? this.displayVersion : String.valueOf(this.compileVersion);
    }
}
