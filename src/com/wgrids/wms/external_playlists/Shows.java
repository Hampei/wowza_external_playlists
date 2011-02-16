package com.wgrids.wms.external_playlists;

import com.wowza.wms.server.*;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.application.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Shows implements IServerNotify2 {
	
//	private static String server_url = null;
	private static Map<String, Show> shows = new ConcurrentHashMap<String, Show>();
	public void onServerInit(IServer server) {
//		server_url = Server.getInstance().getProperties().getPropertyStr("server_url"); 
//		publish_server_start();

//		createShow("stream1", "");
//		stream1.play("mp3:wacky.mp3", 0, -1, true);
//		stream1.setRepeat(false);
//		stream1.addListener(new StreamListener(appInstance));

	}
	
	/**
	 * Create a new show with given id and pop_url.
	 * Does nothing if show_id already exists (won't update pop_url).
	 */
	public static boolean create_show(IApplication app, String id, String pop_url) {
		String sid = app.getName() + '/' + id;
		if (shows.containsKey(sid)) {
//			Show s = shows.get(id);
//			s.check_playing();
			// for now do nothing, the play loop should never end unless with a stream destroy.
		} else {
			Show show = new Show(app, id, pop_url);
			shows.put(sid, show);
			WMSLoggerFactory.getLogger(null).info("added show " + sid);
		}
		return true;
	}
	/**
	 * remove the specified show and stop the stream. 
	 */
	public static void remove_show(String app, String id) {
		String sid = app + '/' + id;
		Show show = shows.get(sid);
		if(show != null) {
			show.close();
			shows.remove(sid);
			WMSLoggerFactory.getLogger(null).info("removed show " + sid);
		} else {
		//log
		}
	}
	public static Map<String, Show> getShows() {
		return shows;
	}
	
	public void onServerCreate(IServer server) {
	}
	public void onServerShutdownComplete(IServer server) {
	}
	public void onServerConfigLoaded(IServer server) {
	}	
	public void onServerShutdownStart(IServer server) {
	}

//	private void publish_server_start() {
//	String callback_server = Server.getInstance().getProperties().getPropertyStr("server_notify_callback");
//	callback_server = callback_server.replace("$action", "start");
//	WMSLoggerFactory.getLogger(null).info(server_url);
//	callback_server = callback_server.replace("$wowza_url", server_url);
//	hit_url(callback_server);
//}
//
//private void hit_url(String url) {
//    URL u;
//	HttpURLConnection uc;
//	try {
//		u = new URL(url);
//		uc = (HttpURLConnection) u.openConnection();
//        BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
//        in.close();
//	} catch (MalformedURLException e) {
//		WMSLoggerFactory.getLogger(null).fatal(url, e); 
//	} catch (IOException e) {
//		// probably down, they will contact us when ready.
//		WMSLoggerFactory.getLogger(null).warn("You can ignore the next warning if the streamer is not online at the moment. ");
//		WMSLoggerFactory.getLogger(null).warn(url, e); 
//	} 
//}
	
}
