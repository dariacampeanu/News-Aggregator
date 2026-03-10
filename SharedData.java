import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class SharedData {

    public final List<String> articleFiles;
    public final Set<String> validLanguages;
    public final Set<String> validCategories;
    public final Set<String> linkingWords;
    public final int numThreads;

    public final ConcurrentLinkedQueue<Article> allArticles = new ConcurrentLinkedQueue<>();

    public final ConcurrentHashMap<String, AtomicInteger> uuidCount = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, AtomicInteger> titleCount = new ConcurrentHashMap<>();
    public final Set<String> duplicateUuids = ConcurrentHashMap.newKeySet();
    public final Set<String> duplicateTitles = ConcurrentHashMap.newKeySet();
    public final ConcurrentLinkedQueue<Article> uniqueArticles = new ConcurrentLinkedQueue<>();
    public final AtomicInteger duplicatesCount = new AtomicInteger(0);

    public final ConcurrentHashMap<String, Set<String>> categoriesMap = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, Set<String>> languagesMap = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, AtomicInteger> authorCount = new ConcurrentHashMap<>();

    public final ConcurrentHashMap<String, Set<String>> keywordsMap = new ConcurrentHashMap<>();

    public volatile List<Article> uniqueArticlesList = null;
    public volatile List<Article> englishArticlesList = null;

    public SharedData(List<String> articleFiles,
                      Set<String> validLanguages,
                      Set<String> validCategories,
                      Set<String> linkingWords,
                      int numThreads) {
        this.articleFiles = articleFiles;
        this.validLanguages = validLanguages;
        this.validCategories = validCategories;
        this.linkingWords = linkingWords;
        this.numThreads = numThreads;
    }

    public void finalizeUniqueArticles() {
        this.uniqueArticlesList = new ArrayList<>(uniqueArticles);

        List<Article> english = new ArrayList<>();
        for (Article a : uniqueArticlesList) {
            if ("english".equals(a.language)) {
                english.add(a);
            }
        }
        this.englishArticlesList = english;
    }

    public void addToCategory(String category, String uuid) {
        categoriesMap.computeIfAbsent(category, k -> ConcurrentHashMap.newKeySet()).add(uuid);
    }

    public void addToLanguage(String language, String uuid) {
        languagesMap.computeIfAbsent(language, k -> ConcurrentHashMap.newKeySet()).add(uuid);
    }

    public void incrementAuthor(String author) {
        authorCount.computeIfAbsent(author, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void addKeyword(String word, String uuid) {
        keywordsMap.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet()).add(uuid);
    }
}