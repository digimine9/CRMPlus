import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class UpdateManager {
    private static final String VERSION = "1.0.2";
    private static final String UPDATE_URL = "https://raw.githubusercontent.com/digimine9/CRMPlus/main/crmplus.json";
    private final CRMPlus mainApp;

    public UpdateManager(CRMPlus mainApp) {
        this.mainApp = mainApp;
    }

    public void checkForUpdates() {
        mainApp.log("Checking for updates...");

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(UPDATE_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                mainApp.log("Response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                    mainApp.log("Raw response: " + response.toString());

                    JSONObject updateInfo = new JSONObject(response.toString());
                    String latestVersion = updateInfo.getString("version");
                    boolean majorUpdate = updateInfo.getBoolean("majorUpdate");
                    String downloadUrl = updateInfo.getString("downloadUrl");
                    String patchNotes = updateInfo.optString("patchNotes", "");

                    if (!VERSION.equals(latestVersion)) {
                        final String notes = patchNotes;
                        final String version = latestVersion;
                        final String download = downloadUrl;

                        SwingUtilities.invokeLater(() -> {
                            if (majorUpdate) {
                                showMajorUpdateNotification(version, notes, download);
                            } else {
                                showMinorUpdateNotification(version, notes, download);
                            }
                        });
                    } else {
                        mainApp.log("Your CRM Plus is up to date!");
                    }
                } else {
                    mainApp.log("ERROR: Server returned code " + responseCode);
                }
            } catch (Exception e) {
                mainApp.log("ERROR: Failed to check for updates: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void showMajorUpdateNotification(String version, String patchNotes, String downloadUrl) {
        mainApp.log("Major update available: v" + version);
        String message = "A new major update (v" + version + ") is available!\n\n";
        if (!patchNotes.isEmpty()) {
            message += "What's new:\n" + patchNotes + "\n\n";
        }
        message += "Please download the new version from our website.";
        int option = JOptionPane.showConfirmDialog(
            mainApp.getFrame(),
            message,
            "Major Update Available",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE
        );
        if (option == JOptionPane.YES_OPTION) {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URL(downloadUrl).toURI());
            } catch (Exception e) {
                mainApp.log("ERROR: Failed to open download URL: " + e.getMessage());
            }
        }
    }

    private void showMinorUpdateNotification(String version, String patchNotes, String downloadUrl) {
        mainApp.log("Minor update available: v" + version);
        String message = "A new update (v" + version + ") is available!\n\n";
        if (!patchNotes.isEmpty()) {
            message += "What's new:\n" + patchNotes + "\n\n";
        }
        message += "Would you like to install it now?";
        int option = JOptionPane.showConfirmDialog(
            mainApp.getFrame(),
            message,
            "Update Available",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE
        );
        if (option == JOptionPane.YES_OPTION) {
            downloadAndApplyUpdate(downloadUrl);
        }
    }

    private void downloadAndApplyUpdate(String downloadUrl) {
        mainApp.updateStatus("Updating...", new java.awt.Color(150, 150, 50));
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                mainApp.log("Downloading update...");
                java.io.File updateFile = new java.io.File("CRMPlus_new.exe");
                URL url = new URL(downloadUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                Files.copy(
                    connection.getInputStream(),
                    updateFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                );
                mainApp.log("Update downloaded successfully");
                SwingUtilities.invokeLater(() -> {
                    mainApp.updateStatus("Updated!", new java.awt.Color(50, 150, 50));
                    JOptionPane.showMessageDialog(
                        mainApp.getFrame(),
                        "Update downloaded successfully! Please close the app and run CRMPlus_new.exe.",
                        "Update Complete",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                });
            } catch (Exception e) {
                mainApp.log("ERROR: Failed to download update: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    mainApp.updateStatus("Update failed", new java.awt.Color(150, 50, 50));
                    JOptionPane.showMessageDialog(
                        mainApp.getFrame(),
                        "Failed to download update: " + e.getMessage(),
                        "Update Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }
}