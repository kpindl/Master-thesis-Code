package variantdb.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import variantdb.models.*;
import variantdb.wrapper.QueryWrapper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

public class CNVClassificationController {

    @FXML
    private ResourceBundle resources;
    @FXML
    private URL location;

    // region classifications criteria
    @FXML
    private ChoiceBox<CriterionChoice> chbTandem;
    @FXML
    private TextField txtComTandem;
    @FXML
    private ChoiceBox<CriterionChoice> chbReadingFrame;
    @FXML
    private TextField txtComReadingFrame;
    @FXML
    private TextField txtValSize;
    @FXML
    private ChoiceBox<CriterionChoice> chbSize;
    @FXML
    private ChoiceBox<CriterionChoice> chbInheritance;
    @FXML
    private TextField txtComInheritance;
    @FXML
    private ChoiceBox<CriterionChoice> chbZygosity;
    @FXML
    private TextField txtComZygosity;
    @FXML
    private ChoiceBox<CriterionChoice> chbParentsPheno;
    @FXML
    private TextField txtComParentsPheno;

    @FXML
    private ChoiceBox<CriterionChoice> chbGeneRich;
    @FXML
    private TextField txtComGeneRich;
    @FXML
    private ChoiceBox<CriterionChoice> chbPseudogenes;
    @FXML
    private TextField txtComPseudogenes;
    @FXML
    private ChoiceBox<CriterionChoice> chbRepetitiveElem;
    @FXML
    private TextField txtComRepetitiveElem;
    @FXML
    private ChoiceBox<CriterionChoice> chbRegulatoryElem;
    @FXML
    private TextField txtComRegulatoryElem;

    @FXML
    private TextField txtValOmimG;
    @FXML
    private ChoiceBox<CriterionChoice> chbGeneCompellingFunc;
    @FXML
    private TextField txtComGeneCompellingFunc;
    @FXML
    private ChoiceBox<CriterionChoice> chbGeneDosageSens;
    @FXML
    private TextField txtComGeneDosageSens;
    @FXML
    private ChoiceBox<CriterionChoice> chbGeneClinDisorder;
    @FXML
    private TextField txtComGeneClinDisorder;
    @FXML
    private TextField txtValGeneClinDisorder;


    @FXML
    private ChoiceBox<CriterionChoice> chbPathoLit;
    @FXML
    private TextField txtComPathoLit;
    @FXML
    private ChoiceBox<CriterionChoice> chbPathoDB;
    @FXML
    private TextField txtComPathoDB;
    @FXML
    private ChoiceBox<CriterionChoice> chbPathoOverlap;
    @FXML
    private TextField txtComPathoOverlap;
    @FXML
    private ChoiceBox<CriterionChoice> chbBenignDB;
    @FXML
    private TextField txtComBenignDB;
    @FXML
    private ChoiceBox<CriterionChoice> chbContradictPub;
    @FXML
    private TextField txtComContradictPub;
    @FXML
    private ChoiceBox<CriterionChoice> chbUnknownSign;
    @FXML
    private TextField txtComUnknownSign;
    @FXML
    private TextField txtValPopulation;
    @FXML
    private ChoiceBox<CriterionChoice> chbPopulation;

    //endregion

    @FXML
    private ChoiceBox<Classification> chbClassification;
    @FXML
    private Button btnSaveDraft;
    @FXML
    private Button btnClassify;
//    @FXML
//    private Button btnAttachments;
//    @FXML
//    private ListView<CNVClassification.CNVAttachment> lvwAttachments;
//    @FXML
//    private ContextMenu cxmAttachments;
    @FXML
    private MenuItem mniRemove;
    @FXML
    private TextField txtReport;
    @FXML
    private TextField txtReportComment;
    @FXML
    private TextField txtLinkReport;
    @FXML
    private Label lblGene;
    @FXML
    private Label lblTmpClassification;

    private CNV cnv;
    private CNVClassification tmpClassification;
    private Map<String, CNVClassification.CNVClassificationCriterion> newCriteria;

    private Map<String, ChoiceBox<CriterionChoice>> choiceBoxes;
    private Map<String, TextField> commentFields;
    private Map<String, TextField> valueFields;

    /**
     * Cache
     */
    private Map<Integer, Classification> classifications;
    private Map<String, Integer> criteria;
    private Map<Integer, CriterionChoice> criteriaChoices;

    private final List<String> filterClassifications = Arrays.asList( "pathogenic", "likely pathogenic",
            "uncertain significance", "benign", "likely benign", "not classified");


    public CNVClassificationController(CNV cnv) {
        this.cnv = cnv;
        this.newCriteria = new HashMap<>();
        this.choiceBoxes = new HashMap<>();
        this.commentFields = new HashMap<>();
        this.valueFields = new HashMap<>();

        this.classifications = new HashMap<>();
        this.criteria = new HashMap<>();
        this.criteriaChoices = new HashMap<>();
    }

    public static Optional<CNVClassification> showAndWait(CNV cnv, Window window) {
        CNVClassificationController controller = new CNVClassificationController(cnv);
        FXMLLoader loader = new FXMLLoader(CNVClassificationController.class.getResource("/fxml/variantdb/CNV/CNVClassification.fxml"));
        loader.setController(controller);
        try {
            Parent root = loader.load();
            Scene scene = new Scene(root);
            Stage secondaryStage = new Stage();
            secondaryStage.initOwner(window);
            secondaryStage.setTitle("CNV Classification");
            secondaryStage.setScene(scene);
            secondaryStage.showAndWait();
            return Optional.ofNullable(controller.tmpClassification);

        }
        catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText("Konnte CNV Klassifikationstool nicht laden");
            a.initOwner(window);
            a.showAndWait();
        }

        return Optional.empty();
    }

    @FXML
    public void initialize(){

        fetchClassifications();
        fetchCriteria();
        fetchCriteriaChoices();
        accumulateControls();
        initClassificationWindow();
    }

    private void initClassificationWindow() {

        if(cnv.getType().equals("Duplication")) {
            chbTandem.setDisable(false);
        }

        lblGene.setText(cnv.getGenes());

        Integer currentId_cla = cnv.getClassifications().isEmpty() ? 1 : cnv.getClassifications().get(cnv.getClassifications().size()-1).getIdCla();
        lblTmpClassification.setText(classifications.get(currentId_cla).getName());
        chbClassification.getSelectionModel().select(classifications.get(currentId_cla));

        if(cnv.getClassifications().isEmpty()) {
            return;
        }

        CNVClassification currentClass = cnv.getClassifications().get(0);
        txtReport.setText(currentClass.getEvaluationtext());
        txtReportComment.setText(currentClass.getComment());
        txtLinkReport.setText(currentClass.getReport_ref());

        initCriteria(currentClass);
    }


    @FXML
    private void handleClassify(){

        if (chbClassification.getSelectionModel().getSelectedItem() == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText("Keine Klassifikation ausgewählt!");
            alert.showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(btnClassify.getScene().getWindow());
        alert.setHeaderText("Sind Sie sicher?");
        alert.setTitle("CNV klassifizieren");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.get() == ButtonType.OK) {
            updateTmpClassification(false);
            Stage window = (Stage) btnClassify.getScene().getWindow();
            window.close();
        }
    }




    /**
     * Update current CNV clasification with selected criteria choices and values
     *
     * @param draft Defines if CNV classification is draft or not
     */
    private void updateTmpClassification(Boolean draft) {
        System.out.println("updateTmpClassification");

        if(Objects.isNull(tmpClassification)) {
            tmpClassification = new CNVClassification();
        }

        tmpClassification.setIdCnv(this.cnv.getIdCnv());
        tmpClassification.setIdCla(chbClassification.getValue().getIdCla());
        tmpClassification.setName(chbClassification.getValue().getName());
        tmpClassification.setComment(txtReportComment.getText());
        tmpClassification.setEvaluationtext(txtReport.getText());
        tmpClassification.setDraft(draft);
        tmpClassification.setReport_ref(txtLinkReport.getText());

        // Update drowndownchoices
        choiceBoxes.keySet().stream()
                .filter(c -> !choiceBoxes.get(c).getSelectionModel().isEmpty())
                .forEach(c -> {
                    if(!newCriteria.containsKey(c)) {
                        newCriteria.put(c, new CNVClassification.CNVClassificationCriterion());
                        newCriteria.get(c).setIdCri(criteria.get(c));
                        newCriteria.get(c).setShortid(c);
                    }
                    newCriteria.get(c).setIdCrs(choiceBoxes.get(c).getValue().getIdCrs());

                });

        // Update comments
        commentFields.keySet().stream()
                .filter(c -> !commentFields.get(c).getText().isEmpty())
                .forEach(c -> {
                    if(!newCriteria.containsKey(c)) {
                        newCriteria.put(c, new CNVClassification.CNVClassificationCriterion());
                    }
                    newCriteria.get(c).setComment(commentFields.get(c).getText());
                });

        // Update values
        valueFields.keySet().stream()
                .filter(c -> !valueFields.get(c).getText().isEmpty())
                .forEach(c -> {
                    if(!newCriteria.containsKey(c)) {
                        newCriteria.put(c, new CNVClassification.CNVClassificationCriterion());
                    }
                    newCriteria.get(c).setComment(valueFields.get(c).getText());
                });

        tmpClassification.setCriteria(new ArrayList<>(newCriteria.values()));
    }


    /**
     * Fetches all abstract classifications
     */
    private void fetchClassifications() {
        List<Classification> classifications = QueryWrapper.getQueryWrapper(Classification.class).getClassifications();

        classifications.stream()
            .filter(c -> filterClassifications.contains(c.getName()))
            .forEach(c -> {
                this.classifications.put(c.getIdCla(), c);
                this.chbClassification.getItems().add(c);
            });
    }

    /**
     * Fetches all abstract criteria
     */
    private void fetchCriteria() {

        List<Criterion> criteria = QueryWrapper.getQueryWrapper(Criterion.class).getCriteria();
        criteria.forEach(c -> this.criteria.put(c.getShortid(), c.getIdCri()));
    }

    /**
     * Fetches all abstract criteria choices
     */
    private void fetchCriteriaChoices() {

        List<CriterionChoice> criteriaChoicesDb = QueryWrapper.getQueryWrapper(CriterionChoice.class).getCriterionChoices();

        criteriaChoicesDb.forEach(c -> this.criteriaChoices.put(c.getIdCrs(), c));

        List<CriterionChoice> defaultCriteriaChoices = Arrays.asList(
                criteriaChoices.get(1),
                criteriaChoices.get(2),
                criteriaChoices.get(3));
        List<CriterionChoice> databaseCriteriaChoices = Arrays.asList(
                criteriaChoices.get(1),
                criteriaChoices.get(4),
                criteriaChoices.get(6));

        chbTandem.getItems().addAll(defaultCriteriaChoices);
        chbReadingFrame.getItems().addAll(defaultCriteriaChoices);
        chbSize.getItems().addAll(Arrays.asList(
                criteriaChoices.get(16),
                criteriaChoices.get(17),
                criteriaChoices.get(18)));
        chbInheritance.getItems().addAll(Arrays.asList(
                criteriaChoices.get(1),
                criteriaChoices.get(7),
                criteriaChoices.get(8),
                criteriaChoices.get(9),
                criteriaChoices.get(10)));
        chbZygosity.getItems().addAll(Arrays.asList(
                criteriaChoices.get(1),
                criteriaChoices.get(11),
                criteriaChoices.get(12),
                criteriaChoices.get(13)));
        chbParentsPheno.getItems().addAll(defaultCriteriaChoices);
        chbGeneRich.getItems().addAll(defaultCriteriaChoices);
        chbPseudogenes.getItems().addAll(defaultCriteriaChoices);
        chbRepetitiveElem.getItems().addAll(defaultCriteriaChoices);
        chbRegulatoryElem.getItems().addAll(defaultCriteriaChoices);
        chbGeneCompellingFunc.getItems().addAll(defaultCriteriaChoices);
        chbGeneDosageSens.getItems().addAll(Arrays.asList(
                criteriaChoices.get(1),
                criteriaChoices.get(14),
                criteriaChoices.get(15)));
        chbGeneClinDisorder.getItems().addAll(defaultCriteriaChoices);
        chbPathoLit.getItems().addAll(databaseCriteriaChoices);
        chbPathoDB.getItems().addAll(databaseCriteriaChoices);
        chbPathoOverlap.getItems().addAll(defaultCriteriaChoices);
        chbBenignDB.getItems().addAll(databaseCriteriaChoices);
        chbContradictPub.getItems().addAll(defaultCriteriaChoices);
        chbUnknownSign.getItems().addAll(defaultCriteriaChoices);
        chbPopulation.getItems().addAll(Arrays.asList(
                criteriaChoices.get(19),
                criteriaChoices.get(20),
                criteriaChoices.get(21),
                criteriaChoices.get(22),
                criteriaChoices.get(23),
                criteriaChoices.get(24),
                criteriaChoices.get(25)));
    }


    /**
     * Indexes ChoiceBoxes and TextFields into Maps so they can be accessed by their associated criterion.
     * ChoiceBoxes must start with "chb" and textFields must either start with "txtCom" for comment fields
     * or with "txtVal" for value fields
     */
    private void accumulateControls() {
        for (Field field : this.getClass().getDeclaredFields()) {
            try {

                if (field.getType() == ChoiceBox.class) {
                    String criterionName = field.getName().substring(3);
                    ChoiceBox choiceBox = (ChoiceBox) field.get(this);
                    if (choiceBox != chbClassification)
                        choiceBoxes.put(criterionName, choiceBox);
                }

                if (field.getType() == TextField.class) {
                    String criterionName = field.getName().substring(6);
                    TextField textField = (TextField) field.get(this);

                    if (field.getName().startsWith("txtCom")) {
                        commentFields.put(criterionName, textField);
                    } else if (field.getName().startsWith("txtVal")) {
                        valueFields.put(criterionName, textField);
                    }
                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     *  Initialise criteriachoices, comments and values based on eventual previous classifications
     */
    private void initCriteria(CNVClassification currentClass) {

        System.out.println("init criteria for current classification");
        for (CNVClassification.CNVClassificationCriterion criterion : currentClass.getCriteria()) {
            String shortid = criterion.getShortid();

            // Set criteriachoices
            if (choiceBoxes.containsKey(shortid) && Objects.nonNull(criteriaChoices.get(criterion.getIdCrs()))) {
                choiceBoxes.get(shortid).getSelectionModel().select(criteriaChoices.get(criterion.getIdCrs()));
            }

            // Set comments
            if(commentFields.containsKey(shortid) && Objects.nonNull(criterion.getComment())) {
                System.out.println("Set comment: " + shortid + " value: " + criterion.getComment());
                commentFields.get(shortid).setText(criterion.getComment());
            }

            // Set values
            if(valueFields.containsKey(shortid) && Objects.nonNull(criterion.getValue())) {
                valueFields.get(shortid).setText(criterion.getValue());
            }
        }
        System.out.println();
    }

}
