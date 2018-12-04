package com.madebyaron.genai.app.views;

import com.madebyaron.genai.ai.Network;
import com.madebyaron.genai.ai.Process;
import com.madebyaron.genai.app.presenters.PlayNetworkPresenter;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public final class PlayNetworkView {

	private static final int STAGE_WIDTH = 960;
	private static final int STAGE_HEIGHT = 540;

	private final PlayNetworkPresenter presenter = new PlayNetworkPresenter();
	private final Stage stage;

	private Canvas canvas;
	private GraphicsContext graphics;

	public PlayNetworkView(Stage stage, Process process, Network network) {
		this.stage = stage;
		stage.setWidth(STAGE_WIDTH);
		stage.setHeight(STAGE_HEIGHT);
		stage.setScene(new Scene(createLayout()));
		stage.show();

		presenter.attachView(this);
		presenter.setProcess(process);
		presenter.setNetwork(network);

		AnimationTimer timer = new AnimationTimer() {
			@Override
			public void handle(long now) {
				presenter.setSize(canvas.getWidth(), canvas.getHeight());
				presenter.tick();
			}
		};
		timer.start();
	}

	private Parent createLayout() {
		Pane layout = new Pane();
		layout.setMaxWidth(Double.POSITIVE_INFINITY);
		layout.setMaxHeight(Double.POSITIVE_INFINITY);

		canvas = new Canvas();
		canvas.widthProperty().bind(layout.widthProperty());
		canvas.heightProperty().bind(layout.heightProperty());

		graphics = canvas.getGraphicsContext2D();

		layout.getChildren().add(canvas);

		return layout;
	}

	public void setColor(double r, double g, double b, double a) {
		graphics.setFill(Color.rgb(
				(int) (r * 255),
				(int) (g * 255),
				(int) (b * 255),
				a
		));
	}

	public void drawLine(double x1, double y1, double x2, double y2) {
		graphics.strokeLine(x1, y1, x2, y2);
	}

	public void drawRect(double x, double y, double w, double h) {
		graphics.fillRect(x, y, w, h);
	}

	public void drawOval(double x, double y, double w, double h) {
		graphics.fillOval(x, y, w, h);
	}

	public void drawCircle(double x, double y, double r) {
		graphics.fillOval(x - r / 2, y - r / 2, r, r);
	}

	public void close() {
		Platform.runLater(stage::close);
	}
}
