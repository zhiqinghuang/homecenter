package hc.server.ui.design;

import hc.core.L;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.core.util.UIUtil;
import hc.server.ui.ClientDesc;
import hc.server.ui.HCByteArrayOutputStream;
import hc.server.ui.ICanvas;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.hpj.HCjar;
import hc.util.BaseResponsor;
import hc.util.ResourceUtil;

import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Map;

public class JarMainMenu extends MCanvasMenu implements ICanvas {
	private final HCByteArrayOutputStream baos = ServerUIUtil.buildForMaxIcon();
	public String[][] items;
	private BufferedImage[] cacheOriImage;
	public int[] itemTypes;
	public String[] extendMap;
	public static final int itemDescNum = 3;
	private final String menuName;
	public final String menuId;
	private final int menuColNum;
	public String[] listener;
	public final boolean isRoot;
	
	public static final int ITEM_NAME_IDX = 0;
	public static final int ITEM_IMG_IDX = 1;
	public static final int ITEM_URL_IDX = 2;
	private final BaseResponsor baseRe;
	private final String linkMenuName;
	private final int menuIdx;
	boolean enableQR, enableWiFi;
	final Map map;
	
	public JarMainMenu(final int menuIdx, final Map map, final boolean isRoot, final BaseResponsor baseRep, final String linkMenuName) {
		this.isRoot = isRoot;
		this.baseRe  = baseRep;
		this.linkMenuName = linkMenuName;
		
		menuName = HCjar.getMenuName(map, menuIdx);
		menuId = (String)map.get(HCjar.replaceIdxPattern(HCjar.MENU_ID, menuIdx));
		menuColNum = Integer.parseInt((String)map.get(HCjar.replaceIdxPattern(HCjar.MENU_COL_NUM, menuIdx)));
		this.menuIdx = menuIdx;
		
		this.map = map;
	}

	public final void rebuildMenuItemArray() {
		cacheOriImage = null;
		
		int appendMenuItemNum = 0;//新增按钮及子工程的文件夹数量
		int projFoldNum = 0;//优先显示的工程文件夹数量
		if(isRoot){
			final Iterator<LinkProjectStore> it = LinkProjectManager.getLinkProjsIterator(false);
			while(it.hasNext()){
				final LinkProjectStore lps = it.next();
				if( isNeedAddFolder(lps)){
					projFoldNum++;
				}
			}
			
			appendMenuItemNum = projFoldNum;
			
			//手机须有相机和WiFi
			if(enableQR){
				appendMenuItemNum += 1;//增加两个添加设备
			}
			if(enableWiFi){
				appendMenuItemNum += 1;
			}
		}
		final Object menuItemObj = map.get(HCjar.replaceIdxPattern(HCjar.MENU_CHILD_COUNT, menuIdx));
		if(menuItemObj != null){
			final int itemCount = Integer.parseInt((String)menuItemObj);
			items = new String[itemCount + appendMenuItemNum][itemDescNum];
			itemTypes = new int[itemCount + appendMenuItemNum];
			listener = new String[itemCount + appendMenuItemNum];
			extendMap = new String[itemCount + appendMenuItemNum];
			
			final int fold_type = 0;
			
			if(appendMenuItemNum > 0){
				//添加LinkProject文件夹
				final Iterator<LinkProjectStore> it = LinkProjectManager.getLinkProjsIterator(false);
				int i = 0;
				while(it.hasNext()) {
					final LinkProjectStore lps = it.next();
					if( isNeedAddFolder(lps)){
					}else{
						//如果工程内没有菜单项，则不显示。含事件或未来其它
						continue;
					}
					final int itemIdx = (i++);
					items[itemIdx][ITEM_NAME_IDX] = lps.getLinkScriptName();
					itemTypes[itemIdx] = fold_type;
					items[itemIdx][ITEM_IMG_IDX] = UIUtil.SYS_FOLDER_ICON;
					items[itemIdx][ITEM_URL_IDX] = HCURL.buildStandardURL(HCURL.MENU_PROTOCAL, lps.getProjectID());
					
					listener[itemIdx] = ""; 
					extendMap[itemIdx] = "";
				}
			}

			final String Iheader = HCjar.replaceIdxPattern(HCjar.MENU_ITEM_HEADER, menuIdx);
			int storeIdx = 0;
			for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
				final String header = Iheader + itemIdx + ".";
				
				storeIdx = itemIdx + projFoldNum;
				
				items[storeIdx][ITEM_NAME_IDX] = (String)map.get(header + HCjar.ITEM_NAME);
				itemTypes[storeIdx] = Integer.parseInt((String)map.get(header + HCjar.ITEM_TYPE));
				items[storeIdx][ITEM_IMG_IDX] = (String)map.get(header + HCjar.ITEM_IMAGE);
				items[storeIdx][ITEM_URL_IDX] = (String)map.get(header + HCjar.ITEM_URL);
				
				listener[storeIdx] = (String)map.get(header + HCjar.ITEM_LISTENER);
				extendMap[storeIdx] = (String)map.get(header + HCjar.ITEM_EXTENDMAP); 
			}
			
			//添加"增加设备"按钮
			if(enableQR){
				final int itemIdx = (++storeIdx);
				items[itemIdx][ITEM_NAME_IDX] = (String)ResourceUtil.get(9016);
				itemTypes[itemIdx] = fold_type;
				items[itemIdx][ITEM_IMG_IDX] = UIUtil.SYS_ADD_DEVICE_BY_QR_ICON;
				items[itemIdx][ITEM_URL_IDX] = HCURL.URL_CFG_ADD_DEVICE_BY_QR;
				
				listener[itemIdx] = ""; 
				extendMap[itemIdx] = "";
			}
			if(enableWiFi){
				final int itemIdx = (++storeIdx);
				items[itemIdx][ITEM_NAME_IDX] = (String)ResourceUtil.get(9016);
				itemTypes[itemIdx] = fold_type;
				items[itemIdx][ITEM_IMG_IDX] = UIUtil.SYS_ADD_DEVICE_BY_WIFI_ICON;
				items[itemIdx][ITEM_URL_IDX] = HCURL.URL_CFG_ADD_DEVICE_BY_WIFI;
				
				listener[itemIdx] = ""; 
				extendMap[itemIdx] = "";
			}

		}else{
			items = new String[0][itemDescNum];
			itemTypes = new int[0];
			listener = new String[0];
			extendMap = new String[0];
		}
	}

	private boolean isNeedAddFolder(final LinkProjectStore lps) {
		return lps.isActive() && findExistMenuItemNumInProj(lps.getProjectID());
	}
	
	private boolean findExistMenuItemNumInProj(final String projID){
		if(baseRe instanceof MobiUIResponsor){
			try{
				final MobiUIResponsor mr = (MobiUIResponsor)baseRe;
				for (int i = 0; i < mr.responserSize; i++) {
					if(mr.projIDs[i].equals(projID)){
						final Map map = mr.maps[i];
						final Object chileCount = map.get(HCjar.replaceIdxPattern(HCjar.MENU_CHILD_COUNT, 0));
						if(chileCount != null){
							if(Integer.parseInt((String)chileCount) > 0){
								return true;
							}
						}
					}
				}
			}catch (final Exception e) {
			}
		}
		return false;
	}

	@Override
	public String[] getIconLabels() {
		final String[] out = new String[items.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = items[i][ITEM_NAME_IDX];
		}
		return out;
	}

	@Override
	public String[] getIcons() {
		final boolean oldEnableQR  = enableQR;
		final boolean oldEnableWiFi  = enableWiFi;
		if(isRoot){
			//可能新用户重新登录，手机WiFi状态不一
			enableQR = ClientDesc.getAgent().hasCamera();// || PropertiesManager.isTrue(PropertiesManager.p_IsSimu);
			//关闭WiFi广播HAR
			enableWiFi = HCURL.isUsingWiFiWPS && ResourceUtil.canCtrlWiFi();// || PropertiesManager.isTrue(PropertiesManager.p_IsSimu);
		}
		
		if(items == null || oldEnableQR != enableQR || oldEnableWiFi != enableWiFi){
			synchronized (ServerUIUtil.LOCK) {
				rebuildMenuItemArray();
			}
		}
		
		final int targetMobileIconSize = UIUtil.calMenuIconSize(ClientDesc.getClientWidth(), ClientDesc.getClientHeight(), ClientDesc.getDPI());
		final int size = items.length;
		final String[] out = new String[size];
		for (int i = 0; i < size; i++) {
			String imgData = items[i][ITEM_IMG_IDX];
			if(imgData.startsWith(UIUtil.SYS_ICON_PREFIX)){
				
			}else{
				//仅传送适合目标手机尺寸的图标
				if(cacheOriImage == null){
					cacheOriImage = new BufferedImage[size];
				}
				BufferedImage oriImage = cacheOriImage[i];
				if(oriImage == null){
					oriImage = ServerUIUtil.base64ToBufferedImage(imgData);
					cacheOriImage[i] = oriImage;
				}
				if(oriImage != null){
					if(UIUtil.isIntBeiShu(targetMobileIconSize, oriImage.getWidth())){
					}else{
						//服务器端进行图片缩放
						final BufferedImage newImg = ResourceUtil.resizeImage(oriImage, targetMobileIconSize, targetMobileIconSize);
						//使用适应尺寸的base64图标
						imgData = ServerUIUtil.imageToBase64(newImg, baos);
					}
				}
			}
			out[i] = imgData;
		}
		return out;
	}

	@Override
	public int getNumCol() {
		return menuColNum;
	}

	@Override
	public int getNumRow() {
		return (items.length/3 + 1);
	}

	@Override
	public int getSize() {
		return items.length;
	}

	@Override
	public String getTitle() {
		return menuName;
	}

	@Override
	public String[] getURLs() {
		final String[] out = new String[items.length];
		for (int i = 0; i < out.length; i++) {
			out[i] = items[i][ITEM_URL_IDX];
		}
		return out;
	}

	@Override
	public boolean isFullMode() {
		return false;
	}

	@Override
	public void onStart() {
	}

	@Override
	public void onPause() {
	}

	@Override
	public void onResume() {
	}

	@Override
	public void onExit() {
		//返回root主菜单
		if(isRoot){
			//当前是root
		}else{
			L.V = L.O ? false : LogManager.log(" exit/back link menu [" + linkMenuName + "]");
			if(baseRe instanceof MobiUIResponsor){
				final MobiUIResponsor mobiUIResponsor = (MobiUIResponsor)baseRe;
				mobiUIResponsor.enterContext(mobiUIResponsor.findRootContextID());
			}
			//千万别执行如下，确保每次手机连接使用同一服务实例，从而共享数据状态
//			backServer = null;
		}
	}

}
