package daviewer;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;


public class ResizableHeaderTable extends JScrollPane {
	
	protected ResizableRowSyncTable mainTable, headerColumn;
	private static Color headerColor = new Color(192, 192, 192);
	
	// constructors//////////////////////////////////////////////////////////////////////////////
	public ResizableHeaderTable() {}
	
	public ResizableHeaderTable(TableModel tm) {
		setTableModel(tm);
	}
	
	// public methods//////////////////////////////////////////////////////////////////////////////
	public void setTableModel(TableModel tm) {
		// create models and renderer
		TableColumnModel mainColumnModel = new DefaultTableColumnModel() {
			boolean first = true;
			public void addColumn(TableColumn tc) {
				if (first) {
					first = false;
					return;
				}
				super.addColumn(tc);
			}
		};
		
		TableColumnModel rowHeaderModel = new DefaultTableColumnModel() {
			boolean first = true;
			public void addColumn(TableColumn tc) {
				if (first) {
					tc.setMinWidth(75);
					tc.setMaxWidth(75);
					super.addColumn(tc);
					first = false;
				}
			}
		};
		
		TableCellRenderer rowHeaderRenderer = new DefaultTableCellRenderer (){
	    	public Component getTableCellRendererComponent( JTable table, Object value,
	                boolean isSelected, boolean hasFocus, int row, int column) {
	    		Component comp = super.getTableCellRendererComponent(table, value,
	                    isSelected, hasFocus, row, column);
	    		comp.setBackground(headerColor);
	    		((JComponent)comp).setBorder(BorderFactory.createRaisedBevelBorder());
	    		return comp;
	    	}
		};
		
		// create tables
		mainTable = new ResizableRowSyncTable(tm, mainColumnModel);				
		headerColumn = new ResizableRowSyncTable(tm, rowHeaderModel);
		
		// set header column
		headerColumn.setSyncTable(mainTable);
		headerColumn.setResizable(true, false);
		
		headerColumn.setRowSelectionAllowed(false);
	    headerColumn.setColumnSelectionAllowed(false);
	    headerColumn.setCellSelectionEnabled(false);
	    headerColumn.setAutoscrolls(false);
	    
	    headerColumn.createDefaultColumnsFromModel();
	    headerColumn.setDefaultRenderer(String.class, rowHeaderRenderer);	    
	    
	    // set main table
	    mainTable.setSyncTable(headerColumn);
	    mainTable.setResizable(true, true);
	    
	    mainTable.setShowGrid(true);
	    mainTable.setGridColor(Color.GRAY);
	    mainTable.setColumnSelectionAllowed(false);
	    mainTable.setRowSelectionAllowed(false);
	    mainTable.setCellSelectionEnabled(false);
	    mainTable.setAutoscrolls(false);
	    mainTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	    
	    mainTable.createDefaultColumnsFromModel();
	    mainTable.getTableHeader().setDefaultRenderer(rowHeaderRenderer);
	    mainTable.setSelectionModel(headerColumn.getSelectionModel()); 
	    
	    //create views	    	    
	    JViewport jv = new JViewport();
	    jv.setView(headerColumn);
	    jv.setPreferredSize(headerColumn.getMinimumSize());
	    
	    JViewport tableview = new JViewport();
	    tableview.setView(mainTable);
	    tableview.setPreferredSize(mainTable.getPreferredSize());
	    
	    this.setViewport(tableview);
	    this.setRowHeader(jv);
		this.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, headerColumn.getTableHeader());
	}
	
	public void clearTable() {
		mainTable = headerColumn = null;
		this.setViewport(null);
		this.setRowHeader(null);
		this.setColumnHeader(null);
		this.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, null);
		repaint();
	}
	
	public int getColumnPreferredWidth(int col) {
		int colWidth = 0;
		for (int row = 0; row < mainTable.getRowCount(); row++) {
			Component comp = mainTable.prepareRenderer(mainTable.getCellRenderer(row, col), row, col);
			colWidth = Math.max(colWidth, comp.getPreferredSize().width);
		}
		
		return colWidth;
	}
	
	public int getRowPreferredHeight(int row) {
		int rowHeight = 0;
		for (int column = 0; column < mainTable.getColumnCount(); column++) {
			Component comp = mainTable.prepareRenderer(mainTable.getCellRenderer(row, column), row, column);
			rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
		}
		
		return rowHeight;
	}
	
	public void setResizalbe(boolean row, boolean column) {
		mainTable.setResizable(row, column);
	}
	
	public void setCellEditor(Class type,TableCellEditor editor) {
		mainTable.setDefaultEditor(type, editor);
	}
	
	public void setCellRender(Class type,TableCellRenderer renderer) {
		mainTable.setDefaultRenderer(type, renderer);
	}
	
	public ResizableRowSyncTable getMainTable() {
		return mainTable;
	}
	
	public ResizableRowSyncTable getHeaderColumn() {
		return headerColumn;
	}
	
	public void setRowHeight(int row, int rowHeight) {
		mainTable.setRowHeight(row, rowHeight);
		headerColumn.setRowHeight(row, rowHeight);
	}
	
	public void setColumnWidth(int col, int colWidth) {
		TableColumn tc = mainTable.getColumnModel().getColumn(col);
		tc.setPreferredWidth(colWidth);
	}
	
	public void initTableCellSize() {
		for (int row = 0; row < mainTable.getRowCount(); row++)
			setRowHeight(row, getRowPreferredHeight(row));
		for (int col = 0; col < mainTable.getColumnCount(); col++) 
			setColumnWidth(col, getColumnPreferredWidth(col));
		
	}

}
