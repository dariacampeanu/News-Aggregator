import java.io.*;
import java.util.*;

public class Utils {

    public static Map<String, String[]> readAuxiliaryFiles(String inputsFile) throws IOException {
        Map<String, String[]> result = new HashMap<>();

        File inputsFileObj = new File(inputsFile);
        String baseDir = inputsFileObj.getParent();
        if (baseDir == null) baseDir = ".";

        BufferedReader br = new BufferedReader(new FileReader(inputsFile));
        br.readLine();
        String langFile = br.readLine().trim();
        String catFile = br.readLine().trim();
        String linkingFile = br.readLine().trim();
        br.close();

        result.put("languages", readLines(resolvePath(baseDir, langFile)));
        result.put("categories", readLines(resolvePath(baseDir, catFile)));
        result.put("linking_words", readLines(resolvePath(baseDir, linkingFile)));

        return result;
    }

    private static String[] readLines(String file) throws IOException {
        List<String> lines = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(file));
        br.readLine();
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(line.trim());
        }
        br.close();
        return lines.toArray(new String[0]);
    }

    public static List<String> readArticlesList(String articlesFile) throws IOException {
        List<String> files = new ArrayList<>();

        File articlesFileObj = new File(articlesFile);
        String baseDir = articlesFileObj.getParent();
        if (baseDir == null) baseDir = ".";

        BufferedReader br = new BufferedReader(new FileReader(articlesFile));
        int count = Integer.parseInt(br.readLine().trim());
        for (int i = 0; i < count; i++) {
            String relativePath = br.readLine().trim();
            files.add(resolvePath(baseDir, relativePath));
        }
        br.close();
        return files;
    }

    private static String resolvePath(String baseDir, String relativePath) {
        if (relativePath.startsWith("./")) {
            relativePath = relativePath.substring(2);
        }
        return new File(baseDir, relativePath).getPath();
    }

    public static String normalizeCategoryName(String category) {
        return category.replace(",", "").replaceAll("\\s+", "_");
    }
}