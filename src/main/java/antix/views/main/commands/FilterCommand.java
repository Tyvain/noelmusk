package antix.views.main.commands;

import antix.model.SocialMediaPost;
import antix.utils.FeedbackUtils;
import antix.utils.GridUtils;
import antix.views.main.PostSelector;

import com.vaadin.flow.component.grid.Grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Commande filtrant les posts ayant plus de n réponses.
 * Exemple : \"replies > 10\" ou \"r > 5\"
 */
public class FilterCommand extends Command {
    private final Grid<SocialMediaPost> grid;
    private final PostSelector selector;

    /**
     * Constructeur.
     *
     * @param grid     Grille contenant les posts.
     * @param selector Interface de sélection du post à afficher.
     */
    public FilterCommand(Grid<SocialMediaPost> grid, PostSelector selector) {
        super(
                List.of("filter"),
                "Filter",
                """
                        💬 filter <attribut> > / < <n>

                        💡 Filtrer les posts ayant plus ou moins n attributs
                        Exemple : filter replies > 10 ou filter likes > 5.
                        """);
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
        if (!checkValidCommand(input)) {
            FeedbackUtils.showError("Format invalide.");
            return;
        }
        String[] parts = input.split(" ");
        String attribute = parts[1];
        String operator = parts[2];
        int value = Integer.parseInt(parts[3]);

        List<SocialMediaPost> posts = GridUtils.fetchAll(grid);
        List<SocialMediaPost> filtered = new ArrayList<>();

        // Filtrer en fonction de l'attribut spécifié
        switch (attribute) {
            case "replies":
                filtered = posts.stream()
                        .filter(post -> compare(post.getRepliesCount(), operator, value))
                        .collect(Collectors.toList());
                break;
            case "reposts":
                filtered = posts.stream()
                        .filter(post -> compare(post.getShareCount(), operator, value))
                        .collect(Collectors.toList());
                break;
            case "likes":
                filtered = posts.stream()
                        .filter(post -> compare(post.getLikeCount(), operator, value))
                        .collect(Collectors.toList());
                break;
            default:
                break;
        }

        grid.setItems(filtered);

        if (!filtered.isEmpty()) {
            selector.selectAndDisplay(filtered.get(0));
            FeedbackUtils.showSuccess(filtered.size() + " post(s) trouvés avec " + operator + " " + value + " "
                    + attribute + ".");
        } else {
            FeedbackUtils
                    .showMessage("Aucun post trouvé avec " + operator + " " + value + " " + attribute + ".");

        }
    }

    private static boolean checkValidCommand(String input) {
        String[] parts = input.split(" ");
        if (parts.length != 4) {
            return false;
        }

        String attribute = parts[1];
        String operator = parts[2];
        String valueStr = parts[3];

        // Liste des attributs valides
        List<String> validAttributes = Arrays.asList("reposts", "likes", "replies");
        // Liste des opérateurs valides
        List<String> validOperators = Arrays.asList(">", "<");

        if (!validAttributes.contains(attribute)) {
            return false;
        }
        if (!validOperators.contains(operator)) {
            return false;
        }
        try {
            Integer.parseInt(valueStr);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    private boolean compare(int postValue, String operator, int value) {
        if (">".equals(operator)) {
            return postValue > value;
        } else if ("<".equals(operator)) {
            return postValue < value;
        }
        return false;
    }
}
