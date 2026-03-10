import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class WorkerThread extends Thread {

    private final int threadId;
    private final SharedData data;
    private final CyclicBarrier barrier;
    private static final ObjectMapper mapper = new ObjectMapper();

    public WorkerThread(int threadId, SharedData data, CyclicBarrier barrier) {
        this.threadId = threadId;
        this.data = data;
        this.barrier = barrier;
    }

    @Override
    public void run() {
        try {
            phase1_ReadArticles();
            barrier.await();

            phase2a_CountForDuplicates();
            barrier.await();

            if (threadId == 0) {
                phase2b_IdentifyDuplicates();
            }
            barrier.await();

            phase2c_FilterUniqueArticles();
            barrier.await();

            if (threadId == 0) {
                data.finalizeUniqueArticles();
            }
            barrier.await();

            phase3_ProcessArticles();
            barrier.await();

            phase4_ProcessKeywords();
            barrier.await();

            if (threadId == 0) {
                phase5_GenerateOutput();
            }
            barrier.await();

        } catch (Exception e) {
            System.err.println("Error in thread " + threadId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void phase1_ReadArticles() {
        List<String> files = data.articleFiles;
        int totalFiles = files.size();

        for (int i = threadId; i < totalFiles; i += data.numThreads) {
            try {
                List<Article> articles = parseJsonFile(files.get(i));
                data.allArticles.addAll(articles);
            } catch (Exception e) {
                System.err.println("Error reading file: " + files.get(i));
            }
        }
    }

    private List<Article> parseJsonFile(String filePath) throws IOException {
        List<Article> articles = new ArrayList<>();
        JsonNode rootArray = mapper.readTree(new File(filePath));

        for (JsonNode node : rootArray) {
            Article article = parseArticle(node);
            if (article != null) {
                articles.add(article);
            }
        }
        return articles;
    }

    private Article parseArticle(JsonNode node) {
        try {
            String uuid = getTextOrEmpty(node, "uuid");
            String title = getTextOrEmpty(node, "title");
            String author = getTextOrEmpty(node, "author");
            String url = getTextOrEmpty(node, "url");
            String text = getTextOrEmpty(node, "text");
            String published = getTextOrEmpty(node, "published");
            String language = getTextOrEmpty(node, "language");
            String[] categories = extractCategories(node);

            if (uuid.isEmpty() || title.isEmpty()) {
                return null;
            }

            return new Article(uuid, title, author, url, text, published, language, categories);
        } catch (Exception e) {
            return null;
        }
    }

    private String getTextOrEmpty(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) return "";
        return field.asText("");
    }

    private String[] extractCategories(JsonNode node) {
        JsonNode categoriesNode = node.get("categories");
        if (categoriesNode == null || !categoriesNode.isArray()) {
            return new String[0];
        }
        List<String> catList = new ArrayList<>();
        for (JsonNode cat : categoriesNode) {
            if (!cat.isNull()) {
                catList.add(cat.asText());
            }
        }
        return catList.toArray(new String[0]);
    }

    private void phase2a_CountForDuplicates() {
        List<Article> articles = new ArrayList<>(data.allArticles);
        int total = articles.size();

        for (int i = threadId; i < total; i += data.numThreads) {
            Article article = articles.get(i);
            data.uuidCount.computeIfAbsent(article.uuid, k -> new AtomicInteger(0)).incrementAndGet();
            data.titleCount.computeIfAbsent(article.title, k -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    private void phase2b_IdentifyDuplicates() {
        data.uuidCount.forEach((uuid, count) -> {
            if (count.get() > 1) {
                data.duplicateUuids.add(uuid);
            }
        });

        data.titleCount.forEach((title, count) -> {
            if (count.get() > 1) {
                data.duplicateTitles.add(title);
            }
        });
    }

    private void phase2c_FilterUniqueArticles() {
        List<Article> articles = new ArrayList<>(data.allArticles);
        int total = articles.size();

        for (int i = threadId; i < total; i += data.numThreads) {
            Article article = articles.get(i);
            boolean isDuplicate = data.duplicateUuids.contains(article.uuid)
                    || data.duplicateTitles.contains(article.title);

            if (isDuplicate) {
                data.duplicatesCount.incrementAndGet();
            } else {
                data.uniqueArticles.add(article);
            }
        }
    }

    private void phase3_ProcessArticles() {
        List<Article> articles = data.uniqueArticlesList;
        if (articles == null) return;

        int total = articles.size();

        for (int i = threadId; i < total; i += data.numThreads) {
            Article article = articles.get(i);

            for (String cat : article.categories) {
                if (data.validCategories.contains(cat)) {
                    data.addToCategory(cat, article.uuid);
                }
            }

            if (data.validLanguages.contains(article.language)) {
                data.addToLanguage(article.language, article.uuid);
            }

            if (article.author != null && !article.author.isEmpty()) {
                data.incrementAuthor(article.author);
            }
        }
    }

    private void phase4_ProcessKeywords() {
        List<Article> englishArticles = data.englishArticlesList;
        if (englishArticles == null) return;

        int total = englishArticles.size();

        for (int i = threadId; i < total; i += data.numThreads) {
            Article article = englishArticles.get(i);
            if (article.text == null || article.text.isEmpty()) continue;

            Set<String> words = extractWords(article.text);
            for (String word : words) {
                data.addKeyword(word, article.uuid);
            }
        }
    }

    private Set<String> extractWords(String text) {
        Set<String> words = new HashSet<>();

        String[] tokens = text.toLowerCase().split("\\s+");

        for (String token : tokens) {
            StringBuilder cleanWord = new StringBuilder();
            for (int i = 0; i < token.length(); i++) {
                char c = token.charAt(i);
                if (c >= 'a' && c <= 'z') {
                    cleanWord.append(c);
                }
            }

            if (cleanWord.length() > 0) {
                String word = cleanWord.toString();
                if (!data.linkingWords.contains(word)) {
                    words.add(word);
                }
            }
        }

        return words;
    }

    private void phase5_GenerateOutput() throws IOException {
        generateAllArticlesFile();
        generateCategoryFiles();
        generateLanguageFiles();
        generateKeywordsFile();
        generateReportsFile();
    }

    private void generateAllArticlesFile() throws IOException {
        List<Article> articles = new ArrayList<>(data.uniqueArticlesList);

        articles.sort((a1, a2) -> {
            int cmp = a2.published.compareTo(a1.published);
            if (cmp != 0) return cmp;
            return a1.uuid.compareTo(a2.uuid);
        });

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("all_articles.txt"))) {
            for (Article article : articles) {
                writer.write(article.uuid + " " + article.published);
                writer.newLine();
            }
        }
    }

    private void generateCategoryFiles() throws IOException {
        for (String category : data.validCategories) {
            Set<String> uuids = data.categoriesMap.get(category);
            if (uuids == null || uuids.isEmpty()) continue;

            String normalizedName = Utils.normalizeCategoryName(category);
            String filename = normalizedName + ".txt";

            List<String> sortedUuids = new ArrayList<>(uuids);
            Collections.sort(sortedUuids);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
                for (String uuid : sortedUuids) {
                    writer.write(uuid);
                    writer.newLine();
                }
            }
        }
    }

    private void generateLanguageFiles() throws IOException {
        for (String language : data.validLanguages) {
            Set<String> uuids = data.languagesMap.get(language);
            if (uuids == null || uuids.isEmpty()) continue;

            String filename = language + ".txt";

            List<String> sortedUuids = new ArrayList<>(uuids);
            Collections.sort(sortedUuids);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
                for (String uuid : sortedUuids) {
                    writer.write(uuid);
                    writer.newLine();
                }
            }
        }
    }

    private void generateKeywordsFile() throws IOException {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>();
        data.keywordsMap.forEach((word, uuids) -> {
            entries.add(new AbstractMap.SimpleEntry<>(word, uuids.size()));
        });

        entries.sort((e1, e2) -> {
            int cmp = e2.getValue().compareTo(e1.getValue());
            if (cmp != 0) return cmp;
            return e1.getKey().compareTo(e2.getKey());
        });

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("keywords_count.txt"))) {
            for (Map.Entry<String, Integer> entry : entries) {
                writer.write(entry.getKey() + " " + entry.getValue());
                writer.newLine();
            }
        }
    }

    private void generateReportsFile() throws IOException {
        int duplicates = data.duplicatesCount.get();
        int unique = data.uniqueArticlesList.size();

        String bestAuthor = findBestAuthor();

        String topLanguage = findTopLanguage();

        String topCategory = findTopCategory();

        String mostRecentArticle = findMostRecentArticle();

        String topKeyword = findTopKeyword();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("reports.txt"))) {
            writer.write("duplicates_found - " + duplicates);
            writer.newLine();
            writer.write("unique_articles - " + unique);
            writer.newLine();
            writer.write("best_author - " + bestAuthor);
            writer.newLine();
            writer.write("top_language - " + topLanguage);
            writer.newLine();
            writer.write("top_category - " + topCategory);
            writer.newLine();
            writer.write("most_recent_article - " + mostRecentArticle);
            writer.newLine();
            writer.write("top_keyword_en - " + topKeyword);
            writer.newLine();
        }
    }

    private String findBestAuthor() {
        String bestAuthor = null;
        int maxCount = 0;

        for (Map.Entry<String, AtomicInteger> entry : data.authorCount.entrySet()) {
            String author = entry.getKey();
            int count = entry.getValue().get();

            if (count > maxCount || (count == maxCount && (bestAuthor == null || author.compareTo(bestAuthor) < 0))) {
                maxCount = count;
                bestAuthor = author;
            }
        }

        return bestAuthor != null ? bestAuthor + " " + maxCount : "N/A 0";
    }

    private String findTopLanguage() {
        String topLang = null;
        int maxCount = 0;

        for (Map.Entry<String, Set<String>> entry : data.languagesMap.entrySet()) {
            String lang = entry.getKey();
            int count = entry.getValue().size();

            if (count > maxCount || (count == maxCount && (topLang == null || lang.compareTo(topLang) < 0))) {
                maxCount = count;
                topLang = lang;
            }
        }

        return topLang != null ? topLang + " " + maxCount : "N/A 0";
    }

    private String findTopCategory() {
        String topCat = null;
        String topCatNormalized = null;
        int maxCount = 0;

        for (Map.Entry<String, Set<String>> entry : data.categoriesMap.entrySet()) {
            String cat = entry.getKey();
            String catNormalized = Utils.normalizeCategoryName(cat);
            int count = entry.getValue().size();

            if (count > maxCount ||
                    (count == maxCount && (topCatNormalized == null || catNormalized.compareTo(topCatNormalized) < 0))) {
                maxCount = count;
                topCat = cat;
                topCatNormalized = catNormalized;
            }
        }

        return topCatNormalized != null ? topCatNormalized + " " + maxCount : "N/A 0";
    }

    private String findMostRecentArticle() {
        Article mostRecent = null;

        for (Article article : data.uniqueArticlesList) {
            if (mostRecent == null) {
                mostRecent = article;
            } else {
                int cmp = article.published.compareTo(mostRecent.published);
                if (cmp > 0 || (cmp == 0 && article.uuid.compareTo(mostRecent.uuid) < 0)) {
                    mostRecent = article;
                }
            }
        }

        return mostRecent != null ? mostRecent.published + " " + mostRecent.url : "N/A N/A";
    }

    private String findTopKeyword() {
        String topWord = null;
        int maxCount = 0;

        for (Map.Entry<String, Set<String>> entry : data.keywordsMap.entrySet()) {
            String word = entry.getKey();
            int count = entry.getValue().size();

            if (count > maxCount || (count == maxCount && (topWord == null || word.compareTo(topWord) < 0))) {
                maxCount = count;
                topWord = word;
            }
        }

        return topWord != null ? topWord + " " + maxCount : "N/A 0";
    }
}