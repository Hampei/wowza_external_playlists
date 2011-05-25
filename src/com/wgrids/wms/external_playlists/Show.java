package com.wgrids.wms.external_playlists;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import com.wowza.wms.application.IApplication;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.logging.*;
import com.wowza.wms.stream.publish.IStreamActionNotify;
import com.wowza.wms.stream.publish.PlaylistItem;
import com.wowza.wms.stream.publish.Stream;
import com.wowza.wms.vhost.ThreadPool;
import com.wowza.wms.amf.AMFDataMixedArray;
import com.wowza.wms.amf.AMFDataItem;

public class Show extends com.wowza.wms.stream.publish.Stream implements IStreamActionNotify {
    private static final WMSLogger logger = WMSLoggerFactory.getLogger(Show.class);
	private Stream stream = null;
	private String pop_url = null;
	private String id = null;
	private IApplication app = null;
	/** there might still be callbacks scheduled after closing the stream
	 * We can check this var to stop the loop.
	 */
	private boolean closed = false; 
	public boolean pop_error = false; 
	public String last_action = "";
	private JSONObject current_track = null;
	private static ThreadPool pop_track_thread_pool = null;
	
	/**
	 * Create underlying stream and queue the first play_next_track.
	 * @param id Name of the stream.
	 * @param pop_url The url to request the next track from.
	 */
	Show(IApplication app, String id, String pop_url)
	{
		IApplicationInstance appInstance = app.getAppInstance("_definst_");
		stream = Show.createInstance(appInstance, id);
		stream.setRepeat(false);
		stream.addListener(this);
		this.pop_url = pop_url;
		this.id = id;
		this.app = app;
		schedule_play_next_track();
	}
	
	/* Schedule play_next_track in the threadpool of the server, 
	 * so it won't block the thread on errors or on creation. 
	 */
	private void schedule_play_next_track() {
		if (closed) return;
		
		pop_track_thread_pool.execute(new Runnable() {
			@Override public void run() {
                                play_next_track(); 
		} });
	}
	
	/**
	 * Request the next track from the trackmanager and play it. 
	 * When a 404 is returned, delete this show.
	 * On any other error: log it and try again in 0.5s.
	 */
	private void play_next_track() {
		try {
			last_action = "get next track";
			current_track = new JSONObject(get_url(pop_url));
			WMSLoggerFactory.getLogger(null).info(current_path() + " on " + id);
			stream.play("mp3:" + current_path(), 0, -1, true);
			pop_error = false;
			last_action = "play " + current_path();
			
		} catch (Callback404Exception e) {
			last_action = "remove show";
			Shows.remove_show(app.getName(), id);
                        return;
		} catch (IOException e) { // streamer offline?
			WMSLoggerFactory.getLogger(null).error("streamer unreachable (IOException in get_url) in " + id);
			WMSLoggerFactory.getLogger(null).error("asdf", e);
			pop_error = true;
			wait(500);
			schedule_play_next_track();
		} catch (Exception e) {
			WMSLoggerFactory.getLogger(null).error("Unexpected exception in " + id, e );
			pop_error = true;
			wait(500);
			schedule_play_next_track();
		}
	}
	
	/**
	 * Delete the file we're currently playing if delete_after_play == true
	 */
	private void maybe_delete_current_track() {
		try {
			if (current_track.getBoolean("delete_after_play?")){
				boolean b = new File(current_absolute_path()).delete();
				last_action += " -> delete";
				if (!b)
					WMSLoggerFactory.getLogger(null).error("Delete file returned false " + id );
				else
					WMSLoggerFactory.getLogger(null).info("Deleted file " + current_absolute_path() );
			}
		} catch (JSONException e) {
			WMSLoggerFactory.getLogger(null).error("Json exception " + id, e );
			/* delete_after_play not found, so we don't delete.
		   	   if current_track was the reason for the fail, the track would not have been played. */ 
		} catch (Exception e) {
			/* Just making sure streams don't die by unknown errors */
			WMSLoggerFactory.getLogger(null).error("Unexpected exception in " + id, e );
		}
	}
	
	private String current_absolute_path() throws JSONException {
			return this.app.getAppInstance("_definst_").getStreamStoragePath() + '/' + current_path();
	}
	
	private String current_path() throws JSONException {
		return "" + current_track.getString("bucket") + '/' + current_track.getString("filename");
	}
	
	/**
	 * Thread.sleep, but just returns when InterruptedException is thrown.
	 * @param ms
	 */
	private static void wait(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e1) {}
	}
	
//	private String s3bucket() {
//		return Server.getInstance().getProperties().getPropertyStr("s3bucket");
//	}
	
	public void onPlaylistItemStop(Stream stream, PlaylistItem item)
	{
		WMSLoggerFactory.getLogger(null).info("Track finished: " + item.getName() + "on Stream: " + stream.getName());
		maybe_delete_current_track();
		play_next_track();
	}
	public void onPlaylistItemStart(Stream stream, PlaylistItem item) 
	{
		WMSLoggerFactory.getLogger(null).info("Item Started: " + item.getName() + "on Stream: " + stream.getName());
		send_stream_title();
	}
	private void send_stream_title() {
		AMFDataMixedArray data = new AMFDataMixedArray();
		try {
			String stream_title = current_track.getString("stream_title");
			byte[] title2 = stream_title.getBytes(java.nio.charset.Charset.defaultCharset().name());
			data.put("StreamTitle", new AMFDataItem(new String(title2, "UTF-8")));
			stream.getPublisher().getStream().send("onMetaData", data);
		} catch (java.io.UnsupportedEncodingException e) {
		} catch (JSONException e) {}

	}
	
	/**
	 * Get the body of the given url.
	 * @param url
	 * @return
	 * @throws IOException when the connection fails or dies
	 * @throws Callback404Exception when 404 is returned by the other side. 
	 */
	public String get_url(String url) throws IOException, Callback404Exception {
		String ret = "";
        URL u = new URL(url);
        HttpURLConnection uc = (HttpURLConnection) u.openConnection();
		uc.setConnectTimeout(5000);
		uc.setReadTimeout(5000);

        if( uc.getResponseCode() == 404) {
           	WMSLoggerFactory.getLogger(null).info("404 returned");
          	throw new Callback404Exception(id);
        }
        
        BufferedReader in = new BufferedReader(new InputStreamReader(
          uc.getInputStream()));
        String inputLine;

        while ((inputLine = in.readLine()) != null) 
            ret += inputLine;
        in.close();
        WMSLoggerFactory.getLogger(null).debug(ret);
        return ret;
	}
	
	public class Callback404Exception extends java.lang.Exception {
		private static final long serialVersionUID = 1L;

		Callback404Exception(String msg){
			super(msg);
		}
	}

	public void close() {
		super.close();
		closed = true;
		stream.close();
	}

	{
		pop_track_thread_pool = new ThreadPool(null, "pop_retry");
		pop_track_thread_pool.init(5);
	}

}
