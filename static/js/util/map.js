/* From http://stackoverflow.com/questions/7095574/google-maps-api-3-custom-marker-color-for-default-dot-marker */

function createMarkerIcon(color) {
    var icon = new google.maps.MarkerImage("http://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|" + color,
        new google.maps.Size(21, 34),
        new google.maps.Point(0,0),
        new google.maps.Point(10, 34));
    return icon;
}

function createMarkerShadow() {
    var shadow = new google.maps.MarkerImage("http://chart.apis.google.com/chart?chst=d_map_pin_shadow",
        new google.maps.Size(40, 37),
        new google.maps.Point(0, 0),
        new google.maps.Point(12, 35));
    return shadow;
}
