package com.madebyaron.genai.app.presenters;

import com.madebyaron.genai.ai.Network;
import com.madebyaron.genai.ai.Pool;
import com.madebyaron.genai.ai.Process;
import com.madebyaron.genai.app.views.OpenHostView;

import java.io.*;
import java.util.Collection;
import java.util.LinkedList;

/**
 * The presenter for the host opening view.
 * Handles the loading of the host pool and the creation of the host process.
 */
public final class OpenHostPresenter {

	private OpenHostView view;

	private int port;

	/**
	 * Attaches a view to this presenter.
	 *
	 * @param view The attached view
	 */
	public void attachView(OpenHostView view) {
		this.view = view;
	}

	/**
	 * Sets the host server's port.
	 *
	 * @param port The host server's port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Initiates the loading of the host saved at the given file.
	 *
	 * @param file The save file
	 */
	public void load(File file) {
		view.setLoading(file.getAbsolutePath());

		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));

			Thread loadingThread = new Thread(() -> {
				try {
					load(in);
				} catch (IOException | ClassNotFoundException e) {
					view.showError(
							"Error while loading file",
							e.getClass().getSimpleName() + ": " + e.getMessage()
					);
				}
			});

			loadingThread.start();
		} catch (IOException e) {
			view.showError(
					"Error while opening file",
					e.getClass().getSimpleName() + ": " + e.getMessage()
			);
		}
	}

	/**
	 * Loads the pool from the given input stream and creates the process.
	 * Also updates the progress in the view.
	 *
	 * @param in The input stream
	 * @throws IOException Any exception thrown when reading the input stream
	 * @throws ClassNotFoundException An exception thrown when the saved network class can't be found
	 */
	private void load(ObjectInputStream in) throws IOException, ClassNotFoundException {
		view.setProgress("Loading pool data", 0);

		Process process = Process.getProcess(in.readUTF());
		if (process == null) {
			view.showError("Wrong parameters", "The process can't be found.");
			return;
		}

		int networkCount = in.readInt();
		Collection<Network> networks = new LinkedList<>();
		for (int i = 0; i < networkCount; i++) {
			view.setProgress(
					"Loading networks (" + i + " / " + networkCount + ")",
					(double) i / networkCount
			);
			networks.add((Network) in.readObject());
		}

		view.setProgress("Done", 1);

		Pool pool = new Pool(process, networks);

		view.openHost(port, pool);
	}
}
