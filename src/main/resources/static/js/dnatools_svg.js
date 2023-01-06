var forcedWidth = -1;


function svg(svgID, data) {
    this.svgID = svgID;
    this.data = data;
    this.nucBlocks = 10;
    this.fontHeight = 12;
    this.arrowHeight = 12;
    this.blockmargin = 5;
    this.marginleft = 10;
    this.annotationheight = 15;
    this.belowHeight = 0;
    this.showAnnotation = true;

    this.svgWidth = -1;
    this.charWidth = -1;
}

svg.prototype.getWidth = function () {
    if (forcedWidth < 0)
        return this.svgID.width();
    else
        return forcedWidth;
};

svg.prototype.updateSize = function () {
    var newWidth = this.getWidth();
    if (this.svgWidth === newWidth)
        return;

    if (Math.floor((this.svgWidth - this.marginleft) / ((this.nucBlocks * this.charWidth) + this.blockmargin)) === Math.floor((newWidth - this.marginleft) / ((this.nucBlocks * this.charWidth) + this.blockmargin))) {
        this.svgWidth = newWidth;
        return;
    }
    if (newWidth < 100)
        return;
    this.svgWidth = newWidth;
    if (this.charWidth < 0) {
        this.charWidth = document.getElementById('measureText').getComputedTextLength() / 12;
    }
    this.redraw();
};

svg.prototype.redraw = function () {
    //html groups
    this.seqsGroup = this.svgID.find('.seqs');
    this.formsGroup = this.svgID.find('.forms');

    this.clear();
    this.draw();
};

svg.prototype.clear = function () {
    this.seqsGroup.empty();
    this.formsGroup.empty();
};

svg.prototype.drawText = function (text, x, y) {
    var txt = document.createElementNS("http://www.w3.org/2000/svg", "text");
    txt.setAttributeNS(null, "x", x);
    txt.setAttributeNS(null, "y", y);
    txt.appendChild(document.createTextNode(text));
    this.seqsGroup.append(txt);
    return txt;
};

svg.prototype.getAboveHeight = function () {
    if (!this.showAnnotation)
        return 0;
    if (this.data.mark === null || this.data.mark === 0)
        return 0;
    for (var i = 0; i < this.data.mark.length; i++) {
        if (this.data.mark[i].type >= 100)
            return this.annotationheight;
    }
    return 0;
};

svg.prototype.getBelowHeight = function () {
    return this.belowHeight;
};

svg.prototype.draw = function () {
    this.updateSize();
    this.clear();
    var aboveHeight = this.getAboveHeight();
    var lineheight = this.getLineheight();
    var totalheight = this.drawSequence(aboveHeight, lineheight);

    if (this.showAnnotation) {
        for (var i = 0; i < this.data.mark.length; i++) {
            if (this.data.mark[i].type < 100) {
                if (this.data.mark[i].type < 0) {
                    this.drawFind(this.data.mark[i].start, this.data.mark[i].stop);
                } else {
                    this.drawMark(this.data.mark[i].start, this.data.mark[i].stop, this.data.mark[i].type);
                }
            } else {
                this.drawAnnotation(this.data.mark[i].start, this.data.mark[i].stop, this.data.mark[i].type, this.data.mark[i].name);
            }
        }
    }
    this.svgID.attr('height', (totalheight + lineheight + 10));
    this.drawClassSpecific();
};

svg.prototype.drawAnnotation = function (start, stop, type, annotation) {

    var s = this.getXY(start);
    var e = this.getXY(stop);
    var reverse = (start > stop);

    var group = document.createElementNS("http://www.w3.org/2000/svg", "g");
    group.setAttributeNS(null, "class", "annotation color" + type);

    if (s.y === e.y) {
        var block = document.createElementNS("http://www.w3.org/2000/svg", "polygon");
        if (reverse) {
            block.setAttributeNS(null, "points", e.x + "," + (e.y - (this.arrowHeight / 2)) + " " + (e.x + this.charWidth) + "," + (e.y - this.arrowHeight) + " " + (s.x + this.charWidth) + "," + (s.y - this.arrowHeight) + " " + (s.x + this.charWidth) + "," + (s.y) + " " + (e.x + this.charWidth) + "," + (e.y));
        } else {
            block.setAttributeNS(null, "points", (e.x + this.charWidth) + "," + (e.y - (this.arrowHeight / 2)) + " " + (e.x) + "," + (e.y - this.arrowHeight) + " " + s.x + "," + (s.y - this.arrowHeight) + " " + s.x + "," + (s.y) + " " + (e.x) + "," + (e.y));
        }
        group.appendChild(block);
        var text = document.createElementNS("http://www.w3.org/2000/svg", "text");
        text.setAttributeNS(null, "x", (reverse ? e.x : s.x) + this.charWidth);
        text.setAttributeNS(null, "y", (e.y - 2));
        text.appendChild(document.createTextNode(annotation));
        group.appendChild(text);
    } else {
        var lineheight = this.getLineheight();
        var block = document.createElementNS("http://www.w3.org/2000/svg", "polygon");

        if (reverse) {
            block.setAttributeNS(null, "points", e.x + "," + (e.y - (this.arrowHeight / 2)) + " " + (e.x + this.charWidth) + "," + (e.y - this.arrowHeight) + " " + (this.svgWidth + 10) + "," + (e.y - this.arrowHeight) + " " + (this.svgWidth + 10) + "," + (e.y) + " " + (e.x + this.charWidth) + "," + (e.y));

        } else {
            block.setAttributeNS(null, "points", (e.x + this.charWidth) + "," + (e.y - (this.arrowHeight / 2)) + " " + (e.x) + "," + (e.y - this.arrowHeight) + " -10," + (e.y - this.arrowHeight) + " -10," + (e.y) + " " + (e.x) + "," + (e.y));
        }
        group.appendChild(block);
        var text = document.createElementNS("http://www.w3.org/2000/svg", "text");
        text.setAttributeNS(null, "x", (reverse ? e.x : 0) + this.charWidth);
        text.setAttributeNS(null, "y", (e.y - 2));
        text.appendChild(document.createTextNode(annotation));
        group.appendChild(text);

        var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
        if (reverse) {
            block.setAttributeNS(null, "x", "-10");
            block.setAttributeNS(null, "y", s.y - this.arrowHeight);
            block.setAttributeNS(null, "width", s.x + this.charWidth + 10);
            block.setAttributeNS(null, "height", this.arrowHeight);
        } else {
            block.setAttributeNS(null, "x", s.x);
            block.setAttributeNS(null, "y", s.y - this.arrowHeight);
            block.setAttributeNS(null, "width", this.svgWidth - s.x + 10);
            block.setAttributeNS(null, "height", this.arrowHeight);
        }
        group.appendChild(block);
        var text = document.createElementNS("http://www.w3.org/2000/svg", "text");
        text.setAttributeNS(null, "x", (reverse ? 0 : s.x) + this.charWidth);
        text.setAttributeNS(null, "y", (s.y - 2));
        text.appendChild(document.createTextNode(annotation));
        group.appendChild(text);
        var sy = s.y;
        var ey = e.y;
        if (reverse) {
            sy = e.y;
            ey = s.y;
        }
        for (var i = sy + lineheight; i < ey; i += lineheight) {
            var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
            block.setAttributeNS(null, "x", "-10");
            block.setAttributeNS(null, "y", i - this.arrowHeight);
            block.setAttributeNS(null, "width", this.svgWidth + 20);
            block.setAttributeNS(null, "height", this.arrowHeight);
            group.appendChild(block);
            var text = document.createElementNS("http://www.w3.org/2000/svg", "text");
            text.setAttributeNS(null, "x", "0");
            text.setAttributeNS(null, "y", (i - 2));
            if (reverse) {
                text.appendChild(document.createTextNode("<<"));
            } else {
                text.appendChild(document.createTextNode(">>"));
            }
            group.appendChild(text);
            var text = document.createElementNS("http://www.w3.org/2000/svg", "text");
            text.setAttributeNS(null, "x", this.svgWidth - 13);
            text.setAttributeNS(null, "y", (i - 2));
            if (reverse) {
                text.appendChild(document.createTextNode("<<"));
            } else {
                text.appendChild(document.createTextNode(">>"));
            }
            group.appendChild(text);
        }
    }
    this.formsGroup.append(group);
};

svg.prototype.drawHighlight = function (start, end) {
    if (start > end) {
        var b = start;
        start = end;
        end = b;
    }
    var lineheight = this.getLineheight();
    var s = this.getXY(start);
    var e = this.getXY(end);

    var st = document.createElementNS("http://www.w3.org/2000/svg", "line");
    st.setAttributeNS(null, "class", "highlight");
    st.setAttributeNS(null, "x1", s.x);
    st.setAttributeNS(null, "y1", s.y - 4);
    st.setAttributeNS(null, "x2", s.x);
    st.setAttributeNS(null, "y2", s.y + s.height + 4);
    st.setAttributeNS(null, "stroke", "blue");
    st.setAttributeNS(null, "stroke-width", "2");
    this.formsGroup.append(st);

    var en = document.createElementNS("http://www.w3.org/2000/svg", "line");
    en.setAttributeNS(null, "class", "highlight");
    en.setAttributeNS(null, "x1", e.x + this.charWidth);
    en.setAttributeNS(null, "y1", e.y - 4);
    en.setAttributeNS(null, "x2", e.x + this.charWidth);
    en.setAttributeNS(null, "y2", e.y + s.height + 4);
    en.setAttributeNS(null, "stroke", "blue");
    en.setAttributeNS(null, "stroke-width", "2");
    this.formsGroup.append(en);

    if (s.y === e.y) {
        var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
        block.setAttributeNS(null, "class", "highlight");
        block.setAttributeNS(null, "x", s.x);
        block.setAttributeNS(null, "y", s.y);
        block.setAttributeNS(null, "width", e.x + this.charWidth - s.x);
        block.setAttributeNS(null, "height", s.height + 2);
        block.setAttributeNS(null, "fill", "blue");
        block.setAttributeNS(null, "fill-opacity", "0.5");
        this.formsGroup.append(block);
    } else {
        var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
        block.setAttributeNS(null, "class", "highlight");
        block.setAttributeNS(null, "x", s.x);
        block.setAttributeNS(null, "y", s.y);
        block.setAttributeNS(null, "width", this.svgWidth - s.x);
        block.setAttributeNS(null, "height", s.height + 2);
        block.setAttributeNS(null, "fill", "blue");
        block.setAttributeNS(null, "fill-opacity", "0.5");
        this.formsGroup.append(block);

        for (var i = s.y + lineheight; i < e.y; i += lineheight) {
            var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
            block.setAttributeNS(null, "class", "highlight");
            block.setAttributeNS(null, "x", "0");
            block.setAttributeNS(null, "y", i);
            block.setAttributeNS(null, "width", this.svgWidth);
            block.setAttributeNS(null, "height", s.height + 2);
            block.setAttributeNS(null, "fill", "blue");
            block.setAttributeNS(null, "fill-opacity", "0.5");
            this.formsGroup.append(block);
        }

        var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
        block.setAttributeNS(null, "class", "highlight");
        block.setAttributeNS(null, "x", "0");
        block.setAttributeNS(null, "y", e.y);
        block.setAttributeNS(null, "width", e.x + this.charWidth);
        block.setAttributeNS(null, "height", e.height + 2);
        block.setAttributeNS(null, "fill", "blue");
        block.setAttributeNS(null, "fill-opacity", "0.5");
        this.formsGroup.append(block);
    }
};

svg.prototype.removeHighlight = function () {
    this.svgID.find(".highlight").remove();
};

svg.prototype.drawMark = function (start, stop, color) {
    var lineheight = this.getLineheight();
    var s = this.getXY(start);
    var e = this.getXY(stop);
    if (s.y === e.y) {
        var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
        block.setAttributeNS(null, "class", "mark color" + color);
        block.setAttributeNS(null, "x", s.x);
        block.setAttributeNS(null, "y", s.y);
        block.setAttributeNS(null, "width", e.x + this.charWidth - s.x);
        block.setAttributeNS(null, "height", s.height + 2);
        this.formsGroup.append(block);
    } else {
        var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
        block.setAttributeNS(null, "class", "mark color" + color);
        block.setAttributeNS(null, "x", s.x);
        block.setAttributeNS(null, "y", s.y);
        block.setAttributeNS(null, "width", this.svgWidth - s.x);
        block.setAttributeNS(null, "height", s.height + 2);
        this.formsGroup.append(block);

        for (var i = s.y + lineheight; i < e.y; i += lineheight) {
            var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
            block.setAttributeNS(null, "class", "mark color" + color);
            block.setAttributeNS(null, "x", "0");
            block.setAttributeNS(null, "y", i);
            block.setAttributeNS(null, "width", this.svgWidth);
            block.setAttributeNS(null, "height", s.height + 2);
            this.formsGroup.append(block);
        }

        var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
        block.setAttributeNS(null, "class", "mark color" + color);
        block.setAttributeNS(null, "x", "0");
        block.setAttributeNS(null, "y", e.y);
        block.setAttributeNS(null, "width", e.x + this.charWidth);
        block.setAttributeNS(null, "height", s.height + 2);
        this.formsGroup.append(block);
    }
};

//SequenceSVG
SequenceSVG.prototype = new svg();
SequenceSVG.prototype.constructor = SequenceSVG;

function SequenceSVG(svgID, data) {
    this.svgID = svgID;
    this.data = data;
}


SequenceSVG.prototype.getMaxSeqLength = function () {
    return this.data.sequence.length;
};

SequenceSVG.prototype.drawSequence = function (aboveHeight, lineheight) {
    var x = this.marginleft;
    var line = 0;
    var seqLength = this.getMaxSeqLength();
    for (var i = 0; i < seqLength; i += this.nucBlocks) {
        this.drawText(this.data.sequence.substring(i, (i + this.nucBlocks > seqLength ? seqLength : i + this.nucBlocks)), x, line + (5 + aboveHeight + this.fontHeight));
        x += (this.nucBlocks * this.charWidth) + this.blockmargin;
        if (x + ((this.nucBlocks * this.charWidth) + this.blockmargin) > this.svgWidth) {
            x = this.marginleft;
            line += lineheight;
        }
    }
    return line; //totalheight
};

SequenceSVG.prototype.getXY = function (pos) {
    var maxLength = this.getMaxSeqLength();
    var lineheight = this.getLineheight();
    if (pos >= maxLength)
        return null;
    var nucsPerLine = Math.floor((this.svgWidth - this.marginleft) / ((this.nucBlocks * this.charWidth) + this.blockmargin)) * this.nucBlocks;
    var y = ((Math.floor(pos / nucsPerLine)) * lineheight) + 5 + this.getAboveHeight();
    var x = ((pos % nucsPerLine) * this.charWidth) + (Math.floor((pos % nucsPerLine) / this.nucBlocks) * this.blockmargin);
    return {x: (x + this.marginleft), y: y, height: (lineheight - this.getAboveHeight() - this.getBelowHeight() - 5)};
};

SequenceSVG.prototype.getNucPosition = function (x, y) {
    var lineheight = this.getLineheight();
    x -= this.marginleft;
    var maxLength = this.getMaxSeqLength();
    var pos = Math.floor((x - this.blockmargin * Math.floor(x / ((this.nucBlocks * this.charWidth) + this.blockmargin))) / this.charWidth);
    pos += Math.floor(y / lineheight) * (Math.floor((this.svgWidth - this.marginleft) / ((this.nucBlocks * this.charWidth) + this.blockmargin)) * this.nucBlocks);
    if (pos >= maxLength)
        return null;
    return pos;
};

SequenceSVG.prototype.getLineheight = function () {
    return (5 + this.getAboveHeight() + this.fontHeight + this.getBelowHeight());
};

SequenceSVG.prototype.drawClassSpecific = function () {

};

SequenceSVG.prototype.drawFind = function (start, stop, row) {
    var s = this.getXY(start);
    var e = this.getXY(stop);
    var lineheight = this.getLineheight();
    if (s.y === e.y) {
        var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
        block.setAttributeNS(null, "class", "markFind");
        block.setAttributeNS(null, "x", s.x);
        block.setAttributeNS(null, "y", s.y);
        block.setAttributeNS(null, "width", e.x + this.charWidth - s.x);
        block.setAttributeNS(null, "height", s.height + 2);
        block.setAttributeNS(null, "fill", "orange");
        block.setAttributeNS(null, "stroke", "green");
        block.setAttributeNS(null, "stroke-width", "2");
        this.formsGroup.append(block);
    } else {
        var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
        block.setAttributeNS(null, "class", "markFind");
        block.setAttributeNS(null, "x", s.x);
        block.setAttributeNS(null, "y", s.y);
        block.setAttributeNS(null, "width", this.svgWidth - s.x);
        block.setAttributeNS(null, "height", s.height + 2);
        block.setAttributeNS(null, "fill", "orange");
        block.setAttributeNS(null, "stroke", "green");
        block.setAttributeNS(null, "stroke-width", "2");
        this.formsGroup.append(block);

        for (var i = s.y + lineheight; i < e.y; i += lineheight) {
            var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
            block.setAttributeNS(null, "class", "markFind");
            block.setAttributeNS(null, "x", "0");
            block.setAttributeNS(null, "y", i);
            block.setAttributeNS(null, "width", this.svgWidth);
            block.setAttributeNS(null, "height", s.height + 2);
            block.setAttributeNS(null, "fill", "orange");
            block.setAttributeNS(null, "stroke", "green");
            block.setAttributeNS(null, "stroke-width", "2");
            this.formsGroup.append(block);
        }

        var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
        block.setAttributeNS(null, "class", "markFind");
        block.setAttributeNS(null, "x", "0");
        block.setAttributeNS(null, "y", e.y);
        block.setAttributeNS(null, "width", e.x + this.charWidth);
        block.setAttributeNS(null, "height", e.height + 2);
        block.setAttributeNS(null, "fill", "orange");
        block.setAttributeNS(null, "stroke", "green");
        block.setAttributeNS(null, "stroke-width", "2");
        this.formsGroup.append(block);
    }
};

//alignmentSVG
alignmentSVG.prototype = new svg();
alignmentSVG.prototype.constructor = alignmentSVG;

function alignmentSVG(svgID, data) {
    this.svgID = svgID;
    this.data = data;
}


alignmentSVG.prototype.getLineheight = function () {
    return (5 + this.getAboveHeight() + (this.data.seqs.length * (this.fontHeight + 2)) + this.getBelowHeight());
};

alignmentSVG.prototype.getMaxSeqLength = function () {
    return this.data.seqs[0].sequence.length;
};

alignmentSVG.prototype.drawSequence = function (aboveHeight, lineheight) {
    var x = this.marginleft;
    var line = 0;
    var seqLength = this.getMaxSeqLength();
    var rows = this.data.seqs.length;

    for (var i = 0; i < seqLength; i += this.nucBlocks) {
        for (var j = 0; j < rows; j++) {
            var y = (line + (5 + aboveHeight + ((j + 1) * (this.fontHeight + 2))));
            this.drawText(this.data.seqs[j].sequence.substring(i, (i + this.nucBlocks > seqLength ? seqLength : i + this.nucBlocks)), x, y);
        }
        x += (this.nucBlocks * this.charWidth) + this.blockmargin;
        if (x + ((this.nucBlocks * this.charWidth) + this.blockmargin) > this.svgWidth) {
            x = this.marginleft;
            line += lineheight;
        }
    }
    return line; //totalheight
};

alignmentSVG.prototype.getXY = function (pos) {
    var maxLength = this.getMaxSeqLength();
    var lineheight = this.getLineheight();
    if (pos >= maxLength)
        return null;
    var nucsPerLine = Math.floor((this.svgWidth - this.marginleft) / ((this.nucBlocks * this.charWidth) + this.blockmargin)) * this.nucBlocks;
    var y = Math.floor(pos / nucsPerLine) * lineheight + this.getAboveHeight() + 7;
    var x = ((pos % nucsPerLine) * this.charWidth) + (Math.floor((pos % nucsPerLine) / this.nucBlocks) * this.blockmargin);
    return {x: x + this.marginleft, y: y, height: (lineheight - this.getAboveHeight() - this.getBelowHeight() - 5)};
};

alignmentSVG.prototype.getNucPosition = function (x, y) {

    var lineheight = this.getLineheight();
    x -= this.marginleft;
    var maxLength = this.getMaxSeqLength();
    var pos = Math.floor((x - this.blockmargin * Math.floor(x / ((this.nucBlocks * this.charWidth) + this.blockmargin))) / this.charWidth);
    pos += Math.floor(y / lineheight) * (Math.floor((this.svgWidth - this.marginleft) / ((this.nucBlocks * this.charWidth) + this.blockmargin)) * this.nucBlocks);
    if (pos >= maxLength)
        return null;
    return pos;
};

alignmentSVG.prototype.drawClassSpecific = function () {
    for (var i = 0; i < this.data.matches.length; i++) {
        var color;
        switch (this.data.matches.charAt(i)) {
            case " ":
                continue;
            case "*":
                color = "A1";
                break;
            case ":":
                color = "A2";
                break;
            case ".":
                color = "A3";
                break;
            default:
                continue;
        }
        this.drawMark(i, i, color);
    }
};


alignmentSVG.prototype.drawFind = function (start, stop, row) {
    var s = this.getXY(start);
    var e = this.getXY(stop);
    var lineheight = this.getLineheight();
    var rowOffset = (row) * (this.fontHeight + 2);
    if (s.y === e.y) {
        var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
        block.setAttributeNS(null, "class", "markFind");
        block.setAttributeNS(null, "x", s.x);
        block.setAttributeNS(null, "y", s.y + rowOffset);
        block.setAttributeNS(null, "width", e.x + this.charWidth - s.x);
        block.setAttributeNS(null, "height", this.fontHeight + 2);
        block.setAttributeNS(null, "fill", "orange");
        block.setAttributeNS(null, "stroke", "green");
        block.setAttributeNS(null, "stroke-width", "2");
        this.formsGroup.append(block);
    } else {
        var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
        block.setAttributeNS(null, "class", "markFind");
        block.setAttributeNS(null, "x", s.x);
        block.setAttributeNS(null, "y", s.y + rowOffset);
        block.setAttributeNS(null, "width", this.svgWidth - s.x);
        block.setAttributeNS(null, "height", this.fontHeight + 2);
        block.setAttributeNS(null, "fill", "orange");
        block.setAttributeNS(null, "stroke", "green");
        block.setAttributeNS(null, "stroke-width", "2");
        this.formsGroup.append(block);

        for (var i = s.y + lineheight; i < e.y; i += lineheight) {
            var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
            block.setAttributeNS(null, "class", "markFind");
            block.setAttributeNS(null, "x", "0");
            block.setAttributeNS(null, "y", i + rowOffset);
            block.setAttributeNS(null, "width", this.svgWidth);
            block.setAttributeNS(null, "height", this.fontHeight + 2);
            block.setAttributeNS(null, "fill", "orange");
            block.setAttributeNS(null, "stroke", "green");
            block.setAttributeNS(null, "stroke-width", "2");
            this.formsGroup.append(block);
        }

        var block = document.createElementNS("http://www.w3.org/2000/svg", "rect");
        block.setAttributeNS(null, "class", "markFind");
        block.setAttributeNS(null, "x", "0");
        block.setAttributeNS(null, "y", e.y + rowOffset);
        block.setAttributeNS(null, "width", e.x + this.charWidth);
        block.setAttributeNS(null, "height", this.fontHeight + 2);
        block.setAttributeNS(null, "fill", "orange");
        block.setAttributeNS(null, "stroke", "green");
        block.setAttributeNS(null, "stroke-width", "2");
        this.formsGroup.append(block);
    }
};

//alignmentSVG
textSVG.prototype = new svg();
textSVG.prototype.constructor = textSVG;

function textSVG(data) {
    this.data = data;
}

textSVG.prototype.updateSize = function () {
};
textSVG.prototype.redraw = function () {
};
textSVG.prototype.removeHighlight = function () {
};