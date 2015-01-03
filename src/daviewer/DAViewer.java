package daviewer;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.border.*;


public final class DAViewer extends JFrame {
	
	private JMenuBar menuBar = new JMenuBar();
	private MenuActionListener listener = new MenuActionListener();
	private String[] fileMenuName = {"Open Workspace", "Save Workspace", "-", "Open TreeSet", "Import Tree(s)", "-", "Exit"};
	private String[] dataMenuName = {"Set Refrence Column", "-", "Append Column", "Remove Columns", "Append Row", "Remove Rows"};
	private String[] viewMenuName = {"View Selected Trees", "Add Selected Rows/Columns", "Remove Selected Rows/Columns", "-",
			"Node-Link View", "Icicle-Plot View", "-", "Hybrid Text View", "Continuous Text View", "Seperated Text View"};
	private String[] noteMenuName = {"Add Note to Selected Trees", "Add Note to Selected Nodes", "-", "Remove Selected Note"};
	
	public DSTreeTableView tableview = new DSTreeTableView();
	public DSTreeOverview overview = new DSTreeOverview();
	public DSTreeArticlePane textPane = new DSTreeArticlePane();
	public DSTreeInfoPanel infoPanel = new DSTreeInfoPanel();
	public DSTreeNotePanel notePanel = new DSTreeNotePanel();
	public DSTreeSearchPanel searchPanel = new DSTreeSearchPanel();
	public AutoLogger autoLogger = new AutoLogger(false, false);

	//constructors//////////////////////////////////////////////////////////////////////////////
	public DAViewer() {
		super("DAViewer");
		
		createMenu();
		this.setJMenuBar(menuBar);
		
		// is this the best way?
		DSTreePanel.appFrame = this;
		DSTreeTableView.appFrame = this;
		DSTreeOverview.appFrame = this;
		DSTreeArticlePane.appFrame = this;
		DSTreeNotePanel.appFrame = this;
		DSTreeSearchPanel.appFrame = this;
		DSTreeInfoPanel.appFrame = this;
		autoLogger.appFrame = this;
		overview.createContextMenu();
		textPane.createContextMenu();
		autoLogger.start();
		
		textPane.setPreferredSize(new Dimension(280, 500));
		notePanel.setPreferredSize(new Dimension(280, 220));
		//overview.setPreferredSize(new Dimension(700, 150));
		overview.setPreferredSize(new Dimension(200, 800));
		tableview.setPreferredSize(new Dimension(700, 800));
		infoPanel.setPreferredSize(new Dimension(1264, 80));		
		
		JScrollPane spane = new JScrollPane(textPane);
		spane.setBorder(BorderFactory.createTitledBorder("Article View"));
		notePanel.setBorder(BorderFactory.createTitledBorder("Notes"));
		JSplitPane jsp1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, notePanel, spane);		
		//jsp1.setDividerLocation(0.3);
		jsp1.setResizeWeight(0.3);
		jsp1.setOneTouchExpandable(true);
		
		tableview.setBorder(BorderFactory.createTitledBorder("Tree Detail View"));
		overview.setBorder(BorderFactory.createTitledBorder("TreeSet Overview"));	
		//JSplitPane jsp2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, overview, tableview);
		JSplitPane jsp2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, overview, tableview);
		//jsp2.setDividerLocation(0.2);
		jsp2.setResizeWeight(0.2);
		jsp2.setOneTouchExpandable(true);
		
		JSplitPane mainjsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jsp2, jsp1);
		//mainjsp.setDividerLocation(0.8);
		mainjsp.setResizeWeight(0.8);
		mainjsp.setOneTouchExpandable(true);
		
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(mainjsp, BorderLayout.CENTER);
		this.getContentPane().add(infoPanel, BorderLayout.SOUTH);
		ToolTipManager.sharedInstance().setInitialDelay(300);
	}
	
	//private methods//////////////////////////////////////////////////////////////////////////////
	private void createMenuItems(JMenu menu, String[] names) {
		for(String name : names) {
			if(name.equals("-"))
				menu.add(new JSeparator());
			else {
				JMenuItem item = new JMenuItem(name);
				menu.add(item);
				item.addActionListener(listener);
			}
		}
		menuBar.add(menu);
	}
	
	private void createMenu() {
		menuBar.setBorder(new BevelBorder(BevelBorder.RAISED));
		
		createMenuItems(new JMenu("File"), fileMenuName);
		createMenuItems(new JMenu("Data"), dataMenuName);
		createMenuItems(new JMenu("View"), viewMenuName);
		createMenuItems(new JMenu("Note"), noteMenuName);
	}
	
	//inner classes//////////////////////////////////////////////////////////////////////////////
	public class MenuActionListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			String event = e.getActionCommand();
			//file menu
			if(event.equals("Open TreeSet")){
				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(new File("."));
				int option = chooser.showOpenDialog(DAViewer.this);
				if(option == JFileChooser.APPROVE_OPTION){					
					try {
						overview.loadTreeSet(chooser.getSelectedFile(), true);
					} catch (Exception e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(DAViewer.this, e1);
					}	
					
					autoLogger.logAction("dataset operation,opentxt");
				}	
			}
			else if(event.equals("Import Tree(s)")) {
				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(new File("."));
				int option = chooser.showOpenDialog(DAViewer.this);
				if(option == JFileChooser.APPROVE_OPTION){					
					try {
						overview.loadTreeSet(chooser.getSelectedFile(), false);
					} catch (Exception e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(DAViewer.this, e1);
					}			
					
					autoLogger.logAction("dataset operation,import");
				}
			}
			else if(event.equals("Save Workspace")) {
				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(new File("."));
				chooser.setSelectedFile(new File("*.tree.wksp"));
				int option = chooser.showSaveDialog(DAViewer.this);
				if(option == JFileChooser.APPROVE_OPTION){			
					try {
						overview.saveTreeTableModel(chooser.getSelectedFile());
					} catch (Exception e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(DAViewer.this, e1);
					}	
					
					autoLogger.logAction("dataset operation,save");
				}
			}
			else if(event.equals("Open Workspace")) {
				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(new File("."));
				int option = chooser.showOpenDialog(DAViewer.this);
				if(option == JFileChooser.APPROVE_OPTION){					
					try {
						overview.openTreeTableModel(chooser.getSelectedFile());
					} catch (Exception e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(DAViewer.this, e1);
					}		
					
					autoLogger.logAction("dataset operation,open");
				}
			}
			else if(event.equals("Exit")) {
				int result = JOptionPane.showConfirmDialog(DAViewer.this, "Do you really want to quit? Unsaved data will be lost.", 
						"DAViewer", JOptionPane.OK_CANCEL_OPTION);
				if(result == JOptionPane.OK_OPTION) {
					autoLogger.stop();
					System.exit(0);
				}
			}
			// data menu
			else if(event.equals("Append Column")) {
				String name = JOptionPane.showInputDialog(DAViewer.this, "Please enter the column name:", "new column");
				overview.appendTable(name, false);
				
				autoLogger.logAction("dataset operation,append column");
			}
			else if(event.equals("Append Row")) {
				String name = JOptionPane.showInputDialog(DAViewer.this, "Please enter the row name:", "new row");
				overview.appendTable(name, true);
				
				autoLogger.logAction("dataset operation,append row");
			}
			else if(event.equals("Remove Rows")) {
				overview.removeTable(true);
				
				autoLogger.logAction("dataset operation,remove row");
			}
			else if(event.equals("Remove Columns")) {
				overview.removeTable(false);
				
				autoLogger.logAction("dataset operation,remove column");
			}
			else if(event.equals("Set Refrence Column")) {
				overview.setReferenceColumn();
				
				autoLogger.logAction("tree viewing operation,set reference column");
			}
			// view menu
			else if(event.equals("View Selected Trees")) {
				DSTreeOverview.DSTreeSelection selections = overview.getNewFocusedSelection(DSTreeOverview.New_Focus);
				if(selections != null) {
					tableview.setDSTrees(selections.trees, selections.cheader, selections.rheader);
				}
				
				autoLogger.logAction("tree viewing operation,view");
			}
			else if(event.equals("Add Selected Rows/Columns")) {
				DSTreeOverview.DSTreeSelection selections = overview.getNewFocusedSelection(DSTreeOverview.Add_Focus);
				if(selections != null) 
					tableview.setDSTrees(selections.trees, selections.cheader, selections.rheader);	
				
				autoLogger.logAction("tree viewing operation,add");
			}
			else if(event.equals("Remove Selected Rows/Columns")) {
				DSTreeOverview.DSTreeSelection selections = overview.getNewFocusedSelection(DSTreeOverview.Del_Focus);
				if(selections != null) 
					tableview.setDSTrees(selections.trees, selections.cheader, selections.rheader);	
				
				autoLogger.logAction("tree viewing operation,remove");
			}
			else if(event.equals("Continuous Text View")) {
				textPane.setViewMode(DSTreeArticlePane.DisplayMode.Continuous);
				
				autoLogger.logAction("text viewing operation,continuous");
			}
			else if(event.equals("Seperated Text View")) {
				textPane.setViewMode(DSTreeArticlePane.DisplayMode.Seperated);		
				
				autoLogger.logAction("text viewing operation,seperated");
			}
			else if(event.equals("Hybrid Text View")) {
				textPane.setViewMode(DSTreeArticlePane.DisplayMode.Hybrid);
				
				autoLogger.logAction("text viewing operation,hybrid");
			}
			else if(event.equals("Node-Link View")) {
				tableview.setDisplayType(DSTreePanel.DisplayType.NodeLink);
				
				autoLogger.logAction("tree exploration operation,nodelink view");
			}
			else if(event.equals("Icicle-Plot View")){
				tableview.setDisplayType(DSTreePanel.DisplayType.Compact);
				
				autoLogger.logAction("tree exploration operation,compact view");
			}
			// note menu
			else if(event.equals("Add Note to Selected Trees")) {
				overview.addTreeNote();
				
				autoLogger.logAction("notes operation,tree note");
			}
			else if(event.equals("Add Note to Selected Nodes")) {
				tableview.addNodeNote();
				
				autoLogger.logAction("notes operation,node note");
			}
			else if(event.equals("Remove Selected Note")) {
				notePanel.removeNote();
				
				autoLogger.logAction("notes operation,remove");
			}
		}
	
	}

	//main method//////////////////////////////////////////////////////////////////////////////
	public static void main(String[] args) {
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		
		final DAViewer f = new DAViewer();
		f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				int result = JOptionPane.showConfirmDialog(f, "Do you really want to quit? Unsaved data will be lost.", 
						"DAViewer", JOptionPane.OK_CANCEL_OPTION);
				if(result == JOptionPane.OK_OPTION) {
					f.autoLogger.stop();
					System.exit(0);
				}
			}
		});
		f.setSize(1280, 960);
		f.setLocation(dim.width/2 - 640, dim.height/2 - 400);		
		//f.pack();
		f.setVisible(true);
		
		JFrame searchWindow = new JFrame("Search Window");
		searchWindow.setContentPane(f.searchPanel);
		searchWindow.setLocation(dim.width/2 + 500, dim.height/2 - 400);
		searchWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		searchWindow.pack();
		searchWindow.setAlwaysOnTop(true);
		searchWindow.setVisible(true);
	}

}
