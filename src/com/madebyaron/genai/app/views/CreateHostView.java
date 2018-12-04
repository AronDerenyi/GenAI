package com.madebyaron.genai.app.views;

import com.madebyaron.genai.ai.Network;
import com.madebyaron.genai.ai.Pool;
import com.madebyaron.genai.app.presenters.CreateHostPresenter;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.util.List;

public final class CreateHostView {

	private static final int STAGE_WIDTH = 360;
	private static final int STAGE_HEIGHT = 160;

	private static final Insets LAYOUT_PADDING = new Insets(8);
	private static final int LAYOUT_SPACING = 8;

	private final CreateHostPresenter presenter = new CreateHostPresenter();
	private final Stage stage;

	public CreateHostView(Stage stage, int port) {
		this.stage = stage;
		stage.setWidth(STAGE_WIDTH);
		stage.setHeight(STAGE_HEIGHT);
		stage.setScene(new Scene(createLayout()));
		stage.show();

		presenter.attachView(this);
		presenter.setPort(port);
	}

	private Parent createLayout() {
		GridPane layout = new GridPane();
		layout.setPadding(LAYOUT_PADDING);
		layout.setHgap(LAYOUT_SPACING);
		layout.setVgap(LAYOUT_SPACING);

		Label processLabel = new Label("Process");
		TextField processInput = new TextField("");

		Label poolSizeLabel = new Label("Pool size");
		TextField poolSizeInput = new TextField("1000");

		Button createButton = new Button("Create");
		createButton.setOnAction((actionEvent) -> presenter.create(
				processInput.getText(),
				poolSizeInput.getText()
		));

		GridPane.setHgrow(processInput, Priority.ALWAYS);
		GridPane.setHgrow(poolSizeInput, Priority.ALWAYS);
		GridPane.setHgrow(createButton, Priority.ALWAYS);
		GridPane.setVgrow(createButton, Priority.ALWAYS);
		GridPane.setHalignment(createButton, HPos.RIGHT);
		GridPane.setValignment(createButton, VPos.BOTTOM);

		layout.add(processLabel, 0, 0);
		layout.add(processInput, 1, 0);
		layout.add(poolSizeLabel, 0, 1);
		layout.add(poolSizeInput, 1, 1);
		layout.add(createButton, 1, 2);

		return layout;
	}

	public void showError(String title, String message) {
		Platform.runLater(() -> {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle(title);
			alert.setHeaderText(title);
			alert.setContentText(message);
			alert.show();
		});
	}

	public void openHost(int port, Pool pool) {
		Platform.runLater(() -> {
			new HostView(new Stage(), port, pool);
			stage.close();
		});
	}
}
