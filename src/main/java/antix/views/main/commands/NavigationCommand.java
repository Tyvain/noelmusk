package antix.views.main.commands;

import antix.model.SocialMediaPost;
import antix.utils.FeedbackUtils;
import antix.utils.GridUtils;
import antix.views.main.PostSelector;

import com.vaadin.flow.component.grid.Grid;

import java.util.List;

public abstract class NavigationCommand extends Command {
    protected final Grid<SocialMediaPost> grid;
    protected final PostSelector selector;

    // Constructeur pour initialiser le titre, la description, la grid et le selector
    public NavigationCommand(List<String> aliases, String title, String description, Grid<SocialMediaPost> grid, PostSelector selector) {
        super(aliases, title, description); // Appelle le constructeur de Command
        this.grid = grid;
        this.selector = selector;
    }

    // Méthode abstraite pour obtenir le post cible
    protected abstract SocialMediaPost getTargetPost(List<SocialMediaPost> items, SocialMediaPost current);

    @Override
    public void execute(String input) {
        List<SocialMediaPost> items = GridUtils.fetchAll(grid);
        if (items.isEmpty()) {
            FeedbackUtils.showError("Aucun post disponible.");
            return;
        }

        SocialMediaPost current = grid.asSingleSelect().getValue();
        SocialMediaPost target = getTargetPost(items, current);

        if (target != null) {
            int index = items.indexOf(target);
            if (index != -1) {
                selector.selectAndDisplay(target);
                grid.scrollToIndex(index); // Fait défiler la grille jusqu'à l'index
                FeedbackUtils.showSuccess("Post sélectionné : " + (index + 1));
            }
        } else {
            FeedbackUtils.showError("Aucun post correspondant trouvé.");
        }
    }
}