package me.rogueliver.vouchforme.data;

import me.rogueliver.vouchforme.VouchForMePlugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VouchManager {

    private final VouchForMePlugin plugin;
    private Connection connection;

    private final Map<UUID, Map<UUID, VouchEntry>> vouchCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldownCache = new ConcurrentHashMap<>();

    public VouchManager(VouchForMePlugin plugin) {
        this.plugin = plugin;
        initializeDatabase();
        loadCache();
    }

    private void initializeDatabase() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            String dbPath = new File(dataFolder, "vouches.db").getAbsolutePath();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            createTables();
            plugin.getLogger().info("Database initialized successfully");

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        String createVouchesTable = """
            CREATE TABLE IF NOT EXISTS vouches (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                voucher_uuid TEXT NOT NULL,
                target_uuid TEXT NOT NULL,
                is_active BOOLEAN NOT NULL DEFAULT 1,
                reason TEXT,
                timestamp INTEGER NOT NULL,
                UNIQUE(voucher_uuid, target_uuid)
            )
            """;

        String createCooldownsTable = """
            CREATE TABLE IF NOT EXISTS cooldowns (
                player_uuid TEXT PRIMARY KEY,
                last_vouch_time INTEGER NOT NULL
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createVouchesTable);
            stmt.execute(createCooldownsTable);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_target_uuid ON vouches(target_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_voucher_uuid ON vouches(voucher_uuid)");
        }
    }

    private void loadCache() {
        try {
            loadVouches();
            loadCooldowns();
            plugin.getLogger().info("Cache loaded successfully");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load cache: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadVouches() throws SQLException {
        String query = "SELECT voucher_uuid, target_uuid, is_active, reason, timestamp FROM vouches";
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID voucherUuid = UUID.fromString(rs.getString("voucher_uuid"));
                UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
                boolean isActive = rs.getBoolean("is_active");
                String reason = rs.getString("reason");
                long timestamp = rs.getLong("timestamp");

                VouchEntry entry = new VouchEntry(voucherUuid, targetUuid, isActive, reason, timestamp);
                vouchCache.computeIfAbsent(targetUuid, k -> new ConcurrentHashMap<>()).put(voucherUuid, entry);
            }
        }
    }

    private void loadCooldowns() throws SQLException {
        String query = "SELECT player_uuid, last_vouch_time FROM cooldowns";
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                long lastVouchTime = rs.getLong("last_vouch_time");
                cooldownCache.put(playerUuid, lastVouchTime);
            }
        }
    }

    public void addVouch(UUID voucherUuid, UUID targetUuid, String reason) {
        long timestamp = System.currentTimeMillis();

        try {
            String query = "INSERT OR REPLACE INTO vouches (voucher_uuid, target_uuid, is_active, reason, timestamp) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, voucherUuid.toString());
                stmt.setString(2, targetUuid.toString());
                stmt.setBoolean(3, true);
                stmt.setString(4, reason);
                stmt.setLong(5, timestamp);
                stmt.executeUpdate();
            }

            updateCooldown(voucherUuid, timestamp);

            VouchEntry entry = new VouchEntry(voucherUuid, targetUuid, true, reason, timestamp);
            vouchCache.computeIfAbsent(targetUuid, k -> new ConcurrentHashMap<>()).put(voucherUuid, entry);
            cooldownCache.put(voucherUuid, timestamp);

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add vouch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removeVouch(UUID voucherUuid, UUID targetUuid, String reason) {
        long timestamp = System.currentTimeMillis();

        try {
            String query = "INSERT OR REPLACE INTO vouches (voucher_uuid, target_uuid, is_active, reason, timestamp) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, voucherUuid.toString());
                stmt.setString(2, targetUuid.toString());
                stmt.setBoolean(3, false);
                stmt.setString(4, reason);
                stmt.setLong(5, timestamp);
                stmt.executeUpdate();
            }

            boolean applyDevouchCooldown = plugin.getConfig().getBoolean("cooldown.apply-to-devouch", false);
            if (applyDevouchCooldown) {
                updateCooldown(voucherUuid, timestamp);
                cooldownCache.put(voucherUuid, timestamp);
            }

            VouchEntry entry = new VouchEntry(voucherUuid, targetUuid, false, reason, timestamp);
            Map<UUID, VouchEntry> targetVouches = vouchCache.get(targetUuid);
            if (targetVouches != null) {
                targetVouches.put(voucherUuid, entry);
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove vouch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateCooldown(UUID playerUuid, long timestamp) throws SQLException {
        String query = "INSERT OR REPLACE INTO cooldowns (player_uuid, last_vouch_time) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setLong(2, timestamp);
            stmt.executeUpdate();
        }
    }

    public boolean hasVouched(UUID voucherUuid, UUID targetUuid) {
        Map<UUID, VouchEntry> targetVouches = vouchCache.get(targetUuid);
        if (targetVouches != null) {
            VouchEntry entry = targetVouches.get(voucherUuid);
            return entry != null && entry.isActive();
        }
        return false;
    }

    public List<VouchEntry> getVouchesFor(UUID targetUuid) {
        Map<UUID, VouchEntry> targetVouches = vouchCache.get(targetUuid);
        return targetVouches != null ? new ArrayList<>(targetVouches.values()) : new ArrayList<>();
    }

    public long getCooldownRemaining(UUID playerUuid) {
        Long lastTime = cooldownCache.get(playerUuid);
        if (lastTime == null) return 0;

        long cooldownDuration = plugin.getConfig().getLong("cooldown.duration", 2592000);
        long elapsed = (System.currentTimeMillis() - lastTime) / 1000;
        long remaining = cooldownDuration - elapsed;

        return Math.max(0, remaining);
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Database connection closed");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reloadData() {
        vouchCache.clear();
        cooldownCache.clear();
        loadCache();
    }
}