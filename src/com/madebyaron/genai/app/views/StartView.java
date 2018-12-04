package com.madebyaron.genai.app.views;

import com.madebyaron.genai.app.presenters.StartPresenter;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.InetSocketAddress;

public final class StartView {

	private static final int STAGE_WIDTH = 360;
	private static final int STAGE_HEIGHT = 240;

	private static final Insets CLIENT_PANE_PADDING = new Insets(8);
	private static final int CLIENT_PANE_SPACING = 8;
	private static final Insets SERVER_PANE_PADDING = new Insets(8);
	private static final int SERVER_PANE_SPACING = 8;

	private final StartPresenter presenter = new StartPresenter();
	private final Stage stage;

	public StartView(Stage stage) {
		this.stage = stage;
		stage.setWidth(STAGE_WIDTH);
		stage.setHeight(STAGE_HEIGHT);
		stage.setScene(new Scene(createLayout()));
		stage.show();

		presenter.attachView(this);
	}

	private Parent createLayout() {
		TabPane layout = new TabPane();

		Tab clientTab = new Tab("Client", createClientPane());

		Tab serverTab = new Tab("Host", createServerPane());

		layout.getTabs().addAll(clientTab, serverTab);
		layout.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

		return layout;
	}

	private Node createClientPane() {
		GridPane pane = new GridPane();
		pane.setPadding(CLIENT_PANE_PADDING);
		pane.setHgap(CLIENT_PANE_SPACING);
		pane.setVgap(CLIENT_PANE_SPACING);

		Label hostAddressLabel = new Label("Host Address");
		TextField hostAddressInput = new TextField("aronmac.sch.bme.hu");

		Label hostPortLabel = new Label("Host Port");
		TextField hostPortInput = new TextField("8800");

		Button connectButton = new Button("Connect");
		connectButton.setOnAction((actionEvent) -> presenter.connect(
				hostAddressInput.getText(),
				hostPortInput.getText()
		));

		GridPane.setHgrow(hostAddressInput, Priority.ALWAYS);
		GridPane.setHgrow(hostPortInput, Priority.ALWAYS);
		GridPane.setHgrow(connectButton, Priority.ALWAYS);
		GridPane.setVgrow(connectButton, Priority.ALWAYS);
		GridPane.setHalignment(connectButton, HPos.RIGHT);
		GridPane.setValignment(connectButton, VPos.BOTTOM);

		pane.add(hostAddressLabel, 0, 0);
		pane.add(hostAddressInput, 1, 0);
		pane.add(hostPortLabel, 0, 1);
		pane.add(hostPortInput, 1, 1);
		pane.add(connectButton, 1, 2);

		return pane;
	}

	private Node createServerPane() {
		GridPane pane = new GridPane();
		pane.setPadding(SERVER_PANE_PADDING);
		pane.setHgap(SERVER_PANE_SPACING);
		pane.setVgap(SERVER_PANE_SPACING);

		Label hostPortLabel = new Label("Host Port");
		TextField hostPortInput = new TextField("8800");

		HBox buttons = new HBox();
		buttons.setSpacing(SERVER_PANE_SPACING);
		buttons.setAlignment(Pos.BOTTOM_RIGHT);

		Button openButton = new Button("Open");
		openButton.setOnAction((actionEvent) -> {
			FileChooser fileChooser = new FileChooser();
			File file = fileChooser.showOpenDialog(stage);
			if (file != null) presenter.openHost(hostPortInput.getText(), file);
		});

		Button createButton = new Button("Create");
		createButton.setOnAction((actionEvent) -> {
			presenter.createHost(hostPortInput.getText());
		});

		GridPane.setHgrow(hostPortInput, Priority.ALWAYS);
		GridPane.setHgrow(buttons, Priority.ALWAYS);
		GridPane.setVgrow(buttons, Priority.ALWAYS);
		GridPane.setHalignment(buttons, HPos.RIGHT);
		GridPane.setValignment(buttons, VPos.BOTTOM);

		buttons.getChildren().addAll(openButton, createButton);

		pane.add(hostPortLabel, 0, 0);
		pane.add(hostPortInput, 1, 0);
		pane.add(buttons, 1, 2);

		return pane;
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

	public void openClient(InetSocketAddress socketAddress) {
		Platform.runLater(() -> {
			new ClientView(new Stage(), socketAddress);
		});
	}

	public void openOpenHost(int port, File file) {
		Platform.runLater(() -> {
			new OpenHostView(new Stage(), port, file);
		});
	}

	public void openCreateHost(int port) {
		Platform.runLater(() -> {
			new CreateHostView(new Stage(), port);
		});
	}
}
