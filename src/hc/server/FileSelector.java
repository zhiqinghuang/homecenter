package hc.server;

import hc.App;
import hc.util.PropertiesManager;
import java.awt.Component;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileSelector {

	private final static JFileChooser chooser = new JFileChooser((File)null);
	static {
		String path = PropertiesManager.getValue(PropertiesManager.p_FileChooserDir);
		if(path != null){
			File pathFile = new File(path);
			if(pathFile.exists()){
				chooser.setCurrentDirectory(pathFile);
			}
		}
	}
	public static final int IMAGE_FILTER = 1;
	public static final int MUSIC_FILTER = 2;
	public static final int JAR_FILTER = 3;
	public static final int HAR_FILTER = 4;
	public static final int FOLDER_FILTER = 5;
	public static final int NATIVE_FILTER = 6;
	
	private static FileNameExtensionFilter imageFilter = new FileNameExtensionFilter(
			"image file",
			ImageIO.getReaderFileSuffixes());
	private static String[] musicFile = {"au"};
	private static FileNameExtensionFilter musicFilter = new FileNameExtensionFilter(
			"music file", musicFile
			);
	private static String[] jarFile = {"jar"};
	private static FileNameExtensionFilter jarFilter = new FileNameExtensionFilter(
			"jar file", jarFile
			);
	private static FileFilter nativeFilter = new FileFilter(){
		public boolean accept(File f) {
			if (f.isDirectory()) {
                return false;
            }
			return true;
		}
		
		public String getDescription() {
			return "native file";
		}
	};
	private static String[] harFile = {"har"};
	private static FileNameExtensionFilter harFilter = new FileNameExtensionFilter(
			"har (HomeCenter archive) file", harFile
			);
	private static String[] dirFile = {"directory"};
	private static FileNameExtensionFilter dirFilter = new FileNameExtensionFilter(
			"directory", dirFile
			);
	
	public static File selectImageFile(final Component parent, int type, final boolean isOpen) {
		FileFilter fnef = null;

		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		
		if(type == MUSIC_FILTER){
			fnef = musicFilter;
		}else if(type == JAR_FILTER){
			fnef = jarFilter;
		}else if(type == NATIVE_FILTER){
			fnef = nativeFilter;
		}else if(type == HAR_FILTER){
			fnef = harFilter;
		}else if(type == FOLDER_FILTER){
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			fnef = dirFilter;
		}else{
			fnef = imageFilter;
		}
		
		chooser.setFileFilter(fnef);
				
		if(isOpen){
			chooser.setDialogTitle("select / open " + fnef.getDescription());
		}else{
			chooser.setDialogTitle("save to " + fnef.getDescription());
		}
		
		try{
			Thread.sleep(100);//增加延时，否则debug时，不出FileDialog
		}catch (Exception e) {
		}
		int ans = (isOpen?chooser.showOpenDialog(parent):chooser.showSaveDialog(parent));
		if (ans == JFileChooser.APPROVE_OPTION) {
			PropertiesManager.setValue(PropertiesManager.p_FileChooserDir, 
					chooser.getCurrentDirectory().getAbsolutePath());
			PropertiesManager.saveFile();
			return chooser.getSelectedFile();
		}
		return null;
	}
}
