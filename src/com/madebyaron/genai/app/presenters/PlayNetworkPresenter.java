package com.madebyaron.genai.app.presenters;

import com.madebyaron.genai.ai.Network;
import com.madebyaron.genai.ai.Process;
import com.madebyaron.genai.app.views.PlayNetworkView;

/**
 * The presenter of the view capable of drawing a networks evaluation.
 */
public final class PlayNetworkPresenter {

	private PlayNetworkView view;
	private Process process;
	private Network network;

	private Process.Data data;
	private Graphics graphics = new Graphics();

	/**
	 * Attaches a view to this presenter.
	 *
	 * @param view The attached view
	 */
	public void attachView(PlayNetworkView view) {
		this.view = view;
	}

	/**
	 * Sets the process, that evaluates the given network.
	 *
	 * @param process The process
	 */
	public void setProcess(Process process) {
		this.process = process;
		data = process.createData();
	}

	/**
	 * Sets the network that will be evaluated.
	 *
	 * @param network The network
	 */
	public void setNetwork(Network network) {
		this.network = network;
	}

	/**
	 * This is called when the screen size changes.
	 *
	 * @param width Screen width
	 * @param height Screen height
	 */
	public void setSize(double width, double height) {
		graphics.width = width;
		graphics.height = height;
	}

	/**
	 * Ticks the process and renders a frame.
	 */
	public void tick() {
		process.tick(network, data, graphics);
		if (data.isEvaluated()) view.close();
	}

	/**
	 * A graphics class implementation capable of drawing to the view.
	 */
	private final class Graphics extends Process.Graphics {

		private double width = 0;
		private double height = 0;

		@Override
		public double getWidth() {
			return width;
		}

		@Override
		public double getHeight() {
			return height;
		}

		@Override
		public void drawLine(double x1, double y1, double x2, double y2) {
			view.setColor(getColor().r, getColor().g, getColor().b, getColor().a);
			view.drawLine(x1, y1, x2, y2);
		}

		@Override
		public void drawRect(double x, double y, double w, double h) {
			view.setColor(getColor().r, getColor().g, getColor().b, getColor().a);
			view.drawRect(x, y, w, h);
		}

		@Override
		public void drawOval(double x, double y, double w, double h) {
			view.setColor(getColor().r, getColor().g, getColor().b, getColor().a);
			view.drawOval(x, y, w, h);
		}

		@Override
		public void drawCircle(double x, double y, double r) {
			view.setColor(getColor().r, getColor().g, getColor().b, getColor().a);
			view.drawCircle(x, y, r);
		}
	}
}
