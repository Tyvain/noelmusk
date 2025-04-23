package antix.views.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.router.Route;

import antix.model.MastodonPost;

@PageTitle("main")
@Route("")
public class MainView extends VerticalLayout {
    private final Div output;
    private List<MastodonPost> currentPosts = new ArrayList<>();
    private int currentIndex = 0;

    public MainView() {
        setSizeFull();

        output = new Div();
        output.getStyle().set("white-space", "pre-wrap");
        output.setWidthFull();

        prompt.setPlaceholder("Entrez une commande...");
        prompt.setWidthFull();

        AtomicBoolean internalChange = new AtomicBoolean(false);

        prompt.addValueChangeListener(v -> {
            if (internalChange.get()) return;
            String text = v.getValue().trim();
            internalChange.set(true);
            prompt.setValue("");
            internalChange.set(false);

            if (text.equals("help") || text.equals("h")) {
                showMessage("""
                        🆘 Commandes disponibles :
                        - `s tag1 tag2` : rechercher des posts par hashtags
                        - `n` ou `next` : post suivant
                        - `p` ou `previous` : post précédent
                        - `md` : exporter le post actuel en Markdown
                        - `clear` : effacer l'écran
                        - `help` : afficher cette aide
                        """);
            } else if (text.equals("clear")) {
                contentDiv.removeAll();
            } else if (text.equals("md")) {
                exportMarkdown();
            } else if (text.startsWith("s ") || text.startsWith("search ")) {
                String[] parts = text.split(" ");
                Set<MastodonPost> results = new HashSet<>();
                for (int i = 1; i < parts.length; i++) {
                    results.addAll(fetchPostsFromTag(parts[i]));
                }
                posts.clear();
                posts.addAll(results);
                currentIndex = 0;
                showCurrentPost();
            } else if (text.equals("n") || text.equals("next")) {
                if (currentIndex < posts.size() - 1) currentIndex++;
                showCurrentPost();
            } else if (text.equals("p") || text.equals("previous")) {
                navigate(-1);
            } else if (text.equals("list") || text.equals("l")) {
                listPosts();
            } else if (text.startsWith("goto ")) {
                try {
                    int index = Integer.parseInt(text.substring(5).trim()) - 1;
                    if (index >= 0 && index < currentPosts.size()) {
                        currentIndex = index;
                        renderCurrentPost();
                    } else {
                        output.setText("Index hors limites.");
                    }
                } catch (NumberFormatException e) {
                    output.setText("Format invalide pour goto.");
                }
            } else if (text.equals("help")) {
                output.setText("""
Commandes disponibles :
  s tag1 tag2        Recherche avec au moins un tag (OU)
  s& tag1 tag2       Recherche avec tous les tags (ET)
  next / n           Post suivant
  previous / p       Post précédent
  list / l           Affiche un sommaire des posts
  goto N             Aller au post numéro N
  help               Afficher cette aide
""");
            } else if (text.startsWith("s& ")) {
                String[] tags = text.substring(3).trim().split(" ");
                Map<String, MastodonPost> commonPosts = new HashMap<>();

                for (String tag : tags) {
                    List<MastodonPost> postsForTag = fetchPostsFromTag(tag);
                    for (MastodonPost post : postsForTag) {
                        commonPosts.merge(post.getId(), post, (oldPost, newPost) -> oldPost);
                    }
                }

                // Filtrer les posts qui contiennent tous les tags spécifiés
                List<MastodonPost> filtered = commonPosts.values().stream()
                        .filter(post -> {
                            String content = post.getContent().toLowerCase();
                            return Arrays.stream(tags).allMatch(tag -> content.contains("#" + tag.toLowerCase()));
                        })
                        .collect(Collectors.toList());

                currentPosts = filtered;
                currentIndex = 0;
                renderCurrentPost();
            } else if (text.startsWith("s ")) {
                String[] tags = text.substring(2).trim().split(" ");
                Set<MastodonPost> results = new HashSet<>();
                for (String tag : tags) {
                    results.addAll(fetchPostsFromTag(tag));
                }
                currentPosts = new ArrayList<>(results);
                currentIndex = 0;
                renderCurrentPost();
            } else {
                output.setText("Commande inconnue. Tapez 'help' pour la liste des commandes.");
            }
        });

        var container = new VerticalLayout();
        container.setSizeFull();
        container.setSpacing(false);
        container.setPadding(false);
        container.setMargin(false);
        container.add(output);
        add(container);
        add(prompt);
    }

    private void navigate(int offset) {
        if (!currentPosts.isEmpty()) {
            currentIndex = Math.max(0, Math.min(currentIndex + offset, currentPosts.size() - 1));
            renderCurrentPost();
        } else {
            output.setText("Aucun post à naviguer.");
        }
    }

    private void listPosts() {
        StringBuilder builder = new StringBuilder("Sommaire:\n");
        for (int i = 0; i < currentPosts.size(); i++) {
            MastodonPost post = currentPosts.get(i);
            builder.append("[").append(i + 1).append("] ")
                   .append(post.getAccount().getUsername())
                   .append(" : ")
                   .append(StringUtils.left(Jsoup.parse(post.getContent()).text(), 50))
                   .append("\n");
        }
        output.setText(builder.toString());
    }

    private void renderCurrentPost() {
        if (currentPosts.isEmpty()) {
            output.setText("Aucun post trouvé.");
            return;
        }
        MastodonPost post = currentPosts.get(currentIndex);
        output.setText("Post " + (currentIndex + 1) + "/" + currentPosts.size() + "\n"
                + "Auteur : @" + post.getAccount().getUsername() + "\n"
                + "Date : " + post.getCreatedAt() + "\n"
                + "Contenu :\n" + Jsoup.parse(post.getContent()).text() + "\n"
                + "URL : " + post.getUrl());
    }

    public List<MastodonPost> fetchPostsFromTag(String tag) {
        if (StringUtils.isEmpty(tag)) return List.of();
        try {
            var uri = new URIBuilder("https://mastodon.social/api/v1/timelines/tag/" + tag)
                    .addParameter("limit", "40")
                    .build();
            URL url = uri.toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return Arrays.stream(mapper.readValue(response.toString(), MastodonPost[].class)).collect(Collectors.toList());
        } catch (IOException | URISyntaxException e) {
            output.setText("Erreur de récupération : " + e.getMessage());
            return List.of();
        }
    }
}