package com.madebyaron.genai.net;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

/**
 * A wrapper class that handles a socket's connection.
 */
public final class ConnectionManager {

	private final Socket socket;

	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;

	private boolean connected;

	private final List<Listener> listeners = new LinkedList<>();

	private IOException disconnectionException = null;

	/**
	 * Creates a connection manager that handles the connection on the given socket.
	 *
	 * @param socket The connection's socket
	 */
	public ConnectionManager(Socket socket) {
		this.socket = socket;
		connected = true;

		perform((in, out) -> {
			this.out = new ObjectOutputStream(socket.getOutputStream());
			this.in = new ObjectInputStream(socket.getInputStream());
		});
	}

	/**
	 * Returns whether or not the socket is still connected to the server.
	 *
	 * @return The socket's connection status
	 */
	public boolean isConnected() {
		synchronized (socket) {
			return connected;
		}
	}

	/**
	 * Adds a connection listener to this connection.
	 *
	 * @param listener The connection listener
	 */
	public void addListener(Listener listener) {
		synchronized (listeners) {
			if (!listeners.contains(listener)) {
				listeners.add(listener);

				if (!isConnected()) {
					listener.onDisconnected(disconnectionException);
				}
			}
		}
	}

	/**
	 * Removes a connection listener from this connection.
	 *
	 * @param listener The connection listener
	 */
	public void removeListener(Listener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	/**
	 * Performs a task. If the task throws an exception the connection closes.
	 *
	 * @param task The task
	 */
	public void perform(Task task) {
		if (!isConnected()) return;

		try {
			task.perform(in, out);
		} catch (EOFException e) {
			disconnect();
		} catch (IOException e) {
			disconnect(e);
		}
	}

	/**
	 * Disconnects and notifies the listeners.
	 *
	 * @param exception The exception that caused the connection to end
	 */
	private void disconnect(IOException exception) {
		synchronized (listeners) {
			synchronized (socket) {
				if (!connected) return;
				connected = false;

				disconnectionException = exception;

				try {
					socket.close();
				} catch (IOException ignored) {

				}

				for (Listener listener : listeners) {
					listener.onDisconnected(exception);
				}
			}
		}
	}

	/**
	 * Closes the connection.
	 */
	public void disconnect() {
		disconnect(null);
	}

	/**
	 * A task that can be performed by a connection manager.
	 */
	public interface Task {

		/**
		 * The body of the task. Any I/O operation should be done here.
		 * If an exception is thrown, de connection closes.
		 *
		 * @param in The connection's input stream
		 * @param out The connection's output stream
		 * @throws IOException The exception thrown by the task
		 */
		void perform(ObjectInputStream in, ObjectOutputStream out) throws IOException;
	}

	/**
	 * A listener that is listening for changes to the connection's state.
	 */
	public interface Listener {

		/**
		 * Gets called if the connection closes.
		 *
		 * @param exception The exception that closed the connection
		 */
		void onDisconnected(IOException exception);
	}
}
