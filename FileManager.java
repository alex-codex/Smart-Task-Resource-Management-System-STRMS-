import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe utilitaire pour la lecture et l'écriture de fichiers.
 */
public class FileManager {

    public static void writeLines(String filePath, List<String> lines, boolean append)
            throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, append))) {
            for (String line : lines) {
                pw.println(line);
            }
        }
    }

    public static List<String> readLines(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }

        return lines;
    }

    public static boolean fileExists(String filePath) {
        return new File(filePath).exists();
    }
}