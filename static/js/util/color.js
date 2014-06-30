/* From http://stackoverflow.com/questions/7128675/from-green-to-red-color-depend-on-percentage */

function getGreenToRed(percent){
    percent = 100 - percent;
    r = percent<50 ? 255 : Math.floor(255-(percent*2-100)*255/100);
    g = percent>50 ? 255 : Math.floor((percent*2)*255/100);
    return rgbToHex(r, g, 0);
}

/* From http://stackoverflow.com/questions/5623838/rgb-to-hex-and-hex-to-rgb */

function rgbToHex(r, g, b) {
    return ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
}
