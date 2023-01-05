let sequenceEntry = [];
let selected = [];
let countSelected = 0;
let countSelectedDNA = 0;
let countSelectedAA = 0;
let countSelectedAlignment = 0;
let highlighted = false;
let highlightEntry = -1;
let highlightStart = -1;
let highlightEnd = -1;

let mousemoveEntry = 1;
let mousePos = {x: 0, y: 0};
let hits = [];
let currentHit = 0;

let stored = false;
let stickies;

//Helper functions
function updateSelectedCount() {
    countSelected = 0;
    countSelectedDNA = 0;
    countSelectedAA = 0;
    countSelectedAlignment = 0;
    for (var i = 0; i < selected.length; i++) {
        if (selected[i] === true) {
            countSelected++;
            if (sequenceEntry[i].data.type === 'aa')
                countSelectedAA++;
            else if (sequenceEntry[i].data.type === 'dna')
                countSelectedDNA++;
            else if (sequenceEntry[i].data.type === 'alignmentDNA' || sequenceEntry[i].data.type === 'alignmentProt')
                countSelectedAlignment++;
        }
    }
    if (countSelected === 0)
        $("#selectCount").text("none selected");
    else
        $("#selectCount").text(countSelected + " selected");
}

function closeNavigator() {
    $('#sortable').hide();
    $('#navigator').addClass("navHidden");
    $('#navArrow').removeClass('ui-icon ui-icon-triangle-1-s').addClass('ui-icon ui-icon-triangle-1-w');
    $('#sequenceArea').css("margin-left", "");
    $('#logo').css("margin-left", "");
    $('#footer').css("margin-left", "");
    updateSizeAll();
}

function openNavigator() {
    $('#sortable').show();
    $('#navigator').removeClass("navHidden");
    $('#navArrow').removeClass('ui-icon ui-icon-triangle-1-w').addClass('ui-icon ui-icon-triangle-1-s');
    $('#sequenceArea').css("margin-left", "200px");
    $('#logo').css("margin-left", "200px");
    $('#footer').css("margin-left", "200px");
    updateSizeAll();
}

function updateSizeAll() {
    for (var i = 0; i < sequenceEntry.length; i++) {
        if (sequenceEntry[i] !== null && sequenceEntry[i] !== "placeholder")
            sequenceEntry[i].updateSize();
    }
    setStickies();
}

function scrollToEntry(entry) {
    $('html, body').animate({scrollTop: $("#en" + entry).offset().top}, 500);
}

function scrollToBp(entry, pos) {
    $(window).scrollTop($("#en" + entry).offset().top + sequenceEntry[entry].getXY(pos).y - 100);
}

function deselectAll() {
    for (var i = 0; i < selected.length; i++) {
        if (selected[i] === true) {
            selected[i] = false;
            $("#en" + i).removeClass("entrySelected");
            $("#li" + i).removeClass("ui-selected");
        }
    }
    updateSelectedCount();
}

function downloadText(filename, text) {
    text = escapeQuotes(text);
    $("<form action='dnatoolsServletDownload' method='POST' target='_blank'>" +
        "<input type='hidden' name='filename' value='" + filename + "'>" +
        "<input type='hidden' name='text' value='" + text + "'>" +
        "</form>").appendTo('body').submit().remove();
}

function escapeQuotes(text) {
    return text.replace(/'/g, "&#39;").replace(/"/g, "&quot;");
}

function removeFindMarks() {
    hits = [];
    $('.markFind').remove();
    $('#findHitForward').hide();
    $('#findHitBack').hide();
    $('#findHits').text("");
}

function getDisplaypos(pos) {
    pos = parseInt(pos);
    var c = getDisplayOrder();
    for (var i = 0; i < c.length; i++) {
        if (c[i] === pos)
            return i;
    }
    return -1;
}

function getDisplayOrder() {
    var c = [];
    $('li').each(function () {
        var id = $(this).attr("id");
        if (id.substring(0, 2) === "li") {
            c.push(parseInt(id.substring(2)));
        }
    });
    return c;
}

function addSequence(seq, afterPos) {
    if (seq === null)
        return;
    removeFindMarks();
    if (afterPos === 'last') {
        afterPos = -1;
    } else if (afterPos === 'response') {
        afterPos = seq.pos;
        if (!(afterPos >= 0 && afterPos < sequenceEntry.length))
            afterPos = -1;
    } else if (!(afterPos >= 0 && afterPos < sequenceEntry.length)) {
        afterPos = -1;
    }
    var pos = sequenceEntry.length;
    //find unused slot
    for (var i = 0; i < sequenceEntry.length; i++) {
        if (sequenceEntry[i] === null) {
            pos = i;
            break;
        }
    }
    seq.pos = pos;
    selected[pos] = false;
    if (seq.type === 'dna') {
        var bla = "<div style=\"display:none\" class=\"entryDNA\" id=\"en" + pos + "\"><div style=\"display:inline-block;float:right;margin-left:20px\" class=\"ui-state-default ui-corner-all\" title=\"Delete entry\" id=\"icons\"><span class=\"ui-icon ui-icon-close\" onclick=\"deleteEntry(" + pos + ")\"></span></div><div style=\"display:inline-block;float:right\" class=\"ui-state-default ui-corner-all\" title=\"View settings\" id=\"icons\"><span class=\"ui-icon ui-icon-wrench\" id=\"vs" + pos + "\" onclick=\"viewSettings(" + pos + ")\"></span></div><div class=\"stickyTitle\"><h3 title=\"Doubleclick to change name\" class=\"entryTitle\" id=\"ti" + pos + "\">" + seq.name + "</h3><br><span class=\"info\" id=\"in" + pos + "\">" + seq.info + "</span> <span class=\"markInfo\" id=\"mi" + pos + "\"></span></div><svg class=\"entrySeq\" id=\"se" + pos + "\" width=\"100%\" height=\"300\"><g class=\"forms\"></g><g class=\"seqs\"></g></svg></div>";
        var nav = "<li class=\"liDNA\" id=\"li" + pos + "\"><div class='handle'><span class='ui-icon ui-icon-carat-2-n-s'></span></div>" + seq.name + "</li>";
        if (afterPos === -1) {
            $('#sequenceArea').append(bla);
            $('#sortable').append(nav);
        } else {
            $("#en" + afterPos).after(bla);
            $("#li" + afterPos).after(nav);
        }
        //svg[pos] = new sequenceSVG(document.getElementById("se" + pos), seq);
        sequenceEntry[pos] = new sequenceSVG($("#se" + pos), seq);
        sequenceEntry[pos].draw();
    } else if (seq.type === 'aa') {
        var bla = "<div style=\"display:none\" class=\"entryAA\" id=\"en" + pos + "\"><div style=\"display:inline-block;float:right\" class=\"ui-state-default ui-corner-all\" title=\"Delete entry\" id=\"icons\"><span class=\"ui-icon ui-icon-close\" onclick=\"deleteEntry(" + pos + ")\"></span></div><div class=\"stickyTitle\"><h3 title=\"Doubleclick to change name\" class=\"entryTitle\" id=\"ti" + pos + "\">" + seq.name + "</h3><br><span class=\"info\" id=\"in" + pos + "\">" + seq.info + "</span> <span class=\"markInfo\" id=\"mi" + pos + "\"></span></div><svg class=\"entrySeq\" id=\"se" + pos + "\" width=\"100%\" height=\"300\"><g class=\"forms\"></g><g class=\"seqs\"></g></svg></div>";
        var nav = "<li class=\"liAA\" id=\"li" + pos + "\"><div class='handle'><span class='ui-icon ui-icon-carat-2-n-s'></span></div>" + seq.name + "</li>";
        if (afterPos === -1) {
            $('#sequenceArea').append(bla);
            $('#sortable').append(nav);
        } else {
            $("#en" + afterPos).after(bla);
            $("#li" + afterPos).after(nav);
        }
        //svg[pos] = new sequenceSVG(document.getElementById("se" + pos), seq);
        sequenceEntry[pos] = new sequenceSVG($("#se" + pos), seq);
        sequenceEntry[pos].draw();
    } else if (seq.type === 'alignmentDNA' || seq.type === 'alignmentProt') {
        var bla = "<div style=\"display:none\" class=\"entryAlignment\" id=\"en" + pos + "\"><div style=\"display:inline-block;float:right\" class=\"ui-state-default ui-corner-all\" title=\"Delete entry\" id=\"icons\"><span class=\"ui-icon ui-icon-close\" onclick=\"deleteEntry(" + pos + ")\"></span></div><div class=\"stickyTitle\"><h3 title=\"Doubleclick to change name\" class=\"entryTitle\" id=\"ti" + pos + "\">" + seq.name + "</h3><br><span class=\"info\" id=\"in" + pos + "\">" + seq.info + "</span> <span class=\"markInfo\" id=\"mi" + pos + "\"></span></div><svg class=\"entryAlign\" id=\"al" + pos + "\" width=\"100%\" height=\"300\"><g class=\"forms\"></g><g class=\"seqs\"></g></svg></div>";
        var nav = "<li class=\"liAlignment\" id=\"li" + pos + "\"><div class='handle'><span class='ui-icon ui-icon-carat-2-n-s'></span></div>" + seq.name + "</li>";
        if (afterPos === -1) {
            $('#sequenceArea').append(bla);
            $('#sortable').append(nav);
        } else {
            $("#en" + afterPos).after(bla);
            $("#li" + afterPos).after(nav);
        }
        //svg[pos] = new alignmentSVG(document.getElementById("al" + pos), seq);
        sequenceEntry[pos] = new alignmentSVG($("#al" + pos), seq);
        sequenceEntry[pos].draw();
    } else if (seq.type === 'text') {
        var bla = "<div style=\"display:none\" class=\"entryText\" id=\"en" + pos + "\"><div style=\"display:inline-block;float:right\" class=\"ui-state-default ui-corner-all\" title=\"Delete entry\" id=\"icons\"><span class=\"ui-icon ui-icon-close\" onclick=\"deleteEntry(" + pos + ")\"></span></div><div class=\"stickyTitle\"><h3 title=\"Doubleclick to change name\" class=\"entryTitle\" id=\"ti" + pos + "\">" + seq.name + "</h3><br><span class=\"info\" id=\"in" + pos + "\">" + seq.info + "</span></div><p id=\"se" + pos + "\">" + seq.html + "</p></div>";
        var nav = "<li class=\"liText\" id=\"li" + pos + "\"><div class='handle'><span class='ui-icon ui-icon-carat-2-n-s'></span></div>" + seq.name + "</li>";
        if (afterPos === -1) {
            $('#sortable').append(nav);
            $('#sequenceArea').append(bla);
        } else {
            $("#en" + afterPos).after(bla);
            $("#li" + afterPos).after(nav);
        }
        sequenceEntry[pos] = new textSVG(seq);
    }
    $("#en" + pos).fadeIn("slow");
    if (sequenceEntry.length > 2) {
        $('#navigator').show();
        openNavigator();
    }
    setStickies();
    updateSizeAll();
    return pos;
}

function exchangeSequenceFromResponse(responseText) {
    var newSequences = $.parseJSON(responseText);
    for (var i = 0; i < newSequences.length; i++) {
        if (newSequences[i].type === 'dna' || newSequences[i].type === 'aa' || newSequences[i].type === 'alignmentDNA' || newSequences[i].type === 'alignmentProt') {
            sequenceEntry[newSequences[i].pos].data = newSequences[i];
            $('#ti' + newSequences[i].pos).text(newSequences[i].name);
            $('#in' + newSequences[i].pos).html(newSequences[i].info);
            console.log(newSequences[i].info);
            $('#li' + newSequences[i].pos).html("<div class='handle'><span class='ui-icon ui-icon-carat-2-n-s'></span></div>" + newSequences[i].name);
            sequenceEntry[newSequences[i].pos].redraw();
        } else if (newSequences[i].type === 'error') {
            alert2(newSequences[i].error);
        }
    }
    updateSizeAll();
    storeLocally();
}

function sendQueryPlaceholder(placeholderTitle, serverQuery, afterPos) {
    //Insert Placeholder
    var pos = sequenceEntry.length;
    for (var i = 0; i < sequenceEntry.length; i++) {
        if (sequenceEntry[i] === null) {
            pos = i;
            break;
        }
    }
    sequenceEntry[pos] = "placeholder";
    var bla = "<div style=\"display:none\" class=\"entryText\" id=\"en" + pos + "\"><h3 class=\"entryTitle\" id=\"ti" + pos + "\">" + placeholderTitle + "</h3><br><p id=\"se" + pos + "\"><img src=\"css/ajax-loader.gif\">Computation in progress</p></div>";
    var nav = "<li class=\"liText\" id=\"li" + pos + "\"><div class='handle'><span class='ui-icon ui-icon-carat-2-n-s'></span></div>" + placeholderTitle + "</li>";
    if (afterPos === -1) {
        $('#sortable').append(nav);
        $('#sequenceArea').append(bla);
    } else {
        $("#en" + afterPos).after(bla);
        $("#li" + afterPos).after(nav);
    }

    $("#en" + pos).fadeIn("slow");
    scrollToEntry(pos);

    serverQuery.pos = pos;

    $.ajax({
        url: 'dnatoolsServlet',
        data: serverQuery,
        type: 'POST',
        posValue: pos,
        insertAfterValue: afterPos,
        timeout: 25000
    })
        .done(function (responseText) {
            var answer = $.parseJSON(responseText);
            if (answer[0].type === 'error') {
                $("#se" + this.posValue).html("<span style=\"color: #cd0a0a\"><span class=\"ui-icon ui-icon-alert\" style=\"float: left; margin-right: .3em;\"></span> Error: " + answer[0].error + "</span><p><button id=\"button" + this.posValue + "\" onclick=\"deleteEntry(" + this.posValue + ")\">Remove</button></p>");
                $("#button" + this.posValue).button();
            } else {
                $("#en" + this.posValue).remove();
                $("#li" + this.posValue).remove();
                sequenceEntry[pos] = null;
                addSequence(answer[0], this.insertAfterValue);
            }
        })
        .fail(function (responseText) {
            $("#se" + this.posValue).html("<span style=\"color: #cd0a0a\"><span class=\"ui-icon ui-icon-alert\" style=\"float: left; margin-right: .3em;\"></span> Failed contacting server:<br>" + responseText.statusText + "</span><p><button id=\"button" + this.posValue + "\" onclick=\"deleteEntry(" + this.posValue + ")\">Remove</button></p>");
            $("#button" + this.posValue).button();
        });
}

function addSequencesFromResponse(responseText, afterPos) {
    var newSequences = $.parseJSON(responseText);
    var lastPos;
    for (var i = 0; i < newSequences.length; i++) {
        if (newSequences[i].type === 'dna' || newSequences[i].type === 'aa' || newSequences[i].type === 'alignmentDNA' || newSequences[i].type === 'alignmentProt' || newSequences[i].type === 'text') {
            lastPos = addSequence(newSequences[i], afterPos);
        } else if (newSequences[i].type === 'error') {
            alert2(newSequences[i].error);
        }
    }
    storeLocally();
    return lastPos;
}

function highlightSequence(entry, start, end, closeAccordion) {
    var isAlignment = false;
    if (sequenceEntry[entry].data.type === "alignmentDNA" || sequenceEntry[entry].data.type === "alignmentProt") {
        isAlignment = true;
    }
    dehighlightSequence();
    var first = (start < end ? start : end);
    var second = (start < end ? end : start);
    if (sequenceEntry[entry].data.type === "aa" || sequenceEntry[entry].data.type === "alignmentProt") {
        start = first;
        end = second;
    }
    sequenceEntry[entry].drawHighlight(first, second);
    if (!closeAccordion)
        $("#accordion").accordion({active: 3, event: "click"});
    var marklength = (second - first + 1);
    highlighted = true;
    highlightEntry = entry;
    if (isAlignment) {
        $("#mi" + entry).text("Marked Column: " + (marklength > 1 ? ((first + 1) + "-" + (second + 1)) : (first + 1)) + ": " + marklength + " bp");
        highlightStart = start;
        highlightEnd = end;
        var copytext = "";
        for (var i = 0; i < sequenceEntry[entry].data.seqs.length; i++) {
            copytext += sequenceEntry[entry].data.seqs[i].sequence.substring(highlightStart, highlightEnd + 1) + "\n";
        }
        selectText(copytext);
    } else {
        $("#mi" + entry).text("Marked: " + (marklength > 1 ? ((start + 1) + "-" + (end + 1)) : (start + 1)) + ": " + marklength + " bp");
        highlightStart = start;
        highlightEnd = end;
        selectText(sequenceEntry[entry].data.sequence.substring(first, second + 1));
    }
    deselectAll();
}


function selectText(text) { //enables ctrl+c after selection
    var b = document.getElementById("ignore2");
    b.value = text;
    b.select();
}

function dehighlightSequence() {
    if (highlighted) {
        for (var i = 0; i < sequenceEntry.length; i++) {
            if (sequenceEntry[i] !== null)
                sequenceEntry[i].removeHighlight();
        }
    }
    $(".markInfo").text("");
    highlighted = false;
    highlightEntry = -1;
    highlightStart = -1;
    highlightEnd = -1;
}

function viewSettings(entryNr) {
    removeFindMarks();
    vsButton = $("#vs" + entryNr);
    var x = vsButton.position().left;
    console.log(x);
    var y = vsButton.position().top + vsButton.height();
    $("#popUpMenu").html("<ul id=\"menu\">\n\
        <li><span class='ui-icon ui-icon-seek-start'></span>Reverse strand</li>\n\
        <li>Translation</li>\n\
    </ul>");
    var menu = $("#menu").menu().hide();
    menu.show().width(200).position({
        my: "right top",
        at: "right bottom",
        of: $("#vs" + entryNr),
        collision: "none"
    });


}

function deleteEntry(entryNr) {
    removeFindMarks();
    selected[entryNr] = null;
    sequenceEntry[entryNr] = null;
    $("#en" + entryNr).fadeOut("slow", function () {
        $(this).remove();
    });
    $("#li" + entryNr).remove();
    updateSelectedCount();
    //Clear everything if no sequences left
    for (var i = 0; i < sequenceEntry.length; i++) {
        if (sequenceEntry[i] !== null)
            return;
    }
    sequenceEntry = [];
    selected = [];
    storeLocally();
}

//Store sequences local storage
function storeLocally() {
    if (typeof (Storage) !== "undefined") {
        var order = getDisplayOrder();
        var sequencesOrdered = [];
        for (var i = 0; i < order.length; i++) {
            sequencesOrdered.push(sequenceEntry[order[i]].data);
        }
        localStorage.setItem("seqs", JSON.stringify(sequencesOrdered));
        stored = true;
    }
}

//Show documentation: modal window with iframe
function showDoc(url) {
    document.getElementById('help-modal').innerHTML = "<iframe id='showDocIframe' height='100%' width='95%' src=\"" + url + "\" frameBorder='0'>";
    $('#help-modal').dialog({
        modal: true,
        width: "900",
        height: "600",
        buttons: {
            "Open in Tab": function () {
                window.open($('#showDocIframe').contents().get(0).location.href);
                $(this).dialog("close");
            },
            Close: function () {
                $(this).dialog("close");
            }
        }
    });
    //Adjust iFrameHeight for consistent scrollbars
    $('#showDocIframe').load(function () {
        var iFrameID = document.getElementById('showDocIframe');
        $("#showDocIframe").contents().find("#docHeader").hide();
        if (iFrameID) {
            iFrameID.height = (iFrameID.contentWindow.document.body.scrollHeight + 40) + "px";
        }
    });
}


function showPluginPopup(htmlURI, title, data) {
    document.getElementById('plugin-modal').innerHTML = "<iframe id='showPopupIframe' height='100%' width='100%' src=\"" + htmlURI + "\" frameBorder='0'>";
    $('#plugin-modal').dialog({
        modal: true,
        width: "900",
        height: "600",
        buttons: {
            Close: function () {
                $(this).dialog("close");
            }
        }
    });
    $('#plugin-modal').dialog('option', 'title', title);
    //Adjust iFrameHeight for consistent scrollbars
    $('#showPopupIframe').load(function () {
        var iFrameID = document.getElementById('showPopupIframe');
        iFrameID.height = "98%";
        iFrameID.width = "98%";
//        if (iFrameID) {
//            iFrameID.height = (iFrameID.contentWindow.document.body.scrollHeight + 20) + "px";
//        }
        iFrameID.contentWindow.initialize(data);
    });

}

//Show alert: modal window
function alert2(text) {
    document.getElementById('alert-modal').innerHTML = "<div class= \"ui-widget\"><div class=\"ui-state-error ui-corner-all\" style=\"padding: 0 .7em;\"><p><span class=\"ui-icon ui-icon-alert\" style=\"float: left; margin-right: .3em;\"></span>" + text + "</p></div></div>";
    $('#alert-modal').dialog({
        modal: true,
        width: "800",
        buttons: {
            Close: function () {
                $(this).dialog("close");
            }
        }
    });
}


function findHitForward() {
    currentHit++;
    if (currentHit >= hits.length)
        currentHit = 0;
    highlightSequence(hits[currentHit].entry, hits[currentHit].start, hits[currentHit].end, true);
    scrollToBp(hits[currentHit].entry, hits[currentHit].start);
}

function findHitBack() {
    currentHit--;
    if (currentHit < 0)
        currentHit = hits.length - 1;
    highlightSequence(hits[currentHit].entry, hits[currentHit].start, hits[currentHit].end, true);
    scrollToBp(hits[currentHit].entry, hits[currentHit].start);
}


//Styles

$(document)
    .ajaxStart(function () {
        $('#loadSpinner').show();
    })
    .ajaxStop(function () {
        $('#loadSpinner').hide();
    });
$(function () {
    $(window).resize(function () {
        updateSizeAll();
    });
    $("#addSequences").button();
    $("#accordion").accordion();
    $("#icons").hover(
        function () {
            $(this).addClass("ui-state-hover");
        },
        function () {
            $(this).removeClass("ui-state-hover");
        }
    );
    $("#radioset").buttonset();
    $('#findHitForward').hide();
    $('#findHitBack').hide();
    $("#sortable").sortable({
        handle: ".handle", stop: function () {
            //Navigation: change order
            $("#sortable li").each(function () {
                var sel = parseInt($(this).attr("id").substring(2));
                var entry = $("#en" + sel);
                entry.detach();
                $("#sequenceArea").append(entry);
                setStickies();
            });
        }
    })
        .selectable({
            filter: "li", cancel: ".handle", stop: function () {
                //Navigator: selection event
                var c = 0;
                var lastSelected = 0;
                $("#sortable li").each(function () {
                    var sel = parseInt($(this).attr("id").substring(2));
                    if ($(this).hasClass("ui-selected")) {
                        selected[sel] = true;
                        $("#en" + sel).addClass("entrySelected");
                        c++;
                        lastSelected = sel;
                    } else {
                        selected[sel] = false;
                        $("#en" + sel).removeClass("entrySelected");
                    }
                });
                updateSelectedCount();
                if (c === 1) {
                    scrollToEntry(lastSelected);
                }
            }
        })
        .find("li").addClass("ui-corner-all");
});

//Store seqs locally
$(window).bind('beforeunload', function () {
    storeLocally();
    if (!stored && c > 0) {
        return "Your project couldn't be saved on your computer. Are you sure you want to leave?";
    }
});

function welcomeWindow() { //welcome window for non-linked entry
    var html = '<img src="/css/genewarrior_dnatools.png"><br>';

    //Load local storage seqs
    var storedSeqsExist = false;
    var storedSeqsLength = 0;
    if (typeof (Storage) !== "undefined") {
        if (localStorage.getItem("seqs") !== null) {
            var storedSeqs = $.parseJSON(localStorage.getItem("seqs"));
            if (storedSeqs.length > 0) {
                storedSeqsLength = storedSeqs.length;
                storedSeqsExist = true;
            }
        }
    }

    html += "<table style=\"width:100%\"><tr><td>";

    if (storedSeqsExist === false) { //no previous sequences
        html += "<h3>Get started with GeneWarrior</h3>";
        html += "<p><a class=\"command\" onclick=\"showDoc('docs/index.html');$('#welcome-modal').dialog('close');\">See Tutorials</a></p>";
        html += "<p><a class=\"command\" onclick=\"$('#loadExample').click();$('#welcome-modal').dialog('close');\">Load some example sequences to try out</a></p>";
        html += "<p><a class=\"command\" onclick=\"$('#welcome-modal').dialog('close');\">Start with a blank workspace</a></p>";

    } else {//there are previous sequences
        html += "<h3>Welcome back</h3>";
        html += "<a class=\"command\" onclick=\"var storedSeqs = $.parseJSON(localStorage.getItem('seqs'));for (var i = 0; i < storedSeqs.length; i++) {addSequence(storedSeqs[i], -1);}$('#welcome-modal').dialog('close');\">Load your previous project (" + storedSeqsLength + " entries)</a><br>";
        html += "<p><a class=\"command\" onclick=\"$('#welcome-modal').dialog('close');\">Start with a blank workspace</a></p>";
        html += "<p><a class=\"command\" onclick=\"showDoc('docs/index.html');$('#welcome-modal').dialog('close');\">Show Tutorials</a></p>";
        html += "<p><a class=\"command\" onclick=\"$('#loadExample').click();$('#welcome-modal').dialog('close');\">Load some example sequences to try out</a></p>";
    }
    html += "</td><td>";
    html += "";
    html += "</td></tr></table>";

    document.getElementById('welcome-modal').innerHTML = html;
    $('#welcome-modal').dialog({
        modal: true,
        width: "800",
        buttons: {
            Close: function () {
                $(this).dialog("close");
            }
        }
    });
}

function makeforceWidth(width) {
    forceWidth = width;
    updateSizeAll();
}

function deforceWidth() {
    forceWidth = -1;
    updateSizeAll();
}

function loadFromDatabase(key) { //show welcome window and load project from database
    var html = '<img src="/css/genewarrior_dnatools.png"><br>';
    html += "<p id='inProgress'><img src=\"/css/ajax-loader.gif\">Retrieving linked project...</p>";
    html += "<p><a class=\"command\" onclick=\"showDoc('docs/index.html');$('#welcome-modal').dialog('close');\">Show Tutorials to get started with GeneWarrior</a></p>";
    document.getElementById('welcome-modal').innerHTML = html;
    $('#welcome-modal').dialog({
        modal: true,
        width: "800",
        buttons: {
            Close: function () {
                $(this).dialog("close");
            }
        }
    });

    $.post('readProject', {key: key})
        .done(function (responseText) {
            addSequencesFromResponse(responseText, 'last');
            $('#inProgress').html("<p>Successfully loaded stored project.<br>You can now modify the project, but remember to generate a new link (\"Share &amp; Export > Share project\") if you want to store and share your changes.</p>");
        })
        .fail(function (responseText) {
            alert2("Failed contacting server");
            $('#inProgress').remove();
        });
}

//Actions
$(document).ready(function () {
    //stickyTitles
    setStickies();
    $(window).on("scroll", function () {
        stickies.scroll();
    });

    //ctrl+f
    $(document).on("keydown", function (e) {
        if (e.keyCode === 70 && e.ctrlKey) {
            $("#accordion").accordion({active: 4, event: "click"});
            $('#findText').focus();
            event.stopPropagation();
        }
    });

    //press enter in find text field
    $('#findText').keyup(function (e) {
        if (e.keyCode === 13) {
            $('#findButton').click();
        }
    });

    //press enter in annotate text field
    $('#annotateText').keyup(function (e) {
        if (e.keyCode === 13) {
            $('#annotateOK').click();
        }
    });

    $('#loadSpinner').hide();

    //Click on Sequence
    var mousedownEntry;
    var mousedownPos;
    $('html').on("mousedown", function (event) {
        dehighlightSequence();
    }).on("mouseup", function (event) {
        dehighlightSequence();
    });

    //Exceptions for mark disabling
    $("#inputfield").on("mousedown", function (event) {
        event.stopPropagation();
    }).on("mouseup", function (event) {
        event.stopPropagation();
    }).on("click", function (event) {
        event.stopPropagation();
    });

    $("#findText").on("mousedown", function (event) {
        event.stopPropagation();
    }).on("mouseup", function (event) {
        event.stopPropagation();
        deselectAll();
    }).on("click", function (event) {
        event.stopPropagation();
    });

    //Highlight color
    $("#highlight1, #highlight2, #highlight3, #highlight4, #highlight5").on("mousedown", function (event) {
        event.stopPropagation();
    }).on("mouseup", function (event) {
        event.stopPropagation();
    }).on("click", function (event) {
        if (!highlighted || sequenceEntry[highlightEntry].data.type === "alignmentDNA" || sequenceEntry[highlightEntry].type === "alignmentProt") {
            return;
        }
        event.stopPropagation();
        var value = event.target.id.substring(9);
        $.post('dnatoolsServlet', {
            type: "highlightSequence",
            start: highlightStart,
            end: highlightEnd,
            color: value,
            pos: highlightEntry,
            text: JSON.stringify(sequenceEntry[highlightEntry].data)
        })
            .done(function (responseText) {
                exchangeSequenceFromResponse(responseText);
            })
            .fail(function (responseText) {
                alert2("Failed contacting server:" + responseText.statusText);
            });
        dehighlightSequence();
    });

    $("#annotatio101, #annotatio102, #annotatio103, #annotatio104, #annotatio105").on("mousedown", function (event) {
        event.stopPropagation();
    }).on("mouseup", function (event) {
        event.stopPropagation();
    }).on("click", function (event) {
        if (!highlighted) {
            return;
        }
        event.stopPropagation();
        var value = event.target.id.substring(9);

        var text = $("#annotateText").val();
        if (text.length === 0) {
            alert2("Enter Annotation-Text in Field");
            return;
        }
        $.post('dnatoolsServlet', {
            type: "highlightSequence",
            start: highlightStart,
            end: highlightEnd,
            annotation: text,
            color: value,
            pos: highlightEntry,
            text: JSON.stringify(sequenceEntry[highlightEntry].data)
        })
            .done(function (responseText) {
                exchangeSequenceFromResponse(responseText);
            })
            .fail(function (responseText) {
                alert2("Failed contacting server:" + responseText);
            });
        dehighlightSequence();
    });

    $("#annotateText").on("mousedown", function (event) {
        event.stopPropagation();
    }).on("mouseup", function (event) {
        event.stopPropagation();
    });

    //Remove formatting color
    $("#NoFormattingColor").on("mousedown", function (event) {
        event.stopPropagation();
    }).on("mouseup", function (event) {
        event.stopPropagation();
    }).on("click", function (event) {
        if (!highlighted) {
            if (countSelected < 1) {
                return;
            }
            for (var i = 0; i < sequenceEntry.length; i++) {
                if (selected[i] === true) {
                    sequenceEntry[i].data.display = sequenceEntry[i].data.sequence;
                    sequenceEntry[i].data.mark = [];
                    sequenceEntry[i].redraw();
                }
            }
            deselectAll();
            return;
        }
        if (sequenceEntry[highlightEntry].data.type === 'dna' || sequenceEntry[highlightEntry].data.type === 'aa') {
            $.post('dnatoolsServlet', {
                type: "highlightSequence",
                start: highlightStart,
                end: highlightEnd,
                color: 'removeColor',
                pos: highlightEntry,
                text: JSON.stringify(sequenceEntry[highlightEntry].data)
            })
                .done(function (responseText) {
                    exchangeSequenceFromResponse(responseText);

                })
                .fail(function (responseText) {
                    alert2("Failed contacting server:" + responseText);
                });
            dehighlightSequence();
        }
    });

    //Remove formatting
    $("#NoFormattingAnnotation").on("mousedown", function (event) {
        event.stopPropagation();
    }).on("mouseup", function (event) {
        event.stopPropagation();
    }).on("click", function (event) {
        if (!highlighted) {
            if (countSelected < 1) {
                return;
            }
            for (var i = 0; i < sequenceEntry.length; i++) {
                if (selected[i] === true) {
                    sequenceEntry[i].data.display = sequenceEntry[i].data.sequence;
                    sequenceEntry[i].data.mark = [];
                    sequenceEntry[i].redraw();
                }
            }
            deselectAll();
            return;
        }
        if (sequenceEntry[highlightEntry].data.type === 'dna' || sequenceEntry[highlightEntry].data.type === 'aa') {
            $.post('dnatoolsServlet', {
                type: "highlightSequence",
                start: highlightStart,
                end: highlightEnd,
                color: 'removeAnnotation',
                pos: highlightEntry,
                text: JSON.stringify(sequenceEntry[highlightEntry].data)
            })
                .done(function (responseText) {
                    exchangeSequenceFromResponse(responseText);

                })
                .fail(function (responseText) {
                    alert2("Failed contacting server:" + responseText);
                });
            dehighlightSequence();
        }
    });

    //Click on entry to select
    $("#sequenceArea").on("click", "div.entryDNA, div.entryAA, div.entryAlignment, div.entryText", function (event) {
        var clickedID = parseInt($(this).attr("id").substring(2));
        if (selected[clickedID] === true) {
            selected[clickedID] = false;
            $("#en" + clickedID).removeClass("entrySelected");
            $("#li" + clickedID).removeClass("ui-selected");
        } else if (selected[clickedID] === false) {
            selected[clickedID] = true;
            $("#en" + clickedID).addClass("entrySelected");
            $("#li" + clickedID).addClass("ui-selected");
        }
        updateSelectedCount();
    }).on("click", ".annotation", function (event) {
        var entry = $(this).parent().parent().parent().attr("id").substring(2);
        var pos = sequenceEntry[entry].getNucPosition(event.pageX - $(this).parent().parent().offset().left, event.pageY - $(this).parent().parent().offset().top);
        for (var i = 0; i < sequenceEntry[entry].data.mark.length; i++) {
            var m = sequenceEntry[entry].data.mark[i];
            if (m.type < 100)
                continue;
            if ((pos < m.stop && pos > m.start) || (pos > m.stop && pos < m.start)) {
                highlightSequence(entry, m.start, m.stop);
                event.stopPropagation();
                return;
            }
        }
    }).on("mousedown", "svg.entrySeq", function (event) {
        dehighlightSequence();
        //mousedownEntry = null;
        mousedownPos = null;
        mousedownEntry = parseInt($(this).attr("id").substring(2));
        mousedownPos = sequenceEntry[mousedownEntry].getNucPosition(event.pageX - $(this).offset().left, event.pageY - $(this).offset().top);
        if (mousedownPos === null) {
            mousedownEntry = null;
        }
        mousePos = {x: event.pageX, y: event.pageY};
        event.stopPropagation();
    }).on("mousedown", "svg.entryAlign", function (event) {
        dehighlightSequence();
        //mousedownEntry = null;
        mousedownPos = null;
        mousedownEntry = parseInt($(this).attr("id").substring(2));
        mousedownPos = sequenceEntry[mousedownEntry].getNucPosition(event.pageX - $(this).offset().left, event.pageY - $(this).offset().top);
        if (mousedownPos === null) {
            mousedownEntry = null;
        }
        mousePos = {x: event.pageX, y: event.pageY};
        event.stopPropagation();
    }).on("mouseup", "svg.entrySeq", function (event) {
        var mouseupEntry = parseInt($(this).attr("id").substring(2));
        var mouseupPos = sequenceEntry[mouseupEntry].getNucPosition(event.pageX - $(this).offset().left, event.pageY - $(this).offset().top);
        if (mouseupPos !== null && mousedownEntry === mouseupEntry && !(mousePos.x === event.pageX && mousePos.y === event.pageY)) {
            highlightSequence(mousedownEntry, mousedownPos, mouseupPos);
            event.stopPropagation();
        } else
            dehighlightSequence();
        mousedownEntry = null;
        mousedownPos = null;
    }).on("mouseup", "svg.entryAlign", function (event) {
        var mouseupEntry = parseInt($(this).attr("id").substring(2));
        var mouseupPos = sequenceEntry[mouseupEntry].getNucPosition(event.pageX - $(this).offset().left, event.pageY - $(this).offset().top);
        if (mouseupPos !== null && mousedownEntry === mouseupEntry && !(mousePos.x === event.pageX && mousePos.y === event.pageY)) {
            highlightSequence(mousedownEntry, mousedownPos, mouseupPos);
            event.stopPropagation();
        } else
            dehighlightSequence();
        mousedownEntry = null;
        mousedownPos = null;
    }).on("dblclick", ".entryTitle", function (event) {
        clickedID = parseInt($(this).attr("id").substring(2));
        var newName = prompt("Change name", sequenceEntry[clickedID].data.name);

        if (newName !== null) {
            $.post('dnatoolsServlet', {type: "validateName", text: newName})
                .done(function (responseText) {
                    if (responseText) {
                        responseText = responseText.replace(/(\r\n|\n|\r)/gm, "");

                        if (responseText.length === 0) {
                            alert2("No valid characters in new name");
                            return;
                        }
                        sequenceEntry[clickedID].data.name = responseText;
                        $('#ti' + clickedID).text(responseText);
                        $('#li' + clickedID).html("<div class='handle'><span class='ui-icon ui-icon-carat-2-n-s'></span></div>" + responseText);
                        setStickies();
                    }
                })
                .fail(function (responseText) {
                    alert2("Failed contacting server:" + responseText);
                    dehighlightSequence();
                });
        }
    }).on("mousemove", "svg.entrySeq", function (event) {
        mousemoveEntry = parseInt($(this).attr("id").substring(2));
        if (!highlighted) {
            var pos = sequenceEntry[mousemoveEntry].getNucPosition(event.pageX - $(this).offset().left, event.pageY - $(this).offset().top);
            if (pos !== null)
                $("#mi" + mousemoveEntry).text("Position: " + (pos + 1));
            else
                $("#mi" + mousemoveEntry).html("");
        }
        if (mousedownEntry === mousemoveEntry) {
            var pos = sequenceEntry[mousemoveEntry].getNucPosition(event.pageX - $(this).offset().left, event.pageY - $(this).offset().top);
            if (pos !== null) {
                highlightSequence(mousedownEntry, mousedownPos, pos);
                event.stopPropagation();
            }
        }
    }).on("mousemove", "svg.entryAlign", function (event) {
        mousemoveEntry = parseInt($(this).attr("id").substring(2));
        if (!highlighted) {
            var pos = sequenceEntry[mousemoveEntry].getNucPosition(event.pageX - $(this).offset().left, event.pageY - $(this).offset().top);
            if (pos !== null)
                $("#mi" + mousemoveEntry).text("Position: Column " + (pos + 1));
            else
                $("#mi" + mousemoveEntry).html("");
        }
        if (mousedownEntry === mousemoveEntry) {
            var pos = sequenceEntry[mousemoveEntry].getNucPosition(event.pageX - $(this).offset().left, event.pageY - $(this).offset().top);
            if (pos !== null) {
                highlightSequence(mousedownEntry, mousedownPos, pos);
                event.stopPropagation();
            }
        }
    });

    //Click on navigator
    $("#navigator").on("click", "h3", function (event) {
        if ($('#sortable').is(':visible')) {
            closeNavigator();
        } else {
            openNavigator();
        }
    });

    //Click on 'addSequences' button
    $('#addSequences').click(function () {
        $.post('dnatoolsServlet', {type: "addSequences", text: $('#inputfield').val()})
            .done(function (responseText) {
                addSequencesFromResponse(responseText, 'last');
                $('#inputfield').val("");
            })
            .fail(function (responseText) {
                alert2("Failed contacting server");
            });
    });

    //Click on 'loadExample' button
    $('#loadExample').click(function () {
        $.post('dnatoolsServlet', {type: "loadExample"})
            .done(function (responseText) {
                addSequencesFromResponse(responseText, 'last');
            })
            .fail(function (responseText) {
                alert2("Failed contacting server");
            });
    });

    ////////////Tools
    //Delete selected
    $('#deleteSelected').click(function () {
        if (countSelected === 0) {
            return;
        }
        if (countSelected > 1) {
            var r = confirm("Delete " + countSelected + " entries?");
            if (r === false) {
                return;
            }
        }
        for (var i = selected.length - 1; i >= 0; i--) {
            if (selected[i] === true)
                deleteEntry(i);
        }
    });

    //ReverseComplement
    $('#ReverseComplement').click(function () {
        if (countSelectedDNA < 1) {
            alert2("Select at least one DNA sequence to continue");
        } else {
            for (var i = 0; i < sequenceEntry.length; i++) {
                if (selected[i] === true && sequenceEntry[i].data.type === 'dna') {
                    sendQueryPlaceholder("Reverse Complement", {
                        type: "RevComp",
                        pos: i,
                        text: JSON.stringify(sequenceEntry[i].data)
                    }, i);
                }
            }
            deselectAll();
        }
    });
    $('#Reverse').click(function () {
        if (countSelectedDNA < 1) {
            alert2("Select at least one DNA sequence to continue");
        } else {
            for (var i = 0; i < sequenceEntry.length; i++) {
                if (selected[i] === true && sequenceEntry[i].data.type === 'dna') {
                    sendQueryPlaceholder("Reverse only", {
                        type: "Rev",
                        pos: i,
                        text: JSON.stringify(sequenceEntry[i].data)
                    }, i);
                }
            }
            deselectAll();
        }
    });
    $('#Complement').click(function () {
        if (countSelectedDNA < 1) {
            alert2("Select at least one DNA sequence to continue");
        } else {
            for (var i = 0; i < sequenceEntry.length; i++) {
                if (selected[i] === true && sequenceEntry[i].data.type === 'dna') {
                    sendQueryPlaceholder("Complement only", {
                        type: "Comp",
                        pos: i,
                        text: JSON.stringify(sequenceEntry[i].data)
                    }, i);
                }
            }
            deselectAll();
        }
    });

    //Translation
    $("#TranslateF1, #TranslateF2, #TranslateF3, #TranslateR1, #TranslateR2, #TranslateR3").on("click", function (event) {

        var value = event.target.id.substring(9);

        var c = 0;
        for (var i = 0; i < sequenceEntry.length; i++) {
            if (selected[i] === true && sequenceEntry[i].data !== null && sequenceEntry[i].data.type === 'dna') {
                c++;
                $.post('dnatoolsServlet', {
                    type: "translate",
                    translationTable: $('#translationTable').val(),
                    frame: value,
                    text: JSON.stringify(sequenceEntry[i].data)
                })
                    .done(function (responseText) {
                        var newSequences = $.parseJSON(responseText);
                        for (var i = 0; i < newSequences.length; i++) {
                            if (newSequences[i].type === 'aa') {
                                addSequencesFromResponse(responseText, 'response');
                            } else if (newSequences[i].type === 'error') {
                                alert2(newSequences[i].error);
                            }
                        }
                    })
                    .fail(function (responseText) {
                        alert2("Failed contacting server:" + responseText);
                        dehighlightSequence();
                    });
            }
        }
        if (c < 1) {
            alert2("No DNA Sequences selected");
            return;
        }
        deselectAll();
    });
    $('#longestOrf').click(function () {
        var c = 0;
        for (var i = 0; i < sequenceEntry.length; i++) {
            if (selected[i] === true && sequenceEntry[i].data !== null && sequenceEntry[i].data.type === 'dna') {
                c++;
                sendQueryPlaceholder("Longest ORF", {
                    type: "longestOrf",
                    translationTable: $('#translationTable').val(),
                    text: JSON.stringify(sequenceEntry[i].data)
                }, i);
            }
        }
        if (c < 1) {
            alert2("No DNA Sequences selected");
            return;
        }
        deselectAll();
    });

    $('#ExportFastaSequences').click(function () {
        var formatLength = 70;
        var data = "";
        for (var i = 0; i < sequenceEntry.length; i++) {
            if (sequenceEntry[i] !== null && (sequenceEntry[i].data.type === 'dna' || sequenceEntry[i].data.type === 'aa')) {
                data += ">" + sequenceEntry[i].data.name + "\n";
                var k = 0;
                for (k = 0; k < sequenceEntry[i].data.sequence.length - formatLength; k += formatLength) {
                    data += sequenceEntry[i].data.sequence.substring(k, k + formatLength) + "\n";
                }
                if (k < sequenceEntry[i].data.sequence.length) {
                    data += sequenceEntry[i].data.sequence.substring(k, sequenceEntry[i].data.sequence.length) + "\n";
                }
            }
        }
        if (data.length < 1) {
            alert2("No Sequences to export");
            return;
        }

        downloadText("sequences.fasta", data);
    });


    $('#ExportHTML').click(function () {
        var data = "<html><head><title>GeneWarrior</title><style>";
        $.ajax({
            url: "css/dnatools.css",
            type: 'GET',
            async: false,
            cache: false,
            timeout: 30000,
            error: function () {
                alert2("Error contacting server");
            },
            success: function (text) {
                data += text.replace(/(\r\n|\n|\r)/gm, "") + "</style></head><body>";
            }
        });
        makeforceWidth(1000);
        data += $("#sequenceArea").html();
        deforceWidth();
        data += "</body></html>"
        downloadText("sequences.html", data);
    });

    $('#ExportClustalAlignments').click(function () {
        var formatLength = 60;
        var data = "";
        for (var i = 0; i < sequenceEntry.length; i++) {
            if (sequenceEntry[i] !== null && (sequenceEntry[i].data.type === 'alignmentDNA' || sequenceEntry[i].data.type === 'alignmentProt')) {
                var names = [];
                for (var j = 0; j < sequenceEntry[i].data.seqs.length; j++) {
                    if (sequenceEntry[i].data.seqs[j].name.length > 10) {
                        names.push(sequenceEntry[i].data.seqs[j].name.substring(0, 10));
                    } else if ((sequenceEntry[i].data.seqs[j].name.length < 10)) {
                        var str = (Array(11 - sequenceEntry[i].data.seqs[j].name.length).join(" "));
                        names.push(sequenceEntry[i].data.seqs[j].name.concat(str));
                    } else {
                        names.push(sequenceEntry[i].data.seqs[j].name);
                    }
                }
                data += "CLUSTAL " + sequenceEntry[i].data.name + "\n\n";
                var seqlength = sequenceEntry[i].data.seqs[0].sequence.length;

                var k = 0;
                for (k = 0; k < seqlength - formatLength; k += formatLength) {
                    for (var j = 0; j < sequenceEntry[i].data.seqs.length; j++) {
                        data += names[j] + "\t" + sequenceEntry[i].data.seqs[j].sequence.substring(k, k + formatLength) + "\n";
                    }
                    data += "          \t" + sequenceEntry[i].data.matches.substring(k, k + formatLength) + "\n\n";
                }
                if (k < seqlength) {
                    for (var j = 0; j < sequenceEntry[i].data.seqs.length; j++) {
                        data += names[j] + "\t" + sequenceEntry[i].data.seqs[j].sequence.substring(k, seqlength) + "\n";
                    }
                    data += "          \t" + sequenceEntry[i].data.matches.substring(k, seqlength) + "\n\n";
                }
                data += "\n\n";
            }
        }
        if (data.length < 1) {
            alert2("No Sequences to export");
            return;
        }

        downloadText("alignments.aln", data);
    });

    $('#getSubsequence').on("mousedown", function (event) {
        event.stopPropagation();
    }).on("mouseup", function (event) {
        event.stopPropagation();
    }).click(function () {
        if (!highlighted) {
            return;
        }
        $.post('dnatoolsServlet', {
            type: "getSubsequence",
            start: highlightStart,
            end: highlightEnd,
            pos: highlightEntry,
            text: JSON.stringify(sequenceEntry[highlightEntry].data)
        })
            .done(function (responseText) {
                scrollToEntry(addSequencesFromResponse(responseText, 'response'));
            })
            .fail(function (responseText) {
                alert2("Failed contacting server:" + responseText);
            });
        dehighlightSequence();
    });

    $('#ExportPrint').click(function () {
        deselectAll();
        closeNavigator();
        makeforceWidth(500);
        window.print();
        deforceWidth();
    });


    $('#BlastnNCBI').click(function () {
        if (countSelectedDNA < 1) {
            alert2("Select at least one DNA sequence to continue");
        } else {
            var seqFasta = "";
            for (var i = 0; i < sequenceEntry.length; i++) {
                if (selected[i] === true && sequenceEntry[i].data.type === 'dna') {
                    seqFasta += '>' + sequenceEntry[i].data.name + '\n' + sequenceEntry[i].data.sequence + '\n';
                }
            }
            $("<form action='http://blast.ncbi.nlm.nih.gov/Blast.cgi' method='POST' target='_blank'>" +
                "<input type='hidden' name='PAGE' value='MegaBlast'>" +
                "<input type='hidden' name='PROGRAM' value='blastn'>" +
                "<input type='hidden' name='BLAST_PROGRAMS' value='megaBlast'>" +
                "<input type='hidden' name='PAGE_TYPE' value='BlastSearch'>" +
                "<input type='hidden' name='DBSEARCH' value='true'>" +
                "<input type='hidden' name='QUERY' value='" + escapeQuotes(seqFasta) + "'>" +
                "</form>").appendTo('body').submit().remove();
            deselectAll();
        }
    });

    $('#BlastpNCBI').click(function () {
        if (countSelectedAA < 1) {
            alert2("Select at least one protein sequence to continue");
        } else {
            var seqFasta = "";
            for (var i = 0; i < sequenceEntry.length; i++) {
                if (selected[i] === true && sequenceEntry[i].sequence.type === 'aa') {
                    seqFasta += '>' + sequenceEntry[i].data.name + '\n' + sequenceEntry[i].data.sequence + '\n';
                }
            }
            $("<form action='http://blast.ncbi.nlm.nih.gov/Blast.cgi' method='POST' target='_blank'>" +
                "<input type='hidden' name='PAGE' value='MegaBlast'>" +
                "<input type='hidden' name='PROGRAM' value='blastp'>" +
                "<input type='hidden' name='BLAST_PROGRAMS' value='blastp'>" +
                "<input type='hidden' name='PAGE_TYPE' value='BlastSearch'>" +
                "<input type='hidden' name='DBSEARCH' value='true'>" +
                "<input type='hidden' name='QUERY' value='" + escapeQuotes(seqFasta) + "'>" +
                "</form>").appendTo('body').submit().remove();
            deselectAll();
        }
    });

    //Click on find button
    $('#findButton').click(function () {
        removeFindMarks();
        var isDNA = true;
        if ($('#findRadio2').is(':checked'))
            isDNA = false;
        var data = [];
        for (var i = 0; i < sequenceEntry.length; i++) {
            if (sequenceEntry[i] === null)
                continue;
            if (isDNA) {
                if (sequenceEntry[i].data !== null && (sequenceEntry[i].data.type === 'dna' || sequenceEntry[i].data.type === 'alignmentDNA')) {
                    data.push(sequenceEntry[i].data);
                }
            } else {
                if (sequenceEntry[i].data !== null && (sequenceEntry[i].data.type === 'aa' || sequenceEntry[i].data.type === 'alignmentProt')) {
                    data.push(sequenceEntry[i].data);
                }
            }
        }
        if (data.length < 1) {
            alert2("Enter sequences first");
            return;
        }
        $.post('dnatoolsServlet', {type: (isDNA ? "validateDNA" : "validateAA"), text: $('#findText').val()})
            .done(function (responseText) {
                $('#findText').val(responseText);

                if ($('#findText').val().trim() === "") {
                    alert2("Enter sequence to search for");
                    return;
                }
                $.post('dnatoolsServlet', {
                    type: (isDNA ? "findSequenceDNA" : "findSequenceAA"),
                    searchFor: $('#findText').val(),
                    bothStrands: ($('#findCheck').is(':checked')),
                    text: JSON.stringify(data)
                })
                    .done(function (responseText) {
                        hits = $.parseJSON(responseText);
                        if (hits.length === 0) {
                            $('#findHits').text("no matches");
                            $('#findHitForward').hide();
                            $('#findHitBack').hide();
                        } else {
                            for (var i = 0; i < hits.length; i++) {
                                sequenceEntry[hits[i].entry].drawFind(hits[i].start, hits[i].end, hits[i].row);
                            }
                            $('#findHits').text(hits.length + " matches");
                            currentHit = -1;
                            findHitForward();
                            $('#findHitForward').show();
                            $('#findHitBack').show();
                        }
                    })
                    .fail(function (responseText) {
                        alert2("Failed contacting server:" + responseText);
                        dehighlightSequence();
                    });
            })
            .fail(function (responseText) {
                alert2("Failed contacting server:" + responseText);
                dehighlightSequence();
            });
    });

    function muscleAlignment() {
        if (countSelectedDNA >= 2) {
            var data = [];
            for (var i = 0; i < sequenceEntry.length; i++) {
                if (selected[i] && sequenceEntry[i].data.type === 'dna') {
                    data.push(sequenceEntry[i].data);
                }
            }
            if (data.length > 20) {
                alert2("Currently the limit for this tool are 20 sequences with max length of 5kb");
                return;
            }
            for (var i = 0; i < data.length; i++) {
                if (data[i].sequence.length > 5000) {
                    alert2("Currently the limit for this tool are 20 sequences with max length of 5kb");
                    return;
                }
            }

            sendQueryPlaceholder("Multiple Sequence Alignment", {
                type: "alignMultiple",
                seqType: "dna",
                text: JSON.stringify(data)
            }, data[data.length - 1].pos);
        } else if (countSelectedAA >= 2) {
            var data = [];
            for (var i = 0; i < sequenceEntry.length; i++) {
                if (selected[i] && sequenceEntry[i].data.type === 'aa') {
                    data.push(sequenceEntry[i].data);
                }
            }
            if (data.length > 10) {
                alert2("Currently the limit for this tool are 10 sequences with max length of 5kb");
                return;
            }
            for (var i = 0; i < data.length; i++) {
                if (data[i].sequence.length > 5000) {
                    alert2("Currently the limit for this tool are 10 sequences with max length of 5kb");
                    return;
                }
            }
            sendQueryPlaceholder("Multiple Sequence Alignment", {
                type: "alignMultiple",
                seqType: "aa",
                text: JSON.stringify(data)
            }, data[data.length - 1].pos);
        } else {
            alert2("Select at least 2 DNA sequences or 2 Protein sequences for multiple sequence alignment");
            return;
        }
        deselectAll();
    }

    function pairwiseAlignment(alignmentType) {
        if (countSelectedDNA === 2) {
            var data = [];
            for (var i = 0; i < sequenceEntry.length; i++) {
                if (selected[i] && sequenceEntry[i].data.type === 'dna') {
                    data.push(sequenceEntry[i].data);
                }
            }
            sendQueryPlaceholder("Pairwise Alignment", {
                type: "alignPw",
                seqType: "dna",
                alignmentType: alignmentType,
                text: JSON.stringify(data)
            }, data[data.length - 1].pos);
        } else if (countSelectedAA === 2) {
            var data = [];
            for (var i = 0; i < sequenceEntry.length; i++) {
                if (selected[i] && sequenceEntry[i].data.type === 'aa') {
                    data.push(sequenceEntry[i].data);
                }
            }
            sendQueryPlaceholder("Pairwise Alignment", {
                type: "alignPw",
                seqType: "aa",
                alignmentType: alignmentType,
                text: JSON.stringify(data)
            }, data[data.length - 1].pos);
        } else {
            alert2("Select either 2 DNA sequences or 2 Protein sequences to align");
            return;
        }
        deselectAll();
    }

    $('#PairwiseCostFreeEnds').click(function () {
        pairwiseAlignment('CFE');
    });

    $('#PairwiseGlobal').click(function () {
        pairwiseAlignment('Global');
    });

    $('#PairwiseLocal').click(function () {
        pairwiseAlignment('Local');
    });

    $('#MuscleAlignment').click(function () {
        muscleAlignment();
    });

    $('#AlignmentConsensus').click(function () {
        if (countSelectedAlignment < 1) {
            alert2("Select at least one alignment to continue");
        } else {
            for (var i = 0; i < sequenceEntry.length; i++) {
                if (selected[i] === true && (sequenceEntry[i].data.type === 'alignmentDNA' || sequenceEntry[i].data.type === 'alignmentProt')) {
                    sendQueryPlaceholder("Alignment Consensus", {
                        type: "alignmentConsensus",
                        pos: i,
                        text: JSON.stringify(sequenceEntry[i].data)
                    }, i);
                }
            }
            deselectAll();
        }
    });

    $('#AlignmentTree').click(function () {
        if (countSelectedAlignment < 1) {
            alert2("Select at least one alignment to continue");
        } else {
            for (var i = 0; i < sequenceEntry.length; i++) {
                if (selected[i] === true && (sequenceEntry[i].data.type === 'alignmentDNA' || sequenceEntry[i].data.type === 'alignmentProt')) {
                    sendQueryPlaceholder("Alignment Tree", {
                        type: "alignmentTree",
                        pos: i,
                        text: JSON.stringify(sequenceEntry[i].data)
                    }, i);
                }
            }
            deselectAll();
        }
    });

    $('#AlignmentLogo').click(function () {
        if (countSelectedAlignment < 1) {
            alert2("Select at least one alignment to continue");
        } else {
            for (var i = 0; i < sequenceEntry.length; i++) {
                if (selected[i] === true && (sequenceEntry[i].data.type === 'alignmentDNA' || sequenceEntry[i].data.type === 'alignmentProt')) {
                    if (sequenceEntry[i].data.seqs[0].sequence.length > 1000) {
                        alert2("Currently the limit for this tool is an alignment with maximal 1000 columns");
                        return;
                    }
                    sendQueryPlaceholder("Sequence Logo", {
                        type: "alignmentLogo",
                        pos: i,
                        text: JSON.stringify(sequenceEntry[i].data)
                    }, i);
                }
            }
            deselectAll();
        }
    });

    $('#findCancel').click(function () {
        $('#findText').val("");
        removeFindMarks();
    });

    $('#findRadio1').click(function () {
        $('#findBothStrands').show();
        removeFindMarks();
    });
    $('#findRadio2').click(function () {
        $('#findBothStrands').hide();
        removeFindMarks();
    });

    $('#showTranslation').click(function () {
        var selectedIndex = -1;
        for (var i = 0; i < sequenceEntry.length; i++) {
            if (selected[i] && sequenceEntry[i].data.type === 'dna') {
                selectedIndex = i;
                break;
            }
        }

        if (selectedIndex < 0) {
            alert2('Select DNA sequence to translate');
            return;
        }
        if (countSelectedDNA > 1) {
            alert2('Select only one DNA sequence.');
            return;
        }
        showPluginPopup("/translation", "Translation", sequenceEntry[selectedIndex].data);
    });

    $('#selectPrimer').click(function () {
        var selectedIndex = -1;
        for (var i = 0; i < sequenceEntry.length; i++) {
            if (selected[i] && sequenceEntry[i].data.type === 'dna') {
                selectedIndex = i;
                break;
            }
        }

        if (selectedIndex < 0) {
            alert2('Select DNA sequence as PCR template');
            return;
        }
        if (countSelectedDNA > 1) {
            alert2('Select only one DNA sequence.');
            return;
        }
        showPluginPopup("/primer", "Primer", sequenceEntry[selectedIndex].data);
    });

    $('#shareProject').click(function () {

        if (sequenceEntry.length < 1) {
            alert2('No Entries to share');
            return;
        }
        var order = getDisplayOrder();
        var sequencesOrdered = [];
        for (var i = 0; i < order.length; i++) {
            if (sequenceEntry[order[i]].data !== null)
                sequencesOrdered.push(sequenceEntry[order[i]].data);
        }
        if (sequencesOrdered.length > 100) {
            alert2('You have ' + sequencesOrdered.length + ' entries in your project. The current limit is 100.<br>Remove some entries and try again.');
        }
        showPluginPopup("/share", "Share", JSON.stringify(sequencesOrdered));
    });
});


//StickyTitles
function setStickies() {
    $(".fixed").each(function () {
        $(this).removeClass("fixed");
    });
    stickies = new stickyTitles($(".stickyTitle"));
    stickies.load();
}

function stickyTitles(stickies) {
    this.load = function () {
        stickies.each(function () {
            var thisSticky = $(this);

            if (!thisSticky.parent().is(".followWrap")) {
                thisSticky.wrap('<div class="followWrap" />');

                //thisSticky.parent().width(300);
            } else {
                thisSticky.parent().css("height", "auto");
                thisSticky.parent().css("width", "auto");
                thisSticky.css("height", "auto");
                thisSticky.css("width", "auto");

                var w1 = thisSticky.find(".entryTitle").outerWidth() + 10;
                var w2 = thisSticky.find(".info").outerWidth() + 210;
                thisSticky.parent().width((w1 > w2 ? w1 : w2));
                thisSticky.width((w1 > w2 ? w1 : w2));
                thisSticky.parent().height(thisSticky.outerHeight() + 10);
                thisSticky.height(thisSticky.outerHeight() + 10);
            }
            //thisSticky.height(thisSticky.outerHeight());
            //thisSticky.width(300);
        });
    };
    this.scroll = function () {
        stickies.each(function (i) {
            var thisSticky = $(this);
            var pos = thisSticky.parent().offset().top;
            var outerFrame = pos + thisSticky.parent().parent().outerHeight();
            if (pos <= $(window).scrollTop() && $(window).scrollTop() < outerFrame - thisSticky.outerHeight() - 100) {
                thisSticky.addClass("fixed");
            } else {
                thisSticky.removeClass("fixed");
            }
        });
    };
}
