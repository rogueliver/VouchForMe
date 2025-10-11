package me.rogueliver.vfm.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import me.rogueliver.vfm.VouchForMe;
import me.rogueliver.vfm.models.Vouch;
import me.rogueliver.vfm.models.VouchType;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private final VouchForMe plugin;
    @Getter
    private HikariDataSource dataSource;

    public DatabaseManager(VouchForMe plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        try {
            FileConfiguration config = plugin.getConfig();

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl("jdbc:mariadb://" +
                config.getString("database.host") + ":" +
                config.getInt("database.port") + "/" +
                config.getString("database.database"));
            hikariConfig.setUsername(config.getString("database.username"));
            hikariConfig.setPassword(config.getString("database.password"));
            hikariConfig.setMaximumPoolSize(config.getInt("database.pool-size"));
            hikariConfig.setConnectionTimeout(10000);
            hikariConfig.setLeakDetectionThreshold(60000);

            dataSource = new HikariDataSource(hikariConfig);

            createTables();

            plugin.getLogger().info("Database connected successfully!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void createTables() {
        String vouchesTable = "CREATE TABLE IF NOT EXISTS vouches (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "target_uuid VARCHAR(36) NOT NULL," +
                "target_name VARCHAR(16) NOT NULL," +
                "sender_uuid VARCHAR(36) NOT NULL," +
                "sender_name VARCHAR(16) NOT NULL," +
                "type VARCHAR(10) NOT NULL," +
                "reason TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "INDEX idx_target (target_uuid)," +
                "INDEX idx_sender (sender_uuid)," +
                "UNIQUE KEY unique_vouch (target_uuid, sender_uuid)" +
                ")";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(vouchesTable);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addVouch(Vouch vouch) {
        String sql = "INSERT INTO vouches (target_uuid, target_name, sender_uuid, sender_name, type, reason, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE type = ?, reason = ?, created_at = ?, target_name = ?, sender_name = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp timestamp = Timestamp.valueOf(vouch.getCreatedAt());

            stmt.setString(1, vouch.getTargetUuid().toString());
            stmt.setString(2, vouch.getTargetName());
            stmt.setString(3, vouch.getSenderUuid().toString());
            stmt.setString(4, vouch.getSenderName());
            stmt.setString(5, vouch.getType().name());
            stmt.setString(6, vouch.getReason());
            stmt.setTimestamp(7, timestamp);
            stmt.setString(8, vouch.getType().name());
            stmt.setString(9, vouch.getReason());
            stmt.setTimestamp(10, timestamp);
            stmt.setString(11, vouch.getTargetName());
            stmt.setString(12, vouch.getSenderName());

            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add vouch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Vouch> getVouches(UUID targetUuid) {
        List<Vouch> vouches = new ArrayList<>();
        String sql = "SELECT * FROM vouches WHERE target_uuid = ? ORDER BY created_at DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, targetUuid.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                vouches.add(mapResultSetToVouch(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get vouches: " + e.getMessage());
            e.printStackTrace();
        }

        return vouches;
    }

    public Vouch getVouch(UUID targetUuid, UUID senderUuid) {
        String sql = "SELECT * FROM vouches WHERE target_uuid = ? AND sender_uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, targetUuid.toString());
            stmt.setString(2, senderUuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToVouch(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get vouch: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public void removeAllVouches(UUID targetUuid) {
        String sql = "DELETE FROM vouches WHERE target_uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, targetUuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove vouches: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Vouch mapResultSetToVouch(ResultSet rs) throws SQLException {
        return Vouch.builder()
                .targetUuid(UUID.fromString(rs.getString("target_uuid")))
                .targetName(rs.getString("target_name"))
                .senderUuid(UUID.fromString(rs.getString("sender_uuid")))
                .senderName(rs.getString("sender_name"))
                .type(VouchType.valueOf(rs.getString("type")))
                .reason(rs.getString("reason"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .build();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database disconnected!");
        }
    }
}
