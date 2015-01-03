package daviewer;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.text.*;


public class DSTreeArticlePane extends JTextPane {

	public enum DisplayMode {
		Continuous, Seperated, Hybrid
	};

	private DisplayMode displayMode = DisplayMode.Hybrid;
	private DSTree dstree;
	
	private int[] textStarts;
	private DSTree.DSTreeNode lastNode;
	private int lastLevel = -1;
	private int lastCaret = 0;
	
	private JPopupMenu popupMenu = new JPopupMenu();
	private String[] popMenuName = {"Hybrid Text View", "Continuous Text View", "Seperated Text View"};

	private static SimpleAttributeSet[] textAttrs = initTextAttributes();
	public static DAViewer appFrame;
	
	// public methods//////////////////////////////////////////////////////////////////////////////
	public DSTreeArticlePane() {
		this.addMouseListener(new ArticleMouseListener());
		this.setEditable(false);
	}
	
	public void setDSTree(DSTree tree) {
		if (dstree != tree) {
			dstree = tree;
			lastLevel = -1;
			lastNode = null;
			lastCaret = 0;
			
			if(dstree != null)
				setArticle();
			else
				clearText();
		}
	}

	public void setViewMode(DisplayMode mode) {
		if (mode == displayMode)
			return;
		displayMode = mode;

		if(dstree != null) {
			setArticle();
			organizeText(lastLevel);
			highlightNodeText(lastNode);
		}
	}

	public void clearText() {
		this.setText("");
	}
	
	public void organizeText(int level) {
		lastLevel = level;

		if (displayMode != DisplayMode.Hybrid)
			return;
		if (level < 0 || level >= dstree.getTreeLevels())
			return;

		StringBuilder sb = new StringBuilder();
		textStarts = new int[dstree.article.size()];
		
		if (level == dstree.getTreeLevels() - 1) { // the rootnode - basically continuous view
			int pos = 0;
			sb.append("[" + dstree.rootNode.textRange.start + "-" + dstree.rootNode.textRange.end + "] ");
			
			for(int i = 0; i < dstree.article.size(); i++){
				DSTree.DSTreeNode node = dstree.nodeLevelList[0].get(i);
				sb.append(dstree.article.get(i));
				sb.append("  ");
				
				textStarts[i] = pos;
				pos = sb.length();
			}		
		} 
		else {
			int pos = 0;
			int i = 0;
			while (i < dstree.article.size()) {
				DSTree.DSTreeNode node = dstree.nodeLevelList[0].get(i);
				DSTree.DSTreeNode prevnode = node;
				while (node != null && node.height <= level) {
					prevnode = node;
					node = node.parent;
				}
				node = prevnode;

				if(node.leftChild != null) {
					sb.append("[" + node.textRange.start + "-" + node.textRange.end + "] ");
				}
				else {
					sb.append("[" + node.textRange.start + "] ");
				}
				
				for (int t = 0; t < node.textRange.end - node.textRange.start + 1; t++) {
					sb.append(dstree.article.get(t + i) + "  ");
					textStarts[t + i] = pos;
					pos = sb.length();
				}
				sb.replace(sb.length() - 2, sb.length(), "\n\n");

				i += node.textRange.end - node.textRange.start + 1;
			}
		}

		this.setText(sb.toString());
		StyledDocument doc = this.getStyledDocument();
		doc.setCharacterAttributes(0, doc.getLength(), textAttrs[0], false);
		this.setCaretPosition(lastCaret);
	}

	public void highlightNodeText(DSTree.DSTreeNode node) {
		lastNode = node;

		if (node == null)
			return;

		StyledDocument doc = this.getStyledDocument();
		doc.setCharacterAttributes(0, doc.getLength(), textAttrs[0], false);

		if (node.relation == DSTree.Relation.None) { // leaf node
			int start = textStarts[node.textRange.start];
			int len = node.textRange.start == textStarts.length - 1 ? doc.getLength() - 1 - start : 
				textStarts[node.textRange.start+ 1] - start;
			doc.setCharacterAttributes(start, len, textAttrs[1], false);
			this.setCaretPosition(start);
			lastCaret = start;
		} else {	// non-leaf node
			Color c = DSTreePanel.relationColors[node.relation.ordinal()];
			// left child
			SimpleAttributeSet textAttr = new SimpleAttributeSet();
			if(node.leftChild.nuclearity == DSTree.Nuclearity.Nucleus)
				StyleConstants.setBackground(textAttr, new Color(c.getRGB() ^ 0x7F000000, true));
			else
				StyleConstants.setBackground(textAttr, new Color(c.getRGB() ^ 0xC3000000, true));
			StyleConstants.setBold(textAttr, true);
			StyleConstants.setUnderline(textAttr, true);
			
			int start = textStarts[node.textRange.start];
			int len = textStarts[node.rightChild.textRange.start] - start;
			doc.setCharacterAttributes(start, len, textAttr, false);
			
			// right child
			textAttr = new SimpleAttributeSet();
			if(node.rightChild.nuclearity == DSTree.Nuclearity.Nucleus)
				StyleConstants.setBackground(textAttr, new Color(c.getRGB() ^ 0x7F000000, true));
			else
				StyleConstants.setBackground(textAttr, new Color(c.getRGB() ^ 0xC3000000, true));
			StyleConstants.setBold(textAttr, true);
			
			start = start + len;
			len = node.textRange.end == textStarts.length - 1 ? doc.getLength() - 1 - start : 
				textStarts[node.textRange.end + 1] - start;
			doc.setCharacterAttributes(start, len, textAttr, false);

			this.setCaretPosition(start);
			lastCaret = start;
		}
	}
	
	public void createContextMenu() {
		for(String name : popMenuName) {
			if(name.equals("-"))
				popupMenu.add(new JSeparator());
			else {
				JMenuItem item = new JMenuItem(name);
				popupMenu.add(item);
				item.addActionListener(appFrame.new MenuActionListener());
			}
		}
	}

	// private methods//////////////////////////////////////////////////////////////////////////////
	private void setArticle() {
		StringBuilder sb = new StringBuilder();
		textStarts = new int[dstree.article.size()];
		
		int pos = 0;
		if (displayMode == DisplayMode.Continuous){
			for(int i = 0; i < dstree.article.size(); i++){
				DSTree.DSTreeNode node = dstree.nodeLevelList[0].get(i);
				sb.append("[" + node.textRange.start + "] ");
				sb.append(dstree.article.get(i));
				sb.append("  ");
				
				textStarts[i] = pos;
				pos = sb.length();
			}		
		}
		else {
			for(int i = 0; i < dstree.article.size(); i++){
				DSTree.DSTreeNode node = dstree.nodeLevelList[0].get(i);
				sb.append("[" + node.textRange.start + "] ");
				sb.append(dstree.article.get(i));
				sb.append("\n\n");
				
				textStarts[i] = pos;
				pos = sb.length();
			}
		}

		this.setText(sb.toString());
		StyledDocument doc = this.getStyledDocument();
		doc.setCharacterAttributes(0, doc.getLength(), textAttrs[0], false);
	}

	private static SimpleAttributeSet[] initTextAttributes() {		
		SimpleAttributeSet[] textAttr = new SimpleAttributeSet[2];

		textAttr[0] = new SimpleAttributeSet();
		StyleConstants.setBackground(textAttr[0], Color.WHITE);
		StyleConstants.setBold(textAttr[0], false);
		StyleConstants.setUnderline(textAttr[0], false);
		
		textAttr[1] = new SimpleAttributeSet();
		StyleConstants.setBackground(textAttr[1], Color.WHITE);
		StyleConstants.setBold(textAttr[1], true);
		StyleConstants.setUnderline(textAttr[1], false);
		
		return textAttr;
	}
	
	
	
	public class ArticleMouseListener extends MouseAdapter {
		public void mousePressed(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON3) {
	            popupMenu.show(e.getComponent(), e.getX(), e.getY());
	        }
	    }
	}

}
