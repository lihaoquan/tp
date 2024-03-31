package seedu.address.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.Logic;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;

/**
 * The Main Window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 */
public class MainWindow extends UiPart<Stage> {
    public static final int TOTAL_NUMBER_OF_STAGES = 4;
    private static final String FXML = "MainWindow.fxml";

    private final Logger logger = LogsCenter.getLogger(getClass());

    private Stage primaryStage;
    private Logic logic;

    // Independent Ui parts residing in this Ui container
    private PersonListPanel personListPanel;
    private ResultDisplay resultDisplay;
    private HelpWindow helpWindow;

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private StackPane personListPanelPlaceholder;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;
    @FXML
    private Label initialAssessmentCountLabel;
    @FXML
    private Label technicalAssessmentCountLabel;
    @FXML
    private Label interviewCountLabel;
    @FXML
    private Label decisionAndOfferCountLabel;
    @FXML
    private RadioButton initialAssessmentRadioButton;

    @FXML
    private RadioButton technicalAssessmentRadioButton;

    @FXML
    private RadioButton interviewRadioButton;

    @FXML
    private RadioButton decisionOfferRadioButton;
    private IntegerProperty initialAssessmentCount = new SimpleIntegerProperty(0);
    private IntegerProperty technicalAssessmentCount = new SimpleIntegerProperty(0);
    private IntegerProperty interviewCount = new SimpleIntegerProperty(0);
    private IntegerProperty decisionAndOfferCount = new SimpleIntegerProperty(0);
    /**
     * Creates a {@code MainWindow} with the given {@code Stage} and {@code Logic}.
     */
    public MainWindow(Stage primaryStage, Logic logic) {
        super(FXML, primaryStage);

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = logic;

        // Configure the UI
        setWindowDefaultSize(logic.getGuiSettings());

        setAccelerators();

        helpWindow = new HelpWindow();
        initializeBindings();
    }

    public Stage getPrimaryStage() {
        initializeBindings();
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
    }

    /**
     * Sets the accelerator of a MenuItem.
     * @param keyCombination the KeyCombination value of the accelerator
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in later version of SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField, and
         * ResultDisplay contains a TextArea, thus some accelerators (e.g F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add following event filter to capture such key events and open
         * help window purposely so to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    void fillInnerParts() {
        personListPanel = new PersonListPanel(logic.getFilteredPersonList());
        personListPanelPlaceholder.getChildren().add(personListPanel.getRoot());

        resultDisplay = new ResultDisplay();
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());

        StatusBarFooter statusBarFooter = new StatusBarFooter(logic.getAddressBookFilePath());
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        CommandBox commandBox = new CommandBox(this::executeCommand);
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());
        logic.initialize();
        initializeBindings();
        updateOverviewCount();
    }

    /**
     * Sets the default size based on {@code guiSettings}.
     */
    private void setWindowDefaultSize(GuiSettings guiSettings) {
        primaryStage.setHeight(guiSettings.getWindowHeight());
        primaryStage.setWidth(guiSettings.getWindowWidth());
        if (guiSettings.getWindowCoordinates() != null) {
            primaryStage.setX(guiSettings.getWindowCoordinates().getX());
            primaryStage.setY(guiSettings.getWindowCoordinates().getY());
        }
    }

    /**
     * Opens the help window or focuses on it if it's already opened.
     */
    @FXML
    public void handleHelp() {
        if (!helpWindow.isShowing()) {
            helpWindow.show();
        } else {
            helpWindow.focus();
        }
    }
    @FXML
    private void handleFilter() {
        List<String> selectedStages = new ArrayList<>();
        if (initialAssessmentRadioButton.isSelected()) {
            selectedStages.add("initial_application");
        }
        if (technicalAssessmentRadioButton.isSelected()) {
            selectedStages.add("Technical Assessment");
        }
        if (interviewRadioButton.isSelected()) {
            selectedStages.add("Interview");
        }
        if (decisionOfferRadioButton.isSelected()) {
            selectedStages.add("final_stage");
        }

        Consumer<Void> filterCompleteCallback = unused -> {
            updateOverviewCount();
            if (selectedStages.isEmpty()) {
                resultDisplay.setFeedbackToUser("No stage selected, showing applicants in all stages");
            } else {
                resultDisplay.setFeedbackToUser("Showing applicants that are in the selected stages");
            }
        };

        // Filter persons by selected stages and update counts after filtering
        logic.filterPersonsByButton(selectedStages, filterCompleteCallback);
    }
    public void initializeBindings() {
        // Retrieve count properties from the logic
        IntegerProperty initialAssessmentCountProperty = logic.initialAssessmentCountProperty();
        IntegerProperty technicalAssessmentCountProperty = logic.technicalAssessmentCountProperty();
        IntegerProperty interviewCountProperty = logic.interviewCountProperty();
        IntegerProperty decisionAndOfferCountProperty = logic.decisionAndOfferCountProperty();

        // Bind count properties to corresponding UI labels
        initialAssessmentCountLabel.textProperty().bind(Bindings.concat("Initial Assessment (", initialAssessmentCountProperty, ")"));
        technicalAssessmentCountLabel.textProperty().bind(Bindings.concat("Technical Assessment (", technicalAssessmentCountProperty, ")"));
        interviewCountLabel.textProperty().bind(Bindings.concat("Interview (", interviewCountProperty, ")"));
        decisionAndOfferCountLabel.textProperty().bind(Bindings.concat("Decision & Offer (", decisionAndOfferCountProperty, ")"));
    }


//    @FXML
//    public void updateOverviewCount() {
//        initialAssessmentCount.set(logic.updateCount("initial_application"));
//        technicalAssessmentCount.set(logic.updateCount("Technical Assessment"));
//        interviewCount.set(logic.updateCount("Interview"));
//        decisionAndOfferCount.set(logic.updateCount("final_stage"));
//        initialAssessmentCountLabel.textProperty().bind(Bindings.concat("Initial Assessment (", initialAssessmentCount, ")"));
//        technicalAssessmentCountLabel.textProperty().bind(Bindings.concat("Technical Assessment (", technicalAssessmentCount, ")"));
//        interviewCountLabel.textProperty().bind(Bindings.concat("Interview (", interviewCount, ")"));
//        decisionAndOfferCountLabel.textProperty().bind(Bindings.concat("Decision & Offer (", decisionAndOfferCount, ")"));
//    }

    public void updateOverviewCount() {
        initialAssessmentCountLabel.textProperty().bind(Bindings.concat("Initial Assessment (", logic.initialAssessmentCountProperty(), ")"));
        technicalAssessmentCountLabel.textProperty().bind(Bindings.concat("Technical Assessment (", logic.technicalAssessmentCountProperty(), ")"));
        interviewCountLabel.textProperty().bind(Bindings.concat("Interview (", logic.interviewCountProperty(), ")"));
        decisionAndOfferCountLabel.textProperty().bind(Bindings.concat("Decision & Offer (", logic.decisionAndOfferCountProperty(), ")"));
    }

    public void deselectAllButtons() {
        initialAssessmentRadioButton.setSelected(false);
        technicalAssessmentRadioButton.setSelected(false);
        interviewRadioButton.setSelected(false);
        decisionOfferRadioButton.setSelected(false);
    }


    void show() {
        primaryStage.show();
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
        GuiSettings guiSettings = new GuiSettings(primaryStage.getWidth(), primaryStage.getHeight(),
                (int) primaryStage.getX(), (int) primaryStage.getY());
        logic.setGuiSettings(guiSettings);
        helpWindow.hide();
        primaryStage.hide();
    }

    public PersonListPanel getPersonListPanel() {
        updateOverviewCount();
        return personListPanel;
    }

    /**
     * Executes the command and returns the result.
     *
     * @see seedu.address.logic.Logic#execute(String)
     */
    private CommandResult executeCommand(String commandText) throws CommandException, ParseException {
        try {
            CommandResult commandResult = logic.execute(commandText);
            logger.info("Result: " + commandResult.getFeedbackToUser());
            resultDisplay.setFeedbackToUser(commandResult.getFeedbackToUser());

            if (commandResult.isShowHelp()) {
                handleHelp();
            }

            if (commandResult.isExit()) {
                handleExit();
            }

            return commandResult;
        } catch (CommandException | ParseException e) {
            logger.info("An error occurred while executing command: " + commandText);
            resultDisplay.setFeedbackToUser(e.getMessage());
            throw e;
        }
    }
}
