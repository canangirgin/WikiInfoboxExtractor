package crf;/* Copyright (C) 2003 University of Pennsylvania.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
http://www.cs.umass.edu/~mccallum/mallet
This software is provided under the terms of the Common Public License,
version 1.0, as published by http://www.opensource.org.  For further
information, see the file `LICENSE' included with this distribution. */

import cc.mallet.fst.*;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.iterator.LineGroupIterator;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Sequence;
import cc.mallet.util.MalletLogger;
import crf.features.Options;
import crf.features.SimpleTaggerSentence2FeatureVectorSequence;

import java.io.*;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * This class's main method trains, tests, or runs a generic crf-based
 * sequence tagger.
 * <p>
 * Training and test files consist of blocks of lines, one block for each instance, 
 * separated by blank lines. Each block of lines should have the first form 
 * specified for the input of {@link crf.features.SimpleTaggerSentence2FeatureVectorSequence}.
 * A variety of command line options control the operation of the main program, as
 * described in the comments for {@link #main main}.
 *
 * @author Fernando Pereira <a href="mailto:pereira@cis.upenn.edu">pereira@cis.upenn.edu</a>
 * @version 1.0
 */
public class Tagger
{
  private static Logger logger =
    MalletLogger.getLogger(cc.mallet.fst.SimpleTagger.class.getName());

  /**
   * No <code>crf.SimpleTagger</code> objects allowed.
   */
  private Tagger()
  {
  }




  /**
   * Create and train a crf model from the given training data,
   * optionally testing it on the given test data.
   *
   * @param training training data
   * @param testing test data (possibly <code>null</code>)
   * @param eval accuracy evaluator (possibly <code>null</code>)
   * @param orders label Markov orders (main and backoff)
   * @param defaultLabel default label
   * @param forbidden regular expression specifying impossible label
   * transitions <em>current</em><code>,</code><em>next</em>
   * (<code>null</code> indicates no forbidden transitions)
   * @param allowed regular expression specifying allowed label transitions
   * (<code>null</code> indicates everything is allowed that is not forbidden)
   * @param connected whether to include even transitions not
   * occurring in the training data.
   * @param iterations number of training iterations
   * @param var Gaussian prior variance
   * @return the trained model
   */
  public static CRF train(InstanceList training, InstanceList testing,
      TransducerEvaluator eval, int[] orders,
      String defaultLabel,
      String forbidden, String allowed,
      boolean connected, int iterations, double var, CRF crf)
  {
    Pattern forbiddenPat = Pattern.compile(forbidden);
    Pattern allowedPat = Pattern.compile(allowed);
    if (crf == null) {
      crf = new CRF(training.getPipe(), (Pipe)null);
      String startName =
        crf.addOrderNStates(training, orders, null,
            defaultLabel, forbiddenPat, allowedPat,
            connected);
      for (int i = 0; i < crf.numStates(); i++)
        crf.getState(i).setInitialWeight (Transducer.IMPOSSIBLE_WEIGHT);
      crf.getState(startName).setInitialWeight(0.0);
    }
    logger.info("Training on " + training.size() + " instances");
    if (testing != null)
      logger.info("Testing on " + testing.size() + " instances");
    
  	assert(Options.numThreads.value > 0);
    if (Options.numThreads.value > 1) {
      CRFTrainerByThreadedLabelLikelihood crft = new CRFTrainerByThreadedLabelLikelihood(crf,Options.numThreads.value);
      crft.setGaussianPriorVariance(var);
      
      if (Options.weightsOption.value.equals("dense")) {
        crft.setUseSparseWeights(false);
        crft.setUseSomeUnsupportedTrick(false);
      }
      else if (Options.weightsOption.value.equals("some-dense")) {
        crft.setUseSparseWeights(true);
        crft.setUseSomeUnsupportedTrick(true);
      }
      else if (Options.weightsOption.value.equals("sparse")) {
        crft.setUseSparseWeights(true);
        crft.setUseSomeUnsupportedTrick(false);
      }
      else {
        throw new RuntimeException("Unknown weights option: " + Options.weightsOption.value);
      }
      
      if (Options.featureInductionOption.value) {
      	throw new IllegalArgumentException("Multi-threaded feature induction is not yet supported.");
      } else {
      	boolean converged;
      	for (int i = 1; i <= iterations; i++) {
      		converged = crft.train (training, 1);
      		if (i % 1 == 0 && eval != null) // Change the 1 to higher integer to evaluate less often
      			eval.evaluate(crft);
      		if (Options.viterbiOutputOption.value && i % 10 == 0)
      			new ViterbiWriter("", new InstanceList[] {training, testing}, new String[] {"training", "testing"}).evaluate(crft);
      		if (converged)
      			break;
      	}
      }
      crft.shutdown();
    }
    else {
      CRFTrainerByLabelLikelihood crft = new CRFTrainerByLabelLikelihood(crf);
      crft.setGaussianPriorVariance(var);
      
      if (Options.weightsOption.value.equals("dense")) {
        crft.setUseSparseWeights(false);
        crft.setUseSomeUnsupportedTrick(false);
      }
      else if (Options.weightsOption.value.equals("some-dense")) {
        crft.setUseSparseWeights(true);
        crft.setUseSomeUnsupportedTrick(true);
      }
      else if (Options.weightsOption.value.equals("sparse")) {
        crft.setUseSparseWeights(true);
        crft.setUseSomeUnsupportedTrick(false);
      }
      else {
        throw new RuntimeException("Unknown weights option: " + Options.weightsOption.value);
      }
      
      if (Options.featureInductionOption.value) {
      	 crft.trainWithFeatureInduction(training, null, testing, eval, iterations, 10, 20, 500, 0.5, false, null);
      } else {
      	boolean converged;
      	for (int i = 1; i <= iterations; i++) {
      		converged = crft.train (training, 1);
      		if (i % 1 == 0 && eval != null) // Change the 1 to higher integer to evaluate less often
      			eval.evaluate(crft);
      		if (Options.viterbiOutputOption.value && i % 10 == 0)
      			new ViterbiWriter("", new InstanceList[] {training, testing}, new String[] {"training", "testing"}).evaluate(crft);
      		if (converged)
      			break;
      	}
      }
    }
    
    

    return crf;
  }

  /**
   * Test a transducer on the given test data, evaluating accuracy
   * with the given evaluator
   *
   * @param model a <code>Transducer</code>
   * @param eval accuracy evaluator
   * @param testing test data
   */
  public static void test(TransducerTrainer tt, TransducerEvaluator eval,
      InstanceList testing)
  {
    eval.evaluateInstanceList(tt, testing, "Testing");
  }

  /**
   * Apply a transducer to an input sequence to produce the k highest-scoring
   * output sequences.
   *
   * @param model the <code>Transducer</code>
   * @param input the input sequence
   * @param k the number of answers to return
   * @return array of the k highest-scoring output sequences
   */
  public static Sequence[] apply(Transducer model, Sequence input, int k)
  {
    Sequence[] answers;
    if (k == 1) {
      answers = new Sequence[1];
      answers[0] = model.transduce (input);
    }
    else {
      MaxLatticeDefault lattice =
              new MaxLatticeDefault(model, input, null, Options.cacheSizeOption.value());

      answers = lattice.bestOutputSequences(k).toArray(new Sequence[0]);
    }
    return answers;
  }

  /**
   * Command-line wrapper to train, test, or run a generic crf-based tagger.
   *
   * @param args the command line arguments. Options (shell and Java quoting should be added as needed):
   *<dl>
   *<dt><code>--help</code> <em>boolean</em></dt>
   *<dd>Print this command line option usage information.  Give <code>true</code> for longer documentation. Default is <code>false</code>.</dd>
   *<dt><code>--prefix-code</code> <em>Java-code</em></dt>
   *<dd>Java code you want run before any other interpreted code.  Note that the text is interpreted without modification, so unlike some other Java code options, you need to include any necessary 'new's. Default is null.</dd>
   *<dt><code>--gaussian-variance</code> <em>positive-number</em></dt>
   *<dd>The Gaussian prior variance used for training. Default is 10.0.</dd>
   *<dt><code>--train</code> <em>boolean</em></dt>
   *<dd>Whether to train. Default is <code>false</code>.</dd>
   *<dt><code>--iterations</code> <em>positive-integer</em></dt>
   *<dd>Number of training iterations. Default is 500.</dd>
   *<dt><code>--test</code> <code>lab</code> or <code>seg=</code><em>start-1</em><code>.</code><em>continue-1</em><code>,</code>...<code>,</code><em>start-n</em><code>.</code><em>continue-n</em></dt>
   *<dd>Test measuring labeling or segmentation (<em>start-i</em>, <em>continue-i</em>) accuracy. Default is no testing.</dd>
   *<dt><code>--training-proportion</code> <em>number-between-0-and-1</em></dt>
   *<dd>Fraction of data to use for training in a random split. Default is 0.5.</dd>
   *<dt><code>--model-file</code> <em>filename</em></dt>
   *<dd>The filename for reading (train/run) or saving (train) the model. Default is null.</dd>
   *<dt><code>--random-seed</code> <em>integer</em></dt>
   *<dd>The random seed for randomly selecting a proportion of the instance list for training Default is 0.</dd>
   *<dt><code>--orders</code> <em>comma-separated-integers</em></dt>
   *<dd>List of label Markov orders (main and backoff)  Default is 1.</dd>
   *<dt><code>--forbidden</code> <em>regular-expression</em></dt>
   *<dd>If <em>label-1</em><code>,</code><em>label-2</em> matches the expression, the corresponding transition is forbidden. Default is <code>\\s</code> (nothing forbidden).</dd>
   *<dt><code>--allowed</code> <em>regular-expression</em></dt>
   *<dd>If <em>label-1</em><code>,</code><em>label-2</em> does not match the expression, the corresponding expression is forbidden. Default is <code>.*</code> (everything allowed).</dd>
   *<dt><code>--default-label</code> <em>string</em></dt>
   *<dd>Label for initial context and uninteresting tokens. Default is <code>O</code>.</dd>
   *<dt><code>--viterbi-output</code> <em>boolean</em></dt>
   *<dd>Print Viterbi periodically during training. Default is <code>false</code>.</dd>
   *<dt><code>--fully-connected</code> <em>boolean</em></dt>
   *<dd>Include all allowed transitions, even those not in training data. Default is <code>true</code>.</dd>
   *<dt><code>--weights</code> <em>sparse|some-dense|dense</em></dt>
   *<dd>Create sparse, some dense (using a heuristic), or dense crf.features on transitions. Default is <code>some-dense</code>.</dd>
   *<dt><code>--n-best</code> <em>positive-integer</em></dt>
   *<dd>Number of answers to output when applying model. Default is 1.</dd>
   *<dt><code>--include-input</code> <em>boolean</em></dt>
   *<dd>Whether to include input crf.features when printing decoding output. Default is <code>false</code>.</dd>
   *<dt><code>--threads</code> <em>positive-integer</em></dt>
   *<dd>Number of threads for crf training. Default is 1.</dd>
   *</dl>
   * Remaining arguments:
   *<ul>
   *<li><em>training-data-file</em> if training </li>
   *<li><em>training-and-test-data-file</em>, if training and testing with random split</li>
   *<li><em>training-data-file</em> <em>test-data-file</em> if training and testing from separate files</li>
   *<li><em>test-data-file</em> if testing</li>
   *<li><em>input-data-file</em> if applying to new data (unlabeled)</li>
   *</ul>
   * @exception Exception if an error occurs
   */
  public static void main (String[] args) throws Exception
  {
    Reader trainingFile = null, testFile = null;
    InstanceList trainingData = null, testData = null;
    int numEvaluations = 0;
    int iterationsBetweenEvals = 16;
    int restArgs = Options.commandOptions.processOptions(args);
    if (restArgs == args.length)
    {
        Options.commandOptions.printUsage(true);
      throw new IllegalArgumentException("Missing data file(s)");
    }
    if (Options.trainOption.value)
    {
      trainingFile = new FileReader(new File(args[restArgs]));
      if (Options.testOption.value != null && restArgs < args.length - 1)
        testFile = new FileReader(new File(args[restArgs+1]));
    } else 
      testFile = new FileReader(new File(args[restArgs]));

    Pipe p = null;
    CRF crf = null;
    TransducerEvaluator eval = null;
    if (Options.continueTrainingOption.value || !Options.trainOption.value) {
      if (Options.modelOption.value == null)
      {
          Options.commandOptions.printUsage(true);
        throw new IllegalArgumentException("Missing model file option");
      }
      ObjectInputStream s =
        new ObjectInputStream(new FileInputStream(Options.modelOption.value));
      crf = (CRF) s.readObject();
      s.close();
      p = crf.getInputPipe();
    }
    else {
      p = new SimpleTaggerSentence2FeatureVectorSequence();
      p.getTargetAlphabet().lookupIndex(Options.defaultOption.value);
    }


    if (Options.trainOption.value)
    {
      p.setTargetProcessing(true);
      trainingData = new InstanceList(p);
      trainingData.addThruPipe(
          new LineGroupIterator(trainingFile,
            Pattern.compile("^\\s*$"), true));
      logger.info
        ("Number of crf.features in training data: "+p.getDataAlphabet().size());
      if (Options.testOption.value != null)
      {
        if (testFile != null)
        {
          testData = new InstanceList(p);
          testData.addThruPipe(
              new LineGroupIterator(testFile,
                Pattern.compile("^\\s*$"), true));
        } else
        {
          Random r = new Random (Options.randomSeedOption.value);
          InstanceList[] trainingLists =
            trainingData.split(
                r, new double[] {Options.trainingFractionOption.value,
                  1-Options.trainingFractionOption.value});
          trainingData = trainingLists[0];
          testData = trainingLists[1];
        }
      }
    } else if (Options.testOption.value != null)
    {
      p.setTargetProcessing(true);
      testData = new InstanceList(p);
      testData.addThruPipe(
          new LineGroupIterator(testFile,
            Pattern.compile("^\\s*$"), true));
    } else
    {
      p.setTargetProcessing(false);
      testData = new InstanceList(p);
      testData.addThruPipe(
          new LineGroupIterator(testFile,
            Pattern.compile("^\\s*$"), true));
    }
    logger.info ("Number of predicates: "+p.getDataAlphabet().size());
    
    
    if (Options.testOption.value != null)
    {
      if (Options.testOption.value.startsWith("lab"))
        eval = new TokenAccuracyEvaluator(new InstanceList[] {trainingData, testData}, new String[] {"Training", "Testing"});
      else if (Options.testOption.value.startsWith("seg="))
      {
        String[] pairs = Options.testOption.value.substring(4).split(",");
        if (pairs.length < 1)
        {
            Options.commandOptions.printUsage(true);
          throw new IllegalArgumentException(
              "Missing segment start/continue labels: " + Options.testOption.value);
        }
        String startTags[] = new String[pairs.length];
        String continueTags[] = new String[pairs.length];
        for (int i = 0; i < pairs.length; i++)
        {
          String[] pair = pairs[i].split("\\.");
          if (pair.length != 2)
          {
              Options.commandOptions.printUsage(true);
            throw new
              IllegalArgumentException(
                  "Incorrectly-specified segment start and end labels: " +
                  pairs[i]);
          }
          startTags[i] = pair[0];
          continueTags[i] = pair[1];
        }
        eval = new MultiSegmentationEvaluator(new InstanceList[] {trainingData, testData}, new String[] {"Training", "Testing"},
        		startTags, continueTags);
      }
      else
      {
          Options.commandOptions.printUsage(true);
        throw new IllegalArgumentException("Invalid test option: " +
                Options.testOption.value);
      }
    }
    
    
    
    if (p.isTargetProcessing())
    {
      Alphabet targets = p.getTargetAlphabet();
      StringBuffer buf = new StringBuffer("Labels:");
      for (int i = 0; i < targets.size(); i++)
        buf.append(" ").append(targets.lookupObject(i).toString());
      logger.info(buf.toString());
    }
    if (Options.trainOption.value)
    {
      crf = train(trainingData, testData, eval,
              Options.ordersOption.value, Options.defaultOption.value,
              Options.forbiddenOption.value, Options.allowedOption.value,
              Options.connectedOption.value, Options.iterationsOption.value,
              Options.gaussianVarianceOption.value, crf);
      if (Options.modelOption.value != null)
      {
        ObjectOutputStream s =
          new ObjectOutputStream(new FileOutputStream(Options.modelOption.value));
        s.writeObject(crf);
        s.close();
      }
    }
    else
    {
      if (crf == null)
      {
        if (Options.modelOption.value == null)
        {
            Options.commandOptions.printUsage(true);
          throw new IllegalArgumentException("Missing model file option");
        }
        ObjectInputStream s =
          new ObjectInputStream(new FileInputStream(Options.modelOption.value));
        crf = (CRF) s.readObject();
        s.close();
      }
      if (eval != null)
        test(new NoopTransducerTrainer(crf), eval, testData);
      else
      {
        boolean includeInput = Options.includeInputOption.value();
        for (int i = 0; i < testData.size(); i++)
        {
          Sequence input = (Sequence)testData.get(i).getData();
          Sequence[] outputs = apply(crf, input, Options.nBestOption.value);
          int k = outputs.length;
          boolean error = false;
          for (int a = 0; a < k; a++) {
            if (outputs[a].size() != input.size()) {
              logger.info("Failed to decode input sequence " + i + ", answer " + a);
              error = true;
            }
          }
          if (!error) {
            for (int j = 0; j < input.size(); j++)
            {
               StringBuffer buf = new StringBuffer();
              for (int a = 0; a < k; a++)
                 buf.append(outputs[a].get(j).toString()).append(" ");
              if (includeInput) {
                FeatureVector fv = (FeatureVector)input.get(j);
                buf.append(fv.toString(true));                
              }
              System.out.println(buf.toString());
            }
            System.out.println();
          }
        }
      }
    }
  }
}
