public class Article {
    public final String uuid;
    public final String title;
    public final String author;
    public final String url;
    public final String text;
    public final String published;
    public final String language;
    public final String[] categories;

    public Article(String uuid, String title, String author, String url,
                   String text, String published, String language, String[] categories) {
        this.uuid = uuid;
        this.title = title;
        this.author = author;
        this.url = url;
        this.text = text;
        this.published = published;
        this.language = language;
        this.categories = categories;
    }
}