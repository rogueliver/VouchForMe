package me.rogueliver.vfm.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vouch {
    private UUID targetUuid;
    private String targetName;
    private UUID senderUuid;
    private String senderName;
    private VouchType type;
    private String reason;
    private LocalDateTime createdAt;
}
