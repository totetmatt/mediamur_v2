# Mediamur V2

## Disclamer

The purpose of this tools is to ease the visualization of media (photo, video) in twitter in real time.

Mediamur is reflecting the video activity of twitter users. The Twitter policy, at least on the streaming api used by Mediamur, on media and video is quite lax on the content that could be send. Mediamur doesn't apply any extra filter from what it receive from the API and show "raw result" from the twitter api. 

**Please be aware** that you could be exposed to medias that contains adult topic or shocking/distrubing graphical content. Use the tool in the correct place with the adequate participants as it could contain unadapted contents for certain public.

**By downloading and running the application, you understand the risk exposed and acknowledge that Mediamur won't be responsible of the content of the media fetched** 

## Twitter API

You will need to create a Twitter API Application Key https://developer.twitter.com/en/apps.

## Prerequisites

Mediamur needs Java to run.

## Quickstart

* Download here
* Unzip somewhere in your disk
* Edit the `conf/application.conf`
*   ```yaml
    mediamur {
      twitter {
        bearer_key = ""
      }
    
    }
    ```

* With a console, go to `cd $PATH_WHERE_YOU_UNZIP/mediamur/` and run `bin/mediamur` (or windows `bin\mediamur.bat`)
* Open a web browser to `http://localhost:48099/`

