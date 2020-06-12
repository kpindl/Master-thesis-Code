import lombok.Getter;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Getter
public class MIDAS_DB {

    private static final Logger LOGGER = Logger.getLogger(MIDAS_DB.class.getName());
    
    private Connection connection;
    private HashMap<String, Integer> enrichmentversion;
    private HashMap<String, String> map_symbol_identifier;

    private PreparedStatement pstmt_selectEnrvers;
    private PreparedStatement pstmt_selectTypeByName;
    private PreparedStatement pstmt_selectRegion;
    private PreparedStatement pstmt_selectRegionEnrvers;
    private PreparedStatement pstmt_insertRegionEnrvers;
    private PreparedStatement pstmt_insertRegion;
    private PreparedStatement pstmt_insertType;

    public MIDAS_DB() {
        try {
            connection = DriverManager.getConnection(getSqlConnectionString());
        } catch (SQLException | NullPointerException | IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } 

        LOGGER.setLevel(Level.WARNING);
        initPreparedStatements(); 
        initEnrichmentversion();
        loadChromosomes();
    }

    private void initPreparedStatements() {
        try {
            pstmt_selectEnrvers = connection.prepareStatement(
                    getFileContent("sql/table_enrichmentversion/select.sql"));
            pstmt_selectTypeByName = connection.prepareStatement(
                    getFileContent("sql/table_type/selectByName.sql"));
            pstmt_selectRegion = connection.prepareStatement(
                    getFileContent("sql/table_region/select.sql"));
            pstmt_selectRegionEnrvers = connection.prepareStatement(
                    getFileContent("sql/table_region_enrvers/select.sql"));
            pstmt_insertType = connection.prepareStatement(
                    getFileContent("sql/table_type/insert.sql"), Statement.RETURN_GENERATED_KEYS);
            pstmt_insertRegion = connection.prepareStatement(
                    getFileContent("sql/table_region/insert.sql"), Statement.RETURN_GENERATED_KEYS);
            pstmt_insertRegionEnrvers = connection.prepareStatement(
                    getFileContent("sql/table_region_enrvers/insert.sql"), Statement.RETURN_GENERATED_KEYS);

        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Fill HashMap that maps enrichmentversion name to id_enrvers
     */
    private void initEnrichmentversion() {

        this.enrichmentversion = new HashMap<>();

        try {
            ResultSet rs = pstmt_selectEnrvers.executeQuery();
            while(rs.next()) {
                int id_enrvers = rs.getInt("id_enrvers");
                String name = rs.getString("enrichmentversion");

                enrichmentversion.put(name, id_enrvers);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }


    /**
     * Select all chromosomes out of table region. Fills HashMaps
     * identifiers_symbol_id_reg and identifiers_identifier_symbol. Symbols of
     * mitochondrial chromosome will be changed from 'chrMT' to 'chrM'.
     */
    private void loadChromosomes() {

        map_symbol_identifier = new HashMap<>();

        try (PreparedStatement pstmt = connection.prepareStatement(getFileContent("sql/table_region/select_chr.sql"))) {

            /*
             * This is the id_type of region type region/chromosome
             */
            pstmt.setInt(1, 3);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {

                String symbol = rs.getString("symbol");
                String identifier = rs.getString("identifier");

                if (symbol.equals("chrMT"))
                    symbol = "chrM";

                /*
                 * symbol 'chr' must be skipped
                 */
                if (symbol.equals(("chr")))
                    continue;

                map_symbol_identifier.put(symbol, identifier);

            }

        } catch (SQLException | NullPointerException | IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

    }


    public Optional<Integer> getId_TypTarget() {
        try {
            pstmt_selectTypeByName.setString(1, "target");
            ResultSet rsSelect = pstmt_selectTypeByName.executeQuery();

            if(rsSelect.next()) {
                return Optional.of(rsSelect.getInt(1));

            } else {
                pstmt_insertType.setString(1, "target");
                pstmt_insertType.executeUpdate();
                ResultSet rsInsert = pstmt_insertType.getGeneratedKeys();

                if (rsInsert.next())
                    return Optional.of(rsInsert.getInt(1));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not get id_typ of 'target'.");
        }

        return Optional.empty();
    }


    /**
     * Checks whether region (id_typ, chromosome, g_start, g_end) exists in DB
     * and inserts new region otherwise
     *
     * @param id_typ
     * @param chromosome
     * @param g_start
     * @param g_end
     * @return Optional of id_reg
     */
    public Optional<Integer> insertRegion(int id_typ, String chromosome, int g_start, int g_end)  {
        try {
            pstmt_selectRegion.setInt(1, id_typ);
            pstmt_selectRegion.setString(2, map_symbol_identifier.get(chromosome));
            pstmt_selectRegion.setInt(3, g_start);
            pstmt_selectRegion.setInt(4, g_end);
            ResultSet rsSelect = pstmt_selectRegion.executeQuery();

            if(rsSelect.next()) {
                LOGGER.log(Level.INFO, "Region already in DB: " + chromosome + ":" + g_start + "-" + g_end);
                return Optional.of(rsSelect.getInt("id_reg"));
            }

            pstmt_insertRegion.setInt(1, id_typ);
            pstmt_insertRegion.setString(2, map_symbol_identifier.get(chromosome));
            pstmt_insertRegion.setInt(3, g_start);
            pstmt_insertRegion.setInt(4, g_end);

            pstmt_insertRegion.executeUpdate();
            ResultSet rsInsert = pstmt_insertRegion.getGeneratedKeys();

            if (rsInsert.next())
                LOGGER.log(Level.INFO, "Inserted region: " + chromosome + ":" + g_start + "-" + g_end);
                return Optional.of(rsInsert.getInt("id_reg"));


        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not get id_reg of region: " + chromosome + ":" + g_start + "-" + g_end);
            e.printStackTrace();
        }

        return Optional.empty();
    }


    /**
     * Checks whether a region_enrichmentversion already exists in DB
     * and inserts new table entry otherwise
     *
     * @param id_reg
     * @param id_enrvers
     */
    public Optional<Integer> insertRegionEnrichmentversion(int id_reg, int id_enrvers) {

        try {
            pstmt_selectRegionEnrvers.setInt(1, id_reg);
            pstmt_selectRegionEnrvers.setInt(2, id_enrvers);

            ResultSet rsSelect = pstmt_selectRegionEnrvers.executeQuery();
            if(rsSelect.next()) {
                int id_region_enrvers = rsSelect.getInt("id_region_enrvers");
                LOGGER.log(Level.INFO, "Region_enrichmentversion already in DB (id_region_enrvers: " + id_region_enrvers + ")");
                return Optional.of(id_region_enrvers);
            }

            pstmt_insertRegionEnrvers.setInt(1, id_reg);
            pstmt_insertRegionEnrvers.setInt(2, id_enrvers);

            pstmt_insertRegionEnrvers.executeUpdate();
            ResultSet rsInsert = pstmt_insertRegionEnrvers.getGeneratedKeys();

            if (rsInsert.next())
                LOGGER.log(Level.INFO, "Inserted region_enrichmentversion (id_reg: " + id_reg + ", id_enrvers: " + id_enrvers + ")");
                return Optional.of(rsInsert.getInt("id_region_enrvers"));

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not get id_region_enrvers of id_reg: " + id_reg + " and id_enrvers: " + id_enrvers);
            e.printStackTrace();
        }

        return Optional.empty();
    }

    // region helper functions
    private static String getSqlConnectionString() throws IOException {

        final Path CONFIGFILEPATH = Paths.get("conf/config.cfg");
        File file = new File(CONFIGFILEPATH.toString());
        Properties prop = new Properties();
        InputStream inputStream = new FileInputStream(file.getAbsolutePath());
        prop.load(inputStream);
        return prop.getProperty("sqlConnectionString");
    }

    /**
     * Reads file and returns file content in single string
     *
     * @param resourceName
     * @return
     * @throws IOException
     * @throws NullPointerException
     */
    public static String getFileContent(String resourceName) throws IOException, NullPointerException {
        Function<String, InputStream> getResource = ClassLoader.getSystemClassLoader()::getResourceAsStream;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getResource.apply(resourceName)))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }



    //endregion
}
