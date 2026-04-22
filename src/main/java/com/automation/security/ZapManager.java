package com.automation.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zaproxy.clientapi.core.Alert;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ClientApiException;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

// Thin wrapper around the ZAP Java client API — keeps ZAP interaction out of test classes.
public class ZapManager {

    private static final Logger log = LogManager.getLogger(ZapManager.class);
    private static final int PASSIVE_SCAN_TIMEOUT_SECONDS = 60;

    private final ClientApi api;
    private final String host;
    private final int port;

    public ZapManager(String host, int port) {
        this.host = host;
        this.port = port;
        this.api  = new ClientApi(host, port);
    }

    // Quick TCP probe — cheaper than a full API call for an early skip check.
    public boolean isRunning() {
        try (Socket s = new Socket(host, port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Reset ZAP to a clean state before each scan suite.
    public void newSession() throws ClientApiException {
        api.core.newSession("", "true");
        log.info("ZAP: new session started");
    }

    // Polls until ZAP's passive scan queue drains or the timeout is reached.
    public void waitForPassiveScan() throws ClientApiException, InterruptedException {
        log.info("ZAP: waiting for passive scan to complete...");
        int elapsed = 0;
        while (elapsed < PASSIVE_SCAN_TIMEOUT_SECONDS) {
            int pending = Integer.parseInt(
                    ((ApiResponseElement) api.pscan.recordsToScan()).getValue());
            if (pending == 0) {
                log.info("ZAP: passive scan complete");
                return;
            }
            Thread.sleep(2_000);
            elapsed += 2;
        }
        log.warn("ZAP: passive scan did not complete within {}s — results may be partial",
                PASSIVE_SCAN_TIMEOUT_SECONDS);
    }

    // Returns all alerts ZAP found for URLs under the given base URL.
    public List<Alert> getAlerts(String baseUrl) throws ClientApiException {
        return api.getAlerts(baseUrl, 0, 0);
    }

    // Counts alerts matching the given risk level.
    public long countByRisk(List<Alert> alerts, Alert.Risk risk) {
        return alerts.stream().filter(a -> a.getRisk() == risk).count();
    }

    // Writes ZAP's built-in HTML report to the specified file path.
    public void exportHtmlReport(String outputPath) throws ClientApiException, IOException {
        byte[] report = api.core.htmlreport();
        Files.write(Paths.get(outputPath), report);
        log.info("ZAP: HTML report saved to {}", outputPath);
    }
}
