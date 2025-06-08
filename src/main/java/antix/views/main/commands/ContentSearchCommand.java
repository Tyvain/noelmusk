package antix.views.main.commands;

import antix.model.SocialMediaPost;
import antix.utils.FeedbackUtils;
import antix.utils.GridUtils;
import antix.views.main.PostSelector;

import com.vaadin.flow.component.grid.Grid;
import org.jsoup.Jsoup;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Commande permettant de rechercher un mot dans le contenu textuel des posts.
 * Exemple : c "pouvoir"
 */
public class ContentSearchCommand extends Command {
    private final Grid<SocialMediaPost> grid;
    private final PostSelector selector;

    /**
     * Constructeur.
     *
     * @param grid        Grille contenant les posts.
     * @param selector    Sélecteur de post pour mise en avant.
     */
    public ContentSearchCommand(Grid<SocialMediaPost> grid, PostSelector selector) {
        super(
            List.of("c"),
            "Content Search",
            """
            🔍 c <mot-clé>

            💡 Recherche un mot dans le contenu des posts
            """
        );
        this.grid = grid;
        this.selector = selector;
    }

    /**
     * Recherche le mot-clé donné dans le texte des posts (HTML nettoyé).
     *
     * @param input Commande utilisateur, ex : c "mot"
     */
    @Override
    public void execute(String input) {
        String query = input.replaceFirst("c\\s*\"?", "").replaceAll("\"$", "").trim().toLowerCase();
        if (query.isEmpty()) {
            FeedbackUtils.showError("Veuillez entrer un mot à rechercher (ex: c \"mot\").");
            return;
        }

        List<SocialMediaPost> filtered = GridUtils.fetchAll(grid).stream()
                .filter(post -> Jsoup.parse(post.getContent()).text().toLowerCase().contains(query))
                .collect(Collectors.toList());

        grid.setItems(filtered);

        if (!filtered.isEmpty()) {
            selector.selectAndDisplay(filtered.get(0));
            FeedbackUtils.showSuccess(filtered.size() + " post(s) contiennent : \"" + query + "\"");
        } else {
            FeedbackUtils.showMessage("Aucun post ne contient : \"" + query + "\"");
        }
    }
}