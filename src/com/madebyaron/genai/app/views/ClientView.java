package com.madebyaron.genai.app.views;

import com.madebyaron.genai.app.presenters.ClientPresenter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.InetSocketAddress;

public final class ClientView {

	private static final int STAGE_WIDTH = 360;
	private static final int STAGE_HEIGHT = 240;

	private static final Insets LAYOUT_PADDING = new Insets(8);
	private static final int LAYOUT_SPACING = 2;

	private final ClientPresenter presenter = new ClientPresenter();
	private final Stage stage;

	private Text addressText;
	private Text portText;
	private Text processText;
	private Text statusText;

	public ClientView(Stage stage, InetSocketAddress address) {
		this.stage = stage;
		stage.setWidth(STAGE_WIDTH);
		stage.setHeight(STAGE_HEIGHT);
		stage.setScene(new Scene(createLayout()));
		stage.show();

		presenter.attachView(this);
		presenter.start(address);

		stage.setOnHidden(event -> presenter.stop());
	}

	private Parent createLayout() {
		VBox layout = new VBox();
		layout.setPadding(LAYOUT_PADDING);
		layout.setSpacing(LAYOUT_SPACING);
		layout.setFillWidth(true);

		addressText = new Text();

		portText = new Text();

		processText = new Text();

		statusText = new Text();

		layout.getChildren().addAll(
				addressText,
				portText,
				processText,
				statusText
		);

		return layout;
	}

	public void setAddress(String address, int port) {
		Platform.runLater(() -> {
			addressText.setText("Address: " + address);
			portText.setText("Port: " + Integer.toString(port));
		});
	}

	public void setProcess(String process) {
		Platform.runLater(() -> {
			processText.setText("Process: " + process);
		});
	}

	public void setStatus(String status) {
		Platform.runLater(() -> {
			statusText.setText("Status: " + status);
		});
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

	public void close() {
		Platform.runLater(stage::close);
	}
}
