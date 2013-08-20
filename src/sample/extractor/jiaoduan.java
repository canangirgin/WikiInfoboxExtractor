package edu.uwm.jiaoduan.acl;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

import cc.mallet.fst.*;
import cc.mallet.optimize.Optimizable;
import cc.mallet.optimize.Optimizable.ByGradientValue;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.pipe.tsf.*;
import cc.mallet.types.*;
import cc.mallet.util.*;

  //http://lancet.googlecode.com/svn-history/r55/trunk/src/edu/uwm/jiaoduan/acl/TrainCRF.java

public class TrainCRF {

    private static String GREEK = "(alpha|beta|gamma|delta|epsilon|zeta|eta|theta|iota|kappa|lambda|mu|nu|xi|omicron|pi|rho|sigma|tau|upsilon|phi|chi|psi|omega)";


    public TrainCRF(String trainingFilename, String testingFilename) throws IOException {

        ArrayList<Pipe> pipes = new ArrayList<Pipe>();

        //		pipes.add(new SimpleTaggerSentence2TokenSequence());
        pipes.add(new Input2TokenSequence());


        //		ABNER features
        //		RegexMathches: Attempts to match the entire region against the pattern.

        pipes.add(new RegexMatches("INITCAPS", Pattern
                .compile("[A-Z].*")));
        pipes.add(new RegexMatches("INITCAPSALPHA", Pattern
                .compile("[A-Z][a-z].*")));
        pipes.add(new RegexMatches("ALLCAPS", Pattern
                .compile("[A-Z]+")));
        pipes.add(new RegexMatches("CAPSMIX", Pattern
                .compile("[A-Za-z]+")));
        pipes.add(new RegexMatches("HASDIGIT", Pattern
                .compile(".*[0-9].*")));
        pipes.add(new RegexMatches("SINGLEDIGIT", Pattern
                .compile("[0-9]")));
        pipes.add(new RegexMatches("DOUBLEDIGIT", Pattern
                .compile("[0-9][0-9]")));
        pipes.add(new RegexMatches("NATURALNUMBER", Pattern
                .compile("(0|[1-9]\\d*)")));
        pipes.add(new RegexMatches("REALNUMBER", Pattern
                .compile("[-0-9]+[.,]+[0-9.,]+")));
        pipes.add(new RegexMatches("HASDASH", Pattern
                .compile(".*-.*")));
        pipes.add(new RegexMatches("INITDASH", Pattern.compile("-.*")));
        pipes.add(new RegexMatches("ENDDASH", Pattern.compile(".*-")));
        pipes.add(new TokenTextCharPrefix("PREFIX=", 3));
        pipes.add(new TokenTextCharPrefix("PREFIX=", 4));
        pipes.add(new TokenTextCharSuffix("SUFFIX=", 3));
        pipes.add(new TokenTextCharSuffix("SUFFIX=", 4));
        //		NumberAlphabet
        pipes.add(new RegexMatches("ALPHANUMERIC", Pattern
                .compile(".*[A-Za-z].*[0-9].*")));
        pipes.add(new RegexMatches("ALPHANUMERIC", Pattern
                .compile(".*[0-9].*[A-Za-z].*")));
        //		RomanNum
        pipes.add(new RegexMatches("ROMAN", Pattern
                .compile("[IVXDLCM]+")));
        pipes.add(new RegexMatches("HASROMAN", Pattern
                .compile(".*\\b[IVXDLCM]+\\b.*")));
        pipes.add(new RegexMatches("GREEK", Pattern.compile(GREEK)));
        pipes.add(new RegexMatches("HASGREEK", Pattern
                .compile(".*\\b" + GREEK + "\\b.*")));
        pipes.add(new RegexMatches("PUNCTUATION", Pattern
                .compile(".*[,.;:?!-+].*")));

        //	    Offset Conjuction
        int[][] conjunctions = new int[2][];
        conjunctions[0] = new int[] { -2,-1 };
        conjunctions[1] = new int[] { 1, 2};
        pipes.add(new OffsetConjunctions(conjunctions));
        //		end of abner features

        pipes.add(new TokenSequence2FeatureVectorSequence(true, true));

        Pipe pipe = new SerialPipes(pipes);

        InstanceList trainingInstances = new InstanceList(pipe);
        InstanceList testingInstances = new InstanceList(pipe);

        trainingInstances.addThruPipe(new LineGroupIterator(new BufferedReader(new InputStreamReader(new FileInputStream(trainingFilename))), Pattern.compile("^\\s*$"), true));
        testingInstances.addThruPipe(new LineGroupIterator(new BufferedReader(new InputStreamReader(new FileInputStream(testingFilename))), Pattern.compile("^\\s*$"), true));

        CRF crf = new CRF(pipe, null);
        //crf.addStatesForLabelsConnectedAsIn(trainingInstances);
        //crf.addStatesForThreeQuarterLabelsConnectedAsIn(trainingInstances);
        //crf.addStartState();

        //trainByLabelLikehood(crf,trainingInstances, testingInstances);

        //not good
//		trainByValueGradients(crf,trainingInstances, testingInstances);

        //good performance
        trainByStochasticGradient(crf,trainingInstances, testingInstances);



    }

    private void trainByStochasticGradient(CRF crf,
                                           InstanceList trainingInstances, InstanceList testingInstances) {

        //		http://wikilink.googlecode.com/svn/trunk/WikiLink/mallet-2.0-RC2/src/cc/mallet/fst/tests/TestCRF.java

        // CRF trainer
        InstanceList[] lists = trainingInstances.split(new double[]{.2,.5, .8});
        // by default, use L-BFGS as the optimizer

        //crfTrainer.setMaxResets(0);
        // train till convergence
        //crf.addFullyConnectedStatesForLabels();
        crf.addFullyConnectedStatesForBiLabels();

        //crf.addStatesForBiLabelsConnectedAsIn(trainingInstances);

        crf.setWeightsDimensionAsIn(lists[0], false);
        CRFTrainerByStochasticGradient crft = new CRFTrainerByStochasticGradient (crf, 0.0001);
        System.out.println("Training Accuracy before training = " + crf.averageTokenAccuracy(lists[0]));
        System.out.println("Testing  Accuracy before training = " + crf.averageTokenAccuracy(lists[1]));
        System.out.println("Training...");
        // either fixed learning rate or selected on a sample
        crft.setLearningRateByLikelihood(lists[0]);
        // crft.setLearningRate(0.01);
//		crft.train(trainingInstances);
        crft.train(lists[2], 100);

        crf.print();
        System.out.println("Training Accuracy after training = " + crf.averageTokenAccuracy(lists[0]));
        System.out.println("Testing  Accuracy after training = " + crf.averageTokenAccuracy(lists[1]));



        //zuofeng's code
        //save crf model
        //refer: /cc/mallet.fst.tests/TestCRF.java
        // Create a file to store the CRF
        File f = new File("model.crf");
        //trainer.setUseSparseWeights(false);

        try {
            ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(f));
            oos.writeObject(crf);
            oos.close();
        } catch (IOException e) {
            System.err.println("Exception writing file: " + e);
        }
        //end of zuofeng's code


        //		parameters

    }

    /**
     * http://code.google.com/p/sequence-labeler/source/browse/trunk/src/edu/jhu/nlp/sequence/classify/CRFSequenceLearner.java?spec=svn6&r=6
     * @param crf
     * @param trainingInstances
     * @param testingInstances
     */
    private void trainByValueGradients(CRF crf, InstanceList trainingInstances,
                                       InstanceList testingInstances) {

        CRFOptimizableByBatchLabelLikelihood batchOptLabel =
                new CRFOptimizableByBatchLabelLikelihood(crf, trainingInstances, 1);

        ThreadedOptimizable optLabel = new ThreadedOptimizable(
                batchOptLabel, trainingInstances, crf.getParameters().getNumFactors(),
                new CRFCacheStaleIndicator(crf));
        // CRF trainer
        ByGradientValue[] opts =
                new ByGradientValue[]{optLabel};
        // by default, use L-BFGS as the optimizer
        CRFTrainerByValueGradients crfTrainer =
                new CRFTrainerByValueGradients(crf, opts);
        //crfTrainer.setMaxResets(0);
        // train till convergence
        crfTrainer.train(trainingInstances, 999);
        optLabel.shutdown();



        //zuofeng's code
        //save crf model
        //refer: /cc/mallet.fst.tests/TestCRF.java
        // Create a file to store the CRF
        File f = new File("model.crf");
        //trainer.setUseSparseWeights(false);

        try {
            ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(f));
            oos.writeObject(crf);
            oos.close();
        } catch (IOException e) {
            System.err.println("Exception writing file: " + e);
        }
        //end of zuofeng's code


        //		parameters

    }

    private void trainByLabelLikehood(CRF crf, InstanceList trainingInstances, InstanceList testingInstances ) {
        CRFTrainerByLabelLikelihood trainer =
                new CRFTrainerByLabelLikelihood(crf);


        //zuofeng's code
        //save crf model
        //refer: /cc/mallet.fst.tests/TestCRF.java
        // Create a file to store the CRF
        File f = new File("model.crf");
        //trainer.setUseSparseWeights(false);

        try {
            ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(f));
            oos.writeObject(crf);
            oos.close();
        } catch (IOException e) {
            System.err.println("Exception writing file: " + e);
        }
        //end of zuofeng's code




        trainer.addEvaluator(new PerClassAccuracyEvaluator(testingInstances, "Class level testing"));
        trainer.addEvaluator(new TokenAccuracyEvaluator(testingInstances, "Token Level testing"));


        trainer.train(trainingInstances);

        //		parameters
        System.out.println("\nCRF parameters.\n\thyperbolicPriorSlope: "
                + trainer.getUseHyperbolicPriorSlope()
                + "\n\thyperbolicPriorSharpness: "
                + trainer.getUseHyperbolicPriorSharpness()
                + "\n\tgaussianPriorVariance: "
                + trainer.getGaussianPriorVariance());

    }

    public static void main (String[] args) throws Exception {
        TrainCRF trainer = new TrainCRF(args[0], args[1]);
    }

}