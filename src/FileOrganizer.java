import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;

import static java.nio.file.StandardWatchEventKinds.*;

class FileOrganizer {
    // Biến thành viên (Field) - Chứa bộ luật
    private final ArrayList<Rule> rules;

    // Constructor: Nơi khởi tạo bộ luật
    public FileOrganizer() {
        rules = new ArrayList<>();
        rules.add(new SizeRule(100, "Heavy"));

        loadRulesFromFile();

        if (rules.size() <= 1) {
            rules.add(new ExtensionRule(".jpg", "Images"));
            rules.add(new ExtensionRule(".png", "Images"));
            rules.add(new ExtensionRule(".mp3", "Music"));
            rules.add(new ExtensionRule(".mp4", "Videos"));
            rules.add(new ExtensionRule(".docx", "Documents"));
            rules.add(new ExtensionRule(".pdf", "Documents"));
            rules.add(new ExtensionRule(".msi", "Installers"));
            rules.add(new ExtensionRule(".exe", "Installers"));
            rules.add(new ExtensionRule(".iso", "Installers"));
            rules.add(new ExtensionRule(".rar", "Compressed"));
            rules.add(new ExtensionRule(".zip", "Compressed"));
        }

    }



    // Hàm chính: Quét dọn file cũ
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

        //Lọc bớt các file rác
        Arrays.stream(listOfFiles).filter(f -> {
            String name = f.getName().toLowerCase();
            return name.endsWith(".tmp") ||
                    name.endsWith(".log");
        }).forEach(
                f -> {
                    if (f.delete()) {
                        System.out.println("Đã xóa file rác: " + f.getName());
                    } else {
                        System.err.println("Không xóa được: " + f.getName());
                    }
                }
        );

        for (File file : listOfFiles) {
            try {
                processFile(file);
            } catch (FileDangerousException e) {
                System.out.println("CẢNH BÁO: " + e.getMessage());
            }
        }
        System.out.println("✅ Hoàn thành dọn dẹp file cũ!");
    }

    private void loadRulesFromFile() {
        Path configFile = Path.of("rules.txt");

        if (Files.notExists(configFile)) {
            System.out.println("Không tìm thấy file rules.txt, dùng luật mặc định.");
            return;
        }

        System.out.println("Đang đọc luật từ file...");
        try (java.util.stream.Stream<String> lines = Files.lines(configFile)) {
            lines
                    //Lọc dòng trống và comment(#)
                    .filter(line -> !line.trim().isEmpty() && !line.startsWith("#"))
                    .map(line -> line.split("\\|")) //Chưa rõ
                    .filter(parts -> parts.length == 2)
                    .map(parts -> new ExtensionRule(parts[0].trim(), parts[1].trim()))
                    .forEach(this.rules::add);
            System.out.println("Đã nạp xong " + rules.size() + " luật.");
        } catch (IOException e) {
            System.err.println("Lỗi đọc file rules.txt: " + e.getMessage());
        }
    }


    // Hàm xử lý logic cho từng file
    private void processFile(File file) throws FileDangerousException {
        // Kiểm tra chắc chắn file còn tồn tại mới làm (Tránh lỗi file ảo)
        if (!file.exists()) return;

        String name = file.getName().toLowerCase();
        long sizeMB = file.length() / (1024 * 1024);

        //Nếu là exe/bat mà nhẹ (<2MB) -> Nghi virus
        if ((name.endsWith(".exe") || name.endsWith(".bat")) && sizeMB < 2) {
            //Dừng ngay lập tức
            throw new FileDangerousException("Phát hiện file nghi vấn (Virus?): " + file.getName());
        }
        String archiveFolderName = "Old_Cleanup";

        if (file.isFile()) {
            String targetFolder = rules.stream().filter(r -> r.check(file)).findFirst().map(Rule::getFolder).orElse("Others");
        } else if (file.isDirectory()) {
            String currentFolderName = file.getName();
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

    // Hàm chạy ngầm (Canh gác)
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
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                    }

                    // Gọi hàm xử lý
                    try {
                        processFile(fileCanXuLy);
                    } catch (FileDangerousException e) {
                        Main.showNotification("BỎ QUA FILE!", e.getMessage());
                        System.err.println(e.getMessage());
                    }
                }

                boolean valid = key.reset();
                if (!valid) break;
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
        } catch (FileSystemException e) {
            System.err.println("Không thể chuyển file: " + file.getName() + " -> " + destinationFolder);
            System.err.println("Lý do: File đang được sử dụng bởi ứng dụng khác!");
        } catch (IOException e) {
            System.err.println("Lỗi kỹ thuật: " + e.getMessage());
        } catch (Exception e) {
            //Lọc nốt mấy cái lỗi khác
            System.err.println("Lỗi không xác định: " + e.toString());

        }
    }

}