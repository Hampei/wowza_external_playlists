package com.wgrids.wms.external_playlists;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.wowza.wms.application.IApplication;
import com.wowza.wms.http.*;
import com.wowza.wms.logging.*;
import com.wowza.wms.vhost.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ShowManager extends HTTProvider2Base {
        private static final Logger logger = WMSLoggerFactory.getLogger(null);
    
        private static final ScheduledThreadPoolExecutor threadPool = new ScheduledThreadPoolExecutor(2);
	/**
	 * Handles
	 *   POST /app/id?pop_url=http://full_path_to_pop_url
	 *   DELETE /app/id
	 *   GET /
	 */
	public void onHTTPRequest(IVHost vhost, IHTTPRequest req, IHTTPResponse resp) {
		if (!doHTTPAuthentication(vhost, req, resp))
			return;
		WMSLoggerFactory.getLogger(null).info(req.getMethod() + " " + req.getRequestURI());
		String[] path = req.getRequestURL().split("/");
		StringWriter ret = new StringWriter();

		try {
			if (path.length == 1) {
				list_shows(ret);
			} else {
				show_request(ret, vhost, req, path);
			}
		} catch (RuntimeException e) {
			ret.write(e.toString());
			resp.setResponseCode(501);
                        logger.error("Error handling request", e);
		}

		try {
			byte[] outBytes = ret.toString().getBytes();
			resp.getOutputStream().write(outBytes);
		} catch (Exception e) {
                    logger.error("Error handling request", e);
		}
	}
	private void list_shows(StringWriter sw) {
		Map<String, Show> shows = Shows.getShows();
		sw.write("<html><head><script src='https://ajax.googleapis.com/ajax/libs/jquery/1.4.2/jquery.min.js'></script></head> <body><table>");
		for (String key : shows.keySet()) {
			sw.write("<tr><td>" + key + "</td><td>" + (shows.get(key).pop_error ? "error " + delete_show_link(key) : "ok") + 
					 "</td><td>(" + shows.get(key).last_action + ")</td></tr>");
		}
		sw.write("</table></body></html>");
	}

	private String delete_show_link(String key) {
		return "<a href='" + key + "' onclick=\"$.ajax({type: 'delete', url: $(this).attr('href')}); return false;\">Delete</a>";
	}

	private void show_request(final StringWriter sw, final IVHost vhost, final IHTTPRequest req, String[] path) {
		String app_name, show_id; 
		if (path.length == 2) {
			app_name = vhost.getProperty("default_app_name");
			show_id = path[1];
		} else {
			app_name = path[1];
			show_id = path[2];
		}
                
                final String showId = show_id;
		final IApplication app = vhost.getApplication(app_name);

		if ( "POST".equals(req.getMethod()) ) {
                        threadPool.schedule(new Runnable() { 
                                public void run() {
                                    WMSLoggerFactory.getLogger(null).info("creating show " + showId);
                                    Shows.create_show(app, showId, req.getParameter("pop_track_url"));
                                }
                            },1, TimeUnit.SECONDS);
                WMSLoggerFactory.getLogger(null).info("start show: " + showId + " pop: " + req.getParameter("pop_track_url"));
                sw.write("done");
                
		} else if ("DELETE".equals(req.getMethod())) {
			WMSLoggerFactory.getLogger(null).info("stop show " + show_id );
		    Shows.remove_show(app_name, show_id);
		    sw.write("done");
		} 
	}

}
