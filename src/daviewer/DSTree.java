package daviewer;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import daviewer.DSTemplateTree.DSTemplateTreeNode;


public class DSTree implements Serializable {
	
	private static final long serialVersionUID = 1210560467061637637L;

	public enum Nuclearity {
		Any, Nucleus, Satellite
	};

	public enum Relation {
		None, Attribution, Background, Cause, Comparison, Condition, Contrast, 
		Elaboration, Enablement, Evaluation, Explanation, Joint, Manner_means, 
		Summary, Temporal, Topic_change, Topic_comment, Same_unit, Textual_organization,
		Any
	};

	private static final String[] relationString = { 
		"attribution", "attribution-negative",
		"background", "circumstance",
		"cause", "consequence", "result",
		"comparison", "preference", "analogy", "proportion",
		"condition", "hypothetical", "contingency", "otherwise",
		"contrast", "concession", "antithesis",
		"elaboration", "elaboration-additional", "elaboration-general-specific", "elaboration-part-whole",
			"elaboration-process-step", "elaboration-object-attribute", "elaboration-set-member", "example", "definition",
		"enablement", "purpose",
		"evaluation", "interpretation", "conclusion", "comment",
		"explanation", "evidence", "explanation-argumentative", "reason",
		"joint", "list", "disjunction",
		"manner-means", "manner", "means",
		"summary", "restatement",
		"temporal", "temporal-before", "temporal-after", "temporal-same-time", "sequence", "invertedsequence",
		"topic-change", "topic-shift", "topic-drift",
		"topic-comment", "problem-solution", "question-answer", "statement-response", "topic-comment", "comment-topic", "rhetorical-question",
		"same-unit", 
		"textual-organization", "textualorganization"
		};
	
	private static final int[] relationMap = {
		1, 1,
		2, 2,
		3, 3, 3,
		4, 4, 4, 4,
		5, 5, 5, 5,
		6, 6, 6,
		7, 7, 7, 7, 7, 7, 7, 7, 7,
		8, 8,
		9, 9, 9, 9,
		10, 10, 10, 10,
		11, 11, 11,
		12, 12, 12,
		13, 13,
		14, 14, 14, 14, 14, 14,
		15, 15, 15,
		16, 16, 16, 16, 16, 16, 16,
		17,
		18, 18
	};
	

	public DSTreeNode rootNode;
	public ArrayList<String> article;
	public ArrayList<DSTreeNode>[] nodeLevelList;
	public Range[] pathList;
	public ArrayList<NodeNote> notes = new ArrayList<NodeNote>();
	public int[] relationDist = new int[Relation.values().length - 1];
	public int hitnum = 0;
	public boolean isReference = false;
	
	private int count = 0;

	// public methods//////////////////////////////////////////////////////////////////////////////
	public int getTreeLevels() {
		return rootNode.height + 1;
	}
	
	public void loadFromFile(File f) throws IOException {
		if(f.getName().endsWith("hilda.tree"))
			loadFromFileHILDA(f);
		else if(f.getName().endsWith("dis"))
			loadFromFileDIS(f);
		
		generateUntilLists();
	}
	
	public boolean compareToTree(DSTree refTree) {
		if(article.size() != refTree.article.size()){
			rootNode.score = -1;
			return false;	// must have the same number of leaf nodes
		}
		
		for(int i = 1; i < nodeLevelList.length; i++) {
			for(DSTreeNode node : nodeLevelList[i]) {
				ArrayList<Range> refTreePathSet = refTree.getPathSet(node.textRange);
				node.score = computeScore(getPathSet(node), refTreePathSet);
			}
		}
		
		return true;
	}

	public void searchTree(DSTemplateTree template, boolean isnew){
		if(isnew) {
			hitnum = 0;
			for(int i = 0; i < nodeLevelList.length; i++) {
				if(i < template.rootNode.height) 
					for(DSTreeNode node : nodeLevelList[i])
						node.matched = false;
				else
					for(DSTreeNode node : nodeLevelList[i]) {
						boolean result = matchNode(node, template.rootNode);
						node.matched = result;
						if(result)
							hitnum++;
					}
			}
		}
		else {
			for(int i = 0; i < nodeLevelList.length; i++) {
				for(DSTreeNode node : nodeLevelList[i]) {
					if(!node.matched)
						continue;
					
					if(i < template.rootNode.height) {
						node.matched = false;
						hitnum--;
						continue;
					}
					
					boolean result = matchNode(node, template.rootNode);
					if(!result){
						node.matched = false;
						hitnum--;
					}
				}
			}
		}
	}
	
	public void searchText(String text, boolean isnew){
		if(isnew) {
			clearMatched(rootNode);
			hitnum = 0;
			for(int i = 0; i < article.size(); i++)
				if(article.get(i).contains(text)){
					DSTreeNode node = nodeLevelList[0].get(i);
					node.matched = true;
					hitnum++;
					
					while(node.parent != null && !node.parent.matched){
						node.parent.matched = true;
						node = node.parent;
						hitnum++;
					}
				}
		}
		else {
			//no need in this program, but better to implement it
		}
	}

	public ArrayList<Range> getPathSet(Range idRange) {
		ArrayList<Range> pathset = new ArrayList<Range>();
		
		int i = idRange.start;
		while(i <= idRange.end) {
			DSTreeNode node = nodeLevelList[0].get(i);
			while(node.parent != null && node.parent.textRange.start >= idRange.start 
					&& node.parent.textRange.end <= idRange.end)
				node = node.parent;
			pathset.addAll(getPathSet(node));
			
			i = node.textRange.end + 1;
		}
		
		return pathset;
	}
	
	public ArrayList<Range> getPathSet(DSTreeNode node) {
		ArrayList<Range> pathset = new ArrayList<Range>(node.dfsID.end - node.dfsID.start + 1);
		for(int i = node.dfsID.start; i <= node.dfsID.end; i++)
			pathset.add(pathList[i]);
		return pathset;
	}
	
	// private methods//////////////////////////////////////////////////////////////////////////////
	private void loadFromFileDIS(File f) throws IOException {
		BufferedReader input = new BufferedReader(new FileReader(f));
		StringBuilder treetext = new StringBuilder();
		try{		
			String line = input.readLine();
			while(line != null){
				treetext.append(line.trim());
				line = input.readLine();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		} finally {
			input.close();
		}
			
		Pattern nodep = Pattern.compile("\\(\\s([A-Za-z]*)\\s\\([^\\(\\)]*\\)\\s*(\\(rel2par\\s(\\S*)\\))?");
		Matcher nodem = nodep.matcher(treetext);
		Pattern leafp = Pattern.compile("\\(text _!([^_]*)_!\\)\\s*(\\)*)");
		
		int start = 0;
		if(nodem.find())  // root node
			rootNode = new DSTreeNode();
		else
			return;
		
		Stack<DSTreeNode> stack = new Stack<DSTreeNode>();
		stack.push(rootNode);
		article = new ArrayList<String>();
		start = nodem.end();
		while(nodem.find()) {
			if(start != nodem.start()) { // leaf nodes inside
				String leaftext = treetext.substring(start, nodem.start());
				Matcher leafm = leafp.matcher(leaftext);
				
				while(leafm.find()) {
					article.add(leafm.group(1));
					
					//stack.peek().textID = article.size() - 1;
					//stack.peek().textSpan = 1;
					stack.peek().textRange.start = article.size() - 1;
					stack.peek().textRange.end = article.size() - 1;
					stack.peek().height = 0;
					
					for(int i = 0; i < leafm.group(2).length(); i++)
						stack.pop();							
				}
			}
			
			// process new node
			DSTreeNode newnode = new DSTreeNode();
			if(nodem.group(1).equals("Nucleus"))
				newnode.nuclearity = Nuclearity.Nucleus;
			else
				newnode.nuclearity = Nuclearity.Satellite;
			DSTreeNode parent = stack.peek();
			newnode.parent = parent;
			if(nodem.group(3) != null && !nodem.group(3).equals("span"))
				parent.relation = getRelation(nodem.group(3));
			
			if(parent.leftChild == null)
				parent.leftChild = newnode;
			else if(parent.rightChild == null)
				parent.rightChild = newnode;
			else {	// resolve n-ary tree to binary tree
				while(parent.rightChild != null)
					parent = parent.rightChild;
				parent = parent.parent;
				
				DSTreeNode newparent = new DSTreeNode();
				newparent.relation = parent.relation;
				
				newparent.leftChild = parent.rightChild;
				parent.rightChild.parent = newparent;
				newparent.rightChild = newnode;
			
				newnode.parent = newparent;
				parent.rightChild = newparent;
			}
			
			stack.push(newnode);
			start = nodem.end();
		}
		
		// process the rest
		String leaftext = treetext.substring(start, treetext.length());
		Matcher leafm = leafp.matcher(leaftext);
		
		while(leafm.find()) {
			article.add(leafm.group(1));
			
			stack.peek().textRange.start = article.size() - 1;
			stack.peek().textRange.end = article.size() - 1;
			stack.peek().height = 0;
			
			for(int i = 0; i < leafm.group(2).length(); i++){
				DSTreeNode popnode = stack.pop();
				if(popnode.leftChild != null && popnode.rightChild != null){
					popnode.textRange.start = popnode.leftChild.textRange.start;
					popnode.textRange.end = popnode.rightChild.textRange.end;
					popnode.height = popnode.leftChild.height > popnode.rightChild.height ? popnode.leftChild.height + 1
							: popnode.rightChild.height + 1;
				}
			}		
		}
		
		computeHeightAndTextspan(rootNode);
	}
	
	private void loadFromFileHILDA(File f) throws IOException {
		BufferedReader input = new BufferedReader(new FileReader(f));
		StringBuilder treetext = new StringBuilder();
		try{		
			String line = input.readLine();
			while(line != null){
				treetext.append(line.trim());
				line = input.readLine();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		} finally {
			input.close();
		}
		
		Pattern nodep = Pattern.compile("\\(([a-zA-Z-]*)\\[(N|S)\\]\\[(N|S)\\]");
		Matcher nodem = nodep.matcher(treetext);
		Pattern leafp = Pattern.compile("((\'([^\']*)\')|(\"([^\"]*)\"))(\\)*)");
		
		int start = 0;
		if(nodem.find()) { // root node
			rootNode = new DSTreeNode();
			rootNode.relation = getRelation(nodem.group(1));
			rootNode.leftChild = new DSTreeNode();
			rootNode.rightChild = new DSTreeNode();
			rootNode.leftChild.parent = rootNode;
			rootNode.rightChild.parent = rootNode;
			if (nodem.group(2).equals("N"))
				rootNode.leftChild.nuclearity = Nuclearity.Nucleus;
			else
				rootNode.leftChild.nuclearity = Nuclearity.Satellite;
			if (nodem.group(3).equals("N"))
				rootNode.rightChild.nuclearity = Nuclearity.Nucleus;
			else
				rootNode.rightChild.nuclearity = Nuclearity.Satellite;
		}
		else
			return;
		
		Stack<DSTreeNode> stack = new Stack<DSTreeNode>();
		stack.push(rootNode);
		article = new ArrayList<String>();
		start = nodem.end();		
		while(nodem.find()) {			
			if(start != nodem.start()) { // leaf nodes inside
				String leaftext = treetext.substring(start, nodem.start());
				Matcher leafm = leafp.matcher(leaftext);
				
				while(leafm.find()) {
					article.add(leafm.group(3) == null ? leafm.group(5) : leafm.group(3));
					
					if(leafm.group(6).isEmpty()) { // left leaf node						
						stack.peek().leftChild.textRange.start = article.size() - 1;
						stack.peek().leftChild.textRange.end = article.size() - 1;
						stack.peek().leftChild.height = 0;
					}
					else { // right leaf node
						stack.peek().rightChild.textRange.start = article.size() - 1;
						stack.peek().rightChild.textRange.end = article.size() - 1;
						stack.peek().rightChild.height = 0;
						
						for(int i = 0; i < leafm.group(6).length(); i++) {
							DSTreeNode popnode = stack.pop();
							popnode.textRange.start = popnode.leftChild.textRange.start;
							popnode.textRange.end = popnode.rightChild.textRange.end;
							popnode.height = popnode.leftChild.height > popnode.rightChild.height ? popnode.leftChild.height + 1
									: popnode.rightChild.height + 1;
						}							
					}
				}
			}
			
			//process new node
			DSTreeNode newnode;
			if (stack.peek().leftChild.relation == Relation.None && stack.peek().leftChild.textRange.start == -1)
				newnode = stack.peek().leftChild;
			else
				newnode = stack.peek().rightChild;
			
			newnode.relation = getRelation(nodem.group(1));
			newnode.leftChild = new DSTreeNode();
			newnode.rightChild = new DSTreeNode();
			newnode.leftChild.parent = newnode;
			newnode.rightChild.parent = newnode;
			
			if (nodem.group(2).equals("N"))
				newnode.leftChild.nuclearity = Nuclearity.Nucleus;
			else
				newnode.leftChild.nuclearity = Nuclearity.Satellite;
			if (nodem.group(3).equals("N"))
				newnode.rightChild.nuclearity = Nuclearity.Nucleus;
			else
				newnode.rightChild.nuclearity = Nuclearity.Satellite;
			
			stack.push(newnode);
			start = nodem.end();
		}
		
		// parse the rest, basically leaf nodes
		String leaftext = treetext.substring(start, treetext.length());
		Matcher leafm = leafp.matcher(leaftext);
		
		while(leafm.find()) {
			article.add(leafm.group(3) == null ? leafm.group(5) : leafm.group(3));
			
			if(leafm.group(6).isEmpty()) { // left leaf node						
				stack.peek().leftChild.textRange.start = article.size() - 1;
				stack.peek().leftChild.textRange.end = article.size() - 1;
				stack.peek().leftChild.height = 0;
			}
			else { // right leaf node
				stack.peek().rightChild.textRange.start = article.size() - 1;
				stack.peek().rightChild.textRange.end = article.size() - 1;
				stack.peek().rightChild.height = 0;
				
				for(int i = 0; i < leafm.group(6).length(); i++) {
					DSTreeNode popnode = stack.pop();
					popnode.textRange.start = popnode.leftChild.textRange.start;
					popnode.textRange.end = popnode.rightChild.textRange.end;
					popnode.height = popnode.leftChild.height > popnode.rightChild.height ? popnode.leftChild.height + 1
							: popnode.rightChild.height + 1;
				}							
			}
		}
		
	}
	
	private double computeScore(ArrayList<Range> pathset1, ArrayList<Range> pathset2) {
		Collections.sort(pathset1);
		Collections.sort(pathset2);
		
		int i = 0, j = 0;
		int count = 0;
		while(i < pathset1.size() && j < pathset2.size()) {
			int cmp = pathset1.get(i).compareTo(pathset2.get(j));
			if (cmp < 0)
				i++;
			else if(cmp > 0)
				j++;
			else {
				i++;
				j++;
				count++;
			}			
		}
		
		return count / (double)(pathset1.size() + pathset2.size() - count);
	}
	
	private boolean matchNode(DSTreeNode node, DSTemplateTreeNode node2) {
		if(node == null && node2 != null)
			return false;
		
		if(node2 == null)
			return true;
		
		boolean result = node2.relation == Relation.Any || node2.relation == node.relation;
		result = result && (node2.nuclearity == Nuclearity.Any || node2.nuclearity == node.nuclearity);	
		if(!result)
			return false;
		
		boolean leftresult = matchNode(node.leftChild, node2.leftChild);
		if(!leftresult)
			return false;
		
		boolean rightresult = matchNode(node.rightChild, node2.rightChild);
		return rightresult;
	}
	
	private void clearMatched(DSTreeNode node) {
		if(node.leftChild != null)
			clearMatched(node.leftChild);
		if(node.rightChild != null)
			clearMatched(node.rightChild);
		node.matched = false;
	}
	
	private void generateUntilLists() {
		if (rootNode == null)
			return;

		nodeLevelList = (ArrayList<DSTreeNode>[]) new ArrayList[rootNode.height + 1];
		for (int i = 0; i <= rootNode.height; i++)
			nodeLevelList[i] = new ArrayList<DSTreeNode>();
		pathList = new Range[article.size() * 2 - 1];
		count = 0;
		generateUntilListsIter(rootNode);
		
		int leafnum = nodeLevelList[0].size();
		int index = 0;
		double ystart = 0.5 / leafnum;
		double xstart = 0.5 / nodeLevelList.length;
		for (DSTreeNode node : nodeLevelList[0]) {
			node.realypos = node.ypos = ystart + 1.0 / leafnum * index;
			node.realxpos = 1.0 - xstart;
			index++;
		}

		relationDist[0] = nodeLevelList[0].size();
		for(int i = 1; i < relationDist.length; i++)
			relationDist[i] = 0;
		
		for (int i = 1; i < nodeLevelList.length; i++) {
			double xpos = 1.0 - i * 1.0 / nodeLevelList.length - xstart;
			for (DSTreeNode node : nodeLevelList[i]) {
				//node.ypos = node.leftChild.ypos;
				node.realypos = node.ypos = (node.leftChild.ypos + node.rightChild.ypos) / 2;
				node.realxpos = xpos;
				relationDist[node.relation.ordinal()]++;
			}
		}
	}
	
	private void generateUntilListsIter(DSTreeNode node) {
		if (node.leftChild != null)
			generateUntilListsIter(node.leftChild);
		if (node.rightChild != null)
			generateUntilListsIter(node.rightChild);
		
		if (node.leftChild != null)
			node.dfsID = new Range(node.leftChild.dfsID.start, count);
		else
			node.dfsID = new Range(count, count);
		pathList[count++] = node.textRange;
		
		nodeLevelList[node.height].add(node);
	}
	
	private Relation getRelation(String rel) {
		rel = rel.toLowerCase();//.replaceAll("\\s|-", "");
		for (int i = 0; i < relationString.length; i++)
			if (rel.startsWith(relationString[i]))
				return Relation.values()[relationMap[i]];

		return Relation.None;
	}

	private void computeHeightAndTextspan(DSTreeNode node) {
		if(node.leftChild == null && node.rightChild == null)
			return;
		computeHeightAndTextspan(node.leftChild);
		computeHeightAndTextspan(node.rightChild);
		
		node.textRange.start = node.leftChild.textRange.start;
		node.textRange.end = node.rightChild.textRange.end;
		node.height = node.leftChild.height > node.rightChild.height ? node.leftChild.height + 1
				: node.rightChild.height + 1;
	}
	
	// inner classes//////////////////////////////////////////////////////////////////////////////
	public class DSTreeNode implements Serializable{
		
		private static final long serialVersionUID = -823973162520472683L;
		
		public Relation relation = Relation.None;
		public Nuclearity nuclearity = Nuclearity.Any;
		public Range textRange = new Range();
		
		public int height = -1;
		public Range dfsID;
		public double score = 1;

		public double ypos = 0;
		public double realypos = 0;
		public double realxpos = 0;
		public double rectStart = 0;
		public double rectLen = 0;
		public boolean collapsed = false;
		public boolean matched = false;
		public boolean highlighted = false;

		public DSTreeNode leftChild;
		public DSTreeNode rightChild;
		public DSTreeNode parent;
	}
	
	public static class Range implements Comparable<Range>, Serializable{
		
		private static final long serialVersionUID = 7641922312250006687L;
		public int start = -1;
		public int end = -1;

		public Range() { }
		
		public Range(int start, int end) {
			this.start = start;
			this.end = end;
		}
		
		public int compareTo(Range range) {
			if(this.start < range.start)
				return -1;
			else if(this.start > range.start)
				return 1;
			else {
				if(this.end < range.end)
					return -1;
				else if(this.end > range.end)
					return 1;
			}
			
			return 0;
		}
	}
	
	public static class NodeNote implements Serializable {
		private static final long serialVersionUID = 7077640552692022316L;
		
		public HashSet<DSTreeNode> nodeset = new HashSet<DSTreeNode>();
		public String title = "";
		public String note = "";
	}
	
	public static class TreeNote implements Serializable {
		private static final long serialVersionUID = 3285611684402617054L;
		
		public HashSet<DSTree> treeset = new HashSet<DSTree>();
		public String title = "";
		public String note = "";
	}
}
