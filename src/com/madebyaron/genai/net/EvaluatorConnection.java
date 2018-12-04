package com.madebyaron.genai.net;

import com.madebyaron.genai.ai.Network;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

/**
 * This is the server side connection of a connected client.
 */
public final class EvaluatorConnection {

	private static final int VALIDATION_TIMEOUT = 5000;
	private static final Random RANDOM = new Random();

	private final Socket socket;
	private final String process;

	private final ConnectionManager connectionManager;

	private final Thread responseHandlingThread = new Thread(this::handleResponse);

	private final List<Listener> listeners = new LinkedList<>();

	private final Map<Long, Network> pendingNetworks = new HashMap<>();
	private final Collection<Network> unmodifiablePendingNetworks = Collections.unmodifiableCollection(pendingNetworks.values());

	/**
	 * Creates a connection from the connection's socket and the host process name.
	 *
	 * @param socket The connection's socket
	 * @param process The host process
	 */
	public EvaluatorConnection(Socket socket, String process) {
		this.socket = socket;
		this.process = process;

		connectionManager = new ConnectionManager(socket);
		connectionManager.perform((in, out) -> {
			socket.setSoTimeout(VALIDATION_TIMEOUT);

			out.writeUTF(process);
			out.flush();
			if (!in.readUTF().equals(process)) {
				throw new IOException("Invalid connection");
			}

			socket.setSoTimeout(0);

			responseHandlingThread.start();
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
		synchronized (pendingNetworks) {
			return unmodifiablePendingNetworks;
		}
	}

	/**
	 * Returns the number of pending networks representing
	 * the load on the connected client.
	 *
	 * @return The load on the connected client
	 */
	public int getLoad() {
		synchronized (pendingNetworks) {
			return pendingNetworks.size();
		}
	}

	/**
	 * Adds an evaluation listener to this connection
	 *
	 * @param listener The listener
	 */
	public void addListener(Listener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	/**
	 * Removes an evaluation listener from this connection
	 *
	 * @param listener The listener
	 */
	public void removeListener(Listener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	/**
	 * Adds a connection listener to this connection
	 *
	 * @param listener The listener
	 */
	public void addConnectionListener(ConnectionManager.Listener listener) {
		connectionManager.addListener(listener);
	}

	/**
	 * Removes a connection listener from this connection
	 *
	 * @param listener The listener
	 */
	public void removeConnectionListener(ConnectionManager.Listener listener) {
		connectionManager.removeListener(listener);
	}

	/**
	 * Disconnects from the client.
	 */
	public void disconnect() {
		connectionManager.disconnect();
	}

	/**
	 * Disconnects from the client with an error message.
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
	 * Initiates the evaluation of the given network.
	 *
	 * @param network The network to be evaluated
	 */
	public void evaluate(Network network) {
		long networkID;

		synchronized (pendingNetworks) {
			long uniqueID;
			do {
				uniqueID = RANDOM.nextLong();
			} while (pendingNetworks.containsKey(uniqueID));
			networkID = uniqueID;
		}

		synchronized (pendingNetworks) {
			pendingNetworks.put(networkID, network);
		}

		synchronized (listeners) {
			for (Listener listener : listeners) {
				listener.onEvaluationStared(network);
			}
		}

		connectionManager.perform((in, out) -> {
			synchronized (connectionManager) {
				out.writeByte(0);
				out.writeLong(networkID);
				out.writeObject(network);
				out.flush();
			}
		});
	}

	/**
	 * A loop that receives finished evaluations from the client.
	 */
	private void handleResponse() {
		while (connectionManager.isConnected()) {
			connectionManager.perform((in, out) -> {
				if (in.readByte() == 0) {
					long networkID = in.readLong();
					double evaluation = in.readDouble();
					Network network;

					synchronized (pendingNetworks) {
						network = pendingNetworks.remove(networkID);
					}

					synchronized (listeners) {
						for (Listener listener : listeners) {
							listener.onEvaluationFinished(network, evaluation);
						}
					}
				} else {
					String message = in.readUTF();
					throw new IOException("Client error: " + message);
				}
			});
		}
	}

	/**
	 * A listener that is listening for the start and finish of networks' evaluation.
	 */
	public interface Listener {

		/**
		 * Called when a network's evaluation starts.
		 *
		 * @param network The network
		 */
		void onEvaluationStared(Network network);

		/**
		 * Called when a network is finished evaluating.
		 *
		 * @param network The network
		 * @param evaluation The network's evaluation
		 */
		void onEvaluationFinished(Network network, double evaluation);
	}
}
