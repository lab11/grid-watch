import csv
import pytz
from datetime import datetime
import collections

events = {}

with open('events.csv', 'rb') as csvfile:
     eventreader = csv.reader(csvfile, delimiter=',')
     tz1 = pytz.timezone('Africa/Nairobi')
     for row in eventreader:
         i = 0
         row_struct = {}
         date_object = ''
         for data in row:
            i = i + 1
            if i == 4: #plug event
               row_struct['type'] = data
            if i == 13: #time
               date_object = datetime.strptime(data, '%b %d, %Y %H:%M:%S %p')              
               date_object.replace(tzinfo=tz1)
               date_object = date_object.strftime('%s')
               row_struct['time'] = data
            if i == 17: #id
               row_struct['id'] = data
               events[date_object] = row_struct


od = collections.OrderedDict(sorted(events.items()))
#print "time, id, type"    
for timestep, value in od.items():
   if (value.items()[0][1]!='wd'):
    conv_type = -1
    if (value.items()[0][1]=='unplugged'):
         conv_type = 0
    elif (value.items()[0][1]=='plugged'):
         conv_type = 1
#    print str(timestep) + "," + value.items()[1][1] + "," + str(conv_type)

step_size = 120 #seconds +- from new report
cluster_size = 3

cur_time = -1

bucket_cnt = 0
bucket = {}
writer = csv.writer(open('clusters.csv', 'wb'))

print "min_time, max_time, size, raw"
for timestep, value in od.items():
    if int(cur_time) + step_size < int(timestep):
      if (len(bucket) >= cluster_size):
         to_write = {}
         to_write['min_time'] = str(cur_time)
         to_write['max_time'] = str(timestep)
         to_write['size'] = str(len(bucket))
         to_write['raw'] = str(bucket)         
         print str(cur_time) + "," + str(timestep) + "," + str(len(bucket)) + ",\"" + str(bucket) + "\""         
      cur_time = timestep
      bucket = {}
      bucket_cnt = bucket_cnt + 1
    else:
      if value['id'] not in bucket: 
         bucket[value['id']] = [value['type'], timestep]


