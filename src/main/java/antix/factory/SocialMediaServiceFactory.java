package antix.factory;

import antix.service.*;

public class SocialMediaServiceFactory {
    private static SocialMediaService currentService = new MastodonService();
    private static String currentPlatform = "mastodon";
    
    public static SocialMediaService getCurrentService() {
        return currentService;
    }
    
    public static void switchToPlatform(String platform) {
        currentService = switch (platform.toLowerCase()) {
            case "mastodon" -> new MastodonService();
            case "reddit" -> new RedditService();
            default -> throw new IllegalArgumentException("Plateforme non supportée: " + platform);
        };
        currentPlatform = platform;
    }
    
    public static String getCurrentPlatform() {
        return currentPlatform;
    }
}
