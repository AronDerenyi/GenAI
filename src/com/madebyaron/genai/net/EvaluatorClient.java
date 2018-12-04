package com.madebyaron.genai.net;

import com.madebyaron.genai.ai.Network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The client that handles network evaluations from a given host.
 */
public final class EvaluatorClient {

	private static final int VALIDATION_TIMEOUT = 5000;

	private final Socket socket;
	private final EvaluationHandler handler;

	private final ConnectionManager connectionManager;

	private String process;

	private final Thread requestHandlingThread = new Thread(this::handleRequest);
	private final Thread evaluationHandlingThread = new Thread(this::handleEvaluation);

	private final BlockingQueue<Network> pendingNetworks = new LinkedBlockingQueue<>();
	private final BlockingQueue<Long> pendingNetworkIDs = new LinkedBlockingQueue<>();
	private final Collection<Network> unmodifiablePendingNetworks = Collections.unmodifiableCollection(pendingNetworks);

	/**
	 * Creates a client that handles evaluations from a given host.
	 *
	 * @param socket The socket connected to a host
	 * @param handler The handler that handles the network evaluations
	 */
	public EvaluatorClient(Socket socket, EvaluationHandler handler) {
		this.socket = socket;
		this.handler = handler;

		connectionManager = new ConnectionManager(socket);
		connectionManager.addListener(exception -> evaluationHandlingThread.interrupt());
		connectionManager.perform((in, out) -> {
			socket.setSoTimeout(VALIDATION_TIMEOUT);

			process = in.readUTF();
			out.writeUTF(process);
			out.flush();

			socket.setSoTimeout(0);

			requestHandlingThread.start();
			evaluationHandlingThread.start();
		});
	}

	/**
	 * Returns the connection socket's address.
	 *
	 * @return The address as an InetAddress
	 */
	public InetAddress getAddress() {
		return socket.getInetAddress();
	}

	/**
	 * Returns the connection socket's port.
	 *
	 * @return The port as an int
	 */
	public int getPort() {
		return socket.getPort();
	}

	/**
	 * Returns the evaluator process' name
	 *
	 * @return The process' name
	 */
	public String getProcess() {
		return process;
	}

	/**
	 * Returns networks waiting to be evaluated.
	 *
	 * @return An unmodifiable version of the pending networks
	 */
	public Collection<Network> getPendingNetworks() {
		return unmodifiablePendingNetworks;
	}

	/**
	 * Adds a connection listener to this client
	 *
	 * @param listener The listener
	 */
	public void addConnectionListener(ConnectionManager.Listener listener) {
		connectionManager.addListener(listener);
	}

	/**
	 * Removes a connection listener from this client
	 *
	 * @param listener The listener
	 */
	public void removeConnectionListener(ConnectionManager.Listener listener) {
		connectionManager.removeListener(listener);
	}

	/**
	 * Disconnects from the host.
	 */
	public void disconnect() {
		connectionManager.disconnect();
	}

	/**
	 * Disconnects from the host with an error message.
	 *
	 * @param message The error message
	 */
	public void disconnect(String message) {
		connectionManager.perform((in, out) -> {
			synchronized (connectionManager) {
				out.writeByte(1);
				out.writeUTF(message);
				out.flush();
			}
		});

		connectionManager.disconnect();
	}

	/**
	 * A loop that receives evaluation requests from the host.
	 */
	private void handleRequest() {
		while (connectionManager.isConnected()) {
			connectionManager.perform((in, out) -> {
				try {
					if (in.readByte() == 0) {
						long networkID = in.readLong();
						Network network = (Network) in.readObject();

						pendingNetworks.add(network);
						pendingNetworkIDs.add(networkID);
					} else {
						String message = in.readUTF();
						throw new IOException("Server error: " + message);
					}
				} catch (ClassNotFoundException e) {
					disconnect("Received class not found: " + e.getMessage());
				}
			});
		}
	}

	/**
	 * A loop that handles and sends evaluations to the host.
	 */
	private void handleEvaluation() {
		while (connectionManager.isConnected()) {
			connectionManager.perform((in, out) -> {
				try {
					Network network;
					long networkID;

					network = pendingNetworks.take();
					networkID = pendingNetworkIDs.take();

					double evaluation = handler.evaluate(network);

					synchronized (connectionManager) {
						out.writeByte(0);
						out.writeLong(networkID);
						out.writeDouble(evaluation);
						out.flush();
					}
				} catch (EvaluationException e) {
					disconnect(e.getMessage());
				} catch (InterruptedException ignored) {

				}
			});
		}
	}

	/**
	 * The handler that handles network evaluations
	 */
	public interface EvaluationHandler {

		/**
		 * Evaluates the given network.
		 *
		 * @param network The network to be evaluated
		 * @return The given network's evaluation as a double value
		 * @throws EvaluatorClient.EvaluationException Any exception that happens while evaluating
		 */
		double evaluate(Network network) throws EvaluationException;
	}

	/**
	 * An exception thrown by the evaluation handler
	 */
	public static class EvaluationException extends Exception {

		public EvaluationException() {

		}

		public EvaluationException(String message) {
			super(message);
		}
	}
}
