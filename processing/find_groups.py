
class Event:
    def __init__(self, startTime, endTime, eventType):
         self.startTime = startTime
         self.endTime = endTime
         self.eventType = eventType


import csv, math

group_size = 6 #reporting phones
time_thresh = 30 #seconds close to each other
cnt = 0

phones = [{'id': 150, 'time': -1, 'state': 0},
          {'id': 151, 'time': -1, 'state': 0},
          {'id': 152, 'time': -1, 'state': 0},
          {'id': 153, 'time': -1, 'state': 0},
          {'id': 154, 'time': -1, 'state': 0},
          {'id': 155, 'time': -1, 'state': 0},
          {'id': 156, 'time': -1, 'state': 0},
          {'id': 157, 'time': -1, 'state': 0}]

events = []

def sum_phones():
   global cnt 
   cnt = 0
   for phone in phones:
      cnt += phone['state']

def check_time(direction):
   res = [False,99999999999999,-1]
   for phone in phones: # determine if any phone is outside of the threshold
     if (phone['state'] == direction):
        time = int(phone['time'])
        if (time < int(res[1])):
           res[1] = time
        elif (time > int(res[2])):
           res[2] = time
   if ((int(res[2]) - int(res[1])) < 500000):
     res[0] = True
   return res

with open('gw_dump.csv', 'rb') as csvfile:
   reader = csv.reader(csvfile, delimiter=",")
   for row in reader:
      for phone in phones:
         if (phone['id'] == int(row[2])):
           phone['state'] = int(row[1])
           phone['time'] = row[0];
           break
      sum_phones()
      #print cnt
      if (cnt >= group_size or cnt <= -group_size):
         res = check_time(math.copysign(1, cnt))
         if (res[0] == True):
           event_type = -1
           if (cnt < 0):
               event_type = 1
           event = Event(res[1], res[2], event_type)
           events.append(event)


events = list(set(events))
print str(len(events)) + " events found"

with open('gw_events.csv', 'wb') as csvfile:
   writer = csv.writer(csvfile, delimiter=",")
   for event in events:
      writer.writerow([event.startTime, event.endTime, event.eventType])

      
