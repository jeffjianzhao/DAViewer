package daviewer;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.*;

import daviewer.DSTree.DSTreeNode;


public class DSTreeTableView extends ResizableHeaderTable {
	
	//private static Color editColor = new Color(255, 240, 200);
	private static Color selectColor = new Color(220, 255, 255);
	private static Border editBorder = BorderFactory.createLineBorder(Color.RED);
	private String[] popMenuName = {"Minimize Selected Row/Column", "Maximize Selected Row/Column", "Remove Selected Row/Column"};
	private JPopupMenu popupMenu = new JPopupMenu();
	private Point mousepoint;
	
	private ColumnHeaderListener columnListener = new ColumnHeaderListener();
	private RowHeaderListener rowListener = new RowHeaderListener();
	
	public DSTreePanel editingPanel;
	public int editingRow;
	public static DAViewer appFrame;
	
	public DSTreeTableView() {
		for(String name : popMenuName) {
			if(name.equals("-"))
				popupMenu.add(new JSeparator());
			else {
				JMenuItem item = new JMenuItem(name);
				popupMenu.add(item);
				item.addActionListener(new MenuActionListener());
			}
		}
	}
	
	public void setDSTrees(DSTree[][] tree, String[] cheader, String[] rheader) {
		DSTreeTableModel treeTableModel = new DSTreeTableModel();
		treeTableModel.setData(tree, cheader, rheader);
		
		this.setTableModel(treeTableModel);
		this.setCellRender(DSTreePanel.class, new DSTreeTableRenderer());
		this.setCellEditor(DSTreePanel.class, new DSTreeTableEditor());
		this.initTableCellSize();
		
		mainTable.getTableHeader().setReorderingAllowed(false);
		if(!Arrays.asList(mainTable.getTableHeader().getMouseListeners()).contains(columnListener))
			mainTable.getTableHeader().addMouseListener(columnListener);//new ColumnHeaderListener());
		if(!Arrays.asList(headerColumn.getMouseListeners()).contains(rowListener))
			headerColumn.addMouseListener(rowListener);//new RowHeaderListener());
	}
	
	public void setDisplayType(DSTreePanel.DisplayType viewtype) {
		DSTreePanel.display = viewtype;
		if(mainTable == null)
			return;
		mainTable.repaint();
		this.initTableCellSize();
	}
	
	public void addNodeNote() {
		if(editingPanel != null && editingPanel.getSelectedNodes().size() != 0) {
			DSTree.NodeNote nodenote = new DSTree.NodeNote();
			
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			String result = JOptionPane.showInputDialog(this, "Please enter the note title", 
					"Note-" + dateFormat.format(Calendar.getInstance().getTime()));
			nodenote.title = result;
			
			for(DSTree.DSTreeNode node : editingPanel.getSelectedNodes())
				nodenote.nodeset.add(node);
			editingPanel.getDSTree().notes.add(nodenote);
			appFrame.notePanel.addNodeNotePanel(nodenote);
		}
	}
	
	public void removeNodeNote(DSTree.NodeNote note) {
		if(editingPanel != null){
			editingPanel.getDSTree().notes.remove(note);
		}
	}
	
	public void highlightSelection(HashSet<DSTree.DSTreeNode> nodes) {
		editingPanel.setSelectedNodes(new ArrayList<DSTree.DSTreeNode>(nodes));
		appFrame.infoPanel.setNodeInfo(editingPanel.getSelectedNodes(), editingPanel.getDSTree());
	}
	
	public void updatePeerTreeSelection(){
		DSTreePanel[] treePanels = ((DSTreeTableModel)mainTable.getModel()).getPeerTrees(editingRow);
		ArrayList<DSTree.DSTreeNode> selectedNodes = editingPanel.getSelectedNodes();
		DSTree editingTree = editingPanel.getDSTree();
		boolean[] allrange = new boolean[editingTree.nodeLevelList[0].size()];
		for(DSTree.DSTreeNode node : selectedNodes){
			for(int i = node.textRange.start; i <= node.textRange.end; i++)
				if(!allrange[i])
					allrange[i] = true;
		}
		
		ArrayList<DSTree.Range> ranges = new ArrayList<DSTree.Range>();
		boolean isInRange = false;
		int laststart = 0;
		for(int i = 0; i < allrange.length; i++)
			if(isInRange && !allrange[i]) {
				ranges.add(new DSTree.Range(laststart, i - 1));
				isInRange = false;
			}
			else if(!isInRange && allrange[i]) {
				laststart = i;
				isInRange = true;
			}
		if(isInRange)
			ranges.add(new DSTree.Range(laststart, allrange.length - 1));
		
		for (DSTreePanel panel : treePanels) {
			if(panel == editingPanel)
				continue;
			
			DSTree tree = panel.getDSTree();
			if(tree == null)
				continue;
			ArrayList<DSTree.DSTreeNode> snodes = new ArrayList<DSTree.DSTreeNode>();
			for (DSTree.Range range : ranges) {
				int i = range.start;
				while (i <= range.end) {
					DSTreeNode node = tree.nodeLevelList[0].get(i);
					while (node.parent != null && node.parent.textRange.start >= range.start
							&& node.parent.textRange.end <= range.end)
						node = node.parent;
					snodes.add(node);
					i = node.textRange.end + 1;
				}
			}
			
			panel.setSelectedNodes(snodes);
		}
		
		mainTable.repaint();
	}
	
	public void setScoreThresh(double thresh){
		DSTreePanel.scoreThresh = thresh;
		if(mainTable != null)
			mainTable.repaint();
	}
	
	public void updateRelationFilter(DSTree.Relation relation, boolean isadded){
		if(mainTable != null){
			if(isadded)
				DSTreePanel.relationFilter.add(relation);
			else
				DSTreePanel.relationFilter.remove(relation);
			mainTable.repaint();
		}
	}
	
	public void stopEditing(){
		if (mainTable != null && mainTable.isEditing())
			mainTable.getCellEditor().stopCellEditing();
	}
	
	public int searchByTemplate(DSTemplateTree template, String text, DSTreeSearchPanel.SearchMethod method){
		int hitnum = 0;
		if(mainTable == null)
			return 0;
		
		if(method == DSTreeSearchPanel.SearchMethod.Text) 
			searchText(text, true);
		else if(method == DSTreeSearchPanel.SearchMethod.Structure)
			searchTrees(template, true);
		else {
			searchText(text, true);
			searchTrees(template, false);
		}

		DSTreeTableModel model = (DSTreeTableModel)mainTable.getModel();
		for(int i = 0; i < model.getRowCount(); i++)
			for(int j = 1; j < model.getColumnCount(); j++){
				DSTreePanel panel = (DSTreePanel)model.getValueAt(i, j);
				hitnum += panel.getDSTree().hitnum;
			}
		
		return hitnum;
	}
	
	private void searchTrees(DSTemplateTree template, boolean isnew){
		DSTreeTableModel model = (DSTreeTableModel)mainTable.getModel();
		for(int i = 0; i < model.getRowCount(); i++)
			for(int j = 1; j < model.getColumnCount(); j++){
				DSTreePanel panel = (DSTreePanel)model.getValueAt(i, j);
				panel.getDSTree().searchTree(template, isnew);
			}
		mainTable.repaint();
	}
	
	private void searchText(String text, boolean isnew){
		DSTreeTableModel model = (DSTreeTableModel)mainTable.getModel();
		for(int i = 0; i < model.getRowCount(); i++)
			for(int j = 1; j < model.getColumnCount(); j++){
				DSTreePanel panel = (DSTreePanel)model.getValueAt(i, j);
				panel.getDSTree().searchText(text, isnew);
			}
		mainTable.repaint();
	}
	// inner classes//////////////////////////////////////////////////////////////////////////////
	public class DSTreeTableModel extends AbstractTableModel {
		
		private DSTreePanel[][] data = new DSTreePanel[0][0];
		private String[] cheaders = new String[0];
		private String[] rheaders = new String[0];
		
		public int getColumnCount() {
			return cheaders.length + 1;
		}

		public int getRowCount() {
			return rheaders.length;
		}

		public Class getColumnClass(int c) {
			if(c == 0)
				return String.class;
			else
				return DSTreePanel.class;
		}
		
		public String getColumnName(int c) {
			if(c == 0)
				return " ";
			else
				return cheaders[c - 1];
		}
		
		public boolean isCellEditable(int r, int c) {
			if(c == 0)
				return false;
			else
				return true;
		}
		
		public Object getValueAt(int r, int c) {
			if(c == 0)
				return rheaders[r];
			else
				return data[r][c - 1];
		}
		
		public void setData(DSTree[][] tree, String[] cheader, String[] rheader) {
			this.cheaders = cheader;
			this.rheaders = rheader;
			this.data = new DSTreePanel[rheaders.length][cheaders.length];
			for(int i = 0; i < rheaders.length; i++)
				for(int j = 0; j < cheaders.length; j++) {
					data[i][j] = new DSTreePanel();
					data[i][j].setDSTree(tree[i][j]);
				}
			this.fireTableDataChanged();
		}
		
		public DSTreePanel[] getPeerTrees(int row) {
			return data[row];
		}
	}
	
	public class DSTreeTableRenderer implements TableCellRenderer {

		public Component getTableCellRendererComponent(JTable table, Object value, 
				boolean isSelected, boolean hasFocus, int row, int col) {
			DSTreePanel treepanel = (DSTreePanel) value;
			if (value != null)
				if(isSelected)
					treepanel.setBackground(selectColor);
				else
					treepanel.setBackground(Color.WHITE);
			treepanel.setBorder(null);
			return treepanel;
		}
	}

	public class DSTreeTableEditor extends AbstractCellEditor implements TableCellEditor {

		public Object getCellEditorValue() {
			return null;
		}

		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int col) {
			DSTreePanel treepanel = (DSTreePanel) value;
			if (value != null) {
				editingPanel = treepanel;
				editingRow = row;
				treepanel.setDSTreeArticle();
				//treepanel.setBackground(editColor);
				treepanel.setBorder(editBorder);
				appFrame.overview.updateSelection(row, col);
				if(treepanel.getDSTree() != null)
					appFrame.notePanel.updateNodeNodePanel(treepanel.getDSTree());
			}
		
			mainTable.clearSelection();
			mainTable.setColumnSelectionAllowed(false);
			mainTable.setRowSelectionAllowed(false);
			return treepanel;
		}
		
		public boolean stopCellEditing(){
			for(DSTreePanel panel : ((DSTreeTableModel)mainTable.getModel()).getPeerTrees(editingRow)){
				panel.clearSelection();
			}
			
			mainTable.repaint();
			appFrame.infoPanel.setNodeInfo(null, null);
			return super.stopCellEditing();
		}
	}
	
	public class ColumnHeaderListener extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			TableColumnModel colModel = mainTable.getColumnModel();
			int col = colModel.getColumnIndexAtX(e.getX());
			if(col == -1)
				return;
			
			appFrame.overview.updateSelection(-1, col);
			if (mainTable.isEditing())
				mainTable.getCellEditor().stopCellEditing();
			
			if(e.getClickCount() == 1){
				mainTable.clearSelection();
				mainTable.setColumnSelectionAllowed(true);
				mainTable.setRowSelectionAllowed(false);
				mainTable.setColumnSelectionInterval(col, col);
				appFrame.textPane.setDSTree(null);
			}
			else if(e.getClickCount() == 2) {
				TableColumn tc = colModel.getColumn(col);
				int oldWidth = tc.getPreferredWidth();
				if(oldWidth > DSTreePanel.smallSize) {
					tc.setPreferredWidth(DSTreePanel.smallSize);
					appFrame.autoLogger.logAction("tree viewing operation,minimize");
				}
				else {
					tc.setPreferredWidth(getColumnPreferredWidth(col));
					appFrame.autoLogger.logAction("tree viewing operation,maximize");
				}
			}
		}
		
		public void mousePressed(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON3) {
				mousepoint = e.getPoint();
	            popupMenu.show(mainTable.getTableHeader(), e.getX(), e.getY());
	        }
		}
	}
	
	public class RowHeaderListener extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {			
			int row = headerColumn.rowAtPoint(e.getPoint());
			if(row == -1)
				return;

			appFrame.overview.updateSelection(row, -1);
			if (mainTable.isEditing())
				mainTable.getCellEditor().stopCellEditing();
			
			if(e.getClickCount() == 1){
				mainTable.clearSelection();
				mainTable.setColumnSelectionAllowed(false);
				mainTable.setRowSelectionAllowed(true);
				mainTable.setRowSelectionInterval(row, row);
				appFrame.textPane.setDSTree(null);
			}
			else if(e.getClickCount() == 2) {
				int oldHeight = mainTable.getRowHeight(row);
				if(oldHeight > DSTreePanel.smallSize) {
					setRowHeight(row, DSTreePanel.smallSize);
					appFrame.autoLogger.logAction("tree viewing operation,minimize");
				}
				else {
					setRowHeight(row, getRowPreferredHeight(row));
					appFrame.autoLogger.logAction("tree viewing operation,maximize");
				}
			}
		}
		
		public void mousePressed(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON3) {
				mousepoint = e.getPoint();
	            popupMenu.show(headerColumn, e.getX(), e.getY());
	        }
		}
	}
	
	public class MenuActionListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			String event = e.getActionCommand();
			if(event.equals("Minimize Selected Row/Column")) {
				if(popupMenu.getInvoker() == headerColumn) {
					int row = headerColumn.rowAtPoint(mousepoint);
					if(row != -1) 
						setRowHeight(row, DSTreePanel.smallSize);
				}
				else if(popupMenu.getInvoker() == mainTable.getTableHeader()) {
					TableColumnModel colModel = mainTable.getColumnModel();
					int col = colModel.getColumnIndexAtX(mousepoint.x);
					if(col != -1)
						colModel.getColumn(col).setPreferredWidth(DSTreePanel.smallSize);
				}
				
				appFrame.autoLogger.logAction("tree viewing operation,minimize");
			}
			else if(event.equals("Maximize Selected Row/Column")) {
				if(popupMenu.getInvoker() == headerColumn) {
					int row = headerColumn.rowAtPoint(mousepoint);
					if(row != -1) 
						setRowHeight(row, getRowPreferredHeight(row));
				}
				else if(popupMenu.getInvoker() == mainTable.getTableHeader()) {
					TableColumnModel colModel = mainTable.getColumnModel();
					int col = colModel.getColumnIndexAtX(mousepoint.x);
					if(col != -1)
						colModel.getColumn(col).setPreferredWidth(getColumnPreferredWidth(col));
				}
				
				appFrame.autoLogger.logAction("tree viewing operation,maximize");
			}
			else if(event.equals("Remove Selected Row/Column")) {
				DSTreeOverview.DSTreeSelection selections = appFrame.overview.getNewFocusedSelection(DSTreeOverview.Del_Focus);
				if(selections != null) 
					setDSTrees(selections.trees, selections.cheader, selections.rheader);	
				
				appFrame.autoLogger.logAction("tree viewing operation,remove");
			}
		}
		
	}
}
