package daviewer;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

import javax.swing.*;

public class DSTreeNotePanel extends JTabbedPane {

	private ArrayList<TreeNoteCollapsiblePanel> treePanelList;
	private ArrayList<NodeNoteCollapsiblePanel> nodePanelList;
	private JPanel treeNote = new JPanel();
	private JPanel nodeNote = new JPanel();
	private JScrollPane jsp1;
	private JScrollPane jsp2;
	
	public static DAViewer appFrame;
	
	public DSTreeNotePanel() {
		treeNote.setLayout(new BoxLayout(treeNote, BoxLayout.Y_AXIS));
		jsp1 = new JScrollPane(treeNote);
		
		nodeNote.setLayout(new BoxLayout(nodeNote, BoxLayout.Y_AXIS));
		jsp2 = new JScrollPane(nodeNote);
		
		this.addTab("Tree Level Notes", jsp1);
		this.addTab("Node Level Note", jsp2);
	}
	
	public void updateTreeNotePanel(ArrayList<DSTree.TreeNote> treenotes) {
		treeNote.removeAll();
		treePanelList = new ArrayList<TreeNoteCollapsiblePanel>();
		
		for(DSTree.TreeNote note : treenotes) {
			TreeNoteCollapsiblePanel panel = new TreeNoteCollapsiblePanel(note);
			treeNote.add(panel);
			treePanelList.add(panel);
		}
		
		jsp1.validate();
	}
	
	public void addTreeNotePanel(DSTree.TreeNote treenote) {
		TreeNoteCollapsiblePanel panel = new TreeNoteCollapsiblePanel(treenote);
		treePanelList.add(panel);
		treeNote.add(panel);
		panel.setCollapsed(false);
		
		jsp1.validate();
		this.setSelectedIndex(0);
	}
	
	public void highlightTreeNotePanel(DSTree[][] trees) {
		boolean[] status = new boolean[treePanelList.size()];
		
		for(int i = 0; i < trees.length; i++)
			for(int j = 0; j < trees[i].length; j++) {
				for(int k = 0; k < status.length; k++)
					if(!status[k] && treePanelList.get(k).treenote.treeset.contains(trees[i][j]))
						status[k] = true;
			}
		
		for(int i = 0; i < status.length; i++)
			treePanelList.get(i).setHighlighted(status[i]);
	}
	
	public void updateNodeNodePanel(DSTree dstree) {
		nodeNote.removeAll();
		nodePanelList = new ArrayList<NodeNoteCollapsiblePanel>();
		
		for(DSTree.NodeNote note : dstree.notes) {
			NodeNoteCollapsiblePanel panel = new NodeNoteCollapsiblePanel(note);
			nodeNote.add(panel);
			nodePanelList.add(panel);
		}
		
		nodeNote.repaint();
		jsp2.validate();
	}
	
	public void addNodeNotePanel(DSTree.NodeNote nodenote) {
		NodeNoteCollapsiblePanel panel = new NodeNoteCollapsiblePanel(nodenote);
		nodePanelList.add(panel);
		nodeNote.add(panel);
		panel.setCollapsed(false);
		
		jsp2.validate();
		this.setSelectedIndex(1);
	} 
	
	public void highlightNodeNotePanel(ArrayList<DSTree.DSTreeNode> nodes) {
		boolean[] status = new boolean[nodePanelList.size()];
		
		for(int i = 0; i < nodes.size(); i++)
			for(int k = 0; k < status.length; k++)
				if(!status[k] && nodePanelList.get(k).nodenote.nodeset.contains(nodes.get(i)))
					status[k] = true;
		
		for(int i = 0; i < status.length; i++)
			nodePanelList.get(i).setHighlighted(status[i]);
	}
	
	public void removeNote(){
		if(this.getSelectedComponent() == jsp1){
			for(TreeNoteCollapsiblePanel panel : treePanelList)
				if(panel.isSelected()){
					appFrame.overview.removeTreeNote(panel.treenote);
					treePanelList.remove(panel);
					treeNote.remove(panel);
					jsp1.validate();
					break;
				}
		}
		else if(this.getSelectedComponent() == jsp2){
			for(NodeNoteCollapsiblePanel panel : nodePanelList)
				if(panel.isSelected()){
					appFrame.tableview.removeNodeNote(panel.nodenote);
					nodePanelList.remove(panel);
					nodeNote.remove(panel);
					jsp2.validate();
					break;
				}
		}
			
	}
	
	// inner classes ////////////////////////////////////////////////////////////////////////////////
	public class TreeNoteCollapsiblePanel extends CollapsiblePanel implements MouseListener {

	    public DSTree.TreeNote treenote;
	    
		public TreeNoteCollapsiblePanel(DSTree.TreeNote treenote) {
			super(treenote.title);
			this.treenote = treenote;
			this.getContentPane().add(new NoteTextArea(treenote));
			this.titleComponent.addMouseListener(this);
		}   
	    
		public void mouseClicked(MouseEvent e) {
			if(e.getClickCount() == 1) {
				appFrame.overview.updateTreeSetSelection(treenote.treeset);
				for(TreeNoteCollapsiblePanel panel : treePanelList)
					if(panel == this)
						panel.setSelected(true);
					else 
						panel.setSelected(false);
			}	
		}

		public void mouseEntered(MouseEvent e) {}

		public void mouseExited(MouseEvent e) {}

		public void mousePressed(MouseEvent e) {}

		public void mouseReleased(MouseEvent e) {}
		
	    public class NoteTextArea extends JTextArea implements FocusListener {
			
			public NoteTextArea(DSTree.TreeNote note) {
				this.setText(note.note);
				this.addFocusListener(this);
			}

			public void focusGained(FocusEvent arg0) {}

			public void focusLost(FocusEvent arg0) {
				treenote.note = this.getText();
			}
		}
	}
	
	public class NodeNoteCollapsiblePanel extends CollapsiblePanel implements MouseListener {
		
		public DSTree.NodeNote nodenote;
		
		public NodeNoteCollapsiblePanel(DSTree.NodeNote nodenote) {
			super(nodenote.title);
			this.nodenote = nodenote;
			this.getContentPane().add(new NoteTextArea(nodenote));
			this.titleComponent.addMouseListener(this);
		}
		
		public void mouseClicked(MouseEvent e) {
			if(e.getClickCount() == 1) {
				appFrame.tableview.highlightSelection(nodenote.nodeset);
				for(NodeNoteCollapsiblePanel panel : nodePanelList)
					if(panel == this)
						panel.setSelected(true);
					else 
						panel.setSelected(false);
			}	
		}

		public void mouseEntered(MouseEvent e) {}

		public void mouseExited(MouseEvent e) {}

		public void mousePressed(MouseEvent e) {}

		public void mouseReleased(MouseEvent e) {}
		
		public class NoteTextArea extends JTextArea implements FocusListener{
			
			public NoteTextArea(DSTree.NodeNote note) {
				this.setText(note.note);
				this.addFocusListener(this);
			}

			public void focusGained(FocusEvent arg0) {}

			public void focusLost(FocusEvent arg0) {
				nodenote.note = this.getText();
			}
		}
	}
	

}
