package com.madebyaron.genai.app.presenters;

import com.madebyaron.genai.app.views.StartView;

import java.io.File;
import java.net.*;

/**
 * The presenter for the starting view.
 * Handles the validation of the parameters.
 */
public final class StartPresenter {

	private StartView view;

	/**
	 * Attaches a view to this presenter.
	 *
	 * @param view The attached view
	 */
	public void attachView(StartView view) {
		this.view = view;
	}

	/**
	 * Initiates the connection to a host defined by it's address and port.
	 * Also checks if the given parameters are valid.
	 *
	 * @param hostAddress The host address
	 * @param hostPort The host port
	 */
	public void connect(String hostAddress, String hostPort) {
		if (hostAddress.isEmpty()) {
			view.showError("Wrong parameters", "Host address field is empty.");
			return;
		}
		if (hostPort.isEmpty()) {
			view.showError("Wrong parameters", "Host port field is empty.");
			return;
		}

		try {
			view.openClient(new InetSocketAddress(
					InetAddress.getByName(hostAddress),
					Integer.parseInt(hostPort)
			));
		} catch (UnknownHostException e) {
			view.showError("Unknown host address", e.getClass().getSimpleName() + ": " + e.getMessage());
		} catch (NumberFormatException e) {
			view.showError("Wrong parameters", "The port must be a number.");
		}
	}

	/**
	 * Initiates the opening of an already existing host on the given port from the give file.
	 * Also checks if the given parameters are valid.
	 *
	 * @param hostPort The host's port
	 * @param file The host's save file
	 */
	public void openHost(String hostPort, File file) {
		if (hostPort.isEmpty()) {
			view.showError("Wrong parameters", "Host port field is empty.");
			return;
		}

		try {
			view.openOpenHost(Integer.parseInt(hostPort), file);
		} catch (NumberFormatException e) {
			view.showError("Wrong parameters", "The port must be a number");
		}
	}

	/**
	 * Initiates the creation of a new host on the given port.
	 * Also checks if the given parameters are valid.
	 *
	 * @param hostPort The host's port
	 */
	public void createHost(String hostPort) {
		if (hostPort.isEmpty()) {
			view.showError("Wrong parameters", "Host port field is empty.");
			return;
		}

		try {
			view.openCreateHost(Integer.parseInt(hostPort));
		} catch (NumberFormatException e) {
			view.showError("Wrong parameters", "The port must be a number.");
		}
	}
}
