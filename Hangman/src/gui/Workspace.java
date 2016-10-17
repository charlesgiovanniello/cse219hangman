package gui;

import apptemplate.AppTemplate;
import components.AppWorkspaceComponent;
import controller.HangmanController;
import data.GameData;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import propertymanager.PropertyManager;
import sun.management.Agent;
import ui.AppGUI;

import java.io.IOException;
import java.util.ArrayList;

import static hangman.HangmanProperties.*;

/**
 * This class serves as the GUI component for the Hangman game.
 *
 * @author Ritwik Banerjee
 */
public class Workspace extends AppWorkspaceComponent {

    AppTemplate app; // the actual application
    AppGUI      gui; // the GUI inside which the application sits

    Label             guiHeadingLabel;   // workspace (GUI) heading label
    HBox              headPane;          // conatainer to display the heading
    HBox              bodyPane;          // container for the main game displays
    ToolBar           footToolbar;       // toolbar for game buttons
    BorderPane        figurePane;        // container to display the namesake graphic of the (potentially) hanging person
    VBox              gameTextsPane;     // container to display the text-related parts of the game
    HBox              guessedLetters;    // text area displaying all the letters guessed so far
    HBox              wronglyGuessedLetters;
    HBox              remainingGuessBox; // container to display the number of remaining guesses
    HBox              littleBoxes;
    Button            startGame;         // the button to start playing a game of Hangman
    Button            hintButton;         // the button to start playing a game of Hangman
    HangmanController controller;
    Canvas            canvas;
    GraphicsContext   gc;
    GameData          gamedata;

    boolean           head;
    boolean           lEye;
    boolean           rEye;
    boolean           hair;
    boolean           lArm;
    boolean           rArm;
    boolean           body;
    boolean           lLeg;
    boolean           rLeg;
    boolean           mouth;


    /**
     * Constructor for initializing the workspace, note that this constructor
     * will fully setup the workspace user interface for use.
     *
     * @param initApp The application this workspace is part of.
     * @throws IOException Thrown should there be an error loading application
     *                     data for setting up the user interface.
     */
    public Workspace(AppTemplate initApp) throws IOException {
        app = initApp;
        gui = app.getGUI();
        controller = (HangmanController) gui.getFileController();    //new HangmanController(app, startGame); <-- THIS WAS A MAJOR BUG!??
        layoutGUI();     // initialize all the workspace (GUI) components including the containers and their layout
        setupHandlers(); // ... and set up event handling
    }

    private void layoutGUI() {
        PropertyManager propertyManager = PropertyManager.getManager();


        guiHeadingLabel = new Label(propertyManager.getPropertyValue(WORKSPACE_HEADING_LABEL));

        headPane = new HBox();
        headPane.getChildren().add(guiHeadingLabel);
        headPane.setAlignment(Pos.CENTER);

        figurePane = new BorderPane();
        guessedLetters = new HBox();
        guessedLetters.setStyle("-fx-background-color: transparent;");

        wronglyGuessedLetters = new HBox();
        wronglyGuessedLetters.setStyle("-fx-background-color: transparent;");

        remainingGuessBox = new HBox();
        //********
        //add a little box for each letter




        //*********

        gameTextsPane = new VBox();
        //canvas
        canvas = new Canvas();
        canvas.setStyle("-fx-background-color: cyan");
        gc = canvas.getGraphicsContext2D();
        Group root = new Group();
        root.getChildren().add(canvas);
        canvas.setWidth(gui.getPrimaryScene().getWidth());
        canvas.setHeight(gui.getPrimaryScene().getHeight() / 1.6);




        //TEXT PANES
        gameTextsPane.getChildren().setAll(remainingGuessBox, guessedLetters, wronglyGuessedLetters);
        bodyPane = new HBox();
        bodyPane.getChildren().addAll(figurePane, gameTextsPane);
        //BUTTONS
        startGame = new Button("Start Playing");
        HBox blankBoxLeft  = new HBox();
        HBox blankBoxRight = new HBox();
        HBox.setHgrow(blankBoxLeft, Priority.ALWAYS);
        HBox.setHgrow(blankBoxRight, Priority.ALWAYS);

        //Create hint button
        hintButton = new Button("Hint");
        footToolbar = new ToolBar(blankBoxLeft, startGame,blankBoxRight, hintButton);


        //gamedata = (GameData) app.getDataComponent();
        //int numBoxes = gamedata.getTargetWord().length();

        //Add everything to workspace ***************
        workspace = new VBox();
        workspace.getChildren().addAll(headPane, bodyPane, canvas, footToolbar);//add littleBox
    }

    public void addLittleBoxes(AppTemplate app){
        littleBoxes = new HBox();
        gamedata = (GameData) app.getDataComponent();
        int numBoxes = gamedata.getTargetWord().length();

        for(int i = 0; i < numBoxes; i++){
            StackPane boxHolder = new StackPane();
            littleBoxes.getChildren().add(boxHolder);
            WordBox box = new WordBox();
            Text letter = new Text();
            letter.setText(Character.toString(gamedata.getTargetWord().charAt(i)));
            boxHolder.getChildren().addAll(box,letter);
        }


        workspace.getChildren().add(1,littleBoxes);//add littleBoxes

    }
    public void addLittleBoxesWrong(AppTemplate app){
        littleBoxes = new HBox();
        gamedata = (GameData) app.getDataComponent();
        int numBoxes = gamedata.getTargetWord().length();

        for(int i = 0; i < numBoxes; i++){
            StackPane boxHolder = new StackPane();
            littleBoxes.getChildren().add(boxHolder);
            WordBox box = new WordBox();
            Text letter = new Text();
            letter.setText(Character.toString(gamedata.getTargetWord().charAt(i)));
            boxHolder.getChildren().addAll(box,letter);
        }


        workspace.getChildren().add(1,littleBoxes);//add littleBoxes

    }


    private void setupHandlers() {
        startGame.setOnMouseClicked(e -> controller.start());
        hintButton.setOnMouseClicked(e -> controller.showHint());
    }

    /**
     * This function specifies the CSS for all the UI components known at the time the workspace is initially
     * constructed. Components added and/or removed dynamically as the application runs need to be set up separately.
     */
    @Override
    public void initStyle() {
        PropertyManager propertyManager = PropertyManager.getManager();

        gui.getAppPane().setId(propertyManager.getPropertyValue(ROOT_BORDERPANE_ID));
        gui.getToolbarPane().getStyleClass().setAll(propertyManager.getPropertyValue(SEGMENTED_BUTTON_BAR));
        gui.getToolbarPane().setId(propertyManager.getPropertyValue(TOP_TOOLBAR_ID));

        ObservableList<Node> toolbarChildren = gui.getToolbarPane().getChildren();
        toolbarChildren.get(0).getStyleClass().add(propertyManager.getPropertyValue(FIRST_TOOLBAR_BUTTON));
        toolbarChildren.get(toolbarChildren.size() - 1).getStyleClass().add(propertyManager.getPropertyValue(LAST_TOOLBAR_BUTTON));

        workspace.getStyleClass().add(CLASS_BORDERED_PANE);
        guiHeadingLabel.getStyleClass().setAll(propertyManager.getPropertyValue(HEADING_LABEL));

    }
    public void initBodyParts(){
        head = false;lEye = false;rEye = false;hair = false;
        lArm = false;rArm = false;body = false;lLeg = false;
        rLeg = false;mouth = false;
    }
    public void addBodyPart(){

        if(head == false) {
            //HEAD
            gc.setFill(Paint.valueOf("red"));
            gc.fillRect(300, 50, 115, 80);
            gc.beginPath();
            gc.setStroke(Paint.valueOf("blue"));
            gc.setLineWidth(1);
            gc.rect(300, 50, 115, 80);
            gc.stroke();
            head = true;
            return;
        }

        if(hair == false) {
            //HAIR
            gc.setFill(Paint.valueOf("black"));
            gc.fillRect(300 + 5, 30, 3, 20);
            gc.fillRect(300 + 10, 30, 3, 20);
            gc.fillRect(300 + 15, 30, 3, 20);
            gc.fillRect(300 + 20, 30, 3, 20);
            gc.fillRect(300 + 25, 30, 3, 20);
            gc.fillRect(300 + 30, 30, 3, 20);
            gc.fillRect(300 + 35, 30, 3, 20);
            gc.fillRect(300 + 40, 30, 3, 20);
            gc.fillRect(300 + 45, 30, 3, 20);
            gc.fillRect(300 + 50, 30, 3, 20);
            gc.fillRect(300 + 55, 30, 3, 20);
            gc.fillRect(300 + 60, 30, 3, 20);
            gc.fillRect(300 + 65, 30, 3, 20);
            gc.fillRect(300 + 70, 30, 3, 20);
            gc.fillRect(300 + 75, 30, 3, 20);
            gc.fillRect(300 + 80, 30, 3, 20);
            gc.fillRect(300 + 85, 30, 3, 20);
            gc.fillRect(300 + 90, 30, 3, 20);
            gc.fillRect(300 + 95, 30, 3, 20);
            gc.fillRect(300 + 100, 30, 3, 20);
            gc.fillRect(300 + 105, 30, 3, 20);
            gc.fillRect(300 + 110, 30, 3, 20);
            gc.beginPath();
            gc.stroke();
            hair = true;
            return;
        }
        if(lEye == false) {
            //LEFT EYE
            gc.setFill(Paint.valueOf("blue"));
            gc.fillRect(300 + 15, 50 + 30, 25, 25); //left eye
            gc.setFill(Paint.valueOf("white"));
            gc.fillRect(300 + 23, 50 + 38, 10, 10); //left eye
            gc.beginPath();
            gc.stroke();
            lEye = true;
            return;
        }
        if(rEye == false) {
            //RIGHT EYE
            gc.setFill(Paint.valueOf("blue"));
            gc.fillRect(300 + 75, 50 + 30, 25, 25); //right eye
            gc.setFill(Paint.valueOf("white"));
            gc.fillRect(300 + 83, 50 + 38, 10, 10); //right eye
            gc.beginPath();
            gc.stroke();
            rEye = true;
            return;
        }
        if(mouth == false) {
            //MOUTH
            gc.setFill(Paint.valueOf("black"));
            gc.fillRect(300 + 12, 50 + 60, 85, 3);
            gc.beginPath();
            gc.stroke();
            mouth = true;
            return;
        }
        if(body == false) {
            //BODY
            gc.setFill(Paint.valueOf("green"));
            gc.fillRect(312, 130, 90, 80);
            gc.beginPath();
            gc.setStroke(Paint.valueOf("blue"));
            gc.stroke();
            body = true;
            return;
        }
        if(lLeg == false) {
            //LEFT LEG
            gc.setFill(Paint.valueOf("black"));
            gc.fillRect(300 + 23, 130 + 80, 7, 90);
            gc.beginPath();
            gc.stroke();
            lLeg = true;
            return;
        }

        if(rLeg == false) {
            //RIGHT LEG
            gc.setFill(Paint.valueOf("black"));
            gc.fillRect(300 + 83, 130 + 80, 7, 90);
            gc.beginPath();
            gc.stroke();
            rLeg = true;
            return;
        }
        if (lArm == false) {
            //LEFT ARM
            gc.setFill(Paint.valueOf("black"));
            gc.fillRect(210, 50 + 44, 90, 7);
            gc.beginPath();
            gc.stroke();
            lArm = true;
            return;
        }
        if(rArm == false) {
            //RIGHT ARM
            gc.setFill(Paint.valueOf("black"));
            gc.fillRect(300 + 115, 50 + 44, 90, 7);
            gc.beginPath();
            gc.stroke();
            rArm = true;
            return;
        }
    }

    /** This function reloads the entire workspace */
    @Override
    public void reloadWorkspace() {
        /* does nothing; use reinitialize() instead */
    }


    public VBox getGameTextsPane() {
        return gameTextsPane;
    }

    public HBox getLittleBoxes() {return littleBoxes;}

    public HBox getRemainingGuessBox() {
        return remainingGuessBox;
    }

    public Button getStartGame() {
        return startGame;
    }

    public Button getHintButton() {
        return hintButton;
    }

    public void reinitialize() {
        guessedLetters = new HBox();
        guessedLetters.setStyle("-fx-background-color: transparent;");
        wronglyGuessedLetters = new HBox();
        wronglyGuessedLetters.setStyle("-fx-background-color: transparent;");
        workspace.getChildren().remove(littleBoxes);
        remainingGuessBox = new HBox();
        gameTextsPane = new VBox();
        gameTextsPane.getChildren().setAll(remainingGuessBox, guessedLetters, wronglyGuessedLetters);
        bodyPane.getChildren().setAll(figurePane, gameTextsPane);
        gc.clearRect(0, 0, gc.getCanvas().getWidth(),gc.getCanvas().getHeight());
        initBodyParts();
    }
}
class WordBox extends Rectangle{
    public WordBox(){
        setWidth(20);
        setHeight(20);

        setFill(Color.WHITE.deriveColor(0, 1.2, 1, 0.6));
        setStroke(Color.WHITE);
    }
}
