package cc.mallet.share.canan.extractor;

import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.SimpleTaggerSentence2TokenSequence;
import cc.mallet.pipe.TokenSequence2FeatureVectorSequence;
import cc.mallet.pipe.iterator.LineGroupIterator;
import cc.mallet.pipe.tsf.*;
import cc.mallet.types.InstanceList;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class TestCRFPipe {
	
	public TestCRFPipe(String trainingFilename) throws IOException {
		
		ArrayList<Pipe> pipes = new ArrayList<Pipe>();

		PrintWriter out = new PrintWriter("test.out");

		int[][] conjunctions = new int[3][];
		conjunctions[0] = new int[] { -1 };
		conjunctions[1] = new int[] { 1 };
		conjunctions[2] = new int[] { -2, -1 };

		pipes.add(new SimpleTaggerSentence2TokenSequence());
		//pipes.add(new FeaturesInWindow("PREV-", -1, 1));
		//pipes.add(new FeaturesInWindow("NEXT-", 1, 2));
		pipes.add(new OffsetConjunctions(conjunctions));
		pipes.add(new TokenTextCharSuffix("C1=", 1));
		pipes.add(new TokenTextCharSuffix("C2=", 2));
		pipes.add(new TokenTextCharSuffix("C3=", 3));
		pipes.add(new RegexMatches("CAPITALIZED", Pattern.compile("^\\p{Lu}.*")));
		pipes.add(new RegexMatches("STARTSNUMBER", Pattern.compile("^[0-9].*")));
		pipes.add(new RegexMatches("HYPHENATED", Pattern.compile(".*\\-.*")));
		pipes.add(new RegexMatches("DOLLARSIGN", Pattern.compile("\\$.*")));
		pipes.add(new TokenFirstPosition("FIRSTTOKEN"));
		pipes.add(new TokenSequence2FeatureVectorSequence());
		pipes.add(new SequencePrintingPipe(out));
           /*
           * 	new ConllNer2003Sentence2TokenSequence (),
			new RegexMatches ("INITCAP", Pattern.compile (CAPS+".*")),
			new RegexMatches ("CAPITALIZED", Pattern.compile (CAPS+LOW+"*")),
			new RegexMatches ("ALLCAPS", Pattern.compile (CAPS+"+")),
			new RegexMatches ("MIXEDCAPS", Pattern.compile ("[A-Z][a-z]+[A-Z][A-Za-z]*")),
			new RegexMatches ("CONTAINSDIGITS", Pattern.compile (".*[0-9].*")),
			new RegexMatches ("ALLDIGITS", Pattern.compile ("[0-9]+")),
			new RegexMatches ("NUMERICAL", Pattern.compile ("[-0-9]+[\\.,]+[0-9\\.,]+")),
			//new RegexMatches ("ALPHNUMERIC", Pattern.compile ("[A-Za-z0-9]+")),
			//new RegexMatches ("ROMAN", Pattern.compile ("[ivxdlcm]+|[IVXDLCM]+")),
			new RegexMatches ("MULTIDOTS", Pattern.compile ("\\.\\.+")),
			new RegexMatches ("ENDSINDOT", Pattern.compile ("[^\\.]+.*\\.")),
			new RegexMatches ("CONTAINSDASH", Pattern.compile (ALPHANUM+"+-"+ALPHANUM+"*")),
			new RegexMatches ("ACRO", Pattern.compile ("[A-Z][A-Z\\.]*\\.[A-Z\\.]*")),
			new RegexMatches ("LONELYINITIAL", Pattern.compile (CAPS+"\\.")),
			new RegexMatches ("SINGLECHAR", Pattern.compile (ALPHA)),
			new RegexMatches ("CAPLETTER", Pattern.compile ("[A-Z]")),
			new RegexMatches ("PUNC", Pattern.compile (PUNT)),
			new RegexMatches ("QUOTE", Pattern.compile (QUOTE)),
			//new RegexMatches ("LOWER", Pattern.compile (LOW+"+")),
			//new RegexMatches ("MIXEDCAPS", Pattern.compile ("[A-Z]+[a-z]+[A-Z]+[a-z]*")),
           * */
		Pipe pipe = new SerialPipes(pipes);

		InstanceList trainingInstances = new InstanceList(pipe);

		trainingInstances.addThruPipe(new LineGroupIterator(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(trainingFilename)))), Pattern.compile("^\\s*$"), true));

		out.close();
		
	}

	public static void main (String[] args) throws Exception {
		TestCRFPipe trainer = new TestCRFPipe(args[0]);

	}

}