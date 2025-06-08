package antix.views.main.commands;

import antix.model.SocialMediaPost;
import antix.utils.FeedbackUtils;

import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;

/**
 * Commande permettant de copier le lien d’un post sélectionné.
 * Le lien est automatiquement copié dans le presse-papiers.
 */
public class OpenCommand extends Command {
    private final Grid<SocialMediaPost> grid;
    private final Div contentDiv;

    /**
     * Constructeur.
     *
     * @param grid        Grille des posts.
     * @param contentDiv  Zone d'affichage secondaire (feedback lien).
     */
    public OpenCommand(Grid<SocialMediaPost> grid, Div contentDiv) {
        super(
            List.of("o", "open"),
            "Open",
            """
            🔗 o / open
        
            💡 Ouvre le post sélectionné dans un nouvel onglet
            """
        );
        this.grid = grid;
        this.contentDiv = contentDiv;
    }

    /**
     * Génére un champ contenant l’URL du post, prêt à être copié.
     *
     * @param input Entrée utilisateur (non utilisée).
     */
    @Override
    public void execute(String input) {
        SocialMediaPost post = grid.getSelectedItems().stream().findFirst().orElse(null);

        if (post != null) {
            String url = post.getUrl();
            UI.getCurrent().getPage().open(url,"_blank");
            FeedbackUtils.showSuccess("Post ouvert dans un nouvel onglet.");
        } else {
            FeedbackUtils.showError("Aucun post sélectionné pour générer un lien.");
        }
    }
}
