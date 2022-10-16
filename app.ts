'use strict';


class MediaWall {
    stacks: any = {};
    counts: any = {};
    medias: any = {};
    node: HTMLElement;
    player: HTMLElement;
    constructor(node:HTMLElement, player:HTMLElement) {
        this.node = node;
        this.player = player;
    }
    offer_media(data:JSON) {
        let media = this.medias[data['id']];
        if( media === undefined) {
            media = new MediamurMedia(data['id'],
                                    data['kind'],
                                    data['preview_url'],
                                    data['url'],
                                    data['hit']);
           const player = this.player;
           media.node.onclick = function(e) {
                   player.innerHTML = "";
                   if(media.kind === "photo") {
                         const img = document.createElement("img");
                         img.src= media.url;
                         img.loading="lazy";
                         player.append(img);
                   }
                   if(media.kind === "video" || media.kind==="animated_gif") {

                        const video = document.createElement("video");
                        video.muted=true;
                        video.autoplay=true;
                        video.controls=true;
                        video.height=320;

                        const source = document.createElement("source");
                        source.src= media.url;
                        source.type = "video/mp4";

                         video.append(source);
                         player.append(video);
                   }
                   fetch(`tweet/${media.id}`)
                     .then((response) => response.json())
                     .then((data) => {
                            const tweetsHtml = document.createElement("ul");
                            for(let idx in data){
                              const tweet = data[idx];
                              const text = tweet[0];
                              const users = tweet[1];
                              const nb_users = users.length;
                              let tweetUser = {};

                              const tweetHtml =  document.createElement("li");
                              tweetHtml.append(document.createTextNode(`${nb_users} : ${text}`));
                              tweetsHtml.append(tweetHtml);
                            }
                            player.append(tweetsHtml);

                     })
            }
           this.medias[data["id"]] = media;

        } else {

            let old_stack = this.stacks[media.hit];
            if(old_stack !== undefined){
                old_stack.remove_media(media);
            }
            media.hit = data['hit']
        }
        let new_stack = this.stacks[media.hit];
        if(new_stack === undefined) {
            this.stacks[media.hit] = new WallStack(media.hit,this.node);
            new_stack = this.stacks[media.hit];
        }
        new_stack.offer_media(media);
    }
}

class WallStack {
    id: number
    list: any
    node : HTMLElement
    root: HTMLElement
    element_id() {
        return `wall-stack-${this.id}`
    }
    className() {
        return "pure-g wall-stack";
    }
    constructor(id:number, parent:HTMLElement) {
        this.id = id;
        this.list = {};

        this.root = document.createElement('div');
        this.root.id = `wall-stack-${this.id}`;
        this.root.className = this.className();

        const label_div = document.createElement("div");
        label_div.className ="pure-u-1-24 wall-stack-count";
        label_div.innerHTML = `${this.id}`;
        this.root.append(label_div);

        this.node = document.createElement("div");

        this.node.className = "pure-u-23-24 wall-stack-media";
        this.root.append(this.node);
        let stackUnder = null;
        for(let i=(id-1);i>0;i--){
            let t = document.getElementById(`wall-stack-${i}`);
            if(t!==null) {
                stackUnder = t;
                break;
            }
        }
        parent.insertBefore(this.root, stackUnder);
    };

    remove_media(media:MediamurMedia){
        delete this.list[media.id];
        //this.node.removeChild(media.node);
        if(Object.keys(this.list).length <= 0) {
             this.root.className="invisible";
        }

    };

    offer_media(media:MediamurMedia) {
        this.root.className = this.className();
        this.list[media.id] = true
        this.node.insertBefore(media.node, this.node.firstChild);
    };

}

class MediamurMedia {
    id: string;
    kind: string;
    preview_url: string;
    url: string;
    hit: number;
    node: HTMLImageElement;
    constructor(id :string, kind:string, preview_url:string, url:string, hit:number) {
        this.id = id;
        this.kind = kind,
        this.preview_url = preview_url;
        this.url = url;
        this.hit = hit;
        this.node = document.createElement("img");
        this.node.className = this.kind;
        this.node.id = `media_${id}`
        this.node.src = this.preview_url;

    };
    static from(json_str) {
        let json_data = JSON.parse(json_str);
        return new MediamurMedia(
            json_data['id'],
            json_data['kind'],
            json_data['preview_url'],
            json_data['url'],
            json_data['hit']
        )
    };
}



(function() {
    const streamStartStopToggle = document.getElementById("streamStartStopToggle") as HTMLInputElement
    const isSample = document.getElementById("isSample") as HTMLInputElement

    streamStartStopToggle.onchange = function () {
     let url = streamStartStopToggle.checked ? "/stream/start":"/stream/stop";
     let checked =  isSample.checked;
             fetch(url, {
                      method: 'POST',
                      cache: 'no-cache',
                      credentials: 'same-origin',
                      headers: {
                        'Content-Type': 'application/json'
                      },
                      redirect: 'follow',
                      body: JSON.stringify({"isSample":checked})
                    })

    }
    const ruleList = document.getElementById("rule-list");
    const refreshRules = function () {

              fetch('/rules')
              .then((response) => response.json())
              .then((data) => {
                          ruleList.innerHTML="";
                          for(let idx in data['data']) {
                                const rule = data['data'][idx];
                                const ruleElement = document.createElement("li");
                                const deleteButton = document.createElement("button")

                                deleteButton.textContent = "X"
                                deleteButton.className = "button-xsmall button-error pure-button"
                                deleteButton.onclick = function () {
                                    console.log(rule['id'])
                                     fetch("/rules", {
                                                method: 'DELETE',
                                                cache: 'no-cache',
                                                credentials: 'same-origin',
                                                headers: {
                                                  'Content-Type': 'application/json'
                                                },
                                                redirect: 'follow',
                                               body: JSON.stringify({"tag":"","value":"","id":rule['id']})
                                              }).then((response) => response.json())
                                                .then((data) => {
                                                    refreshRules();
                                                    console.log('Success:', data);
                                                }).catch((error) => {
                                                    console.error('Error:', error);
                                                });
                                }

                                const text = document.createElement("span")
                                text.className= "rule"
                                text.append(document.createTextNode(` ${rule['tag']} : ${rule['value']}`));

                                ruleElement.append(deleteButton)
                                ruleElement.append(text)

                                ruleList.append(ruleElement)
                          }
                      }
              );
      }
    refreshRules();
    document.getElementById("add-rule-btn").onclick = function () {
        const tag: HTMLInputElement = document.getElementById("add-tag")  as HTMLInputElement
        const value: HTMLInputElement = document.getElementById("add-value")  as HTMLInputElement
         fetch("/rules", {
            method: 'POST',
            cache: 'no-cache',
            credentials: 'same-origin',
            headers: {
              'Content-Type': 'application/json'
            },
            redirect: 'follow',
           body: JSON.stringify({"tag":tag.value,"value":value.value,"id":""})
          }).then((response) => response.json())
            .then((data) => {
                refreshRules();
                tag.value = "";
                value.value = "";
                console.log('Success:', data);
            }).catch((error) => {
                console.error('Error:', error);
            });
    }

    const rootWallElement = document.getElementById("wall");
    const rootPlayerElement = document.getElementById("player");
    const wall = new MediaWall(rootWallElement, rootPlayerElement);

    const evtSource = new EventSource("/events");
    evtSource.onmessage = (event) => {
        //console.log(event)
    }

    function registerListener(type:string){
        evtSource.addEventListener(type, (event) => {

            const data = JSON.parse(event.data);
            wall.offer_media(data);
        });

    }
    registerListener("video");
    registerListener("photo");
    registerListener("animated_gif");
})()