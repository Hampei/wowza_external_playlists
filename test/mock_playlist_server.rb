#!/usr/bin/env ruby

require 'rubygems'
require 'sinatra'

class Show
  attr_reader :name
  attr_reader :tracks

  def initialize(name,tracks)
    @name = name
    @current = -1
    @tracks = tracks
  end
  
  def next_track
    ret = @tracks[@current+=1]
    if ret == nil 
      @current = -1
    end
    ret
  end

  def self.create_shows(shows)
    @@shows = shows.reduce({}) { |acc,s| acc[s.name] = s ; acc }
    # POST shows/app/id?pop_url=http://full_path_to_pop_url
  end
end


SHOWS = Show.create_shows [Show.new('foo',['jingle1.mp3','jingle2.mp3']),
                           Show.new('bar',['jingle1.mp3'])]

puts SHOWS.inspect

get "/shows/:show/pop_track.json" do 
  s = SHOWS[params[:show]]
    track = s.next_track
  if track
    puts "Popped track #{track} off playlist for show #{s}"
    "{bucket: 'lat30-jingles', filename: '#{track}', 'delete_after_play?': false}"
  else
    puts "No more tracks in playlist for show #{s}, returning 404...  Next access will restart the playlist #{s.playlist}"
    halt 404, "No more tracks"
  end
  
end


