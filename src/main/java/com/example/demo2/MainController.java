package com.example.demo2;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.*;

public class MainController {

    //Variabili comunicanti con scenebuilder
    @FXML
    private StackPane pageContainer;
    @FXML
    private ScrollPane scrollPage;
    @FXML
    private HBox footer;
    @FXML
    private BorderPane header;

    //Tutte le pagine e l'index attuale
    private Node presentationPage;
    private List<Node> slides = new ArrayList<>();
    private int currentIndex = 0;

    // Gestione ridimensionamento font
    private double initialSceneWidth = -1;
    private double initialSceneHeight = -1;
    private Map<Node, Double> originalFontSizes = new HashMap<>();

    // Metodi per i margini
    private void update(Node node, Scene scene, double weight, double height) {
        this.update(node, scene, weight, height, -1, -1);
    }
    private void update(Node node, Scene scene, double weight, double height, double top) {
        this.update(node, scene, weight, height, top, -1);
    }
    private void update(Node node, Scene scene, double weight, double height, double top, double bottom) {
        //Grandezze iniziali
        double w = scene.getWidth();
        double h = scene.getHeight();

        //Creo un insets per i margini
        Insets margin = new Insets(
                (top > -1 ? top : h * height),
                w * weight,
                (bottom == -1 ? 0 : h * height),
                w * weight
        );

        //Imposto i margini se trovo l'elemento corretto
        Parent parent = node.getParent();
        if (parent == null) return;
        if (parent instanceof BorderPane) BorderPane.setMargin(node, margin);
        else if (parent instanceof StackPane) StackPane.setMargin(node, margin);
        else if (parent instanceof HBox) HBox.setMargin(node, margin);
    }


    //Inizializzazione
    public void initialize() {

        //Solo dopo aver caricato la scena
        Platform.runLater(() -> {
            //Rimuovo lo sfondo che da scenebuilder non riuscivo ho dovuto cercare una soluzione
            Node viewport = scrollPage.lookup(".viewport");
            if (viewport != null) viewport.setStyle("-fx-background-color: transparent;");
        });

        //Funzione che gestisce tutti gli update di dimensione per gestire i margini in caso di ridimensionamento
        header.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene == null) return;

            //Funzione che in caso di resize ripristina i margini
            ChangeListener<Number> listener = ((o, a, b) -> {
                update(header, scene, 0.033, 0.0525);
                update(scrollPage, scene, 0.04, 0, 20);
                update(footer, scene, 0, 0.0525, 0, 1);
            });

            //Esegue funzioni in caso di resize
            scene.widthProperty().addListener(listener);
            scene.heightProperty().addListener(listener);

            //Fa l'update dei margini
            update(header, scene, 0.033, 0.0525);
            update(scrollPage, scene, 0.04, 0, 20);
            update(footer, scene, 0, 0.0525, 0, 1);
        });


        try {
            // Carica presentazione
            this.presentationPage = FXMLLoader.load(getClass().getResource("presentation.fxml"));

            //Imposto il nodo dopo averlo creato
            prepareNodeForExpansion(this.presentationPage);

            // Carica slide
            int slideNumber = 1;

            //Ciclo per cercare e caricare slide
            while (true) {
                String slidePath = "slide" + slideNumber + ".fxml";
                if (getClass().getResource(slidePath) != null) {
                    //Se la slide esisste
                    Node slide = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(slidePath)));
                    //Aggiunge la slide all'array di slide
                    slides.add(slide);
                    //E prepara il nodo per il resize
                    prepareNodeForExpansion(slide);
                    //Imposto il nodo dopo averlo creato
                    slideNumber++;
                } else break;
            }

            //Imposta i padding
            pageContainer.setPadding(new Insets(10));

            // Gestione ridimensionamento e freccette (destra e sinistra)
            // Viene attivato solo quando viene cambiata scena
            pageContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {

                //Solo se esiste la nuova scena
                if (newScene != null) {

                    //Imposto le dimensioni iniziali della scena
                    if (initialSceneWidth == -1 && newScene.getWidth() > 0 && newScene.getHeight() > 0) {
                        initialSceneWidth = newScene.getWidth();
                        initialSceneHeight = newScene.getHeight();
                    }


                    //Imposto gli eventi in caso di ridimensione finestra per modificare le dimensioni del testo
                    newScene.widthProperty().addListener((o, oldVal, newVal) -> this.updateFontScale(newScene));
                    newScene.heightProperty().addListener((o, oldVal, newVal) -> this.updateFontScale(newScene));


                    //Imposto l'evento delle freccette destra e sinistra
                    newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                        switch (event.getCode()) {
                            case RIGHT: this.onNext(); event.consume(); break;
                            case LEFT:  this.onPrevious(); event.consume(); break;
                            default: break;
                        }
                    });
                }
            });

        } catch (IOException e) {
            //Errore
            e.printStackTrace();
        }

        showCurrentPage();
    }

    //Impostazione minima nodo per aggiungerlo al Page
    private void prepareNodeForExpansion(Node node) {

        //Grandezze massime
        node.maxWidth(Double.MAX_VALUE);
        node.maxHeight(Double.MAX_VALUE);

        //Se è un vbox metto FillWidth a true
        if (node instanceof VBox VBox) VBox.setFillWidth(true);
        node.applyCss(); // Forzo l'applicazione degli stili
    }

    //Funzione per calcolare e poi scalare il testo in base alla dimensione
    private void updateFontScale(Scene scene) {

        //Solo se c'è la scena
        if (scene == null) return;

        //Se non è già stata impostata almeno una dimensione iniziale
        if (this.initialSceneWidth <= 0 || this.initialSceneHeight <= 0) {

            //Se la scena ha un altezza e dimensione iniziale imposto le due variabili della classe
            if (scene.getWidth() > 0 && scene.getHeight() > 0) {
                this.initialSceneWidth = scene.getWidth();
                this.initialSceneHeight = scene.getHeight();
            } else return;
        }

        //Calcolo di quanto deve scalare in base alla dimensione attuale della scena e quelle iniziali
        double widthRatio = scene.getWidth() / this.initialSceneWidth;
        double heightRatio = scene.getHeight() / this.initialSceneHeight;

        //Prendo il piu piccolo frai due
        double scaleFactor = Math.min(widthRatio, heightRatio);

        //Prendo il piu' piccolo fra 2,e il piu grande fra 0,5 e la scala calcolata
        scaleFactor = Math.min(2.0, Math.max(0.5, scaleFactor));

        //Applico la scala
        applyFontScaleToCurrentContent(scaleFactor);
    }

    //Funzione che serve da tramite per impostare la scala
    private void applyFontScaleToCurrentContent(double scaleFactor) {
        if (pageContainer.getChildren().isEmpty()) return;

        //Contenuti della pagina corrente
        Node currentContent = pageContainer.getChildren().get(0);

        //Applicazione della scala a tutti gli elementi della pagina corrente
        applyFontScaleRecursive(currentContent, scaleFactor);

        // Forzo il ricalcolo del layout e dello ScrollPane
        scrollPage.layout();
        currentContent.applyCss();
    }

    //Calcolo il nuovo fontsize ed applico la scala a tutti gli elementi del nodo (tramite ciclo in caso di parent->figli)
    private void applyFontScaleRecursive(Node node, double scaleFactor) {

        //Se non esiste ancora una variabile del nodo iniziale, la imposta
        if (!this.originalFontSizes.containsKey(node)) {

            //richiama la funzione che ritorna il fontsize corrente
            double originalSize = this.getCurrentFontSize(node);

            //Lo imposta se maggiore di 0 al map
            if (originalSize > 0 && !this.isFontPropertyBound(node)) {
                this.originalFontSizes.put(node, originalSize);
            }
        }

        //Se invece esistono nelle impostazioni imposta  il nuovo fontsize
        if (this.originalFontSizes.containsKey(node) && !this.isFontPropertyBound(node)) {
            double originalSize = this.originalFontSizes.get(node);

            //Nuovo font size
            double newSize = originalSize * scaleFactor;

            //Imposto il font size all'elemento
            this.setFontSizeDirect(node, newSize);
        }

        //Se è un parent ciclo tutti i suoi elementi richiamando questa funzione
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                this.applyFontScaleRecursive(child, scaleFactor);
            }
        }
    }

    //funzione che imposta il fontSize
    private void setFontSizeDirect(Node node, double newSize) {

        //In base a che elemento è imposto la nuova variabile nell'if
        //E per ognuna eseguo la funzione scale font

        if (node instanceof Labeled labeled) {
            labeled.setFont(this.scaleFont(labeled.getFont(), newSize));
        } else if (node instanceof Text text) {
            text.setFont(this.scaleFont(text.getFont(), newSize));
        } else if (node instanceof TextInputControl input) {
            input.setFont(this.scaleFont(input.getFont(), newSize));
        }

    }

    //Ritorna il font da impostare in base alle nuove dimensioni
    private Font scaleFont(Font original, double newSize) {
        String style = original.getStyle().toLowerCase(); // regular, bold, italic, bold italic
        FontWeight weight = style.contains("bold") ? FontWeight.BOLD : FontWeight.NORMAL;
        FontPosture posture = style.contains("italic") ? FontPosture.ITALIC : FontPosture.REGULAR;
        return Font.font(original.getFamily(), weight, posture, newSize);
    }

    //Funzione che ritorna il font corrente in base al node
    private double getCurrentFontSize(Node node) {
        //Ritorno il fontsize attuale
        if (node instanceof Labeled Labeled) return Labeled.getFont().getSize();
        if (node instanceof Text Text) return Text.getFont().getSize();
        if (node instanceof TextInputControl TextInputControl) return TextInputControl.getFont().getSize();
        return -1;
    }

    //Funzione che mi ritorna true solo se c'è un nodo vallido e posso comunicarci
    private boolean isFontPropertyBound(Node node) {
        if (node instanceof Labeled Labeled) return Labeled.fontProperty().isBound();
        if (node instanceof Text Text) return Text.fontProperty().isBound();
        if (node instanceof TextInputControl TextInputControl) return TextInputControl.fontProperty().isBound();
        return false;
    }


    //Funzione che mostra pagina attuale nella slide
    private void showCurrentPage() {
        //Rimuove tutti gi elementi nella pagina corrente
        this.pageContainer.getChildren().clear();


        //Presentazione
        if (this.currentIndex == 0) {
            //Imposto i nuovi elementi
            this.pageContainer.getChildren().add(this.presentationPage);

            //Se è la presentazione mostriamo un pò meno il bianco del container
            this.scrollPage.setStyle(this.scrollPage.getStyle() + "; -fx-background-color: rgba(255,255,255,0.62);");
        }

        //Altre slide
        else {
            int slideIdx = this.currentIndex - 1;
            //in caso ci si almeno una slide e non sia la pagina di presentazione
            if (slideIdx >= 0 && slideIdx < this.slides.size()) {
                //aggiungo gli elementi
                this.pageContainer.getChildren().add(this.slides.get(slideIdx));
            }
            //Imposto il background normale
            this.scrollPage.setStyle(this.scrollPage.getStyle() + "; -fx-background-color: rgba(255,255,255,0.84);");
        }

        // Ricalcola la scala del font in base alla dimensione attuale della scena
        Scene scene = this.pageContainer.getScene();
        if (scene != null) {
            this.updateFontScale(scene);
        }

        //Forzo il nodo a ricalcolare le dimensioni a tutti
        this.scrollPage.layout();
    }


    //Funzione che cambia colore (comunica direttamente su scene builder)
    @FXML
    private void changeColor(MouseEvent event) {
        Node node = (Node) event.getSource();
        node.setStyle((!node.getStyle().isEmpty() ? node.getStyle() + "; " : "") + "-fx-background-color: #ededed;");
    }

    //Altra Funzione che cambia colore (comunica direttamente su scene builder)
    @FXML
    private void changeColor2(MouseEvent event) {
        Node node = (Node) event.getSource();
        node.setStyle((!node.getStyle().isEmpty() ? node.getStyle() + "; " : "") + "-fx-background-color: #FFF;");
    }


    //Funzione che cambia slide con la successiva
    @FXML
    private void onNext() {
        //Se l'index attuale è piu' piccolo del conteggio delle index
        if (this.currentIndex < this.slides.size()) {
            //Cambia index
            this.currentIndex++;
            //Aggiorna pagina
            this.showCurrentPage();
        }
        //Abbiamo deciso di fermare le slide ma mettiamo lo stesso il codice per completezza
        //else{this.currentIndex = 0;}
    }

    //Funzione che cambia slide con la precedente
    @FXML private void onPrevious() {
        //Se l'index attuale è piu' grande di 0
        if (this.currentIndex > 0) {
            //Cambia index
            this.currentIndex--;
            //Aggiorna pagina
            this.showCurrentPage();
        }
        //Abbiamo deciso di fermare le slide ma mettiamo lo stesso il codice per completezza
        //else{this.currentIndex = this.slides.size();}
    }
}