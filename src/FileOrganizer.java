import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import static java.nio.file.StandardWatchEventKinds.*;

class FileOrganizer {
    // Biến thành viên (Field) - Chứa bộ luật
    private final ArrayList<Rule> rules;

    // 1. Constructor: Nơi khởi tạo bộ luật
    public FileOrganizer() {
        rules = new ArrayList<>();
        rules.add(new SizeRule(100, "Heavy"));
        rules.add(new ExtensionRule(".jpg", "Images"));
        rules.add(new ExtensionRule(".png", "Images"));
        rules.add(new ExtensionRule(".mp3", "Music"));
        rules.add(new ExtensionRule(".docx", "Documents"));
        rules.add(new ExtensionRule(".pdf", "Documents"));
        rules.add(new ExtensionRule(".msi", "Installers"));
        rules.add(new ExtensionRule(".exe", "Installers"));
        rules.add(new ExtensionRule(".iso", "Installers"));
        rules.add(new ExtensionRule(".rar", "Compressed"));
        rules.add(new ExtensionRule(".zip", "Compressed"));

    }

    // 2. Hàm chính: Quét dọn file cũ
    public void startOrganizing(String folderPath) {
        System.out.println("🤖 Bot đang khởi động tại: " + folderPath);

        File folder = new File(folderPath);
        if (!folder.exists()) {
            System.out.println("❌ Đường dẫn không tồn tại!");
            return;
        }

        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null) return;

        System.out.println("--- BẮT ĐẦU DỌN DẸP ---");
        for (File file : listOfFiles) {
            processFile(file);
        }
        System.out.println("✅ Hoàn thành dọn dẹp file cũ!");
    }

    // 3. Hàm xử lý logic cho từng file
    private void processFile(File file) {
        // Kiểm tra chắc chắn file còn tồn tại mới làm (Tránh lỗi file ảo)
        if (!file.exists()) return;

        String archiveFolderName = "Old_Cleanup";

        if (file.isFile()) {
            for (Rule r : rules) {
                if (r.check(file)) {
                    moveFile(file, r.getFolder());
                    return;
                }
            }
            System.out.println("⚠️ File lạ: " + file.getName() + " -> Vào Others");
            moveFile(file, "Others");
        }
        else if (file.isDirectory()) {
            String currentFolderName = file.getName();
            //TODO: Thêm phần xử lý
            //Né các folder dùng để xếp file vào
            //Duyệt qua cái rule nếu trùng tên thì skip qua, không thì move file vào archiveFolderName
            for (Rule r : rules) {
                if (file.getName().equalsIgnoreCase(r.getFolder()) ||
                        file.getName().equalsIgnoreCase(archiveFolderName) ||
                        file.getName().equalsIgnoreCase("Others")) {
                    return;
                }
            }
            moveFile(file, archiveFolderName);
        }
    }

    // 4. Hàm chạy ngầm (Canh gác)
    public void startWatching(String path) {
        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            Path dir = Path.of(path);
            dir.register(watcher, ENTRY_CREATE); //, ENTRY_MODIFY nếu muốn bắt kỹ hơn
            System.out.println("😎 Đang theo dõi thư mục: " + path);

            while (true) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException e) {
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == OVERFLOW) continue;

                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    String fileNameStr = fileName.toString();

                    // 🛑 CHẶN FILE RÁC TRƯỚC KHI IN LOG
                    // Nếu là file tạm download thì bỏ qua ngay lập tức, không in ra luôn
                    if (fileNameStr.endsWith(".crdownload") ||
                            fileNameStr.endsWith(".part") ||
                            fileNameStr.endsWith(".tmp")) {
                        continue;
                    }

                    // In ra file thật sự cần xử lý
                    Main.showNotification("Phát hiện file mới", fileName.toString());

                    Path fullPath = dir.resolve(fileName);
                    File fileCanXuLy = fullPath.toFile();

                    // Ngủ 1 chút để file kịp tải xong/đổi tên xong (Quan trọng)
                    try { Thread.sleep(1500); } catch (InterruptedException e) {}

                    // Gọi hàm xử lý
                    processFile(fileCanXuLy);
                }

                boolean valid = key.reset();
                if(!valid) break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm phụ: Di chuyển file
    private void moveFile(File file, String destinationFolder) {
        try {
            Path destDir = Path.of(file.getParent(), destinationFolder);

            if (!Files.exists(destDir)) {
                Files.createDirectories(destDir);
                System.out.println("📂 Đã tạo folder mới: " + destinationFolder);
            }

            Path targetPath = destDir.resolve(file.getName());

            // Di chuyển (Ghi đè nếu trùng)
            Files.move(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            Main.showNotification("Đã dọn dẹp! 🧹",
                    file.getName() + " -> " + destinationFolder);
        } catch (IOException e) {
            // Không in lỗi nếu lỗi là do file không tồn tại (do bot chạy nhanh quá file bị move rồi)
            if (file.exists()) {
                System.out.println("   ❌ Lỗi khi chuyển file: " + e.getMessage());
            }
        }
    }
}