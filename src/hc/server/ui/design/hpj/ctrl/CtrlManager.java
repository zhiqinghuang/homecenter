package hc.server.ui.design.hpj.ctrl;

import hc.App;
import hc.server.ui.CtrlResponse;

public class CtrlManager {
	final CtrlResponse ctrler;
	
	public CtrlManager(CtrlResponse ctrler) {
		this.ctrler = ctrler;
	}
	
	public void ReceiveOnKey(final int keyValue){
		App.showMessageDialog(null, "maybe unused code here ");
		this.ctrler.click(keyValue);
	}
	
	public void onExit(){
		
	}
}
