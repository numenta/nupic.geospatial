var Visualization = Fiber.extend(function() {
    return {
        init: function(mapContainerID, rangeContainerID, iterationContainerID) {
            this.map = this._initMap(mapContainerID);
            this.rangeContainerID = rangeContainerID;
            this.iterationContainerID = iterationContainerID;

            this.data = null;
            this.fromTimestamp = null;
            this.toTimestamp = null;
            this.startVisible = null;
            this.numVisible = null;
            this.currentPosition = null;

            this.overlays = null;

            this._watchForResize();
        },

        setData: function(data) {
            this.data = data;
            this.updateRange();
            this.refresh();
        },

        drawLine: function(map, datum1, datum2) {
            var longitude1 = datum1[1],
                latitude1 = datum1[2],
                longitude2 = datum2[1],
                latitude2 = datum2[2],
                anomalyScore2 = datum2[4],
                newSequence = datum2[5],
                from = [latitude1, longitude1],
                to = [latitude2, longitude2],
                color = this.calculateColor(datum2),
                weight = newSequence ? 1 : 4;
                polyline = map.drawPolyline({
                    path: [from, to],
                    strokeColor: '#' + color,
                    strokeOpacity: 0.6,
                    strokeWeight: weight,
                    zIndex: anomalyScore2
                });

            return polyline;
        },

        drawMarker: function(map, datum, angle) {
            var longitude = datum[1],
                latitude = datum[2],
                color = this.calculateColor(datum),
                transformStyle = '-webkit-transform: rotate('+angle+'deg);',
                darkerColor = Color('#'+color).darken(0.3).hexString(),
                colorStyle = 'border-color: '+darkerColor+';',
                htmlContent = '<div class="chevron" style="'+colorStyle+' '+transformStyle+'"></div>',
                overlay = map.drawOverlay({
                    lat: latitude,
                    lng: longitude,
                    content: htmlContent
                });

            return overlay;
        },

        calculateColor: function(datum) {
            var anomalyScore = datum[4],
                newSequence = datum[5],
                color = newSequence ? "666666" : getGreenToRed(anomalyScore * 100);

            return color;
        },

        calculateAngle: function(datum1, datum2) {
            var longitude1 = datum1[1],
                latitude1 = datum1[2],
                longitude2 = datum2[1],
                latitude2 = datum2[2],
                dLatitude = latitude2 - latitude1,
                dLongitude = longitude2 - longitude1,
                angle = 45;  // pointing to the right

            angle += -(Math.atan(dLatitude / dLongitude) * 180 / Math.PI);
            if (dLongitude < 0) {
                angle += 180;
            }

            return angle;
        },

        refresh: function() {
            var map = this.map,
                data = this.data,
                fromTimestamp = this.fromTimestamp,
                toTimestamp = this.toTimestamp,
                positions = [],
                overlays = [];

            map.removePolylines();
            map.removeOverlays();

            this.numVisible = 0;
            this.startVisible = null;

            for (var i = 0; i < data.length - 1; i++) {
                var datum1 = data[i],
                    timestamp1 = datum1[0],
                    longitude1 = datum1[1],
                    latitude1 = datum1[2],
                    datum2 = data[i+1],
                    timestamp2 = datum2[0];

                if (timestamp1 < fromTimestamp || timestamp1 > toTimestamp) continue;

                if (this.startVisible == null) this.startVisible = i;

                var angle = this.calculateAngle(datum1, datum2),
                    overlay = this.drawMarker(map, datum1, angle);

                overlays.push(overlay);
                positions.push(new google.maps.LatLng(latitude1, longitude1));
                
                this.numVisible += 1;

                if (timestamp2 < fromTimestamp || timestamp2 > toTimestamp) continue;
                this.drawLine(map, datum1, datum2);
            }

            this.overlays = overlays;

            map.fitLatLngBounds(positions);

            this.updateIteration();
        },

        drawCurrentPosition: function(position) {
            var lastPosition = this.currentPosition,
                overlays = this.overlays;

            if (lastPosition  != null) {
                var lastOverlay = overlays[lastPosition];
                $(lastOverlay.el).removeClass("current");
            }

            var overlay = overlays[position];
            $(overlay.el).addClass("current");

            this.currentPosition = position;

            var iteration = this.startVisible + position,
                data = this.data,
                datum = data[iteration];

            console.log("Iteration: " + iteration);
            console.log(datum);
        },

        updateRange: function() {
            var self = this,
                rangeContainerID = this.rangeContainerID,
                range = $(rangeContainerID),
                data = this.data,
                first = data[0],
                firstTimestamp = first[0],
                last = data[data.length - 1],
                lastTimestamp = last[0],
                dayInterval = 1000 * 60 * 60 * 24,
                fromTimestamp =  lastTimestamp - dayInterval,
                toTimestamp = lastTimestamp;

            if (range.children().length) range.dateRangeSlider("destroy");

            range.dateRangeSlider({
                bounds: {
                    min: firstTimestamp.valueOf(),
                    max: lastTimestamp.valueOf()
                },
                defaultValues: {
                    min: fromTimestamp.valueOf(),
                    max: toTimestamp.valueOf()
                },
                formatter:function(val){
                    return moment(val).format('MMMM Do YYYY, h:mm:ss a');
                }
            });

            this.fromTimestamp = fromTimestamp;
            this.toTimestamp = toTimestamp;

            range.bind("valuesChanging", function(e, data) {
                self.fromTimestamp = data.values.min;
                self.toTimestamp = data.values.max;
                self.refresh();
            });
        },

        updateIteration: function() {
            var self = this,
                iterationContainerID = this.iterationContainerID,
                iteration = $(iterationContainerID),
                numVisible = this.numVisible;

            if (iteration.children().length) iteration.slider("destroy");

            this.currentPosition = null;

            iteration.slider({
                max: numVisible - 1,
                slide: function(event, ui) {
                    self.drawCurrentPosition(ui.value);
                }
            });
        },

        /* Private */

        _initMap: function(containerID) {
            var map = new GMaps({
                div: containerID
            });

            map.fitZoom();

            return map;
        },

        _watchForResize: function() {
            var map = this.map;

            $(window).resize(function() {
                map.refresh();
            });
        },
    };
});
