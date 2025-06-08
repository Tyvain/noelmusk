package antix.views.main;

import antix.model.SocialMediaPost;

// Interface fonctionnelle, elle permet de passer une méthode et d'affichage personnalisée

@FunctionalInterface
public interface PostSelector {
    void selectAndDisplay(SocialMediaPost post);
}
