package antix.views.main.commands;

import antix.model.MastodonPost;
import antix.utils.FeedbackUtils;
import antix.views.main.PostSelector;
import com.vaadin.flow.component.grid.Grid;

import java.util.List;

public class GotoCommand extends NavigationCommand {

    private int targetIndex = -1;

    public GotoCommand(Grid<MastodonPost> grid, PostSelector selector) {
        super(
            List.of("g", "goto"),
            "Goto",
            """
            ⬇️ g / goto <numéro>
        
            💡 Va directement au post numéro n
            """,
            grid,
            selector
        );
    }

    @Override
    public void execute(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length < 2) {
            FeedbackUtils.showError("Veuillez spécifier un index (ex: goto 3).");
            return;
        }

        try {
            targetIndex = Integer.parseInt(parts[1]) - 1;
        } catch (NumberFormatException e) {
            FeedbackUtils.showError("Index invalide : '" + parts[1] + "'.");
            return;
        }

        super.execute(input); // Appelle la logique de NavigationCommand
    }

    @Override
    protected MastodonPost getTargetPost(List<MastodonPost> items, MastodonPost current) {
        if (targetIndex < 0 || targetIndex >= items.size()) {
            FeedbackUtils.showError("Index hors limites. Il doit être entre 1 et " + items.size() + ".");
            return null;
        }
        return items.get(targetIndex);
    }
}
