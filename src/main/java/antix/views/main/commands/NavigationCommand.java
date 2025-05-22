package antix.views.main.commands;

import antix.model.MastodonPost;
import antix.utils.FeedbackUtils;
import antix.utils.GridUtils;
import antix.views.main.PostSelector;

import com.vaadin.flow.component.grid.Grid;

import java.util.List;

public abstract class NavigationCommand extends Command {
    protected final Grid<MastodonPost> grid;
    protected final PostSelector selector;

    // Constructeur pour initialiser le titre, la description, la grid et le selector
    public NavigationCommand(List<String> aliases, String title, String description, Grid<MastodonPost> grid, PostSelector selector) {
        super(aliases, title, description); // Appelle le constructeur de Command
        this.grid = grid;
        this.selector = selector;
    }

    // Méthode abstraite pour obtenir le post cible
    protected abstract MastodonPost getTargetPost(List<MastodonPost> items, MastodonPost current);

    @Override
    public void execute(String input) {
        List<MastodonPost> items = GridUtils.fetchAll(grid);
        if (items.isEmpty()) {
            FeedbackUtils.showError("Aucun post disponible.");
            return;
        }

        MastodonPost current = grid.asSingleSelect().getValue();
        MastodonPost target = getTargetPost(items, current);

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