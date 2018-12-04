package com.madebyaron.genai.app.views;

import com.madebyaron.genai.ai.Network;
import com.madebyaron.genai.ai.Pool;
import com.madebyaron.genai.ai.Process;
import com.madebyaron.genai.net.EvaluatorConnection;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

public final class HostView {

	private static final int STAGE_WIDTH = 960;
	private static final int STAGE_HEIGHT = 540;

	private static final int CLIENT_LIST_WIDTH = 200;
	private static final Insets CLIENT_LIST_PADDING = new Insets(8);
	private static final int CLIENT_LIST_SPACING = 8;

	private static final int CONTROLS_WIDTH = 200;
	private static final Insets CONTROLS_PADDING = new Insets(8);
	private static final int CONTROLS_SPACING = 8;

	private static final Insets DETAILS_PADDING = new Insets(8);
	private static final int DETAILS_SPACING = 8;

	private static final Insets HOST_PADDING = new Insets(8);
	private static final int HOST_SPACING = 2;
	private static final Paint HOST_BACKGROUND_COLOR = Color.GRAY;
	private static final CornerRadii HOST_CORNER_RADIUS = new CornerRadii(5);
	private static final Background HOST_BACKGROUND = new Background(new BackgroundFill(
			HOST_BACKGROUND_COLOR,
			HOST_CORNER_RADIUS,
			Insets.EMPTY)
	);

	private static final Insets CLIENT_PADDING = new Insets(8);
	private static final int CLIENT_SPACING = 2;
	private static final Paint CLIENT_BACKGROUND_COLOR = Color.LIGHTGRAY;
	private static final CornerRadii CLIENT_CORNER_RADIUS = new CornerRadii(5);
	private static final Background CLIENT_BACKGROUND = new Background(new BackgroundFill(
			CLIENT_BACKGROUND_COLOR,
			CLIENT_CORNER_RADIUS,
			Insets.EMPTY)
	);

	private final HostPresenter presenter = new HostPresenter();
	private final Stage stage;

	private final HashMap<EvaluatorConnection, ClientHolder> clientHolders = new HashMap<>();
	private VBox networkListPane;

	private Text processText;
	private Text hostAddressText;
	private Text hostPortText;
	private Text hostLoadText;

	private Button saveButton;
	private Button playButton;

	private Text bestEvaluationText;

	public HostView(Stage stage, int port, Pool pool) {
		this.stage = stage;
		stage.setWidth(STAGE_WIDTH);
		stage.setHeight(STAGE_HEIGHT);
		stage.setScene(new Scene(createLayout()));
		stage.show();

		presenter.attachView(this);
		presenter.setPool(pool);
		presenter.startServer(port);

		stage.setOnHidden(event -> presenter.stopServer());
	}

	private Parent createLayout() {
		BorderPane layout = new BorderPane();

		layout.setLeft(createClientList());
		layout.setRight(createControls());
		layout.setCenter(createDetails());

		return layout;
	}

	private Node createClientList() {
		ScrollPane networkListScrollPane = new ScrollPane();
		networkListScrollPane.setMinWidth(CLIENT_LIST_WIDTH);
		networkListScrollPane.setFitToWidth(true);

		networkListPane = new VBox();
		networkListPane.setPadding(CLIENT_LIST_PADDING);
		networkListPane.setSpacing(CLIENT_LIST_SPACING);
		networkListPane.setFillWidth(true);

		networkListPane.getChildren().add(createHost());
		networkListScrollPane.setContent(networkListPane);

		return networkListScrollPane;
	}

	private Node createHost() {
		VBox hostPane = new VBox();
		hostPane.setBackground(HOST_BACKGROUND);
		hostPane.setPadding(HOST_PADDING);
		hostPane.setSpacing(HOST_SPACING);
		hostPane.setFillWidth(true);

		processText = new Text();

		hostAddressText = new Text();

		hostPortText = new Text();

		hostLoadText = new Text();

		hostPane.getChildren().addAll(
				processText,
				hostAddressText,
				hostPortText,
				hostLoadText
		);

		return hostPane;
	}

	private void createClient(ClientHolder holder) {
		holder.pane = new VBox();
		holder.pane.setBackground(CLIENT_BACKGROUND);
		holder.pane.setPadding(CLIENT_PADDING);
		holder.pane.setSpacing(CLIENT_SPACING);
		holder.pane.setFillWidth(true);

		holder.addressText = new Text();

		holder.portText = new Text();

		holder.loadText = new Text();

		holder.pane.getChildren().addAll(
				holder.addressText,
				holder.portText,
				holder.loadText
		);
	}

	private Node createControls() {
		ScrollPane controlsScrollPane = new ScrollPane();
		controlsScrollPane.setMinWidth(CONTROLS_WIDTH);
		controlsScrollPane.setMaxWidth(CONTROLS_WIDTH);
		controlsScrollPane.setFitToWidth(true);

		VBox controlsPane = new VBox();
		controlsPane.setPadding(CONTROLS_PADDING);
		controlsPane.setSpacing(CONTROLS_SPACING);
		controlsPane.setFillWidth(true);

		saveButton = new Button("Save");
		saveButton.setMaxWidth(Double.POSITIVE_INFINITY);
		saveButton.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			File file = fileChooser.showSaveDialog(stage);
			if (file != null) presenter.save(file);
		});

		playButton = new Button("Play");
		playButton.setMaxWidth(Double.POSITIVE_INFINITY);
		playButton.setOnAction(event -> {
			presenter.play();
		});

		CheckBox evaluateCheckBox = new CheckBox("Evaluate");
		evaluateCheckBox.setMaxWidth(Double.POSITIVE_INFINITY);
		evaluateCheckBox.setOnAction(event -> {
			if (evaluateCheckBox.isSelected()) {
				presenter.startEvaluation();
			} else {
				presenter.stopEvaluation();
			}
		});

		controlsPane.getChildren().addAll(saveButton, playButton, evaluateCheckBox);
		controlsScrollPane.setContent(controlsPane);

		return controlsScrollPane;
	}

	private Node createDetails() {
		ScrollPane detailsScrollPane = new ScrollPane();
		detailsScrollPane.setFitToWidth(true);

		VBox detailsPane = new VBox();
		detailsPane.setPadding(DETAILS_PADDING);
		detailsPane.setSpacing(DETAILS_SPACING);
		detailsPane.setFillWidth(true);

		bestEvaluationText = new Text();
		bestEvaluationText.setText("Run to see the best evaluation");

		detailsPane.getChildren().addAll(bestEvaluationText);
		detailsScrollPane.setContent(detailsPane);

		return detailsScrollPane;
	}

	public void setProcess(String process) {
		Platform.runLater(() -> {
			processText.setText("Hosting: " + process);
		});
	}

	public void setHostAddress(String address, int port) {
		Platform.runLater(() -> {
			hostAddressText.setText("Address: " + address);
			hostPortText.setText("Port: " + Integer.toString(port));
		});
	}

	public void setHostLoad(int load) {
		Platform.runLater(() -> {
			hostLoadText.setText("Load: " + Integer.toString(load));
		});
	}

	public void addClient(EvaluatorConnection connection) {
		ClientHolder holder = new ClientHolder();
		clientHolders.put(connection, holder);

		Platform.runLater(() -> {
			createClient(holder);
			networkListPane.getChildren().add(holder.pane);
		});
	}

	public void removeClient(EvaluatorConnection connection) {
		ClientHolder holder = clientHolders.remove(connection);
		if (holder != null) {
			Platform.runLater(() -> {
				networkListPane.getChildren().remove(holder.pane);
			});
		}
	}

	public void setClientAddress(EvaluatorConnection connection, String address, int port) {
		ClientHolder holder = clientHolders.get(connection);
		if (holder != null) {
			Platform.runLater(() -> {
				holder.addressText.setText("Address: " + address);
				holder.portText.setText("Port: " + Integer.toString(port));
			});
		}
	}

	public void setClientLoad(EvaluatorConnection connection, int load) {
		ClientHolder holder = clientHolders.get(connection);
		if (holder != null) {
			Platform.runLater(() -> {
				holder.loadText.setText("Load: " + Integer.toString(load));
			});
		}
	}

	public void setSaveEnabled(boolean enabled) {
		Platform.runLater(() -> {
			saveButton.setDisable(!enabled);
		});
	}

	public void setPlayEnabled(boolean enabled) {
		Platform.runLater(() -> {
			playButton.setDisable(!enabled);
		});
	}

	public void setBestEvaluation(double evaluation) {
		Platform.runLater(() -> {
			bestEvaluationText.setText("Best evaluation: " + Double.toString(evaluation));
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

	public void openSaveHost(File file, String process, Collection<Network> networks) {
		Platform.runLater(() -> {
			new SaveHostView(new Stage(), file, process, networks);
		});
	}

	public void openPlayNetwork(Process process, Network network) {
		Platform.runLater(() -> {
			new PlayNetworkView(new Stage(), process, network);
		});
	}

	public void close() {
		Platform.runLater(stage::close);
	}

	private static class ClientHolder {

		VBox pane;
		Text addressText;
		Text portText;
		Text loadText;
	}
}
