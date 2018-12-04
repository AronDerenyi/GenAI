package com.madebyaron.genai.app.presenters;

import com.madebyaron.genai.ai.Network;
import com.madebyaron.genai.ai.Pool;
import com.madebyaron.genai.app.views.SaveHostView;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The presenter for the host saving view.
 */
public final class SaveHostPresenter {

	private SaveHostView view;

	/**
	 * Attaches a view to this presenter.
	 *
	 * @param view The attached view
	 */
	public void attachView(SaveHostView view) {
		this.view = view;
	}

	/**
	 * Initiates the saving of a pool defined by a process name and it's networks.
	 *
	 * @param file The save file
	 * @param process The pool process' name
	 * @param networks The pool's networks
	 */
	public void save(File file, String process, Collection<Network> networks) {
		view.setSaving(file.getAbsolutePath());

		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));

			Thread loadingThread = new Thread(() -> {
				try {
					save(out, process, networks);
				} catch (IOException e) {
					e.printStackTrace();
					view.showError(
							"Error while saving file",
							e.getClass().getSimpleName() + ": " + e.getMessage()
					);
				}
			});

			loadingThread.start();
		} catch (IOException e) {
			e.printStackTrace();
			view.showError(
					"Error while opening file",
					e.getClass().getSimpleName() + ": " + e.getMessage()
			);
		}
	}

	/**
	 * Saves a pool defined by a process name and it's networks to the given output stream.
	 * Also updates the progress in the view.
	 *
	 * @param out The output stream where the pool will be saved
	 * @param process The pool process' name
	 * @param networks The pool's networks
	 * @throws IOException Any exception thrown while saving
	 */
	private void save(ObjectOutputStream out, String process, Collection<Network> networks) throws IOException {
		view.setProgress("Saving pool data", 0);

		out.writeUTF(process);

		out.writeInt(networks.size());
		int savedNetworks = 0;
		for (Network network : networks) {
			view.setProgress(
					"Saving networks (" + savedNetworks + " / " + networks.size() + ")",
					(double) savedNetworks / networks.size()
			);
			out.writeObject(network);
			savedNetworks++;
		}

		view.setProgress("Done", 1);

		view.close();
	}
}
