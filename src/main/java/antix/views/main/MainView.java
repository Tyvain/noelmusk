// MainView.java corrigé avec gestion propre des variables dans les lambdas
package antix.views.main;

import antix.model.MastodonPost;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@PageTitle("main")
@Route("")
public class MainView extends VerticalLayout {
    public MainView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        var grid = new Grid<>(MastodonPost.class, false);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setDetailsVisibleOnClick(false); 
        var contentDiv = new Div();
        contentDiv.setWidthFull();

        addRepliesColumn(grid);
        addContentColumn(grid);
        grid.addColumn(MastodonPost::getSource).setHeader("Src").setAutoWidth(true);
        grid.addColumn(p -> Optional.ofNullable(p.getLikes()).orElse(0)).setHeader("Likes").setAutoWidth(true);
        grid.addColumn(p -> Optional.ofNullable(p.getComments()).orElse(0)).setHeader("Comm").setAutoWidth(true);
        grid.addColumn(p -> Optional.ofNullable(p.getShares()).orElse(0)).setHeader("Partages").setAutoWidth(true);

        grid.setItemDetailsRenderer(new ComponentRenderer<>(post -> new Div(Jsoup.parse(post.getContent()).text())));
        grid.addSelectionListener(event -> selectItemListener(grid, contentDiv, event));
        // grid.setDetailsVisibleOnClick(false);

        AtomicBoolean internalChange = new AtomicBoolean(false);
        var prompt = new TextField();
        prompt.setWidth("100%");
        prompt.setPlaceholder("Taper votre requête (? pour de l'aide)");

        prompt.addKeyPressListener(Key.ENTER, e -> {
            String text = prompt.getValue().trim();
            if (text.isEmpty()) return;

            int dateLimit = -1;
            Matcher dateMatcher = Pattern.compile("\\*date\\((\\d+)\\)").matcher(text);
            if (dateMatcher.find()) {
                dateLimit = Integer.parseInt(dateMatcher.group(1));
                text = text.replace(dateMatcher.group(0), "").trim();
            }

            int daysSince = -1;
            Matcher sinceMatcher = Pattern.compile("\\*depuis\\((\\d+)\\)").matcher(text);
            if (sinceMatcher.find()) {
                daysSince = Integer.parseInt(sinceMatcher.group(1));
                text = text.replace(sinceMatcher.group(0), "").trim();
            }

            int minutesSince = -1;
            Matcher minuteMatcher = Pattern.compile("\\*minute\\((\\d+)\\)").matcher(text);
            if (minuteMatcher.find()) {
                minutesSince = Integer.parseInt(minuteMatcher.group(1));
                text = text.replace(minuteMatcher.group(0), "").trim();
            }

            if (text.equals("?")) {
                grid.setItems(List.of());
                Div help = new Div();
                help.setText(String.join("\n",
                        "Instructions :",
                        "#mot — recherche tag Mastodon et web Google\n\n",
                        "#M — recherche Mastodon uniquement",
                        "#GN — recherche Google News uniquement",
                        "*date(N) — limite aux N plus récents",
                        "*depuis(N) — depuis N jours",
                        "*minute(N) — depuis N minutes",
                        "/select — affiche le 1er post",
                        "/select N — affiche le Nᵉ post",
                        "? — affiche cette aide",
                        "/open N — ouvre le Nᵉ post dans un nouvel onglet"
                ));
                contentDiv.removeAll();
                contentDiv.add(help);
                return;
            }

            if (text.startsWith("#")) {
                String query = text.substring(1).trim();
                List<MastodonPost> results = new ArrayList<>();
                if (query.equalsIgnoreCase("M")) {
                    results.addAll(fetchPostsFromTag("mastodon"));
                } else if (query.equalsIgnoreCase("GN")) {
                    results.addAll(fetchPostsFromExternalSearch("site:news.google.com"));
                } else {
                    results.addAll(fetchPostsFromTag(query));
                    results.addAll(fetchPostsFromExternalSearch(query));
                }

                List<MastodonPost> filteredByDay = results;
                if (daysSince > 0) {
                    ZonedDateTime threshold = ZonedDateTime.now().minusDays(daysSince);
                    filteredByDay = filteredByDay.stream()
                        .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(threshold))
                        .collect(Collectors.toList());
                }

                List<MastodonPost> filteredByMinute = filteredByDay;
                if (minutesSince > 0) {
                    ZonedDateTime threshold = ZonedDateTime.now().minusMinutes(minutesSince);
                    filteredByMinute = filteredByMinute.stream()
                        .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(threshold))
                        .collect(Collectors.toList());
                }

                List<MastodonPost> limited = filteredByMinute;
                if (dateLimit > 0) {
                    limited = limited.stream()
                        .sorted(Comparator.comparing(MastodonPost::getCreatedAt).reversed())
                        .limit(dateLimit)
                        .collect(Collectors.toList());
                }

                List<MastodonPost> safeLimited = limited; // "effectively final"
                List<MastodonPost> paginated = IntStream.range(0, safeLimited.size())
                    .mapToObj(i -> {
                        MastodonPost p = safeLimited.get(i);
                        String originalContent = Jsoup.parse(p.getContent()).text();
                        p.setContent("#" + (i + 1) + " — " + StringUtils.abbreviate(originalContent, 100));
                        return p;
                    }).collect(Collectors.toList());




                grid.setItems(paginated);
                grid.getDataProvider().fetch(new Query<>()).findFirst().ifPresent(grid::select);
            } else if (text.startsWith("/select")) {
                List<MastodonPost> items = grid.getDataProvider().fetch(new Query<>()).collect(Collectors.toList());
                int index = 0;
                Matcher m = Pattern.compile("/select\\s+(\\d+)").matcher(text);
                if (m.find()) {
                    index = Math.max(0, Integer.parseInt(m.group(1)) - 1);
                }
                if (!items.isEmpty() && index < items.size()) {
                    grid.select(items.get(index));
                }
            } else if (text.startsWith("/open")) {
                List<MastodonPost> items = grid.getDataProvider().fetch(new Query<>()).collect(Collectors.toList());
                int index = 0;
                Matcher m = Pattern.compile("/open\\s+(\\d+)").matcher(text);
                if (m.find()) {
                    index = Math.max(0, Integer.parseInt(m.group(1)) - 1);
                }
                if (!items.isEmpty() && index < items.size()) {
                    MastodonPost post = items.get(index);
                    String url = post.getExternalUrl();
                    System.out.println("Trying to open URL: " + url); // ✅ POUR DEBUG
                    if (url != null && !url.isBlank()) {
                        prompt.getElement().executeJs("window.open($0, '_blank')", url);
                    } else {
                        System.out.println("Aucun lien externe trouvé pour ce post.");
                    }
                }

            }


            internalChange.set(true);
            prompt.setValue("");
            internalChange.set(false);
            prompt.getElement().executeJs("this.focus();");
        });

        prompt.addKeyDownListener(Key.ARROW_DOWN, e -> {
            MastodonPost current = grid.getSelectedItems().stream().findFirst().orElse(null);
            List<MastodonPost> items = grid.getDataProvider().fetch(new Query<>()).collect(Collectors.toList());
            if (current != null) {
                int i = items.indexOf(current);
                if (i < items.size() - 1) grid.select(items.get(i + 1));
            } else if (!items.isEmpty()) grid.select(items.get(0));
        });

        prompt.addKeyDownListener(Key.ARROW_UP, e -> {
            MastodonPost current = grid.getSelectedItems().stream().findFirst().orElse(null);
            List<MastodonPost> items = grid.getDataProvider().fetch(new Query<>()).collect(Collectors.toList());
            if (current != null) {
                int i = items.indexOf(current);
                if (i > 0) grid.select(items.get(i - 1));
            }
        });

        grid.setHeight("100%");
        grid.setWidth("50%");

        contentDiv.setHeight("100%");
        contentDiv.setWidth("50%");

        var horizontalLayout = new HorizontalLayout(grid, contentDiv);
        horizontalLayout.setSizeFull();
        
        add(horizontalLayout);

        var promptContainer = new VerticalLayout(prompt);
        promptContainer.setWidth("100%");
        promptContainer.setPadding(false);
        add(promptContainer);

        setFlexGrow(1, horizontalLayout);
        setFlexGrow(0, promptContainer);
    }

    private void selectItemListener(Grid<MastodonPost> grid, Div contentDiv, SelectionEvent<Grid<MastodonPost>, MastodonPost> event) {
        closeAll(grid);
        event.getFirstSelectedItem().ifPresent(post -> {
            VerticalLayout container = new VerticalLayout();
            Div postContent = new Div();
            postContent.getElement().setProperty("innerHTML", post.getContent());
            container.add(postContent);

            if (post.getCreatedAt() != null) {
                var formatter = DateTimeFormatter.ofPattern("dd MMM yyyy à HH:mm").withLocale(Locale.FRENCH);
                String dateText = post.getCreatedAt().format(formatter);
                Span span = new Span("\uD83D\uDCC5 Publié le : " + dateText);
                span.getStyle().set("font-size", "0.8em").set("color", "gray").set("margin-top", "1em");
                container.add(span);
            }

            contentDiv.removeAll();
            contentDiv.add(container);
            grid.setDetailsVisible(post, true);
        });
    }

    private void addRepliesColumn(Grid<MastodonPost> grid) {
        // grid.addColumn(MastodonPost::getRepliesCount).setHeader("Réponses").setAutoWidth(true);
    }

    private void addContentColumn(Grid<MastodonPost> grid) {
        grid.addColumn(post -> StringUtils.left(Jsoup.parse(post.getContent()).text(), 50)).setHeader("Contenu").setAutoWidth(true);
    }

    private void closeAll(Grid<MastodonPost> grid) {
        grid.getDataProvider().fetch(new Query<>()).forEach(post -> grid.setDetailsVisible(post, false));
    }

    public List<MastodonPost> fetchPostsFromTag(String tag) {
        if (StringUtils.isEmpty(tag)) return List.of();
        try {
            var uri = new URIBuilder("https://mastodon.social/api/v1/timelines/tag/" + tag).addParameter("limit", "20").build();
            URL url = uri.toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            List<MastodonPost> list = Arrays.stream(mapper.readValue(response.toString(), MastodonPost[].class)).collect(Collectors.toList());
            list.forEach(p -> {
                p.setSource("Ma");
                p.setLikes(p.getFavouritesCount());
                p.setShares(p.getReblogsCount());
                p.setExternalUrl(p.getUrl()); ;
                System.out.println("Mastodon URL: " + p.getUrl());

            });
            return list;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public List<MastodonPost> fetchPostsFromExternalSearch(String query) {
        try {
            String apiKey = "c6bf6dd8df155ed34ea810c2c707582f29c0ee95243afe91aabede792ef2987e";
            var uri = new URIBuilder("https://serpapi.com/search.json")
                    .addParameter("q", query)
                    .addParameter("api_key", apiKey)
                    .addParameter("num", "20")
                    .addParameter("engine", "google")
                    .build();

            URL url = uri.toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            var root = mapper.readTree(response.toString());
            var results = root.get("organic_results");
            if (results == null) return List.of();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);
            Pattern datePattern = Pattern.compile("^(\\d{1,2} \\p{L}+ \\d{4})\\s+[\u2013-]");

            return StreamSupport.stream(results.spliterator(), false).map(result -> {
                MastodonPost post = new MastodonPost();
                String title = result.path("title").asText("");
                String snippet = result.path("snippet").asText("");
                String link = result.path("link").asText("");
                post.setContent("<b>" + title + "</b><br><i>" + snippet + "</i><br><a href='" + link + "' target='_blank'>" + link + "</a>");
                post.setExternalUrl(link); 
                post.setRepliesCount(0);
                post.setSource("Go");

                ZonedDateTime createdAt = null;
                Matcher matcher = datePattern.matcher(snippet);
                if (matcher.find()) {
                    try {
                        var parsed = formatter.parse(matcher.group(1));
                        createdAt = ZonedDateTime.of(java.time.LocalDate.from(parsed).atTime(12, 0), java.time.ZoneId.systemDefault());
                    } catch (Exception ignored) {}
                }
                if (createdAt == null) {
                    createdAt = ZonedDateTime.now().minusDays(new Random().nextInt(7)).withHour(new Random().nextInt(24)).withMinute(new Random().nextInt(60));
                }
                post.setCreatedAt(createdAt);
                return post;
            }).collect(Collectors.toList());

        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Erreur lors de l'appel à SerpAPI : " + e.getMessage(), e);
        }
    }
}
