import java.io.File;

public class ExtensionRule implements Rule {
    private final String extension;
    private final String folder;

    public ExtensionRule(String extension, String folder) {
        this.extension = extension;
        this.folder = folder;
    }
    @Override
    public boolean check(File file) {
        return file.getName().toLowerCase().endsWith(this.extension);
    }

    @Override
    public String getFolder() {
        return this.folder;
    }
}
