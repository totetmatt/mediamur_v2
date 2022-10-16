# Mediamur V2

## Disclamer

The purpose of this tools is to ease the visualization of media (photo, video) in twitter in real time.

Mediamur is reflecting the video activity of twitter users. It might contains unadapted contents for youth.

The tool doesn't have any control on what is send over Twitter. As **no** filter is applied, the results can contain sensitive, choking, or disturbing media.
Please be aware and use the tool in the correct place with the adequate participants.

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

