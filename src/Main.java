import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    private static TrayIcon trayIcon;
    // Tên cuốn sổ tay (sẽ nằm cạnh file .jar)
    private static final String CONFIG_FILE = "bot_config.txt";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }

    private static void createAndShowGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { e.printStackTrace(); }

        if (!SystemTray.isSupported()) {
            JOptionPane.showMessageDialog(null, "Máy không hỗ trợ System Tray!");
            return;
        }

        // 1. KIỂM TRA SỔ TAY TRƯỚC
        String savedPath = loadPathFromConfig();
        String finalPath = null;

        // Nếu có đường dẫn cũ và đường dẫn đó vẫn tồn tại -> Dùng luôn
        if (savedPath != null && new File(savedPath).exists()) {
            finalPath = savedPath;
        } else {
            // 2. NẾU KHÔNG CÓ (HOẶC SAI) -> MỚI HIỆN BẢNG HỎI
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Chọn thư mục Downloads để Bot canh gác");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            // Mặc định mở ở Downloads
            File downloadDir = new File(System.getProperty("user.home"), "Downloads");
            if (downloadDir.exists()) chooser.setCurrentDirectory(downloadDir);

            int result = chooser.showOpenDialog(null);
            if (result != JFileChooser.APPROVE_OPTION) {
                System.out.println("Hủy chọn. Tắt Bot.");
                return;
            }
            finalPath = chooser.getSelectedFile().getAbsolutePath();

            // 3. LƯU LẠI VÀO SỔ TAY ĐỂ LẦN SAU KHỎI HỎI
            savePathToConfig(finalPath);
        }

        // --- Đoạn dưới này giữ nguyên ---
        createSystemTray(finalPath);
        String pathForThread = finalPath; // Biến final để ném vào Thread

        new Thread(() -> {
            FileOrganizer bot = new FileOrganizer();
            // Chỉ hiện thông báo lần đầu hoặc khi cần thiết, khởi động cùng win thì có thể bỏ dòng này cho đỡ phiền
            // showNotification("Bot đã online! 🥷", "Đang canh gác: " + pathForThread);

            bot.startOrganizing(pathForThread);
            bot.startWatching(pathForThread);
        }).start();
    }

    // --- Hàm đọc file config ---
    private static String loadPathFromConfig() {
        try {
            File file = new File(CONFIG_FILE);
            if (file.exists()) {
                // Đọc nội dung file
                return Files.readString(file.toPath()).trim();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- Hàm ghi file config ---
    private static void savePathToConfig(String path) {
        try {
            Files.writeString(Path.of(CONFIG_FILE), path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Các hàm tạo TrayIcon giữ nguyên như cũ ---
    private static void createSystemTray(String path) {
        PopupMenu popup = new PopupMenu();
        MenuItem itemInfo = new MenuItem("Dang chay tai: " + path);
        itemInfo.setEnabled(false);
        MenuItem exitItem = new MenuItem("Thoat (Exit)");
        exitItem.addActionListener(e -> System.exit(0));

        // Nút Reset để chọn lại thư mục (Tính năng mới)
        MenuItem resetItem = new MenuItem("Doi thu muc (Reset)");
        resetItem.addActionListener(e -> {
            // Xóa file config và restart (đơn giản là bảo người dùng bật lại)
            try { Files.deleteIfExists(Path.of(CONFIG_FILE)); } catch (IOException ex) {}
            JOptionPane.showMessageDialog(null, "Đã xóa cài đặt. Hãy khởi động lại Bot để chọn thư mục mới.");
            System.exit(0);
        });

        popup.add(itemInfo);
        popup.addSeparator();
        popup.add(resetItem); // Thêm nút reset vào menu
        popup.add(exitItem);

        Image image = createImage();
        trayIcon = new TrayIcon(image, "Java File Bot", popup);
        trayIcon.setImageAutoSize(true);

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public static void showNotification(String title, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    private static Image createImage() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.GREEN);
        g.fillOval(0, 0, 16, 16);
        g.dispose();
        return img;
    }
}