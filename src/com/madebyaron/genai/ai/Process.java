package com.madebyaron.genai.ai;

import com.madebyaron.genai.ai.processes.FlappyProcess;

/**
 * This class is responsible for evaluating the networks.
 * Every process should create their ow networks
 * as different processes may need different networks
 * with different neuron counts. It is also advised to
 * implement a custom evaluation data class to store
 * the evaluation's variables.
 *
 * @param <D>
 */
public interface Process<D extends Process.Data> {

	Process[] PROCESSES = {
		new FlappyProcess()
	};

	/**
	 * Returns a process by it's name.
	 *
	 * @param name The process' name
	 * @return The process with the given name
	 */
	static Process getProcess(String name) {
		for (Process process : PROCESSES) {
			if (process.getName().equals(name)) {
				return process;
			}
		}
		return null;
	}

	/**
	 * Returns the process' name.
	 *
	 * @return the process' name.
	 */
	String getName();

	/**
	 * Creates a new network suitable for this process.
	 *
	 * @return The new network
	 */
	Network createNetwork();

	/**
	 * Creates a new network suitable for this process by combining two other networks.
	 *
	 * @param networkA One of the networks to combine
	 * @param networkB One of the networks to combine
	 * @return The new network created by combining the given networks
	 */
	Network createNetwork(Network networkA, Network networkB);

	/**
	 * Creates a data object for this specific process.
	 *
	 * @return The new data object
	 */
	D createData();

	/**
	 * This is where one tick (e.g. step / frame) of the evaluation should be implemented.
	 * Any information about the evaluation should be stored in the data.
	 *
	 * @param network The evaluating network
	 * @param data The evaluation data (e.g. player position, environment variables)
	 * @param graphics The graphics class used for rendering
	 */
	void tick(Network network, D data, Graphics graphics);

	/**
	 * The process data that is carried over from one tick to the next.
	 * It also signals whether or not the network has been evaluated,
	 * and the evaluation itself.
	 */
	abstract class Data {

		private boolean evaluated = false;
		private double evaluation = 0;

		/**
		 * Sets the evaluating network's evaluation.
		 *
		 * @param evaluation The evaluation
		 */
		public final void setEvaluation(double evaluation) {
			this.evaluated = true;
			this.evaluation = evaluation;
		}

		/**
		 * Returns whether or not the network has been evaluated.
		 *
		 * @return True if the evaluating network has been evaluated.
		 */
		public final boolean isEvaluated() {
			return evaluated;
		}

		/**
		 * Returns the evaluating network's evaluation.
		 *
		 * @return The evaluation
		 */
		public final double getEvaluation() {
			return evaluation;
		}
	}

	/**
	 * This class should implement the proper drawing methods.
	 * An implementation of this class is used to drawing the evaluation.
	 */
	abstract class Graphics {

		/**
		 * This class represents a color by it's red, green, blue and alpha (opacity) component.
		 */
		public static final class Color {

			public final double r;
			public final double g;
			public final double b;
			public final double a;

			/**
			 * Creates a new color from the given color components.
			 *
			 * @param r The red component
			 * @param g The green component
			 * @param b The blue component
			 * @param a The alpha (opacity) component
			 */
			public Color(double r, double g, double b, double a) {
				this.r = r;
				this.g = g;
				this.b = b;
				this.a = a;
			}

			/**
			 * Creates a new color from the given color components.
			 * The opacity is 1 by default.
			 *
			 * @param r The red component
			 * @param g The green component
			 * @param b The blue component
			 */
			public Color(double r, double g, double b) {
				this(r, g, b, 1);
			}

			/**
			 * Creates a gray color from it's lightness.
			 * The opacity is 1 by default.
			 *
			 * @param c The lightness of the color
			 */
			public Color(double c) {
				this(c, c, c, 1);
			}
		}

		/**
		 * A graphics implementation that does nothing.
		 */
		public static final Graphics NONE = new Graphics() {
			@Override public double getWidth() { return 0; }
			@Override public double getHeight() { return 0; }
			@Override public void drawLine(double x1, double y1, double x2, double y2) { }
			@Override public void drawRect(double x, double y, double w, double h) { }
			@Override public void drawOval(double x, double y, double w, double h) { }
			@Override public void drawCircle(double x, double y, double r) { }
		};

		public static final Color BLACK = new Color(0);
		public static final Color WHITE = new Color(1);
		public static final Color RED = new Color(1, 0, 0);
		public static final Color ORANGE = new Color(1, 0.5, 0);
		public static final Color YELLOW = new Color(1, 1, 0);
		public static final Color LIME = new Color(0.5, 1, 0);
		public static final Color GREEN = new Color(0, 1, 0);
		public static final Color CYAN = new Color(0, 1, 1);
		public static final Color BLUE = new Color(0, 0, 1);

		private Color color = BLACK;

		/**
		 * Sets the current drawing color.
		 *
		 * @param color The drawing color
		 */
		public final void setColor(Color color) {
			this.color = color;
		}

		/**
		 * Returns the current drawing color.
		 *
		 * @return The drawing color
		 */
		public final Color getColor() {
			return color;
		}

		/**
		 * Returns the canvas width
		 *
		 * @return The canvas width
		 */
		public abstract double getWidth();

		/**
		 * Returns the canvas height
		 *
		 * @return The canvas height
		 */
		public abstract double getHeight();

		/**
		 * Draws a line.
		 *
		 * @param x1 The starting point's x coordinate
		 * @param y1 The starting point's y coordinate
		 * @param x2 The ending point's x coordinate
		 * @param y2 The ending point's y coordinate
		 */
		public abstract void drawLine(double x1, double y1, double x2, double y2);

		/**
		 * Draws a rect.
		 *
		 * @param x The top left corner's x coordinate
		 * @param y The top left corner's y coordinate
		 * @param w The rect's width
		 * @param h The rect's height
		 */
		public abstract void drawRect(double x, double y, double w, double h);

		/**
		 * Draws an oval.
		 *
		 * @param x The top left corner's x coordinate
		 * @param y The top left corner's y coordinate
		 * @param w The rect's width
		 * @param h The rect's height
		 */
		public abstract void drawOval(double x, double y, double w, double h);

		/**
		 * Draws a circle.
		 *
		 * @param x The center's x coordinate
		 * @param y The center's y coordinate
		 * @param r The radius
		 */
		public abstract void drawCircle(double x, double y, double r);
	}
}
