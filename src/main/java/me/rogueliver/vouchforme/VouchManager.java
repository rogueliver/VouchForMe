package me.rogueliver.vouchforme;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VouchManager {

    private final VouchForMePlugin plugin;
    private Connection connection;

    // In-memory caching
    private final Map<UUID, Map<UUID, VouchEntry>> vouchCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldownCache = new ConcurrentHashMap<>();

    public VouchManager(VouchForMePlugin plugin) {
        this.plugin = plugin;
        initializeDatabase();
        loadDataIntoCache();
    }

    private void initializeDatabase() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();

            String dbPath = new File(dataFolder, "vouches.db").getAbsolutePath();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTables();
            plugin.getLogger().info("SQLite database initialized successfully!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize SQLite database: " + e.getMessage());
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

    private void loadDataIntoCache() {
        try {
            String selectVouches = "SELECT voucher_uuid, target_uuid, is_active, reason, timestamp FROM vouches";
            try (PreparedStatement stmt = connection.prepareStatement(selectVouches);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID voucherUuid = UUID.fromString(rs.getString("voucher_uuid"));
                    UUID targetUuid = UUID.fromString(rs.getString("target_uuid"));
                    boolean isActive = rs.getBoolean("is_active");
                    String reason = rs.getString("reason");
                    long timestamp = rs.getLong("timestamp");

                    VouchEntry entry = new VouchEntry(voucherUuid, targetUuid, isActive, reason, timestamp);
                    vouchCache.computeIfAbsent(targetUuid, k -> new ConcurrentHashMap<>())
                            .put(voucherUuid, entry);
                }
            }

            String selectCooldowns = "SELECT player_uuid, last_vouch_time FROM cooldowns";
            try (PreparedStatement stmt = connection.prepareStatement(selectCooldowns);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                    cooldownCache.put(playerUuid, rs.getLong("last_vouch_time"));
                }
            }

            plugin.getLogger().info("Loaded vouch data from database successfully!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load data from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add vouch
    public void addVouch(UUID voucherUuid, UUID targetUuid, String reason) {
        addOrUpdateVouch(voucherUuid, targetUuid, true, reason);
    }

    // Add devouch
    public void addDevouch(UUID voucherUuid, UUID targetUuid, String reason) {
        addOrUpdateVouch(voucherUuid, targetUuid, false, reason);
    }

    // Internal helper to insert/update vouch/devouch
    private void addOrUpdateVouch(UUID voucherUuid, UUID targetUuid, boolean active, String reason) {
        long timestamp = System.currentTimeMillis();
        try {
            String sql = """
                INSERT OR REPLACE INTO vouches (voucher_uuid, target_uuid, is_active, reason, timestamp)
                VALUES (?, ?, ?, ?, ?)
                """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, voucherUuid.toString());
                stmt.setString(2, targetUuid.toString());
                stmt.setBoolean(3, active);
                stmt.setString(4, reason);
                stmt.setLong(5, timestamp);
                stmt.executeUpdate();
            }

            updateCooldown(voucherUuid, timestamp);

            VouchEntry entry = new VouchEntry(voucherUuid, targetUuid, active, reason, timestamp);
            vouchCache.computeIfAbsent(targetUuid, k -> new ConcurrentHashMap<>())
                    .put(voucherUuid, entry);
            cooldownCache.put(voucherUuid, timestamp);

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add vouch/devouch to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Remove any vouch/devouch
    public void removeVouch(UUID voucherUuid, UUID targetUuid, String reason) {
        long timestamp = System.currentTimeMillis();
        try {
            String sql = """
                INSERT OR REPLACE INTO vouches (voucher_uuid, target_uuid, is_active, reason, timestamp)
                VALUES (?, ?, ?, ?, ?)
                """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, voucherUuid.toString());
                stmt.setString(2, targetUuid.toString());
                stmt.setBoolean(3, false); // inactive
                stmt.setString(4, reason);
                stmt.setLong(5, timestamp);
                stmt.executeUpdate();
            }

            VouchEntry entry = new VouchEntry(voucherUuid, targetUuid, false, reason, timestamp);
            Map<UUID, VouchEntry> targetVouches = vouchCache.get(targetUuid);
            if (targetVouches != null) {
                targetVouches.put(voucherUuid, entry);
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove vouch/devouch from database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateCooldown(UUID playerUuid, long timestamp) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO cooldowns (player_uuid, last_vouch_time)
            VALUES (?, ?)
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
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

    public boolean hasDevouched(UUID voucherUuid, UUID targetUuid) {
        Map<UUID, VouchEntry> targetVouches = vouchCache.get(targetUuid);
        if (targetVouches != null) {
            VouchEntry entry = targetVouches.get(voucherUuid);
            return entry != null && !entry.isActive();
        }
        return false;
    }

    public List<VouchEntry> getVouchesFor(UUID targetUuid) {
        Map<UUID, VouchEntry> targetVouches = vouchCache.get(targetUuid);
        if (targetVouches != null) return new ArrayList<>(targetVouches.values());
        return new ArrayList<>();
    }

    public long getCooldownRemaining(UUID playerUuid) {
        Long lastTime = cooldownCache.get(playerUuid);
        if (lastTime == null) return 0;

        long cooldownDuration = plugin.getConfig().getLong("cooldown.duration", 2592000); // default 30 days
        long elapsed = (System.currentTimeMillis() - lastTime) / 1000;
        return Math.max(0, cooldownDuration - elapsed);
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void reloadData() {
        vouchCache.clear();
        cooldownCache.clear();
        loadDataIntoCache();
    }
}
