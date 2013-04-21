#from Scientific.IO import NetCDF
#import Scientific.IO.NetCDF
import sys
from numpy import *
from Scientific.IO.NetCDF import NetCDFFile

#variables
input = []
temp = []


#open file
try:
    file = open(sys.argv[1],"r")
except Exception, e:
    print"Fehler beim oeffnen der Datei"
    sys.exit()

#load file into a list
for line in file:
    input.append(line)

#extract time and frequency from list
for i in input:
    temp = eval(i)

#get dimensions
timeDimension = len(temp)
frequencyDimension = len(temp[0])

#open a netCDF file
file = NetCDFFile("NetCDF.cdf","w")

#create the netCDF dimensions
file.createDimension("time", timeDimension)
#file.createDimension("time", None)
file.createDimension("frequency", frequencyDimension)

#create the netCDF variable
dataDims = ("time", "frequency")
data = file.createVariable("data", "d", dataDims)

#get size of variable
dataShape = data.shape

#put data in variable
for i in range(dataShape[0]):
    for j in range(dataShape[1]):
        data[i,j] = temp[i][j]


#close a netCDF file
file.close()

