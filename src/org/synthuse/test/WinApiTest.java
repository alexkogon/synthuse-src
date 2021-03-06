package org.synthuse.test;


import java.util.LinkedHashMap;
import java.util.Map;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.synthuse.Api;
import org.synthuse.WindowInfo;
import org.synthuse.WindowsEnumeratedXml;
import org.synthuse.Api.Kernel32Ex;
import org.synthuse.Api.PsapiEx;
import org.synthuse.Api.User32Ex;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinDef.HMENU;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.ptr.PointerByReference;

public class WinApiTest {
	Api api = new Api();

	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {
		
	}
	
	public static void output(String val) {
		//System.out.println(val);
	}
	
	//copied and modified slightly from WindowInfo class
	public String getWindow32Information(HWND hWnd, boolean isChild) {
		String hwndStr = "";
		HWND parent = null;
		String parentStr = "";
		RECT rect;
		String text;
		String className = "";
		String processName = "";
		long pid = 0;
		
		Map<String, String> extra = null;

        byte[] buffer = new byte[1024];
        output("Calling GetWindowTextA");
        User32Ex.instance.GetWindowTextA(hWnd, buffer, buffer.length);
        text = Native.toString(buffer);
        output("GetWindowTextA returned: " + text);
        if (text.isEmpty()) {
            output("Calling sendWmGetText");
        	text = new Api().sendWmGetText(hWnd);
        	output("sendWmGetText returned: " + text);
        }
        
        char[] buffer2 = new char[1026];
        output("Calling GetClassName");
		User32Ex.instance.GetClassName(hWnd, buffer2, 1026);
		className = Native.toString(buffer2);
		output("GetClassName returned: " + className);
		
    	HMENU hmenu = Api.User32Ex.instance.GetMenu(hWnd);
		//hmenu = Api.User32.instance.GetSubMenu(hmenu, 0);
		if (hmenu != null) { //menu item count
	    	int menuCount = Api.User32Ex.instance.GetMenuItemCount(hmenu);
	    	if (menuCount > 0) {
				if (extra == null)
					extra = new LinkedHashMap<String, String>(); 
				extra.put("menuCount", menuCount + "");
				System.out.println("className: " + className);
				System.out.println("text: " + text);
				System.out.println("menuCount: " + menuCount);

				for (int m = 0 ; m < menuCount ; m++) {
					String menuTxt = api.GetMenuItemText(hmenu, m);
					System.out.println("Menu Text: " + menuTxt);
				}
	    	}
		}
		
		rect = new RECT();
        output("Calling GetWindowRect");
		User32Ex.instance.GetWindowRect(hWnd, rect);
		output("GetWindowRect returned: " + rect.toString());
		
		if (isChild) {
			output("Calling GetParent");
			parent = User32Ex.instance.GetParent(hWnd);
			parentStr = Api.GetHandleAsString(parent);
			output("GetParent returned: " + parentStr);
		}
		else {
			//User32.instance.GetWindowModuleFileName(hWnd, path, 512);
			//IntByReference pointer = new IntByReference();
			PointerByReference pointer = new PointerByReference();
			output("Calling GetWindowThreadProcessId");
			User32Ex.instance.GetWindowThreadProcessId(hWnd, pointer);
			pid =  pointer.getPointer().getInt(0);
			output("GetWindowThreadProcessId returned: " + pid);
		    Pointer process = Kernel32Ex.instance.OpenProcess(Api.PROCESS_QUERY_INFORMATION | Api.PROCESS_VM_READ, false, pointer.getPointer());
		    //output("OpenProcess returned: " + process.getLong(0));
		    output("Calling GetModuleBaseNameW");
		    PsapiEx.instance.GetModuleBaseNameW(process, null, buffer2, 512);
		    processName = Native.toString(buffer2);
		    output("GetModuleBaseNameW returned: " + processName);
			//processName = Native.toString(path);
		}
		hwndStr = Api.GetHandleAsString(hWnd);
		output("GetHandleAsString returned: " + hwndStr);
		return hwndStr;
	}
	
	@Test
	public void enumerateParentWindowsTest() {
		final Map<String, WindowInfo> infoList = new LinkedHashMap<String, WindowInfo>();

	    class ParentWindowCallback implements WinUser.WNDENUMPROC {
			@Override
			public boolean callback(HWND hWnd, Pointer lParam) {
				output("Getting window info " + Api.GetHandleAsString(hWnd));
				//WindowInfo wi = new WindowInfo(hWnd, false);
				infoList.put(getWindow32Information(hWnd, false), null);
				//Api.User32.instance.EnumChildWindows(hWnd, new ChildWindowCallback(), new Pointer(0));
				return true;
			}
	    }	    
	    Api.User32Ex.instance.EnumWindows(new ParentWindowCallback(), 0);
	    output("enumerateParentWindowsTest size: " + infoList.size());
	    assertTrue(infoList.size() > 0);
	}

	@Test
	public void getXmlTest() {
		String xml = WindowsEnumeratedXml.getXml();
		output("getXmlTest len: " + xml.length());
		assertTrue(xml.length() > 500);
	}
	
	public static void main(String[] args) {
		output("WinApiTest");
		WinApiTest wat = new WinApiTest();
		wat.enumerateParentWindowsTest();
		wat.getXmlTest();
		output("done.");
		
	}

}
