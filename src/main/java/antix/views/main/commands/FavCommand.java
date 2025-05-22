package antix.views.main.commands;

import antix.model.MastodonPost;
import antix.utils.FeedbackUtils;

import com.vaadin.flow.component.grid.Grid;

import java.util.List;

/**
 * Commande permettant d'ajouter un post aux favoris locaux.
 * Le post sélectionné est ajouté s’il n’est pas déjà présent.
 */
public class FavCommand extends Command {
    private final Grid<MastodonPost> grid;
    private final List<MastodonPost> favoris;

    /**
     * Constructeur de la commande Fav.
     *
     * @param grid    Grille contenant les posts.
     * @param favoris Liste des posts favoris (locale).
     */
    public FavCommand(Grid<MastodonPost> grid, List<MastodonPost> favoris) {
        super(
            List.of("f", "fav"),
            "Favoris",
            """
            ⭐ f / fav
        
            💡 Ajoute le post sélectionné aux favoris
            """
        );
        this.grid = grid;
        this.favoris = favoris;
    }

    /**
     * Ajoute le post sélectionné à la liste des favoris.
     * Ne fait rien si aucun post n’est sélectionné ou si déjà en favoris.
     *
     * @param input Entrée utilisateur (ignorée ici).
     */
    @Override
    public void execute(String input) {
        MastodonPost selectedPost = grid.getSelectedItems().stream().findFirst().orElse(null);

        if (selectedPost == null) {
            FeedbackUtils.showError("Aucun post sélectionné pour ajouter aux favoris.");
            return;
        }

        boolean alreadyFavorited = favoris.stream()
                .anyMatch(post -> post.getId().equals(selectedPost.getId()));

        if (alreadyFavorited) {
            FeedbackUtils.showMessage("Ce post est déjà dans les favoris.");
        } else {
            favoris.add(selectedPost);
            FeedbackUtils.showSuccess("Post ajouté aux favoris !");
        }
    }
}
