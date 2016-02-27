package hc.server.util;

import hc.App;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;
import hc.res.ImageSrc;
import hc.server.DisposeListener;
import hc.server.HCActionListener;
import hc.server.SingleJFrame;
import hc.server.ui.ClientDesc;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

public class ExceptionViewer {
	private static final ArrayDeque<Object[]> array = new ArrayDeque<Object[]>(32);
	private static final Calendar calendar = Calendar.getInstance();
	private static boolean isPopup = false;
	
	public static final void notifyPopup(boolean p){
		CCoreUtil.checkAccess();
		
		isPopup = p;
	}
	
	public static final Vector<String> exception = new Vector<String>();
	public static final Vector<StackTraceElement[]> stacks = new Vector<StackTraceElement[]>();
	private static ExceptionViewer msbViewer;
	
	private JFrame dialog;
	private final JButton clearBtn = new JButton((String)ResourceUtil.get(8005), new ImageIcon(ImageSrc.REMOVE_SMALL_ICON));
	private int currRow;
	private final JTable tableException, tableStacks;
	private final AbstractTableModel modelException, modelStacks;
	
	final Runnable refreshTable = new Runnable() {
		@Override
		public void run() {
			tableException.updateUI();
		}
	};
	
	
	private final void reset(){
		synchronized (exception) {
			exception.clear();
			stacks.clear();
			currRow = 0;
			
			tableException.updateUI();
			tableStacks.updateUI();
		}
	}

	public ExceptionViewer(){
		
		clearBtn.addActionListener(new HCActionListener(){
			public void actionPerformed(ActionEvent e) {
				reset();
			}
		});
		
		modelException = new AbstractTableModel() {
			@Override
			public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
				return;
			}
			
			@Override
			public void removeTableModelListener(TableModelListener l) {
			}
			
			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}
			
			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				final String rowValue = exception.elementAt(rowIndex);
				return rowValue==null?"":rowValue;
			}
			
			@Override
			public int getRowCount() {
				return exception.size();
			}
			
			@Override
			public String getColumnName(int columnIndex) {
				return "Exception Descrition";
			}
			
			@Override
			public int getColumnCount() {
				return 1;
			}
			
			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return String.class;
			}
			
			@Override
			public void addTableModelListener(TableModelListener l) {
			}
		};
		
		modelStacks = new AbstractTableModel() {
			@Override
			public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
				return;
			}
			
			@Override
			public void removeTableModelListener(TableModelListener l) {
			}
			
			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}
			
			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				if(rowIndex >= getRowCount()){
					return "";
				}
				final StackTraceElement[] value = stacks.elementAt(currRow);
				if(value == null){
					return "";
				}else{
					if(rowIndex < value.length){
						return value[rowIndex];
					}else{
						return "";
					}
				}
			}
			
			@Override
			public int getRowCount() {
				if(currRow < stacks.size()){
					return stacks.elementAt(currRow).length;
				}
				return 0;
			}
			
			@Override
			public String getColumnName(int columnIndex) {
				return "StackTraceElement";
			}
			
			@Override
			public int getColumnCount() {
				return 1;
			}
			
			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return String.class;
			}
			
			@Override
			public void addTableModelListener(TableModelListener l) {
			}
		};
		
		
		tableException = new JTable(modelException);
		tableStacks = new JTable(modelStacks);
		
//		{
//			HCHeaderRenderer rend = new HCHeaderRenderer(tableException.getTableHeader().getDefaultRenderer());
//			tableException.getTableHeader().setDefaultRenderer(rend);
//		}
//		{
//			HCHeaderRenderer rend = new HCHeaderRenderer(tableStacks.getTableHeader().getDefaultRenderer());
//			tableStacks.getTableHeader().setDefaultRenderer(rend);
//		}
		
		tableException.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting() == false){
					final ListSelectionModel lsm = (ListSelectionModel)e.getSource();
			        if (lsm.isSelectionEmpty()) {
			        	return;
			        } else {
			            int minIndex = lsm.getMinSelectionIndex();
			            int maxIndex = lsm.getMaxSelectionIndex();
			            for (int i = minIndex; i <= maxIndex; i++) {
			                if (lsm.isSelectedIndex(i)) {
			                	currRow = i;
			                	tableStacks.updateUI();
			                	return;
			                }
			            }
			        }
				}
			}
		});
	    
	}
	
	private final void show(){
		if(exception.size() == 0){
			return;
		}
		
		if(dialog != null){
			updateUI();
			return;
		}
		
		JPanel panel = new JPanel(new GridBagLayout());
		Insets insets = new Insets(ClientDesc.hgap, ClientDesc.hgap, 0, ClientDesc.vgap);
		{
			GridBagConstraints c = new GridBagConstraints();
			c.insets = insets;
			c.gridy = 0;
			c.anchor = GridBagConstraints.CENTER;
			c.fill = GridBagConstraints.BOTH;
			c.weighty = 0.7;
			c.weightx = 1.0;
			
			tableException.setRowSelectionAllowed(true);
//			panel.add(tableException, c);
			final JScrollPane scrollPane = new JScrollPane(tableException);
			scrollPane.setPreferredSize(new Dimension(500, 200));
			panel.add(scrollPane, c);
		}
		
		{
			GridBagConstraints c = new GridBagConstraints();
			c.insets = insets;
			c.gridy = 1;
			c.anchor = GridBagConstraints.CENTER;
			c.fill = GridBagConstraints.BOTH;
			c.weighty = 0.3;
			c.weightx = 1.0;
			
			tableStacks.setRowSelectionAllowed(true);
			final JScrollPane scrollPane = new JScrollPane(tableStacks);
			scrollPane.setPreferredSize(new Dimension(500, 150));
			panel.add(scrollPane, c);
		}
		
		tableException.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		tableStacks.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

		JPanel total = new JPanel(new BorderLayout(0, 0));
		{
			JToolBar toolbar = new JToolBar(JToolBar.HORIZONTAL);
			toolbar.add(clearBtn);
//			toolbar.setRequestFocusEnabled(false);
			
			total.add(toolbar, BorderLayout.NORTH);
			total.add(panel, BorderLayout.CENTER);
		}
		
		final ActionListener listener = null;
		JButton closeBtn = App.buildDefaultCloseButton();
		dialog = (JFrame)App.showCenterPanel(total, 0, 0, "Exception List", false, closeBtn, null, 
				listener, null, null, false, true, null, true, false);
		App.setDisposeListener(dialog, new DisposeListener() {
			@Override
			public void dispose() {
				dialog = null;
				SingleJFrame.removeJFrame(ExceptionViewer.class.getName());
			}
		});
		SingleJFrame.addJFrame(ExceptionViewer.class.getName(), dialog);
	}
	
	void updateUI(){
		if(dialog != null){
			App.invokeLaterUI(refreshTable);
		}
	}

	private static final Thread daemon = new Thread("ExceptionViewWriter"){
		public void run(){
			Object[] para;
			while(true){
				synchronized (array) {
					para = array.pollFirst();
					if(para == null){
						try {
							array.wait();
						} catch (Exception e) {
							e.printStackTrace();
						}
						continue;
					}
				}
				
				pushIn((String)para[0], (StackTraceElement[])para[1]);
			}
		}
		
		public void pushIn(String paraMessage, StackTraceElement[] ste) {
			StringBuilder sb = new StringBuilder(13);
			
			calendar.setTimeInMillis(System.currentTimeMillis());
			sb.append(calendar.get(Calendar.HOUR_OF_DAY));
			sb.append(":");
			sb.append(calendar.get(Calendar.MINUTE));
			sb.append(":");
			sb.append(calendar.get(Calendar.SECOND));
			sb.append(".");
			sb.append(calendar.get(Calendar.MILLISECOND));
			sb.append(" ");
			
			String tmp = sb.toString() + paraMessage;
			
			synchronized (exception) {
				exception.add(tmp);
				stacks.add(ste);
			}
			
			if(msbViewer == null){
				msbViewer = new ExceptionViewer();
			}

			msbViewer.show();
		}
	};

	public static void pushIn(final String tmpMsg) {
		if(isPopup){
			Object[] para = {tmpMsg, Thread.currentThread().getStackTrace()};
			synchronized (array) {
				array.addLast(para);
				array.notify();
			}
		}else{
			if(LogManager.INI_DEBUG_ON){
				System.err.println("Exception : " + tmpMsg);
				StackTraceElement[] ste = Thread.currentThread().getStackTrace();
				final int size = ste.length;
				for (int i = 0; i < size; i++) {
					System.err.print("\tat : " + ste[i] + "\n");
				}
			}
		}
	}
	
	public static void init() {
		CCoreUtil.checkAccess();
		
		daemon.setPriority(ThreadPriorityManager.LOWEST_PRIORITY);
		daemon.setDaemon(true);
		daemon.start();
	}
}