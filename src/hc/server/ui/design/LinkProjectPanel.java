package hc.server.ui.design;

import hc.App;
import hc.UIActionListener;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.L;
import hc.core.cache.CacheManager;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.res.ImageSrc;
import hc.server.AbstractDelayBiz;
import hc.server.FileSelector;
import hc.server.HCActionListener;
import hc.server.HCTablePanel;
import hc.server.LinkMenuManager;
import hc.server.ProcessingWindowManager;
import hc.server.ui.ClientDesc;
import hc.server.ui.LinkProjectStatus;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.hpj.HCjad;
import hc.server.ui.design.hpj.HCjar;
import hc.server.util.ContextSecurityConfig;
import hc.server.util.DelDeployedProjManager;
import hc.server.util.SignHelper;
import hc.server.util.ai.AIPersistentManager;
import hc.util.HttpUtil;
import hc.util.IBiz;
import hc.util.LinkPropertiesOption;
import hc.util.PropertiesManager;
import hc.util.PropertiesSet;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class LinkProjectPanel extends ProjectListPanel{
	final ThreadGroup threadPoolToken = App.getThreadPoolToken();
	final String saveAndApply = (String) ResourceUtil.get(1017) + " + " + (String) ResourceUtil.get(9041);

	final HCTablePanel tablePanel;
	final JButton designBut, rebindBut, importBut, editBut, removeBut, upBut, downBut;
	final Vector<LinkEditData> delList = new Vector<LinkEditData>(0);
	boolean isChanged = false;
	boolean isCancelOp = false;
	public static String ACTIVE = (String)ResourceUtil.get(8020);

	final JRadioButton rb_startup = new JRadioButton(LinkPropertiesOption.getDispOpNextStartUp());
	final JRadioButton rb_ask = new JRadioButton(LinkPropertiesOption.getDispOpAsk());
	final JRadioButton rb_imme = new JRadioButton(LinkPropertiesOption.getDispOpImmediate());
	final JRadioButton rb_perm_no = new JRadioButton(LinkPropertiesOption.getDispOpPermNoChange());
	final JRadioButton rb_perm_acceptIfSigned = new JRadioButton(LinkPropertiesOption.getDispOpPermAcceptIfSigned());

	final JCheckBox ch_autoUpgrade = new JCheckBox((String)ResourceUtil.get(9138));
	
	public static final String getNewLinkedInProjOp(){
		final String op = PropertiesManager.getValue(PropertiesManager.p_OpNewLinkedInProjVer, LinkPropertiesOption.OP_NEXT_START_UP);
		if(op.equals(LinkPropertiesOption.OP_NEXT_START_UP) || op.equals(LinkPropertiesOption.OP_ASK) || op.equals(LinkPropertiesOption.OP_IMMEDIATE)){
			return op;
		}else{
			return LinkPropertiesOption.OP_NEXT_START_UP;
		}
	}
	
	public static final String getAcceptNewPermissionsOp(){
		final String op = PropertiesManager.getValue(PropertiesManager.p_OpAcceptNewPermissions, LinkPropertiesOption.OP_PERM_NO_CHANGE);
		if(op.equals(LinkPropertiesOption.OP_PERM_NO_CHANGE) || op.equals(LinkPropertiesOption.OP_PERM_ACCEPT_IF_SIGNED)){
			return op;
		}else{
			return LinkPropertiesOption.OP_PERM_NO_CHANGE;
		}
	}
	
	private void transRootToOtherActive(){
		final int length = data.size();
		for (int i = 0; i < length; i++) {
			final LinkEditData led = (LinkEditData)data.elementAt(i)[IDX_OBJ_STORE];
			if(led != null){
				if(led.lps.isActive()){
					led.lps.setRoot(true);
					led.op = LinkProjectManager.STATUS_MODIFIED;
					return;
				}
			}
		}
	}
	
	private LinkProjectStore searchRoot(){
		final int size = data.size();
		for (int i = 0; i < size; i++) {
			final LinkEditData led = (LinkEditData)data.elementAt(i)[IDX_OBJ_STORE];
			if(led != null){
				if(led.lps.isRoot()){
					return led.lps;
				}
			}else{
				return null;
			}
		}
		return null;
	}
	final JPanel contentPane;
	final JButton saveAndApplyBtn;
	boolean isOpenApply = false;
	boolean isNotifyModi = false;

	final JButton exitBtn;
	final UIActionListener saveAction;
	final UIActionListener exitAction;
	final Window dialog;
	final Component relativeTo;
	final JLabel compSelectMode, compPermssionsMode;
	
	public LinkProjectPanel(final JFrame owner, final boolean newFrame, final Component relativeTo) {
		super();
		final String title = (String)ResourceUtil.get(9059);
		dialog = App.buildCloseableWindow(newFrame, owner, title, true);
		final JFrame self = (owner==null && (dialog instanceof JFrame))?(JFrame)dialog:owner;
		this.relativeTo = relativeTo;
		
		final String threeSpace = "   ";
		compSelectMode = new JLabel((String)ResourceUtil.get(9149) + threeSpace);
		compPermssionsMode = new JLabel((String)ResourceUtil.get(9232) + threeSpace);

		contentPane = new JPanel();
		
		contentPane.setLayout(new BorderLayout(ClientDesc.hgap, ClientDesc.vgap));
		
		ImageIcon designIco = null;
		try {
			designIco = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/designer_22.png")));
		} catch (final IOException e) {
			e.printStackTrace();
		}    					

		designBut = new JButton((String)ResourceUtil.get(9014), designIco);
		final String designTip = StringUtil.replace((String)ResourceUtil.get(9140), "{apply}", saveAndApply);
		designBut.setToolTipText("<html>" + designTip + "</html>");
		rebindBut = new JButton((String)ResourceUtil.get(9139), Designer.loadImg("device_24.png"));
		final BindButtonRefresher bindBtnRefresher = new BindButtonRefresher() {
			@Override
			public void disableManageButtons() {
				rebindBut.setEnabled(false);
			}
			
			@Override
			public void checkHARButtonsEnable() {
				Designer.checkBindEnable(rebindBut);
			}
		};
		Designer.buildRebindButton(rebindBut, threadPoolToken, bindBtnRefresher, self);
		
		upBut = new JButton((String)ResourceUtil.get(9019), new ImageIcon(ImageSrc.UP_SMALL_ICON));
		downBut = new JButton((String)ResourceUtil.get(9020), new ImageIcon(ImageSrc.DOWN_SMALL_ICON));
		removeBut = new JButton((String)ResourceUtil.get(9018), new ImageIcon(ImageSrc.REMOVE_SMALL_ICON));
		removeBut.setToolTipText("<html>" + (String)ResourceUtil.get(9141) + "</html>");
		importBut = new JButton((String)ResourceUtil.get(9016) + "▼", new ImageIcon(ImageSrc.ADD_SMALL_ICON));
		importBut.setToolTipText("<html>" + (String)ResourceUtil.get(9142)  + "</html>");
		importBut.requestFocus();
		{
			String editBtnText = (String)ResourceUtil.get(9017);
			if(ResourceUtil.isJ2SELimitFunction()){
				editBtnText += " | " + (String)ResourceUtil.get(9094);
			}
			editBut = new JButton(editBtnText , new ImageIcon(ImageSrc.MODIFY_SMALL_ICON));
		}
		editBut.setToolTipText((String)ResourceUtil.get(9143));
		final AbstractTableModel tableModel = new AbstractTableModel() {
			@Override
			public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
				if(columnIndex == COL_NO){
					data.elementAt(rowIndex)[columnIndex] = aValue;
				} else {
					final LinkEditData led = (LinkEditData)data.elementAt(rowIndex)[IDX_OBJ_STORE];
					final LinkProjectStore lps = led.lps;
					if(lps == null){
						return;
					}
					if(columnIndex == COL_PROJ_ID){
						lps.setProjectID((String)aValue);
					}else if(columnIndex == COL_VER){
							lps.setVersion((String)aValue);
					}else if(columnIndex == COL_IS_ROOT){
						if(lps.isRoot()){
							lps.setRoot(false);
							lps.setActive(false);
							
							transRootToOtherActive();
						}else{
							final LinkProjectStore root_lps = searchRoot();
							if(root_lps != null){
								root_lps.setRoot(false);
	//							if(root_lps.getProjectID().equals(HCURL.ROOT_MENU)){
	//								root_lps.setProjectID("oldroot");
	//							}
							}
							lps.setRoot((Boolean)aValue);
							lps.setActive(true);
						}
						notifyNeedToSave();
						led.op = (LinkProjectManager.STATUS_MODIFIED);
						
						tablePanel.table.repaint();
					}else if(columnIndex == COL_PROJ_ACTIVE){
//						if(lps.isRoot()){
//							if(lps.isActive()){
//								JPanel panel = new JPanel(new BorderLayout());
//								panel.add(new JLabel("root project must be active!", App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING), BorderLayout.CENTER);
//								App.showCenterPanel(panel, 0, 0, (String)ResourceUtil.get(IContext.ERROR), false, null, null, null, null, self, true, false, null, false);
//								return;
//							}
//						}
						lps.setActive((Boolean)aValue);
						if((Boolean)aValue){
							if(searchRoot() == null){
								lps.setRoot(true);
							}
						}else{
							if(lps.isRoot()){
								lps.setRoot(false);
								//将root移交给其它工程
								transRootToOtherActive();
							}
						}
						notifyNeedToSave();
						tablePanel.table.repaint();
						led.op = (LinkProjectManager.STATUS_MODIFIED);
					}else if(columnIndex == COL_PROJ_LINK_NAME){
						lps.setLinkName((String)aValue);
					}else if(columnIndex == COL_PROJ_DESC){
						lps.setProjectRemark((String)aValue);
					}else if(columnIndex == COL_UPGRADE_URL){
						lps.setProjectUpgradeURL((String)aValue);
					}
				}
			}
			
			@Override
			public void removeTableModelListener(final TableModelListener l) {
			}
			
			@Override
			public boolean isCellEditable(final int rowIndex, final int columnIndex) {
				if(rowIndex < dataRowNum 
						&& (columnIndex == COL_PROJ_ACTIVE 
						  ||columnIndex == COL_IS_ROOT)){
					return true;
				}
				return false;
			}
			
			@Override
			public Object getValueAt(final int rowIndex, final int columnIndex) {
				if(rowIndex >= dataRowNum){
					return null;
				}

				if(columnIndex == COL_NO){
					return data.elementAt(rowIndex)[columnIndex];
				} else {
					final LinkEditData led = (LinkEditData)data.elementAt(rowIndex)[IDX_OBJ_STORE];
					final LinkProjectStore lps = led==null?null:led.lps;
					if(columnIndex == COL_IS_ROOT){
						return (lps==null)?Boolean.FALSE:(lps.isRoot());
					}else if(columnIndex == COL_PROJ_ID){
						return (lps==null)?"":lps.getProjectID();
					}else if(columnIndex == COL_VER){
						return (lps==null)?LinkProjectStore.DEFAULT_UNKOWN_VER:lps.getVersion();
					}else if(columnIndex == COL_PROJ_ACTIVE){
						return (lps==null)?Boolean.FALSE:lps.isActive();
					}else if(columnIndex == COL_PROJ_LINK_NAME){
						return (lps==null)?"":lps.getLinkName();
					}else if(columnIndex == COL_PROJ_DESC){
						return (lps==null)?"":lps.getProjectRemark();
					}else if(columnIndex == COL_UPGRADE_URL){
						return (lps==null)?"":lps.getProjectUpgradeURL();
					}else{
						return null;
					}
				}
			}
			
			@Override
			public int getRowCount() {
				return data.size();
			}
			
			@Override
			public String getColumnName(final int columnIndex) {
				return colNames[columnIndex].toString();
			}
			
			@Override
			public int getColumnCount() {
				return colNames.length;
			}
			
			@Override
			public Class<?> getColumnClass(final int columnIndex) {
				switch (columnIndex) {
                case COL_IS_ROOT:
                    return Boolean.class;
                case COL_PROJ_ACTIVE:
                    return Boolean.class;
                default:
                    return String.class;
				}
			}
			
			@Override
			public void addTableModelListener(final TableModelListener l) {
			}
		};
		
		dataRowNum = data.size();
		
		tablePanel = new HCTablePanel(tableModel, data, colNames, dataRowNum, 
				upBut, downBut, removeBut, importBut, editBut,
				//upOrDownMovingBiz
				new AbstractDelayBiz(null){
					@Override
					public final void doBiz() {
						isChanged = true;
						notifyNeedToSave();
					}},
				//Remove
				new AbstractDelayBiz(null) {
					@Override
					public final void doBiz() {
						if (ResourceUtil.isAndroidServerPlatform() && data.size() == 1 && isDeployed(0)){
							final JPanel askPanel = new JPanel();
							final String keepOne = (String)ResourceUtil.get(9258);
							askPanel.add(new JLabel(keepOne, App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEFT));
							
							final AbstractDelayBiz selfBiz = this;
							final HCActionListener listener = new HCActionListener(new Runnable() {
								@Override
								public void run() {
									final boolean[] back = {false};
									selfBiz.setPara(back);
								}
							}, threadPoolToken);
							App.showCenterPanelMain(askPanel, 0, 0, (String)ResourceUtil.get(9193), false, null, null, 
									listener, listener, self, true, false, null, false, false);

							return;
						}
						
						removeBiz();
					}
					
					final void removeBiz() {
						final AbstractDelayBiz selfBiz = this;
						final Object[] rows = (Object[])getPara();
						final LinkEditData led = (LinkEditData)rows[IDX_OBJ_STORE];
						final JPanel askPanel = new JPanel();
						final String replaceID = StringUtil.replace((String)ResourceUtil.get(9144), "{id}", led.lps.getProjectID());
						askPanel.add(new JLabel(replaceID, App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEFT));
						App.showCenterPanelMain(askPanel, 0, 0, (String)ResourceUtil.get(9145), true, null, null, new HCActionListener(new Runnable() {
							@Override
							public void run() {
								final LinkProjectStore lps = led.lps;
								
								if(lps.isRoot()){
									for (int i = 0; i < dataRowNum; i++) {
										final LinkEditData ledzero = (LinkEditData)data.elementAt(i)[IDX_OBJ_STORE];
										if(lps != ledzero.lps && ledzero.lps.isActive()){
											ledzero.lps.setRoot(true);
											break;
										}
									}
								}
								
								delProjInList(led);
								
								final boolean[] back = {true};
								selfBiz.setPara(back);
								notifyNeedToSave();
							}
						}, threadPoolToken), new ActionListener() {
							@Override
							public void actionPerformed(final ActionEvent e) {
								final boolean[] back = {false};
								selfBiz.setPara(back);
							}
						}, self, true, false, null, false, false);//不relativeTo removeBut，提示会盖住OK按钮
					}
				}, 
				//import
				new AbstractDelayBiz(null) {
					@Override
					public final void doBiz() {
						final AbstractDelayBiz selfBiz = this;
						
						final JPopupMenu pop = new JPopupMenu();
						final JMenuItem addFromLocal = new JMenuItem((String)ResourceUtil.get(9146));
						final JMenuItem addFromRemote = new JMenuItem((String)ResourceUtil.get(9147));
						
						pop.addPopupMenuListener(new PopupMenuListener() {
							@Override
							public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
							}
							
							@Override
							public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
							}
							
							@Override
							public void popupMenuCanceled(final PopupMenuEvent e) {
								selfBiz.setPara(Boolean.FALSE);
							}
						});
						
						pop.add(addFromLocal);
						pop.add(addFromRemote);
						
						addFromRemote.addActionListener(new HCActionListener(new Runnable() {
							@Override
							public void run() {
								doAddOp();
							}
							void doAddOp() {
								String url = null;
								try{
									if(PropertiesManager.isSimu() && ResourceUtil.isAndroidServerPlatform()){
										url = App.showInputDialog(self, ResourceUtil.get(9148), 
											"http://192.168.1.102:8080/download/test/sample_test_2_0_signed.har");
									}else{
										url = App.showInputDialog(self, ResourceUtil.get(9148), "");
									}
								}catch (final Exception ex) {
								}finally{
									if(url == null){
										selfBiz.setPara(Boolean.FALSE);
										return;
									}
								}
								
								final File fileHar = ResourceUtil.getTempFileName(Designer.HAR_EXT);
								final Properties had = new Properties();
								try{
									String strharurl = null;
									//支持har和had下载
									final String lowerCaseURL = url.toLowerCase();
									if(lowerCaseURL.startsWith("http") == false){
										throw new Exception("it must be download file.");
									}
									if(lowerCaseURL.endsWith(Designer.HAR_EXT)){
										strharurl = url;
									}else if(lowerCaseURL.endsWith(Designer.HAD_EXT)){
										LinkProjectManager.loadHAD(url, had);
										strharurl = had.getProperty(HCjad.HAD_HAR_URL, HCjad.convertToExtHar(url));
										final String projID = had.getProperty(HCjad.HAD_ID, "");
										if(DelDeployedProjManager.isDeledDeployed(projID)){
											throw new Exception(ResourceUtil.PROJ_IS_DELED_NEED_RESTART);
										}
									}else{
										throw new Exception("invaild url, it must be har or had file.");
									}
									PropertiesManager.addDelFile(fileHar);
									
									final String hadmd5 = had.getProperty(HCjad.HAD_HAR_MD5, "");
									ProcessingWindowManager.showCenterMessage("downloading...");
									final boolean succ = HttpUtil.download(fileHar, new URL(strharurl), 3, ResourceUtil.getUserAgentForHAD());
									if(succ == false){
										ProcessingWindowManager.disposeProcessingWindow();
										throw new Exception("http connection error");
									}
									
							        if((hadmd5.length() > 0 && ResourceUtil.getMD5(fileHar).toLowerCase().equals(hadmd5.toLowerCase()))
							        		|| hadmd5.length() == 0){
							        	addProjFromLocal(self, selfBiz, fileHar);
							        }else{
							        	throw new Exception("md5 error, try after a minute");
							        }
								}catch (final Exception e) {
									ExceptionReporter.printStackTrace(e);
									App.showConfirmDialog(self, "Fail download, Exception : " + e.toString(), "Fail download", JOptionPane.OK_OPTION);
									selfBiz.setPara(Boolean.FALSE);
									return;
								}
							}
						}, threadPoolToken));
						addFromLocal.addActionListener(new HCActionListener(new Runnable() {
							@Override
							public void run() {
								final File file = FileSelector.selectImageFile(importBut, FileSelector.HAR_FILTER, true);
								addProjFromLocal(self, selfBiz, file);
							}
						}, threadPoolToken));
						
						isCancelOp = false;
						App.invokeLaterUI(new Runnable() {
							@Override
							public void run() {
								pop.show(importBut, importBut.getWidth() - pop.getPreferredSize().width, importBut.getHeight());
								pop.updateUI();//在Window JRE 6环境下，需此行才正常，否则出现白框
							}
						});
						
						while(true){
							try{
								Thread.sleep(200);
							}catch (final Exception e) {
							}
							final Object back = selfBiz.getPara();
							if(back != null){
								if(back instanceof Boolean){
									selfBiz.setPara(null);
								}
								break;
							}
							if(isCancelOp){
								selfBiz.setPara(null);
								break;
							}
						}
					}
				}, true, COL_NUM){//HCTabelPanel
			@Override
			protected void refreshButton() {
				super.refreshButton();
				
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if(tablePanel != null && tablePanel.table != null){//Android会出现null
							final int selectedRow = tablePanel.table.getSelectedRow();
							if(selectedRow >= 0){
								designBut.setEnabled(isDeployed(selectedRow));
							}else{
								designBut.setEnabled(false);
							}
						}
					}
				});
			}
		};
		final DefaultTableCellRenderer centerCellRender = new DefaultTableCellRenderer(){
	        @Override
			public Component getTableCellRendererComponent(
	                final JTable table, final Object value, final boolean isSelected,
	                final boolean hasFocus, final int row, final int column) {
	        	setHorizontalAlignment(CENTER);
		        return super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
			}
        };
        
        final ListSelectionModel selectModel = tablePanel.table.getSelectionModel();
        selectModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    	
        designBut.setEnabled(dataRowNum > 0);

    	tablePanel.table.getColumnModel().getColumn(COL_NO).setCellRenderer(centerCellRender);
        tablePanel.table.getColumnModel().getColumn(COL_VER).setCellRenderer(centerCellRender);
        tablePanel.table.getColumnModel().getColumn(COL_PROJ_DESC).setCellRenderer(new DefaultTableCellRenderer(){
	        @Override
			public Component getTableCellRendererComponent(
	                final JTable table, final Object value, final boolean isSelected,
	                final boolean hasFocus, final int row, final int column) {
	        	setToolTipText((String)value);
		        return super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
			}
        });
        tablePanel.table.getColumnModel().getColumn(COL_UPGRADE_URL).setCellRenderer(new DefaultTableCellRenderer(){
	        @Override
			public Component getTableCellRendererComponent(
	                final JTable table, final Object value, final boolean isSelected,
	                final boolean hasFocus, final int row, final int column) {
		        final JLabel label = (JLabel)super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);

	        	String url = (String)value;
	        	label.setToolTipText(url);
	        	try{
		        	if(url.length() > 0){
			    		final int httpIdx = url.indexOf("//");
			    		final int pathIdx = url.indexOf("/", httpIdx + 2);
			    		url = url.substring(0, pathIdx + 1) + "...";
		        	}
	        	}catch (final Throwable e) {
	        		ExceptionReporter.printStackTrace(e);
	        	}
	        	label.setText(url);
	        	
	        	return label;
			}
        });
        
        initTable(tablePanel.table);
		
		editBut.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final LinkEditData led = (LinkEditData)data.elementAt(tablePanel.table.getSelectedRow())[IDX_OBJ_STORE];
				final LinkProjectStore lps = led.lps;
				
				final LinkNamePanel panel = showInputLinkName(self, lps.getLinkName(), lps.getProjectRemark(), editBut, lps);
				if(checkIsCancle(panel)){
					return;
				}
				final String newLinkName = panel.linkNameField.getText();
				final String newComment = panel.projRemarkField.getText();
				if((panel.isModiPermission == false) && newLinkName.equals(lps.getLinkName())
						&& newComment.equals(lps.getProjectRemark())){
					//相同或取消
				}else{
					led.op = (LinkProjectManager.STATUS_MODIFIED);

					lps.setLinkName(newLinkName);
					lps.setProjectRemark(newComment);
					panel.csc.saveToMap();
					ContextSecurityConfig.copyMapsToLPS(lps, panel.csc, true);

					tablePanel.table.updateUI();
					
					notifyNeedToSave();
				}
			}
		}, threadPoolToken));
		final JPanel buttonsList = new JPanel();
		buttonsList.setLayout(new GridLayout(1, 6, ClientDesc.hgap, ClientDesc.vgap));
		if(ResourceUtil.isJ2SELimitFunction()){
			buttonsList.add(designBut);
		}
		
		//由于设计器可堆栈出本界面，而本界面又可堆栈出Bind窗口，在此情形下，会导致画面难以管理，故仅限Android
		if(false && ResourceUtil.isAndroidServerPlatform()){//关闭Android服务器的rebind，改为手机方式
			rebindBut.setEnabled(false);
			buttonsList.add(rebindBut);//仅限android
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					while(ServerUIUtil.isStarted() == false){
						try{
							Thread.sleep(500);
						}catch (final Exception e) {
						}
					}
					Designer.checkBindEnable(rebindBut);
				}
			}, threadPoolToken);
		}
		
		buttonsList.add(upBut);
		buttonsList.add(downBut);
		buttonsList.add(removeBut);
		buttonsList.add(editBut);
		buttonsList.add(importBut);

		final JScrollPane scrollpane = new JScrollPane(tablePanel.table);
		int tableHeight = 200;//100在乐1下正好一行
		if(ResourceUtil.isAndroidServerPlatform()){
			final Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
			final int heightOfLetvOne = 1080;
			final int screenHeight = screensize.height;
			if(screenHeight >= 1080){
				tableHeight = 150;
			}
			if(screenHeight <= heightOfLetvOne){
				tableHeight = tableHeight * screenHeight / heightOfLetvOne;
			}
		}
		scrollpane.setPreferredSize(new Dimension(950, tableHeight));
		contentPane.add(scrollpane, BorderLayout.CENTER);
		contentPane.add(buttonsList, BorderLayout.NORTH);
		{
			final JPanel panel = new JPanel(new BorderLayout(ClientDesc.hgap, ClientDesc.vgap));
			final int hgap = 15;
			final JPanel group = new JPanel(new FlowLayout(FlowLayout.CENTER, hgap, 0));
			final JPanel groupPerm = new JPanel(new FlowLayout(FlowLayout.CENTER, hgap, 0));
			{
				final String op = getNewLinkedInProjOp();
				final String opPerm = getAcceptNewPermissionsOp();
				
				final ButtonGroup bgUpgradeMode = new ButtonGroup();
				
				final ItemListener itemListener = new ItemListener() {
					@Override
					public void itemStateChanged(final ItemEvent e) {
						notifyNeedToSave();
					}
				};
				
				rb_startup.setToolTipText("<html>" + (String)ResourceUtil.get(9150) + "</html>");
				bgUpgradeMode.add(rb_startup);
				group.add(rb_startup);
				if(op.equals(LinkPropertiesOption.OP_NEXT_START_UP)){
					rb_startup.setSelected(true);
				}
				
				rb_ask.setToolTipText((String)ResourceUtil.get(9151));
				bgUpgradeMode.add(rb_ask);
				group.add(rb_ask);
				if(op.equals(LinkPropertiesOption.OP_ASK)){
					rb_ask.setSelected(true);
				}
				
				rb_imme.setToolTipText((String)ResourceUtil.get(9152));
				bgUpgradeMode.add(rb_imme);
				group.add(rb_imme);
				if(op.equals(LinkPropertiesOption.OP_IMMEDIATE)){
					rb_imme.setSelected(true);
				}
				
				rb_startup.addItemListener(itemListener);
				rb_ask.addItemListener(itemListener);
				rb_imme.addItemListener(itemListener);
				
				{
					final ButtonGroup bgUpgradePermMode = new ButtonGroup();
					
					rb_perm_no.setToolTipText("<html>" + (String)ResourceUtil.get(9230) + "</html>");
					bgUpgradePermMode.add(rb_perm_no);
					groupPerm.add(rb_perm_no);
					if(opPerm.equals(LinkPropertiesOption.OP_PERM_NO_CHANGE)){
						rb_perm_no.setSelected(true);
					}
					
					rb_perm_acceptIfSigned.setToolTipText("<html>" + (String)ResourceUtil.get(9231) + "</html>");
					bgUpgradePermMode.add(rb_perm_acceptIfSigned);
					groupPerm.add(rb_perm_acceptIfSigned);
					if(opPerm.equals(LinkPropertiesOption.OP_PERM_ACCEPT_IF_SIGNED)){
						rb_perm_acceptIfSigned.setSelected(true);
					}
					
					rb_perm_no.addItemListener(itemListener);
					rb_perm_acceptIfSigned.addItemListener(itemListener);
				}

				String autoUpgradeTip = StringUtil.replace((String)ResourceUtil.get(9153), "{upgradeurl}", upgradeURL);
				autoUpgradeTip = StringUtil.replace(autoUpgradeTip, "{active}", ACTIVE);
				ch_autoUpgrade.setToolTipText("<html>" + autoUpgradeTip + "</html>");
				final boolean isEnableUpgrade = PropertiesManager.getValue(
					PropertiesManager.p_EnableLinkedInProjUpgrade, IConstant.TRUE).equals(IConstant.TRUE);
				ch_autoUpgrade.setSelected(isEnableUpgrade);
				enableUpgradeMode(isEnableUpgrade);

				ch_autoUpgrade.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(final ItemEvent e) {
						enableUpgradeMode(ch_autoUpgrade.isSelected());
						notifyNeedToSave();
					}
				});
			}
			final JPanel titl_group = new JPanel(new GridBagLayout());
			{
				final GridBagConstraints c = new GridBagConstraints();
				c.anchor = GridBagConstraints.LINE_START;

				c.weightx = 0;
				titl_group.add(compSelectMode, c);
				
				c.gridx = 1;
				c.weightx = 1;
				titl_group.add(group, c);
				
				c.gridy = 1;
				c.gridx = 0;
				c.weightx = 0;
				titl_group.add(compPermssionsMode, c);
				
				c.gridx = 1;
				c.weightx = 1;
				titl_group.add(groupPerm, c);
			}
			
			{
				final JPanel checkPanel = new JPanel(new BorderLayout());
				checkPanel.setBorder(new TitledBorder(""));
				
				checkPanel.add(ch_autoUpgrade, BorderLayout.NORTH);
				checkPanel.add(titl_group, BorderLayout.CENTER);
				panel.add(checkPanel, BorderLayout.NORTH);				
			}
			{
				final String line1 = StringUtil.replace((String)ResourceUtil.get(9154), "{isRoot}", (String)ResourceUtil.get(8017));
				final String line2 = StringUtil.replace((String)ResourceUtil.get(9155), "{apply}", saveAndApply);
				final String line3 = (String)ResourceUtil.get(9156);
				final JPanel descPanel = ServerUIUtil.buildDescPanel(
						HttpUtil.getHtmlLineStartTag() + line1 +
						"<BR>" +
						HttpUtil.getHtmlLineStartTag() + line2 +
						"<BR>" +
						HttpUtil.getHtmlLineStartTag() + line3);
				panel.add(descPanel, BorderLayout.CENTER);
			}
			contentPane.add(panel, BorderLayout.SOUTH);
		}
		saveAndApplyBtn = new JButton(saveAndApply, new ImageIcon(ImageSrc.OK_ICON)){
			@Override
			public void setEnabled(final boolean enabled){
				if(enabled){
					if(isOpenApply){
						super.setEnabled(enabled);
					}else{
						isNotifyModi = true;
					}
				}else{
					super.setEnabled(enabled);
				}
			}
		};
		saveAndApplyBtn.setEnabled(false);
		if(ServerUIUtil.isStarted() == false){
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					while(ServerUIUtil.isStarted() == false){
						try{
							Thread.sleep(500);
						}catch (final Exception e) {
						}
					}
					isOpenApply = true;
					if(isNotifyModi){
						saveAndApplyBtn.setEnabled(true);
					}
				}
			}, threadPoolToken);
		}else{
			isOpenApply = true;
		}
		final String exitText = (String) ResourceUtil.get(1011);
		
		final ActionListener cancelListener = new HCActionListener(new Runnable() {
			@Override
			public void run() {
				LinkProjectStatus.exitStatus();
				isCancelOp = true;
			}
		}, threadPoolToken);
		exitBtn = new JButton(((exitText == null)?(String) ResourceUtil.get(1018):exitText),
				new ImageIcon(ImageSrc.CANCEL_ICON));
		exitAction = new UIActionListener() {
			@Override
			public void actionPerformed(final Window window, final JButton ok,
					final JButton cancel) {
				if (cancelListener != null) {
					cancelListener.actionPerformed(null);//尽快释放Project-Lock状态
				}
				window.dispose();
			}
		};
		final ActionListener listener = new HCActionListener(new Runnable() {
			@Override
			public void run() {
				saveAndApplyBtn.setEnabled(false);
				
				final Window[] back = new Window[1];
				ProcessingWindowManager.showCenterMessageOnTop(self, true, saveAndApply + "...", back);
				saveAndApply(self);
				Designer.checkBindEnable(rebindBut);
				back[0].dispose();
			}
		}, threadPoolToken);
		saveAction = new UIActionListener() {
			@Override
			public void actionPerformed(final Window window, final JButton ok,
					final JButton cancel) {
				try {
					if (listener != null) {
						listener.actionPerformed(null);
					}
				} catch (final Exception e) {
					ExceptionReporter.printStackTrace(e);
				}
			}
		};

		designBut.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				dialog.dispose();
				LinkProjectStatus.exitStatus();
				
				final LinkEditData de_led = (LinkEditData)data.elementAt(tablePanel.table.getSelectedRow())[IDX_OBJ_STORE];
				
				LinkProjectManager.reloadLinkProjects();//必须要拉新，否则有可能有修改，但不要保存的。
				final LinkProjectStore lps = LinkProjectManager.getProjByID(de_led.lps.getProjectID());
				final File defaultFile = new File(ResourceUtil.getBaseDir(), LinkProjectManager.EDIT_HAR);
				if(L.isInWorkshop){
					LogManager.log("shift project check default HAR : " + defaultFile.getAbsolutePath() + " exists : " + defaultFile.exists());
				}
				if(defaultFile.exists() == false){
					//如果第一次是从ProjManager进入到设计器，防止初始加载缺省工程，所以要进行复制
					LinkProjectManager.copyCurrEditFromStorage(lps, false);
				}
				
				final Designer d = Designer.getInstance();
				if(d == null){
					LinkMenuManager.startDesigner(false);
				}
				Designer.getInstance().shiftProject(lps);
			}
		}, threadPoolToken));

	}
	
	private final boolean isDeployed(final int selectedRow){
		final Object[] elementAt = data.elementAt(selectedRow);
		if(elementAt == null){
			return false;
		}
		
		final LinkEditData led = (LinkEditData)elementAt[IDX_OBJ_STORE];
		return led != null && led.status == LinkProjectManager.STATUS_DEPLOYED;
	}

	public Window toShow(){
		return App.showCenterNoOwner(contentPane, 0, 0, true, exitBtn, saveAndApplyBtn, 
				true, exitAction, saveAction, dialog, relativeTo, true);//isResizabel=false,会导致漂移
	}

	private void checkAndStoreData() {
		final int size = dataRowNum;
		
		//检查Root和Active条件相关约束
		boolean hasRoot = false;
		boolean hasActive = false;
		LinkProjectStore firstActive = null;
		for (int i = 0; i < size; i++) {
			final LinkProjectStore lps = ((LinkEditData)data.elementAt(i)[IDX_OBJ_STORE]).lps;
			if(lps.isRoot()){
				lps.setActive(true);
				hasRoot = true;
				break;
			}
			if(lps.isActive()){
				if(firstActive == null){
					firstActive = lps;
				}
				hasActive = true;
			}
		}
		if(hasActive && hasRoot == false){
			firstActive.setRoot(true);
		}
		
		final LinkProjectStore[] lpss = new LinkProjectStore[size];
		for (int i = 0; i < size; i++) {
			lpss[i] = ((LinkEditData)data.elementAt(i)[IDX_OBJ_STORE]).lps;
		}
		
		final PropertiesSet projIDSet = AddHarHTMLMlet.newLinkProjSetInstance();
		AddHarHTMLMlet.saveLinkStore(lpss, projIDSet);
	}

	protected LinkNamePanel showInputLinkName(final JFrame self, final String linkName, final String mem, 
			final Component relativeTo, final LinkProjectStore lps) {
		final LinkNamePanel panel = new LinkNamePanel(linkName, mem, ContextSecurityConfig.getContextSecurityConfig(lps), lps);
		App.showCenterPanelMain(panel, 0, 0, (String)ResourceUtil.get(9017), true, null, null,
			null, //cancel
			new HCActionListener(new Runnable() {
				@Override
				public void run() {
					panel.linkNameField.setText(panel.CANCLE);
				}
			}, threadPoolToken), self, true, false, relativeTo, false, false);//isNewFrame必须，因为JRE6会导致锁，所以不在addProjFromLocal中使用
		return panel;
	}

	public boolean checkIsCancle(final LinkNamePanel panel) {
		try{
			Thread.sleep(500);//等待上个CancelLister逻辑执行完成
		}catch (final Exception e) {
		}
		return panel.linkNameField.getText().equals(panel.CANCLE);
	}

	/**
	 * 重要，请勿在Event线程中调用，
	 * @param self
	 * @param selfBiz
	 * @param file
	 */
	private final void addProjFromLocal(final JFrame self, final AbstractDelayBiz selfBiz, final File file) {
		if(file != null){
			final String failTitle = "fail to load HAR project";
			
			if(ResourceUtil.checkSysPackageNameInJar(file)){
				App.showMessageDialog(self, ResourceUtil.RESERVED_PACKAGE_NAME_IS_IN_HAR, 
						failTitle, JOptionPane.ERROR_MESSAGE);
				//不能return
			}else{
				final Map<String, Object> map = AddHarHTMLMlet.getMap(file);
				if(map.isEmpty()){
					App.showMessageDialog(self, ResourceUtil.HAR_PROJECT_FILE_IS_CORRUPTED, 
							failTitle, JOptionPane.ERROR_MESSAGE);
					//不能return
				}else{
					final String proj_id = (String)map.get(HCjar.PROJ_ID);
					
					if(DelDeployedProjManager.isDeledDeployed(proj_id)){
						App.showMessageDialog(self, ResourceUtil.PROJ_IS_DELED_NEED_RESTART, 
								failTitle, JOptionPane.ERROR_MESSAGE);
					}else if(SignHelper.verifyJar(file, LinkProjectManager.getCertificatesByID(proj_id)) == null){//完整性检查进行前置
						App.showMessageDialog(self, ResourceUtil.FILE_IS_MODIFIED_AFTER_SIGNED, 
								failTitle, JOptionPane.ERROR_MESSAGE);
						//不能return
					}else{
						final String licenseURL = ((String)map.get(HCjar.PROJ_LICENSE)).trim();
						if(ResourceUtil.needAccepLicense(licenseURL)){
							ProcessingWindowManager.showCenterMessageOnTop(self, true, (String)ResourceUtil.get(9110), null);//processing...
							final IBiz biz = new IBiz(){
								@Override
								public void setMap(final HashMap map) {
								}
		
								@Override
								public void start() {
									loadToTable(self, selfBiz, file, map);
								}
							};
							App.showAgreeLicense("License of [" + map.get(HCjar.PROJ_NAME) + "]", false, licenseURL, biz, null, true);
							return;
						}
						loadToTable(self, selfBiz, file, map);
						return;
					}
				}
			}
		}
		
		selfBiz.setPara(Boolean.FALSE);
	}

	/**
	 * 重要，请勿在Event线程中调用，
	 * @param self
	 * @param selfBiz
	 * @param file
	 * @param map
	 */
	private final void loadToTable(final JFrame self, final AbstractDelayBiz selfBiz,
			final File file, final Map<String, Object> map) {
		int delOldProjIndex = -1;
		String linkName = "";
		String linkRemark = "";
		boolean delIsRoot = false;
		
		//检查是否存在同名的工程
		final String proj_id = (String)map.get(HCjar.PROJ_ID);
		for (int i = 0; i < dataRowNum; i++) {
			final LinkEditData led = (LinkEditData)data.elementAt(i)[IDX_OBJ_STORE];
			final LinkProjectStore lps = led.lps;
			
			if(lps.getProjectID().equals(proj_id)){
//						tablePanel.table.setRowSelectionInterval(i, i);
				//override project
				final String overrideStr = StringUtil.replace((String)ResourceUtil.get(9157), "{id}", proj_id);
				final int out = App.showOptionDialog(self, overrideStr, (String)ResourceUtil.get(9158));
				if(out == JOptionPane.YES_OPTION){
					delProjInList(led);
					
					delOldProjIndex = i;
					linkName = lps.getLinkName();
					linkRemark = lps.getProjectRemark();
					delIsRoot = lps.isRoot();
					led.isUpgrade = true;
				}else{
					selfBiz.setPara(Boolean.FALSE);
					return;
				}
				break;
			}
		}
		
//				if(delOldProjIndex < 0){
//					LinkNamePanel panel = showInputLinkName(self, "", "", editBut);
//					if(checkIsCancle(panel)){
//						selfBiz.setPara(Boolean.FALSE);
//						return;
//					}else{
//						linkName = panel.linkNameField.getText();
//						linkRemark = panel.projRemarkField.getText();
//					}
//				}
		
		final LinkEditData led = AddHarHTMLMlet.buildAddHarDesc(file, map, linkName, linkRemark);
		final Object[] libName = {
			"", led
		};
		
		//必须要提前
		dataRowNum++;
		
		notifyNeedToSave();
		
		if(delOldProjIndex < 0){
			selfBiz.setPara(libName);
		}else{
			final LinkEditData oldLed = (LinkEditData)data.elementAt(delOldProjIndex)[IDX_OBJ_STORE];
			oldLed.lps.copyBindTo(led.lps);
			data.elementAt(delOldProjIndex)[IDX_OBJ_STORE] = led;
			led.lps.setRoot(delIsRoot);
			
			//因为下行无对象返回，所以本处进行刷新
			tablePanel.table.updateUI();
			selfBiz.setPara(Boolean.FALSE);
		}
		
		return;
	}

	private void notifyNeedToSave(){
		saveAndApplyBtn.setEnabled(true);
	}
	
	private void delProjInList(final LinkEditData led) {
		delList.add(led);
		dataRowNum--;
	}

	private void saveAndApply(final JFrame self) {
		{
			final Vector<LinkProjectStore> stores = new Vector<LinkProjectStore>();
			
			for (int i = 0; i < dataRowNum; i++) {
				final LinkEditData led = (LinkEditData)data.elementAt(i)[IDX_OBJ_STORE];
				final LinkProjectStore lps = led.lps;
				
				if(lps.isActive()){
					stores.add(lps);
				}
			}
			
			if(LinkProjectManager.checkReferencedDependency(self, stores) == false){
				return;
			}
		}
		
		final HashMap<String, File> delBackFileMap = new HashMap<String, File>();
		
		final String[] delCacheProjIDS;
		final String[] removedAndNotUpgrade;
		synchronized (ServerUIUtil.LOCK) {
			//将已发布，且准备进行删除的进行删除操作
			{
				final int size = delList.size();
				delCacheProjIDS = new String[size];
				removedAndNotUpgrade = new String[size];
				
				for (int i = 0; i < size; i++) {
					final LinkEditData led = delList.elementAt(i);
					final LinkProjectStore lps = led.lps;
					boolean isRemoved = false;
					if(led.status == LinkProjectManager.STATUS_DEPLOYED){
						final File oldBackEditFile = LinkProjectManager.removeLinkProjectPhic(lps, true);
						isRemoved = true;
						if(oldBackEditFile != null){
							delBackFileMap.put(lps.getProjectID(), oldBackEditFile);
						}
						isChanged = true;
					}
					
					if(led.isUpgrade == false){
//						LinkProjectManager.removeOnlyLPS(lps);
						delCacheProjIDS[i] = lps.getProjectID();
						if(isRemoved){
							DelDeployedProjManager.addDeledDeployed(lps.getProjectID());
							removedAndNotUpgrade[i] = lps.getProjectID();
						}
					}
				}
				delList.removeAllElements();
			}
			
			final Vector<String> noActiveProjs = new Vector<String>();
			
			for (int i = 0; i < dataRowNum; i++) {
				final LinkEditData led = (LinkEditData)data.elementAt(i)[IDX_OBJ_STORE];
				final LinkProjectStore lps = led.lps;
				if(led.status == LinkProjectManager.STATUS_NEW){
					final File oldBackEditFile = delBackFileMap.get(lps.getProjectID());
					final boolean result = AddHarHTMLMlet.addHarToDeployArea(J2SESession.NULL_J2SESESSION_FOR_PROJECT, 
							led, lps, false, true, oldBackEditFile);
					isChanged = isChanged?true:result;
				}else if(led.op == LinkProjectManager.STATUS_MODIFIED){
					if(lps.isActive() == false){
						noActiveProjs.add(lps.getProjectID());
					}
					isChanged = true;
				}
				led.op = (LinkProjectManager.STATUS_NEW);
			}
			
			if(noActiveProjs.size() > 0){
				final String[] noActive = new String[noActiveProjs.size()];
				for (int i = 0; i < noActive.length; i++) {
					noActive[i] = noActiveProjs.elementAt(i);
				}
				CacheManager.delProjects(noActive);//因为手机端会上线后，进行同步，所以noActive必须执行本操作
			}
			
			if(rb_startup.isSelected()){
				PropertiesManager.setValue(PropertiesManager.p_OpNewLinkedInProjVer, LinkPropertiesOption.OP_NEXT_START_UP);
			}else if(rb_ask.isSelected()){
				PropertiesManager.setValue(PropertiesManager.p_OpNewLinkedInProjVer, LinkPropertiesOption.OP_ASK);
			}else if(rb_imme.isSelected()){
				PropertiesManager.setValue(PropertiesManager.p_OpNewLinkedInProjVer, LinkPropertiesOption.OP_IMMEDIATE);
			}

			if(rb_perm_no.isSelected()){
				PropertiesManager.setValue(PropertiesManager.p_OpAcceptNewPermissions, LinkPropertiesOption.OP_PERM_NO_CHANGE);
			}else if(rb_perm_acceptIfSigned.isSelected()){
				PropertiesManager.setValue(PropertiesManager.p_OpAcceptNewPermissions, LinkPropertiesOption.OP_PERM_ACCEPT_IF_SIGNED);
			}
			
			PropertiesManager.setValue(PropertiesManager.p_EnableLinkedInProjUpgrade, ch_autoUpgrade.isSelected()?IConstant.TRUE:IConstant.FALSE);
			
			PropertiesManager.saveFile();
			if(isChanged){
//								LogManager.log("restarting service...");
				//启动时，需要较长时间初始化，有可能用户快速打开并更新保存，所以加锁。
				synchronized (ServerUIUtil.LOCK) {
					if(ServerUIUtil.promptAndStop(true, self) == false){
					}
					
					checkAndStoreData();
					
					//更新后必须reload
					LinkProjectManager.reloadLinkProjects();
					
					//由于上行已更新，所以可以采用searchRoot
					final LinkProjectStore root = LinkProjectManager.searchRoot(true);//必须查询为active状态的。
					if(root != null){
						Designer.setProjectOn();
					}else{
						Designer.setProjectOff();	
					}
				}
				
				//启动远屏或菜单
				ServerUIUtil.restartResponsorServer(self, null);
				
				if(isChanged){
					final Designer design = Designer.getInstance();
					if(design != null){
						design.refresh();//注意：要在restartResponsorServer之后
					}
				}

				loadData();
				tablePanel.table.repaint();//检查完善Root,Active，故刷新
				
				//如果是升级型，则可能出现null
				CacheManager.delProjects(delCacheProjIDS);
				AIPersistentManager.removeAndNotUpgrade(removedAndNotUpgrade);
				PropertiesManager.saveFile();
				
				isChanged = false;
			}
		}
	}

	private final void enableUpgradeMode(final boolean selected) {
		rb_startup.setEnabled(selected);
		rb_ask.setEnabled(selected);
		rb_imme.setEnabled(selected);
		
		rb_perm_no.setEnabled(selected);
		rb_perm_acceptIfSigned.setEnabled(selected);
		
		compSelectMode.setEnabled(selected);
		compPermssionsMode.setEnabled(selected);
	}

}
