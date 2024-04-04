package seedu.address.ui;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import seedu.address.model.applicant.Applicant;
import seedu.address.model.person.Person;

/**
 * Panel containing the list of persons.
 */
public class PersonListPanel extends UiPart<Region> {

    private static final String FXML = "PersonListPanel.fxml";

    private static final String UPDATE_PIC_BTN = "Update Picture";
    private static final String UPDATE_PIC_BTN_CLASS = "button";
    private static final String FILE_CHOOSER_TITLE = "Select Profile Image";
    private static final String FILE_UPLOAD_INFO = "Changes will reflect after the app is restarted.";
    private static final String FILE_UPLOAD_ERROR = "Unable to upload image.";

    private static final String INFO_DIALOG_TEXT = "Image Uploaded";
    private static final String ERROR_DIALOG_TEXT = "Image Upload Failed";

    private static final String ADDRESS_BOOK_DIR = "data/addressbook.json";
    private static final String DATA_PATH_NAME = "data";
    private static final String JSON_PERSON_OBJ_NAME = "persons";
    private static final String JSON_IMG_OBJ_NAME = "img";

    @FXML
    private ListView<Person> personListView;

    /**
     * Creates a {@code PersonListPanel} with the given {@code ObservableList}.
     */
    public PersonListPanel(javafx.collections.ObservableList<Person> personList) {
        super(FXML);
        personListView.setItems(personList);
        personListView.setCellFactory(listView -> new PersonListViewCell());
    }

    /**
     * Custom {@code ListCell} that displays the graphics of a {@code Person} using a {@code PersonCard}.
     */
    class PersonListViewCell extends ListCell<Person> {
        @Override
        protected void updateItem(Person person, boolean empty) {
            super.updateItem(person, empty);

            if (empty || person == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            Button updatePictureButton = new Button(UPDATE_PIC_BTN);
            updatePictureButton.getStyleClass().add(UPDATE_PIC_BTN_CLASS);
            updatePictureButton.setOnAction(event -> handleApplicantClick(event));
            updatePictureButton.setId(getIndex() + "");

            HBox buttonHBox = new HBox(updatePictureButton);
            buttonHBox.setAlignment(Pos.BOTTOM_RIGHT);
            buttonHBox.setPrefHeight(25.0);
            buttonHBox.setPrefWidth(130.0);
            buttonHBox.setPadding(new Insets(10, 10, 10, 10));

            VBox combinedVBox = new VBox();
            if (person instanceof Applicant) {
                combinedVBox.getChildren().add(new ApplicantCard((Applicant) person,
                        getIndex() + 1, person.getImg()).getRoot());
            } else {
                combinedVBox.getChildren().add(new PersonCard(person, getIndex() + 1).getRoot());
            }
            combinedVBox.getChildren().add(buttonHBox);

            setGraphic(combinedVBox);
        }
    }

    private void handleApplicantClick(ActionEvent event) {

        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter imageFilter = new FileChooser
                .ExtensionFilter("Image Files", "*.jpg", "*.png", "*.gif", "*.bmp");
        fileChooser.getExtensionFilters().add(imageFilter);
        fileChooser.setTitle(FILE_CHOOSER_TITLE);
        File file = fileChooser.showOpenDialog(null);

        Button clickedButton = (Button) event.getSource();
        int buttonId = Integer.parseInt(clickedButton.getId());

        if (file != null) {
            try {
                saveImgToJson(file, buttonId);
                showInfoDialog();
            } catch (Exception e) {
                showErrorDialog();
            }
        }
    }
    private void showInfoDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(INFO_DIALOG_TEXT);

        alert.setHeaderText(null);
        alert.setContentText(FILE_UPLOAD_INFO);

        alert.showAndWait();
    }

    private void showErrorDialog() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(ERROR_DIALOG_TEXT);

        alert.setHeaderText(null);
        alert.setContentText(FILE_UPLOAD_ERROR);

        alert.showAndWait();
    }

    private void saveImgToJson(File file, int buttonId) throws IOException, ParseException {

        Path sourcePath = file.toPath();
        Path destinationPath = Paths.get(DATA_PATH_NAME, file.getName());
        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        String imagePath = destinationPath.toString();

        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(ADDRESS_BOOK_DIR));
        JSONArray persons = (JSONArray) jsonObject.get(JSON_PERSON_OBJ_NAME);
        JSONObject person = (JSONObject) persons.get(buttonId);
        person.put(JSON_IMG_OBJ_NAME, imagePath);

        try (FileWriter fileWriter = new FileWriter(ADDRESS_BOOK_DIR)) {
            fileWriter.write(jsonObject.toJSONString());
        }
    }
}
