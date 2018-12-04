package com.madebyaron.genai.app.views;

import com.madebyaron.genai.ai.Network;
import com.madebyaron.genai.app.presenters.SaveHostPresenter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.util.Collection;

public final class SaveHostView {

	private static final int STAGE_WIDTH = 540;

	private static final Insets LAYOUT_PADDING = new Insets(16);
	private static final int LAYOUT_SPACING = 8;

	private final SaveHostPresenter presenter = new SaveHostPresenter();
	private final Stage stage;

	private Label savingLabel;
	private Label progressLabel;
	private ProgressBar progressBar;

	public SaveHostView(Stage stage, File file, String process, Collection<Network> networks) {
		this.stage = stage;
		stage.setWidth(STAGE_WIDTH);
		stage.setScene(new Scene(createLayout()));
		stage.show();

		presenter.attachView(this);
		presenter.save(file, process, networks);
	}

	private Parent createLayout() {
		VBox layout = new VBox();
		layout.setPadding(LAYOUT_PADDING);
		layout.setSpacing(LAYOUT_SPACING);
		layout.setFillWidth(true);

		savingLabel = new Label();

		progressLabel = new Label();

		progressBar = new ProgressBar();
		progressBar.setMaxWidth(Double.POSITIVE_INFINITY);

		layout.getChildren().addAll(
				savingLabel,
				progressLabel,
				progressBar
		);

		return layout;
	}

	public void setSaving(String fileName) {
		Platform.runLater(() -> {
			savingLabel.setText("Saving: " + fileName);
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

	public void close() {
		Platform.runLater(stage::close);
	}
}
