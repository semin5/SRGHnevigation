package sarangit.semin5.nev;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DesignatedDeviceService {
    private final DesignatedDeviceRepository repository;
    private final SecureRandom random = new SecureRandom();

    public DesignatedDeviceService(DesignatedDeviceRepository repository) { this.repository = repository; }

    public record Registration(String secret, String hash) {}
    public record DeviceView(UUID id, boolean thisDevice, String ipAddress, String userAgent) {}
    public record Status(boolean enabled, boolean thisDevice, List<DeviceView> devices) {}

    @Transactional
    public Registration designate(String username, String existingSecret, String userAgent, String ipAddress) {
        String existingHash = matchingHash(username, existingSecret);
        if (existingHash != null) return new Registration(existingSecret, existingHash);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = hash(secret);
        DesignatedDevice device = new DesignatedDevice();
        device.setUsername(username);
        device.setTokenHash(hash);
        device.setUserAgent(limit(userAgent, 512));
        device.setIpAddress(limit(ipAddress, 64));
        device.setRegisteredAt(Instant.now());
        repository.saveAndFlush(device);
        return new Registration(secret, hash);
    }

    @Transactional
    public void removeCurrent(String username, String secret) {
        String tokenHash = matchingHash(username, secret);
        if (tokenHash != null) repository.findByUsernameAndTokenHash(username, tokenHash).ifPresent(repository::delete);
    }

    @Transactional
    public boolean removeSelected(String username, List<UUID> ids, String currentSecret) {
        String currentHash = matchingHash(username, currentSecret);
        boolean currentRemoved = false;
        for (DesignatedDevice device : repository.findAllById(ids.stream().distinct().toList())) {
            if (!username.equals(device.getUsername())) continue;
            currentRemoved |= device.getTokenHash().equals(currentHash);
            repository.delete(device);
        }
        return currentRemoved;
    }

    @Transactional(readOnly = true)
    public String matchingHash(String username, String secret) {
        if (secret == null || !secret.matches("[A-Za-z0-9_-]{43}")) return null;
        String tokenHash = hash(secret);
        return repository.findByUsernameAndTokenHash(username, tokenHash)
                .map(DesignatedDevice::getTokenHash)
                .filter(stored -> MessageDigest.isEqual(stored.getBytes(StandardCharsets.US_ASCII),
                        tokenHash.getBytes(StandardCharsets.US_ASCII))).orElse(null);
    }

    @Transactional(readOnly = true)
    public Status status(String username, String secret) {
        String currentHash = matchingHash(username, secret);
        List<DeviceView> entries = repository.findByUsernameOrderByRegisteredAtDesc(username).stream()
                .map(device -> new DeviceView(device.getId(), device.getTokenHash().equals(currentHash),
                        device.getIpAddress(), device.getUserAgent())).toList();
        return new Status(!entries.isEmpty(), currentHash != null, entries);
    }

    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    private String limit(String value, int max) {
        if (value == null) return null;
        return value.substring(0, Math.min(value.length(), max));
    }
}
