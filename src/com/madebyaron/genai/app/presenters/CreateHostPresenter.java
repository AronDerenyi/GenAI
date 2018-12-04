package com.madebyaron.genai.app.presenters;

import com.madebyaron.genai.ai.Pool;
import com.madebyaron.genai.ai.Process;
import com.madebyaron.genai.app.views.CreateHostView;

/**
 * The host creation view's presenter. Handles the conversion of
 * parameters and the creation of the host pool and process.
 */
public final class CreateHostPresenter {

	private CreateHostView view;

	private int port;

	/**
	 * Attaches a view to this presenter.
	 *
	 * @param view The attached view
	 */
	public void attachView(CreateHostView view) {
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
	 * Initiates the creation of a new host.
	 * Also checks if the given parameters are valid.
	 *
	 * @param processName The host process' name
	 * @param poolSize The host pool's size
	 */
	public void create(String processName, String poolSize) {
		if (processName.isEmpty()) {
			view.showError("Wrong parameters", "Process field is empty.");
			return;
		}

		if (poolSize.isEmpty()) {
			view.showError("Wrong parameters", "Pool size field is empty.");
			return;
		}

		try {
			Process process = Process.getProcess(processName);
			if (process == null) {
				view.showError("Wrong parameters", "The process can't be found.");
				return;
			}

			int size = Integer.parseInt(poolSize);

			Pool pool = new Pool(process, size);
			view.openHost(port, pool);
		} catch (NumberFormatException e) {
			view.showError("Wrong parameters", "The pool size must be a number.");
		}
	}
}
