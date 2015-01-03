package daviewer;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;

import daviewer.DSTree.Relation;


public class DSTreeInfoPanel extends JPanel {

	private JLabel treeInfo = new JLabel(treeStart + "</html>");
	private JLabel nodeInfo = new JLabel(nodeStart + "</html>");
	private ScoreLengend scrPanel = new ScoreLengend();
	private RelationLegend relPanel = new RelationLegend();
	private JSlider scrSlider = new JSlider(JSlider.VERTICAL, 0, 100, 0);
	private DistributionLengend distrLegend = new DistributionLengend();
	
	private static String nodeStart = "<html><font color=red>Node Info:</font>";
	private static String treeStart = "<html><font color=red>Tree Info:</font>";
	private static String bluefont1 = "<font color=blue>";
	private static DecimalFormat twoPlaces = new DecimalFormat("0.00");
	
	public static DAViewer appFrame;
	
	public DSTreeInfoPanel() {			
		treeInfo.setFont(new Font("Arial", Font.BOLD, 10));
		nodeInfo.setFont(new Font("Arial", Font.BOLD, 10));
		
		this.setLayout(new BorderLayout());	
		
		JPanel statusPane = new JPanel();
		statusPane.setLayout(new GridLayout(4, 1, 0, 0));
		statusPane.add(distrLegend);
		statusPane.add(relPanel);
		statusPane.add(treeInfo);
		statusPane.add(nodeInfo);
		
		JPanel scorePane = new JPanel();
		scorePane.setLayout(new BorderLayout());
		scrPanel.setPreferredSize(new Dimension(80, 80));
		scrSlider.addChangeListener(new ScoreChangeListener());
		scorePane.add(scrSlider, BorderLayout.WEST);
		scorePane.add(scrPanel, BorderLayout.CENTER);
		
		this.add(statusPane, BorderLayout.CENTER);
		this.add(scorePane, BorderLayout.WEST);
	}
	
	public void setNodeInfo(ArrayList<DSTree.DSTreeNode> nodes, DSTree tree) {		
		distrLegend.setTreeRelDistr(nodes, tree);
		
		if(nodes == null || nodes.size() == 0) {
			nodeInfo.setText(nodeStart + "</html>");
			return;
		}
		
		if(nodes.size() == 1) {

			DSTree.DSTreeNode node = nodes.get(0);
			
			StringBuilder sb = new StringBuilder(nodeStart);
			sb.append(bluefont1 + "  Relation </font>" + node.relation);
			sb.append(bluefont1 + ",  Nuclearity </font>" + node.nuclearity);
			sb.append(bluefont1 + ",  Level </font>" + node.height);
			sb.append(bluefont1 + ",  Text Span </font>[" + node.textRange.start + "-" + node.textRange.end +"]");
			sb.append(bluefont1 + ",  Similarity Score </font>" + twoPlaces.format(node.score));
			
			nodeInfo.setText(sb.toString());
		}
		else {
			StringBuilder sb = new StringBuilder(nodeStart);
			sb.append(nodes.size() + " nodes selected");
			nodeInfo.setText(sb.toString());
		}
	}
	
	public void setTreeInfo(String rowh, String colh, DSTree tree) {
		distrLegend.setTreeRelDistr(tree);
		
		if(tree == null) {
			treeInfo.setText(treeStart + "</html>");
			return;
		}	
		
		StringBuilder sb = new StringBuilder(treeStart);
		sb.append(bluefont1 + "  Articles </font>" + rowh);
		sb.append(bluefont1 + ",  Algorithms </font>" + colh);
		sb.append(bluefont1 + ",  Height </font>" + tree.getTreeLevels());
		sb.append(bluefont1 + ",  Similarity Score </font>" + twoPlaces.format(tree.rootNode.score));
		sb.append(bluefont1 + ",  Node Number </font>");
		int num = tree.article.size();
		sb.append(num + ", ");
		for(int i = 1; i < tree.nodeLevelList.length; i++) {
			num -= tree.nodeLevelList[i].size();
			sb.append(num + ", ");
		}
		/*
		sb.append(bluefont1 + "  Relations </font>");
		for(int i = 0; i < tree.relationDist.length; i++)
			if(tree.relationDist[i] > 0){
				sb.append(DSTree.Relation.values()[i].toString() + " " + tree.relationDist[i] + ",");
			}
		*/		
		
		treeInfo.setText(sb.toString());
	}
	
	public void setTreeInfo(String[] rowh, String[] colh) {
		StringBuilder sb = new StringBuilder(treeStart);
		
		sb.append(bluefont1 + "  Articles </font>");
		for(int i = 0; i < rowh.length; i++)
			sb.append(rowh[i] + ", ");
		sb.append(bluefont1 + "  Algorithms </font>");
		for(int i = 0; i < colh.length; i++)
			sb.append(colh[i] + ", ");		
		sb.append("  " + rowh.length * colh.length + " trees selected");
		
		treeInfo.setText(sb.toString());
		distrLegend.setTreeRelDistr(null);
	}
	
	public void setHoveredLegend(DSTree.DSTreeNode node){
		relPanel.setHovered(node);
		scrPanel.setHovered(node);
	}
	
	// inner classes//////////////////////////////////////////////////////////////////////////////
	public class ScoreChangeListener implements ChangeListener{

		public void stateChanged(ChangeEvent e) {		
	        if (!scrSlider.getValueIsAdjusting()) {
	            double scr = 1 - (double)scrSlider.getValue() / scrSlider.getMaximum();
	            scrPanel.setThreshold(scr);
	            scrSlider.setToolTipText(twoPlaces.format(scr));
				appFrame.autoLogger.logAction("legend filter operation,score");
	        }    
		}
		
	}
	
	public class ScoreLengend extends JPanel implements MouseMotionListener{
		
		private final float maxr = 60, minr = 10;
		private Ellipse2D[] circles = new Ellipse2D[DSTreePanel.scoreColors.length];
		private double[] scores = new double[DSTreePanel.scoreColors.length];
		private double threshold = 1.0;
		private int hovered = -1;
		
		public ScoreLengend(){
			this.addMouseMotionListener(this);
			for(int i = 0; i < scores.length; i++)
				scores[i] = 1 - 1.0 / (scores.length - 1) * i;
		}
		
		public void setThreshold(double score){
			threshold = score;
			appFrame.tableview.setScoreThresh(threshold);
			repaint();
		}
		
		public void paint(Graphics g){
			super.paint(g);
			Dimension d = this.getSize();
			Graphics2D g2 = (Graphics2D) g;
			
			float deltar = (maxr - minr) / (DSTreePanel.scoreColors.length - 1);
			for (int i = DSTreePanel.scoreColors.length - 1; i >= 0; i--) {
				float rad = minr + deltar * i;
				Ellipse2D circle = new Ellipse2D.Float(d.width / 2 - rad / 2, d.height / 2 + maxr / 2 - rad, rad, rad);
				circles[i] = circle;
				
				if(scores[i] <= threshold)
					g2.setPaint(DSTreePanel.scoreColors[DSTreePanel.scoreColors.length - i - 1]);
				else
					g2.setPaint(Color.LIGHT_GRAY);
				g2.fill(circle);
				if(i != hovered)
					g2.setPaint(Color.BLACK);
				else
					g2.setPaint(Color.RED);
				g2.draw(circle);
			}
			
			g2.setPaint(Color.RED);
			g2.drawString("0.0", d.width / 2 - maxr / 2 - 10, d.height / 2 - maxr / 2 + 8);
			g2.drawString("1.0", d.width / 2 - maxr / 2 - 10, d.height / 2 + maxr / 2);
		}
		
		public void setHovered(DSTree.DSTreeNode node){
			if(node != null)
				hovered = scores.length - 1 - (int)(node.score / scores[scores.length - 2]);
			else
				hovered = -1;
			repaint();
		}

		public void mouseDragged(MouseEvent e) {}

		public void mouseMoved(MouseEvent e) {
			Point p = e.getPoint();
			for(int i = 0; i < circles.length; i++){
				if(circles[i].contains(p)){
					this.setToolTipText(twoPlaces.format(scores[i]));
					return;
				}
					
			}
			
			this.setToolTipText(null);
		}
	}
	
	public class RelationLegend extends JPanel implements MouseListener{
		
		private JLabel[] labels = new JLabel[DSTreePanel.relationColors.length];
		private int previousHovered = 0;
		private Border border1 = BorderFactory.createLineBorder(Color.BLACK);
		private Border border2 = BorderFactory.createLineBorder(Color.WHITE);
		
		public RelationLegend(){
			this.setLayout(new GridLayout(1, 19, 1, 1));
			this.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
			
			Font f = new Font("Arial", Font.BOLD, 10);
			Relation[] relations = DSTree.Relation.values();
			for(int i = 0; i < relations.length - 1; i++){
				JLabel label = new JLabel(relations[i].toString());
				label.setFont(f);
				label.setOpaque(true);
				label.setBackground(DSTreePanel.relationColors[i]);
				label.setBorder(border1);
				label.addMouseListener(this);
				if(i != 16 && i != 11 && i != 7)
					label.setForeground(Color.WHITE);
				label.setToolTipText(relations[i].toString());
				labels[i] = label;
				this.add(label);
			}
		}
		
		public void setHovered(DSTree.DSTreeNode node){
			if(node != null) {
				int index = node.relation.ordinal();
				labels[previousHovered].setBorder(border1);
				labels[index].setBorder(border2);
				previousHovered = index;
			}
			else {
				labels[previousHovered].setBorder(border1);
			}
		}
		
		public void mouseClicked(MouseEvent arg0) {
			for(int i = 0; i < labels.length; i++)
				if(labels[i] == arg0.getSource()){
					if(labels[i].getBackground() == Color.LIGHT_GRAY) {
						labels[i].setBackground(DSTreePanel.relationColors[i]);
						appFrame.tableview.updateRelationFilter(DSTree.Relation.values()[i], false);
					}
					else {
						labels[i].setBackground(Color.LIGHT_GRAY);
						appFrame.tableview.updateRelationFilter(DSTree.Relation.values()[i], true);
					}
					
					break;
				}
			appFrame.autoLogger.logAction("legend filter operation,relation");
		}

		public void mouseEntered(MouseEvent arg0) {}

		public void mouseExited(MouseEvent arg0) {}

		public void mousePressed(MouseEvent arg0) {}

		public void mouseReleased(MouseEvent arg0) {}
	}
	
	public class DistributionLengend extends JPanel{
		
		private JLabel[] labels = new JLabel[DSTreePanel.relationColors.length];
		private int[] nodeDistr = new int[DSTreePanel.relationColors.length];
		
		public DistributionLengend(){
			this.setLayout(new GridLayout(1, 19, 1, 1));
			Font f = new Font("Arial", Font.BOLD, 10);
			for(int i = 0; i < labels.length; i++){
				JLabel label = new JLabel();
				label.setHorizontalAlignment(SwingConstants.CENTER);
				label.setFont(f);
				//label.setForeground(DSTreePanel.relationColors[i]);
				labels[i] = label;
				this.add(label);
			}
		}
		
		public void setTreeRelDistr(DSTree tree){
			if(tree  == null)
				for(JLabel label : labels)
					label.setText("");
			else {
				for(int i = 0; i < labels.length; i++)
					labels[i].setText(Integer.toString(tree.relationDist[i]));
			}
		}
		
		public void setTreeRelDistr(ArrayList<DSTree.DSTreeNode> nodes, DSTree tree){
			if(tree  == null)
				for(JLabel label : labels)
					label.setText("");
			else {
				labelNodeHighlight(tree.rootNode, nodes, false);
				for(int i = 0; i < nodeDistr.length; i++)
					nodeDistr[i] = 0;
				computeNodeDistr(tree.rootNode);
				for(int i = 0; i < labels.length; i++)
					labels[i].setText(nodeDistr[i] + "/" + tree.relationDist[i]);
			}
		}
		
		private void computeNodeDistr(DSTree.DSTreeNode node){
			if(node.highlighted)
				nodeDistr[node.relation.ordinal()]++;
			
			if(node.leftChild == null && node.rightChild == null)
				return;
			computeNodeDistr(node.leftChild);
			computeNodeDistr(node.rightChild);
		}
		
		private void labelNodeHighlight(DSTree.DSTreeNode node, ArrayList<DSTree.DSTreeNode> nodes, boolean isIn){
			if(!isIn && nodes.contains(node))
				isIn = true;
			if(isIn)
				node.highlighted = true;
			else
				node.highlighted = false;
			
			if (node.leftChild != null)
				labelNodeHighlight(node.leftChild, nodes, isIn);
			if (node.rightChild != null)
				labelNodeHighlight(node.rightChild, nodes, isIn);
			
		}
	}
}
