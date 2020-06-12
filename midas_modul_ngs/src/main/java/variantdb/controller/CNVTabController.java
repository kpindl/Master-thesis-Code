package variantdb.controller;

import io.aexp.nodes.graphql.Argument;
import io.aexp.nodes.graphql.Arguments;
import io.aexp.nodes.graphql.InputObject;
import javafx.beans.Observable;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import ngstransfer.entities.ngs.Sample;
import variantdb.models.*;
import variantdb.models.CNVTableRecord;
import variantdb.wrapper.MutationWrapper;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CNVTabController {

    // region treeTableView
    @FXML
    private TreeTableView<CNVTableRecord> cnvTreeTableView;
    @FXML
    private TreeTableColumn<CNVTableRecord, String> typeClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, String> geneClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, String> nomenclatureClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, String> hgvsNomenclatureClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, String> iscnNomenclatureClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, String> numTargetsClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, Integer> minSizeClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, Integer> maxSizeClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, String> classClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, String> statusApprovedClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, String> methodApprovedClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, String> chrClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, Integer> startClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, Integer> stopClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, Integer> numCallerClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, String> callerClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, Double> frequClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, Integer> numSamplesClm;
    @FXML
    private TreeTableColumn<CNVTableRecord, String> transcriptClm;
    //endregion

    @FXML
    private MenuItem mniClassify;
    @FXML
    private MenuItem mniNomenclature;
    @FXML
    private MenuItem mnuCollapse;
    @FXML
    private MenuItem mnuExpand;

    private ObservableList<CNV> cnvs = FXCollections.observableArrayList();

    private SimpleObjectProperty<Sample> sample = new SimpleObjectProperty<>(this, "sample");

    // region getter and setter
    public ObservableList<CNV> getCnvs() {
        return cnvs;
    }

    public void setSample(Sample sample) {
        this.sample.set(sample);
    }
    // endregion


    @FXML
    public void initialize() {
        initColumns();
        cnvTreeTableView.getSelectionModel().selectedItemProperty().addListener(this::handleTableSelectionChanged);
        cnvs.addListener(this::handleCNVsChanged);
    }


    private void initColumns() {
        typeClm.setCellValueFactory(t -> new SimpleObjectProperty<>(t.getValue().getValue().getType()));
        geneClm.setCellValueFactory(t -> new SimpleObjectProperty<>(t.getValue().getValue().getGenes()));
        hgvsNomenclatureClm.setCellValueFactory(t -> new SimpleObjectProperty<>(t.getValue().getValue().getHgvsNomenclature()));
        iscnNomenclatureClm.setCellValueFactory(t -> new SimpleObjectProperty<>(t.getValue().getValue().getIscnNomenclature()));
        numTargetsClm.setCellValueFactory(t -> {
            if (t.getValue().getValue().getTargets().isEmpty()) {
                return new SimpleStringProperty();
            } else {
                Integer ind = t.getValue().getValue().getTargets().size();
                return new SimpleStringProperty(String.valueOf(ind));
            }
        });
        minSizeClm.setCellValueFactory(t -> new SimpleObjectProperty<>(t.getValue().getValue().getMinSize()));
        maxSizeClm.setCellValueFactory(t -> new SimpleObjectProperty<>(t.getValue().getValue().getMaxSize()));
        classClm.setCellValueFactory(t -> {
            if (Objects.isNull(t.getValue().getValue().getClassifications())) {
                return new SimpleStringProperty();
            } else {
                String name = t.getValue().getValue().getRecentClassificationName();
                return new SimpleStringProperty(String.valueOf(name));
            }
        });
        statusApprovedClm.setCellValueFactory(t -> new SimpleObjectProperty<>(t.getValue().getValue().getStatusApproved()));
        methodApprovedClm.setCellValueFactory(t -> new SimpleObjectProperty<>(t.getValue().getValue().getMethodApproved()));
        chrClm.setCellValueFactory(t -> {
            String chrom = t.getValue().getValue().getIdentifier();
            return new SimpleStringProperty(chrom.replaceAll("NC_[0]*([1-9][0-9]?)\\.[0-9]*", "chr$1"));
        });
        startClm.setCellValueFactory(t -> new SimpleObjectProperty<>(t.getValue().getValue().getStart()));
        stopClm.setCellValueFactory(t -> new SimpleObjectProperty<>(t.getValue().getValue().getEnd()));
        numCallerClm.setCellValueFactory(t -> new SimpleObjectProperty<>(t.getValue().getValue().getNumCaller()));
        callerClm.setCellValueFactory(t -> new SimpleObjectProperty<>(t.getValue().getValue().getCaller()));
        frequClm.setCellValueFactory(t -> new SimpleObjectProperty<>(t.getValue().getValue().getFrequency()));
        numSamplesClm.setCellValueFactory(t -> new SimpleObjectProperty<>(t.getValue().getValue().getNumSamples()));
        transcriptClm.setCellValueFactory(t -> new SimpleObjectProperty<>(t.getValue().getValue().getTranscriptsAsString()));
    }


    private void handleCNVsChanged(Observable observable) {

        if(cnvs.isEmpty()){
            cnvTreeTableView.setRoot(null);
            return;
        }

        TreeItem<CNVTableRecord> root = new TreeItem<>();
        cnvTreeTableView.setRoot(root);
        cnvTreeTableView.setShowRoot(false);


        for (CNV cnv : this.cnvs){
            TreeItem<CNVTableRecord> cnvEntry = new TreeItem<>(cnv);

            for (Target target : cnv.getTargets()){
                cnvEntry.getChildren().add(new TreeItem<>(target));
            }
            root.getChildren().add(cnvEntry);
        }
    }

    private void handleTableSelectionChanged(Observable observable, TreeItem oldValue, TreeItem newValue) {

        if (newValue == null || newValue.getValue() instanceof Target){
            mniNomenclature.setDisable(true);
            mniClassify.setDisable(true);
            return;
        }

        if (newValue.getValue() instanceof CNV) {
            mniNomenclature.setDisable(false);
            mniClassify.setDisable(false);
        }
    }


    @FXML
    private void handleClassify(ActionEvent actionEvent) {
        CNV item = (CNV) cnvTreeTableView.getSelectionModel().getSelectedItem().getValue();
        if(item == null) return;
        Optional<CNVClassification> result = CNVClassificationController.showAndWait(item, cnvTreeTableView.getScene().getWindow());
        result.ifPresent(c -> System.out.println(c.printClassification()));
        result.ifPresent(classification -> {
            item.addClassification(result.get());
            importClassificationDB(classification);
        });
        cnvTreeTableView.refresh();
    }


    @FXML
    private void handleNomenclature(ActionEvent event) {

        CNV item = (CNV) cnvTreeTableView.getSelectionModel().getSelectedItem().getValue();
        if(item == null) return;
        Optional<Nomenclature> result = CNVNomenclatureController.showAndWait(cnvTreeTableView.getScene().getWindow());
        result.ifPresent(nomenclature -> {
            item.setNomenclature(nomenclature.getGuideline(), nomenclature.getNomenclature());

            Boolean updateDone = MutationWrapper.getMutationWrapper(MutationWrapper.UpdateCNV.class,
                    new Arguments("updateCNVByNomenclature",
                    new Argument("id_cnv", item.getIdCnv()),
                    new Argument("hgvs", item.getHgvs()),
                    new Argument("iscn", item.getIscn()))).getUpdateCNVByNomenclature();

            if(!updateDone) {
                Logger.getLogger(CNVTabController.class.getName()).log(Level.SEVERE,
                        "Could not udpate new nomenclature (id_cnv: " + item.getIdCnv() + ")");
            }
        });
        cnvTreeTableView.refresh();
    }


    @FXML
    void handleCollapse(ActionEvent event) {
        cnvTreeTableView.getRoot().getChildren().forEach(c -> c.setExpanded(false));
    }


    @FXML
    void handleExpand(ActionEvent event) {
        cnvTreeTableView.getRoot().getChildren().forEach(c -> c.setExpanded(true));
    }

    private void importClassificationDB(CNVClassification classification) {

        // Import new classification
        InputObject cnvClassificationInput = classification.toInputObject();
        int id_ccl = MutationWrapper.getMutationWrapper(MutationWrapper.InsertClassification.class,
                new Arguments("insertCNVClassification",
                        new Argument("classification", cnvClassificationInput)))
                .getInsertCNVClassification()
                .getIdCcl();

        // Import all criteria
        classification.getCriteria().forEach(c -> {
            InputObject criterionInput = c.toInputObject();
            criterionInput.getMap().put("idCcl", id_ccl);
            int id_ccc =  MutationWrapper.getMutationWrapper(MutationWrapper.InsertCriterion.class,
                    new Arguments("insertCNVClassificationCriterion",
                            new Argument("criterion", criterionInput)))
                    .getInsertCNVClassificationCriterion()
                    .getIdCcc();
        });
    }
}
