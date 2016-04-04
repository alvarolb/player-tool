// EShapes.js
//
// Based on an idea, and some lines of code, by "thetoy" 
//
//   This Javascript is provided by Mike Williams
//   Community Church Javascript Team
//   http://www.bisphamchurch.org.uk/   
//   http://econym.org.uk/gmap/
//
//   This work is licenced under a Creative Commons Licence
//   http://creativecommons.org/licenses/by/2.0/uk/
//
// Version 0.0 04/Apr/2008 Not quite finished yet
// Version 1.0 10/Apr/2008 Initial release
// Version 3.0 12/Oct/2011 Ported to v3 by Lawrence Ross

google.maps.Polyline.Shape = function(point,r1,r2,r3,r4,rotation,vertexCount,opts,tilt) {
    var rot = -rotation*Math.PI/180;
    var points = [];
    var latConv =  google.maps.geometry.spherical.computeDistanceBetween(point, new google.maps.LatLng(point.lat()+0.1,point.lng()))*10;
    var lngConv =  google.maps.geometry.spherical.computeDistanceBetween(point, new google.maps.LatLng(point.lat(),point.lng()+0.1))*10;
    var step = (360/vertexCount)||10;
        
    var flop = -1;
    if (tilt) {
        var I1=180/vertexCount;
    } else {
        var  I1=0;
    }
    for(var i=I1; i<=360.001+I1; i+=step) {
        var r1a = flop?r1:r3;
        var r2a = flop?r2:r4;
        flop = -1-flop;
        var y = r1a * Math.cos(i * Math.PI/180);
        var x = r2a * Math.sin(i * Math.PI/180);
        var lng = (x*Math.cos(rot)-y*Math.sin(rot))/lngConv;
        var lat = (y*Math.cos(rot)+x*Math.sin(rot))/latConv;

        points.push(new google.maps.LatLng(point.lat()+lat,point.lng()+lng));
    }
    return (new google.maps.Polyline({
        path:points,
        opts:opts
    }))
}

google.maps.Polyline.Circle = function(point, radius, opts) {
    return google.maps.Polyline.Shape(point,radius,radius,radius,radius,0,100,opts)
}

google.maps.Polyline.RegularPoly = function(point,radius,vertexCount,rotation,opts) {
    rotation = rotation||0;
    var tilt = !(vertexCount&1);
    return google.maps.Polyline.Shape(point,radius,radius,radius,radius,rotation,vertexCount,opts,tilt)
}

google.maps.Polyline.Star = function(point,r1,r2,points,rotation,opts) {
    rotation = rotation||0;
    return google.maps.Polyline.Shape(point,r1,r1,r2,r2,rotation,points*2,opts)
}

google.maps.Polyline.Ellipse = function(point,r1,r2,rotation,opts) {
    rotation = rotation||0;
    return google.maps.Polyline.Shape(point,r1,r2,r1,r2,rotation,100,opts)
}

google.maps.Polygon.Pointer = function(point, size, rotation, opts){
    
    var point1 = new google.maps.geometry.spherical.computeOffset(point, size, 0+rotation);
    var point2 = new google.maps.geometry.spherical.computeOffset(point, size, 225+rotation);
    var point3 = point;
    var point4 = new google.maps.geometry.spherical.computeOffset(point, size, 135+rotation);
    var point5 = point1;
    
    var pointerCoords = [
       point1,
       point2,
       point3,
       point4,
       point5
    ];

    var polygon = new google.maps.Polygon({
        paths: pointerCoords,
        opts: opts
    });
    
    polygon.currentSize = size;
    
    return polygon;
}

google.maps.Polygon.Shape = function(point,r1,r2,r3,r4,rotation,vertexCount,opts,tilt) {
    var rot = -rotation*Math.PI/180;
    var points = [];
    var latConv =  google.maps.geometry.spherical.computeDistanceBetween(point, new google.maps.LatLng(point.lat()+0.1,point.lng()))*10;
    var lngConv =  google.maps.geometry.spherical.computeDistanceBetween(point, new google.maps.LatLng(point.lat(),point.lng()+0.1))*10;
    var step = (360/vertexCount)||10;
        
    var flop = -1;
    if (tilt) {
        var I1=180/vertexCount;
    } else {
        var  I1=0;
    }
    for(var i=I1; i<=360.001+I1; i+=step) {
        var r1a = flop?r1:r3;
        var r2a = flop?r2:r4;
        flop = -1-flop;
        var y = r1a * Math.cos(i * Math.PI/180);
        var x = r2a * Math.sin(i * Math.PI/180);
        var lng = (x*Math.cos(rot)-y*Math.sin(rot))/lngConv;
        var lat = (y*Math.cos(rot)+x*Math.sin(rot))/latConv;

        points.push(new google.maps.LatLng(point.lat()+lat,point.lng()+lng));
    }
    return (new google.maps.Polygon({
        paths: points,
        opts: opts
    }))
}

google.maps.Polygon.Circle = function(point,radius,opts) {
    return google.maps.Polygon.Shape(point,radius,radius,radius,radius,0,100,opts)
}

google.maps.Polygon.Circle.Paths = function(point,radius) {
    return google.maps.Polygon.Paths(point, radius, radius, radius, radius, 0, 100)
}

google.maps.Polygon.RegularPoly = function(point,radius,vertexCount,rotation,opts) {
    rotation = rotation||0;
    var tilt = !(vertexCount&1);
    return google.maps.Polygon.Shape(point,radius,radius,radius,radius,rotation,vertexCount,opts,tilt)
}

google.maps.Polygon.Star = function(point,r1,r2,points,rotation,opts) {
    rotation = rotation||0;
    return google.maps.Polygon.Shape(point,r1,r1,r2,r2,rotation,points*2,opts)
}

google.maps.Polygon.Ellipse = function(point,r1,r2,rotation,opts) {
    rotation = rotation||0;
    return google.maps.Polygon.Shape(point,r1,r2,r1,r2,rotation,100,opts)
}

google.maps.Polygon.Ellipse.Paths = function(point,r1,r2,rotation) {
    rotation = rotation||0;
    return google.maps.Polygon.Paths(point, r1, r2, r1, r2, rotation, 100);
}

google.maps.Polygon.Paths = function(point,r1,r2,r3,r4,rotation,vertexCount, tilt) {
    var rot = -rotation*Math.PI/180;
    var points = [];
    var latConv =  google.maps.geometry.spherical.computeDistanceBetween(point, new google.maps.LatLng(point.lat()+0.1,point.lng()))*10;
    var lngConv =  google.maps.geometry.spherical.computeDistanceBetween(point, new google.maps.LatLng(point.lat(),point.lng()+0.1))*10;
    var step = (360/vertexCount)||10;
        
    var flop = -1;
    if (tilt) {
        var I1=180/vertexCount;
    } else {
        var  I1=0;
    }
    for(var i=I1; i<=360.001+I1; i+=step) {
        var r1a = flop?r1:r3;
        var r2a = flop?r2:r4;
        flop = -1-flop;
        var y = r1a * Math.cos(i * Math.PI/180);
        var x = r2a * Math.sin(i * Math.PI/180);
        var lng = (x*Math.cos(rot)-y*Math.sin(rot))/lngConv;
        var lat = (y*Math.cos(rot)+x*Math.sin(rot))/latConv;

        points.push(new google.maps.LatLng(point.lat()+lat,point.lng()+lng));
    }
    return points;
}

function EOffset(point,easting,northing) {
    var latConv = google.maps.geometry.spherical.computeDistanceBetween(point, new google.maps.LatLng(point.lat()+0.1,point.lng()))*10;
    var lngConv =  google.maps.geometry.spherical.computeDistanceBetween(point,new google.maps.LatLng(point.lat(),point.lng()+0.1))*10;
    return new google.maps.LatLng(point.lat()+northing/latConv,point.lng()+easting/lngConv)      
}

function EOffsetBearing(point,dist,bearing) {
    var latConv =  google.maps.geometry.spherical.computeDistanceBetween(point,new google.maps.LatLng(point.lat()+0.1,point.lng()))*10;
    var lngConv =  google.maps.geometry.spherical.computeDistanceBetween(point,new google.maps.LatLng(point.lat(),point.lng()+0.1))*10;
    var lat=dist * Math.cos(bearing * Math.PI/180)/latConv;
    var lng=dist * Math.sin(bearing * Math.PI/180)/lngConv; 
    return new google.maps.LatLng(point.lat()+lat,point.lng()+lng)      
}


