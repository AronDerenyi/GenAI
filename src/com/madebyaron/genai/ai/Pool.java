package com.madebyaron.genai.ai;

import java.util.*;

/**
 * This class represents a collection of networks.
 * It is responsible for the calculation of the generations.
 */
public final class Pool {

	private static final Random RANDOM = new Random();

	private final Process process;
	private final List<Network> networks = new ArrayList<>();
	private final List<Network> unmodifiableNetworks = Collections.unmodifiableList(networks);

	/**
	 * Creates a new pool and fills it with new networks provided by the given process.
	 *
	 * @param process The process handling the networks' generation
	 *                (in normal cases it is equal to the process that evaluates the networks)
	 * @param size The number of generated networks
	 */
	public Pool(Process process, int size) {
		if (process == null) throw new NullPointerException("Process can't be null.");
		if (size < 0) throw new IllegalArgumentException("Size can't be negative.");

		this.process = process;
		while (networks.size() < size) networks.add(process.createNetwork());
	}

	/**
	 * Creates a new pool from the given networks.
	 *
	 * @param process The process handling the networks' generation
	 *                (in normal cases it is equal to the process that evaluates the networks)
	 * @param networks The networks in the pool
	 */
	public Pool(Process process, Collection<Network> networks) {
		if (process == null) throw new NullPointerException("Process can't be null.");
		if (networks == null) throw new NullPointerException("Networks can't be null.");

		this.process = process;
		this.networks.addAll(networks);
	}

	/**
	 * Returns the process that handles the generation of networks.
	 * In normal cases it is equal to the process that evaluates the networks.
	 *
	 * @return The process
	 */
	public Process getProcess() {
		return process;
	}

	/**
	 * Returns the networks in this pool.
	 *
	 * @return An unmodifiable version of the networks.
	 */
	public List<Network> getNetworks() {
		synchronized (networks) {
			return unmodifiableNetworks;
		}
	}

	/**
	 * Creates a new evaluation to store the networks' evaluation.
	 *
	 * @return The new evaluation holder
	 */
	public Evaluation createEvaluation() {
		return new Evaluation();
	}

	/**
	 * Calculates the next generation by randomly removing
	 * a given ratio of the networks then combining them
	 * to fill the pool again.
	 *
	 * @param evaluation The evaluation holing each network's evaluation
	 * @param purgeRatio The ratio of the removed networks (0 = none, 1 = all)
	 */
	public void generation(Evaluation evaluation, double purgeRatio) {
		if (evaluation == null) throw new NullPointerException("Evaluation can't be null.");
		if (!evaluation.isDone()) throw new IllegalStateException("The evaluation hasn't finished yet.");
		if (purgeRatio < 0 || purgeRatio > 1) throw new IllegalArgumentException("Purge ratio out of bounds.");

		int initialSize = networks.size();
		List<Network> networks = new ArrayList<>(this.networks);

		int desiredSize = (int) (initialSize * (1 - purgeRatio));
		networks.sort(Comparator.comparingDouble(evaluation::getEvaluation));
		while (networks.size() > desiredSize) {
			networks.remove((int) (Math.pow(RANDOM.nextDouble(), 3) * networks.size()));
		}

		int resultingSize = networks.size();
		if (resultingSize == 0) {
			networks.add(process.createNetwork());
			resultingSize = 1;
		}
		while (networks.size() < initialSize) {
			networks.add(process.createNetwork(
					networks.get((int) (RANDOM.nextDouble() * resultingSize)),
					networks.get((int) (RANDOM.nextDouble() * resultingSize))
			));
		}

		synchronized (this.networks) {
			this.networks.clear();
			this.networks.addAll(networks);
		}
	}

	/**
	 * The object holding each network's evaluation.
	 */
	public final class Evaluation {

		private final Map<Network, Double> evaluations = new HashMap<>();

		/**
		 * Returns the stored evaluation of the given network.
		 *
		 * @param network The network
		 * @return The evaluation
		 */
		public double getEvaluation(Network network) {
			if (network == null) throw new NullPointerException("Network can't be null.");

			return evaluations.getOrDefault(network, 0.0);
		}

		/**
		 * Stores an evaluation for the given network.
		 *
		 * @param network The network
		 * @param evaluation The evaluation
		 */
		public void setEvaluation(Network network, double evaluation) {
			if (network == null) throw new NullPointerException("Network can't be null.");

			if (networks.contains(network)) {
				evaluations.put(network, evaluation);
			}
		}

		/**
		 * Return whether or not every network has been evaluated.
		 *
		 * @return True if every network has an associated evaluation.
		 */
		public boolean isDone() {
			return evaluations.size() == networks.size();
		}

		/**
		 * Returns the network with the highest evaluation.
		 *
		 * @return The network
		 */
		public Network getBestNetwork() {
			Map.Entry<Network, Double> best = null;
			for (Map.Entry<Network, Double> entry : evaluations.entrySet()) {
				if (best == null || entry.getValue() > best.getValue()) best = entry;
			}
			return best.getKey();
		}

		/**
		 * Returns the network with the lowest evaluation.
		 *
		 * @return The network
		 */
		public Network getWorstNetwork() {
			Map.Entry<Network, Double> worst = null;
			for (Map.Entry<Network, Double> entry : evaluations.entrySet()) {
				if (worst == null || entry.getValue() < worst.getValue()) worst = entry;
			}
			return worst.getKey();
		}

		/**
		 * Returns the highest evaluation value.
		 *
		 * @return The evaluation
		 */
		public Double getBestEvaluation() {
			Double best = null;
			for (Double evaluation : evaluations.values()) {
				if (best == null || evaluation > best) best = evaluation;
			}
			return best;
		}

		/**
		 * Returns the lowest evaluation value.
		 *
		 * @return The evaluation
		 */
		public Double getWorstEvaluation() {
			Double worst = null;
			for (Double evaluation : evaluations.values()) {
				if (worst == null || evaluation < worst) worst = evaluation;
			}
			return worst;
		}
	}
}
