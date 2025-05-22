package antix.views.main.commands;

import java.util.List;



public abstract class Command {
    private final List<String> aliases;
    private final String title;
    private final String description;

    // Constructeur pour forcer les sous-classes à fournir un titre et une description
    protected Command(List<String> aliases, String title, String description) {
        this.aliases = aliases;
        this.title = title;
        this.description = description;
    }

    // Méthode abstraite pour exécuter la commande
    public abstract void execute(String input);

    // Getter pour la commande
    public List<String> getAliases() {
        return aliases;
    }

    // Getter pour le titre
    public String getTitle() {
        return title;
    }

    // Getter pour la description
    public String getDescription() {
        return description;
    }
}