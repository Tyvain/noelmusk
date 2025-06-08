package antix.views.main.commands;

import antix.model.SocialMediaPost;
import antix.model.Tag;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Commande listant les tags les plus fréquents dans les posts actuellement
 * visibles.
 * Utile pour identifier les sujets dominants.
 */
public class TagListCommand extends Command {
    private final Grid<SocialMediaPost> grid;
    private final Div contentDiv;

    /**
     * Constructeur.
     *
     * @param grid       Grille contenant les posts.
     * @param contentDiv Zone où afficher les résultats.
     */
    public TagListCommand(Grid<SocialMediaPost> grid, Div contentDiv) {
        super(
            List.of("taglist"),
            "Tag List",
            """
            🏷️ taglist
            
            💡 Liste les tags les plus fréquents dans les posts affichés
            """
        );
        this.grid = grid;
        this.contentDiv = contentDiv;
    }

    /**
     * Exécute la commande : extrait tous les tags visibles dans la grille
     * et affiche les 20 plus fréquents triés par fréquence.
     *
     * @param input Entrée utilisateur (ignorée ici).
     */
    @Override
    public void execute(String input) {
        List<SocialMediaPost> items = grid.getDataProvider().fetch(new com.vaadin.flow.data.provider.Query<>())
                .collect(Collectors.toList());
        Map<String, Integer> tagFrequency = new HashMap<>();

        for (SocialMediaPost post : items) {
            if (post.getTags() != null) {
                for (Tag tag : post.getTags()) {
                    tagFrequency.put(tag.getName(), tagFrequency.getOrDefault(tag.getName(), 0) + 1);
                }
            }
        }

        StringBuilder out = new StringBuilder("Tags les plus fréquents :\n");
        tagFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .forEach(entry -> out.append(entry.getKey()).append(" (").append(entry.getValue()).append(")\n"));

        contentDiv.removeAll();
        Div div = new Div();
        div.getStyle().set("white-space", "pre-wrap");
        div.setText(out.toString());
        contentDiv.add(div);
    }
}
