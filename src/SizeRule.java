import java.io.File;

public class SizeRule implements Rule {
    private final long sizeInMB;
    private final String folder;

    public SizeRule(long sizeInMB, String folder) {
        this.sizeInMB = sizeInMB;
        this.folder = folder;
    }
    @Override
    public boolean check(File file) {
        // file.length() trả về Bytes.
        // 1 MB = 1024 * 1024 Bytes.
        long sizeInBytes = this.sizeInMB * 1024 * 1024;
        return file.length() > sizeInBytes; //File nặng hơn giới hạn -> Khớp luật
    }

    @Override
    public String getFolder() {
        return this.folder;
    }
}
