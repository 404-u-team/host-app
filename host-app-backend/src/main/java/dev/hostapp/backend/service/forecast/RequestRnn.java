package dev.hostapp.backend.service.forecast;

import java.util.Arrays;
import java.util.Random;

/** A single tanh recurrent layer with a linear output, trained using the next day's observed count. */
public final class RequestRnn {
    public static final int WINDOW = 14;
    private static final int HIDDEN = 4;
    private static final int EPOCHS = 80;
    private static final double RATE = 0.025;
    private static final double GRADIENT_LIMIT = 1.0;

    private final double[] inputWeights = new double[HIDDEN];
    private final double[][] recurrentWeights = new double[HIDDEN][HIDDEN];
    private final double[] hiddenBias = new double[HIDDEN];
    private final double[] outputWeights = new double[HIDDEN];
    private final double scale;
    private double outputBias;

    private RequestRnn(double scale) {
        this.scale = scale;
        Random random = new Random(42);
        for (int unit = 0; unit < HIDDEN; unit++) {
            inputWeights[unit] = initialWeight(random);
            outputWeights[unit] = initialWeight(random);
            for (int previous = 0; previous < HIDDEN; previous++) {
                recurrentWeights[unit][previous] = initialWeight(random);
            }
        }
    }

    public static RequestRnn train(double[] days, int trainingEnd) {
        if (trainingEnd <= WINDOW || trainingEnd > days.length) {
            throw new IllegalArgumentException("Нужны минимум 15 полных дней обучения");
        }
        double scale = Math.max(1, Arrays.stream(days, 0, trainingEnd).max().orElseThrow());
        RequestRnn network = new RequestRnn(scale);
        for (int epoch = 0; epoch < EPOCHS; epoch++) {
            for (int target = WINDOW; target < trainingEnd; target++) {
                network.trainWindow(days, target);
            }
        }
        return network;
    }

    public double next(double[] history, int end) {
        double[][] states = states(history, end - WINDOW);
        return Math.max(0, output(states[WINDOW]) * scale);
    }

    public double twoDaysAhead(double[] history, int end) {
        double[] window = Arrays.copyOfRange(history, end - WINDOW + 1, end + 1);
        window[WINDOW - 1] = next(history, end);
        return next(window, WINDOW);
    }

    private void trainWindow(double[] days, int target) {
        int start = target - WINDOW;
        double[][] states = states(days, start);
        double error = output(states[WINDOW]) - days[target] / scale;

        double[] hiddenGradient = new double[HIDDEN];
        double[] inputGradient = new double[HIDDEN];
        double[] biasGradient = new double[HIDDEN];
        double[][] recurrentGradient = new double[HIDDEN][HIDDEN];
        for (int unit = 0; unit < HIDDEN; unit++) {
            hiddenGradient[unit] = error * outputWeights[unit];
        }

        for (int step = WINDOW; step > 0; step--) {
            double[] previousGradient = new double[HIDDEN];
            for (int unit = 0; unit < HIDDEN; unit++) {
                double state = states[step][unit];
                double delta = hiddenGradient[unit] * (1 - state * state);
                inputGradient[unit] += delta * days[start + step - 1] / scale;
                biasGradient[unit] += delta;
                for (int previous = 0; previous < HIDDEN; previous++) {
                    recurrentGradient[unit][previous] += delta * states[step - 1][previous];
                    previousGradient[previous] += recurrentWeights[unit][previous] * delta;
                }
            }
            hiddenGradient = previousGradient;
        }

        outputBias -= adjustment(error);
        for (int unit = 0; unit < HIDDEN; unit++) {
            outputWeights[unit] -= adjustment(error * states[WINDOW][unit]);
            inputWeights[unit] -= adjustment(inputGradient[unit]);
            hiddenBias[unit] -= adjustment(biasGradient[unit]);
            for (int previous = 0; previous < HIDDEN; previous++) {
                recurrentWeights[unit][previous] -= adjustment(recurrentGradient[unit][previous]);
            }
        }
    }

    private double[][] states(double[] days, int start) {
        double[][] states = new double[WINDOW + 1][HIDDEN];
        for (int step = 1; step <= WINDOW; step++) {
            double input = days[start + step - 1] / scale;
            for (int unit = 0; unit < HIDDEN; unit++) {
                double sum = hiddenBias[unit] + inputWeights[unit] * input;
                for (int previous = 0; previous < HIDDEN; previous++) {
                    sum += recurrentWeights[unit][previous] * states[step - 1][previous];
                }
                states[step][unit] = Math.tanh(sum);
            }
        }
        return states;
    }

    private double output(double[] lastState) {
        double result = outputBias;
        for (int unit = 0; unit < HIDDEN; unit++) {
            result += outputWeights[unit] * lastState[unit];
        }
        return result;
    }

    private double adjustment(double gradient) {
        return RATE * Math.clamp(gradient, -GRADIENT_LIMIT, GRADIENT_LIMIT);
    }

    private static double initialWeight(Random random) {
        return (random.nextDouble() - 0.5) * 0.4;
    }
}
