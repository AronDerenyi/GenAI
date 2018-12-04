package com.madebyaron.genai.app;

import com.madebyaron.genai.app.views.StartView;
import javafx.application.Application;
import javafx.stage.Stage;

public final class App extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) {
		new StartView(stage);
	}
}
