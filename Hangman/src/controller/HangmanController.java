package controller;

import apptemplate.AppTemplate;
import data.GameData;
import gui.Workspace;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import propertymanager.PropertyManager;
import ui.AppMessageDialogSingleton;
import ui.OkayButtonDialog;
import ui.YesNoCancelDialogSingleton;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static settings.AppPropertyType.*;
import static settings.InitializationParameters.APP_WORKDIR_PATH;

/**
 * @author Ritwik Banerjee
 */
public class HangmanController implements FileController {

    public enum GameState {
        UNINITIALIZED,
        INITIALIZED_UNMODIFIED,
        INITIALIZED_MODIFIED,
        ENDED
    }

    private AppTemplate appTemplate; // shared reference to the application
    private GameData    gamedata;    // shared reference to the game being played, loaded or saved
    private GameState   gamestate;   // the state of the game being shown in the workspace
    private Text[]      progress;    // reference to the text area for the word
    private boolean     success;     // whether or not player was successful
    private int         discovered;  // the number of letters already discovered
    private Button      gameButton;  // shared reference to the "start game" button
    private Button      hintButton;  // shared reference to the "start game" button
    private Label       remains;// dynamically updated label that indicates the number of remaining guesses
    private Label       badGuessesLabel;
    private Path        workFile;
    private String      badGuesses = "";  //updates in method badGuessUpdateGraphics
    private HBox        littleBoxes;

    public HangmanController(AppTemplate appTemplate, Button gameButton) {
        this(appTemplate);
        this.gameButton = gameButton;
    }

    public GameData getGamedata(){
        return this.gamedata;
    }

    public HangmanController(AppTemplate appTemplate) {
        this.appTemplate = appTemplate;
        this.gamestate = GameState.UNINITIALIZED;
    }

    public void enableGameButton() {
        if (gameButton == null) {
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameButton = workspace.getStartGame();
            hintButton = workspace.getHintButton();
        }
        gameButton.setDisable(false);
    }


    public void disableGameButton() {
        if (gameButton == null) {
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameButton = workspace.getStartGame();
        }
        gameButton.setDisable(true);
    }

    public void enableHintButton() {
        if (hintButton == null) {
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            hintButton = workspace.getHintButton();
        }
        hintButton.setDisable(false);
    }
    public void disableHintButton() {
        if (hintButton == null) {
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            hintButton = workspace.getHintButton();
        }
        hintButton.setDisable(true);
    }

    public void setGameState(GameState gamestate) {
        this.gamestate = gamestate;
    }

    public GameState getGamestate() {
        return this.gamestate;
    }

    /**
     * In the homework code given to you, we had the line
     * gamedata = new GameData(appTemplate, true);
     * This meant that the 'gamedata' variable had access to the app, but the data component of the app was still
     * the empty game data! What we need is to change this so that our 'gamedata' refers to the data component of
     * the app, instead of being a new object of type GameData. There are several ways of doing this. One of which
     * is to write (and use) the GameData#init() method.
     */
    public void start() {
        gamedata = (GameData) appTemplate.getDataComponent();
        success = false;
        discovered = 0;
        badGuesses = "";
        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
        gamedata.init();
        setGameState(GameState.INITIALIZED_UNMODIFIED);

        gameWorkspace.addLittleBoxes(appTemplate);
        littleBoxes            = gameWorkspace.getLittleBoxes();
        for(int i = 0; i < gamedata.getTargetWord().length(); i++ ) {
            ((StackPane)littleBoxes.getChildren().get(i)).getChildren().get(1).setVisible(false);
        }
        HBox remainingGuessBox = gameWorkspace.getRemainingGuessBox();
        HBox guessedLetters    = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(1);
        HBox wronglyGuessedLetters    = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(2);
        remains = new Label(Integer.toString(GameData.TOTAL_NUMBER_OF_GUESSES_ALLOWED));
        badGuessesLabel = new Label("");
        remainingGuessBox.getChildren().addAll(new Label("Remaining Guesses: "), remains);
        wronglyGuessedLetters.getChildren().addAll(new Label("Incorrect guesses: "), badGuessesLabel );
        initWordGraphics(guessedLetters);
        play();
    }

    private void end() {
        appTemplate.getGUI().getPrimaryScene().setOnKeyTyped(null);
        gameButton.setDisable(false);
        hintButton.setDisable(true);
        setGameState(GameState.ENDED);
        appTemplate.getGUI().updateWorkspaceToolbar(gamestate.equals(GameState.INITIALIZED_MODIFIED));
        Platform.runLater(() -> {
            PropertyManager           manager    = PropertyManager.getManager();
            AppMessageDialogSingleton dialog     = AppMessageDialogSingleton.getSingleton();
            String                    endMessage = manager.getPropertyValue(success ? GAME_WON_MESSAGE : GAME_LOST_MESSAGE);
            if (!success)
                for (int i = 0; i < progress.length; i++) {
                    if ( !gamedata.getGoodGuesses().contains(gamedata.getTargetWord().charAt(i)) ) {
                        ((StackPane)littleBoxes.getChildren().get(i)).getChildren().get(1).setStyle("-fx-stroke: red;");
                        ((StackPane)littleBoxes.getChildren().get(i)).getChildren().get(1).setVisible(true);
                    }
                }
            if (dialog.isShowing())
                dialog.toFront();
            else {
                dialog.show(manager.getPropertyValue(GAME_OVER_TITLE), endMessage);
                Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
                littleBoxes = new HBox();
                //littleBoxes.setVisible(false);
                workspace.reinitialize();
            }

        });
    }

    private void initWordGraphics(HBox guessedLetters) {
        char[] targetword = gamedata.getTargetWord().toCharArray();
        progress = new Text[targetword.length];
        for (int i = 0; i < progress.length; i++) {
            progress[i] = new Text(Character.toString(targetword[i]));
            progress[i].setVisible(false);
        }
        guessedLetters.getChildren().addAll(progress);
    }

    public void play() {
        disableGameButton();
        if(gamedata.getTargetWord().length() >= 7){
            enableHintButton();
            hintButton.setText("Hint available!");
        }
        if(gamedata.getTargetWord().length() < 7){
            disableHintButton();
            hintButton.setText("No hint available");
        }
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                appTemplate.getGUI().updateWorkspaceToolbar(gamestate.equals(GameState.INITIALIZED_MODIFIED));
                boolean validInput = false;

                appTemplate.getGUI().getPrimaryScene().setOnKeyTyped((KeyEvent event) -> {
                    char guess = event.getCharacter().charAt(0);
                    //check if input is valid
                    if( (guess > 122) || (guess < 65) || ( (guess > 90) && (guess < 96) ) ){
                        //don't do anything, but prevents else if from running
                    }
                    else if (!alreadyGuessed(guess)) {
                        boolean goodguess = false;

                        for (int i = 0; i < progress.length; i++) {
                            if (gamedata.getTargetWord().charAt(i) == guess) {
                                //progress[i].setVisible(true);
                                ((StackPane)littleBoxes.getChildren().get(i)).getChildren().get(1).setVisible(true);
                                gamedata.addGoodGuess(guess);
                                goodguess = true;
                                discovered++;
                            }
                        }
                        if (!goodguess) {
                            gamedata.addBadGuess(guess);
                            badGuesses += guess;
                            badGuessesLabel.setText(badGuesses);
                            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
                            workspace.addBodyPart();

                        }
                        success = (discovered == progress.length);
                        remains.setText(Integer.toString(gamedata.getRemainingGuesses()));
                    }
                    setGameState(GameState.INITIALIZED_MODIFIED);
                });
                if (gamedata.getRemainingGuesses() <= 0 || success)
                    stop();
            }

            @Override
            public void stop() {
                super.stop();
                end();
            }
        };
        timer.start();
    }

    private void restoreGUI() {
        disableGameButton();
        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
        gameWorkspace.reinitialize();

        HBox remainingGuessBox = gameWorkspace.getRemainingGuessBox();
        gameWorkspace.addLittleBoxes(appTemplate);
        littleBoxes            = gameWorkspace.getLittleBoxes();
        for(int i = 0; i < gamedata.getTargetWord().length(); i++ ) {
            if(gamedata.getGoodGuesses().contains(gamedata.getTargetWord().charAt(i)))
                ((StackPane) littleBoxes.getChildren().get(i)).getChildren().get(1).setVisible(true);
            else
                ((StackPane) littleBoxes.getChildren().get(i)).getChildren().get(1).setVisible(false);
        }

        HBox guessedLetters    = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(1);
        HBox wronglyGuessedLetters    = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(2);
        remains = new Label(Integer.toString(GameData.TOTAL_NUMBER_OF_GUESSES_ALLOWED));
        badGuessesLabel = new Label("");
        remainingGuessBox.getChildren().addAll(new Label("Remaining Guesses: "), remains);
        wronglyGuessedLetters.getChildren().addAll(new Label("Incorrect guesses: "), badGuessesLabel );
        initWordGraphics(guessedLetters);

        Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
        workspace.initBodyParts();
        badGuesses = "";

        int resetManCounter = 10 - gamedata.getRemainingGuesses();
        Character[] badGuessArray = gamedata.getBadGuesses().toArray(new Character[gamedata.getBadGuesses().size()]);
        remains.setText(Integer.toString(gamedata.getRemainingGuesses()));
        for (int i = 0; i < resetManCounter; i++){
            badGuesses += badGuessArray[i];
            workspace.addBodyPart();
        }
        badGuessesLabel.setText(badGuesses);
        play();
    }

    private void restoreWordGraphics(HBox guessedLetters) {
        discovered = 0;
        char[] targetword = gamedata.getTargetWord().toCharArray();
        progress = new Text[targetword.length];
        for (int i = 0; i < progress.length; i++) {
            progress[i] = new Text(Character.toString(targetword[i]));
            progress[i].setVisible(gamedata.getGoodGuesses().contains(progress[i].getText().charAt(0)));
            if (progress[i].isVisible())
                discovered++;
        }
        guessedLetters.getChildren().addAll(progress);
    }

    private boolean alreadyGuessed(char c) {
        return gamedata.getGoodGuesses().contains(c) || gamedata.getBadGuesses().contains(c);
    }

    @Override
    public void handleNewRequest() {
        AppMessageDialogSingleton messageDialog   = AppMessageDialogSingleton.getSingleton();
        PropertyManager           propertyManager = PropertyManager.getManager();
        boolean                   makenew         = true;
        if (gamestate.equals(GameState.INITIALIZED_MODIFIED))
            try {
                makenew = promptToSave();
            } catch (IOException e) {
                messageDialog.show(propertyManager.getPropertyValue(NEW_ERROR_TITLE), propertyManager.getPropertyValue(NEW_ERROR_MESSAGE));
            }
        if (makenew) {
            appTemplate.getDataComponent().reset();                // reset the data (should be reflected in GUI)
            appTemplate.getWorkspaceComponent().reloadWorkspace(); // load data into workspace
            ensureActivatedWorkspace();                            // ensure workspace is activated
            workFile = null;                                       // new workspace has never been saved to a file
            ((Workspace) appTemplate.getWorkspaceComponent()).reinitialize();
            enableGameButton();
            disableHintButton();
        }
        if (gamestate.equals(GameState.ENDED)) {
            appTemplate.getGUI().updateWorkspaceToolbar(false);
            Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameWorkspace.reinitialize();
        }

    }

    @Override
    public void handleSaveRequest() throws IOException {
        PropertyManager propertyManager = PropertyManager.getManager();
        if (workFile == null) {
            FileChooser filechooser = new FileChooser();
            Path        appDirPath  = Paths.get(propertyManager.getPropertyValue(APP_TITLE)).toAbsolutePath();
            Path        targetPath  = appDirPath.resolve(APP_WORKDIR_PATH.getParameter());
            filechooser.setInitialDirectory(targetPath.toFile());
            filechooser.setTitle(propertyManager.getPropertyValue(SAVE_WORK_TITLE));
            String description = propertyManager.getPropertyValue(WORK_FILE_EXT_DESC);
            String extension   = propertyManager.getPropertyValue(WORK_FILE_EXT);
            ExtensionFilter extFilter = new ExtensionFilter(String.format("%s (*.%s)", description, extension),
                                                            String.format("*.%s", extension));
            filechooser.getExtensionFilters().add(extFilter);
            File selectedFile = filechooser.showSaveDialog(appTemplate.getGUI().getWindow());
            if (selectedFile != null)
                save(selectedFile.toPath());
        } else
            save(workFile);
    }

    @Override
    public void handleLoadRequest() throws IOException {
        boolean load = true;
        if (gamestate.equals(GameState.INITIALIZED_MODIFIED))
            load = promptToSave();
        if (load) {
            PropertyManager propertyManager = PropertyManager.getManager();
            FileChooser     filechooser     = new FileChooser();
            Path            appDirPath      = Paths.get(propertyManager.getPropertyValue(APP_TITLE)).toAbsolutePath();
            Path            targetPath      = appDirPath.resolve(APP_WORKDIR_PATH.getParameter());
            filechooser.setInitialDirectory(targetPath.toFile());
            filechooser.setTitle(propertyManager.getPropertyValue(LOAD_WORK_TITLE));
            String description = propertyManager.getPropertyValue(WORK_FILE_EXT_DESC);
            String extension   = propertyManager.getPropertyValue(WORK_FILE_EXT);
            ExtensionFilter extFilter = new ExtensionFilter(String.format("%s (*.%s)", description, extension),
                                                            String.format("*.%s", extension));
            filechooser.getExtensionFilters().add(extFilter);
            File selectedFile = filechooser.showOpenDialog(appTemplate.getGUI().getWindow());
            if (selectedFile != null && selectedFile.exists())
                load(selectedFile.toPath());
            restoreGUI(); // restores the GUI to reflect the state in which the loaded game was last saved
        }
    }

    @Override
    public void handleExitRequest() {
        try {
            boolean exit = true;
            if (gamestate.equals(GameState.INITIALIZED_MODIFIED))
                exit = promptToSave();
            if (exit)
                System.exit(0);
        } catch (IOException ioe) {
            AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
            PropertyManager           props  = PropertyManager.getManager();
            dialog.show(props.getPropertyValue(SAVE_ERROR_TITLE), props.getPropertyValue(SAVE_ERROR_MESSAGE));
        }
    }

    private void ensureActivatedWorkspace() {
        appTemplate.getWorkspaceComponent().activateWorkspace(appTemplate.getGUI().getAppPane());
    }

    private boolean promptToSave() throws IOException {
        PropertyManager            propertyManager   = PropertyManager.getManager();
        YesNoCancelDialogSingleton yesNoCancelDialog = YesNoCancelDialogSingleton.getSingleton();

        yesNoCancelDialog.show(propertyManager.getPropertyValue(SAVE_UNSAVED_WORK_TITLE),
                               propertyManager.getPropertyValue(SAVE_UNSAVED_WORK_MESSAGE));

        if (yesNoCancelDialog.getSelection().equals(YesNoCancelDialogSingleton.YES))
            handleSaveRequest();

        return !yesNoCancelDialog.getSelection().equals(YesNoCancelDialogSingleton.CANCEL);
    }

    /**
     * A helper method to save work. It saves the work, marks the current work file as saved, notifies the user, and
     * updates the appropriate controls in the user interface
     *
     * @param target The file to which the work will be saved.
     * @throws IOException
     */
    private void save(Path target) throws IOException {
        appTemplate.getFileComponent().saveData(appTemplate.getDataComponent(), target);
        workFile = target;
        setGameState(GameState.INITIALIZED_UNMODIFIED);
        AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
        PropertyManager           props  = PropertyManager.getManager();
        dialog.show(props.getPropertyValue(SAVE_COMPLETED_TITLE), props.getPropertyValue(SAVE_COMPLETED_MESSAGE));
    }

    /**
     * A helper method to load saved game data. It loads the game data, notified the user, and then updates the GUI to
     * reflect the correct state of the game.
     *
     * @param source The source data file from which the game is loaded.
     * @throws IOException
     */
    private void load(Path source) throws IOException {
        // load game data
        appTemplate.getFileComponent().loadData(appTemplate.getDataComponent(), source);

        // set the work file as the file from which the game was loaded
        workFile = source;

        // notify the user that load was successful
        AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
        PropertyManager           props  = PropertyManager.getManager();
        dialog.show(props.getPropertyValue(LOAD_COMPLETED_TITLE), props.getPropertyValue(LOAD_COMPLETED_MESSAGE));

        setGameState(GameState.INITIALIZED_UNMODIFIED);
        Workspace gameworkspace = (Workspace) appTemplate.getWorkspaceComponent();
        ensureActivatedWorkspace();
        gameworkspace.reinitialize();
        gamedata = (GameData) appTemplate.getDataComponent();
    }
    public void showHint(){
        char hintLetter = '\0';
        String targetWord = gamedata.getTargetWord();
        Set<Character> goodGuesses = gamedata.getGoodGuesses();
        Character[] goodGuessArray = goodGuesses.toArray(new Character[goodGuesses.size()]);
        for(int i = 0; i < targetWord.length(); i++){
            boolean guessed = false;
            for(int j = 0; j < goodGuessArray.length; j++ ){
                if(targetWord.charAt(i) == goodGuessArray[j]){
                    guessed = true;
                }
            }
            if(guessed == false){
                hintLetter = targetWord.charAt(i);
                i = targetWord.length();
            }
        }

        OkayButtonDialog dialog = OkayButtonDialog.getSingleton();
        dialog.init(appTemplate.getGUI().getWindow());
        dialog.show("Super secret hint", "Try a : " + hintLetter);

        dialog.toFront();
        disableHintButton();
    }
}
