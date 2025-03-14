import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class CRMPlus {
    private JFrame frame;
    private JTextArea logsArea;
    private JButton launchButton;
    private JButton stopButton;
    private Process proxyProcess;
    private volatile boolean isProxyRunning = false;
    private JTextField fortnitePathField;
    private JCheckBox[] cosmeticChecks;
    private JLabel statusLabel;
    private static final String BASE_DIR;
    static {
        try {
            BASE_DIR = new File(CRMPlus.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        } catch (Exception e) {
            throw new RuntimeException("Failed to determine BASE_DIR: " + e.getMessage());
        }
    }
    private static final String MITMPROXY_PATH = "C:\\Users\\noahw\\AppData\\Local\\Packages\\PythonSoftwareFoundation.Python.3.13_qbz5n2kfra8p0\\LocalCache\\local-packages\\Python313\\Scripts\\mitmproxy.exe";
    private static final String PROXY_SCRIPT = BASE_DIR + "\\proxy\\proxy_script.py";
    private static final String VERSION = "1.2";
    private UpdateManager updateManager;

    public CRMPlus() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            UIManager.put("Panel.background", new Color(30, 30, 50));
            UIManager.put("Label.foreground", new Color(211, 211, 211));
            UIManager.put("Button.foreground", new Color(211, 211, 211));
            UIManager.put("TextField.background", new Color(40, 40, 60));
            UIManager.put("TextField.foreground", new Color(211, 211, 211));
            UIManager.put("TextArea.background", new Color(20, 20, 40));
            UIManager.put("TextArea.foreground", new Color(211, 211, 211));
            UIManager.put("CheckBox.background", new Color(30, 30, 50));
            UIManager.put("CheckBox.foreground", new Color(211, 211, 211));
            UIManager.put("TabbedPane.background", new Color(30, 30, 50));
            UIManager.put("TabbedPane.foreground", new Color(0, 0, 0)); // Darker tab text
        } catch (Exception e) {
            System.err.println("Failed to set Windows Look and Feel: " + e.getMessage());
        }
        initializeUI();
        checkSetup();
        updateManager = new UpdateManager(this);
        updateManager.checkForUpdates();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopProxy();
            }
        });
    }

    private void initializeUI() {
        frame = new JFrame("CRM Plus");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setMinimumSize(new Dimension(400, 300));
        frame.setLocationRelativeTo(null);

        try {
            ImageIcon icon = new ImageIcon("C:/Users/noahw/Downloads/icons8-fighter-jet-80.png");
            frame.setIconImage(icon.getImage());
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + e.getMessage());
        }

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(30, 30, 50));
        tabbedPane.setForeground(new Color(0, 0, 0)); // Darker tab text

        JPanel homePanel = new JPanel(new GridBagLayout());
        homePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        homePanel.setBackground(new Color(30, 30, 50));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("CRM Plus", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(211, 211, 211));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        homePanel.add(titleLabel, gbc);

        JLabel logoLabel = new JLabel();
        try {
            ImageIcon logoIcon = new ImageIcon("C:/Users/noahw/Downloads/icons8-fighter-jet-80.png");
            logoLabel.setIcon(new ImageIcon(logoIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH)));
        } catch (Exception e) {
            System.err.println("Failed to load logo: " + e.getMessage());
        }
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        homePanel.add(logoLabel, gbc);

        JLabel developedBy = new JLabel("Developed by Noah", SwingConstants.RIGHT);
        developedBy.setFont(new Font("Arial", Font.ITALIC, 12));
        developedBy.setForeground(new Color(150, 150, 150));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        homePanel.add(developedBy, gbc);

        JPanel checkPanel = new JPanel(new GridLayout(5, 1, 0, 5));
        checkPanel.setBackground(homePanel.getBackground());
        String[] cosmeticNames = {"Skins", "Backblings", "Pickaxes", "Emotes", "Wraps"};
        cosmeticChecks = new JCheckBox[cosmeticNames.length];
        for (int i = 0; i < cosmeticNames.length; i++) {
            cosmeticChecks[i] = new JCheckBox("Enable All " + cosmeticNames[i], true);
            cosmeticChecks[i].setForeground(new Color(211, 211, 211));
            cosmeticChecks[i].setFont(new Font("Arial", Font.PLAIN, 14));
            cosmeticChecks[i].setBackground(homePanel.getBackground());
            checkPanel.add(cosmeticChecks[i]);
        }
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        homePanel.add(checkPanel, gbc);

        fortnitePathField = new JTextField("C:\\Program Files\\Epic Games\\Fortnite\\FortniteGame\\Binaries\\Win64\\FortniteLauncher.exe");
        fortnitePathField.setFont(new Font("Arial", Font.PLAIN, 12));
        gbc.gridy = 3;
        homePanel.add(fortnitePathField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(homePanel.getBackground());
        launchButton = new JButton("Launch Fortnite");
        launchButton.setFont(new Font("Arial", Font.BOLD, 14));
        launchButton.setOpaque(false);
        launchButton.setContentAreaFilled(false);
        launchButton.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 1));
        launchButton.addActionListener(e -> new Thread(this::launchFortnite).start());
        launchButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                launchButton.setBorder(BorderFactory.createLineBorder(new Color(211, 211, 211), 1));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                launchButton.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 1));
            }
        });
        buttonPanel.add(launchButton);

        stopButton = new JButton("Stop Proxy");
        stopButton.setFont(new Font("Arial", Font.BOLD, 14));
        stopButton.setVisible(false);
        stopButton.addActionListener(e -> stopProxy());
        buttonPanel.add(stopButton);

        JButton checkUpdatesButton = new JButton("Check for Updates");
        checkUpdatesButton.setFont(new Font("Arial", Font.BOLD, 14));
        checkUpdatesButton.setOpaque(false);
        checkUpdatesButton.setContentAreaFilled(false);
        checkUpdatesButton.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 1));
        checkUpdatesButton.addActionListener(e -> updateManager.checkForUpdates());
        checkUpdatesButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                checkUpdatesButton.setBorder(BorderFactory.createLineBorder(new Color(211, 211, 211), 1));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                checkUpdatesButton.setBorder(BorderFactory.createLineBorder(new Color(150, 150, 150), 1));
            }
        });
        buttonPanel.add(checkUpdatesButton);

        gbc.gridy = 4;
        homePanel.add(buttonPanel, gbc);

        statusLabel = new JLabel("Ready", SwingConstants.CENTER);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(50, 150, 50));
        statusLabel.setForeground(new Color(211, 211, 211));
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        gbc.gridy = 5;
        homePanel.add(statusLabel, gbc);

        logsArea = new JTextArea(10, 50);
        logsArea.setEditable(false);
        logsArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        logsArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(logsArea);
        gbc.gridy = 6;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        homePanel.add(scrollPane, gbc);

        JButton clearLogsButton = new JButton("Clear Logs");
        clearLogsButton.setFont(new Font("Arial", Font.BOLD, 12));
        clearLogsButton.setForeground(new Color(0, 0, 0)); // Darker text
        clearLogsButton.addActionListener(e -> logsArea.setText(""));
        gbc.gridy = 7;
        gbc.weighty = 0;
        homePanel.add(clearLogsButton, gbc);

        JPanel infoPanel = new JPanel();
        infoPanel.setBackground(new Color(30, 30, 50));

        JPanel aboutPanel = new JPanel(new BorderLayout());
        aboutPanel.setBackground(new Color(30, 30, 50));
        JPanel termsPanel = new JPanel();
        termsPanel.setLayout(new BoxLayout(termsPanel, BoxLayout.Y_AXIS));
        termsPanel.setBackground(new Color(30, 30, 50));

        JLabel termsLabel = new JLabel("Terms of Use");
        termsLabel.setForeground(new Color(211, 211, 211));
        termsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        JButton toggleButton = new JButton("Show/Hide");
        toggleButton.setForeground(new Color(211, 211, 211));
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBackground(new Color(30, 30, 50));
        headerPanel.add(termsLabel);
        headerPanel.add(toggleButton);

        JTextArea termsText = new JTextArea(
            "By using CRM Plus, you acknowledge that:\n" +
            "- The software is provided as-is, with no guarantees of functionality or uptime.\n" +
            "- Any data entered is the user's responsibility, and CRM Plus does not store or manage personal information beyond what is necessary for operation.\n" +
            "- The developers reserve the right to update or modify the software at any time.\n\n" +
            "The Provider reserves the right to revoke access to CRM Plus at any time if misuse, violations, or actions that compromise the intended use of the software are detected. This decision is made solely at the Provider’s discretion to ensure compliance and proper usage of the application.\n\n" +
            "Users of CRM Plus agree to comply with all applicable laws and regulations while using the software. The Provider does not condone or support any use of CRM Plus that violates legal, ethical, or platform-specific policies."
        );
        termsText.setEditable(false);
        termsText.setFont(new Font("Arial", Font.PLAIN, 12));
        termsText.setForeground(new Color(211, 211, 211));
        termsText.setBackground(new Color(30, 30, 50));
        termsText.setLineWrap(true);
        termsText.setWrapStyleWord(true);
        termsText.setVisible(false);
        JScrollPane termsScroll = new JScrollPane(termsText);
        termsScroll.setPreferredSize(new Dimension(550, 400));

        toggleButton.addActionListener(e -> termsText.setVisible(!termsText.isVisible()));

        termsPanel.add(headerPanel);
        termsPanel.add(termsScroll);
        aboutPanel.add(termsPanel, BorderLayout.CENTER);

        tabbedPane.addTab("Home", homePanel);
        tabbedPane.addTab("Information", infoPanel);
        tabbedPane.addTab("About CRM Plus", aboutPanel);

        frame.add(tabbedPane);
        frame.setVisible(true);
    }

    private void checkSetup() {
        log("Checking setup...");
        File proxyScriptFile = new File(PROXY_SCRIPT);
        if (proxyScriptFile.exists()) {
            log("OUTPUT: Proxy script found at " + PROXY_SCRIPT);
        } else {
            log("ERROR: Proxy script not found at " + PROXY_SCRIPT + ". Expected: " + proxyScriptFile.getAbsolutePath());
        }
        File mitmproxyFile = new File(MITMPROXY_PATH);
        if (mitmproxyFile.exists()) {
            log("OUTPUT: MITMproxy found at " + MITMPROXY_PATH);
        } else {
            log("ERROR: MITMproxy not found at " + MITMPROXY_PATH + ". Expected: " + mitmproxyFile.getAbsolutePath());
        }
    }

    private void launchFortnite() {
        updateStatus("Launching...", new Color(150, 150, 50));
        if (!isProxyRunning) {
            startProxy();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                log("ERROR: Launch interrupted: " + e.getMessage());
            }
        }
        if (isProxyRunning) {
            try {
                String fortnitePath = fortnitePathField.getText();
                File fortniteFile = new File(fortnitePath);
                if (!fortniteFile.exists()) {
                    log("ERROR: Fortnite not found at " + fortnitePath + ". Expected: " + fortniteFile.getAbsolutePath());
                    updateStatus("Error", new Color(150, 50, 50));
                    return;
                }

                ProcessBuilder pb = new ProcessBuilder(fortnitePath);
                Map<String, String> env = pb.environment();
                env.put("http_proxy", "http://localhost:8080");
                env.put("https_proxy", "http://localhost:8080");
                env.put("NO_PROXY", "127.0.0.1,localhost");

                Process fortniteProcess = pb.start();
                log("OUTPUT: Fortnite launched with PID " + fortniteProcess.pid());
                updateStatus("Running", new Color(50, 150, 50));
                launchButton.setEnabled(false);
            } catch (IOException ex) {
                log("ERROR: Failed to launch Fortnite: " + ex.getMessage());
                updateStatus("Error", new Color(150, 50, 50));
            }
        } else {
            updateStatus("Proxy Failed", new Color(150, 50, 50));
        }
    }

    private void startProxy() {
        try {
            File proxyScriptFile = new File(PROXY_SCRIPT);
            File mitmproxyFile = new File(MITMPROXY_PATH);
            if (!proxyScriptFile.exists()) throw new IOException("proxy script not found at " + proxyScriptFile.getAbsolutePath());
            if (!mitmproxyFile.exists()) throw new IOException("mitmproxy.exe not found at " + mitmproxyFile.getAbsolutePath());

            log("OUTPUT: Starting MITMproxy...");

            Map<String, Boolean> config = new HashMap<>();
            config.put("enableAllSkins", cosmeticChecks[0].isSelected());
            config.put("enableAllBackblings", cosmeticChecks[1].isSelected());
            config.put("enableAllPickaxes", cosmeticChecks[2].isSelected());
            config.put("enableAllEmotes", cosmeticChecks[3].isSelected());
            config.put("enableAllWraps", cosmeticChecks[4].isSelected());
            Files.writeString(new File(BASE_DIR + "\\config.json").toPath(), new JSONObject(config).toString());
            log("OUTPUT: Config written to " + BASE_DIR + "\\config.json");

            ProcessBuilder pb = new ProcessBuilder(
                MITMPROXY_PATH,
                "-s", PROXY_SCRIPT,
                "--set", "block_global=false"
            );
            pb.directory(new File(BASE_DIR));
            proxyProcess = pb.start();

            log("OUTPUT: MITMproxy started with PID " + proxyProcess.pid());

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proxyProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String output = line;
                        SwingUtilities.invokeLater(() -> {
                            log("OUTPUT: " + output);
                            if (output.contains("Proxy server started")) {
                                isProxyRunning = true;
                                stopButton.setVisible(true);
                            }
                        });
                    }
                } catch (IOException ex) {
                    log("ERROR: MITMproxy output error: " + ex.getMessage());
                }
            }).start();

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proxyProcess.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log("ERROR: " + line);
                    }
                } catch (IOException ex) {
                    log("ERROR: MITMproxy error stream: " + ex.getMessage());
                }
            }).start();

        } catch (IOException ex) {
            log("ERROR: Failed to start MITMproxy: " + ex.getMessage());
            updateStatus("Error", new Color(150, 50, 50));
        }
    }

    private void stopProxy() {
        if (proxyProcess != null && proxyProcess.isAlive()) {
            proxyProcess.destroy();
            proxyProcess = null;
            isProxyRunning = false;
            log("OUTPUT: MITMproxy stopped");
            stopButton.setVisible(false);
            launchButton.setEnabled(true);
            updateStatus("Ready", new Color(50, 150, 50));
        }
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logsArea.append(message + "\n");
            logsArea.setCaretPosition(logsArea.getDocument().getLength());
        });
    }

    public void updateStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            statusLabel.setBackground(color);
        });
    }

    public JFrame getFrame() {
        return frame;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CRMPlus::new);
    }
}