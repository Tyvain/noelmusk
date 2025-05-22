package antix.factory;

import antix.model.MastodonPost;
import antix.views.main.PostSelector;
import antix.views.main.commands.*;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fabrique centralisée pour construire toutes les commandes disponibles.
 * Permet de regrouper leur instanciation et de les passer à l'application.
 */
public class CommandFactory {

    /**
     * Construit l’ensemble des commandes disponibles.
     *
     * @param grid         Grille d'affichage des posts.
     * @param contentDiv   Div utilisé pour afficher du contenu (HTML,
     *                     formulaire...).
     * @param selector     Sélecteur de post à afficher.
     * @param favoris      Liste des favoris à mettre à jour.
     * @param resetFetcher Fonction de récupération initiale (ex: tag par défaut).
     * @param tagFetcher   Fonction de récupération des posts par hashtag.
     * @return Map des commandes associées à leurs mots-clés.
     */
    public static Map<String, Command> build(
            Grid<MastodonPost> grid,
            Div contentDiv,
            PostSelector selector,
            List<MastodonPost> favoris,
            Supplier<List<MastodonPost>> resetFetcher,
            Function<String, List<MastodonPost>> tagFetcher,
            List<String> commandesTapees) {
        Map<String, Command> commands = new LinkedHashMap<>();

        addCommands(new ResetCommand(grid, resetFetcher, selector), commands);
        addCommands(new RepliesGreaterCommand(grid, selector), commands);
        addCommands(new SelectByIdCommand(grid, selector), commands);
        addCommands(new ContentSearchCommand(grid, selector), commands);
        addCommands(new HashtagCommand(grid, tagFetcher, selector), commands);
        addCommands(new LinkCommand(grid, contentDiv), commands);
        addCommands(new FavCommand(grid, favoris), commands);
        addCommands(new HelpCommand(commands, contentDiv), commands);
        addCommands(new NextCommand(grid, selector), commands);
        addCommands(new PreviousCommand(grid, selector), commands);
        addCommands(new TopCommand(grid, selector), commands);
        addCommands(new BottomCommand(grid, selector), commands);
        PlayCommand play = new PlayCommand(grid);
        addCommands(play, commands);
        addCommands(new StopCommand(play), commands);
        addCommands(new GotoCommand(grid, selector), commands);
        addCommands(new SortCommand(grid, selector), commands);
        addCommands(new TagListCommand(grid, contentDiv), commands);
        addCommands(new HistoryCommand(grid, selector, commandesTapees, contentDiv), commands);
        return commands;
    }

    private static void addCommands(Command command, Map<String, Command> commandsMap) {
        List<String> aliases = command.getAliases();
        for (String alias : aliases) {
            commandsMap.put(alias, command);
        }
    }
}
