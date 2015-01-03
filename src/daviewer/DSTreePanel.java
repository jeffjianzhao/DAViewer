package daviewer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import daviewer.DSTree.DSTreeNode;



public class DSTreePanel extends JPanel implements MouseListener, MouseMotionListener {

	private DSTree dstree;
	private ArrayList<VisualNode> visualNodes = new ArrayList<VisualNode>();
	private DSTreeNode hoveredNode;
	private ArrayList<DSTreeNode> selectedNodes = new ArrayList<DSTreeNode>();
	private int selectedLevel = -1, hoveredLevel = -1;
	private Point mousePos;
	private double rightBound = 1.0;
	private int levelBound = 0;
	private double levelWidth = 0;
	private ArrayList<DSTreeNode> visualLeafNodes = new ArrayList<DSTreeNode>();
	
	private JPopupMenu popupMenu = initPopupMenu();
	private static final int margin = 6;
	private static final int nodeR = 6;
	private static final Color levelColor = new Color(218, 180, 80);
	private static Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
	private static DecimalFormat twoPlaces = new DecimalFormat("0.00");
	private static int selectionMask = 0x87000000;
	private static int filterMask = 0xCD000000;
	private static BasicStroke thinStroke = new BasicStroke(1);
	private static BasicStroke thickStroke = new BasicStroke(2);
	private static Color matchcolor = new Color(Color.BLUE.getRGB() ^ 0x9B000000, true);
	private static Color filterColor = new Color(240, 240, 240);
    
	public static Color[] relationColors = initRelationColors();
	public static Color[] defaultColors;
	public static Color[] scoreColors = initScoreColors();
	public static final int smallSize = 30; 
	public static double scoreThresh = 1.0;
	public static ArrayList<DSTree.Relation> relationFilter = new ArrayList<DSTree.Relation>();
	public static DAViewer appFrame;
	public enum DisplayType { NodeLink, Compact };
	public static DisplayType display = DisplayType.NodeLink;
	
	// constructors//////////////////////////////////////////////////////////////////////////////
	public DSTreePanel() {
		this.setPreferredSize(new Dimension(200, 300));
		this.setBackground(Color.WHITE);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
	}

	// public methods//////////////////////////////////////////////////////////////////////////////
	public void setDSTree(DSTree tree) {
		this.dstree = tree;
		updateTreeTopology();
	}
	
	public DSTree getDSTree() {
		return dstree;
	}
	
	public ArrayList<DSTreeNode> getSelectedNodes() {
		return selectedNodes;
	}

	public void setSelectedNodes(ArrayList<DSTreeNode> selectedNodes) {
		this.selectedNodes = selectedNodes;
		repaint();
	}

	public void loadTree(File f) throws IOException {
		DSTree tree = new DSTree();
		tree.loadFromFile(f);
		setDSTree(tree);
	}

	public void clearSelection() {
		selectedLevel = -1; 
		hoveredLevel = -1;
		hoveredNode = null;
		selectedNodes.clear();
	}
	
	public void paint(Graphics g) {
		super.paint(g);
		if(dstree == null)
			return;
		if(dstree.nodeLevelList == null)
			return;
		
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		visualNodes.clear();
		Dimension d = this.getSize();
		
		levelWidth = (this.getSize().width - margin * 2) / (double) (dstree.getTreeLevels() - levelBound);
		
		if(d.height > smallSize && d.width > smallSize) {
			paintBackground(g2, d);
			paintNodesByDFS(g2, d, dstree.rootNode, selectedNodes.contains(dstree.rootNode));
			paintSelection(g2, d);
		}
		else if(d.height <= smallSize && d.width > smallSize) {
			paintHorizontal(g2, d);
		}
		else if(d.height > smallSize && d.width <= smallSize) {
			paintVertical(g2, d);
		}
		else {
			g2.setPaint(DSTreePanel.getScoreColor((float)dstree.rootNode.score));
			g2.fillRect(0, 0, d.width, d.height);
		}		
	}

	public void setDSTreeArticle() {
		appFrame.textPane.setDSTree(dstree);
		appFrame.textPane.organizeText(selectedLevel);
	}

	// events//////////////////////////////////////////////////////////////////////////////
	public void mouseDragged(MouseEvent arg0) {}

	public void mouseMoved(MouseEvent arg0) {
		if(this.getCursor() != handCursor)
			this.setCursor(handCursor);
		mousePos = arg0.getPoint();
		
		this.setToolTipText(null);
		hoveredNode = getNodeAtPoint(mousePos);
		appFrame.infoPanel.setHoveredLegend(hoveredNode);
		if (hoveredNode != null){
			StringBuilder sb = new StringBuilder("<html>");
			sb.append(hoveredNode.relation.toString());
			sb.append(", " + hoveredNode.nuclearity.toString());
			sb.append(", " + twoPlaces.format(hoveredNode.score));
			sb.append(", [");
			sb.append(hoveredNode.textRange.start + "-" + hoveredNode.textRange.end + "] <br/>\"");
			sb.append(dstree.article.get(hoveredNode.textRange.start) + "...\"");
			this.setToolTipText(sb.toString());		
		}
		else if(this.getSize().height > smallSize && this.getSize().width > smallSize && dstree != null){
			hoveredLevel = (int) ((mousePos.x - margin + levelWidth / 2) / levelWidth);
			if(hoveredLevel >= dstree.getTreeLevels())
				hoveredLevel = dstree.getTreeLevels() - 1;
			this.setToolTipText("Level " + hoveredLevel);
		}
		
		repaint();
	}

	public void mouseClicked(MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON1) {
			int clickcount = e.getClickCount();
	
			if (clickcount == 1) {
				DSTreeNode hitnode = getNodeAtPoint(e.getPoint());
				if(hitnode != null)
					if((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK)
						selectedNodes.add(hitnode);
					else {
						selectedNodes.clear();
						selectedNodes.add(hitnode);
					}
				
				// highlight level
				if(hitnode == null) {
					//double binW = (this.getSize().width - margin * 2) / (double) (dstree.getTreeLevels() - 1 - levelBound);
					mousePos = e.getPoint();
					selectedLevel = (int) ((mousePos.x - margin + levelWidth / 2) / levelWidth);
					appFrame.textPane.organizeText(dstree.getTreeLevels() - 1 - selectedLevel);
				}
				
				// highlight the text
				if (selectedNodes.size() != 0) {
					appFrame.textPane.highlightNodeText(selectedNodes.get(0));
					appFrame.infoPanel.setNodeInfo(selectedNodes, dstree);
					appFrame.notePanel.highlightNodeNotePanel(selectedNodes);
				}
	
				appFrame.tableview.updatePeerTreeSelection();
				repaint();	
				
				appFrame.autoLogger.logAction("tree exploration operation,select");
			} 
			else if (clickcount == 2) {
				DSTreeNode node = getNodeAtPoint(e.getPoint());
				if (node != null)
					node.collapsed = !node.collapsed;
				updateTreeTopology();
	
				repaint();
				
				appFrame.autoLogger.logAction("tree exploration operation,collapse/expand node");
			}
		}
		else if(e.getButton() == MouseEvent.BUTTON3) {
			popupMenu.show(this, e.getX(), e.getY());
		}
	}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {
		mousePos = null;
		repaint();
	}

	public void mousePressed(MouseEvent e) {}

	public void mouseReleased(MouseEvent e) {}

	// private methods//////////////////////////////////////////////////////////////////////////////
	private void paintHorizontal(Graphics2D g2, Dimension d) {
		final int minh = 5;
		double rectw = (double)d.width / dstree.nodeLevelList.length;
		int leafnum = dstree.nodeLevelList[0].size();
		int nodenum = leafnum * 2;
		for(int i = 0; i < dstree.nodeLevelList.length; i++) { 			
			double score = 0;
			double ypos = 0;
			for(DSTreeNode node : dstree.nodeLevelList[i]) {
				score += node.score;
				ypos += node.realypos;
			}
			
			nodenum -= dstree.nodeLevelList[i].size();
			double recth = (d.height - minh) * nodenum / (double)leafnum + minh;
			Rectangle2D rect = new Rectangle2D.Double(d.width - rectw * (i + 1), 
					d.height * ypos / dstree.nodeLevelList[i].size() - recth / 2, rectw, recth);
			
			g2.setPaint(DSTreePanel.getScoreColor((float)score / dstree.nodeLevelList[i].size()));
			g2.fill(rect);
			g2.setPaint(Color.BLACK);
			g2.draw(rect);
			g2.drawLine((int)rect.getMinX() + 3, (int)rect.getCenterY(), 
					(int)rect.getMaxX() - 3, (int)rect.getCenterY());
		}
	}
	
	private void paintVertical(Graphics2D g2, Dimension d) {
		final int minh = 2;
		int[] bins = new int[dstree.nodeLevelList[0].size()];
		double[] scores = new double[dstree.nodeLevelList[0].size()];
		double binw = 1.0 / (bins.length - 1);
		
		/*
		for(int i = 1; i < dstree.nodeLevelList.length; i++) { 
			for(DSTreeNode node : dstree.nodeLevelList[i]) { 
				bins[(int)(node.realypos / binw)]++;
				scores[(int)(node.realypos / binw)] += node.score;
			}
		}*/
		
		for(int i = 1; i < dstree.nodeLevelList.length; i++) { 
			for(DSTreeNode node : dstree.nodeLevelList[i])
				for(int j = node.textRange.start; j <= node.textRange.end; j++) {
					bins[j]++;
					scores[j] += node.score;
				}
		}
		
		int maxnum = 0;
		for(int i = 0; i < bins.length; i++)
			if(bins[i] > maxnum)
				maxnum = bins[i];
		binw *= d.height;
		maxnum += 1;
		for(int i = 0; i < bins.length; i++) {
			double binh = (d.width - minh) * (bins[i] + 1) / (double)maxnum + minh;
			Rectangle2D rect = new Rectangle2D.Double(0/*d.width - binh*/, i * binw, binh, binw);
			
			g2.setPaint(DSTreePanel.getScoreColor((float)(scores[i] + 1) / (bins[i] + 1)));
			g2.fill(rect);
			g2.setPaint(Color.BLACK);
			g2.draw(rect);
		}
	}
	
	private void paintBackground(Graphics2D g2, Dimension d) {
		if(display == DisplayType.NodeLink) {
			paintBackgroundIter(g2, d, dstree.rootNode);
		}
		
		if (selectedLevel != -1) {
			Rectangle2D bin = new Rectangle2D.Double(selectedLevel * levelWidth + margin, margin / 2, levelWidth, d.height - margin);
			g2.setPaint(levelColor);
			g2.fill(bin);
		}

		if (mousePos == null)
			return;

		if (hoveredLevel != -1 && hoveredLevel < dstree.getTreeLevels() && hoveredLevel != selectedLevel) {
			Rectangle2D bin = new Rectangle2D.Double(hoveredLevel * levelWidth + margin, margin / 2, levelWidth, d.height - margin);
			g2.setPaint(levelColor);
			g2.fill(bin);
		}

	}

	private void paintBackgroundIter(Graphics2D g2, Dimension d, DSTreeNode node){		
		if (!node.collapsed) {
			if (node.leftChild != null) 
				paintBackgroundIter(g2, d, node.leftChild);
			
			if (node.rightChild != null) 
				paintBackgroundIter(g2, d, node.rightChild);			
		}
		
		double width = 0;
		if (node.parent == null)
			width = levelWidth;
		else
			width = levelWidth * (node.parent.height - node.height);
		double xpos = (d.width - margin * 2) * node.realxpos / rightBound + margin;
		double xstart = xpos + levelWidth / 2 - width - 1;
		double ystart = node.rectStart * (d.height - 2 * margin) + margin;
		double height = node.rectLen * (d.height - 2 * margin);
		
		Rectangle2D rect = new Rectangle2D.Double(xstart, ystart, width, height);
		g2.setPaint(getScoreColor((float)node.score));
		g2.fill(rect);
	}
	
	private void paintNodesByDFS(Graphics2D g2, Dimension d, DSTreeNode node, boolean isInSubtree) {
		if(!isInSubtree && selectedNodes.contains(node))
			isInSubtree = true;
		
		double xpos = (d.width - margin * 2) * node.realxpos / rightBound + margin;
		double ypos = (d.height - margin * 2) * node.ypos + margin;	

		if (!node.collapsed) {
			if (node.leftChild != null) {
				drawLink(g2, d, node, node.leftChild, xpos, ypos, isInSubtree);
				paintNodesByDFS(g2, d, node.leftChild, isInSubtree);
			}
			if (node.rightChild != null) {
				drawLink(g2, d, node, node.rightChild, xpos, ypos, isInSubtree);
				paintNodesByDFS(g2, d, node.rightChild, isInSubtree);
			}
		}
		drawNode(g2, node, xpos, ypos, isInSubtree);
	}
	
	private void drawNode(Graphics2D g2, DSTreeNode node, double xpos, double ypos, boolean isInSubtree) {
		if(display == DisplayType.NodeLink) {
			double circleR = nodeR;
			boolean isFiltered = false;
			if((node.score > scoreThresh && !dstree.isReference) || relationFilter.contains(node.relation))
				isFiltered = true;
			
			if(node.matched && !isFiltered) {
				double rad = circleR + 8;
				Ellipse2D circleb = new Ellipse2D.Double(xpos - rad / 2, ypos - rad / 2, rad, rad);
				g2.setPaint(matchcolor);
				g2.fill(circleb);
			}
			
			Ellipse2D circle = new Ellipse2D.Double(xpos - circleR / 2, ypos - circleR / 2, circleR, circleR);
			
			if(isFiltered){
				g2.setPaint(filterColor);
				g2.setStroke(thinStroke);
			}
			else if(isInSubtree){
				g2.setPaint(relationColors[node.relation.ordinal()]);
				g2.setStroke(thickStroke);
			}
			else {
				//g2.setPaint(new Color(c.getRGB() ^ selectionMask, true));
				g2.setPaint(defaultColors[node.relation.ordinal()]);
				g2.setStroke(thinStroke);
			}
			
			g2.fill(circle);
			
			if(isInSubtree || !isFiltered) {
				g2.setPaint(Color.BLACK);
				g2.draw(circle);
			}
			else {
				g2.setPaint(filterColor);
				g2.draw(circle);
			}
			
			VisualNode vnode = new VisualNode(circle, node);
			visualNodes.add(vnode);
		}
		else {
			double width = 0;
			if (node.parent == null)
				width = levelWidth - 2;
			else
				width = levelWidth * (node.parent.height - node.height) - 2;
			double xstart = xpos + levelWidth / 2 - width - 1;

			double ystart = node.rectStart * (this.getSize().height - 2 * margin) + margin + 1;
			double height = node.rectLen * (this.getSize().height - 2 * margin) - 2;
			
			Rectangle2D rect = new Rectangle2D.Double(xstart, ystart, width, height);
			boolean isFiltered = false;
			
			if((node.score > scoreThresh && !dstree.isReference) || relationFilter.contains(node.relation)){
				g2.setPaint(filterColor);
				g2.setStroke(thinStroke);
				isFiltered = true;
			}
			else if(isInSubtree){	
				g2.setPaint(relationColors[node.relation.ordinal()]);
				g2.setStroke(thickStroke);
			}
			else {
				g2.setPaint(defaultColors[node.relation.ordinal()]);
				//g2.setPaint(new Color(getScoreColor((float)node.score).getRGB() ^ selectionMask, true));
				g2.setStroke(thinStroke);
			}
			
			g2.fill(rect);
			
			if(node.matched && !isFiltered) {
				Rectangle2D rectb = new Rectangle2D.Double(rect.getX() + 2, rect.getY() + 2, rect.getWidth() - 4, rect.getHeight() - 4);
				g2.setPaint(matchcolor);
				g2.fill(rectb);
			}
			
			if(isInSubtree || !isFiltered) {
				if(node.nuclearity == DSTree.Nuclearity.Nucleus)
					g2.setPaint(Color.BLACK);
				else if(node.nuclearity == DSTree.Nuclearity.Satellite)
					g2.setPaint(Color.WHITE);
				g2.draw(rect);
			}
			else {
				g2.setPaint(filterColor);
				g2.draw(rect);
			}
			
			VisualNode vnode = new VisualNode(rect, node);
			visualNodes.add(vnode);
		}
	}

	private void drawLink(Graphics2D g2, Dimension d, DSTreeNode fromNode, 
			DSTreeNode toNode, double fromx, double fromy, boolean isInSubtree) {
		if(display == DisplayType.NodeLink) {
			double chypos = (d.height - margin * 2) * toNode.ypos + margin;
			double chxpos = (d.width - margin * 2) * toNode.realxpos / rightBound + margin;
			//double chypos = (d.height - margin * 2) * (toNode.ypos - upperBound) / (lowerBound - upperBound) + margin;
			Line2D line1 = new Line2D.Double(fromx, fromy, fromx, chypos);
			Line2D line2 = new Line2D.Double(fromx, chypos, chxpos, chypos);
	
			if (toNode.nuclearity == DSTree.Nuclearity.Nucleus)
				if(isInSubtree){
					g2.setPaint(Color.BLACK);
					g2.setStroke(thickStroke);
				}
				else {
					//g2.setPaint(new Color(Color.BLACK.getRGB() ^ selectionMask, true));
					g2.setPaint(Color.BLACK);
					g2.setStroke(thinStroke);
				}
			else			
				if(isInSubtree) {
					g2.setPaint(Color.LIGHT_GRAY);
					g2.setStroke(thickStroke);
				}
				else {
					//g2.setPaint(new Color(Color.LIGHT_GRAY.getRGB() ^ selectionMask, true));
					g2.setPaint(Color.LIGHT_GRAY);
					g2.setStroke(thinStroke);
				}
			
			if(!isInSubtree && ((fromNode.score > scoreThresh && !dstree.isReference) 
					|| relationFilter.contains(fromNode.relation)))
				g2.setPaint(filterColor);
				
			g2.draw(line1);
			g2.draw(line2);
		}
	}
	
	private void paintSelection(Graphics2D g2, Dimension d) {
		if(hoveredNode != null)
			drawSelectionNode(g2, d, hoveredNode, Color.RED);
		if(selectedNodes.size() != 0) {
			drawSelectionNode(g2, d, selectedNodes.get(0), Color.RED);
			for(int i = 1; i < selectedNodes.size(); i++)
				drawSelectionNode(g2, d, selectedNodes.get(i), Color.MAGENTA);
		}
	}
	
	private void drawSelectionNode(Graphics2D g2, Dimension d, DSTreeNode node, Color c) {
		if(!isVisible(node))
			return;
		
		if(display == DisplayType.NodeLink) {
			double xpos = (d.width - margin * 2) * node.realxpos / rightBound + margin;
			double ypos = (d.height - margin * 2) * node.ypos + margin;
			
			double circleR = nodeR + 6;
			Ellipse2D circle = new Ellipse2D.Double(xpos - circleR / 2, ypos - circleR / 2, circleR, circleR);
			g2.setPaint(new Color(c.getRGB() ^ selectionMask, true));
			g2.setStroke(thickStroke);
			g2.draw(circle);
		}
		else {
			Rectangle2D rect = (Rectangle2D)getShapeAtNode(node);
			if(rect != null) {
				g2.setPaint(new Color(c.getRGB() ^ selectionMask, true));
				g2.setStroke(thickStroke);
				g2.draw(rect);
			}
		}
	}
	
	private DSTreeNode getNodeAtPoint(Point p) {
		for (VisualNode vnode : visualNodes) {
			if (vnode.nodeShape.contains(p)) {
				return vnode.node;
			}
		}

		return null;
	}
	
	private Shape getShapeAtNode(DSTreeNode node) {
		for (VisualNode vnode : visualNodes) {
			if (vnode.node == node) {
				return vnode.nodeShape;
			}
		}

		return null;
	}
	
	private boolean isVisible(DSTreeNode node) {
		for (VisualNode vnode : visualNodes) {
			if (vnode.node == node) {
				return true;
			}
		}

		return false;
	}
	
	private void updateTreeTopology() {
		if(dstree == null)
			return;
		visualLeafNodes.clear();
		levelBound = dstree.getTreeLevels();
		addToVisualLeafNodes(dstree.rootNode);
		
		if(levelBound == dstree.getTreeLevels() - 1)
			rightBound = 0.5;
		else
			rightBound = 1.0 - 1.0 / (dstree.getTreeLevels() - 1) * levelBound;
		
		if(visualLeafNodes.size() != 1) {
			int index = 0;
			for (DSTreeNode node : visualLeafNodes) {
				node.ypos = 1.0 / visualLeafNodes.size() * index + 0.5 / visualLeafNodes.size();
				index++;
			}
		}
		else
			dstree.rootNode.ypos = 0.5;
		
		updateTreeTopologyIter(dstree.rootNode);
		
		if(display == DisplayType.NodeLink)
			this.setPreferredSize(new Dimension((dstree.getTreeLevels() - levelBound) * (nodeR + 2) + margin * 2, 
					visualLeafNodes.size() * (nodeR + 2) + margin * 2));
		else
			this.setPreferredSize(new Dimension((dstree.getTreeLevels() - levelBound) * nodeR + margin * 2, 
					visualLeafNodes.size() * nodeR + margin * 2));
	}
	
	private void addToVisualLeafNodes(DSTreeNode node) {
		if (node.collapsed || (node.leftChild == null && node.rightChild == null)) {
			visualLeafNodes.add(node);
			if(node.height < levelBound)
				levelBound = node.height;
		}
		else {
			addToVisualLeafNodes(node.leftChild);
			addToVisualLeafNodes(node.rightChild);
		}
	}
	
	private void updateTreeTopologyIter(DSTreeNode node) {
		if(node == null)
			return;
		
		updateTreeTopologyIter(node.leftChild);
		updateTreeTopologyIter(node.rightChild);
				
		if(node.collapsed || (node.leftChild == null && node.rightChild == null)) {
			node.rectLen = 1.0 / visualLeafNodes.size();
			node.rectStart = node.ypos - node.rectLen / 2;
		}
		else {
			node.ypos = (node.leftChild.ypos + node.rightChild.ypos) / 2;
			node.rectLen = node.rightChild.rectStart + node.rightChild.rectLen - node.leftChild.rectStart;
			node.rectStart = node.leftChild.rectStart;
		}
		
		
	}
	
	private JPopupMenu initPopupMenu() {
		String[] popMenuName = {"Collapse Nodes upto Selected Level", "Collapse Nodes Above Similarity Score", 
				"Expand SubTrees of Selected Nodes", "Expand All Nodes", 
				"-", "Add Note to Selected Nodes", "-", "Add Nodes to Search Panel",
				"-", "Node-Link View", "Icicle-Plot View"};
		JPopupMenu menu = new JPopupMenu();
		
		for(String name : popMenuName) {
			if(name.equals("-"))
				menu.add(new JSeparator());
			else {
				JMenuItem item = new JMenuItem(name);
				menu.add(item);
				item.addActionListener(new MenuActionListener());
			}
		}
		
		return menu;
	}
	// static methods//////////////////////////////////////////////////////////////////////////////
	public static Color getScoreColor(float score) {
		return scoreColors[Math.round(score * (scoreColors.length - 1))];
	}
	
	private static Color[] initScoreColors() {
		int[] rgbs = new int[] {
				255, 255, 229, 
				247, 252, 185, 
				217, 240, 163, 
				173, 221, 142, 
				120, 198, 121, 
				65, 171, 93, 
				35, 132, 67, 
				0, 104, 55, 
				0, 69, 41};

		Color[] colors = new Color[rgbs.length / 3];

		for (int i = 0; i < colors.length; i++) {
			colors[ colors.length - i - 1] = new Color(rgbs[i * 3], rgbs[i * 3 + 1], rgbs[i * 3 + 2], 200);
		}
		
		return colors;
	}
	
	private static Color[] initRelationColors() {
		int[] rgbs = new int[] {
				240,163,255,
				0,117,220,
				153,63,0,
				76,0,92,
				143,124,0,
				194,0,136,
				148,255,181,
				157,204,0,
				255,164,5,
				45,206,72,
				94,241,242,
				0,153,143,
				116,10,255,
				153,0,0,
				255,80,0,
				255,255,0,
				255,0,16,
				66,102,0
		};
		
		Color[] colors = new Color[19];
		colors[0] = new Color(0, 0, 0);
		for(int i = 0; i < 18; i++)
			colors[i + 1] = new Color(rgbs[i * 3], rgbs[i * 3 + 1], rgbs[i * 3 + 2]);

		defaultColors = new Color[19];
		defaultColors[0] = Color.LIGHT_GRAY;
		for(int i = 1; i < 19; i++) {
			HSLColor c = new HSLColor(colors[i]);
			defaultColors[i] = colors[i];//c.adjustLuminance(75);
		}
		
		return colors;
	}

	// inner classes//////////////////////////////////////////////////////////////////////////////
	public class VisualNode {
		public Shape nodeShape;
		public DSTreeNode node;

		public VisualNode() {
		}

		public VisualNode(Shape shape, DSTreeNode node) {
			this.nodeShape = shape;
			this.node = node;
		}
	}
	
	public class MenuActionListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			String event = e.getActionCommand();
			if(event.equals("Add Note to Selected Nodes")) {
				appFrame.tableview.addNodeNote();
				
				appFrame.autoLogger.logAction("notes operation,node note");
			}
			else if(event.equals("Collapse Nodes upto Selected Level")){
				if(selectedLevel == -1)
					return;
				for(int i = 0; i <= dstree.getTreeLevels() - 1 - selectedLevel; i++)
					for(DSTreeNode node : dstree.nodeLevelList[i]){
						node.collapsed = true;
					}
				updateTreeTopology();
				repaint();
				
				appFrame.autoLogger.logAction("tree exploration operation,collapse level");
			}
			else if(event.equals("Collapse Nodes Above Similarity Score")){
				String num = JOptionPane.showInputDialog(DSTreePanel.this, "Please enter the similarity score", "1.0");
				double score = -1;
				try{
					score = Double.parseDouble(num);
				}
				catch(NumberFormatException exp){					
				}
				
				if(score > 0 && score < 1){
					for(int i = 0; i < dstree.nodeLevelList.length; i++)
						for(DSTreeNode node : dstree.nodeLevelList[i]){
							node.collapsed = false;
						}
					for(DSTreeNode node : dstree.nodeLevelList[0]){
						while(node.parent != null && node.score >= score) {
							node.collapsed = true;
							node = node.parent;
						}
					}
					updateTreeTopology();
					repaint();
					
					appFrame.autoLogger.logAction("tree exploration operation,collapse score");
				}
			}
			else if(event.equals("Expand All Nodes")){
				for(int i = 0; i < dstree.nodeLevelList.length; i++)
					for(DSTreeNode node : dstree.nodeLevelList[i]){
						node.collapsed = false;
					}
				updateTreeTopology();
				repaint();
				
				appFrame.autoLogger.logAction("tree exploration operation,expand all");
			}
			else if(event.equals("Expand SubTrees of Selected Nodes")){
				for(DSTreeNode node : selectedNodes)
					expandSubTree(node);
				updateTreeTopology();
				repaint();
				
				appFrame.autoLogger.logAction("tree exploration operation,expand sub");
			}
			else if(event.equals("Add Nodes to Search Panel")){
				String num = JOptionPane.showInputDialog(DSTreePanel.this, "Please enter the number of levels you want to add", "1");
				int level = 0;
				try{
					level = Integer.parseInt(num);
				}
				catch(NumberFormatException exp){					
				}
				
				if(level > 0){
					appFrame.searchPanel.addTemplateTree(selectedNodes.get(0), level);					
					appFrame.autoLogger.logAction("searching operation,add");
				}
			}
			else if(event.equals("Node-Link View")) {
				appFrame.tableview.setDisplayType(DSTreePanel.DisplayType.NodeLink);				
				appFrame.autoLogger.logAction("tree exploration operation,nodelink view");
			}
			else if(event.equals("Icicle-Plot View")){
				appFrame.tableview.setDisplayType(DSTreePanel.DisplayType.Compact);			
				appFrame.autoLogger.logAction("tree exploration operation,compact view");
			}
		}
		
		private void expandSubTree(DSTreeNode node){
			node.collapsed = false;
			if(node.leftChild != null)
				expandSubTree(node.leftChild);
			if(node.rightChild != null)
				expandSubTree(node.rightChild);
		}
	}

}
