/* ----------------------------------------------------------------------
 *  Copyright © 2014 Numenta Inc. All rights reserved.
 *
 *  The information and source code contained herein is the
 *  exclusive property of Numenta Inc. No part of this software
 *  may be used, reproduced, stored or distributed in any form,
 *  without explicit written authorization from Numenta Inc.
 * ----------------------------------------------------------------------
 */
(function() {

  var _renderer;
  var _service;
  var _map;
  var _preview;

  /**
   * Save selected route as CSV file.
   *
   * CSV Data format:
   *
   * |device|time|longitude|latitude|altitude|speed|bearing|accuracy                |
   * |------|----|---------|--------|--------|-----|-------|------------------------|
   * | TEXT |UNIX|  WGS84  | WGS84  | meters | m/s | 0-360 |radius of 68% confidence|
   *
   */
  function save() {
    var content = "";
    var path = buildPath();

    $.each(path, function(index, val) {
      content += "simroute," // Device
              + val.timestamp + ","
              + val.point.lng() + ","
              + val.point.lat() + ","
              + "0," // altitude
              + val.speed + ","
              + "0," // heading
              +  "1," // accuracy
              + "\n";
    });

    // Save file
    var blob = new Blob([content], {
      type: "text/csv;charset=utf-8;"
    });
    var url = window.URL.createObjectURL(blob);
    var link = $('#download')[0];
    var $filename = $('#filename');
    link.href = url;
    link.download = $filename.val();
    link.click();
    window.URL.revokeObjectURL(url);
    showAlert('Saved ' + link.download, 'success');
  }

  /**
   * Generate Geo Data based on route and sampling rate
   *
   * @return {[Object]} Array of [timestamp, point, speed]
   */
  function buildPath() {

    // timestamp, point, speed
    var results =[];
    var lastPoint, point;

    var directions = _renderer.getDirections();
    var routeIdx = _renderer.getRouteIndex();
    var route = directions.routes[routeIdx];
    var rate = parseInt($('#rate').val()) * 1000;
    var speed = 0;

    // Convert local time to unix time
    var timestamp = Date.parse($('#time').val()) + new Date().getTimezoneOffset() * 60000;

    // Traverse every step on the route
    for (var l = 0, legLen = route.legs.length; l < legLen; l++) {
      var steps = route.legs[l].steps;

      for (var s = 0, stepsLen = steps.length; s < stepsLen; s++) {
        // Calculate speed in m/s
        speed = steps[s].distance.value / steps[s].duration.value;

        // Process each point in the step
        var path = steps[s].path;
        var p = 0, pathLen = path.length - 1;
        while (p < pathLen) {
          var time = 0;
          var dist = 0;

          // Starting point
          point = path[p];

          // The steps are connected using the first and last point of the previous step
          if (point.equals(lastPoint)) {
            p++;
            continue;
          }
          lastPoint = point;

          // Join points until distance covers sampling rate
          var nextPoint = point;
          while(p < pathLen && time <= rate) {
            nextPoint = path[p+1];
            // Calculate distance based on the next point
            dist = google.maps.geometry.spherical.computeDistanceBetween(point, nextPoint);
            // Calculate time based on distance covered
            time = parseInt(1000 * dist / speed);
            // Next point
            p++;
          }

          // Calculate heading based on adjacent
          heading = google.maps.geometry.spherical.computeHeading(point, nextPoint);

          // If path is too long then break it into sampling rate chuncks
          dist = speed * rate / 1000;
          while (time > 0) {
            results.push({timestamp:timestamp, point:point, speed:speed});
            // Find next point by adding the distance traveled in the same direction
            point = google.maps.geometry.spherical.computeOffset(point, dist, heading);
            time -= rate;
            timestamp += rate;
          }
        }
      }
    }
    // Add last point
    results.push({timestamp:timestamp, point:point, speed:speed});
    return results;
  }

  /**
   * Preview current configuration
   */
  function showPreview() {
    var coordinates = [];
    var path = buildPath();
    $.each(path, function(index, val) {
      coordinates.push(val.point);
    });
    _preview.setPath(coordinates);
    _preview.setMap(_map);
  }

  /**
   * Show simple alert on top of the map
   *
   * @param msg The message to show
   * @param severity 'success' | 'info' | 'warning' | 'danger'
   *
   * @see http://getbootstrap.com/components/#alerts
   */
  function showAlert(msg, severity) {
    var $alert = $('#alert');
    $alert.removeClass('alert-*');
    $alert.addClass('alert-' + severity);
    $content = $alert.find('span');
    $content.text(msg);
    $alert.show();
    // Hide after 5 secs
    setTimeout(function() {
      $alert.hide();
    }, 5000);
  }

  /**
   * Calculate the route between 2 locations
   * @param  {Function} callback
   */
  function calculateRoute(callback) {
    var start = $('#start').val();
    var end = $('#end').val();
    var mode = $('#mode').val();

    // Request new route
    var request = {
      origin: start,
      destination: end,
      travelMode: google.maps.TravelMode[mode],
      provideRouteAlternatives: true
    };
    _service.route(request, function(directions, status) {
      if (status == google.maps.DirectionsStatus.OK) {
        // Update route
        _renderer.setDirections(directions);
        if ($.isFunction(callback)) {
          callback.call();
        }
      } else {
        showAlert(status, 'danger');
      }
    });
  }

  $(document).ready(function() {
    // Alert pane
    $('#alert .close').click(function() {
      $('#alert').hide();
    });

    // Re-route on changes
    $('#start,#end,#mode').change(function(event) {
      calculateRoute();
    });

    // Update preview on rate changes
    $('#rate').change(function(event) {
      showPreview();
    })

    // Save button
    $('#save').click(function() {
      save();
    });

    // Update initial time
    var now = new Date();

    // Convert to local
    now.setTime(now.getTime() - now.getTimezoneOffset() * 60000);
    $('#time').val(now.toISOString().slice(0, 19));

    // Initialize direction services
    _service = new google.maps.DirectionsService();
    _renderer = new google.maps.DirectionsRenderer({
      draggable: true
    });
    google.maps.event.addListener(_renderer, 'directions_changed', function() {
      showPreview();
    })
    google.maps.event.addListener(_renderer, 'routeindex_changed', function() {
      showPreview();
    })

    // Initialize map
    _map = new google.maps.Map(document.getElementById('map-canvas'), {
      zoom: 10,
      center: new google.maps.LatLng(37.4870969, -122.2284478) // Numenta's office
    });
    _renderer.setMap(_map);
    _renderer.setPanel(document.getElementById('directions-panel'));
    _preview = new google.maps.Polyline({
      path: [],
      geodesic: true,
      strokeColor: '#FF0000',
      strokeOpacity: 1.0,
      strokeWeight: 10,
      zIndex : -1
    });

    // Initialize route
    calculateRoute(function(){
      showPreview();
    });
  });
})();
