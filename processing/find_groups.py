
class Event:
    def __init__(self, startTime, endTime, eventType):
         self.startTime = startTime
         self.endTime = endTime
         self.eventType = eventType


import csv, math,datetime,time,urllib2, sched, json
from httplib2 import Http

group_size = 3 #reporting phones
time_thresh = 3000000 #mseconds thresh from min to max time of event
cnt = 0 #phone array cnt
sys_cnt = 0 #system cnt
offset = 300 #for range in seconds

forever = True
#forever = False

phones = [{'id': 150, 'time': -1, 'state': 0},
          {'id': 151, 'time': -1, 'state': 0},
          {'id': 152, 'time': -1, 'state': 0},
          {'id': 153, 'time': -1, 'state': 0},
          {'id': 154, 'time': -1, 'state': 0},
          {'id': 155, 'time': -1, 'state': 0},
          {'id': 156, 'time': -1, 'state': 0},
          {'id': 157, 'time': -1, 'state': 0}]

events = []
s = sched.scheduler(time.time, time.sleep)
h = Http()

def sum_phones():
   global cnt 
   cnt = 0
   for phone in phones:
      cnt += phone['state']

def send_post(msg):
   global h
   try:
     print "sending " + msg
     url = "http://gatd.eecs.umich.edu:8081/jJZ0N2eKmc"
     data = dict(state_bool=msg)
     req = urllib2.Request(url)
     req.add_header('Content-Type', 'application/json')
     res = urllib2.urlopen(req, json.dumps(data))
   except:
     print "ERROR POSTING... SKIPPING REPORT"

def check_time(direction):
   res = [False,99999999999999,-1]
   for phone in phones: # determine if any phone is outside of the threshold
     if (phone['state'] == direction):
        time = int(phone['time'])
        if (time < int(res[1])):
           res[1] = time
        elif (time > int(res[2])):
           res[2] = time
   if ((int(res[2]) - int(res[1])) < time_thresh):
     res[0] = True
   return res

def inRange(report_time):
  global offset 
  cur_time = int(datetime.datetime.now().strftime('%s'))  
  report_time = int(report_time)/1000
  return True
  #return int(report_time) - int(offset) <= int(cur_time) <= int(report_time) + int(offset)

def run(sc):
 global sys_cnt,events,phones,forever
 print sys_cnt
 sys_cnt += 1
 final_state = ''
 with open('gw_dump.csv', 'rb') as csvfile:
   cur_events = []
   reader = csv.reader(csvfile, delimiter=",")
   for row in reader:
      for phone in phones:
         if (phone['id'] == int(row[2])):
           phone['state'] = int(row[1])
           phone['time'] = row[0];
           break
      sum_phones()
      if (cnt >= group_size or cnt <= -group_size):
         res = check_time(math.copysign(1, cnt))
         if (inRange(row[0])):
          if (res[0] == True):
           final_state = "unplugged"
           if (cnt < 0):
               final_state = "plugged"
      else:
         final_state = "unknown"
      to_write = {};
      to_write['time'] = row[0];
      to_write['event_type'] = final_state
      if (forever == False):
       with open('gw_events.csv', 'ab') as f: 
         w = csv.DictWriter(f, to_write.keys())
         w.writerow(to_write) 
   #res = check_time(math.copysign(1, cnt))
   #print "start time: " + str(res[1])
   if (forever == True):
     send_post(final_state)
     sc.enter(1, 1, run, (sc,))
         
s.enter(1, 1, run, (s,))
s.run()

