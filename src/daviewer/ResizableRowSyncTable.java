package daviewer;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.*;


public class ResizableRowSyncTable extends JTable {
	protected MouseInputAdapter rowResizer, columnResizer = null;
    protected JTable anotherTable;
    
	public ResizableRowSyncTable(TableModel dm) {
		super(dm);
	}
	
	public ResizableRowSyncTable(TableModel dm, TableColumnModel cm) {
		super(dm, cm);
	}

	public void setSyncTable(JTable table) {
		anotherTable = table;
	}
	
	// turn resizing on/of
	public void setResizable(boolean row, boolean column) {
		if (row) {
			if (rowResizer == null) 
					rowResizer = new SyncTableRowResizer(this);
			
		} else if (rowResizer != null) {
			removeMouseListener(rowResizer);
			removeMouseMotionListener(rowResizer);
			rowResizer = null;
		}
		if (column) {
			if (columnResizer == null) 
				columnResizer = new TableColumnResizer(this);				
			
		} else if (columnResizer != null) {
			removeMouseListener(columnResizer);
			removeMouseMotionListener(columnResizer);
			columnResizer = null;
		}
	}

	// mouse press intended for resize shouldn't change row/col/cellcelection
	public void changeSelection(int row, int column, boolean toggle,
			boolean extend) {
		if (getCursor() == TableColumnResizer.resizeCursor || getCursor() == TableRowResizer.resizeCursor)
			return;
		super.changeSelection(row, column, toggle, extend);
	}
	
	public class SyncTableRowResizer extends TableRowResizer {

		public SyncTableRowResizer(JTable table) {
			super(table);	
		}
		
		public void mouseDragged(MouseEvent e){ 
			super.mouseDragged(e);
			if(ResizableRowSyncTable.this.anotherTable != null) {
				int row = this.getResizingRow();
				if(row >= 0)
					ResizableRowSyncTable.this.anotherTable.setRowHeight(row, this.getTable().getRowHeight(row));
			}
		}
		
	}
}
