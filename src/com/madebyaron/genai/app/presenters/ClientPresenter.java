package com.madebyaron.genai.app.presenters;

import com.madebyaron.genai.ai.Network;
import com.madebyaron.genai.ai.Process;
import com.madebyaron.genai.app.views.ClientView;
import com.madebyaron.genai.net.EvaluatorClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * The client view's presenter. Handles the client connection and the evaluation.
 */
public final class ClientPresenter {

	private static final int CONNECTION_TIMEOUT = 2000;

	private ClientView view;

	private EvaluatorClient client = null;
	private Process process = null;

	/**
	 * Attaches a view to this presenter.
	 *
	 * @param view The attached view
	 */
	public void attachView(ClientView view) {
		this.view = view;
	}

	/**
	 * Starts a connection with the host on the given socket address.
	 *
	 * @param address The host's socket address
	 */
	public void start(InetSocketAddress address) {
		if (client != null) throw new IllegalStateException("The server has already been started.");

		try {
			Socket socket = new Socket();
			socket.connect(address, CONNECTION_TIMEOUT);

			client = new EvaluatorClient(socket, this::evaluate);
			client.addConnectionListener(exception -> {
				if (exception != null) {
					exception.printStackTrace();
					view.showError("Client disconnected", exception.getClass().getSimpleName() + ": " + exception.getMessage());
				}
				view.close();
			});

			view.setAddress(client.getAddress().getHostAddress(), client.getPort());
			view.setProcess(client.getProcess());
			view.setStatus("idle");
		} catch (IOException e) {
			e.printStackTrace();
			view.showError("Couldn't connect to server", e.getClass().getSimpleName() + ": " + e.getMessage());
			view.close();
		}
	}

	/**
	 * Stops the client connection and disconnects from the server.
	 */
	public void stop() {
		if (client != null) {
			client.disconnect();
			client = null;
		}
	}

	/**
	 * Evaluates the given network.
	 *
	 * @param network The network to be evaluated
	 * @return The given network's evaluation as a double value
	 * @throws EvaluatorClient.EvaluationException Any exception that happens while evaluating
	 */
	private double evaluate(Network network) throws EvaluatorClient.EvaluationException {
		if (process == null || !process.getName().equals(client.getProcess())) {
			process = Process.getProcess(client.getProcess());
		}

		if (process == null) {
			view.showError("Error while evaluating", "The process can't be found.");
			throw new EvaluatorClient.EvaluationException("The process can't be found.");
		}

		try {
			view.setStatus("evaluating");
			Process.Data data = process.createData();
			while (!data.isEvaluated()) process.tick(network, data, Process.Graphics.NONE);
			view.setStatus("idle");
			return data.getEvaluation();
		} catch (Exception e) {
			view.showError("Error while evaluating", e.getClass().getSimpleName() + ": " + e.getMessage());
			throw new EvaluatorClient.EvaluationException(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
}
