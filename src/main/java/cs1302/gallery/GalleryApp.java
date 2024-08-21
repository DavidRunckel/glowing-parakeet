package cs1302.gallery;

import cs1302.gallery.ItunesResponse;
import cs1302.gallery.ItunesResult;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.ArrayList;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpResponse.BodyHandlers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.control.Alert.AlertType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Represents an iTunes Gallery App.
 */
public class GalleryApp extends Application {

    /** HTTP client. */
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)           // uses HTTP protocol version 2 where possible
        .followRedirects(HttpClient.Redirect.NORMAL)  // always redirects, except from HTTPS to HTTP
        .build();                                     // builds and returns a HttpClient object

    /** Google {@code Gson} object for parsing JSON-formatted strings. */
    public static Gson GSON = new GsonBuilder()
        .setPrettyPrinting()                          // enable nice output when printing
        .create();                                    // builds and returns a Gson object

    private Stage stage;
    private Scene scene;
    private VBox root;

    // Instance Variables for HBox searchBar
    private HBox searchBar;
    private Button playPause;
    private Timeline timeline;
    private KeyFrame keyFrame;
    private Label search;
    private TextField termField;
    private ComboBox<String> dropdown;
    private ObservableList<String> mediaType;
    private Button getImages;
    private String uri;
    private List<String> urls;
    private List<Image> images;

    // Instance Variable for Label messageBar
    private Label messageBar;

    // Instance Variables for TilePane mainContent
    private TilePane mainContent;
    private List<ImageView> imageViews;
    private Image defaultImage;

    // Instance Variables for HBox statusBar
    private HBox statusBar;
    private ProgressBar progressBar;
    private Label iTunesSearchAPI;

    // Constants
    private static final String ITUNES_SEARCH_API = "https://itunes.apple.com/search?";
    private static final String DEFAULT_IMAGE = "file:resources/default.png";

    /**
     * Constructs a {@code GalleryApp} object}. The ordering of the code mirrors the groupings of
     * the instance variables above.
     */
    public GalleryApp() {
        this.stage = null;
        this.scene = null;
        this.root = new VBox();

        searchBar = new HBox(3);
        playPause = new Button("Play");
        timeline = new Timeline();
        search = new Label("Search:");
        termField = new TextField("daft punk");
        mediaType = FXCollections.observableArrayList("movie", "podcast", "music", "musicVideo",
        "audiobook", "shortFilm", "tvShow", "software", "ebook", "all");
        dropdown = new ComboBox<>(mediaType);
        getImages = new Button("Get Images");
        urls = new ArrayList<>();
        images = new ArrayList<>();

        messageBar = new Label("Type in a term, select a media type, then click the button.");

        mainContent = new TilePane();
        imageViews = new ArrayList<>();
        defaultImage = new Image(DEFAULT_IMAGE, 100, 100, false, false);

        statusBar = new HBox(3);
        progressBar = new ProgressBar(0.0);
        iTunesSearchAPI = new Label("Images provided by iTunes Search API.");
    } // GalleryApp

    /** {@inheritDoc} */
    @Override
    public void init() {
        System.out.println("init() called");
        root.getChildren().addAll(searchBar, messageBar, mainContent, statusBar);
        searchBar.getChildren().addAll(playPause, search, termField, dropdown, getImages);
        statusBar.getChildren().addAll(progressBar, iTunesSearchAPI);

        searchBar.setAlignment(Pos.CENTER);
        playPause.setDisable(true);
        timeline.setCycleCount(Timeline.INDEFINITE);
        dropdown.setValue("music");
        // runNow() is called to create the thread for the getImagesButton()
        getImages.setOnAction(event -> {
            runNow(() -> getImagesButton());
        });

        // playPause Button
        EventHandler<ActionEvent> handler = event -> randomReplacement();
        keyFrame = new KeyFrame(Duration.seconds(2), handler);
        timeline.getKeyFrames().add(keyFrame);
        playPause.setOnAction(event -> playPauseButton());

        messageBar.setMinHeight(24);
        messageBar.setFont(new Font(9));

        // places the ImageViews w/ the defaultImage in each
        mainContent.setPrefColumns(5);
        for (int i = 0; i < 20; i++) {
            imageViews.add(new ImageView());
            imageViews.get(i).setImage(defaultImage);
            mainContent.getChildren().add(imageViews.get(i));
        } // for

        statusBar.setMinHeight(24);
        statusBar.setAlignment(Pos.CENTER);
        progressBar.setMinWidth(240);
        iTunesSearchAPI.setAlignment(Pos.CENTER_RIGHT);
    } // init

    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.scene = new Scene(this.root);
        this.stage.setOnCloseRequest(event -> Platform.exit());
        this.stage.setTitle("GalleryApp!");
        this.stage.setScene(this.scene);
        this.stage.sizeToScene();
        this.stage.show();
        Platform.runLater(() -> this.stage.setResizable(false));
    } // start

    /** {@inheritDoc} */
    @Override
    public void stop() {
        System.out.println("stop() called");
    } // stop

    /**
     * playPauseButton(). randomly swaps an image in the display with another in the list
     * not already displayed.
     */
    public void playPauseButton() {
        if (playPause.getText().equals("Play")) {
            playPause.setText("Pause");
            timeline.play();
        } else {
            playPause.setText("Play");
            timeline.stop();
        } // if-else
    } // playPauseButton

    /**
     * randomReplacement(). method that swaps images in the image view randomly
     */
    public void randomReplacement() {
        boolean mainContentCopy = false;
        // the 2 Math.random() functions are used to get a random image for replacement
        // and a place to put that image based on the int random variable to dictate
        // the imageview.
        int random = (int) (Math.random() * 20.0);
        Image replacement = images.get((int) (Math.random() * (images.size())));
        // the loop checks if the replacement image is found in the current display and
        // if it is will set mainContentCopy to true which will begin the while loop.
        for (int i = 0; i < 20; i++) {
            if (imageViews.get(i).getImage().getUrl().equals(replacement.getUrl())) {
                mainContentCopy = true;
            } else {
                mainContentCopy = false;
            } // if-else
        } // for

        // the while loop does the same as the for loop above except it will repeat
        // getting a new image replacement until mainContentCopy is false.
        while (mainContentCopy == true) {
            replacement = images.get((int) (Math.random() * (images.size())));
            for (int i = 0; i < 20; i++) {
                if (imageViews.get(i).getImage().getUrl().equals(replacement.getUrl())) {
                    mainContentCopy = true;
                } else {
                    mainContentCopy = false;
                } // if-else
            } // for
        } // while

        // this is where the image is placed in the imageview
        imageViews.get(random).setImage(replacement);
    } // randomReplacement

    /**
     * getImagesButton(). Method called each time the Get Images Button is pressed.
     */
    public void getImagesButton() {
        try {
            // getImagesButtonStartSettings() is a method called to reset the display
            getImagesButtonStartSettings();
            uri = iTunesQuery(termField.getText(), dropdown.getValue());
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri)).build();
            HttpResponse<String> response = HTTP_CLIENT
                .send(request, BodyHandlers.ofString());
            // exception thrown when the response encounters an error
            if (response.statusCode() != 200) {
                errorChanges();
                throw new IOException(response.toString());
            } // if
            String jsonString = response.body();
            ItunesResponse itunesResponse = GSON.fromJson(jsonString, ItunesResponse.class);
            urlCreator(itunesResponse);
            // error thrown when there are less than 21 unique urls
            if (urls.size() < 21) {
                errorChanges();
                throw new IllegalArgumentException("Exception:java.lang.IllegalArgumentException:"
                + urls.size() + " distinct results found, but 21 or more are needed.");
            } // if
            // these methods change the urls into images and then place them into the display
            imageCreator();
            imagePlacer();
        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            // alertError() is called when one of the exceptions is thrown
            Runnable alertBox = () -> {
                alertError(e);
            };
            Platform.runLater(alertBox);
        } // try-catch
    } // getImagesButton

    /**
     * getImagesButtonStartSettings(). Called when the getImagesButton() method is ran
     */
    public void getImagesButtonStartSettings() {
        playPause.setDisable(true);
        Runnable textChangePlay = () -> {
            playPause.setText("Play");
        };
        Platform.runLater(textChangePlay);
        timeline.stop();
        getImages.setDisable(true);
        progressBar.setProgress(0.0);
        Runnable textChange1 = () -> {
            messageBar.setText("Getting images...");
        };
        Platform.runLater(textChange1);
    } // getImagesButtonStartSettings

    /**
     * iTunesQuery(). constructs a uri based on the information from the textfield and combobox
     * Credit to Example3.java in cs1302-web
     * @param t term in the textfield.
     * @param m media type in the combobox.
     * @return uri.
     */
    public String iTunesQuery(String t, String m) {
        String term = URLEncoder.encode(t, StandardCharsets.UTF_8);
        String media = URLEncoder.encode(m, StandardCharsets.UTF_8);
        String limit = URLEncoder.encode("200", StandardCharsets.UTF_8);
        String query = String.format("term=%s&media=%s&limit=%s", term, media, limit);
        return ITUNES_SEARCH_API + query;
    } // iTunesQuery

    /**
     * urlCreator(). called to add the artworkUrl100's from the itunesResponse to the urls list.
     * @param itunesResponse the json containing all the info pulled from iTunes.
     */
    public void urlCreator(ItunesResponse itunesResponse) {
        urls = new ArrayList<>();
        for (int i = 0; i < itunesResponse.resultCount; i++) {
            if (!urls.contains(itunesResponse.results[i].artworkUrl100)) {
                urls.add(itunesResponse.results[i].artworkUrl100);
            } // if
        } // for
    } // urlCreator

    /**
     * imageCreator(). this method is where the loading takes place due to the images
     * being converted from strings to image objects. also the progress bar is gradually
     * updated here.
     */
    public void imageCreator() {
        images = new ArrayList<>();
        double x = 0.0;
        for (int i = 0; i < urls.size(); i++) {
            images.add(new Image(urls.get(i), 100.0, 100.0, false, false));
            x = x + .05;
            progressBar.setProgress(x);
        } // for
        playPause.setDisable(false);
        getImages.setDisable(false);
        Runnable textChange2 = () -> {
            messageBar.setText(uri);
        };
        Platform.runLater(textChange2);
    } // imageCreator

    /**
     * imagePlacer(). this method places the first 20 images into the imageviews.
     */
    public void imagePlacer() {
        for (int i = 0; i < 20; i++) {
            imageViews.get(i).setImage(images.get(i));
        } // for
    } // imagePlacer

    /**
     * errorChanges(). this method handles all the display changes when an error is thrown.
     */
    public void errorChanges() {
        Runnable textChange3 = () -> {
            messageBar.setText("Last attempt to get images failed...");
        };
        Platform.runLater(textChange3);
        getImages.setDisable(false);
        // if the default image is still displayed in the first imageview, then the play/pause
        // button stays disabled and the progressbar doesn't change to 100 as shown by the mockup
        // slideshow.
        if (imageViews.get(0).getImage().getUrl().equals(DEFAULT_IMAGE)) {
            playPause.setDisable(true);
            progressBar.setProgress(0.0);
        } else {
            playPause.setDisable(false);
            progressBar.setProgress(1.0);
        } // if-else
    } // errorChanges

    /**
     * runNow(). creates the thread.
     * Credit to the JavaFX Threads video from Dr. Cotterell
     * @param target is the object that is ran.
     */
    public void runNow(Runnable target) {
        Thread thread = new Thread(target);
        thread.setDaemon(true);
        thread.start();
    } // runNow

    /**
     * alertError(). called when an error occurs in the getImages button
     * @param cause the data of the error.
     */
    public void alertError(Throwable cause) {
        TextArea text = new TextArea("URI: " + uri + "\n\n" + cause.toString());
        text.setEditable(false);
        Alert alert = new Alert(AlertType.ERROR);
        alert.getDialogPane().setContent(text);
        alert.setResizable(true);
        alert.showAndWait();
    } // alertError

} // GalleryApp
