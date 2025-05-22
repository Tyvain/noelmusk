package antix.views.main.commands;

import antix.model.MastodonPost;
import antix.views.main.PostSelector;
import com.vaadin.flow.component.grid.Grid;
import java.util.Comparator;
import java.util.List;

/**
 * Commande permettant de sélectionner le post le plus haut de la page.
 * Elle sélectionne simplement le premier élément de la liste visible.
 */
public class TopCommand extends NavigationCommand {
    /**
     * Constructeur.
     *
     * @param grid     La grille contenant les posts affichés.
     * @param selector Le sélecteur responsable de l'affichage du post choisi.
     */
    public TopCommand(Grid<MastodonPost> grid, PostSelector selector) {
        super(
            List.of("top"),
            "Top",
            """
            ⬆️ top
            
            💡 Sélectionne le post le plus haut de la page
            """,
            grid,
            selector
        );
    }

    /**
     * Définit la logique de sélection du post à afficher.
     *
     * @param items   La liste de posts visibles.
     * @param current Le post actuellement sélectionné (peut être null).
     * @return Le post le plus haut de la page.
     */
    @Override
    protected MastodonPost getTargetPost(List<MastodonPost> items, MastodonPost current) {
        return items.isEmpty() ? null : items.get(0);
    }
}