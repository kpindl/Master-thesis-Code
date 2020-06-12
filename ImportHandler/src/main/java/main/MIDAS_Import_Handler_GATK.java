package main;

import data.Coverage;
import data.cnv.CNV;
import data.cnv.Target;
import data.run.*;
import data.run.runfiles.Contamination;
import data.run.runfiles.FlagStat;
import data.sample.*;
import database.*;
import exception.InvalidRunException;
import fileparser.FileReader;
import org.apache.commons.lang3.SystemUtils;
import processing.LogFormatter;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;


public class MIDAS_Import_Handler_GATK {

	private static final Path CONFIGFILEPATH = Paths.get("conf/midas-Import-Handler-GATK.cfg");
	private static final Logger LOGGER = Logger.getLogger(MIDAS_Import_Handler_GATK.class.getName());

	private HashMap<String, ArrayList<String>> mappedSequencerIDs;
	private HashMap<String, Integer> enrichmentversions;
	private HashMap<SampleState, Integer> samplestates;
	private HashMap<String, Integer> cnvCaller;
	private HashMap<Integer, String> cnvType;
	private List<Path> pathToStartDirectoryArray;
	private Path pathToStartCNV;
    private String PathAvailableDiskSpace;
	private int threadsCoverage;
	private int minAvailableSpace;

	public MIDAS_Import_Handler_GATK() {

		this.mappedSequencerIDs = new HashMap<>();
		this.enrichmentversions = new HashMap<>();
		this.samplestates = new HashMap<>();
		this.cnvCaller = new HashMap<>();
		this.cnvType = new HashMap<>();
		this.pathToStartDirectoryArray = new ArrayList<>();
		this.PathAvailableDiskSpace = "";
		this.threadsCoverage = 0;
		this.minAvailableSpace = 0;

		setShownLogLevel(Level.FINE);

		LogFormatter.applyFormatter(LOGGER);
	}

	private void setShownLogLevel(Level level) {
		Logger root = Logger.getLogger("");
		root.setLevel(level);
		for (Handler handler : root.getHandlers()) {
			handler.setLevel(level);
		}
	}

	public static void main(String[] args) {

		MIDAS_Import_Handler_GATK mih = new MIDAS_Import_Handler_GATK();
		mih.readConfigFile();
		mih.start();

	}

	private void readConfigFile() {

		File file = new File(CONFIGFILEPATH.toString());

		try (InputStream inputStream = new FileInputStream(file.getAbsolutePath())) {

			Properties prop = new Properties();

			prop.load(inputStream);

			threadsCoverage = Integer.parseInt(prop.getProperty("ThreadsForCoverageReader"));

			minAvailableSpace = Integer.parseInt(prop.getProperty("minAvailableSpace"));

			PathAvailableDiskSpace = prop.getProperty("PathAvailableDiskSpace");

			pathToStartCNV = Paths.get(prop.getProperty("PathToStartCNV"));

			for (String str : prop.getProperty("PathToStartDirectory").split(",")) {
				pathToStartDirectoryArray.add(Paths.get(str));
			}

		} catch (NullPointerException e) {
			LOGGER.log(Level.SEVERE, e.getMessage() + " " + file.getAbsolutePath(), e);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

	}

	private void start() {

		LOGGER.log(Level.INFO, "Start import handler");
		
		MIDAS_DB database = new MIDAS_DB();

		while (true) {
			updateCacheDataForNewIteration(database);

			if (!enoughSpaceOnServer(this.minAvailableSpace, PathAvailableDiskSpace))
				break;
//
			for (Path runDirectory : pathToStartDirectoryArray) {
				List<Run> runs = fillRunModel(runDirectory);

				for (Run run : runs) {
					processRun(database, run);
				}
			}

            processCNVs(database);

			sleepForMilliseconds(60000000);
		}
//
		LOGGER.log(Level.SEVERE, "Less than " + this.minAvailableSpace + " GB of storage space available.");
	}

	private void updateCacheDataForNewIteration(MIDAS_DB database) {
		mappedSequencerIDs = database.getMappedSequencerIDs();
		enrichmentversions = database.getAllEnrichmentversions();
		samplestates = database.getSampleStates();
		cnvCaller = database.getCNVCaller();
		cnvType.put(0, "Deletion");
		cnvType.put(1000, "Duplication");
	}

	/**
	 * This method determines whether there is a given amount of disc space left in a given directory on Linux backend.
	 *
	 * @param gigabyteLeft
	 * @param directory
	 * @return
	 */
	public boolean enoughSpaceOnServer(int gigabyteLeft, String directory) {

		if (!SystemUtils.IS_OS_LINUX)
			return true;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("df -h").getInputStream()))) {

			//line: /dev/mapper/vg0-var               150G     71G   80G   48% /var
			Pattern pattern = Pattern.compile(".+\\s+([0-9]+)G\\s+([0-9]+)G\\s+([0-9]+)G.*");
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains(directory)) {
					Matcher matcher = pattern.matcher(line);
					if (matcher.find()) {
						int availableSpace = Integer.parseInt(matcher.group(3));
						if (availableSpace > gigabyteLeft)
							return true;
					}
					break;
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

		return false;
	}

	private void sleepForMilliseconds(int sleeptime) {
		LOGGER.log(Level.INFO, "Done and sleep for " + sleeptime + "ms.");
		try {
			Thread.sleep(sleeptime);
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}


	/**
	 * This method searches for all runs within a given directory.
	 *
	 * @param pathToStartDirectory path where the data is located
	 * @return List containing all runs located in the pathToStartDirectory
	 */
	private List<Run> fillRunModel(Path pathToStartDirectory) {

		List<Run> runs = new ArrayList<>();

		/*
		 *  walk through whole directory
		 */
		try (DirectoryStream<Path> runStream = Files.newDirectoryStream(pathToStartDirectory)) {

			for (Path runPath : runStream) {

				Run run;
				try {
					run = Run.getRun(runPath, mappedSequencerIDs);
				} catch (InvalidRunException e) {
					continue;
				}

				run.parseRunInfoIfExists(runPath);
				run.parseRunParametersIfExists(runPath);
				run.parseSampleSheetIfExists(runPath, enrichmentversions.keySet());
				run.parseDepartments();
				run.setPrintTimeParsingInterOpFiles(true);
				run.parseInterOpFilesIfExists(runPath);

				try {
					Run.updateRunState(run);
				} catch (InvalidRunException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}

				runs.add(run);

			}

		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

		return runs;
	}


	private void processRun(MIDAS_DB database, Run run) {
		Optional<Integer> id_run = getIdRun(database.select_Run(run.getExperimentName()), run);
		if (id_run.isPresent()) {
			run.setId_run(id_run.get());
			updateRunModel(database, run);
		} else {
			try {
				id_run = database.insertIntoTableRun(run);
				run.set_id_run(id_run.get());
				LOGGER.log(Level.FINE, "Inserted run: " + run.getExperimentName());
			} catch (IOException | SQLException e) {
				LOGGER.log(Level.SEVERE, "Insert run failed: " + run.getExperimentName());
				return;
			}
		}
		// Ab hier ist id_run im Run Model gesetzt!

		insertDepartmentsIfNotExists(database, run);
		fillSampleModel(database, run);
		analysis(run);
		importData(database, run);
		updateRunModel(database, run);
		updateSampleModel(database, run.getSamples());
	}


    private void processCNVs(MIDAS_DB database) {

        Pattern cnvFilePattern = Pattern.compile("(\\d{8}-.*-\\d{6}_.*_\\d{4}_.*)_(.*)\\.track\\.bed");

        for (Path enrversDir : getStream(pathToStartCNV)) {
            LOGGER.log(Level.FINE, "Start enrichmentversion: " + enrversDir.getFileName().toString());

            HashMap<Integer, Sample> samples = new HashMap<>();

            processRawData(database, cnvFilePattern, enrversDir, samples);

            int id_enrvers = getId_enrvers(enrversDir);
            HashMap<Integer, Integer> orderReferenceTargets = database.fillOrderReferenceTargets(id_enrvers);

            // Group CNVs per sample and import into DB
            LOGGER.log(Level.INFO, "Start grouping CNVs and import for " + enrversDir.getFileName().toString());
            for (Sample sample : samples.values()) {

                List<CNV> cnvs = groupSampleCNVs(sample, orderReferenceTargets, database);

				for (CNV cnv : cnvs) {
					database.importCNV(cnv);
				}
            }
        }
    }

    private int getId_enrvers(Path enrversDir) {
        String enrichmentversion = enrversDir.getFileName().toString().split("_saved")[0];
        return enrichmentversions.get(enrichmentversion);
    }

    /**
     * Process raw data of current enrichmentversion
     *
     * @param database
     * @param cnvFilePattern
     * @param enrversDir
     * @param samples
     */
    private void processRawData(MIDAS_DB database, Pattern cnvFilePattern, Path enrversDir, HashMap<Integer, Sample> samples) {
        List<String> processedSamples = parseProcessedSampleFiles(enrversDir);
        for (Path cnvResultsDir : getStream(Paths.get(enrversDir.toString() + "/midas"))) {
            String filename = cnvResultsDir.getFileName().toString();
            Matcher cnvFileMatcher = cnvFilePattern.matcher(filename);

            if(!cnvFileMatcher.matches()) {
                LOGGER.log(Level.WARNING, filename + " is not a valid CNV results file");
                continue;
            }

            String fileNameWithoutCaller = cnvFileMatcher.group(1);
            String caller = cnvFileMatcher.group(2);

            if (!processedSamples.contains(fileNameWithoutCaller)) {
                LOGGER.log(Level.INFO, fileNameWithoutCaller + " not contained in processed samples files.");
                continue;
            }

            Optional<Sample> currentSample = fillSampleModelCNV(fileNameWithoutCaller, samples, database);
            if (!currentSample.isPresent()) continue;

            if(!samples.containsKey(currentSample.get().getId_sam())) {
                samples.put(currentSample.get().getId_sam(), currentSample.get());
            }

            fillTargetModel(database, cnvResultsDir, caller, currentSample.get());
        }
    }


    /**
	 * This method searches in a given {@link List} for the equivalent {@link Run}.
	 * @param databaseRuns
	 * @param run
	 * @return false in case {@link Run} is not in database yet.
	 */
	public Optional<Integer> getIdRun(List<Run> databaseRuns, Run run) {

		/*
		in case experiment name is stored in database
		 */
		for (Run databaseRun : databaseRuns) {
			if (Objects.equals(run.getRunName(), databaseRun.getRunName())) {
				return Optional.of(databaseRun.get_id_run());
			}
		}

		/*
		in case experiment name is not stored in database
		 */
		for (Run databaseRun : databaseRuns) {
			if (Objects.isNull(databaseRun.getRunName()))
				return Optional.of(databaseRun.get_id_run());
		}

		return Optional.empty();
	}


	//region update run model
	/**
	 * This method compares {@link Run} data stored on hard dist with {@link Run} data in database and updates them,
	 * if necessary.
	 *
	 * @param database
	 * @param run
	 */
	public void updateRunModel(MIDAS_DB database, Run run) {

		Run runInDatabase = database.select_Run(run.get_id_run());
		if (!runInDatabase.equals(run)) {
			run.adjustRunState(runInDatabase.getRunState());
			database.updateTableRunById_run(run);
			LOGGER.log(Level.FINE, "Updated run: " + run.getExperimentName());
		}

	}

	private void insertDepartmentsIfNotExists(MIDAS_DB database, Run run) {
		List<String> departments = run.getDepartments().stream().collect(toList());
		List<Integer> id_deps = database.getId_depList(departments);
		database.insertIntoTable_run_dep_IfNotExists(run.get_id_run(), id_deps);
	}
	//endregion


	//region fill sample model
	/**
	 * This method fills all data related to a sample without taking into account model run
	 * @param database
	 * @param run
	 */
	private void fillSampleModel(MIDAS_DB database, Run run) {

		HashMap<String, Integer> enrichmentversions = database.getAllEnrichmentversions();

	    List<Sample> samples = run.getSamples();

		List<Sample> databaseSamples = database.selectSamples(run.get_id_run());

		for (Sample sample : samples) {

			sample.setId_run(run.get_id_run());

			int id_enrvers = enrichmentversions.get(sample.getEnrichmentVersion());
			sample.setId_enrvers(id_enrvers);

			Optional<Sample> correspondingDatabaseSample = databaseSamples
					.stream()
					.filter(s -> s.getPatnr().equals(sample.getPatnr()))
					.filter(s -> s.getId_enrvers() == id_enrvers)
					.findFirst();

			if (correspondingDatabaseSample.isPresent()) {
				sample.updateDatabaseFieldsOnly(correspondingDatabaseSample.get());
			}

			if (sample.getSamplestate() == SampleState.PLANNED) {
				sample.setSamplestate(SampleState.SEQUENCING_ANALYSIS_STARTED,
						database.getId_sas(SampleState.SEQUENCING_ANALYSIS_STARTED));
			}

			sample.setEnrichedRegions(database.getEnrichedRegions(sample.getId_enrvers()));
		}

		sampleProcessedButNotPlanned(samples);

		samples.addAll(samplePlannedButNotProcessed(databaseSamples, samples));

	}


	/**
	 * This method identifies all {@link Sample} that were processed (sample exists in SampleSheet) but were not
	 * planned (sample exists not in database). Unplanned {@link Sample} do not have an id_sam at this point.
	 */
	public void sampleProcessedButNotPlanned(List<Sample> samples) {

		for (Sample sample : samples) {
			if (Objects.isNull(sample.getId_sam())) {
				sample.setPlanned(false);
				sample.setSamplestate(SampleState.LOCKED_SEQUENCING_ANALYSIS_STARTED,
						samplestates.get(SampleState.LOCKED_SEQUENCING_ANALYSIS_STARTED));
			}
		}
	}

	/**
	 * This method identifies and deals with all {@link Sample} that were not processed (sample exists not in
	 * SampleSheet) but were planned (sample exists in database).
	 */
	public List<Sample> samplePlannedButNotProcessed(List<Sample> databaseSamples, List<Sample> samples) {

		List<Sample> notProcessedSamples = databaseSamples
				.stream()
				.filter(sample -> !samples.contains(sample))
				.collect(toList());

		for (Sample sample : notProcessedSamples) {
			sample.setProcessed(false);
			sample.setSamplestate(SampleState.LOCKED_SEQUENCING_ANALYSIS_STARTED,
					samplestates.get(SampleState.LOCKED_SEQUENCING_ANALYSIS_STARTED));
		}

		return notProcessedSamples;
	}
	//endregion


	//region analysis
	/**
	 * @param run
	 */
	private void analysis(Run run) {
	}
	//endregion


	//region import
	/**
	 * Here all data files will be imported into the model (contamination and flagstat files) or database (coverage and
	 * vcf files), respectively.
	 *
	 * @param database
	 * @param run
	 */
	private void importData(MIDAS_DB database, Run run) {

		if (run.isLocked())
			return;

//		if (!run.isAnalysisCompleted()) {
//			return;
//		}

		run.setRunState(RunState.IMPORT_STARTET);
		setStateForAllSamples(run, SampleState.SEQUENCING_ANALYSIS_STARTED);

		Path gatkDir = run.getRunDirectory().resolve("_gatk");
		importCoverageFiles(database, run.getSamples(), run.getRunName(), gatkDir);
		run.setRunState(RunState.IMPORT_COVERAGE_COMPLETED);
		// sample.setEnrichedRegions(database.getEnrichedRegions(sample.getId_enrvers()));

		importContaminationFiles(run.getSamples(), run.getRunName(), gatkDir);
		importFlagStatFiles(run.getSamples(), run.getRunName(), gatkDir);
		run.setRunState(RunState.IMPORT_COMPLETED);
		setStateForAllSamples(run, SampleState.ANALYSIS_PENDING);

	}

	private void setStateForAllSamples(Run run, SampleState state) {
		for (Sample sample : run.getSamples()) {
			sample.setSamplestate(state,
					samplestates.get(state));
		}
	}

	/**
	 * Gets all Files with given {@link Path} {@param gatkDir} that ends with {@link String} {@param fileEnding}.
	 * @param gatkDir
	 * @param fileEnding
	 * @param queue to specify the return type a subclass of {@link AbstractCollection} must be given.
	 * @return any subclass of {@link AbstractCollection} will be returned, depending on {@param queue}.
	 */
	private AbstractCollection<Path> getFiles(Path gatkDir, String fileEnding, AbstractCollection<Path> queue) {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(gatkDir)) {
			for (Path path : stream) {
				if (path.toString().endsWith(fileEnding)) {
					queue.add(path);
				}
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return queue;
	}


	//region coverage files
	/**
	 * This method collects all files ending with "_coverage" within given Path {@param gatkDir}, checks validity of
	 * those files, links those files to corresponding samples, starts reading all files with corresponding samples
	 * and inserts those data into the database.
	 * @param database
	 * @param samples
	 * @param gatkDir
	 */
	private void importCoverageFiles(MIDAS_DB database, List<Sample> samples, String runName, Path gatkDir) {

		if (Objects.isNull(gatkDir) || samples.isEmpty())
			return;

		String filename = "_coverage.txt";
		List<Path> coverageFiles = (List<Path>) getFiles(gatkDir, filename, new ArrayList<>());

		coverageFiles = filtersNonValidCoverageFiles(coverageFiles);

		List<Sample> sampleQueue = setCoverageFilesToSamples(samples, coverageFiles, runName, filename);

		LOGGER.log(Level.FINE, "Start reading coverage file.");
		long sizeBefore = database.getTableSize("covered");
		readingCoverageFiles(database, sampleQueue);
		long sizeAfter = database.getTableSize("covered");
		long size = sizeAfter - sizeBefore;
		LOGGER.log(Level.FINE, "Done reading coverage file.");
		LOGGER.log(Level.FINE, "Size differences of table covered: " + size + " bytes.");
	}

	/**
	 * This method filters all files that are not valid coverage files.
	 * @param coverageFiles
	 * @return
	 */
	private List<Path> filtersNonValidCoverageFiles(List<Path> coverageFiles) {
		FileReader reader = new FileReader();
		coverageFiles
				.stream()
				.filter(p -> reader.determineVersion(p) instanceof Coverage)
				.collect(toList());
		return coverageFiles;
	}

	/**
	 * Here for all {@link Sample} the corresponding coverage file will be searched and stored in its
	 * {@link Sample} object.
	 * @param samples
	 * @param files
	 * @param runname as the illumina name of a run
	 * @param fileending
	 */
	private List<Sample> setCoverageFilesToSamples(
			List<Sample> samples, List<Path> files, String runname, String fileending) {
		List<Sample> filteredList = new ArrayList<>();
		for (Sample sample : samples) {
			if (sample.isLocked())
				continue;
			String filename = createFilenameOf(sample.getPatnr(), sample.getEnrichmentVersion(), runname, fileending);
			for (Path path : files) {
				if (path.endsWith(filename)) {
					sample.setCoverageFile(path);
					filteredList.add(sample);
					break;
				}
			}
		}
		return filteredList;
	}

	private String createFilenameOf(String patnr, String enrichment, String runname, String fileending) {
		if (enrichment.equals("AgilentWE.v6"))
			enrichment = "WholeExome";
		return patnr + "-" + enrichment.replace(".", "_") + "-" + runname + fileending;
	}

	/**
	 * Reads coverage files; each file in another thread.
	 * @param samples
	 */
	private void readingCoverageFiles(MIDAS_DB database, List<Sample> samples) {

		/*
		 initialise and start threadpool
		 */
	    List<String> processAgain = Arrays.asList("", "", "", "");
		ConcurrentLinkedQueue<Sample> queue = samples
				.stream()
                .filter(ele -> processAgain.contains(ele.getPatnr()))
				.collect(Collectors.toCollection(ConcurrentLinkedQueue::new));
		ReadCoverageFiles[] workers = new ReadCoverageFiles[threadsCoverage];
		Thread[] threads = new Thread[threadsCoverage];
		for (int i = 0; i < threadsCoverage; i++) {
			workers[i] = new ReadCoverageFiles(queue, database);
			threads[i] = new Thread(workers[i]);
			threads[i].start();
		}

		/*
		 shuts threadpool down
		 */
		for (int i=0; i < threadsCoverage; i++) {
			Thread thread = threads[i];
			try {
				thread.join();
			} catch (InterruptedException e) {
				thread.interrupt();
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	private class ReadCoverageFiles implements Runnable {

		private ConcurrentLinkedQueue<Sample> queue;
		private Sample sample;
		private Coverage coverage;
		private MIDAS_DB database;

		/**
		 * This class processes a queue {@link ConcurrentLinkedQueue<Sample>} of {@link Sample} to reads the
		 * coresponding coverage file.
		 * @param queue
		 */
		private ReadCoverageFiles(ConcurrentLinkedQueue<Sample> queue, MIDAS_DB database) {
			this.queue = queue;
			this.sample = queue.poll();
			this.coverage = new Coverage();
			this.database = database;
		}

		public void run() {

			while (!Objects.isNull(sample)) {
				parseCoverageForEachSample();
				sample = queue.poll();
			}
		}

		private void parseCoverageForEachSample() {

			Path coverageFilePath = sample.getCoverageFile();

			if (Objects.isNull(coverageFilePath))
				return;

			try (BufferedReader reader = Files.newBufferedReader(sample.getCoverageFile(), Charset.forName("UTF-8"))) {

				LOGGER.log(Level.FINE, "Start reading file   " + coverageFilePath.toString());
				coverage.setEnrichedRegion(sample.getEnrichedRegions());
				coverage.parse(reader);
				LOGGER.log(Level.FINE, "Done reading file    " + coverageFilePath.toString());

				// -> its not easy to transfer parsed information out of threadpool
				insertIntoDatabase();
				LOGGER.log(Level.FINE, "Done database import " + coverageFilePath.toString());

			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		private void insertIntoDatabase() {
			for (Entry<String, ArrayList<Integer[]>> regions : coverage.getCoveredRegions().entrySet()) {
				String chromosome = regions.getKey();
				ArrayList<Region> convertedRegions = regions.getValue()
						.stream()
						.map(Region::new)
						.collect(Collectors.toCollection(ArrayList::new));
				database.insertIntoTableCoveredIfNotExists(chromosome, sample.getId_sam(), convertedRegions);
			}
		}

		public Coverage getCoverageInformation() {
			return coverage;
		}

		public Sample getSample() {
			return sample;
		}
	}
	//endregion


	//region contamination files
	/**
	 * This method collects all files ending with "_contamination.table" within given Path {@param gatkDir}, links
	 * those files to corresponding samples, starts reading all files with corresponding samples and stores those
	 * information within the {@link Sample} model.
	 * @param samples
	 * @param runname
	 * @param gatkDir
	 */
	public void importContaminationFiles(List<Sample> samples, String runname, Path gatkDir) {
		String fileending = "_contamination.table";
		List<Path> contaminationFiles = (ArrayList<Path>) getFiles(gatkDir, fileending, new ArrayList<>());

		for (Sample sample : samples) {
			String filename = createFilenameOf(sample.getPatnr(), sample.getEnrichmentVersion(), runname, fileending);
			Optional<Path> contaminationFilePath = contaminationFiles
					.stream()
					.filter(path -> path.toString().endsWith(filename))
					.findFirst();
			if (contaminationFilePath.isPresent()) {
				sample.setContaminationFile(contaminationFilePath.get());
				Contamination.readsContaminationFile(sample, contaminationFilePath.get());
			}
		}
	}
	//endregion


	//region flag stat files
	/**
	 * This method collects all files ending with "_flagstat.txt" within given Path {@param gatkDir}, links
	 * those files to corresponding samples, starts reading all files with corresponding samples and stores those
	 * information within the {@link Sample} model.
	 * @param samples
	 * @param gatkDir directory to the gatk folder
	 */
	private void importFlagStatFiles(List<Sample> samples, String runname, Path gatkDir) {
		String fileending = "_flagstat.txt";
		ArrayList<Path> flagStatFiles = (ArrayList<Path>) getFiles(gatkDir, fileending, new ArrayList<>());

		for (Sample sample : samples) {
			String filename = createFilenameOf(sample.getPatnr(), sample.getEnrichmentVersion(), runname, fileending);
			Optional<Path> flagstatFilePath = flagStatFiles
					.stream()
					.filter(path -> path.toString().endsWith(filename))
					.findFirst();
			if (flagstatFilePath.isPresent()) {
				sample.setFlagstatFile(flagstatFilePath.get());
				FlagStat flagstat = new FlagStat();
				flagstat.read(flagstatFilePath.get());
				storesFlagstatInformationInSample(sample, flagstat);
			}
		}
	}

	/**
	 * @param sample
	 * @param flagstat
	 */
	private void storesFlagstatInformationInSample(Sample sample, FlagStat flagstat) {
		sample.setSingletons_qc_passed_reads(flagstat.getSingletons());
		sample.setProperlyPaired_qc_passed_reads(flagstat.getProperlyPaired());
		sample.setMapped_qc_passed_reads(flagstat.getMapped());
		sample.setTotal_qc_passed_reads(flagstat.getTotal());

	}
	//endregion
	//endregion


	//region update sample model
	private void updateSampleModel(MIDAS_DB database, List<Sample> samples) {

		for (Sample sample : samples) {

			Optional<Sample> databaseSample = database.selectSample(sample.getId_sam());
			if (!databaseSample.isPresent()) {
				createNewSample(database, sample);
				databaseSample = database.selectSample(sample.getId_sam());
			}
			lockSampleIfNeeded(database, sample);
			sample.adjustSampleState(databaseSample.get().getSamplestate());
			if (sample.needsToGetDatabaseUpdate(databaseSample.get())) {
				database.updateSample(sample);
			}

		}

	}

	private void lockSampleIfNeeded(MIDAS_DB database, Sample sample) {
		Optional<String> message = sample.getLockMessage();
		if (message.isPresent()) {
			lockSampleModel(sample);
			database.insertIntoSampleLockIfExists(
					sample.getId_sam(),
					database.get_id_usr_ImportHandler(),
					sample.getId_sas(),
					Timestamp.from(java.time.Instant.now()),
					message.get());
		}
	}

	/**
	 * Inserts given sample in database and connects it to a likewise newly inserted patient with standard sex unknown.
	 *
	 * @param database
	 * @param sample
	 */
	private void createNewSample(MIDAS_DB database, Sample sample) {
		try {
			Optional<Integer> id_pat = database.insertIntoTablePatient(1);
			sample.setId_pat(id_pat.get());
			database.insertIntoTableSample(sample);
		} catch (NoSuchElementException e) {
			LOGGER.log(Level.SEVERE, "Insert Sample " + sample.getPatnr() +
					" into database failed (" + e.getMessage() + ")", e);
		}
	}

	/**
	 * This method sets the {@link Sample} in the lock {@link SampleState} equivalent to its not locked
	 * {@link SampleState} counterpart.
	 * E.g. SampleState.ANALYSIS_PENDING -> SampleState.LOCKED_ANALYSIS_PENDING
	 *
	 * @param sample
	 */
	private void lockSampleModel(Sample sample) {
		switch (sample.getSamplestate()) {
			case PLANNED:
			case NOTFOUND:
			case SEQUENCING_ANALYSIS_STARTED:
				sample.setSamplestate(SampleState.LOCKED_SEQUENCING_ANALYSIS_STARTED,
						samplestates.get(SampleState.LOCKED_SEQUENCING_ANALYSIS_STARTED));
				break;
			case ANALYSIS_PENDING:
				sample.setSamplestate(SampleState.LOCKED_ANALYSIS_PENDING,
						samplestates.get(SampleState.LOCKED_ANALYSIS_PENDING));
				break;
			case ANALYSIS_PREPARED:
				sample.setSamplestate(SampleState.LOCKED_ANALYSIS_PREPARED,
						samplestates.get(SampleState.LOCKED_ANALYSIS_PREPARED));
				break;
			case ANALYSIS_STARTED:
				sample.setSamplestate(SampleState.LOCKED_ANALYSIS_STARTED,
						samplestates.get(SampleState.LOCKED_ANALYSIS_STARTED));
				break;
			case ANALYSIS_DONE:
				sample.setSamplestate(SampleState.LOCKED_ANALYSIS_DONE,
						samplestates.get(SampleState.LOCKED_ANALYSIS_DONE));
				break;
			case LOCKED_SEQUENCING_ANALYSIS_STARTED:
			case LOCKED_ANALYSIS_PENDING:
			case LOCKED_ANALYSIS_PREPARED:
			case LOCKED_ANALYSIS_STARTED:
			case LOCKED_ANALYSIS_DONE:
				LOGGER.log(Level.FINER, "Sample is already locked: " + sample.getSamplestate());
				break;
			default:
				LOGGER.log(Level.SEVERE, "SampleState could not be identified: " + sample.getSamplestate());
		}
	}
	//endregion


    // region CNV import
    /**
     * Parse all _processed_<gender>.txt files in given directory
     *
     * @param enrversDir
     * @return {@link List} with successfully processed samples (CNV analysis)
     * for given enrichmentversion directory
     */
    private List<String> parseProcessedSampleFiles(Path enrversDir) {

        List<Path> processedFiles = new ArrayList<>();
        List<String> processedSamples = new ArrayList<>();
        Pattern linePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\s{3}(\\d{8}-.*)\\scomplete");

        try {
            DirectoryStream<Path> contentStream = Files.newDirectoryStream(enrversDir);
            for (Path contentDir : contentStream) {
                if(contentDir.getFileName().toString().startsWith("_processed")) {
                    processedFiles.add(contentDir);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.log(Level.WARNING, "Could not open stream for directory: " + enrversDir);
        }


        for (Path file : processedFiles) {
            try (Scanner scanner = new Scanner(file.toFile())) {

                while(scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    Matcher matcher = linePattern.matcher(line);

                    if(matcher.matches()) {
                        processedSamples.add(matcher.group(1));
                    }
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                LOGGER.log(Level.WARNING, "Could not scan file: " + file);
            }
        }

        return processedSamples;
    }


    /**
     * Parses filename to get id_sam and fill sample model
     * Returns Optional.empty if at least one of the following criteria is fulfilled:
     *      - filename doesn't match pattern
     *      - illuminarunname is not in DB
     *      - sample is not in DB
     *      - CNV results of sample are already in DB
     *
     * @param filename
     * @param database
     * @return optional of id_sam
     */
    private Optional<Sample> fillSampleModelCNV(String filename, HashMap<Integer, Sample> samples, MIDAS_DB database) {

        Pattern cnvFilePattern = Pattern.compile("(\\d{8})-(.*)-(\\d{6}_.*_\\d{4}_.*)");
        Matcher cnvFileMatcher = cnvFilePattern.matcher(filename);

        if(!cnvFileMatcher.matches()) {
            LOGGER.log(Level.WARNING, "Not a CNV result file: " + filename);
            return Optional.empty();
        }

        String patnr = cnvFileMatcher.group(1);
        String enrichmentversion = cnvFileMatcher.group(2).replace("_", ".");
        String illuminarunname = cnvFileMatcher.group(3);
        int id_enrvers = enrichmentversions.get(enrichmentversion);

        Optional<Integer> id_run = database.selectRunIDByIlluminarunname(illuminarunname);
        if(!id_run.isPresent()) {
//            LOGGER.log(Level.INFO, "Could not get id_run of run: " + illuminarunname);
            return Optional.empty();
        }

        Optional<Integer> id_sam = database.select_id_sam_by_patnr_id_enrvers_id_run(patnr, id_enrvers, id_run.get());
        if(!id_sam.isPresent()) {
            LOGGER.log(Level.WARNING, "Sample not in DB (patnr: " + patnr +
                    ", enrichmentversion: " + enrichmentversion +
                    ", run: " + illuminarunname + ")");
            return Optional.empty();
        }

        if(samples.containsKey(id_sam.get())) return Optional.of(samples.get(id_sam.get()));

        if(database.sampleCNVsAlreadyInDB(id_sam.get())) {
            LOGGER.log(Level.INFO, "CNV results already in DB for id_sam: " + id_sam.get());
            return Optional.empty();
        }

        Sample sample = new Sample();
        sample.setId_sam(id_sam.get());
        sample.setPatnr(patnr);
        sample.setId_enrvers(id_enrvers);
        sample.setId_run(id_run.get());

        return Optional.of(sample);
    }


    /**
     * Parse file line (target) and fill target model of sample
     *
     * @param database
     * @param cnvResultsDir
     * @param caller
     * @param sample
     */
    private void fillTargetModel(MIDAS_DB database, Path cnvResultsDir, String caller, Sample sample) {
        try (Scanner scanner = new Scanner(cnvResultsDir.toFile())) {
            String line;
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                Pattern targetPattern = Pattern.compile("(chr[0-9XY]+)\t(\\d+)\t(\\d+)\t.*\t(\\d+)\t\\.\t\\d+\t\\d+\t[0-9,]+");
                Matcher matcher = targetPattern.matcher(line);
                if(!matcher.matches()) {
                    continue;
                }

                String identifier = database.getIdentifier(matcher.group(1));
                int g_start = Integer.parseInt(matcher.group(2));
                int g_end = Integer.parseInt(matcher.group(3));
                String type = cnvType.get(Integer.parseInt(matcher.group(4)));
                Optional<Integer> id_rrf = database.getIdRrfTarget(identifier, g_start, g_end);
                if(!id_rrf.isPresent()) {
                    continue;
                }

                Target target = new Target();
                target.setId_rrf(id_rrf.get());
                target.setIdentifier(identifier);
                target.setG_start(g_start);
                target.setG_end(g_end);
                target.setId_cal(cnvCaller.get(caller));
                target.setCaller(caller);
                target.setType(type);

                sample.getTargets().add(target);

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    /**
     * Group sample targets to CNVs
     * @param sample
     * @return
     */
    private List<CNV> groupSampleCNVs(Sample sample, HashMap<Integer, Integer> orderReferenceTargets, MIDAS_DB database) {

    	List<Target> targets = sample.getTargets();

    	// First step: Sort targets by chr, start, stop, type
        Collections.sort(targets);

        // Second step: define evidence group for targets
        defineEvidenceGroup(targets);

        // Third step: group targets to cnvs
		int c = -20; // Score to start new CNV

		// (1) Init matrix M
		int[][] M = initScoreMatrix(orderReferenceTargets, targets, c);

		// (2) Recursion: Fill score matrix M
		fillScoreMatrix(orderReferenceTargets, targets, c, M);

		// (3) Traceback: Find CNVs
		List<CNV> cnvs = traceback(sample, orderReferenceTargets, database, targets, c, M);

		return cnvs;
    }

	private List<CNV> traceback(Sample sample, HashMap<Integer, Integer> orderReferenceTargets, MIDAS_DB database, List<Target> targets, int c, int[][] M) {
		int n = targets.size();
		List<CNV> cnvs = new ArrayList<>();
		CNV currentCNV = new CNV();

		int j = n-1;
		int i = argmax(IntStream.range(0, n).map(x -> M[x][n-1]).toArray());

		currentCNV.getTargets().add(targets.get(j));

		while (i > 0 || j > 0) {

			int addTargetScore = addTargetToCNVScore(targets.get(j-1), targets.get(j), orderReferenceTargets);

			// Horizontal step
			if(i == 0 || M[i-1][j-1] == Integer.MIN_VALUE || M[i][j] == M[i][j-1] + addTargetScore) {
				currentCNV.getTargets().add(targets.get(j-1));
				j = j - 1;
				continue;
			}

			// Diagonal step
			if (M[i][j] == M[i-1][j-1] + c) {
				cnvs.add(fillCNVModel(sample, currentCNV, database));
				currentCNV = new CNV();
				currentCNV.getTargets().add(targets.get(j-1));
				j = j - 1;
				i = i - 1;
				continue;
			}

			System.out.println("No traceback step possible: ");
			System.out.println("M[i][j]: " + M[i][j]);
			System.out.println("M[i-1][j-1]: " + M[i-1][j-1]);
			System.out.println("M[i][j-1]: " + M[i][j-1]);
			System.out.println("Target j: " + targets.get(j).getId_rrf());
			System.out.println("\tType: " + targets.get(j).getType());
			System.out.println("\tEvidence: " + targets.get(j).getEvidencegroup());
			System.out.println("Target j-1: " + targets.get(j-1).getId_rrf());
			System.out.println("\tType: " + targets.get(j-1).getType());
			System.out.println("\tEvidence: " + targets.get(j-1).getEvidencegroup());

		}

		cnvs.add(fillCNVModel(sample, currentCNV, database));
		return cnvs;
	}


	/**
	 * Method initializes score matrix M (First step of grouping algorithm)
	 *
	 * @param orderReferenceTargets
	 * @param targets
	 * @param c
	 * @return
	 */
	private int[][] initScoreMatrix(HashMap<Integer, Integer> orderReferenceTargets, List<Target> targets, int c) {
		int n = targets.size();
		int[][] M = new int[n][n];
		M[0][0] = 0;
		for (int i = 1; i < n; i++) {
			M[i][i] = i * c;

			int addTargetScore = addTargetToCNVScore(targets.get(i - 1), targets.get(i), orderReferenceTargets);

			if (M[0][i - 1] == Integer.MIN_VALUE || addTargetScore == Integer.MIN_VALUE) { // To avoid integer overflow
				M[0][i] = Integer.MIN_VALUE;

			} else {
				M[0][i] = M[0][i - 1] + addTargetScore;
			}
		}

		return M;
	}


	/**
	 * Method fills upper right score matrix M (Second step of grouping algorithm)
	 * @param orderReferenceTargets
	 * @param targets
	 * @param c
	 * @param M
	 */
	private void fillScoreMatrix(HashMap<Integer, Integer> orderReferenceTargets, List<Target> targets, int c, int[][] M) {
		int n = targets.size();
		int dia;
		int hor;

		for (int i = 1; i < n-1; i++) {
			for (int j = i + 1; j < n; j++) {

				int addTargetScore = addTargetToCNVScore(targets.get(j-1), targets.get(j), orderReferenceTargets);
				if (M[i-1][j-1] == Integer.MIN_VALUE) {
					dia = Integer.MIN_VALUE;
				} else {
					dia = M[i-1][j-1] + c;
				}

				if (M[i][j-1] == Integer.MIN_VALUE || addTargetScore == Integer.MIN_VALUE) {
					hor = Integer.MIN_VALUE;
				} else {
					hor = M[i][j-1] + addTargetScore;
				}

				M[i][j] = Math.max(dia, hor);
			}

		}
	}


	/**
	 * Returns the matching score if the current target is added to the previous target
	 * @param target
	 * @param prevTarget
	 * @return score
	 */
    private int addTargetToCNVScore(Target prevTarget, Target target, HashMap<Integer, Integer> orderReferenceTargets) {

    	int sm = 2;

    	if(!target.getIdentifier().equals(prevTarget.getIdentifier())
				|| !target.getType().equals(prevTarget.getType())
				|| target.getG_start() > prevTarget.getG_end() + 100000) {
    		return Integer.MIN_VALUE;
		}

    	if(target.getId_rrf() == prevTarget.getId_rrf()
				|| (target.getEvidencegroup() == prevTarget.getEvidencegroup()
				&& orderReferenceTargets.get(prevTarget.getId_rrf()) == target.getId_rrf())) {
    		return sm;
		}

    	int numTargetsInBetween = countTargetsInBetween(prevTarget.getId_rrf(), target.getId_rrf(), orderReferenceTargets);
    	return -numTargetsInBetween - Math.abs(prevTarget.getEvidencegroup() - target.getEvidencegroup());
	}


	/**
	 * Return argmax of array
	 * @return argmax
	 */
	private static int argmax(int[] m) {
		int bestIdx = -1;
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < m.length; i++) {
			int elem = m[i];
			if (elem > max) {
				max = elem;
				bestIdx = i;
			}
		}
		return bestIdx;
	}


	/**
	 * Return the number of targets between two given target IDs
	 * @param targetId1
	 * @param targetId2
	 * @param orderReferenceTargets
	 * @return number of targets in between
	 */
	private int countTargetsInBetween(int targetId1, int targetId2, HashMap<Integer, Integer> orderReferenceTargets) {

		int cnt = 0;

    	if(targetId1 == targetId2) {
    		return cnt;
		}

    	int prevTargetId = targetId1;
    	while(orderReferenceTargets.get(prevTargetId) != targetId2) {
    		prevTargetId = orderReferenceTargets.get(prevTargetId);
    		cnt++;
		}

    	return cnt;
	}



    /**
     * Fill CNV attributes based on assigned targets
     *
     * @param sample
     * @param currentCNV
     * @return
     */
    private CNV fillCNVModel(Sample sample, CNV currentCNV, MIDAS_DB database) {

        currentCNV.setId_sam(sample.getId_sam());
        currentCNV.setIdentifier(currentCNV.getTargets().get(0).getIdentifier());
        currentCNV.setType(currentCNV.getTargets().get(0).getType());
        currentCNV.determineMinSize();
        currentCNV.determineMaxSize(sample.getId_enrvers(), database);
        return currentCNV;
    }


    /**
	 * This method defines the evidencegroup (number of caller that identified the target) for each target
	 * @param targets
	 */
	private void defineEvidenceGroup(List<Target> targets) {
		Map<Integer, Map<String, List<Target>>> map = targets.stream()
				.collect(groupingBy(Target::getId_rrf, groupingBy(Target::getType)));

		for (Integer id_rrf : map.keySet()) {
			for(String type : map.get(id_rrf).keySet()) {
				for (Target target : map.get(id_rrf).get(type)) {
					target.setEvidencegroup(map.get(id_rrf).get(type).size());
				}
			}
		}

	}
	// endregion



	/**
	 * This method sets the {@link Run} in the lock {@link RunState} equivalent to its not locked {@link RunState} counterpart.
	 * E.g. RunState.SEQUENCING_IN_PROGRESS -> RunState.LOCKED_SEQUENCING_IN_PROGRESS
	 * Lockmessage will be added to {@link Run} as well.
	 *
	 * @param run
	 * @param message
	 */
	private void lockRunModel(Run run, String message) {
		switch (run.getRunState()) {
			case SEQUENCING_IN_PROGRESS:
				run.setRunState(RunState.LOCKED_SEQUENCING_IN_PROGRESS);
				run.setLockreason(message);
				break;
			case SEQUENCING_COMPLETED:
				run.setRunState(RunState.LOCKED_SEQUENCING_COMPLETED);
				run.setLockreason(message);
				break;
			case DEMULTIPLEXING_IN_PROGRESS:
				run.setRunState(RunState.LOCKED_DEMULTIPLEXING_IN_PROGRESS);
				run.setLockreason(message);
				break;
			case DEMULTIPLEXING_COMPLETED:
				run.setRunState(RunState.LOCKED_DEMULTIPLEXING_COMPLETED);
				run.setLockreason(message);
				break;
			case ANALYSIS_IN_PROGRESS:
				run.setRunState(RunState.LOCKED_ANALYSIS_IN_PROGRESS);
				run.setLockreason(message);
				break;
			case ANALYSIS_COMPLETED:
				run.setRunState(RunState.LOCKED_ANALYSIS_COMPLETED);
				run.setLockreason(message);
				break;
			case IMPORT_STARTET:
				run.setRunState(RunState.LOCKED_IMPORT_STARTET);
				run.setLockreason(message);
				break;
			case IMPORT_COVERAGE_COMPLETED:
				run.setRunState(RunState.LOCKED_IMPORT_COVERAGE_COMPLETED);
				run.setLockreason(message);
				break;
			case IMPORT_COMPLETED:
				run.setRunState(RunState.LOCKED_IMPORT_COMPLETED);
				run.setLockreason(message);
				break;
			case ARCHIVED:
				run.setRunState(RunState.LOCKED_ARCHIVED);
				run.setLockreason(message);
				break;
			case LOCKED_SEQUENCING_IN_PROGRESS:
			case LOCKED_SEQUENCING_COMPLETED:
			case LOCKED_DEMULTIPLEXING_IN_PROGRESS:
			case LOCKED_DEMULTIPLEXING_COMPLETED:
			case LOCKED_ANALYSIS_IN_PROGRESS:
			case LOCKED_ANALYSIS_COMPLETED:
			case LOCKED_IMPORT_STARTET:
			case LOCKED_IMPORT_COVERAGE_COMPLETED:
			case LOCKED_IMPORT_COMPLETED:
			case LOCKED_ARCHIVED:
				LOGGER.log(Level.INFO, "Run is already locked: " + run.getRunState());
				break;
			default:
				LOGGER.log(Level.SEVERE, "RunState could not be identified: " + run.getRunState());
		}
	}


    /** Return directory stream for given path
     *
     * @param path
     * @return DirectoryStream
     */
    private DirectoryStream<Path> getStream(Path path) {
        DirectoryStream<Path> stream = null;

        try {
            stream = Files.newDirectoryStream(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stream;
    }

}