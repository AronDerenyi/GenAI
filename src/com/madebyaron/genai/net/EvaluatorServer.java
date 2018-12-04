package com.madebyaron.genai.net;

import com.madebyaron.genai.ai.Network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.*;

/**
 * This is the server that handles connecting clients
 * and distributes the evaluating network between them.
 */
public final class EvaluatorServer {

	private static final int MAX_CONNECTION_LOAD = 10;

	private final ServerSocket serverSocket;
	private final String process;

	private final Thread connectionHandlingThread = new Thread(this::handleConnections);

	private final List<Listener> listeners = new LinkedList<>();
	private final List<EvaluatorConnection.Listener> evaluationListeners = new LinkedList<>();

	private final Collection<EvaluatorConnection> connections = new LinkedList<>();
	private final Collection<EvaluatorConnection> unmodifiableConnections = Collections.unmodifiableCollection(connections);

	private final Collection<Network> pendingNetworks = new HashSet<>();
	private final Collection<Network> unmodifiablePendingNetworks = Collections.unmodifiableCollection(pendingNetworks);

	private final Queue<Network> bufferedNetworks = new LinkedList<>();

	/**
	 * The server socket on which the server will be listening.
	 *
	 * @param serverSocket The server socket
	 * @param process The evaluator process' name
	 */
	public EvaluatorServer(ServerSocket serverSocket, String process) {
		this.serverSocket = serverSocket;
		this.process = process;

		connectionHandlingThread.start();
	}

	/**
	 * Returns the server socket's address.
	 *
	 * @return The address as an InetAddress
	 */
	public InetAddress getAddress() {
		try {
			return InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			return null;
		}
	}

	/**
	 * Returns the server socket's port.
	 *
	 * @return The port as an int
	 */
	public int getPort() {
		return serverSocket.getLocalPort();
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
	 * Returns the currently connected connections.
	 *
	 * @return An unmodifiable version of the connected connections
	 */
	public Collection<EvaluatorConnection> getConnections() {
		synchronized (connections) {
			return unmodifiableConnections;
		}
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
	 * the load on the server.
	 *
	 * @return The load on the server
	 */
	public int getLoad() {
		synchronized (pendingNetworks) {
			return pendingNetworks.size();
		}
	}

	/**
	 * Adds a connection listener to this server
	 *
	 * @param listener The listener
	 */
	public void addListener(Listener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	/**
	 * Removes a connection listener from this server
	 *
	 * @param listener The listener
	 */
	public void removeListener(Listener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	/**
	 * Adds an evaluation listener to this server
	 *
	 * @param listener The listener
	 */
	public void addEvaluationListener(EvaluatorConnection.Listener listener) {
		synchronized (evaluationListeners) {
			evaluationListeners.add(listener);
		}
	}

	/**
	 * Removes an evaluation listener from this server
	 *
	 * @param listener The listener
	 */
	public void removeEvaluationListener(EvaluatorConnection.Listener listener) {
		synchronized (evaluationListeners) {
			evaluationListeners.remove(listener);
		}
	}

	/**
	 * Initiates the evaluation of the given network.
	 *
	 * @param network The network to be evaluated
	 */
	public void evaluate(Network network) {
		synchronized (pendingNetworks) {
			if (pendingNetworks.contains(network)) {
				return;
			} else {
				pendingNetworks.add(network);
			}
		}

		synchronized (evaluationListeners) {
			for (EvaluatorConnection.Listener listener : evaluationListeners) {
				listener.onEvaluationStared(network);
			}
		}

		EvaluatorConnection leastLoadedConnection = null;
		synchronized (connections) {
			for (EvaluatorConnection connection : connections) {
				if (leastLoadedConnection == null || connection.getLoad() < leastLoadedConnection.getLoad()) {
					leastLoadedConnection = connection;
				}
			}
		}

		if (leastLoadedConnection != null && leastLoadedConnection.getLoad() < MAX_CONNECTION_LOAD) {
			leastLoadedConnection.evaluate(network);
		} else {
			synchronized (bufferedNetworks) {
				bufferedNetworks.add(network);
			}
		}
	}

	/**
	 * Closes the server and every connection.
	 */
	public void close() {
		try {
			serverSocket.close();
		} catch (IOException ignored) {

		}

		synchronized (connections) {
			for (EvaluatorConnection connection : new LinkedList<>(connections)) {
				connection.disconnect();
			}
		}
	}

	/**
	 * A loop that listens to incoming connections.
	 */
	private void handleConnections() {
		try {
			while (!serverSocket.isClosed()) {
				EvaluatorConnection connection = new EvaluatorConnection(serverSocket.accept(), process);
				connection.addConnectionListener(createConnectionListener(connection));
				connection.addListener(createEvaluationListener(connection));

				synchronized (connections) {
					connections.add(connection);
				}

				synchronized (listeners) {
					for (Listener listener : listeners) {
						listener.onConnected(connection);
					}
				}

				while (connection.getLoad() < MAX_CONNECTION_LOAD) {
					synchronized (bufferedNetworks) {
						if (bufferedNetworks.size() > 0) {
							connection.evaluate(bufferedNetworks.remove());
						} else {
							break;
						}
					}
				}
			}
		} catch (IOException e) {
			synchronized (listeners) {
				for (Listener listener : listeners) {
					listener.onError(e);
				}
			}
		}
	}

	/**
	 * Creates a connection listener that listens for the connection state of the given connection.
	 *
	 * @param connection The connection
	 * @return The new listener
	 */
	private ConnectionManager.Listener createConnectionListener(EvaluatorConnection connection) {
		return exception -> {
			synchronized (listeners) {
				for (Listener listener : listeners) {
					listener.onDisconnected(connection, exception);
				}
			}

			synchronized (connections) {
				connections.remove(connection);
			}

			// Watch out for this nested synchronization! (possible deadlock)
			for (Network network : connection.getPendingNetworks()) {
				synchronized (pendingNetworks) {
					if (pendingNetworks.contains(network)) {
						synchronized (bufferedNetworks) {
							bufferedNetworks.add(network);
						}
					}
				}
			}
		};
	}

	/**
	 * Creates an evaluation listener that listens for the evaluation callbacks of the given connection.
	 *
	 * @param connection The connection
	 * @return The new listener
	 */
	private EvaluatorConnection.Listener createEvaluationListener(EvaluatorConnection connection) {
		return new EvaluatorConnection.Listener() {

			@Override
			public void onEvaluationStared(Network network) {

			}

			@Override
			public void onEvaluationFinished(Network network, double evaluation) {
				boolean contains;
				synchronized (pendingNetworks) {
					contains = pendingNetworks.remove(network);
				}

				if (contains) {
					synchronized (evaluationListeners) {
						for (EvaluatorConnection.Listener listener : evaluationListeners) {
							listener.onEvaluationFinished(network, evaluation);
						}
					}
				}

				while (connection.getLoad() < MAX_CONNECTION_LOAD) {
					Network bufferedNetwork;
					synchronized (bufferedNetworks) {
						bufferedNetwork = bufferedNetworks.poll();
					}

					if (bufferedNetwork != null) {
						connection.evaluate(bufferedNetwork);
					} else {
						break;
					}
				}
			}
		};
	}

	/**
	 * A listener that is listening for the state changes of the server's connections and the server's errors.
	 */
	public interface Listener {

		/**
		 * Gets called if a new connection is received.
		 *
		 * @param connection The connection
		 */
		void onConnected(EvaluatorConnection connection);

		/**
		 * Gets called if a connection ends.
		 *
		 * @param connection The connection
		 * @param exception The exception that caused the connection to end
		 */
		void onDisconnected(EvaluatorConnection connection, IOException exception);

		/**
		 * Gets called if an error occurs.
		 *
		 * @param exception The exception causing the error
		 */
		void onError(IOException exception);
	}
}
