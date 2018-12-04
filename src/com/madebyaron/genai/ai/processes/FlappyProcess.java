package com.madebyaron.genai.ai.processes;

import com.madebyaron.genai.ai.Network;
import com.madebyaron.genai.ai.Process;

import java.util.Random;

public final class FlappyProcess implements Process<FlappyProcess.FlappyData> {

	private static final Random RANDOM = new Random();

	private static final int INPUT_NEURONS = 6;
	private static final int OUTPUT_NEURONS = 1;
	private static final int[] HIDDEN_NEURONS = {4, 3};

	private static final double GRAVITY = -0.005;

	private static final double FLAPPY_JUMP = 0.04;
	private static final double FLAPPY_SPEED = 0.02;
	private static final double FLAPPY_WIDTH = 0.05;
	private static final double FLAPPY_HEIGHT = 0.05;

	private static final double PIPE_WIDTH = 0.1;
	private static final double PIPE_HEIGHT = 0.25;
	private static final double PIPE_RATE = 1;

	@Override
	public String getName() {
		return "flappy";
	}

	@Override
	public Network createNetwork() {
		return new Network(
				INPUT_NEURONS, OUTPUT_NEURONS, HIDDEN_NEURONS,
				-1, 1,
				RANDOM.nextLong()
		);
	}

	@Override
	public Network createNetwork(Network networkA, Network networkB) {
		return new Network(
				networkA, networkB,
				0.005, 0.02, 0.05,
				RANDOM.nextLong()
		);
	}

	@Override
	public FlappyData createData() {
		return new FlappyData();
	}

	@Override
	public void tick(Network network, FlappyData data, Process.Graphics graphics) {
		double[] input = network.evaluate(new double[]{
				data.flappyY,
				data.flappyVY,
				data.pipeX - data.flappyX,
				data.pipeY,
				data.pipeY + PIPE_HEIGHT,
				1
		});

		if (data.prev < 0 && input[0] > 0) {
			data.flappyVY = FLAPPY_JUMP;
		}
		data.prev = input[0];

		data.flappyVY += GRAVITY;

		data.flappyX += FLAPPY_SPEED;
		data.flappyY += data.flappyVY;

		if (data.pipeX == 0 || data.flappyX > data.pipeX + PIPE_WIDTH) {
			data.pipeX = data.flappyX + (PIPE_RATE - data.flappyX % PIPE_RATE);
			data.pipeY = RANDOM.nextDouble() * (1.0 - PIPE_HEIGHT);
		}

		if (data.flappyX + FLAPPY_WIDTH > data.pipeX && data.flappyX < data.pipeX + PIPE_WIDTH) {
			if (data.flappyY + FLAPPY_HEIGHT > data.pipeY + PIPE_HEIGHT || data.flappyY < data.pipeY) {
				data.died = true;
			}
		}

		if (data.flappyY < 0) {
			data.died = true;
		}

		if (data.died) {
			data.setEvaluation(data.flappyX);
		}

		graphics.setColor(Graphics.BLACK);
		graphics.drawRect(
				0, 0,
				graphics.getWidth(),
				graphics.getHeight()
		);

		graphics.setColor(Graphics.BLUE);
		graphics.drawRect(
				graphics.getWidth() * (data.pipeX - data.flappyX),
				graphics.getHeight() * (1 - data.pipeY),
				graphics.getWidth() * PIPE_WIDTH,
				graphics.getHeight() * data.pipeY
		);
		graphics.drawRect(
				graphics.getWidth() * (data.pipeX - data.flappyX),
				0,
				graphics.getWidth() * PIPE_WIDTH,
				graphics.getHeight() * (1 - data.pipeY - PIPE_HEIGHT)
		);

		graphics.setColor(Graphics.RED);
		graphics.drawRect(
				0,
				graphics.getHeight() * (1 - data.flappyY - FLAPPY_HEIGHT),
				graphics.getWidth() * FLAPPY_WIDTH,
				graphics.getHeight() * FLAPPY_HEIGHT
		);
	}

	public static class FlappyData extends Process.Data {

		private double flappyX = 0;
		private double flappyY = 0.5;
		private double flappyVY = 0;

		private double pipeX = 0;
		private double pipeY = 0;

		private double prev = 0;
		private boolean died = false;
	}
}
