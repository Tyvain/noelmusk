package antix.views.main.commands;

import antix.model.MastodonPost;
import antix.views.main.PostSelector;

import com.vaadin.flow.component.grid.Grid;

import java.util.List;
import java.util.Random;

/**
 * Commande permettant de sélectionner un post aléatoire parmi les posts
 * affichés.
 */
public class RandCommand extends NavigationCommand {
    private final Random random = new Random();

    /**
     * Constructeur de la commande Rand.
     *
     * @param grid     Grille affichant les posts.
     * @param selector Permet d'afficher le post sélectionné.
     */
    public RandCommand(Grid<MastodonPost> grid, PostSelector selector) {
        super(
            List.of("rand", "random"),
            "Rand",
            """
            🎲 rand / random
            
            💡 Sélectionner un post au hasard parmi ceux affichés.
            """,
            grid,
            selector
        );

    }

    /**
     * Sélectionne un post aléatoirement dans la liste.
     *
     * @param items   Liste complète des posts visibles.
     * @param current Le post actuellement sélectionné (non utilisé ici).
     * @return Un post tiré au sort ou null si la liste est vide.
     */
    @Override
    protected MastodonPost getTargetPost(List<MastodonPost> items, MastodonPost current) {
        return items.isEmpty() ? null : items.get(random.nextInt(items.size()));
    }
}
