/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */




/**
 @author Ryan McDonald <a href="mailto:ryantm@cis.upenn.edu">ryantm@cis.upenn.edu</a>
 */

//package edu.umass.cs.mallet.users.ryantm.medline;

import edu.umass.cs.mallet.base.types.*;
import edu.umass.cs.mallet.base.fst.*;
import edu.umass.cs.mallet.base.minimize.*;
import edu.umass.cs.mallet.base.minimize.tests.*;
import edu.umass.cs.mallet.base.pipe.*;
import edu.umass.cs.mallet.base.pipe.iterator.*;
import edu.umass.cs.mallet.base.pipe.tsf.*;
import junit.framework.*;
import java.util.Iterator;
import java.util.Random;
import java.util.regex.*;
import java.util.ArrayList;

import java.io.*;

public class biocreative
{
    int numEvaluations = 0;
    static int iterationsBetweenEvals = 16;

    private static String CAPS = "[A-Z]";
    private static String LOW = "[a-z]";
    private static String CAPSNUM = "[A-Z0-9]";
    private static String ALPHA = "[A-Za-z]";
    private static String ALPHANUM = "[A-Za-z0-9]";
    private static String PUNT = "[,\\.;:?!]";
    private static String QUOTE = "[\"`']";
    private static String SEQ = "[atgcu]+";
    private static String BADSUFFIX = ".*ole|.*ane|.*ate|.*ide|.*ine|.*ite|.*ol|.*ose|.*cooh|.*ar|.*ic|.*al|.*ive|.*ly|.*yl|.*ing|.*ry|.*ian|.*ent|.*ward|.*fold|.*ene|.*ory|.*ized|.*ible|.*ize|.*izes|.*ed|.*tion|.*ity|.*ure|.*ence";
    private static String GOODSUFFIX = ".*gene|.*like|.*ase|homeo.*";

    public static void main (String[] args) throws FileNotFoundException, Exception
    {
        String BASELISTS = args[4];
        String ABGENELISTS = args[5];
        String CLUSTERDIR = args[6];

        CRF4 crf = null;
        InstanceList trainingData, testingData = null;

        if(args[0].equals("train"))
        {
            ArrayList al = new ArrayList(1024);
            al.add(new BioCreativeSentence2TokenSequence ());

            // Pattern matching features on the words
            //al.add(new RegexMatches("DUMMY", Pattern.compile(".*")));
            al.add(new RegexMatches ("INITCAP", Pattern.compile (CAPS+".*")));
            al.add(new RegexMatches ("CAPITALIZED", Pattern.compile (CAPS+LOW+"*")));
            al.add(new RegexMatches ("ALLCAPS", Pattern.compile (CAPS+"+")));
            al.add(new RegexMatches ("MIXEDCAPS", Pattern.compile ("[A-Z][a-z]+[A-Z][A-Za-z]*")));
            al.add(new RegexMatches ("CONTAINSDIGITS", Pattern.compile (".*[0-9].*")));
            al.add(new RegexMatches ("SINGLEDIGITS", Pattern.compile ("[0-9]")));
            al.add(new RegexMatches ("DOUBLEDIGITS", Pattern.compile ("[0-9][0-9]")));
            al.add(new RegexMatches ("ALLDIGITS", Pattern.compile ("[0-9]+")));
            al.add(new RegexMatches ("NUMERICAL", Pattern.compile ("[-0-9]+[\\.,]+[0-9\\.,]+")));
            al.add(new RegexMatches ("ALPHNUMERIC", Pattern.compile ("[A-Za-z0-9]+")));
            al.add(new RegexMatches ("ROMAN", Pattern.compile ("[ivxdlcm]+|[IVXDLCM]+")));
            al.add(new RegexMatches ("MULTIDOTS", Pattern.compile ("\\.\\.+")));
            al.add(new RegexMatches ("ENDSINDOT", Pattern.compile ("[^\\.]+.*\\.")));
            al.add(new RegexMatches ("CONTAINSDASH", Pattern.compile (ALPHANUM+"+-"+ALPHANUM+"*")));
            al.add(new RegexMatches ("ACRO", Pattern.compile ("[A-Z][A-Z\\.]*\\.[A-Z\\.]*")));
            al.add(new RegexMatches ("LONELYINITIAL", Pattern.compile (CAPS+"\\.")));
            al.add(new RegexMatches ("SINGLECHAR", Pattern.compile (ALPHA)));
            al.add(new RegexMatches ("CAPLETTER", Pattern.compile ("[A-Z]")));
            al.add(new RegexMatches ("PUNC", Pattern.compile (PUNT)));
            al.add(new RegexMatches ("QUOTE", Pattern.compile (QUOTE)));
            al.add(new RegexMatches ("STARTDASH", Pattern.compile ("-.*")));
            al.add(new RegexMatches ("ENDDASH", Pattern.compile (".*-")));
            al.add(new RegexMatches ("FORWARDSLASH", Pattern.compile ("/")));
            al.add(new RegexMatches ("ISBRACKET", Pattern.compile ("[()]")));

            /////////////////////////////////////////////////////////
            //  INSERT LIST FEATURES HERE

            String[] clusterfiles = getClusters(CLUSTERDIR);
            System.err.println(" Found " + clusterfiles.length + " clusters.");

            for (int f = 0; f < clusterfiles.length; f++)
            {
                String clustername = clusterfiles[f];
                String clusterfile = CLUSTERDIR +"/"+ clustername;
                al.add(new TrieLexiconMembership(("CLUSTER_"+clustername), new File(clusterfile), true));
            }

            al.add(new FeaturesInWindow("WINDOW=",-1,1,Pattern.compile("^CLUSTER_.+"),true));

            //  END LIST FEATURES ...
            /////////////////////////////////////////////////////////

            al.add(new TokenSequenceLowercase());
            al.add(new RegexMatches ("SEQUENCE",Pattern.compile(SEQ)));

            // Make the word a feature
            al.add(new TokenText ("WORD="));
            al.add(new TokenTextCharSuffix("SUFFIX2=",2));
            al.add(new TokenTextCharSuffix("SUFFIX3=",3));
            al.add(new TokenTextCharSuffix("SUFFIX4=",4));
            al.add(new TokenTextCharPrefix("PREFIX2=",2));
            al.add(new TokenTextCharPrefix("PREFIX3=",3));
            al.add(new TokenTextCharPrefix("PREFIX4=",4));

            al.add(new InBracket("INBRACKET",true));

            // FeatureInWindow features
            al.add(new FeaturesInWindow("WINDOW=",-1,1,Pattern.compile("WORD=.*|SUFFIX.*|PREFIX.*|[A-Z]+"),true));

            al.add(new TokenTextCharNGrams ("CHARNGRAM=", new int[] {2,3,4}));

            // List membership criteria - from hugo and NCBI
            // - gene lists
            String hugodir = "lists/hugo_searchdata";

            if (BASELISTS.equals("1")) // HUGO ONLY
            {
                System.err.println(" + hugo baselists");
                al.add(new TrieLexiconMembership("HUGO3",  new File(hugodir+"/searchdata3"), true));
                al.add(new TrieLexiconMembership("HUGO4",  new File(hugodir+"/searchdata4"), true));
                al.add(new TrieLexiconMembership("HUGO5",  new File(hugodir+"/searchdata5"), true));
                al.add(new TrieLexiconMembership("HUGO8",  new File(hugodir+"/searchdata8"), true));
                al.add(new TrieLexiconMembership("HUGO13", new File(hugodir+"/searchdata13"), true));
            }
            else if (BASELISTS.equals("2")) // GENELEXICON ONLY
            {
                System.err.println(" + genelex baselists");
                al.add(new TrieLexiconMembership("GENELIST2",new File("lists/gene_lexicon/Gene.Lexicon.norm"),true));
            }
            else if (BASELISTS.equals("3")) // HUGO AND GENELEXICON
            {
                System.err.println(" + hugo & genelex baselists");
                al.add(new TrieLexiconMembership("HUGO3",  new File(hugodir+"/searchdata3"), true));
                al.add(new TrieLexiconMembership("HUGO4",  new File(hugodir+"/searchdata4"), true));
                al.add(new TrieLexiconMembership("HUGO5",  new File(hugodir+"/searchdata5"), true));
                al.add(new TrieLexiconMembership("HUGO8",  new File(hugodir+"/searchdata8"), true));
                al.add(new TrieLexiconMembership("HUGO13", new File(hugodir+"/searchdata13"), true));
                al.add(new TrieLexiconMembership("GENELIST2",new File("lists/gene_lexicon/Gene.Lexicon.norm"),true));
            }
            else System.err.println(" - baselists");


            if (ABGENELISTS.equals("1"))
            {
                System.err.println(" + abgenelists");

                al.add(new TrieLexiconMembership("GENELIST3",new File("lists/abgene_lists/singlegenes.lst.norm"),true));
                al.add(new TrieLexiconMembership("GENELIST4",new File("lists/abgene_lists/multigenes.lst.norm"),true));
                al.add(new TrieLexiconMembership("GENELIST5",new File("lists/abgene_lists/othergenes.lst.norm"),true));
                al.add(new TrieLexiconMembership("GENETERMS",new File("lists/abgene_lists/geneterms.lst.norm"),true));

                // - False positive filter lists
                al.add(new TrieLexiconMembership("NOTGENE1",new File("lists/abgene_lists/genbio.lst.norm"),true));
                al.add(new TrieLexiconMembership("NOTGENE2",new File("lists/abgene_lists/aminoacids.lst.norm"),true));
                al.add(new TrieLexiconMembership("NOTGENE3",new File("lists/abgene_lists/restenzymes.lst.norm"),true));
                al.add(new TrieLexiconMembership("NOTGENE4",new File("lists/abgene_lists/celllines.lst.norm"),true));
                al.add(new TrieLexiconMembership("NOTGENE5",new File("lists/abgene_lists/organismsNCBI.lst.norm"),true));
                al.add(new TrieLexiconMembership("NOTGENE6",new File("lists/abgene_lists/nonbio.lst.norm"),true));
                al.add(new TrieLexiconMembership("NOTGENE7",new File("lists/abgene_lists/stopwords.lst.norm"),true));
                al.add(new TrieLexiconMembership("NOTGENE8",new File("lists/abgene_lists/units.lst.norm"),true));
                // - context lists
                al.add(new TrieLexiconMembership(new File("lists/abgene_lists/contextbefore.lst.norm"),true));
                al.add(new TrieLexiconMembership(new File("lists/abgene_lists/contextafter.lst.norm"),true));
            }
            else { System.err.println(" - abgenelists"); }

            // List FeatureInWin
            al.add(new FeaturesInWindow("WINDOW=",-1,1,Pattern.compile("GENELIST.*"),true));
            al.add(new FeaturesInWindow("WINDOW=",-1,1,Pattern.compile("GENETERMS"),true));
            al.add(new FeaturesInWindow("WINDOW=",-1,1,Pattern.compile("NOTGENE.*"),true));
            al.add(new FeaturesInWindow("WINDOW=",-1,0,Pattern.compile("contextbefore.lst.norm"),true));
            al.add(new FeaturesInWindow("WINDOW=",0,1,Pattern.compile("contextafter.lst.norm"),true));

            // - low freq tri grams
            //al.add(new ContainsLowFreqTriGram("lowfreqtri.norm",new File("data/abgene_lists/lowfreqtri.lst.norm"),true));
            //al.add(new FeaturesInWindow("WINDOW=",-1,1,Pattern.compile("lowfreqtri.norm"),true));

            //new PrintTokenSequenceFeatures(),
            al.add(new TokenSequence2FeatureVectorSequence (true, true));

            //  CREATE THE FEATURE PIPE
            Pipe p = new SerialPipes(al.toArray(new Pipe[0]));

            trainingData = new InstanceList(p);
            trainingData.add(new LineGroupIterator(new FileReader(new File (args[1])), Pattern.compile("^$"), true));
            System.exit(0);
            System.out.println ("Number of predicates in training data: "+p.getDataAlphabet().size());

            Alphabet data = p.getDataAlphabet();
            Alphabet targets = p.getTargetAlphabet();
            //data.stopGrowth();
            //targets.stopGrowth();

            testingData = null;
            if (args.length > 2 && !args[2].equals("null")) {
                testingData = new InstanceList (p);
                testingData.add (new LineGroupIterator (new FileReader (new File (args[2])), Pattern.compile("^$"), true));
            }

            crf = null;
            GeneSegmentationEvaluator eval =
                    new GeneSegmentationEvaluator (new String[] {"B-GENE"},
                            new String[] {"I-GENE"});

            crf = new CRF4 (p, null);
            crf.addOrderNStates(trainingData, new int[] {0,1,2},null,"O",Pattern.compile("O,I-GENE"),null,true);
            crf.setGaussianPriorVariance (1.0);
            for (int i = 0; i < crf.numStates(); i++)
                crf.getState(i).setInitialCost (Double.POSITIVE_INFINITY);
            crf.getState("O,O").setInitialCost (0.0);

            System.out.println("Training on "+trainingData.size()+" training instances, "+
                    testingData.size()+" testing instances...");

            //crf.train (trainingData, null, testingData, eval, 80);
            crf.trainWithFeatureInduction(trainingData,null,testingData,eval,310,10,30,700,0.5,false,null);

            System.err.println("DONE TRAINING! ABOUT TO WRITE MODEL: " + args[3]);
            System.out.println("DONE TRAINING! ABOUT TO WRITE MODEL: " + args[3]);

            if(args.length > 3) crf.write(new File(args[3]));
            else crf.write(new File("model.crf"));
        }
        else if(args[0].equals("test"))
        {
            GeneSegmentationEvaluator eval =
                    new GeneSegmentationEvaluator (new String[] {"B-GENE"},
                            new String[] {"I-GENE"});

            ObjectInputStream ois;
            if(args.length > 3)
                ois = new ObjectInputStream(new FileInputStream(args[3]));
            else
                ois = new ObjectInputStream(new FileInputStream("model.crf"));

            crf = (CRF4)ois.readObject();
            crf.getInputAlphabet().stopGrowth();

            SerialPipes p = (SerialPipes)crf.getInputPipe();

            testingData = null;
            if (args.length > 2 && !args[2].equals("null"))
            {
                testingData = new InstanceList (p);
                testingData.add (new LineGroupIterator (new FileReader (new File (args[2])), Pattern.compile("^$"), true));
            }

            crf.evaluate(eval,testingData);

            if (crf != null)
            {
                GeneSegmentationOutput gso = new GeneSegmentationOutput();
                gso.ted_output(args[2],crf,testingData);
            }
        }
    }

    public static String[] getClusters(String dir)
    {
        File d = new File(dir);

        if (!d.isDirectory())
        {
            System.err.println("Error: file `" + dir + "' is not a directory.");
            System.exit(0);
        }

        return d.list();
    }
}