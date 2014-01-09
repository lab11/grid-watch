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
         'time': 20000000}


pp = pprint.PrettyPrinter(indent=4)

class stream_receiver (sioc.BaseNamespace):
	def on_data (self, *args):
		pkt = args[0]
		pp.pprint(pkt)

socketIO = sioc.SocketIO(SOCKETIO_HOST, SOCKETIO_PORT)
stream_namespace = socketIO.define(stream_receiver,
	'/{}'.format(SOCKETIO_NAMESPACE))

stream_namespace.emit('query', query)
socketIO.wait()
