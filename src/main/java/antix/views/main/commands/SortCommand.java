package antix.views.main.commands;

import antix.model.SocialMediaPost;
import antix.utils.FeedbackUtils;
import antix.utils.GridUtils;
import antix.views.main.PostSelector;

import com.vaadin.flow.component.grid.Grid;

import java.util.List;

/**
 * Commande qui trie les posts par nombre de réponses décroissant.
 * Le post ayant le plus de réponses est sélectionné automatiquement.
 */
public class SortCommand extends Command {
    private final Grid<SocialMediaPost> grid;
    private final PostSelector selector;

    /**
     * Constructeur.
     *
     * @param grid     Grille d'affichage.
     * @param selector Permet d'afficher le post après tri.
     */
    public SortCommand(Grid<SocialMediaPost> grid, PostSelector selector) {
        super(
                List.of("sort"),
                "Sort",
                """
                📊 sort <replies | reposts | likes>

                💡 Trie les posts par ordre décroissant selon le critère choisi :
                    • 🗨️ replies : nombre de réponses
                    • 🔁 reposts : nombre de repartages
                    • ❤️ likes : nombre de likes
                """
        );
        this.grid = grid;
        this.selector = selector;
    }

    /**
     * Trie les posts par nombre de réponses décroissant et sélectionne le premier.
     *
     * @param input Non utilisé ici.
     */
    @Override
    public void execute(String input) {

        String[] inputSplit = input.split(" ");
        if (inputSplit.length < 2) {
            FeedbackUtils.showError("Paramètre de tri manquant");
            return;
        }
        String param = inputSplit[1];
        List<SocialMediaPost> items = GridUtils.fetchAll(grid);

        switch (param) {
            case "replies":
                items.sort((a, b) -> Integer.compare(b.getRepliesCount(), a.getRepliesCount()));
                break;
            case "reposts":
                items.sort((a, b) -> Integer.compare(b.getShareCount(), a.getShareCount()));
                break;
            case "likes":
                items.sort((a, b) -> Integer.compare(b.getLikeCount(), a.getLikeCount()));
                break;
            default:
                FeedbackUtils.showError("Paramètre de tri incorrect");
                break;
        }

        grid.setItems(items);

        if (!items.isEmpty()) {
            selector.selectAndDisplay(items.get(0));
        }
    }
}
