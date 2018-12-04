package com.madebyaron.genai.app.views;

import com.madebyaron.genai.ai.Pool;
import com.madebyaron.genai.app.presenters.OpenHostPresenter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;

public final class OpenHostView {

	private static final int STAGE_WIDTH = 540;

	private static final Insets LAYOUT_PADDING = new Insets(16);
	private static final int LAYOUT_SPACING = 8;

	private final OpenHostPresenter presenter = new OpenHostPresenter();
	private final Stage stage;

	private Label loadingLabel;
	private Label progressLabel;
	private ProgressBar progressBar;

	public OpenHostView(Stage stage, int port, File file) {
		this.stage = stage;
		stage.setWidth(STAGE_WIDTH);
		stage.setScene(new Scene(createLayout()));
		stage.show();

		presenter.attachView(this);
		presenter.setPort(port);
		presenter.load(file);
	}

	private Parent createLayout() {
		VBox layout = new VBox();
		layout.setPadding(LAYOUT_PADDING);
		layout.setSpacing(LAYOUT_SPACING);
		layout.setFillWidth(true);

		loadingLabel = new Label();

		progressLabel = new Label();

		progressBar = new ProgressBar();
		progressBar.setMaxWidth(Double.POSITIVE_INFINITY);

		layout.getChildren().addAll(
				loadingLabel,
				progressLabel,
				progressBar
		);

		return layout;
	}

	public void setLoading(String fileName) {
		Platform.runLater(() -> {
			loadingLabel.setText("Loading: " + fileName);
		});
	}

	public void setProgress(String description, double progress) {
		Platform.runLater(() -> {
			progressLabel.setText(description);
			progressBar.setProgress(progress);
		});
	}

	public void showError(String title, String message) {
		Platform.runLater(() -> {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle(title);
			alert.setHeaderText(title);
			alert.setContentText(message);
			alert.show();

			stage.close();
		});
	}

	public void openHost(int port, Pool pool) {
		Platform.runLater(() -> {
			new HostView(new Stage(), port, pool);
			stage.close();
		});
	}
}
