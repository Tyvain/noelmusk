package antix.service;

import antix.model.SocialMediaPost;
import java.util.List;

public interface SocialMediaService {
    /**
     * Récupère des posts basés sur un tag/mot-clé
     * @param tag Le tag ou mot-clé à rechercher
     * @param limit Le nombre maximum de posts à récupérer
     * @return Liste des posts trouvés
     */
    List<SocialMediaPost> fetchPostsFromTag(String tag, int limit);
    
    /**
     * Retourne le nom de la plateforme
     * @return Le nom de la plateforme (ex: "mastodon", "reddit")
     */
    String getPlatformName();
}
