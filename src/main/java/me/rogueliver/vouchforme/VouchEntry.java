package me.rogueliver.vouchforme;

import java.util.UUID;

public class VouchEntry {
    private final UUID voucherUuid;
    private final UUID targetUuid;
    private final boolean active;
    private final String reason;
    private final long timestamp;
    
    public VouchEntry(UUID voucherUuid, UUID targetUuid, boolean active, String reason, long timestamp) {
        this.voucherUuid = voucherUuid;
        this.targetUuid = targetUuid;
        this.active = active;
        this.reason = reason;
        this.timestamp = timestamp;
    }
    
    public UUID getVoucherUuid() {
        return voucherUuid;
    }
    
    public UUID getTargetUuid() {
        return targetUuid;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public String getReason() {
        return reason;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}