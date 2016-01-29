package hc.server;

import hc.App;
import hc.core.RootServerConnector;
import hc.server.ui.LinkProjectStatus;
import hc.server.ui.design.Designer;

import java.awt.Component;
import java.lang.reflect.Method;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class LinkMenuManager {
	public static boolean notifyCloseDesigner(){
		try {
			final Class c = getDesignerClass();
			final Method m = c.getMethod("notifyCloseDesigner", new Class[] {});
			return((Boolean)m.invoke(c, new Object[] {})).booleanValue();
		} catch (final Throwable e) {
			e.printStackTrace();
			App.showConfirmDialog(null, "load link panel error : " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		return false;
	}

	private static Class<?> getDesignerClass() throws ClassNotFoundException {
		return Class.forName(RootServerConnector.unObfuscate("chs.reev.riud.segi.neDisngre"));
	}
	
	public static void showLinkPanel(final JFrame frame){
		try {
			final Class c = getDesignerClass();
			final Method m = c.getMethod("showLinkPanel", new Class[] {JFrame.class, boolean.class, Component.class});
			m.invoke(c, new Object[] {frame, Boolean.TRUE, null});
		} catch (final Throwable e) {
			e.printStackTrace();
			App.showConfirmDialog(null, "load link panel error : " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static void closeLinkPanel(){
		try {
			final Class c = getDesignerClass();
			final Method m = c.getMethod("closeLinkPanel", new Class[] {});
			m.invoke(c, new Object[] {});
		} catch (final Throwable e) {
			e.printStackTrace();
			App.showConfirmDialog(null, "load link panel error : " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static void startDesigner(final boolean loadInit){
		if(LinkProjectStatus.tryEnterStatus(null, LinkProjectStatus.MANAGER_DESIGN)){
			try{
				final Class design = getDesignerClass();
				SingleJFrame.showJFrame(design);
				Designer.getInstance().loadInitProject(loadInit);
			}catch (final Exception ee) {
				ee.printStackTrace();
				App.showConfirmDialog(null, "Cant load Designer", 
						"Error", JOptionPane.CLOSED_OPTION, JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public static void startAutoUpgradeBiz(){
		final Object[] paraNull = new Object[]{};
		final Class[] paraClasNull = new Class[]{};
		try {
			final Class c = getDesignerClass();
			final Method m = c.getMethod("startAutoUpgradeBiz", paraClasNull);
			m.invoke(c, paraNull);
		} catch (final Throwable e) {
			App.showConfirmDialog(null, "startAutoUpgradeBiz error : " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
}
