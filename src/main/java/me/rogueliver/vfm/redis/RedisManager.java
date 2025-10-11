package me.rogueliver.vfm.redis;

import com.google.gson.Gson;
import lombok.Getter;
import me.rogueliver.vfm.VouchForMe;
import me.rogueliver.vfm.models.Vouch;
import me.rogueliver.vfm.models.VouchMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

public class RedisManager {

    private final VouchForMe plugin;
    @Getter
    private JedisPool jedisPool;
    private String channel;
    private final Gson gson = new Gson();
    private VouchSubscriber subscriber;
    private Thread subscriberThread;

    public RedisManager(VouchForMe plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        try {
            FileConfiguration config = plugin.getConfig();

            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(10);
            poolConfig.setMaxIdle(5);
            poolConfig.setMinIdle(1);

            String host = config.getString("redis.host");
            int port = config.getInt("redis.port");
            String password = config.getString("redis.password");
            channel = config.getString("redis.channel");

            if (password == null || password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
            }

            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
            }

            startSubscriber();

            plugin.getLogger().info("Redis connected successfully!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to Redis: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void startSubscriber() {
        subscriber = new VouchSubscriber();
        subscriberThread = new Thread(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(subscriber, channel);
            } catch (Exception e) {
                plugin.getLogger().severe("Redis subscriber error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        subscriberThread.start();
    }

    public void publishVouch(Vouch vouch, String action) {
        try (Jedis jedis = jedisPool.getResource()) {
            VouchMessage message = new VouchMessage(action, vouch);
            String json = gson.toJson(message);
            jedis.publish(channel, json);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to publish vouch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void publishRemoveAll(String targetUuid, String targetName) {
        try (Jedis jedis = jedisPool.getResource()) {
            Vouch vouch = Vouch.builder()
                    .targetUuid(java.util.UUID.fromString(targetUuid))
                    .targetName(targetName)
                    .build();
            VouchMessage message = new VouchMessage("REMOVE_ALL", vouch);
            String json = gson.toJson(message);
            jedis.publish(channel, json);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to publish remove all: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (subscriber != null && subscriber.isSubscribed()) {
            subscriber.unsubscribe();
        }
        if (subscriberThread != null && subscriberThread.isAlive()) {
            subscriberThread.interrupt();
        }
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            plugin.getLogger().info("Redis disconnected!");
        }
    }

    private class VouchSubscriber extends JedisPubSub {
        @Override
        public void onMessage(String channel, String message) {
            try {
                VouchMessage vouchMessage = gson.fromJson(message, VouchMessage.class);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getVouchManager().handleRedisMessage(vouchMessage);
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to handle Redis message: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
