package daviewer;

import java.util.ArrayList;

import daviewer.DSTree.Nuclearity;
import daviewer.DSTree.Relation;


public class DSTemplateTree {
	
	public DSTemplateTreeNode rootNode = new DSTemplateTreeNode();;
	public ArrayList<DSTemplateTreeNode>[] nodeLevelList;
	
	public DSTemplateTree(){
		updateTreeTopology();
	}
	
	public void addChildren(DSTemplateTreeNode node){
		node.leftChild = new DSTemplateTreeNode();
		node.rightChild = new DSTemplateTreeNode();
		
		updateTreeTopology();
	}
	
	public void removeChildren(DSTemplateTreeNode node){
		node.leftChild = null;
		node.rightChild = null;
		
		updateTreeTopology();
	}
	
	public void updateTreeTopology(){
		updateNodeHeights(rootNode);
		
		nodeLevelList = (ArrayList<DSTemplateTreeNode>[]) new ArrayList[rootNode.height + 1];
		for (int i = 0; i <= rootNode.height; i++)
			nodeLevelList[i] = new ArrayList<DSTemplateTreeNode>();
		updateNodeLevelList(rootNode);
		
		int leafnum = nodeLevelList[0].size();
		if(leafnum == 1){
			rootNode.ypos = 0.5;
			rootNode.xpos = 0.5;
			return;
		}
			
		int index = 0;
		for (DSTemplateTreeNode node : nodeLevelList[0]) {
			node.ypos = node.ypos = 1.0 / (leafnum - 1) * index;
			node.xpos = 1.0;
			index++;
		}
		
		for (int i = 1; i < nodeLevelList.length; i++) {
			double xpos = 1.0 - i * 1.0 / (nodeLevelList.length - 1);
			for (DSTemplateTreeNode node : nodeLevelList[i]) {
				node.ypos = node.ypos = (node.leftChild.ypos + node.rightChild.ypos) / 2;
				node.xpos = xpos;
			}
		}
	}
	
	private void updateNodeHeights(DSTemplateTreeNode node){
		if(node.leftChild == null && node.rightChild == null) {
			node.height = 0;
			return;
		}
		
		updateNodeHeights(node.leftChild);
		updateNodeHeights(node.rightChild);
		
		node.height = node.leftChild.height > node.rightChild.height ? node.leftChild.height + 1
				: node.rightChild.height + 1;
	}
	
	private void updateNodeLevelList(DSTemplateTreeNode node){
		if (node.leftChild != null)
			updateNodeLevelList(node.leftChild);
		if (node.rightChild != null)
			updateNodeLevelList(node.rightChild);
		
		nodeLevelList[node.height].add(node);
	}
	
	// inner classes//////////////////////////////////////////////////////////////////////////////
	public static class DSTemplateTreeNode {
		public Relation relation = Relation.None;
		public Nuclearity nuclearity = Nuclearity.Any;
		
		public int height = -1;
		public double ypos = 0;
		public double xpos = 0;

		public DSTemplateTreeNode leftChild;
		public DSTemplateTreeNode rightChild;
		public DSTemplateTreeNode parent;
	}
}
