
var express = require('express');
var app = express();
var mysql = require('mysql');

//Connection pool
var pool = mysql.createPool({
  host     : 'localhost',
  user     : 'root',
  password : '123456',
  port     : '3306',
  database : 'acute',
  connectionLimit: 10 //pool size
});

app.use(express.static("./html"));

var server = require('http').createServer(app);
var ws = require('socket.io')(server);

server.listen(8080);

console.log('server start');

/*>
 * key:socketID,value:data packet
 * @type {Map<any, any>}
 */
var packetMap = new Map();

ws.on('connection', (socket) => {

    var curID = socket.id;
	console.log('new connection:'+curID);

	socket.on('data', (...msgs) => {
		
		var packet = new Array();
		//lng
		packet[0] = parseFloat(msgs[0]);
		//lat
		packet[1] = parseFloat(msgs[1]);
		//temperature
		packet[2] = parseFloat(msgs[2]);

		packetMap.set(curID,packet);

		//Save to database
		var addSqlParams = new Array();
		addSqlParams[0] = msgs[3];
		addSqlParams[1] = msgs[2];
		addSqlParams[2] = msgs[0];
		addSqlParams[3] = msgs[1];
		
		var  addSql = 'INSERT INTO acute_data(deviceID,temperature,lng,lat) VALUES(?,?,?,?)';
		
		pool.getConnection((err, connection) => {
            if (err) {
                console.log("connect fail");
            } else {
                connection.query(addSql, addSqlParams, function (err, result) {
                    if (err) {
                        console.log('[ERROR] - ', err.message);
                        return;
                    }
                });
               
                pool.releaseConnection(connection);
            }
        });

	});

	//Andtoid disconnect
	socket.on('disconnect', () => {
		
		console.log('curID disconnect');
		if(packetMap.has(curID)){
			packetMap.delete(curID);
		}
	});

	socket.on('requestData', (...msgs) => {
		var socketIDForShow = msgs[0];
		if(packetMap.has(socketIDForShow)){
			var packetForCurve = packetMap.get(socketIDForShow);
			socket.emit("dataForCurve",packetForCurve[2]);
		}
	});
	
	
 	//Baidu map request data
	socket.on('bound', (...msgs) => {

		var boundJson = msgs[0];
		//console.log("map bound:"+JSON.stringify(boundJson));
		swLng = boundJson.swLng;
		swLat = boundJson.swLat;
		neLng = boundJson.neLng;
		neLat = boundJson.neLat;

		/**
		 * data packet for show
		 * @type {any[]}
		 */
		var packetForShow = new Array();

		/**
		 * Traverse the map
		 */
		packetMap.forEach(function(value, key) {
			var lng = value[0];
			var lat = value[1];
			//console.log(key + " = " + lng+","+lat);
			//console.log(swLng+","+swLat+","+neLng+","+neLat);
			if(lng > swLng && lat > swLat && lng < neLng && lat < neLat){
				var newPacket = new Array(value[0],value[1],value[2],key);
				//console.log("new"+newPacket);
				packetForShow.push(newPacket);
			}
		}, packetMap)

		socket.emit("packets",packetForShow);
	});


	socket.on("history",(...msgs) =>{

		var boundJson = msgs[0];

		
		var findSql = 'select distinct lng,lat from acute_data where lng>? and lat>? and lng<? and lat<?';
		var findSqlParams = new Array();
		findSqlParams[0] = boundJson.swLng;
		findSqlParams[1] = boundJson.swLat;
		findSqlParams[2] = boundJson.neLng;
		findSqlParams[3] = boundJson.neLat;

		pool.getConnection((err, connection) => {
			if (err) {
				console.log("connect fail");
			} else {
				connection.query(findSql, findSqlParams, function (err, result) {
					if (err) {
						console.log('[ERROR] - ', err.message);
						return;
					} else{
						//console.log(JSON.stringify(result));
						socket.emit("locationsInBound",result);
					}
				});
				//释放
				pool.releaseConnection(connection);
			}
		});
	});

	socket.on('point',(...msgs) => {
		var lng = msgs[0];
		var lat = msgs[1];
		console.log(typeof lng);
		var findSqlParams = new Array();
		var findSql = 'select temperature, time from acute_data where lng = ? and lat = ? order by time desc limit 10';
		findSqlParams[0] = parseFloat(lng);
		findSqlParams[1] = parseFloat(lat);
		console.log(lng+","+lat);

		pool.getConnection((err, connection) => {
			if (err) {
				console.log("connect fail");
			} else {
				connection.query(findSql, findSqlParams, function (err, result) {
					if (err) {
						console.log('[ERROR] - ', err.message);
						return;
					} else{
						console.log(result.length+":"+JSON.stringify(result));
						socket.emit("historyData",result);
					}
				});
				pool.releaseConnection(connection);
			}
		});

	})
	
});
