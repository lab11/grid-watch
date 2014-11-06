#!/usr/bin/env python

import IPy
import json
import pprint
import sys

try:
	import socketIO_client as sioc
except ImportError:
	print('Could not import the socket.io client library.')
	print('sudo pip install socketIO-client')
	sys.exit(1)

import logging
logging.basicConfig()

SOCKETIO_HOST      = 'inductor.eecs.umich.edu'
SOCKETIO_PORT      = 8082
SOCKETIO_NAMESPACE = 'stream'

query = {'profile_id': 'HthZRrHnlC',
         'time': 30000}

pp = pprint.PrettyPrinter(indent=4)

import csv,datetime
class stream_receiver (sioc.BaseNamespace):
   def on_reconnect (self):
      del query['time']
      stream_namespace.emit('query', query)

   def on_connect (self):
      stream_namespace.emit('query', query)
   
   def on_data (self, *args):
      to_write = {};
      pkt = args[0]
      if (pkt['event_type'] == 'plugged'):
         to_write['event_type'] = -1
      if (pkt['event_type'] == 'unplugged'):
         to_write['event_type'] = 1
      if (pkt['event_type'] != 'wd'):
        to_write['id'] = pkt['phone_id'] 
        to_write['time'] = pkt['time']
        #msTime = pkt['time'] / 1000
        #to_write['time'] = datetime.datetime.fromtimestamp(msTime).strftime('%Y-%m-%d %H:%M:%S')
        with open('gw_dump.csv', 'ab') as f:  # Just use 'w' mode in 3.x
           w = csv.DictWriter(f, to_write.keys())
           w.writerow(to_write)

socketIO = sioc.SocketIO(SOCKETIO_HOST, SOCKETIO_PORT)
stream_namespace = socketIO.define(stream_receiver,
	'/{}'.format(SOCKETIO_NAMESPACE))

#stream_namespace.emit('query', query)
socketIO.wait()
