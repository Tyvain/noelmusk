package antix.views.main.commands;

import antix.model.MastodonPost;
import antix.utils.FeedbackUtils;
import antix.utils.GridUtils;
import antix.views.main.PostSelector;

import com.vaadin.flow.component.grid.Grid;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Commande filtrant les posts ayant plus de n réponses.
 * Exemple : \"replies > 10\" ou \"r > 5\"
 */
public class RepliesGreaterCommand extends Command {
    private final Grid<MastodonPost> grid;
    private final PostSelector selector;

    /**
     * Constructeur.
     *
     * @param grid        Grille contenant les posts.
     * @param selector    Interface de sélection du post à afficher.
     */
    public RepliesGreaterCommand(Grid<MastodonPost> grid, PostSelector selector) {
        super(
            List.of("r", "replies"),
            "Replies Greater",
            """
            💬 r / replies > <n>
            
            💡 Filtrer les posts ayant plus de n réponses
            Exemple : r > 10 ou replies > 5.
            """
        );
        this.grid = grid;
        this.selector = selector;
    }

    /**
     * Exécution de la commande : filtre les posts selon la syntaxe \"replies > n\".
     *
     * @param input Entrée utilisateur.
     */
    @Override
    public void execute(String input) {
        String[] parts = input.split(">");
        if (parts.length < 2) {
            FeedbackUtils.showError("Format attendu : replies > n");
            return;
        }

        String valuePart = parts[1].trim();
        if (valuePart.isEmpty()) {
            FeedbackUtils.showError("Veuillez spécifier une valeur numérique après >.");
            return;
        }

        int min;
        try {
            min = Integer.parseInt(valuePart);
        } catch (NumberFormatException e) {
            FeedbackUtils.showError("Nombre invalide : '" + valuePart + "'");
            return;
        }

        List<MastodonPost> filtered = GridUtils.fetchAll(grid).stream()
                .filter(post -> post.getRepliesCount() > min)
                .collect(Collectors.toList());

        grid.setItems(filtered);
        if (!filtered.isEmpty()) {
            selector.selectAndDisplay(filtered.get(0));
            FeedbackUtils.showSuccess(filtered.size() + " post(s) trouvés avec > " + min + " réponses.");
        } else {
            FeedbackUtils.showMessage("Aucun post trouvé avec plus de " + min + " réponses.");
        }
    }
}
