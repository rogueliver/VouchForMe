package me.rogueliver.vfm.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VouchMessage {
    private String action;
    private Vouch vouch;
}
