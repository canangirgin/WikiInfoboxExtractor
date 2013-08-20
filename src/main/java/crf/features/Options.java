package crf.features;

import cc.mallet.util.CommandOption;

/**
 * Created with IntelliJ IDEA.
 * User: a
 * Date: 8/20/13
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class Options {

    public static final CommandOption.Double gaussianVarianceOption = new CommandOption.Double
        (cc.mallet.fst.SimpleTagger.class, "gaussian-variance", "DECIMAL", true, 10.0,
                "The gaussian prior variance used for training.", null);

    public static final CommandOption.Boolean trainOption = new CommandOption.Boolean
            (cc.mallet.fst.SimpleTagger.class, "train", "true|false", true, false,
                    "Whether to train", null);

    public static final CommandOption.String testOption = new CommandOption.String
            (cc.mallet.fst.SimpleTagger.class, "test", "lab or seg=start-1.continue-1,...,start-n.continue-n",
                    true, null,
                    "Test measuring labeling or segmentation (start-i, continue-i) accuracy", null);

    public static final CommandOption.File modelOption = new CommandOption.File
            (cc.mallet.fst.SimpleTagger.class, "model-file", "FILENAME", true, null,
                    "The filename for reading (train/run) or saving (train) the model.", null);

    public static final CommandOption.Double trainingFractionOption = new CommandOption.Double
            (cc.mallet.fst.SimpleTagger.class, "training-proportion", "DECIMAL", true, 0.5,
                    "Fraction of data to use for training in a random split.", null);

    public static final CommandOption.Integer randomSeedOption = new CommandOption.Integer
            (cc.mallet.fst.SimpleTagger.class, "random-seed", "INTEGER", true, 0,
                    "The random seed for randomly selecting a proportion of the instance list for training", null);

    public static final CommandOption.IntegerArray ordersOption = new CommandOption.IntegerArray
            (cc.mallet.fst.SimpleTagger.class, "orders", "COMMA-SEP-DECIMALS", true, new int[]{1},
                    "List of label Markov orders (main and backoff) ", null);

    public static final CommandOption.String forbiddenOption = new CommandOption.String(
            cc.mallet.fst.SimpleTagger.class, "forbidden", "REGEXP", true,
            "\\s", "label1,label2 transition forbidden if it matches this", null);

    public static final CommandOption.String allowedOption = new CommandOption.String(
            cc.mallet.fst.SimpleTagger.class, "allowed", "REGEXP", true,
            ".*", "label1,label2 transition allowed only if it matches this", null);

    public static final CommandOption.String defaultOption = new CommandOption.String(
            cc.mallet.fst.SimpleTagger.class, "default-label", "STRING", true, "O",
            "Label for initial context and uninteresting tokens", null);

    public static final CommandOption.Integer iterationsOption = new CommandOption.Integer(
            cc.mallet.fst.SimpleTagger.class, "iterations", "INTEGER", true, 500,
            "Number of training iterations", null);

    public static final CommandOption.Boolean viterbiOutputOption = new CommandOption.Boolean(
            cc.mallet.fst.SimpleTagger.class, "viterbi-output", "true|false", true, false,
            "Print Viterbi periodically during training", null);

    public static final CommandOption.Boolean connectedOption = new CommandOption.Boolean(
            cc.mallet.fst.SimpleTagger.class, "fully-connected", "true|false", true, true,
            "Include all allowed transitions, even those not in training data", null);

    public static final CommandOption.String weightsOption = new CommandOption.String(
            cc.mallet.fst.SimpleTagger.class, "weights", "sparse|some-dense|dense", true, "some-dense",
            "Use sparse, some dense (using a heuristic), or dense crf.features on transitions.", null);

    public static final CommandOption.Boolean continueTrainingOption = new CommandOption.Boolean(
            cc.mallet.fst.SimpleTagger.class, "continue-training", "true|false", false, false,
            "Continue training from model specified by --model-file", null);

    public static final CommandOption.Integer nBestOption = new CommandOption.Integer(
            cc.mallet.fst.SimpleTagger.class, "n-best", "INTEGER", true, 1,
            "How many answers to output", null);

    public static final CommandOption.Integer cacheSizeOption = new CommandOption.Integer(
            cc.mallet.fst.SimpleTagger.class, "cache-size", "INTEGER", true, 100000,
            "How much state information to memoize in n-best decoding", null);

    public static final CommandOption.Boolean includeInputOption = new CommandOption.Boolean(
            cc.mallet.fst.SimpleTagger.class, "include-input", "true|false", true, false,
            "Whether to include the input crf.features when printing decoding output", null);

    public static final CommandOption.Boolean featureInductionOption = new CommandOption.Boolean(
            cc.mallet.fst.SimpleTagger.class, "feature-induction", "true|false", true, false,
            "Whether to perform feature induction during training", null);

    public static final CommandOption.Integer numThreads = new CommandOption.Integer(
            cc.mallet.fst.SimpleTagger.class, "threads", "INTEGER", true, 1,
            "Number of threads to use for crf training.", null);

    public static final CommandOption.List commandOptions =
            new CommandOption.List (
                    "Training, testing and running a generic tagger.",
                    new CommandOption[] {
                            gaussianVarianceOption,
                            trainOption,
                            iterationsOption,
                            testOption,
                            trainingFractionOption,
                            modelOption,
                            randomSeedOption,
                            ordersOption,
                            forbiddenOption,
                            allowedOption,
                            defaultOption,
                            viterbiOutputOption,
                            connectedOption,
                            weightsOption,
                            continueTrainingOption,
                            nBestOption,
                            cacheSizeOption,
                            includeInputOption,
                            featureInductionOption,
                            numThreads
                    });
}
