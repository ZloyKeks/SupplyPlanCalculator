var stompClient = null;
var app_context = "";
var firstCalculate = false;
let connectedFlag = false;
let user_key = makeid(10);
let visualResult_DTO = undefined;

function setConnected(connected) {
    connectedFlag = connected;
}

function connect(context) {
    if (connectedFlag) return;
    connectedFlag = true;
    app_context = context;
    var socket = new SockJS(context+'/websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/calc', function (StatusProcessMessageDTO) {
            let body = JSON.parse(StatusProcessMessageDTO.body);
            if (body.user_key === user_key) {
                if (body.type === "StartCalculate") {
                    const array = JSON.parse(body.jsonData);
                    array.forEach(element => {
                        console.log(element);
                        $('#progress_bars').append('<div id="name_'+element+'">'+element+'</div>');
                        $('#progress_bars').append('<div id="'+element+'" class="progress-bar" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="95">0%</div>');
                    });
                }
                if (body.type === "ProgressCalculate") {
                    const json = JSON.parse(body.jsonData);
                    var pb = document.getElementById(body.message);
                    var pb_name = document.getElementById('name_'+body.message);
                    json.percent = Math.round(json.percent);
                    pb.setAttribute("aria-valuenow",json.percent);
                    if (json.percent > 90) {
                        json.percent = 100;
                        pb_name.style.display="none";
                        pb.style.display="none";
                    }
                    pb.style.width=json.percent+"%";
                    pb.innerHTML = json.percent + "%";

                }
                if (body.type === "ProgressDopCalculate") {
                    const json = JSON.parse(body.jsonData);
                    var pb = document.getElementById(body.message);
                    json.percent = Math.round(json.percent);
                    pb.innerHTML = 'd-'+json.percent + "%";
                }
                if (body.type === "ProgressDopCalculate") {
                    const json = JSON.parse(body.jsonData);
                    var pb = document.getElementById(body.message);
                    json.percent = Math.round(json.percent);
                    pb.innerHTML = 'd-'+json.percent + "%";
                }
                if (body.type === "ProgressParse") {
                    const json = JSON.parse(body.jsonData);
                    json.percent = Math.round(json.percent);
                    var pb = document.getElementById('calculate_lbl');
                    pb.innerHTML =  'Parse-'+json.percent + "%"
                    if (json.percent >= 99) pb.innerHTML = 'Calculate Please Wait';
                }
                if (body.type === "ProgressExportResult") {
                    const json = JSON.parse(body.jsonData);
                    json.percent = Math.round(json.percent);
                    var pb = document.getElementById('calculate_lbl');
                    pb.innerHTML = 'ExportResult-' + json.percent + "%"
                }
                if (body.type === "FileWriteFinish") {
                    onAjaxSuccess(body.jsonData)
                }
            }
        });
    }, function(message) {
        setConnected(false)
    });

}

function dateToString(date) {
    var month = date.getMonth() + 1;
    var day = date.getDate();
    let hours = date.getHours();
    let minutes = date.getMinutes();
    let seconds = date.getSeconds();
    var dateOfString = "";
    dateOfString += date.getFullYear() + "-";
    dateOfString += (("" + month).length < 2 ? "0" : "") + month  + "-";
    dateOfString += (("" + day).length < 2 ? "0" : "") + day;
    dateOfString += "T"
    dateOfString += (("" + hours).length < 2 ? "0" : "") + hours + ":";
    dateOfString += (("" + minutes).length < 2 ? "0" : "") + minutes + ":";
    dateOfString += (("" + seconds).length < 2 ? "0" : "") + seconds;

    return dateOfString;
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

function arrayBufferToBase64( buffer ) {
    var binary = '';
    var bytes = new Uint8Array( buffer );
    var len = bytes.byteLength;
    for (var i = 0; i < len; i++) {
        binary += String.fromCharCode( bytes[ i ] );
    }
    return window.btoa( binary );
}

function toCalculate (link, context) {
    $('#upload_content').css("display","none");
    $('#calculate_content').css("display","block");
    $('#original_name').text(link);
    clickCalculate(context);
}

function toFirstCalculate (link, context) {
    $('#upload_content').css("display","none");
    $('#calculate_content').css("display","block");
    $('#original_name').text(link);
    clickFirstCalculate(context);
}

function clickFirstCalculate(context){
    const name = $('#original_name').text();
    var dop_l = $("#dop_l").val();
    var dop_w = $("#dop_w").val();
    var dop_h = $("#dop_h").val();
    var max_h = $("#max_h").val();
    $.get(
        context+"/rest/calculateFirst/"+name+"/"+user_key+"/"+dop_l+"/"+dop_w+"/"+dop_h+"/"+max_h,
        onAjaxSuccess,
    ).fail(function(error) {
        console.log(error);
//        $("#calculate_lbl").text("ERROR");
    });
}


function clickCalculate(context){
    const name = $('#original_name').text();
    var dop_l = $("#dop_l").val();
    var dop_w = $("#dop_w").val();
    var dop_h = $("#dop_h").val();
    var max_h = $("#max_h").val();
    $.get(
        context+"/rest/calculate/"+name+"/"+user_key+"/"+dop_l+"/"+dop_w+"/"+dop_h+"/"+max_h,
        onAjaxSuccess,
    ).fail(function(error) {
        console.log(error);
//        $("#calculate_lbl").text("ERROR");
    });
}

function onAjaxSuccess(data)
{
    const json = JSON.parse(data);
    visualResult_DTO = data;
/*
    console.log(Object.keys(json))
    const map = json.resultCalculateMap;
    const map_keys = Object.keys(map);
    map_keys.forEach(key => {
        console.log(key)
        const resultArray = map[key];
        resultArray.forEach(result => {
            console.log(result);
        })
    })
*/
    $('#download_link').attr("href", context+"/rest/download/"+json.resultFileName)
    $('#calculate_content').css("display","none");
    if (!firstCalculate) $('#download_content').css("display","block");
    $('#view3d_content').css("display","block");
}

function makeid(length) {
    var result           = '';
    var characters       = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    var charactersLength = characters.length;
    for ( var i = 0; i < length; i++ ) {
        result += characters.charAt(Math.floor(Math.random() *
            charactersLength));
    }
    return result;
}

// возвращает куки с указанным name,
// или undefined, если ничего не найдено
function getCookie(name) {
    let matches = document.cookie.match(new RegExp(
        "(?:^|; )" + name.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, '\\$1') + "=([^;]*)"
    ));
    return matches ? decodeURIComponent(matches[1]) : 0;
}

function setCookie(name, value, options = {}) {
    options = {
        path: '/',
    };

    if (options.expires instanceof Date) {
        options.expires = options.expires.toUTCString();
    }

    let updatedCookie = encodeURIComponent(name) + "=" + encodeURIComponent(value);

    for (let optionKey in options) {
        updatedCookie += "; " + optionKey;
        let optionValue = options[optionKey];
        if (optionValue !== true) {
            updatedCookie += "=" + optionValue;
        }
    }
    document.cookie = updatedCookie;
}

function deleteCookie(name) {
    setCookie(name, "", {
        'max-age': -1
    })
}

$( document ).ready(function() {
    $('#view3d_btn').click(function (){
        $('input[name="visualResult_DTO"]').val(visualResult_DTO);
        $('#form_viewer').submit();
/*
        $.ajax({
            url: "./viewer",
            type: "POST",
            contentType : "application/json",
            data: JSON.stringify(resultCalculateMap),
            success: function(data){
                const win = window.open();
                win.document.write(data);
            }
        });
*/
    });
});