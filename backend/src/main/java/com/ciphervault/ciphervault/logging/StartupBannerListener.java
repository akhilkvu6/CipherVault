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

    public StartupBannerListener(
            @org.springframework.beans.factory.annotation.Autowired(required = false) DataSource dataSource,
            Environment environment) {
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        int port = 8080;
        try {
            String portProp = environment.getProperty("local.server.port");
            if (portProp == null) {
                portProp = environment.getProperty("server.port", "8080");
            }
            port = Integer.parseInt(portProp);
        } catch (Exception ignored) {
        }

        String host = resolveHostIp();
        String javaVersion = System.getProperty("java.version");
        String springBootVersion = SpringBootVersion.getVersion();

        boolean dbConnected = false;
        String dbEngine = "MySQL";
        String dbVersion = "";
        String dbCatalog = "ciphervault";

        if (dataSource != null) {
            try (Connection conn = dataSource.getConnection()) {
                dbConnected = true;
                DatabaseMetaData meta = conn.getMetaData();
                dbEngine = meta.getDatabaseProductName();
                dbVersion = meta.getDatabaseProductVersion();
                String catalog = conn.getCatalog();
                if (catalog != null && !catalog.isBlank()) {
                    dbCatalog = catalog;
                }
            } catch (Exception e) {
                dbConnected = false;
            }
        }

        List<String> warnings = new ArrayList<>();
        if (jwtSecret == null || jwtSecret.isBlank()) {
            warnings.add("Development JWT fallback is active. Configure CIPHERVAULT_JWT_SECRET for production.");
        }
        if (masterKey == null || masterKey.isBlank()) {
            warnings.add("Development master key fallback is active. Configure CIPHERVAULT_MASTER_KEY for production.");
        }
        if (pbkdf2Salt == null || pbkdf2Salt.isBlank()) {
            warnings.add("Development PBKDF2 salt fallback is active. Configure CIPHERVAULT_PBKDF2_SALT for production.");
        }

        ConsoleLogger.printStartupBanner(
                host,
                port,
                envMode != null ? envMode.toUpperCase() : "DEVELOPMENT",
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
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip;
                        }
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "192.168.1.38";
        }
    }
}
