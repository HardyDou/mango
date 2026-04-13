package io.mango.message.core.channel;

/**
 * SPI interface for message delivery channels.
 */
public interface MessageChannel {

    /**
     * Send message to a specific user.
     */
    void sendToUser(Long userId, String message);

    /**
     * Send message to multiple users.
     */
    default void sendToUsers(java.util.List<Long> userIds, String message) {
        for (Long userId : userIds) {
            sendToUser(userId, message);
        }
    }

    /**
     * Broadcast message to all connected users.
     */
    default void broadcast(String message) {
        // default no-op, subclasses may override
    }

    /**
     * Get channel type.
     */
    String getType();
}
