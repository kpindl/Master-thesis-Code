package variantdb.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import variantdb.models.Nomenclature;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class CNVNomenclatureController {

    @FXML
    private ResourceBundle resources;
    @FXML
    private URL location;
    @FXML
    private ChoiceBox<String> guidelineChb;
    @FXML
    private TextField nomenclatureTxt;
    @FXML
    private Button cancelBtn;
    @FXML
    private Button okBtn;

    private Nomenclature nomenclature;


    // Methods
    public static Optional<Nomenclature> showAndWait(Window window) {
        CNVNomenclatureController controller = new CNVNomenclatureController();
        FXMLLoader loader = new FXMLLoader(CNVNomenclatureController.class.getResource("/fxml/variantdb/CNV/CNVNomenclature.fxml"));
        loader.setController(controller);
        try {
            Parent root = loader.load();
            Scene scene = new Scene(root);
            Stage secondaryStage = new Stage();
            secondaryStage.initOwner(window);
            secondaryStage.setTitle("CNV Nomenclature");
            secondaryStage.setScene(scene);
            secondaryStage.showAndWait();
            return Optional.ofNullable(controller.nomenclature);

        } catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText("Konnte Nomenklaturtool nicht laden");
            a.initOwner(window);
            a.showAndWait();
        }

        return Optional.empty();
    }

    @FXML
    void handleCancel(ActionEvent event) {
        Stage window = (Stage) cancelBtn.getScene().getWindow();
        window.close();
    }

    @FXML
    void handleOK(ActionEvent event) {
        if (guidelineChb.getValue() == null){
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText("Keine Nomenklatur Guideline ausgewählt!");
            alert.showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(okBtn.getScene().getWindow());
        alert.setHeaderText("Sind Sie sicher?");
        alert.setTitle("CNV klassifizieren");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.get() == ButtonType.OK) {
            nomenclature = new Nomenclature(guidelineChb.getValue(), nomenclatureTxt.getText());
            Stage window = (Stage) okBtn.getScene().getWindow();
            window.close();
        }
    }
}

