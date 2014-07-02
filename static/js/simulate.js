/*----------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ----------------------------------------------------------------------
 */
(function() {

  var _renderer;
  var _service;
  var _map;
  var _preview;
  var _route = [];

  /**
   * Convert route to CSV Data.
   *
   * CSV Data format:
   *
   * |device|time|longitude|latitude|altitude|speed|bearing|accuracy                |
   * |------|----|---------|--------|--------|-----|-------|------------------------|
   * | TEXT |UNIX|  WGS84  | WGS84  | meters | m/s | 0-360 |radius of 68% confidence|
   *
   */
  function toCSV(config) {
    var content = "";
    var path = buildPath(config);

    // Build CSV file
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
    return content;
  }

  /**
   * Download data as a CSV file
   */
  function download() {
    // Build CSV data
    var csvdata = "";
    $.each(_route, function(index, val) {
      csvdata += toCSV(val);
    });

    // Get file name from user
    bootbox.prompt({title: "Save Route As:",
                    buttons: {cancel:{label:"Cancel"},
                              confirm: {label:"Save"}},
                    callback: function(name) {
                                if (name === null) {
                                  // Canceled by user
                                  return;
                                }

                                // Save file
                                var blob = new Blob([csvdata], {
                                  type: "text/csv;charset=utf-8;"
                                });
                                var url = window.URL.createObjectURL(blob);
                                var link = $('#download')[0];
                                link.href = url;
                                link.download = name;
                                link.click();
                                window.URL.revokeObjectURL(url);
                              },
                    value: "route.csv",
                    placeholder: "Enter File Name"});
  }

  /**
   * Post Route data to Nupic Model
   * @return {[type]}
   */
  function postRouteData() {
    // Update current route
    updateRoute();

    // Build CSV data
    var csvdata = "";
    $.each(_route, function(index, val) {
      csvdata += toCSV(val);
    });

    // Post data to be processed by the model
    $('#progress').modal('show');
    $.ajax({
      url: "/process",
      type: "POST",
      data: csvdata,
      contentType: "text/csv;charset=utf-8;",
      success: function(data, status, jqxhr) {
        window.location.replace("/");

      },
      error: function(jqxhr, status, error) {
        $('#progress').modal('hide');
      }
     });
  }

  /**
   * Update active route with current configuration
   */
  function updateRoute() {
    var config = getCurrentRouteConfiguration();
    var activeRoute =  $('#route li.active').text();
    _route[activeRoute - 1] = config;
  }

  /**
   * Get current route configuration based on the user input
   * @return {Object} The configuration object in the following format:
   * <pre>
   * {
   *   route: {Object},   // google map route object
   *   timestamp: {long}, // Start timestamp (unix time)
   *   rate: {long}        // Sampling rate in miliseconds
   * }
   * </pre>
   */
  function getCurrentRouteConfiguration() {
    // Get current route
    var directions = _renderer.getDirections();
    var routeIdx = _renderer.getRouteIndex();
    var route = directions.routes[routeIdx];

    // Get Current Timestamp, converting local time to unix time
    var date = $('#time').val();
    var timestamp = Date.parse(date) + new Date().getTimezoneOffset() * 60000;
    var rate = parseInt($('#rate').val()) * 1000;

    return {directions: directions,
            index: _renderer.getRouteIndex(),
            route: route,
            date: date,
            mode: $('#mode').val(),
            start: $('#start').val(),
            end: $('#end').val(),
            timestamp: timestamp,
            rate: rate};
  }

  /**
   * Update current route selection
   * @param  {integer} selection: the new route number to select.
   */
  function selectRoute(selection) {
    // Update old route with latest parameters
    updateRoute();

    // Unselect previous route
    var $oldSelection = $('#route li.active');
    $oldSelection.removeClass('active');

    // Selecte new route
    $newSelection = $('#route li:nth-child(' + selection + ')');
    $newSelection.addClass('active');

    var route = _route[selection - 1];

    // Update route renderer
    _renderer.setDirections(route.directions);
    _renderer.setRouteIndex(route.index);

    // Update fields
    $('#time').val(route.date);
    $('#start').val(route.start);
    $('#end').val(route.end);
    $('#rate').val(route.rate/1000);
    $('#mode').val(route.mode);


  }

 /**
  * Add the current route to the output
  */
  function addRoute() {

    // Update current route
    updateRoute();

    var $route = $('#route');
    var $selected =  $route.find('li.active');
    var activeRoute = $selected.text();

    // Save current configuration
    var config = getCurrentRouteConfiguration();
    _route[activeRoute - 1] = config;

    // Update time based on next time
    var lastRoute = _route[$route.children().size() - 2];
    var timestamp = lastRoute.timestamp;
    var legs = lastRoute.route.legs;
    for (var l = 0, legLen = legs.length; l < legLen; l++) {
      timestamp += legs[l].duration.value * 1000;
    }
    var nextTime = new Date(timestamp - new Date().getTimezoneOffset() * 60000);

    $('#time').val(nextTime.toISOString().slice(0, 19));

    // Add and select new route to paginator
    $selected.removeClass('active');
    var nextRoute = $route.children().size();
    $newEl = $('<li class="active"><a href="#">'+nextRoute+'</a></li>');
    $newEl.click(function() {
      selectRoute($(this).text());
    });
    $newEl.insertBefore("#route li:last");

    // Update new route
    updateRoute();
  }

  /**
   * Generate Geo Data based on route and sampling rate
   * @param  {[Object]} Route configuration object. See "getCurrentRouteConfiguration()"
   * @return {[Object]} Array of [timestamp, point, speed]
   */
  function buildPath(config) {

    // timestamp, point, speed
    var results =[];
    var lastPoint, point;

    var route = config.route;
    var rate = config.rate;
    var timestamp = config.timestamp;
    var speed = 0;

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
    var path = buildPath(getCurrentRouteConfiguration());
    $.each(path, function(index, val) {
      coordinates.push(val.point);
    });
    _preview.setPath(coordinates);
    _preview.setMap(_map);
  }

  /**
   * Calculate the route between 2 locations
   * @param  {Function} callback to be called once route calculation completes
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
        bootbox.alert(status);
      }
    });
  }

  $(document).ready(function() {

    // Re-route on changes
    $('#start,#end,#mode').change(function(event) {
      calculateRoute();
    });

    // Update preview on rate changes
    $('#rate').change(function(event) {
      showPreview();
    })

    // Reverse button
    $('#reverse').click(function(){
      var start = $('#start').val();
      var end = $('#end').val();
      $('#end').val(start);
      $('#start').val(end);
      calculateRoute();
    });

    // Build button
    $('#build').click(function() {
      postRouteData();
    });

    // Save button
    $('#save').click(function() {
      download();
    });

    // Add to route button
    $('#addRoute').click(function() {
      addRoute();
    });

    // Route selection
    $('#route li:first').click(function(){
      selectRoute($(this).text());
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
