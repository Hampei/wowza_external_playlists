`external_playlists` is a plugin for the wowza media server.  It
allows for new streams to be created from outside of wowza, which will
ask via a callback_url which track should be played next.

Usage
=====

Create a script that returns the next track to play in the following
format:

    {bucket: 'subdirectory_of_contents', filename: 'file.mp3', delete_after_play: false}

Create a new show
-----------------

note: The stream will always be added to the **default instance** of
the specified application.

    POST <<server>>/shows/<<app_name>>/<<show_id>>?pop_track_url=<<full_url_to_get_tracks_from>>

You can also leave out the `<<app_name>>/` if you have defined a
default_app_name (see Configure)

Delete a show
-------------

Either call

    DELETE <<server>>/shows/<<app_name>>/<<show_id>>?pop_track_url

or return status 404 from the `pop_track_url`.

List all show
-------------

This will list all show created by this plugin and if the fetching of
the last track was successful or not.

    GET <<server>>/shows

Install
=======

1. Install wowza media server  
   See http://www.wowzamedia.com/store.html

2. link the lib directory to the wowza lib directory  
   e.g. from the project directory 
   
   On Mac:
   
        $ ln -s /Library/WowzaMediaServer/lib
    
   On Linux (Ubuntu) (this will happen automatically if the symlink does not exist):
   
        $ ln -s /usr/local/WowzaMediaServer/lib

3. The build installs the jar directly into the WowzaMediaServer lib
   directory.  Doing this requires appropriate permissions:

        $ sudo ant

   The plugin `wowza_external_playlist.jar` will then be installed through the
   symlink into the wowza lib directory.

Configure
=========

Server.xml
----------

Add Shows to the server listeners, inside the `<ServerListeners>`
tags with the following XML Snippet:

    <ServerListener>
       <BaseClass>com.wgrids.wms.external_playlists.Shows</BaseClass>
    </ServerListener>

VHost.xml
---------

Add ShowManager to the `<HttpProviders>`

    <HTTPProvider>
       <BaseClass>com.wgrids.wms.external_playlists.ShowManager</BaseClass>
       <RequestFilters>shows*</RequestFilters>
       <AuthenticationMethod>digest</AuthenticationMethod>
    </HTTPProvider>

Optional: Add the default_app_name as one of the `<Properties>`.  This
is the application name to add the stream to if it isn't explicitly
provided.  Change `MY_APPLICATION_NAME` appropriately:

     <Property>
      <Name>default_app_name</Name>
      <Value>MY_APPLICATION_NAME</Value>
      <Type>String</Type>
     </Property>
