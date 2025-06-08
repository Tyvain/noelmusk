package antix.views.main.commands;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;

import antix.components.CommandCard;
import antix.utils.FeedbackUtils;

import java.util.*;

/**
 * Commande affichant l'aide utilisateur avec la liste des commandes
 * disponibles.
 */
public class HelpCommand extends Command {
    private final Map<String, Command> commandMap;
    private final Div contentDiv;

    /**
     * Constructeur.
     *
     * @param commandMap Ensemble des commandes enregistrées.
     * @param contentDiv Zone dans laquelle afficher l'aide.
     */
    public HelpCommand(Map<String, Command> commandMap, Div contentDiv) {
        super(
            List.of("?", "help"),
            "Help",
            """
            ❓ ? / help <commande?>
    
            💡 Affiche l'aide utilisateur avec la liste des commandes disponibles :
                • 🗂️ help : affiche toutes les commandes disponibles
                • 🔍 help [commande] : affiche l'aide détaillée pour une commande spécifique
            """
        );
        this.commandMap = commandMap;
        this.contentDiv = contentDiv;
    }

    /**
     * Affiche les descriptions des commandes sans doublon.
     *
     * @param input Entrée utilisateur (non utilisée ici).
     */
    @Override
    public void execute(String input) {
        contentDiv.removeAll();
        Div helpDiv = new Div();
        helpDiv.getStyle().set("white-space", "pre-wrap");

        // Extraire le nom de la commande après "help"
        String[] parts = input.trim().split("\\s+", 2);
        if (parts.length > 1) {
            String commandName = parts[1].toLowerCase();
            Command command = commandMap.get(commandName);

            if (command != null) {
                // Afficher uniquement la description de la commande spécifiée
                helpDiv.add(new H2("Commande : " + commandName));
                helpDiv.add(new H3("Description :"));
                helpDiv.add(new Div(command.getDescription()));
            } else {
                // Afficher un message d'erreur si la commande n'existe pas
                FeedbackUtils.showMessage("Commande inconnue : " + commandName);
            }
        } else {
            // Si aucune commande spécifique n'est demandée, afficher toutes les commandes
            helpDiv.add(new H2("Commandes disponibles :"));
            Div commandsDiv = new Div();
            commandsDiv.getStyle().set("display", "flex");
            commandsDiv.getStyle().set("flex-wrap", "wrap");
    
            Set<String> seenDescriptions = new HashSet<>();
            Set<Command> uniqueCommands = new LinkedHashSet<>(commandMap.values());
    
            uniqueCommands.forEach(cmd -> {
                String desc = cmd.getDescription();
                if (seenDescriptions.add(desc)) {
                    commandsDiv.add(new CommandCard(cmd));
                    // helpDiv.add(new Paragraph(desc));
                }
            });
            helpDiv.add(commandsDiv);
        }
        contentDiv.add(helpDiv);
    }
}
