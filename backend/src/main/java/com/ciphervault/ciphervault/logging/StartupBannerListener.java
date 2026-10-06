package com.ciphervault.ciphervault.logging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * Listens for ApplicationReadyEvent and prints the structured ASCII CipherVault startup console banner.
 */
@Component
public class StartupBannerListener {

    private final DataSource dataSource;
    private final Environment environment;

    @Value("${ciphervault.environment:development}")
    private String envMode;

    @Value("${ciphervault.jwt.secret:}")
    private String jwtSecret;

    @Value("${ciphervault.security.master-key:}")
    private String masterKey;

    @Value("${ciphervault.security.pbkdf2.salt:}")
    private String pbkdf2Salt;

    public StartupBannerListener(DataSource dataSource, Environment environment) {
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        int port = 8080;

        try {
            String portProperty = environment.getProperty("local.server.port");

            if (portProperty == null) {
                portProperty = environment.getProperty("server.port", "8080");
            }

            port = Integer.parseInt(portProperty);
        } catch (Exception ignored) {
            // Keep the default port when the configured value is unavailable or invalid.
        }

        String host = resolveHostIp();
        String javaVersion = System.getProperty("java.version");
        String springBootVersion = SpringBootVersion.getVersion();

        boolean dbConnected = false;
        String dbEngine = "MySQL";
        String dbVersion = "";
        String dbCatalog = "ciphervault";

        try (Connection connection = dataSource.getConnection()) {
            dbConnected = true;

            DatabaseMetaData metadata = connection.getMetaData();
            dbEngine = metadata.getDatabaseProductName();
            dbVersion = metadata.getDatabaseProductVersion();

            String catalog = connection.getCatalog();
            if (catalog != null && !catalog.isBlank()) {
                dbCatalog = catalog;
            }
        } catch (Exception ignored) {
            // Database status remains disconnected when the connection check fails.
        }

        List<String> warnings = new ArrayList<>();

        if (jwtSecret == null || jwtSecret.isBlank()) {
            warnings.add(
                    "Development JWT fallback is active. Configure CIPHERVAULT_JWT_SECRET for production."
            );
        }

        if (masterKey == null || masterKey.isBlank()) {
            warnings.add(
                    "Development master key fallback is active. Configure CIPHERVAULT_MASTER_KEY for production."
            );
        }

        if (pbkdf2Salt == null || pbkdf2Salt.isBlank()) {
            warnings.add(
                    "Development PBKDF2 salt fallback is active. Configure CIPHERVAULT_PBKDF2_SALT for production."
            );
        }

        ConsoleLogger.printStartupBanner(
                host,
                port,
                envMode.toUpperCase(Locale.ROOT),
                javaVersion,
                springBootVersion,
                dbEngine,
                dbVersion,
                dbCatalog,
                dbConnected,
                warnings
        );
    }

    private String resolveHostIp() {
        try {
            Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();

            String fallbackIp = null;

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                String interfaceName =
                        networkInterface.getDisplayName().toLowerCase(Locale.ROOT);

                if (interfaceName.contains("wsl")
                        || interfaceName.contains("virtual")
                        || interfaceName.contains("hyper-v")) {
                    continue;
                }

                Enumeration<InetAddress> addresses =
                        networkInterface.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();

                    if (address instanceof Inet4Address
                            && !address.isLoopbackAddress()) {

                        String ip = address.getHostAddress();

                        if (ip.startsWith("192.168.")
                                || ip.startsWith("10.")) {
                            return ip;
                        }

                        if (fallbackIp == null) {
                            fallbackIp = ip;
                        }
                    }
                }
            }

            if (fallbackIp != null) {
                return fallbackIp;
            }

            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
            return "127.0.0.1";
        }
    }
}