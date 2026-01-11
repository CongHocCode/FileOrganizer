import javax.swing.*;
import java.awt.*;
import java.io.File;

public class Main {
    private static TrayIcon trayIcon;

    public static void main(String[] args) {
        // 1. Chạy trong luồng giao diện an toàn (Fix lỗi crash)
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }

    private static void createAndShowGUI() {
        // 2. Ép giao diện giống Windows 10/11 (Fix lỗi JFileChooser xấu và lỗi)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!SystemTray.isSupported()) {
            JOptionPane.showMessageDialog(null, "Máy không hỗ trợ System Tray!");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn thư mục Downloads để Bot canh gác");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // Mặc định mở ở Downloads
        File downloadDir = new File(System.getProperty("user.home"), "Downloads");
        // Kiểm tra folder có tồn tại không trước khi set (Tránh lỗi nếu máy bạn dùng OneDrive)
        if (downloadDir.exists()) {
            chooser.setCurrentDirectory(downloadDir);
        }

        int result = chooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            System.out.println("Hủy chọn. Tắt Bot.");
            return;
        }

        String path = chooser.getSelectedFile().getAbsolutePath();
        createSystemTray(path);

        // Chạy Bot ở luồng riêng (Thread khác) để không đơ giao diện
        new Thread(() -> {
            FileOrganizer bot = new FileOrganizer();
            showNotification("Bot đang chạy ngầm! 🥷", "Đang canh gác: " + path);
            bot.startOrganizing(path);
            bot.startWatching(path);
        }).start();
    }

    // --- Giữ nguyên các hàm bên dưới không đổi ---
    private static void createSystemTray(String path) {
        PopupMenu popup = new PopupMenu();
        MenuItem itemInfo = new MenuItem("Dang chay tai: " + path);
        itemInfo.setEnabled(false);
        MenuItem exitItem = new MenuItem("Thoat (Exit)");
        exitItem.addActionListener(e -> System.exit(0));

        popup.add(itemInfo);
        popup.addSeparator();
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