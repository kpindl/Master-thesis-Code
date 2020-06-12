package database;

import data.cnv.CNV;
import data.cnv.Target;
import data.run.UncategorisedRun;
import data.sample.Region;
import data.run.Run;
import data.run.RunState;
import data.sample.Sample;
import data.sample.SampleState;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static database.Resources.getFileContent;

/**
 * Database
 * Copyright (c) 2014 - Riccardo Brumm <riccardo.brumm@medizinische-genetik.de>
 * Zentrum fuer Humangenetik und Laboratoriumsdiagnostik (MVZ)
 * Dr. Klein, Dr. Rost und Kollegen
 * All rights reserved.
 * <p>
 * Permission to use, copy, modify, and/or distribute this software
 * for any purpose with or without fee is hereby granted
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE
 * FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
 * WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
 * ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 * <p>
 * This class handles the database connection with jdbc, puts the data into the
 * database and writes all names of runs added to the database into a logfile.
 */
public class MIDAS_DB {

    private static final Logger LOGGER = Logger.getLogger(MIDAS_DB.class.getName());
    private final int TARGET_TYPE = 7;
    private final int VAR_NOT_TESTED_ID_CSV = 1;

    private Connection connection;
    private HashMap<String, Integer> instrumentID;
    private HashMap<RunState, Integer> runstatus;
    private HashMap<SampleState, Integer> samplestatus;
    private HashMap<String, Integer> runID;
    /*
     * here chromosome names (e.g. 'chr1') are linked to its id_rrf in table
     * region_refseq
     */
    private HashMap<String, Integer> map_identifiers_symbol_id_rrf;
    /*
     * here is the chromosome identifier, e.g. NC_000007.14 mapped to its
     * common name like 'chr7'
     */
    public HashMap<String, String> map_identifiers_identifier_symbol;
    /*
     * here is the chromosome identifier symbol, e.g. 'chr7' mapped to its
     * identifier name like 'NC_000007.14'
     */
    private HashMap<String, String> map_identifier_symbol_identifiers;

    //region prepared statements
    private PreparedStatement pstmt_get_id_enrvers;
    private PreparedStatement pstmt_getEnrRegions;
    private PreparedStatement pstmt_getAllEnr;
    private PreparedStatement pstmt_selectCovered;
    private PreparedStatement pstmt_insertCovered;
    private PreparedStatement pstmt_getExistingId_dep;
    private PreparedStatement pstmt_insertRunDep;
    private PreparedStatement pstmt_getId_dep;
    private PreparedStatement pstmt_getMappedSeqIDs;
    private PreparedStatement pstmt_ImportHandlerID;
    private PreparedStatement pstmt_insertPatient;
    private PreparedStatement pstmt_selectIdRrfByTypeChromStartStop;
    private PreparedStatement pstmt_selectIdRrfByEnrversType_sorted;
    private PreparedStatement pstmt_select_g_end_previousTarget;
    private PreparedStatement pstmt_select_g_start_nextTarget;
    private PreparedStatement pstmt_select_RunByExp;
    private PreparedStatement pstmt_selectRunByID;
    private PreparedStatement pstmt_selectAllRun;
    private PreparedStatement pstmt_insertRun;
    private PreparedStatement pstmt_updateRun;
    private PreparedStatement pstmt_updateRunState;
    private PreparedStatement pstmt_getIdRus;
    private PreparedStatement pstmt_insertSample;
    private PreparedStatement pstmt_selectSamplesIdSas;
    private PreparedStatement pstmt_selectSamplesByPatnrIDenrversIDrun;
    private PreparedStatement pstmt_selectIdSamByPatnrIDenrversIDrun;
    private PreparedStatement pstmt_selectSampleByIDsam;
    private PreparedStatement pstmt_selectSampleByIDrun;
    private PreparedStatement pstmt_updateSample;
    private PreparedStatement pstmt_updateIDsas;
    private PreparedStatement pstmt_selectSampleState;
    private PreparedStatement pstmt_insertSampleLock;
    private PreparedStatement pstmt_selectSampleLock;
    private PreparedStatement pstmt_selectSamCNV_by_IdSam;
    private PreparedStatement pstmt_insert_sam_cnv;
    private PreparedStatement pstmt_selectSamTargetByIdCnvIdRrf;
    private PreparedStatement pstmt_insert_sam_target;
    private PreparedStatement pstmt_insert_sam_tar_cal;
    private PreparedStatement pstmt_insertRunLock;
    private PreparedStatement pstmt_selectRunLock;
    private PreparedStatement pstmt_select_table_size;
    private PreparedStatement pstmt_selectAllCNVCaller;
    //endregion



    //region Constructor and initialisers -  Implemented by another developer
    public MIDAS_DB() {

        try {
            connection = DriverManager.getConnection(getSqlConnectionString());
        } catch (SQLException | NullPointerException | IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        initPrepStatements();

        initInstrumentID();
        initRunStates();
        initSampleStates();
        initRunID();
        loadChromosomes();
    }


    /**
     * Initialises HashMap instrumentID by selecting all entries of table instrument.
     */
    private void initInstrumentID() {

        instrumentID = new HashMap<>();

        try (PreparedStatement pstmt = connection.prepareStatement(getFileContent("sql/table_instrument/select.sql"))) {

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {

                int id_ins = rs.getInt("id_ins");
                String name = rs.getString("illuminadevicename");

                instrumentID.put(name, id_ins);

            }

        } catch (SQLException | NullPointerException | IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * This method collects all run states from the database.
     */
    private void initRunStates() {
        runstatus = new HashMap<>();

        runstatus.put(RunState.PLANNED, get_id_rus("Geplant"));
        runstatus.put(RunState.SEQUENCING_IN_PROGRESS, get_id_rus("Seq. gestartet"));
        runstatus.put(RunState.SEQUENCING_COMPLETED, get_id_rus("Seq. abgeschl."));
        runstatus.put(RunState.DEMULTIPLEXING_IN_PROGRESS, get_id_rus("Dem. gestartet"));
        runstatus.put(RunState.DEMULTIPLEXING_COMPLETED, get_id_rus("Dem. abgeschl."));
        runstatus.put(RunState.ANALYSIS_IN_PROGRESS, get_id_rus("Ana. gestartet"));
        runstatus.put(RunState.ANALYSIS_COMPLETED, get_id_rus("Ana. abgeschl."));
        runstatus.put(RunState.IMPORT_STARTET, get_id_rus("Import gestartet"));
        runstatus.put(RunState.IMPORT_COVERAGE_COMPLETED, get_id_rus("Import Coverage abgeschl."));
        runstatus.put(RunState.IMPORT_COMPLETED, get_id_rus("Import abgeschl."));
        runstatus.put(RunState.ARCHIVED, get_id_rus("Archiviert"));
        runstatus.put(RunState.LOCKED_SEQUENCING_IN_PROGRESS, get_id_rus("Seq. gestartet gesp."));
        runstatus.put(RunState.LOCKED_SEQUENCING_COMPLETED, get_id_rus("Seq. abgeschl. gesp."));
        runstatus.put(RunState.LOCKED_DEMULTIPLEXING_IN_PROGRESS, get_id_rus("Dem. gestartet gesp."));
        runstatus.put(RunState.LOCKED_DEMULTIPLEXING_COMPLETED, get_id_rus("Dem. abgeschl. gesp."));
        runstatus.put(RunState.LOCKED_ANALYSIS_IN_PROGRESS, get_id_rus("Ana. gestartet gesp."));
        runstatus.put(RunState.LOCKED_ANALYSIS_COMPLETED, get_id_rus("Ana. abgeschl. gesp."));
        runstatus.put(RunState.LOCKED_IMPORT_STARTET, get_id_rus("Import gestartet gesp."));
        runstatus.put(RunState.LOCKED_IMPORT_COVERAGE_COMPLETED, get_id_rus("Import Coverage abgeschl. gesp."));
        runstatus.put(RunState.LOCKED_IMPORT_COMPLETED, get_id_rus("Import abgeschl. gesp."));
        runstatus.put(RunState.LOCKED_ARCHIVED, get_id_rus("Archiviert gesp."));
    }

    /**
     * Initialises HashMap samplestatus.
     */
    private void initSampleStates() {
        samplestatus = new HashMap<>();

        samplestatus.put(SampleState.PLANNED, getId_sas("Geplant"));
        samplestatus.put(SampleState.NOTFOUND, getId_sas("Nicht Gefunden"));
        samplestatus.put(SampleState.SEQUENCING_ANALYSIS_STARTED, getId_sas("Sequenzierung und Analyse gestartet"));
        samplestatus.put(SampleState.ANALYSIS_PENDING, getId_sas("Auswertung Ausstehend"));
        samplestatus.put(SampleState.ANALYSIS_PREPARED, getId_sas("Auswertung Vorbereitet"));
        samplestatus.put(SampleState.ANALYSIS_STARTED, getId_sas("Auswertung Gestartet"));
        samplestatus.put(SampleState.ANALYSIS_DONE, getId_sas("Auswertung Abgeschlossen"));
        samplestatus.put(SampleState.LOCKED_ANALYSIS_PENDING, getId_sas("Auswertung Ausstehend gesp."));
        samplestatus.put(SampleState.LOCKED_ANALYSIS_PREPARED, getId_sas("Auswertung Vorbereitet gesp."));
        samplestatus.put(SampleState.LOCKED_ANALYSIS_STARTED, getId_sas("Auswertung Gestartet gesp."));
        samplestatus.put(SampleState.LOCKED_ANALYSIS_DONE, getId_sas("Auswertung Abgeschlossen gesp."));
        samplestatus.put(SampleState.LOCKED_SEQUENCING_ANALYSIS_STARTED, getId_sas("Sequenzierung und Analyse gestartet gesp."));
    }

    /**
     * Select all chromosomes out of table region. Fills HashMaps
     * identifiers_symbol_id_rrf and identifiers_identifier_symbol. Symbols of
     * mitochondrial chromosome will be changed from 'chrMT' to 'chrM'.
     */
    private void loadChromosomes() {

        map_identifiers_symbol_id_rrf = new HashMap<>();
        map_identifiers_identifier_symbol = new HashMap<>();
        map_identifier_symbol_identifiers = new HashMap<>();

        try (PreparedStatement pstmt = connection.prepareStatement(getFileContent("sql/table_region/select_chr.sql"))) {

            /*
             * This is the id_type of region type region/chromosome
             */
            pstmt.setInt(1, 3);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {

                int id_rrf = rs.getInt("id_rrf");
                String symbol = rs.getString("symbol");
                String identifier = rs.getString("identifier");

                if (symbol.equals("chrMT"))
                    symbol = "chrM";

                /*
                 * symbol 'chr' must be skipped
                 */
                if (symbol.equals(("chr")))
                    continue;

                map_identifiers_symbol_id_rrf.put(symbol, id_rrf);
                map_identifiers_identifier_symbol.put(identifier, symbol);
                map_identifier_symbol_identifiers.put(symbol, identifier);
            }

        } catch (SQLException | NullPointerException | IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

    }


    private void initPrepStatements() {
        try {
            pstmt_get_id_enrvers = connection.prepareStatement(
                    getFileContent("sql/table_enrichmentversion/select_by_enrichmentversion.sql"));
            pstmt_getEnrRegions = connection.prepareStatement(
                    getFileContent("sql/table_enrichmentversion/select_enriched_regions.sql"));
            pstmt_getAllEnr = connection.prepareStatement(
                    getFileContent("sql/table_enrichmentversion/select.sql"));
            pstmt_selectCovered = connection.prepareStatement(
                    getFileContent("sql/table_covered/select.sql"));
            pstmt_insertCovered = connection.prepareStatement(
                    getFileContent("sql/table_covered/insert.sql"));
            pstmt_getExistingId_dep = connection.prepareStatement(
                    getFileContent("sql/table_run_dep/select.sql"));
            pstmt_insertRunDep = connection.prepareStatement(
                    getFileContent("sql/table_run_dep/insert.sql"));
            pstmt_getId_dep = connection.prepareStatement(
                    getFileContent("sql/table_department/select.sql"));
            pstmt_getMappedSeqIDs = connection.prepareStatement(
                    getFileContent("sql/table_instrument/select.sql"));
            pstmt_ImportHandlerID = connection.prepareStatement(
                    getFileContent("sql/modulcontent_table_user/select.sql"));
            pstmt_insertPatient = connection.prepareStatement(
                    getFileContent("sql/table_patient/insert.sql"), PreparedStatement.RETURN_GENERATED_KEYS);
            pstmt_selectIdRrfByTypeChromStartStop = connection.prepareStatement(
                    getFileContent("sql/table_region_refseq/select_idRrf_by_idTyp_identifier_start_stop.sql"));
            pstmt_selectIdRrfByEnrversType_sorted = connection.prepareStatement(
                    getFileContent("sql/table_region_refseq/select_idRrf_By_EnrversType_sorted.sql"));
            pstmt_select_g_end_previousTarget = connection.prepareStatement(
                    getFileContent("sql/table_region_refseq/select_g_end_previousTarget"));
            pstmt_select_g_start_nextTarget = connection.prepareStatement(
                    getFileContent("sql/table_region_refseq/select_g_start_nextTarget"));
            pstmt_select_RunByExp = connection.prepareStatement(
                    getFileContent("sql/table_run/select_id_run_by_experimentname.sql"));
            pstmt_selectRunByID = connection.prepareStatement(
                    getFileContent("sql/table_run/select_by_id_run.sql"));
            pstmt_selectAllRun = connection.prepareStatement(
                    getFileContent("sql/table_run/select_id_run_illuminarunname.sql"));
            pstmt_insertRun = connection.prepareStatement(
                    getFileContent("sql/table_run/insert.sql"), PreparedStatement.RETURN_GENERATED_KEYS);
            pstmt_updateRun = connection.prepareStatement(
                    getFileContent("sql/table_run/update.sql"));
            pstmt_updateRunState = connection.prepareStatement(
                    getFileContent("sql/table_run/updateRunState.sql"));
            pstmt_getIdRus = connection.prepareStatement(
                    getFileContent("sql/table_runstatus/select.sql"));
            pstmt_insertSample = connection.prepareStatement(
                    getFileContent("sql/table_sample/insert.sql"), PreparedStatement.RETURN_GENERATED_KEYS);
            pstmt_selectSamplesIdSas = connection.prepareStatement(
                    getFileContent("sql/table_sample/select_id_sas_by_id_sam.sql"));
            pstmt_selectSamplesByPatnrIDenrversIDrun = connection.prepareStatement(
                    getFileContent("sql/table_sample/select_by_patnr_id_enrvers_id_run.sql"));
            pstmt_selectIdSamByPatnrIDenrversIDrun = connection.prepareStatement(
                    getFileContent("sql/table_sample/select_id_sam_by_patnr_id_enrvers_id_run.sql"));
            pstmt_selectSampleByIDsam = connection.prepareStatement(
                    getFileContent("sql/table_sample/select_by_id_sam.sql"));
            pstmt_selectSampleByIDrun = connection.prepareStatement(
                    getFileContent("sql/table_sample/select_by_id_run.sql"));
            pstmt_updateSample = connection.prepareStatement(
                    getFileContent("sql/table_sample/update.sql"));
            pstmt_updateIDsas = connection.prepareStatement(
                    getFileContent("sql/table_sample/update_id_sas_by_id_sam.sql"));
            pstmt_selectSampleState = connection.prepareStatement(
                    getFileContent("sql/table_samplestatus/select.sql"));
            pstmt_insertSampleLock = connection.prepareStatement(
                    getFileContent("sql/table_samplelock/insert.sql"));
            pstmt_selectSampleLock = connection.prepareStatement(
                    getFileContent("sql/table_samplelock/select.sql"));
            pstmt_selectSamCNV_by_IdSam = connection.prepareStatement(
                    getFileContent("sql/table_sam_cnv/select_by_id_sam.sql"));
            pstmt_insert_sam_cnv = connection.prepareStatement(
                    getFileContent("sql/table_sam_cnv/insert.sql"), PreparedStatement.RETURN_GENERATED_KEYS);
            pstmt_insert_sam_target = connection.prepareStatement(
                    getFileContent("sql/table_sam_target/insert.sql"), PreparedStatement.RETURN_GENERATED_KEYS);
            pstmt_selectSamTargetByIdCnvIdRrf = connection.prepareStatement(
                    getFileContent("sql/table_sam_target/select_by_IdCnv_IdRrf"));
            pstmt_insert_sam_tar_cal = connection.prepareStatement(
                    getFileContent("sql/table_sam_tar_cal/insert.sql"), PreparedStatement.RETURN_GENERATED_KEYS);
            pstmt_insertRunLock = connection.prepareStatement(
                    getFileContent("sql/table_runlock/insert.sql"));
            pstmt_selectRunLock = connection.prepareStatement(
                    getFileContent("sql/table_runlock/select.sql"));
            pstmt_select_table_size = connection.prepareStatement(
                    getFileContent("sql/select_table_size.sql"));
            pstmt_selectAllCNVCaller = connection.prepareStatement(
                    getFileContent("sql/table_caller/select.sql"));
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }
    //endregion


    //region helpers - Implemented by another developer
    /**
     * This method inserts a {@link Types}.VARCHAR, {@link Types}.NUMERIC or {@link Types}.DATE value into a given
     * {@link PreparedStatement} or null if the value is null, respectively.
     *
     * @param pstmt {@link PreparedStatement} of query
     * @param index index where value should be inserted within {@link PreparedStatement}
     * @param data  value
     * @param type  final int value of class Types
     * @param <T>   generic value object
     * @throws SQLException
     * @throws IllegalArgumentException if a in this implementation not covered {@link Types} is choosen.
     */
    private <T> void setValueOrNull(PreparedStatement pstmt, int index, T data, int type) throws SQLException {
        if (Objects.isNull(data)) {
            pstmt.setNull(index, type);
        } else if (Objects.equals(Types.VARCHAR, type)) {
            pstmt.setString(index, (String) data);
        } else if (Objects.equals(Types.NUMERIC, type)) {
            pstmt.setBigDecimal(index, new BigDecimal(data.toString()));
        } else if (Objects.equals(Types.DATE, type)) {
            pstmt.setDate(index, (Date) data);
        } else {
            throw new IllegalArgumentException(
                    "Choosen java.SQL.Types (" + type + ") not covered in this implementation");
        }
    }
    //endregion


    //region table covered - Implemented by another developer
    /**
     * This method inserts all covered regions of an sample of a chromosome. Checks if regions already exists in
     * database first.
     *
     * @param chr     chromosome
     * @param id_sam  sample id
     * @param regions enrichment regions
     */
    public void insertIntoTableCoveredIfNotExists(String chr, int id_sam, ArrayList<Region> regions) {

        ArrayList<Region> existingEntries = selectTableCovered(chr, id_sam);

        ArrayList<Region> nonExistingEntries = new ArrayList<>();

        for (Region region : regions) {
            if (!existingEntries.contains(region))
                nonExistingEntries.add(region);
        }

        insertIntoTableCovered(chr, id_sam, nonExistingEntries);

    }

    /**
     * This method returns all entries of table covered of a given sample and chromosome.
     *
     * @param chr
     * @param id_sam
     * @return
     */
    public ArrayList<Region> selectTableCovered(String chr, int id_sam) {
        ArrayList<Region> coverageList = new ArrayList<>();

        try {

            pstmt_selectCovered.setString(1, chr);
            pstmt_selectCovered.setInt(2, id_sam);

            ResultSet rs = pstmt_selectCovered.executeQuery();

            while (rs.next()) {
                Region region = new Region();
                region.setG_start(rs.getInt("g_start"));
                region.setG_end(rs.getInt("g_end"));
                coverageList.add(region);
            }

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return coverageList;
    }

    /**
     * This method inserts a list of Regions into the database.
     *
     * @param chr
     * @param id_sam
     * @param regions
     */
    public void insertIntoTableCovered(String chr, int id_sam, ArrayList<Region> regions) {
        try {

            for (Region region : regions) {

                pstmt_insertCovered.setInt(1, id_sam);
                pstmt_insertCovered.setString(2, chr);
                pstmt_insertCovered.setInt(3, region.getG_start());
                pstmt_insertCovered.setInt(4, region.getG_end());

                pstmt_insertCovered.executeUpdate();
            }

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }
    //endregion


    //region tables department and run_dep - Implemented by another developer
    /**
     * This method inserts all id_dep of a given {@link Run} if they do not exists yet.
     *
     * @param id_run
     * @param id_deps
     */
    public void insertIntoTable_run_dep_IfNotExists(int id_run, List<Integer> id_deps) {

        Set<Integer> containingId_deps = getExistingId_dep(id_run);

        List<Integer> id_depToBeInserted = id_deps
                .stream()
                .filter(e -> !containingId_deps.contains(e))
                .collect(Collectors.toList());

        insertIntoTable_run_dep(id_depToBeInserted, id_run);
    }

    /**
     * This method retuns a list of all departments already linked to a given run.
     *
     * @param id_run of a given run
     * @return List of id_dep
     */
    private Set<Integer> getExistingId_dep(int id_run) {
        Set<Integer> containingId_deps = new HashSet<>();
        try {
            pstmt_getExistingId_dep.setInt(1, id_run);
            ResultSet rs = pstmt_getExistingId_dep.executeQuery();
            while (rs.next()) {
                int id_dep = rs.getInt("id_dep");
                containingId_deps.add(id_dep);
            }

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return containingId_deps;
    }

    /**
     * Inserts a list of department to a given run into table run_dep.
     *
     * @param id_deps
     * @param id_run
     */
    private void insertIntoTable_run_dep(List<Integer> id_deps, int id_run) {
        try {
            for (int id_dep : id_deps) {
                pstmt_insertRunDep.setInt(1, id_run);
                pstmt_insertRunDep.setInt(2, id_dep);
                pstmt_insertRunDep.executeUpdate();
            }
        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Returns an id_dep of a given department string. Throws {@link NullPointerException} in case department ist not
     * known.
     *
     * @param department
     * @return corresponding id_dep to a given department string
     */
    public int getId_dep(String department) throws IllegalArgumentException {

        try {

            ResultSet rs = pstmt_getId_dep.executeQuery();
            while (rs.next()) {
                int id_dep = rs.getInt("id_dep");
                String department_db = rs.getString("name");
                if (department_db.equals(department)) {
                    return id_dep;
                }
            }

        } catch (NullPointerException | SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        throw new IllegalArgumentException("Department not known: " + department);
    }

    /**
     * Returns a {@link List} of id_dep of a given {@link List} of department strings.
     *
     * @param departments
     * @return {@link List} of id_dep to a given department {@link List}
     */
    public List<Integer> getId_depList(List<String> departments) {
        List<Integer> id_deps = new ArrayList<>();
        for (String department : departments) {
            try {
                id_deps.add(getId_dep(department));
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return id_deps;
    }
    //endregion


    //region table enrichmentversion - Implemented by another developer
    /**
     * Retuns id_enrvers of a given enrichmentversion (String). Returns default value id_enrvers = 1 in case that a
     * given enrichmentversion is not known. Throws {@link NullPointerException} if
     *
     * @param enrichmentversion
     * @return id_enrvers
     */
    public int get_id_enrvers(String enrichmentversion) {
        try {
            pstmt_get_id_enrvers.setString(1, enrichmentversion);
            ResultSet rs = pstmt_get_id_enrvers.executeQuery();
            if (rs.next()) {
                return rs.getInt("id_enrvers");
            } else {
                return 1;
            }
        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        throw new NullPointerException("Enrichmentversion not known: " + enrichmentversion);
    }

    /**
     * This method selects all enriched regions of an enrichment version.
     *
     * @param id_enrvers
     * @return List of start and end coordinates (Integer[]) wrapped in a hash map, separated by chromosome (key)
     */
    public HashMap<String, ArrayList<Integer[]>> getEnrichedRegions(int id_enrvers) {

        HashMap<String, ArrayList<Integer[]>> enriched = new HashMap<>();

        try {

            pstmt_getEnrRegions.setInt(1, id_enrvers);

            ResultSet rs = pstmt_getEnrRegions.executeQuery();
            while (rs.next()) {
                String identifier = rs.getString("identifier");
                String chr = map_identifiers_identifier_symbol.get(identifier);
                int g_start = rs.getInt("g_start");
                int g_end = rs.getInt("g_end");
                Integer[] region = {g_start, g_end};

                if (enriched.containsKey(chr)) {
                    ArrayList<Integer[]> regions = enriched.get(chr);
                    regions.add(region);
                    enriched.put(chr, regions);
                } else {
                    ArrayList<Integer[]> regions = new ArrayList<>();
                    regions.add(region);
                    enriched.put(chr, regions);
                }

            }

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return enriched;

    }

    /**
     * This method gets all enrichmentversions contained in table enrichmentversion.
     *
     * @return available enrichmentversions
     */
    public HashMap<String, Integer> getAllEnrichmentversions() {

        HashMap<String, Integer> enrichmentversions = new HashMap<>();

        try {

            ResultSet rs = pstmt_getAllEnr.executeQuery();

            while (rs.next()) {
                String enrichmentversion = rs.getString("enrichmentversion");
                Integer id_enrvers = rs.getInt("id_enrvers");
                enrichmentversions.put(enrichmentversion, id_enrvers);
            }

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return enrichmentversions;
    }
    //endregion


    //region table instrument - Implemented by another developer
    /**
     * @return {@link HashMap} filled to all inhouse sequencers names (key) the corresponding illumina device id (value
     * as {@link ArrayList}).
     */
    public HashMap<String, ArrayList<String>> getMappedSequencerIDs() {

        HashMap<String, ArrayList<String>> mappedSequencerIDs = new HashMap<>();

        try {

            ResultSet rs = pstmt_getMappedSeqIDs.executeQuery();

            while (rs.next()) {
                String illuminaID = rs.getString("illuminadevicename");
                String inhousename = rs.getString("inhousename");

                if (mappedSequencerIDs.containsKey(inhousename)) {
                    ArrayList<String> illuminaIDs = mappedSequencerIDs.get(inhousename);
                    illuminaIDs.add(illuminaID);
                    mappedSequencerIDs.put(inhousename, illuminaIDs);
                } else {
                    ArrayList<String> illuminaIDs = new ArrayList<>();
                    illuminaIDs.add(illuminaID);
                    mappedSequencerIDs.put(inhousename, illuminaIDs);
                }
            }

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return mappedSequencerIDs;
    }
    //endregion


    //region table modulcontent.user - Implemented by another developer
    /**
     * This method returns the id_usr of user userimporthandler (column username).
     *
     * @return id_usr of user MIDAS Import Handler
     */
    public int get_id_usr_ImportHandler() throws NullPointerException {
        try {
            ResultSet rs = pstmt_ImportHandlerID.executeQuery();
            if (rs.next())
                return rs.getInt("id");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        throw new NullPointerException("id for userimporthandler not found");
    }
    //endregion


    //region table patient - Implemented by another developer
    /**
     * Inserts a new entry in table patient with a given sex.
     *
     * @param id_sex 1 - unknown; 2 - male; 3 - female; 4 - unknown, because prenatal
     * @return id_pat of newly inserted entry, or null otherwise
     */
    public Optional<Integer> insertIntoTablePatient(int id_sex) {
        Optional<Integer> id_pat = Optional.empty();
        try {

            pstmt_insertPatient.setInt(1, id_sex);
            pstmt_insertPatient.executeUpdate();
            ResultSet rs = pstmt_insertPatient.getGeneratedKeys();
            while (rs.next())
                id_pat = Optional.of(rs.getInt(1));

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return id_pat;
    }
    //endregion


    // region table region - Implemented by another developer
    /**
     * Returns id_rrf for given identifier, g_start and g_end of target
     *
     * @param identifier
     * @param g_start
     * @param g_end
     * @return id_rrf
     */

    public Optional<Integer> getIdRrfTarget(String identifier, int g_start, int g_end) {
        try {
            pstmt_selectIdRrfByTypeChromStartStop.setInt(1, TARGET_TYPE);
            pstmt_selectIdRrfByTypeChromStartStop.setString(2, identifier);
            pstmt_selectIdRrfByTypeChromStartStop.setInt(3, g_start);
            pstmt_selectIdRrfByTypeChromStartStop.setInt(4, g_end);

            ResultSet rs = pstmt_selectIdRrfByTypeChromStartStop.executeQuery();

            if(rs.next()) {
                return Optional.of(rs.getInt("id_rrf"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        LOGGER.log(Level.WARNING, "Could not find id_rrf of target: " + identifier + ":" + g_start + "-" + g_end);
        return Optional.empty();
    }

    /**
     * This method returns a HashMap that maps a target (id_rrf)
     * on the subsequent target (id_rrf) for a given enrichmentversion
     *
     * @param id_enrvers
     * @return
     */
    public HashMap<Integer, Integer> fillOrderReferenceTargets(int id_enrvers) {

        HashMap<Integer, Integer> sortedReferenceTargets = new HashMap<>();

        try {
            pstmt_selectIdRrfByEnrversType_sorted.setInt(1, TARGET_TYPE);
            pstmt_selectIdRrfByEnrversType_sorted.setInt(2, id_enrvers);
            ResultSet rs = pstmt_selectIdRrfByEnrversType_sorted.executeQuery();

            if(rs.next()) {
                int prevIdRrf = rs.getInt("id_rrf");

                while (rs.next()) {
                    sortedReferenceTargets.put(prevIdRrf, rs.getInt("id_rrf"));
                    prevIdRrf = rs.getInt("id_rrf");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return sortedReferenceTargets;
    }


    public Optional<Integer> getG_EndOfPrevTarget(Target target, int id_enrvers) {
        try {
            pstmt_select_g_end_previousTarget.setInt(1, TARGET_TYPE);
            pstmt_select_g_end_previousTarget.setInt(2, id_enrvers);
            pstmt_select_g_end_previousTarget.setString(3, target.getIdentifier());
            pstmt_select_g_end_previousTarget.setInt(4, target.getG_start());

            ResultSet rs = pstmt_select_g_end_previousTarget.executeQuery();

            if(rs.next()) {
                return Optional.of(rs.getInt("g_end"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

//        LOGGER.log(Level.INFO, "Could not find preceding target to given target.");
        return Optional.empty();
    }


    public Optional<Integer> getG_StartOfNextTarget(Target target, int id_enrvers) {
        try {
            pstmt_select_g_start_nextTarget.setInt(1, TARGET_TYPE);
            pstmt_select_g_start_nextTarget.setInt(2, id_enrvers);
            pstmt_select_g_start_nextTarget.setString(3, target.getIdentifier());
            pstmt_select_g_start_nextTarget.setInt(4, target.getG_end());

            ResultSet rs = pstmt_select_g_start_nextTarget.executeQuery();

            if(rs.next()) {
                return Optional.of(rs.getInt("g_start"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

//        LOGGER.log(Level.INFO, "Could not find preceding target to given target.");
        return Optional.empty();

    }
    // endregion


    //region table run - Implemented by another developer
    /**
     * This method returns all runs found in database with the given experiment name.
     *
     * @param experimentname
     * @return exists Boolean value that indicates whether the run has already been imported into the database.
     */
    public List<Run> select_Run(String experimentname) {

        List<Run> id_runs = new ArrayList<>();

        try {

            pstmt_select_RunByExp.setString(1, experimentname);
            ResultSet rs = pstmt_select_RunByExp.executeQuery();

            while (rs.next()) {
                Run run = select_Run(rs.getInt("id_run"));
                id_runs.add(run);
            }

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return id_runs;
    }

    /**
     * @param id_run
     * @return a {@link Run} based on its id_run with all information stored within the database.
     */
    public Run select_Run(int id_run) {

        Run run = new UncategorisedRun();

        try {

            pstmt_selectRunByID.setInt(1, id_run);
            ResultSet rs = pstmt_selectRunByID.executeQuery();

            while (rs.next()) {
                run.set_id_run((rs.getInt("id_run")));
                int id_rus = rs.getInt("id_rus");
                RunState runstate = runstatus
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue() == id_rus)
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);
                run.setRunState(runstate);
                run.setRunName(rs.getString("illuminarunname"));
                run.setFlowcellID(rs.getString("flowcellid"));
                Date date = rs.getDate("runstartdate");
                if (!rs.wasNull())
                    run.setRunStartDate(date.toLocalDate());
                run.setYield(rs.getDouble("yield"));
                run.setCluster(rs.getLong("cluster"));
                run.setClusterPF(rs.getLong("clusterpf"));
                run.setPercentPF(rs.getDouble("percentpf"));
                run.setPercentReadsIdentifiedPF(rs.getDouble("percentreadsidentifiedpf"));
                run.setClusterDensity(rs.getDouble("clusterdensity"));
                run.setPercentAligned(rs.getDouble("percentaligned"));
                run.setExperimentName(rs.getString("experimentname"));
                run.setForwardReadLength(rs.getInt("forwardreadlength"));
                run.setReverseReadLength(rs.getInt("reversereadlength"));
                run.setAverageQuality(rs.getDouble("averagequality"));
                run.setQualOver30(rs.getDouble("qualover30"));
                run.setFlowCellSerialBarCode(rs.getString("flowcellserialbarcode"));
                run.setFlowCellLotNumber(rs.getString("flowcelllotnumber"));
                run.setSbsSerialBarcode(rs.getString("sbsserialbarcode"));
                run.setSbsLotNumber(rs.getString("sbslotnumber"));
                run.setClusterSerialBarcode(rs.getString("clusterserialbarcode"));
                run.setClusterLotNumber(rs.getString("clusterlotnumber"));
                run.setBufferSerialBarcode(rs.getString("bufferserialbarcode"));
                run.setBufferLotNumber(rs.getString("bufferlotnumber"));
                run.setInvestigatorName(rs.getString("investigatorname"));
            }

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return run;
    }

    public Optional<Integer> selectRunIDByIlluminarunname(String illuminarunname) {
        if(runID.containsKey(illuminarunname)) {
            return Optional.of(runID.get(illuminarunname));
        } else {
            return Optional.empty();
        }
    }

    /**
     * This method inserts a {@link Run} into the database and adds id_run to given instance of {@link Run}.
     *
     * @param run
     */
    public Optional<Integer> insertIntoTableRun(Run run) throws IOException, SQLException {

        Optional<Integer> id_run = Optional.empty();
        try {

            pstmt_insertRun.setString(1, run.getRunName());//this cannot be null and must throw an excepterion if null
            setValueOrNull(pstmt_insertRun, 2, run.getFlowcellID(), Types.VARCHAR);
            setValueOrNull(pstmt_insertRun, 3, run.getAssay(), Types.VARCHAR);
            setValueOrNull(pstmt_insertRun, 4, Date.valueOf(run.getRunStartDate()), Types.DATE);
            setValueOrNull(pstmt_insertRun, 5, run.getYield(), Types.NUMERIC);
            setValueOrNull(pstmt_insertRun, 6, run.getCluster(), Types.NUMERIC);
            setValueOrNull(pstmt_insertRun, 7, run.getClusterPF(), Types.NUMERIC);
            setValueOrNull(pstmt_insertRun, 8, run.getPercentPF(), Types.NUMERIC);
            setValueOrNull(pstmt_insertRun, 9, run.getPercentReadsIdentifiedPF(), Types.NUMERIC);
            setValueOrNull(pstmt_insertRun, 10, run.getClusterDensity(), Types.NUMERIC);
            setValueOrNull(pstmt_insertRun, 11, run.getPercentAligned(), Types.NUMERIC);
            setValueOrNull(pstmt_insertRun, 12, run.getExperimentName(), Types.VARCHAR);
            setValueOrNull(pstmt_insertRun, 13, run.getForwardReadLength(), Types.NUMERIC);
            setValueOrNull(pstmt_insertRun, 14, run.getReverseReadLength(), Types.NUMERIC);
            setValueOrNull(pstmt_insertRun, 15, run.getAverageQuality(), Types.NUMERIC);
            setValueOrNull(pstmt_insertRun, 16, run.getQualOver30(), Types.NUMERIC);
            setValueOrNull(pstmt_insertRun, 17, runstatus.get(run.getRunState()), Types.NUMERIC);
            setValueOrNull(pstmt_insertRun, 18, instrumentID.get(run.getInstrument()), Types.NUMERIC);
            setValueOrNull(pstmt_insertRun, 19, run.getFlowCellSerialBarCode(), Types.VARCHAR);
            setValueOrNull(pstmt_insertRun, 20, run.getFlowCellLotNumber(), Types.VARCHAR);
            setValueOrNull(pstmt_insertRun, 21, run.getSbsSerialBarcode(), Types.VARCHAR);
            setValueOrNull(pstmt_insertRun, 22, run.getSbsLotNumber(), Types.VARCHAR);
            setValueOrNull(pstmt_insertRun, 23, run.getClusterSerialBarcode(), Types.VARCHAR);
            setValueOrNull(pstmt_insertRun, 24, run.getClusterLotNumber(), Types.VARCHAR);
            setValueOrNull(pstmt_insertRun, 25, run.getBufferSerialBarcode(), Types.VARCHAR);
            setValueOrNull(pstmt_insertRun, 26, run.getBufferLotNumber(), Types.VARCHAR);
            setValueOrNull(pstmt_insertRun, 27, run.getInvestigatorName(), Types.VARCHAR);

            pstmt_insertRun.executeUpdate();

            ResultSet rs = pstmt_insertRun.getGeneratedKeys();

            while (rs.next())
                id_run = Optional.of(rs.getInt(1));

        } catch (SQLException | NullPointerException e) {
            throw e;
        }
        return id_run;
    }

    /**
     * This method updates a run in table run by a given id_run.
     *
     * @param run
     */
    public void updateTableRunById_run(Run run) {

        try {

            setValueOrNull(pstmt_updateRun, 1, run.getRunName(), Types.VARCHAR);
            setValueOrNull(pstmt_updateRun, 2, run.getFlowcellID(), Types.VARCHAR);
            setValueOrNull(pstmt_updateRun, 3, run.getAssay(), Types.VARCHAR);
            setValueOrNull(pstmt_updateRun, 4, Date.valueOf(run.getRunStartDate()), Types.DATE);
            setValueOrNull(pstmt_updateRun, 5, run.getYield(), Types.NUMERIC);
            setValueOrNull(pstmt_updateRun, 6, run.getCluster(), Types.NUMERIC);
            setValueOrNull(pstmt_updateRun, 7, run.getClusterPF(), Types.NUMERIC);
            setValueOrNull(pstmt_updateRun, 8, run.getPercentPF(), Types.NUMERIC);
            setValueOrNull(pstmt_updateRun, 9, run.getPercentReadsIdentifiedPF(), Types.NUMERIC);
            setValueOrNull(pstmt_updateRun, 10, run.getClusterDensity(), Types.NUMERIC);
            setValueOrNull(pstmt_updateRun, 11, run.getPercentAligned(), Types.NUMERIC);
            setValueOrNull(pstmt_updateRun, 12, run.getExperimentName(), Types.VARCHAR);
            setValueOrNull(pstmt_updateRun, 13, run.getForwardReadLength(), Types.NUMERIC);
            setValueOrNull(pstmt_updateRun, 14, run.getReverseReadLength(), Types.NUMERIC);
            setValueOrNull(pstmt_updateRun, 15, run.getAverageQuality(), Types.NUMERIC);
            setValueOrNull(pstmt_updateRun, 16, run.getQualOver30(), Types.NUMERIC);
            setValueOrNull(pstmt_updateRun, 17, runstatus.get(run.getRunState()), Types.NUMERIC);
            setValueOrNull(pstmt_updateRun, 18, instrumentID.get(run.getInstrument()), Types.NUMERIC);
            setValueOrNull(pstmt_updateRun, 19, run.getFlowCellSerialBarCode(), Types.VARCHAR);
            setValueOrNull(pstmt_updateRun, 20, run.getFlowCellLotNumber(), Types.VARCHAR);
            setValueOrNull(pstmt_updateRun, 21, run.getSbsSerialBarcode(), Types.VARCHAR);
            setValueOrNull(pstmt_updateRun, 22, run.getSbsLotNumber(), Types.VARCHAR);
            setValueOrNull(pstmt_updateRun, 23, run.getClusterSerialBarcode(), Types.VARCHAR);
            setValueOrNull(pstmt_updateRun, 24, run.getClusterLotNumber(), Types.VARCHAR);
            setValueOrNull(pstmt_updateRun, 25, run.getBufferSerialBarcode(), Types.VARCHAR);
            setValueOrNull(pstmt_updateRun, 26, run.getBufferLotNumber(), Types.VARCHAR);
            setValueOrNull(pstmt_updateRun, 27, run.getInvestigatorName(), Types.VARCHAR);
            pstmt_updateRun.setInt(28, run.get_id_run());//this cannot be null and must throw an exception if so

            pstmt_updateRun.executeUpdate();

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

    }

    /**
     * This method updates state of a given {@link Run} .
     *
     * @param run
     */
    public void updateRunState(Run run) {

        try {

            pstmt_updateRunState.setInt(1, runstatus.get(run.getRunState()));
            pstmt_updateRunState.setInt(2, run.get_id_run());
            pstmt_updateRunState.executeUpdate();

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * fills HashMap runID: connect illuminarunname to corresponding id_run
     */
    private void initRunID() {

        runID = new HashMap<>();

        try {
            ResultSet rs = pstmt_selectAllRun.executeQuery();

            while (rs.next()) {
                int id_run = rs.getInt("id_run");
                String illuminarunname = rs.getString("illuminarunname");

                runID.put(illuminarunname, id_run);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    //endregion


    //region table runstatus - Implemented by another developer
    public HashMap<RunState, Integer> getRunStates() {
        return runstatus;
    }

    /**
     * Selects the id_rus by a given run state {@link String} of table runstatus.
     *
     * @param state
     * @return id_rus
     * @throws IllegalArgumentException in case it is not a valid state
     */
    private int get_id_rus(String state) {
        try {

            pstmt_getIdRus.setString(1, state);
            ResultSet rs = pstmt_getIdRus.executeQuery();
            if (rs.next()) {
                return rs.getInt("id_rus");
            }

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        throw new IllegalArgumentException("Runstatus " + state + " not found.");
    }
    //endregion


    //region table sample - Implemented by another developer
    /**
     *
     * @param id_sas
     * @return
     */
    public SampleState getSampleState(int id_sas) {
        for (SampleState sampleState : samplestatus.keySet()) {
            if (samplestatus.get(sampleState) == id_sas)
                return sampleState;
        }
        return null;
    }

    /**
     * Inserts an entry in table sample by inserting the following fields:
     * id_pat, id_run, id_enrvers, id_sas, patnr, nonsyntosyn, homtohet, titotv, yield_mbases_sum, percent_lane_averaged
     *
     * @param sample
     */
    public void insertIntoTableSample(Sample sample) {

        try {

            pstmt_insertSample.setInt(1, sample.getId_pat());
            pstmt_insertSample.setInt(2, sample.getId_run());
            pstmt_insertSample.setInt(3, sample.getId_enrvers());
            pstmt_insertSample.setInt(4, sample.getId_sas());
            pstmt_insertSample.setString(5, sample.getPatnr());
            setValueOrNull(pstmt_insertSample, 6, sample.getNonsyntosyn(), Types.NUMERIC);
            setValueOrNull(pstmt_insertSample, 7, sample.getHomtohet(), Types.NUMERIC);
            setValueOrNull(pstmt_insertSample, 8, sample.getTitotv(), Types.NUMERIC);
            setValueOrNull(pstmt_insertSample, 9, sample.getYield_Mbases(), Types.NUMERIC);
            setValueOrNull(pstmt_insertSample, 10, sample.getPercent_lane_averaged(), Types.NUMERIC);

            pstmt_insertSample.executeUpdate();

            ResultSet rs = pstmt_insertSample.getGeneratedKeys();

            while (rs.next())
                sample.setId_sam(rs.getInt(1));

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

    }

    /**
     * Selects the id_sas of a sample (table sample) by using id_sam.
     *
     * @param id_sam of a given sample
     * @return samples id_sas
     * @throws NullPointerException if sample does not exists.
     */
    public int getSamples_id_sas(int id_sam) {

        try {

            pstmt_selectSamplesIdSas.setInt(1, id_sam);
            ResultSet rs = pstmt_selectSamplesIdSas.executeQuery();
            if (rs.next())
                return rs.getInt("id_sas");

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        throw new NullPointerException("id_sam " + id_sam + " not found.");
    }

    /**
     * This method returns all samples that match given parameters in database.
     *
     * @param patnr
     * @param id_enrvers
     * @param id_run
     * @return List of samples that match given parameters in database
     */
    public List<Sample> selectSamplesBy_patnr_id_enrvers_id_run(String patnr, int id_enrvers, Integer id_run) {

        List<Sample> samples = new ArrayList<>();

        try {

            pstmt_selectSamplesByPatnrIDenrversIDrun.setString(1, patnr);
            pstmt_selectSamplesByPatnrIDenrversIDrun.setInt(2, id_enrvers);
            setValueOrNull(pstmt_selectSamplesByPatnrIDenrversIDrun, 3, id_run, Types.NUMERIC);

            ResultSet rs = pstmt_selectSamplesByPatnrIDenrversIDrun.executeQuery();

            while (rs.next()) {
                samples.add(fillSampleObject(rs));
            }

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return samples;
    }


    /**
     * This method returns sample ID that matches given parameters in database.
     *
     * @param patnr
     * @param id_enrvers
     * @param id_run
     * @return optional of id_sam that match given parameters in database
     */
    public Optional<Integer> select_id_sam_by_patnr_id_enrvers_id_run(String patnr, int id_enrvers, Integer id_run) {
        try {
            pstmt_selectIdSamByPatnrIDenrversIDrun.setString(1, patnr + '%');
            pstmt_selectIdSamByPatnrIDenrversIDrun.setInt(2, id_enrvers);
            pstmt_selectIdSamByPatnrIDenrversIDrun.setInt(3, id_run);

            ResultSet rs = pstmt_selectIdSamByPatnrIDenrversIDrun.executeQuery();

            if (rs.next()) {
                return Optional.of(rs.getInt("id_sam"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Selects a sample by primary key id_sam.
     *
     * @param id_sam
     * @return null in case {@link Sample} could not be found.
     */
    public Optional<Sample> selectSample(Integer id_sam) {

        Optional<Sample> sample = Optional.empty();
        if (Objects.isNull(id_sam))
            return sample;

        try {

            pstmt_selectSampleByIDsam.setInt(1, id_sam);

            ResultSet rs = pstmt_selectSampleByIDsam.executeQuery();

            if (rs.next()) {
                sample = Optional.of(fillSampleObject(rs));
            }

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return sample;
    }

    /**
     * This method returns a list of {@link Sample} planned in a given {@link Run}.
     *
     * @param id_run
     * @return
     */
    public List<Sample> selectSamples(int id_run) {

        List<Sample> databaseSamples = new ArrayList<>();
        try {

            pstmt_selectSampleByIDrun.setInt(1, id_run);

            ResultSet rs = pstmt_selectSampleByIDrun.executeQuery();

            while (rs.next()) {
                Sample sample = fillSampleObject(rs);
                databaseSamples.add(sample);
            }

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return databaseSamples;
    }

    /**
     * @param rs
     * @return
     * @throws SQLException
     */
    private Sample fillSampleObject(ResultSet rs) throws SQLException {

        Sample sample = new Sample();

        int id_sam = rs.getInt("id_sam");
        if (!rs.wasNull())
            sample.setId_sam(id_sam);

        int id_pat = rs.getInt("id_pat");
        if (!rs.wasNull())
            sample.setId_pat(id_pat);

        int id_run = rs.getInt("id_run");
        if (!rs.wasNull())
            sample.setId_run(id_run);

        int id_enrvers = rs.getInt("id_enrvers");
        if (!rs.wasNull())
            sample.setId_enrvers(id_enrvers);

        String patnr = rs.getString("patnr");
        if (!rs.wasNull())
            sample.setPatnr(patnr);

        double nonsyntosyn = rs.getDouble("nonsyntosyn");
        if (!rs.wasNull())
            sample.setNonsyntosyn(nonsyntosyn);

        double homtohet = rs.getDouble("homtohet");
        if (!rs.wasNull())
            sample.setHomtohet(homtohet);

        double titotv = rs.getDouble("titotv");
        if (!rs.wasNull())
            sample.setTitotv(titotv);

        double coveragemean = rs.getDouble("coveragemean");
        if (!rs.wasNull())
            sample.setCoveragemean(coveragemean);

        double coverageoverthreshold = rs.getDouble("coverageoverthreshold");
        if (!rs.wasNull())
            sample.setCoverageoverthreshold(coverageoverthreshold);

        double contamination = rs.getDouble("contamination");
        if (!rs.wasNull())
            sample.setContamination(contamination);

        double error = rs.getDouble("error");
        if (!rs.wasNull())
            sample.setError(error);

        int total_qcpassed_reads = rs.getInt("total_qcpassed_reads");
        if (!rs.wasNull())
            sample.setTotal_qc_passed_reads(total_qcpassed_reads);

        int mapped_qcpassed_reads = rs.getInt("mapped_qcpassed_reads");
        if (!rs.wasNull())
            sample.setMapped_qc_passed_reads(mapped_qcpassed_reads);

        int properlypaired_qcpassed_reads = rs.getInt("properlypaired_qcpassed_reads");
        if (!rs.wasNull())
            sample.setProperlyPaired_qc_passed_reads(properlypaired_qcpassed_reads);

        int singletons_qcpassed_reads = rs.getInt("singletons_qcpassed_reads");
        if (!rs.wasNull())
            sample.setSingletons_qc_passed_reads(singletons_qcpassed_reads);

        int yield = rs.getInt("yield");
        if (!rs.wasNull())
            sample.setYield_Mbases(yield);

        double percent_lane = rs.getDouble("percent_lane");
        if (!rs.wasNull())
            sample.setPercent_lane_averaged(percent_lane);

        int id_sas = rs.getInt("id_sas");
        SampleState samplestate = samplestatus.entrySet()
                .stream()
                .filter(e -> e.getValue() == id_sas)
                .findFirst()
                .get()
                .getKey();
        sample.setSamplestate(samplestate, id_sas);
        return sample;
    }

    /**
     * Data of object Sample corresponding with table sample will be updated by using id_sam.
     *
     * @param sample
     */
    public void updateSample(Sample sample) {

        try {

            pstmt_updateSample.setInt(1, sample.getId_pat());
            pstmt_updateSample.setInt(2, sample.getId_run());
            pstmt_updateSample.setInt(3, sample.getId_enrvers());
            pstmt_updateSample.setInt(4, sample.getId_sas());
            setValueOrNull(pstmt_updateSample, 5, sample.getPatnr(), Types.VARCHAR);
            setValueOrNull(pstmt_updateSample, 6, sample.getNonsyntosyn(), Types.NUMERIC);
            setValueOrNull(pstmt_updateSample, 7, sample.getHomtohet(), Types.NUMERIC);
            setValueOrNull(pstmt_updateSample, 8, sample.getTitotv(), Types.NUMERIC);
            setValueOrNull(pstmt_updateSample, 9, sample.getCoveragemean(), Types.NUMERIC);
            setValueOrNull(pstmt_updateSample, 10, sample.getCoverageoverthreshold(), Types.NUMERIC);
            setValueOrNull(pstmt_updateSample, 11, sample.getContamination(), Types.NUMERIC);
            setValueOrNull(pstmt_updateSample, 12, sample.getError(), Types.NUMERIC);
            setValueOrNull(pstmt_updateSample, 13, sample.getTotal_qc_passed_reads(), Types.NUMERIC);
            setValueOrNull(pstmt_updateSample, 14, sample.getMapped_qc_passed_reads(), Types.NUMERIC);
            setValueOrNull(pstmt_updateSample, 15, sample.getProperlyPaired_qc_passed_reads(), Types.NUMERIC);
            setValueOrNull(pstmt_updateSample, 16, sample.getSingletons_qc_passed_reads(), Types.NUMERIC);
            setValueOrNull(pstmt_updateSample, 17, sample.getYield_Mbases(), Types.NUMERIC);
            setValueOrNull(pstmt_updateSample, 18, sample.getPercent_lane_averaged(), Types.NUMERIC);
            pstmt_updateSample.setInt(19, sample.getId_sam());

            pstmt_updateSample.executeUpdate();

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

    }

    /**
     * @param id_sas
     * @param id_sam
     */
    public void update_id_sas_by_id_sam(int id_sas, int id_sam) {
        try {
            pstmt_updateIDsas.setInt(1, id_sas);
            pstmt_updateIDsas.setInt(2, id_sam);
            pstmt_updateIDsas.executeUpdate();
        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }
    //endregion


    //region table samplestatus - Implemented by another developer
    /**
     * Selects the id_sas by a given sample state {@link String} of table samplestatus.
     *
     * @param state
     * @return id_rus
     * @throws IllegalArgumentException in case it is not a valid state
     */
    private int getId_sas(String state) {

        try {

            pstmt_selectSampleState.setString(1, state);
            ResultSet rs = pstmt_selectSampleState.executeQuery();
            if (rs.next()) {
                return rs.getInt("id_sas");
            }

        } catch (NullPointerException | SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        throw new IllegalArgumentException("Samplestatus " + state + " not valid.");
    }

    /**
     * This method returns the id_sas of a given sample state. This information will be cached.
     *
     * @param samplestate
     * @return
     */
    public Integer getId_sas(SampleState samplestate) {
        return samplestatus.get(samplestate);
    }

    public HashMap<SampleState, Integer> getSampleStates() {
        return samplestatus;
    }
    //endregion


    //region table samplelock - Implemented by another developer
    /**
     * Checks whether the lock entry already exists based on sample, user and comment.
     *
     * @param id_sam
     * @param id_usr
     * @param id_sas
     * @param locktime
     * @param comment
     */
    public void insertIntoSampleLockIfExists(int id_sam, int id_usr, int id_sas, Timestamp locktime, String comment) {
        if (sampleLockCommentAlreadyExists(id_sam, id_usr, comment)) {
            return;
        }

        try {

            pstmt_insertSampleLock.setInt(1, id_sam);
            pstmt_insertSampleLock.setInt(2, id_usr);
            pstmt_insertSampleLock.setInt(3, id_sas);
            pstmt_insertSampleLock.setTimestamp(4, locktime);
            pstmt_insertSampleLock.setString(5, comment);

            pstmt_insertSampleLock.executeUpdate();

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Checks whether the entry already exists in table samplelock based on sample, locking user and comment.
     *
     * @param id_sam
     * @param id_usr
     * @param comment
     * @return id_sal of selected comment
     */
    private boolean sampleLockCommentAlreadyExists(int id_sam, int id_usr, String comment) {
        boolean exists = false;
        try {

            pstmt_selectSampleLock.setInt(1, id_sam);
            pstmt_selectSampleLock.setInt(2, id_usr);
            pstmt_selectSampleLock.setString(3, comment);
            ResultSet rs = pstmt_selectSampleLock.executeQuery();

            if (rs.next())
                exists = true;

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return exists;
    }
    //endregion


    // region table sam_cnv
    public boolean sampleCNVsAlreadyInDB(int id_sam) {
        try {
            pstmt_selectSamCNV_by_IdSam.setInt(1, id_sam);
            ResultSet rs = pstmt_selectSamCNV_by_IdSam.executeQuery();

            if(rs.next()) {
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     *  Import CNV and all corresponding targets
     *
     * @param cnv
     */

    public void importCNV(CNV cnv) {

        int id_cnv;
        Optional<Integer> id_sat;

        try {
            pstmt_insert_sam_cnv.setInt(1, cnv.getId_sam());
            pstmt_insert_sam_cnv.setInt(2, VAR_NOT_TESTED_ID_CSV);
            pstmt_insert_sam_cnv.setString(3, cnv.getIdentifier());
            pstmt_insert_sam_cnv.setString(4, "");
            pstmt_insert_sam_cnv.setInt(5, cnv.getMin_size());

            if(Objects.isNull(cnv.getMax_size()))
                pstmt_insert_sam_cnv.setNull(6, java.sql.Types.INTEGER);
            else
                pstmt_insert_sam_cnv.setInt(6, cnv.getMax_size());

            pstmt_insert_sam_cnv.setString(7, cnv.getType());
            pstmt_insert_sam_cnv.setString(8, "");
            pstmt_insert_sam_cnv.setString(9, "");

            pstmt_insert_sam_cnv.executeUpdate();
            ResultSet rs = pstmt_insert_sam_cnv.getGeneratedKeys();

            if(rs.next()) {
                id_cnv = rs.getInt("id_cnv");

                for(Target target : cnv.getTargets()) {
                     id_sat = getIdSatIfTargetExists(target, id_cnv);

                    if(!id_sat.isPresent())
                        id_sat = importTarget(target, id_cnv);

                    importTargetCaller(target, id_sat.get());
                }
            } else {
                LOGGER.log(Level.WARNING, "Could not insert cnv of sample: " + cnv.getId_sam());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    /**
     * Import of target
     * @param target
     * @param id_cnv
     */
    private Optional<Integer> importTarget(Target target, int id_cnv) {

        try {
            pstmt_insert_sam_target.setInt(1, id_cnv);
            pstmt_insert_sam_target.setInt(2, target.getId_rrf());

            pstmt_insert_sam_target.executeUpdate();
            ResultSet rs = pstmt_insert_sam_target.getGeneratedKeys();

            if(rs.next()) {
                return Optional.of(rs.getInt("id_sat"));

            } else {
                LOGGER.log(Level.WARNING, "Could not insert target (id_rrf: " + target.getId_rrf() + ") of id_cnv: " + id_cnv);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }


    /**
     * Import sam_tar_cal
     *
     * @param target
     * @param id_sat
     */
    private void importTargetCaller(Target target, int id_sat) {
        try {
            pstmt_insert_sam_tar_cal.setInt(1, id_sat);
            pstmt_insert_sam_tar_cal.setInt(2, target.getId_cal());
            pstmt_insert_sam_tar_cal.setString(3, target.getCaller());

            pstmt_insert_sam_tar_cal.executeUpdate();

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not insert sam_tar_cal (id_sat: " + id_sat + ", caller: " + target.getCaller() + ")" );
        }
    }





    /**
     * Check if target exists in DB based on id_cnv and id_rrf
     * and return id_sat if exists
     *
     * @param target
     * @param id_cnv
     * @return
     */
    private Optional<Integer> getIdSatIfTargetExists(Target target, int id_cnv) {
        try {

            pstmt_selectSamTargetByIdCnvIdRrf.setInt(1, id_cnv);
            pstmt_selectSamTargetByIdCnvIdRrf.setInt(2, target.getId_rrf());

            ResultSet rs = pstmt_selectSamTargetByIdCnvIdRrf.executeQuery();

            if(rs.next()) {
                return Optional.of(rs.getInt("id_sat"));
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    // endregion


    // region table sam_target

    // endregion


    // region table sam_tar_cal

    // endregion


    //region table runlock - Implemented by another developer
    /**
     * Checks whether the lock entry already exists based on run, user and comment.
     *
     * @param id_run
     * @param id_usr
     * @param id_rus
     * @param locktime
     * @param comment
     */
    public void insertIntoRunLockIfExists(int id_run, int id_usr, int id_rus, Timestamp locktime, String comment) {
        if (runLockCommentAlreadyExists(id_run, id_usr, comment)) {
            return;
        }

        try {

            pstmt_insertRunLock.setInt(1, id_run);
            pstmt_insertRunLock.setInt(2, id_usr);
            pstmt_insertRunLock.setInt(3, id_rus);
            pstmt_insertRunLock.setTimestamp(4, locktime);
            pstmt_insertRunLock.setString(5, comment);

            pstmt_insertRunLock.executeUpdate();

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Checks whether the entry already exists in table runlock based on run, locking user and comment.
     *
     * @param id_run
     * @param id_usr
     * @param comment
     * @return id_sal of selected comment
     */
    private boolean runLockCommentAlreadyExists(int id_run, int id_usr, String comment) {
        boolean exists = false;
        try {

            pstmt_selectRunLock.setInt(1, id_run);
            pstmt_selectRunLock.setInt(2, id_usr);
            pstmt_selectRunLock.setString(3, comment);
            ResultSet rs = pstmt_selectRunLock.executeQuery();

            if (rs.next())
                exists = true;

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return exists;
    }
    //endregion


    // region table caller
    /**
     * @return a {@link HashMap} that links the name of all CNV caller to the corresponding id_cal
     */
    public HashMap<String, Integer> getCNVCaller() {
        HashMap<String, Integer> cnvCaller = new HashMap<>();

        try {
            ResultSet rs = pstmt_selectAllCNVCaller.executeQuery();

            while (rs.next()) {
                Integer id_cal = rs.getInt("id_cal");
                String caller = rs.getString("caller");

                cnvCaller.put(caller, id_cal);
            }

        } catch (SQLException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return cnvCaller;
    }
    // endregion

    // Implemented by another developer
    /**
     * This method compares the runstate of currently parsed run. In terms of
     * equality, there is no way to differentiate runstate 7, 16 and 17.
     * Therefore those three cases will treated equally. Update runs runstate
     * with state stored in database.
     * <p>
     * id_rus is in database stored runstate and object run contains runstate
     * of currently parsed run.
     *
     * @return true, if status is the same
     */
    private boolean compareRunStatus(Run run, int id_rus) {

        /*
         * if currently parsed run is done analysing
         * (RunState.ANALYSIS_COMPLETED) run, there could be a higher state
         * stored in database, so that it should not be changed
         */

        if (runstatus.get(run.getRunState()).equals(runstatus.get(RunState.ANALYSIS_COMPLETED)) &&
                id_rus == runstatus.get(RunState.IMPORT_STARTET)) {
            run.setRunState(RunState.IMPORT_STARTET);
            return true;

        } else if (runstatus.get(run.getRunState()).equals(runstatus.get(RunState.ANALYSIS_COMPLETED)) &&
                id_rus == runstatus.get(RunState.IMPORT_COMPLETED)) {
            run.setRunState(RunState.IMPORT_COMPLETED);
            return true;

        } else if (runstatus.get(run.getRunState()).equals(runstatus.get(RunState.IMPORT_STARTET)) &&
                id_rus == runstatus.get(RunState.IMPORT_COMPLETED)) {
            run.setRunState(RunState.IMPORT_COMPLETED);
            return true;

        } else if (runstatus.get(run.getRunState()).equals(runstatus.get(RunState.IMPORT_STARTET)) &&
                id_rus == runstatus.get(RunState.IMPORT_COVERAGE_COMPLETED)) {

            run.setRunState(RunState.IMPORT_COVERAGE_COMPLETED);
            return true;

        } else if (runstatus.get(run.getRunState()).equals(runstatus.get(RunState.ANALYSIS_COMPLETED)) &&
                id_rus == runstatus.get(RunState.IMPORT_COVERAGE_COMPLETED)) {
            run.setRunState(RunState.IMPORT_COVERAGE_COMPLETED);
            return true;

        }

        //regardless of the run.getRunState(), the table ngs.run is never changed when the id_rus in the database is "import completed" or "archived".
        if (id_rus == 17) {
            run.setRunState(RunState.IMPORT_COMPLETED);
            return true;
        }
        if (id_rus == 8) {
            run.setRunState(RunState.ARCHIVED);
            return true;
        }

        /*
         * checks non special cases, if there are no special cases.
         */
        return runstatus.get(run.getRunState()) == id_rus;
    }

    // Implemented by another developer
    /**
     * This method selects size in bytes of a given table.
     *
     * @param tableName
     * @return
     * @throws IllegalArgumentException
     */
    public Long getTableSize(String tableName) throws IllegalArgumentException {

        try {

            pstmt_select_table_size.setString(1, tableName);
            ResultSet rs = pstmt_select_table_size.executeQuery();
            if (rs.next())
                return rs.getLong("total_bytes");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.SEVERE, "Table not found: " + tableName, e);
        }
        return 0L;
    }

    //Implemented by another developer
    private String getSqlConnectionString() throws IOException {

        final Path CONFIGFILEPATH = Paths.get("conf/midas-Import-Handler-GATK.cfg");
        File file = new File(CONFIGFILEPATH.toString());
        Properties prop = new Properties();
        InputStream inputStream = new FileInputStream(file.getAbsolutePath());
        prop.load(inputStream);
        return prop.getProperty("sqlConnectionString");
    }


    public String getIdentifier(String chromosome) {
        return map_identifier_symbol_identifiers.get(chromosome);
    }
}