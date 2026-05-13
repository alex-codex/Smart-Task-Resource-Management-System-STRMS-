package strms.manager;

import strms.model.User;
import strms.enums.NotificationType;

/**
 * Gère l'envoi de notifications aux utilisateurs.
 */
public class NotificationManager {

    public static void sendNotification(User user, String message, NotificationType type) {

        if (user == null || message == null || type == null) {
            return;
        }

        switch (type) {

            case CONSOLE:
                System.out.println("[NOTIFICATION → " + user.getName() + "] " + message);
                break;

            case EMAIL:
                System.out.println("[EMAIL → " + user.getEmail() + "] " + message);
                break;

            case SMS:
                System.out.println("[SMS → " + user.getName() + "] " + message);
                break;

            default:
                System.out.println("[NOTIFICATION] " + message);
                break;
        }
    }

    /**
     * Envoie un message global à tous les utilisateurs (console simple).
     */
    public static void broadcast(String message) {
        if (message == null) {
            return;
        }

        System.out.println("[BROADCAST] " + message);
    }

    /**
     * Envoie une notification simple avec un type donné.
     */
    public static void send(NotificationType type, String message) {
        if (message == null || type == null) {
            return;
        }

        switch (type) {
            case CONSOLE:
                System.out.println("[NOTIFICATION] " + message);
                break;
            case EMAIL:
                System.out.println("[EMAIL] " + message);
                break;
            case SMS:
                System.out.println("[SMS] " + message);
                break;
            default:
                System.out.println("[NOTIFICATION] " + message);
                break;
        }
    }
}