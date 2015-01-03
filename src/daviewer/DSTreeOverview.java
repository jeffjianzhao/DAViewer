package daviewer;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.*;


public class DSTreeOverview extends JScrollPane{
	private DSTreeOverviewModel treeTableModel;
	private JTable mainTable;
	private JTable headerColumn;
	private int[] focusedRows = new int[0];
	private int[] focusedColumns = new int[0];

	private JPopupMenu popupMenu = new JPopupMenu();
	
	private static final int tableCellSize = 18;
	private String[] popMenuName = {"View Selected Trees", "Add Selected Rows/Columns", "Remove Selected Rows/Columns", "-", "Set Refrence Column", 
			"-", "Append Column", "Remove Columns", "Append Row", "Remove Rows", "-", "Add Note to Selected Trees"};
	
	public static final int New_Focus = 0, Add_Focus = 1, Del_Focus = 2;
	public static DAViewer appFrame;
	private static DecimalFormat twoPlaces = new DecimalFormat("0.00");
	private static Border focusBorder = BorderFactory.createLineBorder(Color.BLUE);
	private static Border activeBorder = BorderFactory.createLineBorder(Color.RED);
	
	//public methods//////////////////////////////////////////////////////////////////////////////
	public void saveTreeTableModel(File f) throws IOException {
		FileOutputStream fos = new FileOutputStream(f);
		ObjectOutputStream oos = new ObjectOutputStream(fos); 
		if(treeTableModel != null)
			oos.writeObject(treeTableModel);
		oos.close();
	}
	
	public void openTreeTableModel(File f) throws IOException {
		FileInputStream fis = new FileInputStream(f);
		ObjectInputStream ois = new ObjectInputStream(fis);
		try {
			treeTableModel = (DSTreeOverviewModel)ois.readObject();
			mainTable = new JTable(treeTableModel);
			initializeTable();
		    clearTableView();
		    createDSTreeTableView();		
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(appFrame, e);
		}
		finally {
			ois.close();
		}
	}
	
	public void loadTreeSet(File f, boolean isNew) throws Exception {
		if(isNew) {
			if(mainTable != null) {
				int result = JOptionPane.showConfirmDialog(this, "Previous treeset will be replaced. Are you sure?", 
						"Open Treeset", JOptionPane.OK_CANCEL_OPTION);
				if(result == JOptionPane.CANCEL_OPTION)
					return;
			}
			
			DSTreeOverviewModel newModel = new DSTreeOverviewModel();
			newModel.loadTreeSet(f);	
			newModel.setReferenceColumn(0);
			
			treeTableModel = newModel;
			mainTable = new JTable(treeTableModel);
			initializeTable();
			clearTableView();
		}
		else {
			int[] rowIndices = getSelectedIndices(mainTable.getSelectionModel());
			int[] colIndices = getSelectedIndices(mainTable.getColumnModel().getSelectionModel());
			
			DSTreeOverviewModel newModel = new DSTreeOverviewModel();
			newModel.loadTreeSet(f);
			
			if(newModel.getColumnCount() != colIndices.length || newModel.getRowCount() != rowIndices.length) {
				JOptionPane.showMessageDialog(appFrame, "The input treeset does not match the selection.");
				return;
			}
			
			for(int i = 0; i < rowIndices.length; i++)
				for(int j = 0; j < colIndices.length; j++)
					if(treeTableModel.getValueAt(rowIndices[i], colIndices[j]) != null){
						int result = JOptionPane.showConfirmDialog(this, "Existing trees will be replaced. Are you sure?", 
								"Open Treeset", JOptionPane.OK_CANCEL_OPTION);
						if(result == JOptionPane.CANCEL_OPTION)
							return;
						break;
					}				
			boolean cmpres = true;
			for(int i = 0; i < rowIndices.length; i++)
				for(int j = 0; j < colIndices.length; j++) {
					treeTableModel.setValueAt(rowIndices[i], colIndices[j], (DSTree)newModel.getValueAt(i, j));
					boolean res = ((DSTree)treeTableModel.getValueAt(rowIndices[i], colIndices[j])).compareToTree(
							(DSTree)treeTableModel.getValueAt(rowIndices[i], treeTableModel.referenceColumn));
					if(cmpres && !res)
						cmpres = res;
				}
			if(!cmpres)
				JOptionPane.showMessageDialog(this, "It seems that there are some trees with different number of EDUs in the same row");
			createDSTreeTableView();
			mainTable.repaint();
			appFrame.tableview.repaint();	
		}	
	}
	
	public void appendTable(String header, boolean isRow) {
		if(isRow) {
			treeTableModel.appendRow(header);
		}
		else {
			treeTableModel.appendColumn(header);
			mainTable.createDefaultColumnsFromModel();
		}
		initializeTable();
	}
	
	public void removeTable(boolean isRow) {
		if(isRow) {
			int[] rowIndices = getSelectedIndices(mainTable.getSelectionModel());
			treeTableModel.removeRows(rowIndices);
		}
		else {
			int[] colIndices = getSelectedIndices(mainTable.getColumnModel().getSelectionModel());
			treeTableModel.removeColumns(colIndices);
			mainTable.createDefaultColumnsFromModel();
		}
		initializeTable();
		clearTableView();
	}
	
	public DSTreeSelection getNewFocusedSelection(int mode) {
		int[] rowIndices = getSelectedIndices(mainTable.getSelectionModel());
		int[] colIndices = getSelectedIndices(mainTable.getColumnModel().getSelectionModel());
		
		if(mode == DSTreeOverview.New_Focus) {
			focusedRows = rowIndices;
			focusedColumns = colIndices;
		}
		else if(mode == DSTreeOverview.Add_Focus) {
			focusedRows = getUnion(focusedRows, rowIndices);
			focusedColumns = getUnion(focusedColumns, colIndices);
		}
		else if(mode == DSTreeOverview.Del_Focus){
			int[] remRow = getRemainder(focusedRows, rowIndices);
			int[] remCol = getRemainder(focusedColumns, colIndices);
			
			if(remRow.length == 0 && remCol.length == 0)
				return null;
			else if(remRow.length == 0)
				focusedColumns = remCol;
			else if(remCol.length == 0)
				focusedRows = remRow;
			else if(remRow.length != focusedRows.length || remCol.length != focusedColumns.length){
				focusedRows = remRow;
				focusedColumns = remCol;
			}
		}
		else
			return null;
			
		treeTableModel.setFocusByIndices(focusedRows, true);
		treeTableModel.setFocusByIndices(focusedColumns, false);
		headerColumn.repaint();
		mainTable.getTableHeader().repaint();
		
		DSTreeSelection selection = new DSTreeSelection();
		selection.rheader = treeTableModel.getHeaderByIndices(focusedRows, true);
		selection.cheader = treeTableModel.getHeaderByIndices(focusedColumns, false);
		selection.trees = treeTableModel.getValueByIndices(focusedRows, focusedColumns);
		
		/*
		for(int i = 0; i < selection.rheader.length; i++)
			for(int j = 0; j < selection.cheader.length; j++){
				if(selection.trees[i][j] == null) {
					JOptionPane.showMessageDialog(appFrame, "There are empty trees in the selection.");
					return null;
				}
			}
		*/
		return selection;
	}
	
	public void updateSelection(int row, int col) {
		if(row < 0 && col < 0)
			return;
		mainTable.clearSelection();
		if(row < 0) {
			for(int i = 0; i < focusedRows.length; i++)
				mainTable.addRowSelectionInterval(focusedRows[i], focusedRows[i]);			
			mainTable.addColumnSelectionInterval(focusedColumns[col], focusedColumns[col]);
			
			appFrame.infoPanel.setTreeInfo(treeTableModel.getHeaderByIndices(focusedRows, true), 
					treeTableModel.getHeaderByIndices(new int[]{focusedColumns[col]}, false));
			
			appFrame.notePanel.highlightTreeNotePanel(treeTableModel.getValueByIndices(focusedRows, new int[]{col}));
		}
		else if (col < 0) {
			for(int i = 0; i < focusedColumns.length; i++)
				mainTable.addColumnSelectionInterval(focusedColumns[i], focusedColumns[i]);
			mainTable.addRowSelectionInterval(focusedRows[row], focusedRows[row]);
			
			appFrame.infoPanel.setTreeInfo(treeTableModel.getHeaderByIndices(new int[]{focusedRows[row]}, true), 
					treeTableModel.getHeaderByIndices(focusedColumns, false));
			
			appFrame.notePanel.highlightTreeNotePanel(treeTableModel.getValueByIndices(new int[]{row}, focusedColumns));
		}
		else {
			int r = focusedRows[row];
			int c = focusedColumns[col];
			
			mainTable.addRowSelectionInterval(r, r);
			mainTable.addColumnSelectionInterval(c, c);
			
			appFrame.infoPanel.setTreeInfo(treeTableModel.getRowName(r), 
					treeTableModel.getColumnName(c), (DSTree)treeTableModel.getValueAt(r, c));
			
			appFrame.notePanel.highlightTreeNotePanel(new DSTree[][] {{(DSTree) treeTableModel.getValueAt(r, c)}});
		}
		
	}
	
	public void setReferenceColumn() {
		int[] colIndices = getSelectedIndices(mainTable.getColumnModel().getSelectionModel());
		if(colIndices.length <= 0)
			return;
		treeTableModel.setReferenceColumn(colIndices[0]);
		mainTable.repaint();
		appFrame.tableview.repaint();
	}
	
	public void addTreeNote() {
		int[] rowIndices = getSelectedIndices(mainTable.getSelectionModel());
		int[] colIndices = getSelectedIndices(mainTable.getColumnModel().getSelectionModel());
		
		if(rowIndices.length == 0 || colIndices.length == 0)
			return;
			
		DSTree.TreeNote note = new DSTree.TreeNote();
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		String result = JOptionPane.showInputDialog(this, "Please enter the note title", 
				"Note-" + dateFormat.format(Calendar.getInstance().getTime()));
		note.title = result;
		
		DSTree[][] trees = treeTableModel.getValueByIndices(rowIndices, colIndices);
		for(int i = 0; i < rowIndices.length; i++)
			for(int j = 0; j < colIndices.length; j++)
				note.treeset.add(trees[i][j]);
		
		appFrame.notePanel.addTreeNotePanel(note);
		treeTableModel.notes.add(note);
	}
	
	public void removeTreeNote(DSTree.TreeNote note){
		treeTableModel.notes.remove(note);
	}
	
	public void updateTreeSetSelection(HashSet<DSTree> trees) {		
		boolean[] rows = new boolean[treeTableModel.getRowCount()];
		boolean[] cols = new boolean[treeTableModel.getColumnCount()];
		treeTableModel.getIndexByValues(trees, rows, cols);
		
		mainTable.clearSelection();
		for(int i = 0; i < rows.length; i++)
			if(rows[i]) 
				mainTable.addRowSelectionInterval(i, i);	
		for(int j = 0; j < cols.length; j++)
			if(cols[j]) 
				mainTable.addColumnSelectionInterval(j, j);
		
		
		int[] rowIndices = getSelectedIndices(mainTable.getSelectionModel());
		int[] colIndices = getSelectedIndices(mainTable.getColumnModel().getSelectionModel());
		
		if(rowIndices.length == 1 && colIndices.length == 1) {
			int r = rowIndices[0];
			int c = colIndices[0];
			
			appFrame.infoPanel.setTreeInfo(treeTableModel.getRowName(r), 
					treeTableModel.getColumnName(c), (DSTree)treeTableModel.getValueAt(r, c));
		}
		else
			appFrame.infoPanel.setTreeInfo(treeTableModel.getHeaderByIndices(rowIndices, true), 
				treeTableModel.getHeaderByIndices(colIndices, false));
		
		appFrame.notePanel.highlightTreeNotePanel(treeTableModel.getValueByIndices(rowIndices, colIndices));
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
	//private methods//////////////////////////////////////////////////////////////////////////////
	private void clearTableView() {
		appFrame.tableview.clearTable();
		appFrame.textPane.clearText();
	}
	
	private int[] getUnion(int[] x, int[] y) {
		int[] total = new int[x.length + y.length];
		int i = 0, j = 0, k = 0;
		while(i < x.length && j < y.length) {
			if(x[i] < y[j]) 
				total[k++] = x[i++];
			else if(x[i] > y[j]) 
				total[k++] = y[j++];
			else {
				total[k++] = x[i];
				i++;
				j++;
			}	
		}
		
		if(i < x.length)
			while(i < x.length) {
				total[k++] = x[i++];
			}
		if(j < y.length)
			while(j < y.length) {
				total[k++] = y[j++];
			}
		
		int[] union = new int[k];
		System.arraycopy(total, 0, union, 0, k);
		return union;
	}

	private int[] getRemainder(int[] x, int[] y) {
		int[] total = new int[x.length];
		int i = 0, j = 0, k = 0;
		while(i < x.length && j < y.length) {
			if(x[i] < y[j]) 
				total[k++] = x[i++];
			else if(x[i] > y[j]) 
				j++;
			else {
				i++;
				j++;
			}	
		}
		
		if(i < x.length)
			while(i < x.length) {
				total[k++] = x[i++];
			}
		
		int[] remain = new int[k];
		System.arraycopy(total, 0, remain, 0, k);
		return remain;
	}

	private int[] getSelectedIndices(ListSelectionModel model) {
		int start = model.getMinSelectionIndex();
		int stop = model.getMaxSelectionIndex();
		
		if ((start == -1) || (stop == -1)) {
			return new int[0];
		}

		int guesses[] = new int[stop - start + 1];
		int index = 0;
		for (int i = start; i <= stop; i++) {
			if (model.isSelectedIndex(i)) {
				guesses[index++] = i;
			}
		}

		int realthing[] = new int[index];
		System.arraycopy(guesses, 0, realthing, 0, index);
		return realthing;
	}
	
	private void initializeTable() {
		// main table
		mainTable.setDefaultRenderer(DSTree.class, new DSTreeOverviewRenderer());
		mainTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		mainTable.setGridColor(Color.BLACK);
		mainTable.setShowGrid(true);
		
		mainTable.getTableHeader().setResizingAllowed(false);
		mainTable.getTableHeader().setReorderingAllowed(false);
		mainTable.setColumnSelectionAllowed(true);
		mainTable.setRowSelectionAllowed(true);
		mainTable.setCellSelectionEnabled(true);
		
		mainTable.setRowHeight(tableCellSize);
		Enumeration enumeration = mainTable.getColumnModel().getColumns(); 
	    while (enumeration.hasMoreElements()) { 
	    	TableColumn aColumn = (TableColumn)enumeration.nextElement(); 
	    	aColumn.setHeaderRenderer(new ChkBoxHeaderRenderer()); 
	        aColumn.setMaxWidth(tableCellSize * 2); 
	        aColumn.setMinWidth(tableCellSize * 2);
	    }
		
	    if(!checkListeners(mainTable.getTableHeader().getMouseListeners(), ColumnMouseListener.class)){
		    mainTable.addMouseListener(new OverviewTalbeMouseListener());
		    mainTable.getTableHeader().addMouseListener(new ColumnMouseListener());
	    }
	    
		// header column
		TableModel tm = new AbstractTableModel() {
			public int getColumnCount() {
				return 1;
			}

			public int getRowCount() {
				return mainTable.getRowCount();
			}

			public Object getValueAt(int row, int col) {
				return treeTableModel.getRowName(row);
			}
		
		};
		
		headerColumn = new JTable(tm);
		headerColumn.setShowGrid(false);
	    headerColumn.setColumnSelectionAllowed(false);
	    headerColumn.setRowSelectionAllowed(false);   
	    headerColumn.setDefaultRenderer(Object.class, new ChkBoxHeaderRenderer());
		headerColumn.setRowHeight(tableCellSize);
		TableColumn tc = headerColumn.getColumnModel().getColumn(0);
		tc.setMaxWidth(tableCellSize);
		tc.setMinWidth(tableCellSize);
		
		if(!checkListeners(headerColumn.getMouseListeners(), RowMouseListener.class))
			headerColumn.addMouseListener(new RowMouseListener());
		
		// the view
		JViewport tableview = new JViewport();
	    tableview.setView(mainTable);
	    tableview.setPreferredSize(mainTable.getPreferredSize());    
	    this.setViewport(tableview);
	    
	    JViewport jv = new JViewport();
	    jv.setView(headerColumn);
	    jv.setPreferredSize(headerColumn.getMaximumSize());
	    this.setRowHeader(jv);
	    
	    // others
	    focusedRows = treeTableModel.getFocusedIndices(true);
	    focusedColumns = treeTableModel.getFocusedIndices(false);
	    appFrame.notePanel.updateTreeNotePanel(treeTableModel.notes);
	}
	
	private void createDSTreeTableView() {
		DSTreeSelection selections = new DSTreeSelection();
		selections.rheader = treeTableModel.getHeaderByIndices(focusedRows, true);
		selections.cheader = treeTableModel.getHeaderByIndices(focusedColumns, false);
		selections.trees = treeTableModel.getValueByIndices(focusedRows, focusedColumns);
		
		if(selections != null) 
			appFrame.tableview.setDSTrees(selections.trees, selections.cheader, selections.rheader);	
	}
	
	private boolean checkListeners(MouseListener[] listeners, Class type){
		boolean isinstanceof;
		for(MouseListener lis : listeners)
			if(lis.getClass() == type)
				return true;
		
		return false;
	}
	// inner classes//////////////////////////////////////////////////////////////////////////////
	public class DSTreeOverviewRenderer extends JLabel implements TableCellRenderer {
		
		public DSTreeOverviewRenderer(){
			this.setOpaque(true);
		}
		
		public Component getTableCellRendererComponent(JTable mainTable, Object value, 
				boolean isSelected, boolean hasFocus, int row, int col) {		
			if(isSelected)
				this.setBorder(activeBorder);
			else if(treeTableModel.getFocusRow(row) && treeTableModel.getFocusColumn(col))//withinFocusRegion(row, col))
				this.setBorder(focusBorder);
			else
				this.setBorder(null);
			
			if(value == null)
				this.setBackground(Color.GRAY);
			else if(col == treeTableModel.referenceColumn) {
				this.setBackground(Color.LIGHT_GRAY);
				this.setText("1.00");
			}
			else {
				double score = ((DSTree)value).rootNode.score;
				if(score >= 0)
					this.setBackground(DSTreePanel.getScoreColor((float)score)); 
				else
					this.setBackground(Color.BLACK);
				this.setText(twoPlaces.format(score));
			}
			
			this.setToolTipText(treeTableModel.getRowName(row) + "," +treeTableModel.getColumnName(col));
			
			return this;
		}
		
		public boolean withinFocusRegion(int row, int col) {
			boolean inside = false;
			for(int i = 0; i < focusedRows.length; i++)
				if(focusedRows[i] == row) {
					inside = true;
					break;
				}
			if(!inside)
				return false;
			
			for(int i = 0; i < focusedColumns.length; i++)
				if(focusedColumns[i] == col) {
					return true;
				}
			
			return false;
		}
	}
	
	public class ChkBoxHeaderRenderer extends JCheckBox implements TableCellRenderer{   
		public ChkBoxHeaderRenderer(){
			this.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			this.setHorizontalAlignment(SwingConstants.CENTER);
		}
		
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int col) {
			
			if(table == mainTable)
				this.setSelected(treeTableModel.getFocusColumn(col));
			else if(table == headerColumn)
				this.setSelected(treeTableModel.getFocusRow(row));
			return this;
		}
	}
	
	public class OverviewTalbeMouseListener extends MouseAdapter {
		public void mousePressed(MouseEvent e) {
	        maybeShowPopup(e);
	    }
		
		public void mouseReleased(MouseEvent e) {
			if(e.getButton() == MouseEvent.BUTTON1) {
				appFrame.tableview.stopEditing();
				
				int[] rowIndices = getSelectedIndices(mainTable.getSelectionModel());
				int[] colIndices = getSelectedIndices(mainTable.getColumnModel().getSelectionModel());
				
				if(rowIndices.length == 1 && colIndices.length == 1) {
					int r = rowIndices[0];
					int c = colIndices[0];
					
					appFrame.infoPanel.setTreeInfo(treeTableModel.getRowName(r), 
							treeTableModel.getColumnName(c), (DSTree)treeTableModel.getValueAt(r, c));
					appFrame.notePanel.updateNodeNodePanel((DSTree)treeTableModel.getValueAt(r, c));
				}
				else
					appFrame.infoPanel.setTreeInfo(treeTableModel.getHeaderByIndices(rowIndices, true), 
						treeTableModel.getHeaderByIndices(colIndices, false));
				
				appFrame.notePanel.highlightTreeNotePanel(treeTableModel.getValueByIndices(rowIndices, colIndices));
				
				appFrame.autoLogger.logAction("tree selection operation,selection");
			}
			else
				maybeShowPopup(e);
		}
		
		private void maybeShowPopup(MouseEvent e) {
	        if (e.isPopupTrigger()) {
	            popupMenu.show(e.getComponent(), e.getX(), e.getY());
	        }
	    }
	}
	
	public class ColumnMouseListener extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			TableColumnModel colModel = mainTable.getColumnModel();
			int col = colModel.getColumnIndexAtX(e.getX());
			if (col == -1)
				return;
			treeTableModel.setFocusColumn(col, !treeTableModel.getFocusColumn(col));
			focusedColumns = treeTableModel.getFocusedIndices(false);
			createDSTreeTableView();
			mainTable.getTableHeader().repaint();
			mainTable.repaint();
			
			appFrame.autoLogger.logAction("tree selection operation,column");
		}
	}

	public class RowMouseListener extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			int row = headerColumn.rowAtPoint(e.getPoint());
			if (row == -1)
				return;
			treeTableModel.setFocusRow(row, !treeTableModel.getFocusRow(row));
			focusedRows = treeTableModel.getFocusedIndices(true);
			createDSTreeTableView();
			headerColumn.repaint();
			mainTable.repaint();
			
			appFrame.autoLogger.logAction("tree selection operation,row");
		}
	}

	public static class DSTreeSelection {
		public DSTree[][] trees;
		public String[] cheader;
		public String[] rheader;
	}
}
