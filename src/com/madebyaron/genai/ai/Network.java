package com.madebyaron.genai.ai;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;

/**
 * This class represents a neural network.
 * It holds every neuron with the connections between them.
 */
public final class Network implements Serializable {

	private static final long serialVersionUID = -4610410720219508540L;

	private transient double[] inputNeurons;
	private transient double[] outputNeurons;
	private transient double[][] hiddenNeurons;

	private transient double[][] neurons;

	private double[][][] connections;

	/**
	 * Initializes the network. Generates the arrays
	 * for storing the neurons and the connection strengths.
	 *
	 * @param inputNeuronCount The number of input neurons
	 * @param outputNeuronCount The number of output neurons
	 * @param hiddenNeuronCount The number of hidden neurons as an array where each value is a layer
	 */
	private void init(int inputNeuronCount, int outputNeuronCount, int[] hiddenNeuronCount) {
		inputNeurons = new double[inputNeuronCount];
		outputNeurons = new double[outputNeuronCount];
		hiddenNeurons = new double[hiddenNeuronCount.length][];
		for (int layer = 0; layer < hiddenNeuronCount.length; layer++) {
			hiddenNeurons[layer] = new double[hiddenNeuronCount[layer]];
		}

		neurons = new double[hiddenNeurons.length + 2][];
		for (int layer = 0; layer < neurons.length; layer++) {
			if (layer == 0) {
				neurons[layer] = inputNeurons;
			} else if (layer == neurons.length - 1) {
				neurons[layer] = outputNeurons;
			} else {
				neurons[layer] = hiddenNeurons[layer - 1];
			}
		}

		connections = new double[neurons.length - 1][][];
		for (int fromLayer = 0; fromLayer < connections.length; fromLayer++) {
			connections[fromLayer] = new double[neurons[fromLayer].length][neurons[fromLayer + 1].length];
		}
	}

	/**
	 * Creates a new random network from the number of neurons.
	 * Also randomises the connections using the given parameters.
	 *
	 * @param inputNeuronCount The number of input neurons
	 * @param outputNeuronCount The number of output neurons
	 * @param hiddenNeuronCount The number of hidden neurons as an array where each value is a layer
	 * @param initRangeFrom The lowest initial connection value
	 * @param initRangeTo The highest initial connection value
	 * @param seed The seed for the random generation
	 */
	public Network(int inputNeuronCount, int outputNeuronCount, int[] hiddenNeuronCount,
	               double initRangeFrom, double initRangeTo, long seed) {
		init(inputNeuronCount, outputNeuronCount, hiddenNeuronCount);

		Random random = new Random(seed);
		forEachConnection((layer, fromNeuron, toNeuron, strength) ->
				random.nextDouble() * (initRangeTo - initRangeFrom) + initRangeFrom
		);
	}

	/**
	 * Creates a new random network by combining the two parent networks.
	 * Also randomises the connections using the given parameters.
	 *
	 * @param parentA One parent
	 * @param parentB The other parent
	 * @param flipChance The chance of a connection strength to get multiplied by -1
	 * @param mutationChance The chance of a connection strength to mutate
	 * @param mutationStrength The strength of the mutation (0 = no mutation, 1 = a multiplication from 0.5 to 2.0)
	 * @param seed The seed for the random generation
	 */
	public Network(Network parentA, Network parentB,
	               double flipChance, double mutationChance, double mutationStrength, long seed) {
		if (parentA == null) throw new NullPointerException("Parent can't be null");
		if (parentB == null) throw new NullPointerException("Parent can't be null");

		if (parentA.getLayerCount() != parentB.getLayerCount()) {
			throw new IllegalArgumentException("Parent network layer count doesn't match");
		} else {
			for (int layer = 0; layer < parentA.getLayerCount(); layer++) {
				if (parentA.getNeuronCount(layer) != parentB.getNeuronCount(layer)) {
					throw new IllegalArgumentException("Parent network neuron count doesn't match on layer: " + layer);
				}
			}
		}

		int inputNeuronCount = parentA.getInputNeuronCount();
		int outputNeuronCount = parentA.getOutputNeuronCount();
		int[] hiddenNeuronCount = new int[parentA.getHiddenLayerCount()];
		for (int layer = 0; layer < hiddenNeuronCount.length; layer++) {
			hiddenNeuronCount[layer] = parentA.getHiddenNeuronCount(layer);
		}
		init(inputNeuronCount, outputNeuronCount, hiddenNeuronCount);

		Random random = new Random(seed);
		forEachConnection((fromLayer, fromNeuron, toNeuron, strength) ->
				random.nextDouble() < 0.5 ?
						parentA.connections[fromLayer][fromNeuron][toNeuron] :
						parentB.connections[fromLayer][fromNeuron][toNeuron]
		);

		forEachConnection((fromLayer, fromNeuron, toNeuron, strength) ->
				random.nextDouble() > flipChance ?
						strength :
						-strength
		);

		forEachConnection((fromLayer, fromNeuron, toNeuron, strength) ->
				random.nextDouble() > mutationChance ?
						strength :
						strength * Math.pow(2, mutationStrength * (random.nextDouble() * 2 - 1))
		);
	}

	/**
	 * Returns the number of input neurons.
	 *
	 * @return The input neuron count
	 */
	public int getInputNeuronCount() {
		return inputNeurons.length;
	}

	/**
	 * Returns the number of output neurons.
	 *
	 * @return The output neuron count
	 */
	public int getOutputNeuronCount() {
		return outputNeurons.length;
	}

	/**
	 * Returns the number of hidden layers.
	 *
	 * @return The hidden layer count
	 */
	public int getHiddenLayerCount() {
		return hiddenNeurons.length;
	}

	/**
	 * Returns the number of hidden neurons in the given hidden layer.
	 *
	 * @param layer The layer's index
	 * @return The hidden neuron count
	 */
	public int getHiddenNeuronCount(int layer) {
		return hiddenNeurons[layer].length;
	}

	/**
	 * Returns the number of layers.
	 *
	 * @return The layer count
	 */
	public int getLayerCount() {
		return neurons.length;
	}

	/**
	 * Returns the number of neurons in the given hidden layer.
	 *
	 * @param layer The layer's index
	 * @return The neuron count
	 */
	public int getNeuronCount(int layer) {
		return neurons[layer].length;
	}

	/**
	 * Evaluates the network by giving setting it's input neuron values.
	 * It returns the output neuron values.
	 *
	 * @param input The input neuron values
	 * @return The output neuron values
	 */
	public double[] evaluate(double[] input) {
		if (input == null) throw new NullPointerException("Input can't be null");

		if (input.length != getInputNeuronCount()) {
			throw new IllegalArgumentException("Input size doesn't match input neuron count");
		}

		System.arraycopy(input, 0, inputNeurons, 0, getInputNeuronCount());

		for (int fromLayer = 0; fromLayer < connections.length; fromLayer++) {
			double[][] connectionFromLayer = connections[fromLayer];
			double[] neuronFromLayer = neurons[fromLayer];
			double[] neuronToLayer = neurons[fromLayer + 1];

			for (int toNeuron = 0; toNeuron < neuronToLayer.length; toNeuron++) {
				double sum = 0;
				for (int fromNeuron = 0; fromNeuron < neuronFromLayer.length; fromNeuron++) {
					sum += neuronFromLayer[fromNeuron] * connectionFromLayer[fromNeuron][toNeuron];
				}
				neuronToLayer[toNeuron] = sigmoid(sum);
			}
		}

		return outputNeurons.clone();
	}

	/**
	 * Writes this network to an object output stream.
	 *
	 * @param out The object output stream
	 * @throws IOException Any exception that occurs while writing
	 */
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(getInputNeuronCount());
		out.writeInt(getOutputNeuronCount());
		out.writeInt(getHiddenLayerCount());
		for (int layer = 0; layer < getHiddenLayerCount(); layer++) {
			out.writeInt(getHiddenNeuronCount(layer));
		}

		for (double[][] fromLayer : connections) {
			for (double[] fromNeuron : fromLayer) {
				for (double toNeuron : fromNeuron) {
					out.writeDouble(toNeuron);
				}
			}
		}
	}

	/**
	 * Reads this network's values from an object input stream.
	 *
	 * @param in The object input stream
	 * @throws IOException Any exception that occurs while writing
	 */
	private void readObject(ObjectInputStream in) throws IOException {
		int inputNeuronCount = in.readInt();
		int outputNeuronCount = in.readInt();
		int[] hiddenNeuronCount = new int[in.readInt()];
		for (int layer = 0; layer < hiddenNeuronCount.length; layer++) {
			hiddenNeuronCount[layer] = in.readInt();
		}

		init(inputNeuronCount, outputNeuronCount, hiddenNeuronCount);
		for (double[][] fromLayer : connections) {
			for (double[] fromNeuron : fromLayer) {
				for (int toNeuronIndex = 0; toNeuronIndex < fromNeuron.length; toNeuronIndex++) {
					fromNeuron[toNeuronIndex] = in.readDouble();
				}
			}
		}
	}

	/**
	 * Iterates through every connection and applies the give iterator on all of them.
	 *
	 * @param iterator The connection iterator
	 */
	private void forEachConnection(ConnectionIterator iterator) {
		for (int fromLayer = 0; fromLayer < connections.length; fromLayer++) {
			double[][] connectionFromLayer = connections[fromLayer];

			for (int fromNeuron = 0; fromNeuron < connectionFromLayer.length; fromNeuron++) {
				double[] connectionFromNeuron = connectionFromLayer[fromNeuron];

				for (int toNeuron = 0; toNeuron < connectionFromNeuron.length; toNeuron++) {
					connectionFromNeuron[toNeuron] = iterator.strength(
							fromLayer, fromNeuron, toNeuron,
							connectionFromNeuron[toNeuron]
					);
				}
			}
		}
	}

	/**
	 * The function used after adding all the incoming neurons' values to calculate a neuron's value.
	 *
	 * @param value The function parameter
	 * @return The resulting value
	 */
	private double sigmoid(double value) {
		return 1 - 2 / (1 + Math.pow(Math.E, value));
	}

	/**
	 * This iterator is used to iterate over every connection in a network.
	 */
	private interface ConnectionIterator {

		/**
		 * This function is called for every connection when iterating though a network.
		 *
		 * @param fromLayer The layer's index where the connection starts
		 * @param fromNeuron The neuron's index where the connection starts
		 * @param toNeuron The neuron's index where the connection ends
		 * @param strength The current strength of a connection
		 * @return The new strength of the connection
		 */
		double strength(int fromLayer, int fromNeuron, int toNeuron, double strength);
	}
}
