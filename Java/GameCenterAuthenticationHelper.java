import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link GameCenterAuthenticationHelper} implements server-side identity verification @see
 * <a href="https://developer.apple.com/reference/gamekit/gklocalplayer/1515407-generateidentityverificationsign?language=objc">https://developer.apple.com/reference/gamekit/gklocalplayer/1515407-generateidentityverificationsign?language=objc</a>
 *
 * @author Johno Crawford (johno@hellface.com)
 */
@Component
public class GameCenterAuthenticationHelper {

    private static final Logger logger = LogManager.getLogger(GameCenterAuthenticationHelper.class);

    private static final String APPLE_HOST_SUFFIX = "apple.com";

    private final ConcurrentMap<String, Certificate> cache = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        try {
            String publicKeyUrl = "https://static.gc.apple.com/public-key/gc-prod-3.cer";
            cache.put(publicKeyUrl, loadCertificate(publicKeyUrl));
        } catch (CertificateException | IOException e) {
            logger.error("Failed to load certificate, Game Center authentication may fail", e);
        }
    }

    public boolean isAuthenticated(String publicKeyUrl, String playerId, String bundleId, String timestamp, String signature, String salt) {
        if (logger.isDebugEnabled()) {
            logger.debug("Authenticating with " + publicKeyUrl + ", timestamp " + timestamp + ", signature " + signature + ", salt " + salt + ", playerId " + playerId + ", bundle " + bundleId);
        }
        try {
            URL url = new URL(publicKeyUrl);
            if (!url.getHost().endsWith(APPLE_HOST_SUFFIX)) {
                logger.warn(publicKeyUrl + " is not trusted!");
                return false;
            }

            byte[] decodedSalt = Base64.getDecoder().decode(salt);
            byte[] decodedSignature = Base64.getDecoder().decode(signature);
    
            Certificate certificate = cache.computeIfAbsent(getCacheKeyFromUrl(publicKeyUrl), (k) -> {
                try {
                    return loadCertificate(k);
                } catch (CertificateException | IOException e) {
                    throw new RuntimeException(e);
                }
            });

            ByteBuffer tsByteBuffer = ByteBuffer.allocate(8);
            tsByteBuffer.order(ByteOrder.BIG_ENDIAN);
            tsByteBuffer.putLong(Long.parseUnsignedLong(timestamp));

            byte[] payload = concat(playerId.getBytes(StandardCharsets.UTF_8), bundleId.getBytes(StandardCharsets.UTF_8), tsByteBuffer.array(), decodedSalt);
            Signature validatingSignature = Signature.getInstance("SHA256withRSA");
            validatingSignature.initVerify(certificate);
            validatingSignature.update(payload);
            boolean ok = validatingSignature.verify(decodedSignature);
            if (!ok) {
                logger.warn("Signature validation failed with " + publicKeyUrl + ", timestamp " + timestamp + ", signature " + signature + ", salt " + salt + ", playerId " + playerId + ", bundle " + bundleId);
            }
            return ok;
        } catch (Throwable e) {
            logger.error("Game Center authentication failed with " + publicKeyUrl + ", timestamp " + timestamp + ", signature " + signature + ", salt " + salt + ", playerId " + playerId + ", bundle " + bundleId, e);
        }
        return false;
    }
    
    private String getCacheKeyFromUrl(String url) {
        int index = url.indexOf("?");
        if (index != -1) {
            return url.substring(0, index);
        }
        return url;
    }

    private static byte[] concat(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }

        byte[] result = new byte[totalLength];

        int currentIndex = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, currentIndex, array.length);
            currentIndex += array.length;
        }

        return result;
    }

    private Certificate loadCertificate(String publicKeyUrl) throws CertificateException, IOException {
        int retryCount = 0;
        while (true) {
            try {
                URLConnection connection = new URL(publicKeyUrl).openConnection();
                connection.setConnectTimeout(6000);
                connection.setReadTimeout(6000);
                connection.setUseCaches(false);
                return CertificateFactory.getInstance("X.509").generateCertificate(new BufferedInputStream(connection.getInputStream()));
            } catch (IOException e) {
                if (retryCount++ > 2) {
                    throw e;
                }
            }
        }
    }
}
