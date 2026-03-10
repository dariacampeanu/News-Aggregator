import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Tema1 {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Incorect arguments");
            System.exit(1);
        }

        try {
            int numThreads = Integer.parseInt(args[0]);
            String articlesFile = args[1];
            String inputsFile = args[2];

            Map<String, String[]> auxFiles = Utils.readAuxiliaryFiles(inputsFile);
            Set<String> validLanguages = new HashSet<>(Arrays.asList(auxFiles.get("languages")));
            Set<String> validCategories = new HashSet<>(Arrays.asList(auxFiles.get("categories")));
            Set<String> linkingWords = new HashSet<>(Arrays.asList(auxFiles.get("linking_words")));

            List<String> articleFiles = Utils.readArticlesList(articlesFile);

            SharedData sharedData = new SharedData(
                    articleFiles,
                    validLanguages,
                    validCategories,
                    linkingWords,
                    numThreads
            );

            CyclicBarrier barrier = new CyclicBarrier(numThreads);

            Thread[] threads = new Thread[numThreads];
            for (int i = 0; i < numThreads; i++) {
                threads[i] = new WorkerThread(i, sharedData, barrier);
            }

            for (int i = 0; i < numThreads; i++) {
                threads[i].start();
            }

            for (int i = 0; i < numThreads; i++) {
                threads[i].join();
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}