package fr.xephi.authme.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.maxmind.db.GeoIp2Provider;
import com.maxmind.db.Reader;
import com.maxmind.db.Reader.FileMode;
import com.maxmind.db.cache.CHMCache;
import com.maxmind.db.model.Country;
import com.maxmind.db.model.CountryResponse;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.InternetProtocolUtils;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

public class GeoIpService {

    private static final String LICENSE =
            "[LICENSE] This product includes GeoLite2 data created by MaxMind, available at https://www.maxmind.com";

    private static final String DATABASE_NAME = "GeoLite2-Country";
    private static final String DATABASE_FILE = DATABASE_NAME + ".mmdb";
    private final ConsoleLogger logger = ConsoleLoggerFactory.get(GeoIpService.class);
    private final Path dataFile;
    private GeoIp2Provider databaseReader;
    private volatile boolean downloading;

    @Inject
    GeoIpService(@DataFolder File dataFolder) {
        this.dataFile = dataFolder.toPath().resolve(DATABASE_FILE);

        // Fires download of recent data or the initialization of the look up service
        isDataAvailable();
    }

    @VisibleForTesting
    GeoIpService(@DataFolder File dataFolder, GeoIp2Provider reader) {
        this.dataFile = dataFolder.toPath().resolve(DATABASE_FILE);

        this.databaseReader = reader;
    }

    /**
     * Download (if absent or old) the GeoIpLite data file and then try to load it.
     *
     * @return True if the data is available, false otherwise.
     */
    private synchronized boolean isDataAvailable() {
        if (downloading) {
            // we are currently downloading the database
            return false;
        }

        if (databaseReader != null) {
            // everything is initialized
            return true;
        }

        if (Files.exists(dataFile)) {
            try {
                startReading();
                // don't fire the update task - we are up to date
                return true;
            } catch (IOException ioEx) {
                logger.logException("Failed to load GeoLiteAPI database", ioEx);
                return false;
            }
        }
        // File is outdated or doesn't exist - let's try to download the data file!
        // use bukkit's cached threads
        return false;
    }


    private void startReading() throws IOException {
        databaseReader = new Reader(dataFile.toFile(), FileMode.MEMORY, new CHMCache());
        logger.info(LICENSE);

        // clear downloading flag, because we now have working reader instance
        downloading = false;
    }

    /**
     * Get the country code of the given IP address.
     *
     * @param ip textual IP address to lookup.
     * @return two-character ISO 3166-1 alpha code for the country, "LOCALHOST" for local addresses
     *         or "--" if it cannot be fetched.
     */
    public String getCountryCode(String ip) {
        if (InternetProtocolUtils.isLocalAddress(ip)) {
            return "LOCALHOST";
        }
        return getCountry(ip).map(Country::getIsoCode).orElse("--");
    }

    /**
     * Get the country name of the given IP address.
     *
     * @param ip textual IP address to lookup.
     * @return The name of the country, "LocalHost" for local addresses, or "N/A" if it cannot be fetched.
     */
    public String getCountryName(String ip) {
        if (InternetProtocolUtils.isLocalAddress(ip)) {
            return "LocalHost";
        }
        return getCountry(ip).map(Country::getName).orElse("N/A");
    }

    /**
     * Get the country of the given IP address
     *
     * @param ip textual IP address to lookup
     * @return the wrapped Country model or {@link Optional#empty()} if
     *   <ul>
     *     <li>Database reader isn't initialized</li>
     *     <li>MaxMind has no record about this IP address</li>
     *     <li>IP address is local</li>
     *     <li>Textual representation is not a valid IP address</li>
     *   </ul>
     */
    private Optional<Country> getCountry(String ip) {
        if (ip == null || ip.isEmpty() || !isDataAvailable()) {
            return Optional.empty();
        }

        try {
            InetAddress address = InetAddress.getByName(ip);

            // Reader.getCountry() can be null for unknown addresses
            return Optional.ofNullable(databaseReader.getCountry(address)).map(CountryResponse::getCountry);
        } catch (UnknownHostException e) {
            // Ignore invalid ip addresses
            // Legacy GEO IP Database returned a unknown country object with Country-Code: '--' and Country-Name: 'N/A'
        } catch (IOException ioEx) {
            logger.logException("Cannot lookup country for " + ip + " at GEO IP database", ioEx);
        }

        return Optional.empty();
    }
}
