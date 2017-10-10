import random

#random.seed(9)

maze = [['X', 'X', 'X', 'X', 'X', 'X'],
        ['X', 'R', 'X', 'S', 'X', 'X'],
        ['X', 'R', 'X', 'R', 'X', 'X'],       
        ['X', 'R', 'R', 'R', 'X', 'X'],
        ['X', 'R', 'X', 'R', 'R', 'X'],
        ['X', 'R', 'X', 'X', 'R', 'X'],
        ['X', 'R', 'R', 'S', 'R', 'X']]
        
    # set starting point as the 'O'
        

# https://en.wikipedia.org/wiki/Pathfinding

## checks to see whether the new point:
# 1. is actually on the map
# 2. is not already occupied by an X
# 3. is not already in the list with a smaller counter

def check(inputitem, mazeIn, queueIn, addedvariablesIn):
    maze = mazeIn
    if inputitem[0] == len(maze) or inputitem[0] == -1:
        return False
    if inputitem[1] == len(maze[0]) or inputitem[1] == -1:
        return False
    if maze[inputitem[0]][inputitem[1]] not in {'E', 'R', 'B'}:
        return False
    for item in queueIn:
        if item[0] == inputitem[0] and item[1] == inputitem[1] and item[2] <= inputitem[2]:
            return False
    for item in addedvariablesIn:
        if item[0] == inputitem[0] and item[1] == inputitem[1] and item[2] <= inputitem[2]:
            return False
    return True
    
## given a number of points with the next counter, it checks to see which is closest
# if multiple ones are closest it goes with the first one in the list
# should possibly make this random?
    
def bestof(potentialslist, end):
    
    distances = []
    
    for item in potentialslist:
        distance = (item[0] - end[0])**2 + (item[1] - end[1])**2
        distances.append(distance)
    #print(distances)[]
    
    mindistance = min(distances)
    minindices = [i for i, x in enumerate(distances) if x == mindistance]
    return(potentialslist[(minindices[0])])
    #return(potentialslist[random.choice(minindices)])
        
        
        
## beginning of 'main program'
def shortestjourney(startIn, endIn, mazeIn):
        
    #random.seed(20)
    queue = []

    start = startIn
    start.append(0)
    queue.append(start)
    end = endIn
    maze = mazeIn
    
    newvariables = [start]

## goes through looking at the adjacent neighbours of each point, checking whether
# they are suitable, and then (if they are), looking at THEIR adjacent neighbours
# the counter keeps track of how many spaces away from the starting point they are

    for counter in range(300):
        addedvariables = []
        for item in newvariables:
            adj1 = [item[0]+1, item[1], counter+1]
            if check(adj1, maze, queue, addedvariables) == True:
                addedvariables.append(adj1)
                queue.append(adj1)
            adj2 = [item[0]-1, item[1], counter+1]
            if check(adj2, maze, queue, addedvariables) == True:
                addedvariables.append(adj2)
                queue.append(adj2)
            adj3 = [item[0], item[1]+1, counter+1]
            if check(adj3, maze, queue, addedvariables) == True:
                addedvariables.append(adj3)
                queue.append(adj3)
            adj4 = [item[0], item[1]-1, counter+1]
            if check(adj4, maze, queue, addedvariables) == True:
                addedvariables.append(adj4)
                queue.append(adj4)
        newvariables = addedvariables


## decide on a route

    for item in queue:
        if item[0] == end[0] and item[1] == end[1]:
            routelength = item[2]
            endindex = queue.index(item)
        
    end = queue[endindex]
    journey = [end]    
    
    for counter in range(routelength):
        end = journey[-1]
        potentials = []
        for item in queue:
            if item[2] == journey[-1][2] - 1:
                potentials.append(item)
    #print(potentials)
        journey.append(bestof(potentials, end))
    

## outputs the route in the correct format
    journey.reverse()
    return journey

