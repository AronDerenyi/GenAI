package com.madebyaron.genai.app.presenters;

import com.madebyaron.genai.ai.Pool;
import com.madebyaron.genai.app.views.HostView;
import com.madebyaron.genai.net.EvaluatorConnection;
import com.madebyaron.genai.net.EvaluatorServer;
import com.madebyaron.genai.ai.Network;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;

/**
 * The host view's presenter. Handles the host server, pool and the evaluation.
 */
public final class HostPresenter{

	private HostView view;
	private Pool pool;
	private EvaluatorServer server;

	private Pool.Evaluation evaluation;
	private boolean evaluating = false;
	private boolean evaluationStopped = false;

	private Network bestNetwork;

	private final EvaluatorServer.Listener serverListener = new ServerListener();
	private final EvaluatorConnection.Listener evaluationListener = new EvaluationListener();

	/**
	 * Attaches a view to this presenter.
	 *
	 * @param view The attached view
	 */
	public void attachView(HostView view) {
		this.view = view;
	}

	/**
	 * Sets the given pool as the host's pool.
	 *
	 * @param pool The set pool
	 */
	public void setPool(Pool pool) {
		this.pool = pool;
	}

	/**
	 * Starts the host on the given server port.
	 *
	 * @param port The server port
	 */
	public void startServer(int port) {
		if (server != null) throw new IllegalStateException("The server has already been started.");

		try {
			server = new EvaluatorServer(new ServerSocket(port), pool.getProcess().getName());
			server.addListener(serverListener);
			server.addEvaluationListener(evaluationListener);

			view.setProcess(server.getProcess());
			InetAddress hostAddress = server.getAddress();
			view.setHostAddress(hostAddress == null ? "Unknown" : hostAddress.getHostAddress(), server.getPort());
			view.setHostLoad(server.getLoad());

			view.setSaveEnabled(true);
			view.setPlayEnabled(false);
		} catch (IOException e) {
			e.printStackTrace();
			view.showError("Couldn't create server", e.getClass().getSimpleName() + ": " + e.getMessage());
			view.close();
		}
	}

	/**
	 * Stops the server from running.
	 */
	public void stopServer() {
		if (server != null) {
			server.close();
			server = null;
		}
	}

	/**
	 * Initiates the saving of the pool and it's process to the given file.
	 *
	 * @param file The save file
	 */
	public void save(File file) {
		if (evaluating) {
			view.showError("Can't save host", "Wait for the evaluation to finish.");
		} else {
			view.openSaveHost(file,
					pool.getProcess().getName(),
					new ArrayList<>(pool.getNetworks())
			);
		}
	}

	/**
	 * Initiates the playing of the best performing neural network.
	 */
	public void play() {
		if (bestNetwork != null) {
			view.openPlayNetwork(pool.getProcess(), bestNetwork);
		} else {
			view.showError("Can't play network", "Run an evaluation first to determine the best network.");
		}
	}

	/**
	 * Starts the evaluation if it's not running already.
	 */
	public void startEvaluation() {
		if (!evaluating) {
			evaluating = true;
			evaluation = pool.createEvaluation();
			view.setSaveEnabled(false);

			for (Network network : pool.getNetworks()) {
				server.evaluate(network);
			}
		}

		evaluationStopped = false;
	}

	/**
	 * Stops the evaluation but only after finishing the current evaluation.
	 */
	public void stopEvaluation() {
		if (evaluating) {
			evaluationStopped = true;
		}
	}

	/**
	 * Called when the evaluation has finished.
	 * If the host is still evaluating it resets and starts the evaluation again.
	 */
	private void onEvaluationDone() {
		bestNetwork = evaluation.getBestNetwork();
		view.setBestEvaluation(evaluation.getBestEvaluation());

		pool.generation(evaluation, 0.5);
		evaluating = false;
		evaluation = null;
		view.setSaveEnabled(true);
		view.setPlayEnabled(true);

		if (evaluationStopped) {
			evaluationStopped = false;
		} else {
			startEvaluation();
		}
	}

	/**
	 * This is the class handling the evaluation callbacks of the server.
	 */
	private class EvaluationListener implements EvaluatorConnection.Listener {

		/**
		 * Updates the server load in the view.
		 *
		 * @param network The network that started evaluating
		 */
		@Override
		public void onEvaluationStared(Network network) {
			view.setHostLoad(server.getLoad());
		}

		/**
		 * Updates the server load in the view and saves the network's evaluation.
		 *
		 * @param network The evaluated network
		 * @param evaluation The network's evaluation
		 */
		@Override
		public void onEvaluationFinished(Network network, double evaluation) {
			view.setHostLoad(server.getLoad());

			if (evaluating) {
				HostPresenter.this.evaluation.setEvaluation(network, evaluation);
				if (HostPresenter.this.evaluation.isDone()) Platform.runLater(HostPresenter.this::onEvaluationDone);
			}
		}
	}

	/**
	 * This is the class handling the connection callbacks of the server.
	 */
	private class ServerListener implements EvaluatorServer.Listener {

		/**
		 * Updates the connected clients in the view and attaches
		 * an evaluation listener to the connected client.
		 *
		 * @param connection
		 */
		@Override
		public void onConnected(EvaluatorConnection connection) {
			view.addClient(connection);
			view.setClientAddress(connection, connection.getAddress().getHostAddress(), connection.getPort());
			view.setClientLoad(connection, connection.getPendingNetworks().size());

			connection.addListener(new EvaluatorConnection.Listener() {

				/**
				 * Updates the client load in the view.
				 *
				 * @param network The network that started evaluating
				 */
				@Override
				public void onEvaluationStared(Network network) {
					view.setClientLoad(connection, connection.getPendingNetworks().size());
				}

				/**
				 * Updates the client load in the view.
				 *
				 * @param network The evaluated network
				 * @param evaluation The network's evaluation
				 */
				@Override
				public void onEvaluationFinished(Network network, double evaluation) {
					view.setClientLoad(connection, connection.getPendingNetworks().size());
				}
			});
		}

		/**
		 * Updates the connected clients in the view and shows
		 * an error message if an exception occurred while disconnecting.
		 *
		 * @param connection The disconnected client's connection
		 * @param exception The exception that occurred while disconnecting
		 */
		@Override
		public void onDisconnected(EvaluatorConnection connection, IOException exception) {
			view.removeClient(connection);

			if (exception != null) {
				exception.printStackTrace();
				view.showError(
						"Client disconnected",
						exception.getClass().getSimpleName() + ": " + exception.getMessage()
				);
			}
		}

		/**
		 * Show an error message of the exception that occurred in the server.
		 *
		 * @param exception The exception
		 */
		@Override
		public void onError(IOException exception) {
			if (exception != null) {
				exception.printStackTrace();
				view.showError(
						"Server closed",
						exception.getClass().getSimpleName() + ": " + exception.getMessage()
				);
			}

			view.close();
		}
	}
}
