package antix.views.main.commands;

import antix.model.MastodonPost;
import antix.views.main.PostSelector;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.grid.Grid;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Commande pour afficher ou rechercher dans l’historique des commandes.
 * Usage :
 * - hist : affiche tout l'historique
 * - hist <mot-clé> : filtre l’historique contenant le mot-clé
 */
public class HistoryCommand extends Command {
    private final Grid<MastodonPost> grid;
    private final PostSelector selector;
    private final List<String> history;
    private final Div contentDiv;

    public HistoryCommand(Grid<MastodonPost> grid,
                          PostSelector selector,
                          List<String> history,
                          Div contentDiv) {
        super(
            List.of("hist", "histo"),
            "Historique",
            """
            📜 hist / histo <mot-clé?>
    
            💡 Affiche l’historique des commandes saisies :
                • 📜 hist : affiche l’historique complet des commandes
                • 🔍 hist <mot-clé> : recherche dans l’historique les commandes contenant le mot-clé spécifié
            """
        );
        this.grid = grid;
        this.selector = selector;
        this.history = history;
        this.contentDiv = contentDiv;
    }

    @Override
    public void execute(String input) {
        contentDiv.removeAll();

        String keyword = input.trim().replaceFirst("^\\S+\\s*", "").toLowerCase();
        List<String> filtered = keyword.isEmpty()
                ? history
                : history.stream()
                         .filter(cmd -> cmd.toLowerCase().contains(keyword))
                         .collect(Collectors.toList());

        Div historyDiv = new Div();
        historyDiv.getStyle().set("white-space", "pre-wrap");

        if (filtered.isEmpty()) {
            historyDiv.add(new Paragraph("Aucune commande ne correspond à \"" + keyword + "\"."));
        } else {
            historyDiv.add(new H2("Historique des commandes :"));
            filtered.forEach(cmd -> historyDiv.add(new Paragraph(cmd)));
        }

        contentDiv.add(historyDiv);
    }
}
