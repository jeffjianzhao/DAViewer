package daviewer;
import java.awt.Point;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.table.AbstractTableModel;

public class DSTreeOverviewModel extends AbstractTableModel implements Serializable {
	
	private static final long serialVersionUID = -5990229678380782723L;
	private DSTree[][] data = new DSTree[0][0];
	private String[] cheaders = new String[0];
	private String[] rheaders = new String[0];
	
	private boolean[] focusedRows = new boolean[0];
	private boolean[] focusedColumns = new boolean[0];
	public ArrayList<DSTree.TreeNote> notes = new ArrayList<DSTree.TreeNote>();
	
	public int referenceColumn = -1;

	public int getColumnCount() {
		return cheaders.length;
	}

	public int getRowCount() {
		return rheaders.length;
	}

	public Class getColumnClass(int c) {
		return DSTree.class;
	}

	public String getColumnName(int c) {
		return cheaders[c];
	}

	public String getRowName(int r) {
		return rheaders[r];
	}
	
	public boolean getFocusRow(int idx){
		return focusedRows[idx];
	}
	
	public void setFocusRow(int idx, boolean value){
		focusedRows[idx] = value;
	}
	
	public boolean getFocusColumn(int idx){
		return focusedColumns[idx];
	}
	
	public void setFocusColumn(int idx, boolean value){
		focusedColumns[idx] = value;
	}

	public boolean isCellEditable(int r, int c) {
		return false;
	}

	public Object getValueAt(int r, int c) {
		return data[r][c];
	}
	
	public void getIndexByValues(HashSet<DSTree> trees, boolean[] rows, boolean[] columns) {
		
		for(int i = 0; i < rheaders.length; i++)
			for(int j = 0; j < cheaders.length; j++) {
				if((!rows[i] || !columns[j]) && trees.contains(data[i][j])) {
					rows[i] = true;
					columns[j] = true;
				}
			}
	}
	
	public void setValueAt(int r, int c, DSTree dstree) {
		data[r][c] = dstree;
	}
	
	public void appendColumn(String header) {
		String[] newheaders = new String[cheaders.length + 1];
		System.arraycopy(cheaders, 0, newheaders, 0, cheaders.length);
		newheaders[cheaders.length] = header;
		
		boolean[] newfocus = new boolean[focusedColumns.length + 1];
		System.arraycopy(focusedColumns, 0, newfocus, 0, focusedColumns.length);
		newfocus[focusedColumns.length] = false;
		
		DSTree[][] newdata = new DSTree[rheaders.length][cheaders.length + 1];
		for(int i = 0; i < rheaders.length; i++)
			System.arraycopy(data[i], 0, newdata[i], 0, cheaders.length);
			
		cheaders = newheaders;
		focusedColumns = newfocus;
		data = newdata;
		this.fireTableDataChanged();
	}
	
	public void appendRow(String header) {
		String[] newheaders = new String[rheaders.length + 1];
		System.arraycopy(rheaders, 0, newheaders, 0, rheaders.length);
		newheaders[rheaders.length] = header;
		
		boolean[] newfocus = new boolean[focusedRows.length + 1];
		System.arraycopy(focusedRows, 0, newfocus, 0, focusedRows.length);
		newfocus[focusedRows.length] = false;
		
		DSTree[][] newdata = new DSTree[rheaders.length + 1][cheaders.length];
		for(int i = 0; i < rheaders.length; i++)
			System.arraycopy(data[i], 0, newdata[i], 0, cheaders.length);
			
		rheaders = newheaders;
		focusedRows = newfocus;
		data = newdata;
		this.fireTableRowsInserted(rheaders.length - 1, rheaders.length - 1);
	}
	
	public void removeColumns(int[] idx) {
		if(cheaders.length == 0)
			return;
		
		boolean[] tag = new boolean[cheaders.length];
		boolean hasRefCol = false;
		for(int i = 0; i < cheaders.length; i++) {
			boolean contains = false;
			for(int j = 0; j < idx.length; j++)
				if(i == idx[j]) {
					contains = true;
					if(i == referenceColumn)
						hasRefCol = true;
					break;
				}
			tag[i] = contains;
		}
		
		String[] newheaders = new String[cheaders.length - idx.length];
		boolean[] newfocus = new boolean[focusedColumns.length - idx.length];
		int k = 0;
		for(int i = 0; i < cheaders.length; i++)
			if(!tag[i]) {
				newheaders[k] = cheaders[i];
				newfocus[k] = focusedColumns[i];
				k++;
			}
		
		DSTree[][] newdata = new DSTree[rheaders.length][cheaders.length - idx.length];
		for(int i = 0; i < rheaders.length; i++) {
			k = 0;
			for(int j = 0; j < cheaders.length; j++)
				if(!tag[j])
					newdata[i][k++] = data[i][j];
		}
		
		cheaders = newheaders;
		focusedColumns = newfocus;
		data = newdata;
		if(hasRefCol)
			setReferenceColumn(0);
		
		this.fireTableDataChanged();
	}
	
	public void removeRows(int[] idx) {
		if(rheaders.length == 0)
			return;
		
		boolean[] tag = new boolean[rheaders.length];
		for(int i = 0; i < rheaders.length; i++) {
			boolean contains = false;
			for(int j = 0; j < idx.length; j++)
				if(i == idx[j]) {
					contains = true;
					break;
				}
			tag[i] = contains;
		}
		
		String[] newheaders = new String[rheaders.length - idx.length];
		boolean[] newfocus = new boolean[focusedRows.length - idx.length];
		int k = 0;
		for(int i = 0; i < rheaders.length; i++)
			if(!tag[i]) {
				newheaders[k] = rheaders[i];
				newfocus[k] = focusedRows[i];
				k++;
			}
		
		DSTree[][] newdata = new DSTree[rheaders.length - idx.length][cheaders.length];
		k = 0;
		for(int i = 0; i < rheaders.length; i++) {
			if(!tag[i]) {
				for(int j = 0; j < cheaders.length; j++)
					newdata[k][j] = data[i][j];
				k++;
			}
		}
		
		rheaders = newheaders;
		focusedRows = newfocus;
		data = newdata;
		this.fireTableDataChanged();
	}
	
	public void setFocusByIndices(int[] indices, boolean isRow) {
		if (isRow) {
			for(int i = 0; i < focusedRows.length; i++)
				focusedRows[i] = false;
			for (int i = 0; i < indices.length; i++)
				focusedRows[indices[i]] = true;
		}
		else {
			for(int i = 0; i < focusedColumns.length; i++)
				focusedColumns[i] = false;
			for (int i = 0; i < indices.length; i++)
				focusedColumns[indices[i]] = true;
		}
	}
	
	public int[] getFocusedIndices(boolean isRow) {
		if (isRow) {
			int count = 0;
			for(int i = 0; i < focusedRows.length; i++)
				if(focusedRows[i])
					count++;
			
			int[] indices = new int[count];
			int j = 0;
			for(int i = 0; i < focusedRows.length; i++)
				if(focusedRows[i])
					indices[j++] = i;
			
			return indices;
		}
		else {
			int count = 0;
			for(int i = 0; i < focusedColumns.length; i++)
				if(focusedColumns[i])
					count++;
			
			int[] indices = new int[count];
			int j = 0;
			for(int i = 0; i < focusedColumns.length; i++)
				if(focusedColumns[i])
					indices[j++] = i;
			
			return indices;
		}
	}

	public String[] getHeaderByIndices(int[] indices, boolean isRow) {
		String[] headers = new String[indices.length];
		if (isRow) {
			for (int i = 0; i < indices.length; i++)
				headers[i] = rheaders[indices[i]];
		} else {
			for (int i = 0; i < indices.length; i++)
				headers[i] = cheaders[indices[i]];
		}
		return headers;
	}
/*	
	public String[] getHeaderByIndices(boolean[] indices, boolean isRow) {
		ArrayList<String> headers = new ArrayList<String>(); 
		if (isRow) {
			for (int i = 0; i < indices.length; i++)
				if(indices[i])
					headers.add(rheaders[i]);
		} else {
			for (int i = 0; i < indices.length; i++)
				if(indices[i])
					headers.add(cheaders[i]);
		}
		return headers.toArray(new String[headers.size()]);
	}
*/
	public DSTree[][] getValueByIndices(int[] rows, int[] cols) {
		DSTree[][] trees = new DSTree[rows.length][cols.length];
		for (int i = 0; i < rows.length; i++)
			for (int j = 0; j < cols.length; j++) {
				trees[i][j] = data[rows[i]][cols[j]];
			}

		return trees;
	}

	public void setReferenceColumn(int col) {
		if(col == referenceColumn)
			return;
		referenceColumn = col;
		for(int i = 0; i < rheaders.length; i++)
			for(int j = 0; j < cheaders.length; j++)
				if(data[i][j] != null && data[i][col] != null) {
					data[i][j].compareToTree(data[i][col]);
					if(j == col)
						data[i][j].isReference = true;
					else
						data[i][j].isReference = false;
				}
	}
	
	public void loadTreeSet(File f) throws Exception {
		// single tree file
		if(f.getName().endsWith("hilda.tree") || f.getName().endsWith("dis")){
			rheaders = new String[]{"row"};
			cheaders = new String[]{"column"};
			focusedRows = new boolean[rheaders.length];
			focusedColumns = new boolean[cheaders.length];
			data = new DSTree[1][1];
			DSTree tree = new DSTree();
			tree.loadFromFile(f);
			data[0][0] = tree;
			return;
		}
		
		// tree set file
		BufferedReader input = new BufferedReader(new FileReader(f));
		StringBuilder filetext = new StringBuilder();
		try{		
			String line = input.readLine();
			while(line != null){
				filetext.append(line.trim());
				line = input.readLine();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		} finally {
			input.close();
		}

		Pattern pattern = Pattern.compile("<([\\w]*)>([^<>]*)</([\\w]*)>");
		Matcher matcher = pattern.matcher(filetext);	
		
		while(matcher.find()) {
			if(matcher.group(1).equals("rows") && matcher.group(3).equals("rows")) {
				rheaders = matcher.group(2).trim().split("\\,");
				focusedRows = new boolean[rheaders.length];
			}
			else if(matcher.group(1).equals("columns") && matcher.group(3).equals("columns")){
				cheaders = matcher.group(2).trim().split("\\,");
				focusedColumns = new boolean[cheaders.length];
			}
			else if(matcher.group(1).equals("data") && matcher.group(3).equals("data")){
				if(cheaders.length == 0 || rheaders.length == 0)
					throw new Exception("No row/column defination before data.");
				
				String[] filenames = matcher.group(2).trim().split("\\,");
				String dir = f.getParent();
				data = new DSTree[rheaders.length][cheaders.length];
				for (int i = 0; i < rheaders.length; i++)
					for (int j = 0; j < cheaders.length; j++){
						DSTree dstree = new DSTree();
						dstree.loadFromFile(new File(dir, filenames[i * cheaders.length + j]));
						data[i][j] = dstree;
					}
				
				break;
			}
		}
	}
}