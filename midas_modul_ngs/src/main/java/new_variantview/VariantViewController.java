/*
 *  midas: ngs.client
 * Autor: Lydia Leifels <lydia.leifels@medizinische-genetik.de>
 *
 * Zentrum für Humangenetik und Laboratoriumsdiagnostik (MVZ)
 * Dr. Klein, Dr. Rost und Kollegen
 * Copyright (c) 2019. All rights reserved
 *
 * Abhängigkeiten: siehe pom.xml
 * Kurzbeschreibung: siehe Javadoc
 * $Revision$
 */

package new_variantview;

import common.wizard.WizardDialogController;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.util.StringConverter;
import laufdb.models.RunInfoModel;
import laufdb.models.SampleInfoModel;
import new_variantview.filters.GeneFilter;
import new_variantview.model.SamInvCoverageContainer;
import new_variantview.model.VariantViewModel;
import new_variantview.reports.ReportWizard;
import ngstransfer.entities.ngs.Lock;
import ngstransfer.entities.ngs.Region;
import ngstransfer.entities.ngs.Sample;
import ngstransfer.entities.variantdb.regioncoverage.RegionCoverage;
import ngstransfer.new_variantview.VariantTableRecord;
import variantdb.controller.CNVTabController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class VariantViewController {
    @FXML
    private CheckBox chkbColor;
    @FXML
    private ChoiceBox<SamInvCoverageContainer> drpdInvestigation;
    @FXML
    private Button btnDeleteInv;
    @FXML
    private Button btnMidReport;
    @FXML
    private Button btnMainReport;
    @FXML
    private Label lblEnrVersKit;
    @FXML
    private TabPane tbpDisplays;
    @FXML
    private Tab tbVariants;
    @FXML
    private Tab tbCoverage;
    @FXML
    private Tab tbCNV;
    @FXML
    private Parent variantTab;
    @FXML
    private VariantTabController variantTabController;
    @FXML
    private Parent coverageTab;
    @FXML
    private CoverageTabController coverageTabController;
    @FXML
    private Parent cnvTab;
    @FXML
    private CNVTabController cnvTabController;
    @FXML
    private Label lblLoggMessage;

    public ChoiceBox<SamInvCoverageContainer> getDrpdInvestigation() {
        return drpdInvestigation;
    }

    // Implemented by another developer
    @FXML
    void initialize() {
        drpdInvestigation.getSelectionModel().selectedItemProperty().addListener(this::handleSelection);
        drpdInvestigation.setConverter(new StringConverter<SamInvCoverageContainer>() {
            @Override
            public String toString(SamInvCoverageContainer object) {
                return object.getSampleInvestigationCont().toString();
            }

            @Override
            public SamInvCoverageContainer fromString(String string) {
                return null;
            }
        });

        btnDeleteInv.disableProperty().bind(drpdInvestigation.getSelectionModel().selectedItemProperty().isNull());
        btnMidReport.disableProperty().bind(drpdInvestigation.getSelectionModel().selectedItemProperty().isNull());
        btnMainReport.disableProperty().bind(drpdInvestigation.getSelectionModel().selectedItemProperty().isNull());

        lblLoggMessage.setText("");

    }


    /**
     * On Selection of an Element in the dropdown menu
     * All variants for the selected sam-inv-container will be collected and displayed in the tableview
     *
     * @param observable the source of the event
     */
    @SuppressWarnings("unused")
    private void handleSelection(Observable observable) {
        SamInvCoverageContainer container = drpdInvestigation.getSelectionModel().getSelectedItem();
        if (container != null) {
            variantTabController.setDataset(container);
            Sample sample = container.getSampleInvestigationCont().getSample();
            coverageTabController.getCoverages().setAll(container.getRegionCoverages());
            coverageTabController.setSample(sample);

            cnvTabController.getCnvs().setAll(container.getCnvTableRecords());
            cnvTabController.setSample(sample);
            //set label for enrichmentkit
            lblEnrVersKit.setText(sample.getEnrichmentversion().getEnrichmentversion());

            VariantViewConditions conditions = VariantViewConditions.getInstance();
            String warningMessage = conditions.finalReportCreationIsPossible(sample);
            lblLoggMessage.setText(warningMessage);
        } else {
            variantTabController.setDataset(null);
            coverageTabController.getCoverages().clear();
            cnvTabController.getCnvs().clear();
            cnvTabController.setSample(null);
            lblEnrVersKit.setText("");
            lblLoggMessage.setText("");
        }
    }

    // Implemented by another developer
    public void addInvestigation(SamInvCoverageContainer container) {
        if (drpdInvestigation.getItems().contains(container)) {
            return;
        }
        drpdInvestigation.getItems().add(container);
    }


    // Implemented by another developer
    @FXML
    void handleBtnDeleteInv(ActionEvent event) {
        SamInvCoverageContainer selectedItem = drpdInvestigation.getSelectionModel().getSelectedItem();
        drpdInvestigation.getSelectionModel().select(null);
        drpdInvestigation.getItems().remove(selectedItem);

    }

    // Implemented by another developer
    @FXML
    void handleBtnMainReport(ActionEvent event) {
        showReportDialog(true);
    }

    // Implemented by another developer
    @FXML
    void handleBtnMidReport(ActionEvent event) {
        showReportDialog(false);
    }

    // Implemented by another developer
    /**
     * open reportWizard
     *
     * @param finalReport Should the report be final or preliminary?
     */
    private void showReportDialog(boolean finalReport) {
        SamInvCoverageContainer si = drpdInvestigation.getSelectionModel().getSelectedItem();

        if (finalReport) {
            Integer idSam = si.getSampleInvestigationCont().getSample().getId();
            Long idRun = si.getSampleInvestigationCont().getSample().getRun().getId_run();

            if (!validateSampleForFinalreport(idSam, idRun)) {
                return;
            }
        }

        List<VariantTableRecord> variantTableRecords = si.getVariantTableRecords()
                .stream()
                .filter(vtr -> vtr.selected)
                .collect(Collectors.toList());
        VariantViewModel model = new VariantViewModel();
        List<Region> investigationGenes = model.getInvestigationGenes(si.getSampleInvestigationCont().getInvestigation().getId_inv());
        GeneFilter geneFilter = variantTabController.getFilterManager().getGeneFilter();
        // Only show the genes which passed the genes filter
        if (geneFilter != null) {
            investigationGenes.retainAll(geneFilter.getGenes());
        }

        List<RegionCoverage> regionCoverages = coverageTabController.getSelectedCoverages();
        try {
            ReportWizard reportWizard = new ReportWizard(
                    finalReport,
                    si.getSampleInvestigationCont(),
                    variantTableRecords,
                    //list of variant table attributes displayed
                    variantTabController.getAvailableAttributes(),
                    //list of active filters
                    variantTabController.getFilterManager().getAttributeFiltersList(),
                    //list of genes in investigation
                    investigationGenes,
                    //add region coverage list to wizard
                    regionCoverages
            );
            WizardDialogController.showWizardDialog(reportWizard, drpdInvestigation.getScene().getWindow(), Modality.NONE);
        } catch (IOException e) {
            Logger.getGlobal().log(Level.SEVERE, "Could not instantiate ReportWizard", e);
        }
    }

    // Implemented by another developer
    /**
     * Validate whether a final report can be made.
     * Report cannot be made if sample or run is locked.
     * Checks vs. database in case sample or run got locked in the meantime.
     *
     * @param id_sam of object sample of table sample
     * @param id_run of object run of table run
     * @return false if sample is not valid and true otherwise
     */
    private boolean validateSampleForFinalreport(Integer id_sam, Long id_run) {

        SampleInfoModel sampleInfoModel = new SampleInfoModel();
        RunInfoModel runInfoModel = new RunInfoModel();
        List<Lock> sampleLockHistory = new ArrayList<>();
        List<Lock> runLockHistory = new ArrayList<>();
        boolean validSam;
        boolean validRun;

        try {
            sampleLockHistory = sampleInfoModel.getSampleLockHistory(id_sam);
            runLockHistory = runInfoModel.getRunLockHistory(id_run);
        } catch (Exception e) {
            Logger.getGlobal().log(Level.SEVERE, "Could not get sample by id_sam", e);
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("MIDAS");
            a.setHeaderText("Sample-Status oder Run-Status konnte nicht abgefragt werden\n" +
                    " um das Erstellen eines Finalen Reports zu verifizieren.");
            a.setContentText("Bitte kontaktieren sie einen Administrator.");
            a.initOwner(btnMainReport.getScene().getWindow());
            a.showAndWait();
        }

        //get most recent lock history, doesnt matter if entry list is empty
        //sort according to timestamp reversed to get it decreasing
        //get to first item in list --> most recent
        Optional<Lock> latestSamLock = sampleLockHistory.stream().max(Comparator.comparing(Lock::getTimestamp));

        //sort according to timestamp reversed to get it decreasing
        //get to first item in list --> most recent
        Optional<Lock> latestRunLock = runLockHistory.stream().max(Comparator.comparing(Lock::getTimestamp));

        //status for sample locked = 7,8,9,10
        validSam = latestSamLock.map(lock -> !lock.getStatus().isLocked()).orElse(true);

        //status for run locked = 9,10,11,12,13,14,15,18,19
        validRun = latestRunLock.map(lock -> !lock.getStatus().isLocked()).orElse(true);

        //if one of them is invalid
        if(!validSam && !validRun){

            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("MIDAS");
            a.setHeaderText("Für dieses Sample kann im Moment kein Abschlussbericht erstellt werden!");
            a.setContentText("Das Sample und der Lauf sind gesperrt.\n" +
                    "Sample-Sperrung - " + latestSamLock.get().getUserName() + ": " + latestSamLock.get().getComment() +
                    "\nLauf-Sperrung - " + latestRunLock.get().getUserName() + ": " + latestRunLock.get().getComment());
            a.initOwner(btnMainReport.getScene().getWindow());
            a.showAndWait();
            lblLoggMessage.setText("Lauf und Sample\nsind gesperrt");
            return false;

        } else if (!validSam) {

            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("MIDAS");
            a.setHeaderText("Für dieses Sample kann im Moment kein Abschlussbericht erstellt werden!");
            String from = latestSamLock.get().getUserName() + ": " + latestSamLock.get().getComment();
            a.setContentText("Das Sample wurde gesperrt.\n" + from);
            a.initOwner(btnMainReport.getScene().getWindow());
            a.showAndWait();
            lblLoggMessage.setText("Sample\nist gesperrt");
            return false;

        } else if (!validRun) {

            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("MIDAS");
            a.setHeaderText("Für dieses Sample kann im Moment kein Abschlussbericht erstellt werden!");
            String from = latestRunLock.get().getUserName() + ": " + latestRunLock.get().getComment();
            a.setContentText("Der Lauf wurde gesperrt.\n" + from);
            a.initOwner(btnMainReport.getScene().getWindow());
            a.showAndWait();
            lblLoggMessage.setText("Lauf\nist gesperrt");
            return false;

        } else {
            lblLoggMessage.setText("");
            return true;
        }
    }
}
