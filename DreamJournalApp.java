import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Dream Journal Decoder — JavaFX dashboard.
 * Loads dream entries, shows recurring themes, mood trend, and lets
 * the user add new entries live. Also offers a real AI interpretation
 * of the most recently selected/added dream via the Anthropic API.
 */
public class DreamJournalApp extends Application {

    private final DreamAnalyzer analyzer = new DreamAnalyzer();
    private final DreamInterpreter interpreter = new DreamInterpreter();

    private BarChart<String, Number> themeChart;
    private LineChart<String, Number> moodChart;
    private Label summaryLabel;
    private ListView<String> entryList;
    private TextArea aiResultArea;
    private Button interpretButton;
    private String lastDreamText = "";

    @Override
    public void start(Stage stage) {
        try {
            analyzer.loadFromCsv("data/sample_dreams.csv");
        } catch (IOException e) {
            System.out.println("Could not load sample data: " + e.getMessage());
        }

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        summaryLabel = new Label();
        summaryLabel.setWrapText(true);
        summaryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");

        root.setTop(summaryLabel);
        root.setCenter(buildChartsPane());
        root.setRight(buildEntryPanel());

        refreshAll();

        Scene scene = new Scene(root, 1050, 680);
        stage.setTitle("Dream Journal Decoder");
        stage.setScene(scene);
        stage.show();
    }

    private VBox buildChartsPane() {
        CategoryAxis themeX = new CategoryAxis();
        NumberAxis themeY = new NumberAxis();
        themeChart = new BarChart<>(themeX, themeY);
        themeChart.setTitle("Top Recurring Dream Symbols");
        themeChart.setLegendVisible(false);
        themeChart.setPrefHeight(260);

        CategoryAxis moodX = new CategoryAxis();
        NumberAxis moodY = new NumberAxis();
        moodChart = new LineChart<>(moodX, moodY);
        moodChart.setTitle("Mood Trend Over Time (sentiment score)");
        moodChart.setLegendVisible(false);
        moodChart.setPrefHeight(260);

        Label aiTitle = new Label("AI Interpretation");
        aiTitle.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");

        aiResultArea = new TextArea();
        aiResultArea.setEditable(false);
        aiResultArea.setWrapText(true);
        aiResultArea.setPrefRowCount(4);
        aiResultArea.setPromptText("Select a dream from the list on the right, then click \"Interpret with AI\".");

        interpretButton = new Button("Interpret with AI");
        interpretButton.setMaxWidth(Double.MAX_VALUE);
        interpretButton.setOnAction(e -> runInterpretation());

        VBox box = new VBox(10, themeChart, moodChart, aiTitle, aiResultArea, interpretButton);
        box.setPadding(new Insets(0, 15, 0, 0));
        return box;
    }

    private VBox buildEntryPanel() {
        Label title = new Label("Dream Entries (click one to select)");
        title.setStyle("-fx-font-weight: bold;");

        entryList = new ListView<>();
        entryList.setPrefWidth(320);
        entryList.setPrefHeight(280);
        entryList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Entry text is stored after "] " in the display string.
                int idx = newVal.indexOf("] ");
                lastDreamText = idx >= 0 ? newVal.substring(idx + 2) : newVal;
            }
        });

        Label addTitle = new Label("Add New Entry");
        addTitle.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");

        DatePicker datePicker = new DatePicker(LocalDate.now());
        ComboBox<String> moodBox = new ComboBox<>();
        moodBox.getItems().addAll("happy", "anxious", "neutral");
        moodBox.setValue("neutral");
        moodBox.setMaxWidth(Double.MAX_VALUE);

        TextArea textArea = new TextArea();
        textArea.setPromptText("Describe your dream...");
        textArea.setPrefRowCount(4);
        textArea.setWrapText(true);

        Button addButton = new Button("Add & Re-analyze");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(e -> {
            String text = textArea.getText().trim();
            if (!text.isEmpty()) {
                analyzer.addEntry(new DreamEntry(datePicker.getValue(), moodBox.getValue(), text));
                lastDreamText = text;
                textArea.clear();
                refreshAll();
            }
        });

        VBox box = new VBox(8,
            title, entryList,
            addTitle, datePicker, moodBox, textArea, addButton
        );
        box.setPadding(new Insets(0, 0, 0, 5));
        box.setAlignment(Pos.TOP_LEFT);
        return box;
    }

    /** Runs the API call on a background thread so the GUI doesn't freeze. */
    private void runInterpretation() {
        if (lastDreamText.isBlank()) {
            aiResultArea.setText("Select a dream from the list, or add a new one, first.");
            return;
        }

        interpretButton.setDisable(true);
        aiResultArea.setText("Thinking...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return interpreter.interpret(lastDreamText);
            }
        };

        task.setOnSucceeded(e -> {
            aiResultArea.setText(task.getValue());
            interpretButton.setDisable(false);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            aiResultArea.setText("Could not get interpretation: " + ex.getMessage());
            interpretButton.setDisable(false);
        });

        new Thread(task).start();
    }

    private void refreshAll() {
        summaryLabel.setText(analyzer.buildSummary());
        refreshEntryList();
        refreshThemeChart();
        refreshMoodChart();
    }

    private void refreshEntryList() {
        entryList.getItems().clear();
        for (DreamEntry e : analyzer.getEntries()) {
            entryList.getItems().add(e.toString());
        }
    }

    private void refreshThemeChart() {
        themeChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, Integer> entry : analyzer.topThemes(8)) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        themeChart.getData().add(series);
    }

    private void refreshMoodChart() {
        moodChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<Map.Entry<LocalDate, Integer>> trend = analyzer.moodTrend();
        for (Map.Entry<LocalDate, Integer> point : trend) {
            series.getData().add(new XYChart.Data<>(point.getKey().format(fmt), point.getValue()));
        }
        moodChart.getData().add(series);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
