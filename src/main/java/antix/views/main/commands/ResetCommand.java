package antix.views.main.commands;

import antix.model.SocialMediaPost;
import antix.model.SocialMediaPost;
import antix.utils.FeedbackUtils;
import antix.views.main.PostSelector;

import com.vaadin.flow.component.grid.Grid;
import java.util.List;
import java.util.function.Supplier;

/**
 * Commande permettant de réinitialiser l'affichage avec les derniers posts.
 * Elle recharge les données depuis un tag par défaut (ex. \"info\").
 */
public class ResetCommand extends Command {
    private final Grid<SocialMediaPost> grid;
    private final Supplier<List<SocialMediaPost>> fetcher;
    private final PostSelector selector;

    /**
     * Constructeur de la commande Reset.
     *
     * @param grid2       Grille des posts.
     * @param resetFetcher    Fonction permettant de récupérer les posts (via API).
     * @param selector   Sélecteur/afficheur de post.
     */
    public ResetCommand(Grid<SocialMediaPost> grid2, Supplier<List<SocialMediaPost>> resetFetcher, PostSelector selector) {
        super(
            List.of("reset"),
            "Reset",
            """
            🔄 reset
            
            💡 Réinitialise la liste avec les derniers posts du tag par défaut
            """
        );
        this.grid = grid2;
        this.fetcher = resetFetcher;
        this.selector = selector;
    }

    /**
     * Réinitialise la grille avec les données récupérées.
     * Affiche automatiquement le premier post si possible.
     *
     * @param input Entrée utilisateur (ignorée ici).
     */
    @Override
    public void execute(String input) {
        List<SocialMediaPost> posts = fetcher.get();
        grid.setItems(posts);

        if (!posts.isEmpty()) {
            selector.selectAndDisplay(posts.get(0));
            FeedbackUtils.showSuccess("Liste réinitialisée avec " + posts.size() + " posts.");
        } else {
            FeedbackUtils.showMessage("Aucun post trouvé à réinitialiser.");
        }
    }
}
