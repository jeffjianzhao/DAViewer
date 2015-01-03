package daviewer;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;

import javax.swing.*;

import daviewer.DSTemplateTree.DSTemplateTreeNode;
import daviewer.DSTree.DSTreeNode;
import daviewer.DSTree.Nuclearity;
import daviewer.DSTree.Relation;


public class DSTreeSearchPanel extends JPanel {

	public enum SearchMethod {Text, Structure, Both};
	
	private SearchMethod method = SearchMethod.Structure;
	private TreeTemplate templatePanel = new TreeTemplate();
	private JComboBox relationBox = new JComboBox(relation);
	private JComboBox nuclearityBox = new JComboBox(nuclearity);
	private JTextField searchText = new JTextField(15);
	private JLabel hitLabel = new JLabel();
	private TreeTemplateActionListener listener = new TreeTemplateActionListener();
	
	private static String[] nuclearity = new String[] {"Any", "Nucleus", "Satellite"};
	private static String[] relation = new String[]{
		"None", "Attribution", "Background", "Cause", "Comparison", "Condition", "Contrast", 
		"Elaboration", "Enablement", "Evaluation", "Explanation", "Joint", "Manner_means", 
		"Summary", "Temporal", "Topic_change", "Topic_comment", "Same_unit", "Textual_organization",
		"Any"
	};
	
	public static DAViewer appFrame;
	
	public DSTreeSearchPanel(){
		this.setPreferredSize(new Dimension(280, 350));
		this.setLayout(new BorderLayout());		
		
		JPanel textPane = new JPanel();
		textPane.setLayout(new BorderLayout());
		textPane.add(searchText);
		textPane.setBorder(BorderFactory.createTitledBorder("Search Text"));
		
		JPanel treePane = new JPanel();
		treePane.setLayout(new BorderLayout());
		treePane.add(createButtonPane(), BorderLayout.NORTH);
		treePane.add(templatePanel, BorderLayout.CENTER);
		treePane.setBorder(BorderFactory.createTitledBorder("Search Structure"));
			
		this.add(treePane, BorderLayout.CENTER);
		this.add(textPane, BorderLayout.NORTH);
		this.add(createRadioButtonPane(), BorderLayout.SOUTH);
	}
	
	public void addTemplateTree(DSTreeNode root, int level){
		DSTemplateTree tree = new DSTemplateTree();
		addTemplateTreeIter(root, tree.rootNode, level);
		templatePanel.setTree(tree);
	}
	
	private void addTemplateTreeIter(DSTreeNode node, DSTemplateTreeNode tnode, int level){	
		tnode.relation = node.relation;
		tnode.nuclearity = node.nuclearity;
		if(level <= 0){
			return;
		}
		if(node.leftChild == null && node.rightChild == null)
			return;
		
		tnode.leftChild = new DSTemplateTreeNode();
		tnode.rightChild = new DSTemplateTreeNode();
		
		addTemplateTreeIter(node.leftChild, tnode.leftChild, level - 1);
		addTemplateTreeIter(node.rightChild, tnode.rightChild, level - 1);
	}
	
	private JPanel createRadioButtonPane() {
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new GridLayout(2, 3, 2, 2));
		
		JRadioButton b1 = new JRadioButton("Text");
		b1.addActionListener(listener);
		buttonPane.add(b1);
		JRadioButton b2 = new JRadioButton("Structure");
		b2.addActionListener(listener);
		buttonPane.add(b2);
		b2.setSelected(true);
		JRadioButton b3 = new JRadioButton("Both");
		b3.addActionListener(listener);
		buttonPane.add(b3);
		
		ButtonGroup group = new ButtonGroup();
	    group.add(b1);
	    group.add(b2);
	    group.add(b3);
		
	    buttonPane.add(hitLabel);
	    buttonPane.add(new JPanel());
	    
		JButton schbutton = new JButton("Search");
		schbutton.addActionListener(listener);
		buttonPane.add(schbutton);
	    
		return buttonPane;
	}
	
	private JPanel createButtonPane(){
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new GridLayout(2, 2, 2, 2));
		
		JButton addbutton = new JButton("Add");
		addbutton.addActionListener(listener);
		buttonPane.add(addbutton);
		
		JButton delbutton = new JButton("Delete");
		delbutton.addActionListener(listener);
		buttonPane.add(delbutton);
		
		relationBox.addActionListener(listener);
		buttonPane.add(relationBox);
		
		nuclearityBox.addActionListener(listener);
		buttonPane.add(nuclearityBox);
		
		return buttonPane;
	}
	
	// inner classes//////////////////////////////////////////////////////////////////////////////
	public class TreeTemplateActionListener implements ActionListener{

		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if(source == relationBox){
				if(templatePanel.selectedNode != null) 
					templatePanel.selectedNode.relation = Relation.values()[relationBox.getSelectedIndex()];
				
				repaint();
				appFrame.autoLogger.logAction("searching operation,select");
			}
			else if(source == nuclearityBox){
				if(templatePanel.selectedNode != null) 
					templatePanel.selectedNode.nuclearity = Nuclearity.values()[nuclearityBox.getSelectedIndex()];
				
				repaint();
				appFrame.autoLogger.logAction("searching operation,select");
			}
			else {
				String event = e.getActionCommand();
				if(event.equals("Add")){
					templatePanel.theTree.addChildren(templatePanel.selectedNode);
					repaint();					
					appFrame.autoLogger.logAction("searching operation,edit");
				}
				else if(event.equals("Delete")){
					templatePanel.theTree.removeChildren(templatePanel.selectedNode);
					repaint();				
					appFrame.autoLogger.logAction("searching operation,edit");
				}
				else if(event.equals("Text")){
					method = SearchMethod.Text;
				}
				else if(event.equals("Structure")){
					method = SearchMethod.Structure;
				}
				else if(event.equals("Both")){
					method = SearchMethod.Both;
				}
				else if(event.equals("Search")){
					int hitnum = appFrame.tableview.searchByTemplate(templatePanel.theTree, searchText.getText(), method);
					hitLabel.setText("No. of hits: " + hitnum);
					appFrame.autoLogger.logAction("searching operation," + method.toString());
				}
			}
			
		}
		
	}
	
	public class TreeTemplate extends JPanel implements MouseListener, MouseMotionListener {
		
		public DSTemplateTree theTree = new DSTemplateTree();
		public DSTemplateTreeNode selectedNode, hoveredNode;
		
		private ArrayList<VisualNode> visualNodes = new ArrayList<VisualNode>();
		
		private static final int margin = 30;
		private static final int circleR = 8;
		
		public TreeTemplate(){
			this.addMouseListener(this);
			this.addMouseMotionListener(this);
		}
		
		public void setTree(DSTemplateTree tree){
			theTree = tree;
			tree.updateTreeTopology();
			repaint();
		}
		
		public void paint(Graphics g){
			super.paint(g);
			
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			visualNodes.clear();
			Dimension d = this.getSize();
			paintNodesByDFS(g2, d, theTree.rootNode);
			
			drawSelectionNode(g2, d, selectedNode);
			drawSelectionNode(g2, d, hoveredNode);
		}
		
		private void paintNodesByDFS(Graphics2D g2, Dimension d, DSTemplateTreeNode node) {
			double xpos = (d.width - margin * 2) * node.xpos + margin;
			double ypos = (d.height - margin * 2) * node.ypos + margin;
			
			if (node.leftChild != null) {
				drawLink(g2, d, node, node.leftChild, xpos, ypos);
				paintNodesByDFS(g2, d, node.leftChild);
			}
			if (node.rightChild != null) {
				drawLink(g2, d, node, node.rightChild, xpos, ypos);
				paintNodesByDFS(g2, d, node.rightChild);
			}		

			drawNode(g2, node, xpos, ypos);
		}
		
		private void drawNode(Graphics2D g2, DSTemplateTreeNode node, double xpos, double ypos) {
			Ellipse2D circle = new Ellipse2D.Double(xpos - circleR / 2, ypos - circleR / 2, circleR, circleR);
			if(node.relation == Relation.Any)
				g2.setPaint(Color.WHITE);
			else
				g2.setPaint(DSTreePanel.relationColors[node.relation.ordinal()]);
			g2.fill(circle);

			VisualNode vnode = new VisualNode(circle, node);
			visualNodes.add(vnode);
					
			g2.setPaint(Color.BLACK);
			g2.draw(circle);
		}

		private void drawLink(Graphics2D g2, Dimension d, DSTemplateTreeNode fromNode, 
				DSTemplateTreeNode toNode, double fromx, double fromy) {
			double chypos = (d.height - margin * 2) * toNode.ypos + margin;
			double chxpos = (d.width - margin * 2) * toNode.xpos + margin;

			Line2D line1 = new Line2D.Double(fromx, fromy, fromx, chypos);
			Line2D line2 = new Line2D.Double(fromx, chypos, chxpos, chypos);

			if (toNode.nuclearity == Nuclearity.Nucleus)
				g2.setPaint(Color.BLACK);
			else if ((toNode.nuclearity == Nuclearity.Satellite))			
				g2.setPaint(Color.LIGHT_GRAY);
			else
				g2.setPaint(Color.WHITE);
					
			g2.draw(line1);
			g2.draw(line2);
		}
		
		private void drawSelectionNode(Graphics2D g2, Dimension d, DSTemplateTreeNode node) {
			if(node == null)
				return;
			double xpos = (d.width - margin * 2) * node.xpos + margin;
			double ypos = (d.height - margin * 2) * node.ypos + margin;
			
			double rad = circleR + 6;
			Ellipse2D circle = new Ellipse2D.Double(xpos - rad / 2, ypos - rad / 2, rad, rad);
			g2.setPaint(Color.RED);
			g2.draw(circle);
		}
		
		public void mouseClicked(MouseEvent e) {
			selectedNode = getNodeAtPoint(e.getPoint());
			if(selectedNode != null){
				relationBox.setSelectedIndex(selectedNode.relation.ordinal());
				nuclearityBox.setSelectedIndex(selectedNode.nuclearity.ordinal());
			}
			
			repaint();
		}

		public void mouseEntered(MouseEvent e) {}

		public void mouseExited(MouseEvent e) {}

		public void mousePressed(MouseEvent e) {}

		public void mouseReleased(MouseEvent e) {}
				
		public void mouseDragged(MouseEvent e) {}

		public void mouseMoved(MouseEvent e) {
			this.setToolTipText(null);
			hoveredNode = getNodeAtPoint(e.getPoint());
			if (hoveredNode != null){
				this.setToolTipText(hoveredNode.relation + "," + hoveredNode.nuclearity);
			}
			
			repaint();
		}
		
		private DSTemplateTreeNode getNodeAtPoint(Point p) {
			for (VisualNode vnode : visualNodes) {
				if (vnode.nodeShape.contains(p)) {
					return vnode.node;
				}
			}

			return null;
		}
		
		public class VisualNode {
			public Ellipse2D nodeShape;
			public DSTemplateTreeNode node;

			public VisualNode() {
			}

			public VisualNode(Ellipse2D shape, DSTemplateTreeNode node) {
				this.nodeShape = shape;
				this.node = node;
			}
		}


	}
	
}
