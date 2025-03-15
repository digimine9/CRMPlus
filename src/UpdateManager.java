import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.json.JSONException;
import org.json.JSONObject;

public class UpdateManager {
    // Application version - should match the released version
    private static final String VERSION = "1.3"; // Updated to match CRMPlus.java
    
    // Correct URL format for GitHub raw content
    private static final String UPDATE_URL = "https://raw.githubusercontent.com/digimine9/CRMPlus/main/crmplus.json";
    
    // Connection timeouts
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;
    
    // Reference to main application
    private final CRMPlus mainApp;
    
    // Thread pool for background tasks
    private final ExecutorService executorService;

    public UpdateManager(CRMPlus mainApp) {
        this.mainApp = mainApp;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Checks for updates in the background
     */
    public void checkForUpdates() {
        mainApp.log("Checking for updates (current version: " + VERSION + ")...");
        
        CompletableFuture.runAsync(() -> {
            try {
                UpdateInfo updateInfo = fetchUpdateInfo();
                
                if (updateInfo != null) {
                    if (isNewerVersion(VERSION, updateInfo.version)) {
                        SwingUtilities.invokeLater(() -> {
                            if (updateInfo.majorUpdate) {
                                showMajorUpdateNotification(updateInfo);
                            } else {
                                showMinorUpdateNotification(updateInfo);
                            }
                        });
                    } else {
                        mainApp.log("Your CRM Plus is up to date (v" + VERSION + ")");
                    }
                }
            } catch (Exception e) {
                mainApp.log("ERROR: Update check failed: " + e.getMessage());
            }
        }, executorService);
    }

    /**
     * Fetches update information from the server
     */
    private UpdateInfo fetchUpdateInfo() {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(UPDATE_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            int responseCode = connection.getResponseCode();
            mainApp.log("Update server response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readResponseFromConnection(connection);
                mainApp.log("Update check response received");
                
                return parseUpdateInfo(response);
            } else {
                mainApp.log("ERROR: Update server returned code " + responseCode);
            }
        } catch (IOException e) {
            mainApp.log("ERROR: Connection to update server failed: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    /**
     * Reads response from HTTP connection
     */
    private String readResponseFromConnection(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * Parses update information from JSON response
     */
    private UpdateInfo parseUpdateInfo(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            
            UpdateInfo info = new UpdateInfo();
            info.version = json.getString("version");
            info.majorUpdate = json.getBoolean("majorUpdate");
            info.downloadUrl = json.getString("downloadUrl");
            info.patchNotes = json.optString("patchNotes", "");
            
            mainApp.log("Update info parsed: v" + info.version + 
                       " (Major update: " + info.majorUpdate + ")");
            return info;
        } catch (JSONException e) {
            mainApp.log("ERROR: Failed to parse update info: " + e.getMessage());
            return null;
        }
    }

    /**
     * Compares version strings
     * @return true if remoteVersion is newer than localVersion
     */
    private boolean isNewerVersion(String localVersion, String remoteVersion) {
        try {
            String[] localParts = localVersion.split("\\.");
            String[] remoteParts = remoteVersion.split("\\.");
            
            int length = Math.max(localParts.length, remoteParts.length);
            
            for (int i = 0; i < length; i++) {
                int localPart = i < localParts.length ? Integer.parseInt(localParts[i]) : 0;
                int remotePart = i < remoteParts.length ? Integer.parseInt(remoteParts[i]) : 0;
                
                if (remotePart > localPart) {
                    return true;
                } else if (remotePart < localPart) {
                    return false;
                }
            }
            
            // If we get here, versions are identical
            return false;
        } catch (NumberFormatException e) {
            mainApp.log("ERROR: Invalid version format: " + e.getMessage());
            // Default to string comparison if version parsing fails
            return !localVersion.equals(remoteVersion) && 
                   remoteVersion.compareTo(localVersion) > 0;
        }
    }

    /**
     * Shows notification for major updates that require manual installation
     */
    private void showMajorUpdateNotification(UpdateInfo updateInfo) {
        mainApp.log("Major update available: v" + updateInfo.version);
        
        StringBuilder message = new StringBuilder();
        message.append("A new major update (v").append(updateInfo.version).append(") is available!\n\n");
        
        if (!updateInfo.patchNotes.isEmpty()) {
            message.append("What's new:\n").append(updateInfo.patchNotes).append("\n\n");
        }
        
        message.append("Would you like to visit the download page?");
        
        int option = JOptionPane.showConfirmDialog(
            mainApp.getFrame(),
            message.toString(),
            "Major Update Available",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE
        );
        
        if (option == JOptionPane.YES_OPTION) {
            openWebPage(updateInfo.downloadUrl);
        }
    }

    /**
     * Shows notification for minor updates that can be automatically installed
     */
    private void showMinorUpdateNotification(UpdateInfo updateInfo) {
        mainApp.log("Minor update available: v" + updateInfo.version);
        
        StringBuilder message = new StringBuilder();
        message.append("A new update (v").append(updateInfo.version).append(") is available!\n\n");
        
        if (!updateInfo.patchNotes.isEmpty()) {
            message.append("What's new:\n").append(updateInfo.patchNotes).append("\n\n");
        }
        
        message.append("Would you like to download and install it now?");
        
        int option = JOptionPane.showConfirmDialog(
            mainApp.getFrame(),
            message.toString(),
            "Update Available",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE
        );
        
        if (option == JOptionPane.YES_OPTION) {
            downloadAndApplyUpdate(updateInfo.downloadUrl);
        }
    }

    /**
     * Opens the default web browser to the specified URL
     */
    private void openWebPage(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            mainApp.log("ERROR: Failed to open download URL: " + e.getMessage());
            JOptionPane.showMessageDialog(
                mainApp.getFrame(),
                "Failed to open download page. Please visit:\n" + url,
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Downloads and applies the update
     */
    private void downloadAndApplyUpdate(String downloadUrl) {
        mainApp.updateStatus("Downloading update...", new java.awt.Color(150, 150, 50));
        
        CompletableFuture.runAsync(() -> {
            HttpURLConnection connection = null;
            try {
                mainApp.log("Downloading update from: " + downloadUrl);
                
                // Determine the appropriate file name based on the current executable
                String currentExecutableName = getCurrentExecutableName();
                String newExecutableName = getNewExecutableName(currentExecutableName);
                
                File updateFile = new File(newExecutableName);
                URL url = new URL(downloadUrl);
                
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                connection.setReadTimeout(30000); // Longer timeout for download
                
                // Show progress if content length is available
                int contentLength = connection.getContentLength();
                if (contentLength > 0) {
                    downloadWithProgress(connection, updateFile, contentLength);
                } else {
                    // Fall back to simple download if content length is unknown
                    Files.copy(
                        connection.getInputStream(),
                        updateFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    );
                }
                
                mainApp.log("Update downloaded successfully to " + updateFile.getAbsolutePath());
                
                // Create update script if on Windows
                if (isWindows()) {
                    createUpdateScript(currentExecutableName, newExecutableName);
                }
                
                SwingUtilities.invokeLater(() -> {
                    mainApp.updateStatus("Update downloaded!", new java.awt.Color(50, 150, 50));
                    showUpdateCompleteMessage(newExecutableName);
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
        }, executorService);
    }

    /**
     * Downloads file with progress tracking
     */
    private void downloadWithProgress(HttpURLConnection connection, File destination, int contentLength) throws IOException {
        final int bufferSize = 8192;
        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        int totalBytesRead = 0;
        int lastProgressUpdate = 0;
        
        try (java.io.InputStream in = connection.getInputStream();
             java.io.FileOutputStream out = new java.io.FileOutputStream(destination)) {
            
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                
                // Update progress every 5%
                int progressPercentage = (int) ((totalBytesRead * 100.0) / contentLength);
                if (progressPercentage >= lastProgressUpdate + 5) {
                    lastProgressUpdate = progressPercentage;
                    final int progress = progressPercentage;
                    SwingUtilities.invokeLater(() -> {
                        mainApp.updateStatus("Downloading: " + progress + "%", new java.awt.Color(150, 150, 50));
                    });
                }
            }
        }
    }

    /**
     * Gets the name of the current executable
     */
    private String getCurrentExecutableName() {
        String defaultName = "CRMPlus.exe";
        try {
            String jarPath = new File(
                UpdateManager.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
            ).getPath();
            
            if (jarPath.endsWith(".exe")) {
                return new File(jarPath).getName();
            }
        } catch (Exception e) {
            mainApp.log("Could not determine executable name: " + e.getMessage());
        }
        return defaultName;
    }

    /**
     * Generates a new executable name based on the current one
     */
    private String getNewExecutableName(String currentName) {
        if (currentName.contains(".")) {
            int lastDot = currentName.lastIndexOf(".");
            return currentName.substring(0, lastDot) + "_new" + currentName.substring(lastDot);
        } else {
            return currentName + "_new";
        }
    }

    /**
     * Creates a batch script to complete the update on Windows
     */
    private void createUpdateScript(String currentExecutable, String newExecutable) {
        try {
            String scriptContent = 
                "@echo off\r\n" +
                "echo Updating CRM Plus...\r\n" +
                "ping -n 3 127.0.0.1 > nul\r\n" +
                "if exist \"" + currentExecutable + "\" (\r\n" +
                "    del \"" + currentExecutable + "\"\r\n" +
                ")\r\n" +
                "if exist \"" + newExecutable + "\" (\r\n" +
                "    rename \"" + newExecutable + "\" \"" + currentExecutable + "\"\r\n" +
                "    start \"\" \"" + currentExecutable + "\"\r\n" +
                ")\r\n" +
                "del \"%~f0\"\r\n";
            
            Files.write(Paths.get("update_crm.bat"), scriptContent.getBytes());
            mainApp.log("Created update script: update_crm.bat");
        } catch (IOException e) {
            mainApp.log("ERROR: Failed to create update script: " + e.getMessage());
        }
    }

    /**
     * Shows the update complete message
     */
    private void showUpdateCompleteMessage(String newExecutableName) {
        String message;
        if (isWindows()) {
            message = "Update downloaded successfully!\n\n" +
                     "Would you like to restart the application now to complete the update?";
            
            int option = JOptionPane.showConfirmDialog(
                mainApp.getFrame(),
                message,
                "Update Complete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
            );
            
            if (option == JOptionPane.YES_OPTION) {
                try {
                    // Execute the update script
                    Runtime.getRuntime().exec("cmd /c start update_crm.bat");
                    System.exit(0);
                } catch (IOException e) {
                    mainApp.log("ERROR: Failed to execute update script: " + e.getMessage());
                    JOptionPane.showMessageDialog(
                        mainApp.getFrame(),
                        "Failed to apply update: " + e.getMessage(),
                        "Update Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        } else {
            // For non-Windows platforms
            message = "Update downloaded successfully to: " + newExecutableName + "\n\n" +
                     "Please close the application and run the new version manually.";
            
            JOptionPane.showMessageDialog(
                mainApp.getFrame(),
                message,
                "Update Complete",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    /**
     * Checks if running on Windows
     */
    private boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    /**
     * Cleans up resources before application exit
     */
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * Inner class to hold update information
     */
    private static class UpdateInfo {
        String version;
        boolean majorUpdate;
        String downloadUrl;
        String patchNotes;
    }
}