import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TargetImportMain {

    private static final Path CONFIGFILEPATH = Paths.get("conf/config.cfg");
    private static final Logger LOGGER = Logger.getLogger(TargetImportMain.class.getName());

    private Path pathToTargetFiles;
    private MIDAS_DB database;

    public TargetImportMain() {
        database = new MIDAS_DB();
        readConfigFile();
    }

    public static void main(String[] args) {

        TargetImportMain main = new TargetImportMain();
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$-7s] %5$s %n");
        LOGGER.setLevel(Level.WARNING);
        main.startImport();
    }


    private void startImport() {

        Optional<Integer> id_typ = database.getId_TypTarget();
        if(!id_typ.isPresent()) {
            LOGGER.log(Level.SEVERE, "Could not get id_typ for target!");
            return;
        }

        List<Path> targetFiles = getTargetFiles();

        for (Path file : targetFiles) {
            System.out.println(file);
            Optional<Integer> id_enrvers = parseId_Enrvers(file);
            if (!id_enrvers.isPresent()) continue;

            try {
                Scanner scanner = new Scanner(file.toFile());
                Pattern pattern = Pattern.compile("(chr[0-9XY]+)\t(\\d+)\t(\\d+).*");

                while (scanner.hasNextLine()) {

                    // Parse line
                    String data = scanner.nextLine();
                    Matcher matcher = pattern.matcher(data);

                    if (!matcher.matches()) {
                        LOGGER.log(Level.WARNING, "Line could not be parsed: " + data);
                        continue;
                    }

                    String chromosome = matcher.group(1);
                    int g_start = Integer.parseInt(matcher.group(2));
                    int g_end = Integer.parseInt(matcher.group(3));


                    // Get id_reg or insert new region
                    Optional<Integer> id_reg = database.insertRegion(id_typ.get(), chromosome, g_start, g_end);
                    if(!id_reg.isPresent()) continue;

                    // Insert new region_enrichmentversion if necessary
                    Optional<Integer> id_region_enrvers = database.insertRegionEnrichmentversion(id_reg.get(), id_enrvers.get());
                }

                scanner.close();

            } catch (FileNotFoundException e) {
                LOGGER.log(Level.SEVERE, "Could not open file " + file);
            }
        }
    }


    private Optional<Integer> parseId_Enrvers(Path file) {

        Pattern filePattern = Pattern.compile("segmented_([A-Z]{2}\\.v\\d\\w?)_final.bed");
        Matcher enrversMatcher = filePattern.matcher(file.getFileName().toString());

        if(!enrversMatcher.matches()) return Optional.empty();

        String enrichmentversion = enrversMatcher.group(1);

        if(Objects.isNull(database.getEnrichmentversion().get(enrichmentversion))) {
            LOGGER.log(Level.WARNING, "Enrichtmentversion " + enrichmentversion + " does not exist in DB.");
            return Optional.empty();
        }

        return Optional.of(database.getEnrichmentversion().get(enrichmentversion));
    }

    private List<Path> getTargetFiles() {
        List<Path> targetFiles = new ArrayList<>();
        Pattern filePattern = Pattern.compile("segmented_([A-Z]{2}\\.v\\d\\w?)_final.bed");

        try (DirectoryStream<Path> pathStream = Files.newDirectoryStream(pathToTargetFiles)) {
            for (Path filePath : pathStream) {
                Matcher enrversMatcher = filePattern.matcher(filePath.getFileName().toString());
                if(enrversMatcher.matches()) targetFiles.add(filePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return targetFiles;
    }

    private void readConfigFile() {

        File file = new File(CONFIGFILEPATH.toString());

        try (InputStream inputStream = new FileInputStream(file.getAbsolutePath())) {

            Properties prop = new Properties();

            prop.load(inputStream);

            pathToTargetFiles = Paths.get(prop.getProperty("PathToTargetFiles"));

        } catch (NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage() + " " + file.getAbsolutePath(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}

