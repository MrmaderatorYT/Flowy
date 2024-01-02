package com.ccs.flowybrowser;

import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class HelloApplication extends Application {
    private WebView webView;
    private WebEngine webEngine;
    private TabPane tabPane;
    private static final int MAX_TAB_TITLE_LENGTH = 20; // Максимальная длина заголовка вкладки
    private List<String> favorites;
    private JavaConnector javaConnector;

    @Override
    public void start(Stage primaryStage) {
        webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", new JavaConnector());
                String title = (String) webEngine.executeScript("document.title");
                Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                if (selectedTab != null && selectedTab.getContent() == webView) {
                    selectedTab.setText(getTabTitle(title));
                }
            }
        });

        tabPane = new TabPane();

        BorderPane root = new BorderPane();
        root.setTop(createMenuBar());
        root.setCenter(tabPane);

        addNewTab("Новая вкладка");

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("My Browser");
        primaryStage.setOnCloseRequest(event -> {
            if (tabPane.getTabs().size() == 1) {
                // Закрыть окно приложения, если осталась только одна вкладка
                primaryStage.close();
            } else {
                // Отменить закрытие окна, если есть более одной вкладки
                event.consume();
            }
        });
        primaryStage.show();

        loadFavorites(); // Загрузка избранного из файла
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("Файл");
        MenuItem newTabMenuItem = new MenuItem("Новая вкладка");
        newTabMenuItem.setOnAction(event -> addNewTab("Новая вкладка"));
        MenuItem favoritesMenuItem = new MenuItem("Избранное");
        favoritesMenuItem.setOnAction(event -> showFavorites());
        MenuItem addToFavoritesMenuItem = new MenuItem("Добавить в избранное");
        addToFavoritesMenuItem.setOnAction(event -> {
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab != null && selectedTab.getContent() instanceof WebView) {
                WebView webView = (WebView) selectedTab.getContent();
                WebEngine webEngine = webView.getEngine();
                String currentUrl = webEngine.getLocation();
                javaConnector.showAddToFavoritesDialog(currentUrl);
            }
        });
        fileMenu.getItems().addAll(newTabMenuItem, favoritesMenuItem, addToFavoritesMenuItem);

        menuBar.getMenus().add(fileMenu);
        return menuBar;
    }

    private void addNewTab(String titleOrUrl) {
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", new JavaConnector());
                String pageTitle = (String) webEngine.executeScript("document.title");
                Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                if (selectedTab != null && selectedTab.getContent() == webView) {
                    selectedTab.setText(getTabTitle(pageTitle));
                }
            }
        });

        Tab tab;
        if (titleOrUrl.startsWith("http://") || titleOrUrl.startsWith("https://")) {
            webEngine.load(titleOrUrl);
            tab = new Tab(titleOrUrl, webView);
        } else {
            String htmlFilePath = getClass().getResource("/index.html").toExternalForm();
            webEngine.load(htmlFilePath);
            tab = new Tab(getTabTitle(titleOrUrl), webView);
        }

        tab.setOnClosed(event -> {
            webView.getEngine().load(null);
            if (tabPane.getTabs().size() == 0) {
                // Закрыть окно приложения, если осталась только одна вкладка
                Stage stage = (Stage) tabPane.getScene().getWindow();
                stage.close();
            }
        });

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
    }

    private String getTabTitle(String title) {
        if (title.length() <= MAX_TAB_TITLE_LENGTH) {
            return title;
        } else {
            return title.substring(0, MAX_TAB_TITLE_LENGTH - 3) + "...";
        }
    }

    private void showFavorites() {
        ListView<String> favoritesListView = new ListView<>();
        favoritesListView.getItems().addAll(favorites);

        favoritesListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                String selectedUrl = favoritesListView.getSelectionModel().getSelectedItem();
                if (selectedUrl != null) {
                    addNewTab(selectedUrl);
                }
            }
        });

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Избранное");
        dialog.getDialogPane().setContent(favoritesListView);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }

    private void loadFavorites() {
        favorites = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("favorites.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                favorites.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveFavorites() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("favorites.txt"))) {
            for (String favorite : favorites) {
                writer.write(favorite);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addToFavorites(String url) {
        favorites.add(url);
        saveFavorites();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public class JavaConnector {
        public void loadPage(String url) {
            webEngine.load(url);
        }

        public void search(String searchText) {
            String searchUrl = "https://www.google.com/search?q=" + searchText;
            loadPage(searchUrl);
        }

        public void showAddToFavoritesDialog(String url) {
            TextInputDialog dialog = new TextInputDialog(url);
            dialog.setTitle("Добавить в избранное");
            dialog.setHeaderText("Введите URL сайта:");
            dialog.setContentText("URL:");
            dialog.showAndWait().ifPresent(siteUrl -> {
                addToFavorites(siteUrl);
            });
        }
    }
}
