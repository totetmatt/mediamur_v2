'use strict';
var MediaWall = /** @class */ (function () {
    function MediaWall(node, player) {
        this.stacks = {};
        this.counts = {};
        this.medias = {};
        this.node = node;
        this.player = player;
    }
    MediaWall.prototype.offer_media = function (data) {
        var media = this.medias[data['id']];
        if (media === undefined) {
            media = new MediamurMedia(data['id'], data['kind'], data['preview_url'], data['url'], data['hit']);
            var player_1 = this.player;
            media.node.onclick = function (e) {
                player_1.innerHTML = "";
                if (media.kind === "photo") {
                    var img = document.createElement("img");
                    img.src = media.url;
                    img.loading = "lazy";
                    player_1.append(img);
                }
                if (media.kind === "video" || media.kind === "animated_gif") {
                    var video = document.createElement("video");
                    video.muted = true;
                    video.autoplay = true;
                    video.controls = true;
                    video.height = 320;
                    var source = document.createElement("source");
                    source.src = media.url;
                    source.type = "video/mp4";
                    video.append(source);
                    player_1.append(video);
                }
                fetch("tweet/".concat(media.id))
                    .then(function (response) { return response.json(); })
                    .then(function (data) {
                    var tweetsHtml = document.createElement("ul");
                    for (var idx in data) {
                        var tweet = data[idx];
                        var text = tweet[0];
                        var users = tweet[1];
                        var nb_users = users.length;
                        var tweetUser = {};
                        var tweetHtml = document.createElement("li");
                        tweetHtml.append(document.createTextNode("".concat(nb_users, " : ").concat(text)));
                        tweetsHtml.append(tweetHtml);
                    }
                    player_1.append(tweetsHtml);
                });
            };
            this.medias[data["id"]] = media;
        }
        else {
            var old_stack = this.stacks[media.hit];
            if (old_stack !== undefined) {
                old_stack.remove_media(media);
            }
            media.hit = data['hit'];
        }
        var new_stack = this.stacks[media.hit];
        if (new_stack === undefined) {
            this.stacks[media.hit] = new WallStack(media.hit, this.node);
            new_stack = this.stacks[media.hit];
        }
        new_stack.offer_media(media);
    };
    return MediaWall;
}());
var WallStack = /** @class */ (function () {
    function WallStack(id, parent) {
        this.id = id;
        this.list = {};
        this.root = document.createElement('div');
        this.root.id = "wall-stack-".concat(this.id);
        this.root.className = this.className();
        var label_div = document.createElement("div");
        label_div.className = "pure-u-1-24 wall-stack-count";
        label_div.innerHTML = "".concat(this.id);
        this.root.append(label_div);
        this.node = document.createElement("div");
        this.node.className = "pure-u-23-24 wall-stack-media";
        this.root.append(this.node);
        var stackUnder = null;
        for (var i = (id - 1); i > 0; i--) {
            var t = document.getElementById("wall-stack-".concat(i));
            if (t !== null) {
                stackUnder = t;
                break;
            }
        }
        parent.insertBefore(this.root, stackUnder);
    }
    WallStack.prototype.element_id = function () {
        return "wall-stack-".concat(this.id);
    };
    WallStack.prototype.className = function () {
        return "pure-g wall-stack";
    };
    ;
    WallStack.prototype.remove_media = function (media) {
        delete this.list[media.id];
        //this.node.removeChild(media.node);
        if (Object.keys(this.list).length <= 0) {
            this.root.className = "invisible";
        }
    };
    ;
    WallStack.prototype.offer_media = function (media) {
        this.root.className = this.className();
        this.list[media.id] = true;
        this.node.insertBefore(media.node, this.node.firstChild);
    };
    ;
    return WallStack;
}());
var MediamurMedia = /** @class */ (function () {
    function MediamurMedia(id, kind, preview_url, url, hit) {
        this.id = id;
        this.kind = kind,
            this.preview_url = preview_url;
        this.url = url;
        this.hit = hit;
        this.node = document.createElement("img");
        this.node.className = this.kind;
        this.node.id = "media_".concat(id);
        this.node.src = this.preview_url;
    }
    ;
    MediamurMedia.from = function (json_str) {
        var json_data = JSON.parse(json_str);
        return new MediamurMedia(json_data['id'], json_data['kind'], json_data['preview_url'], json_data['url'], json_data['hit']);
    };
    ;
    return MediamurMedia;
}());
(function () {
    var streamStartStopToggle = document.getElementById("streamStartStopToggle");
    streamStartStopToggle.onchange = function () {
        var url = streamStartStopToggle.checked ? "/stream/start" : "/stream/stop";
        fetch(url, {
            method: 'POST',
            cache: 'no-cache',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/json'
            },
            redirect: 'follow'
        });
    };
    var ruleList = document.getElementById("rule-list");
    var refreshRules = function () {
        fetch('/rules')
            .then(function (response) { return response.json(); })
            .then(function (data) {
            ruleList.innerHTML = "";
            var _loop_1 = function (idx) {
                var rule = data['data'][idx];
                var ruleElement = document.createElement("li");
                var deleteButton = document.createElement("button");
                deleteButton.textContent = "X";
                deleteButton.className = "button-xsmall button-error pure-button";
                deleteButton.onclick = function () {
                    console.log(rule['id']);
                    fetch("/rules", {
                        method: 'DELETE',
                        cache: 'no-cache',
                        credentials: 'same-origin',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        redirect: 'follow',
                        body: JSON.stringify({ "tag": "", "value": "", "id": rule['id'] })
                    }).then(function (response) { return response.json(); })
                        .then(function (data) {
                        refreshRules();
                        console.log('Success:', data);
                    })["catch"](function (error) {
                        console.error('Error:', error);
                    });
                };
                var text = document.createElement("span");
                text.className = "rule";
                text.append(document.createTextNode(" ".concat(rule['tag'], " : ").concat(rule['value'])));
                ruleElement.append(deleteButton);
                ruleElement.append(text);
                ruleList.append(ruleElement);
            };
            for (var idx in data['data']) {
                _loop_1(idx);
            }
        });
    };
    refreshRules();
    document.getElementById("add-rule-btn").onclick = function () {
        var tag = document.getElementById("add-tag");
        var value = document.getElementById("add-value");
        fetch("/rules", {
            method: 'POST',
            cache: 'no-cache',
            credentials: 'same-origin',
            headers: {
                'Content-Type': 'application/json'
            },
            redirect: 'follow',
            body: JSON.stringify({ "tag": tag.value, "value": value.value, "id": "" })
        }).then(function (response) { return response.json(); })
            .then(function (data) {
            refreshRules();
            tag.value = "";
            value.value = "";
            console.log('Success:', data);
        })["catch"](function (error) {
            console.error('Error:', error);
        });
    };
    var rootWallElement = document.getElementById("wall");
    var rootPlayerElement = document.getElementById("player");
    var wall = new MediaWall(rootWallElement, rootPlayerElement);
    var evtSource = new EventSource("/events");
    evtSource.onmessage = function (event) {
        //console.log(event)
    };
    function registerListener(type) {
        evtSource.addEventListener(type, function (event) {
            var data = JSON.parse(event.data);
            wall.offer_media(data);
        });
    }
    registerListener("video");
    registerListener("photo");
    registerListener("animated_gif");
})();
