import json
import sys

from Scientific.Functions.Polynomial import zeros
import netcdf_helpers
from numpy import *
from optparse import OptionParser


inputMean = 0
inputStd = 1

#command line options
parser = OptionParser()

#parse command line options
(options, args) = parser.parse_args()
if (len(args)<2):
    print "usage: -options input_filename output_filename"
    print options
    sys.exit(2)

#labels = noten
labels = [str(x).rjust(2,"0")for x in range(1,89)]


inputFilename = args[0]
outputFilename = args[1]
print options
print "input filename", inputFilename
print "output filename", outputFilename

#read in input files
filenames = file(inputFilename + "/filenames.txt").readlines()
seqTags = []
seqDims = []
targetStrings = []
seqLengths = []
for f in filenames:
    fname = f.strip()
    if len(fname):
        try:
            print "reading image dimensions", fname
            json_data = open(inputFilename + "/"+fname)
            data = json.load(json_data)
            transform = data["transform"]
            dims = (len(transform), len(transform[0]))
            label = data["notes"]
            targetStrings.append(label)
            seqLengths.append(dims[0] * dims[1])
            seqTags.append(fname)
            seqDims.append(dims)
        except:
            print "could not open image"
            data.remove(f)
    else:
        data.remove(f)

#inputs array
totalLen = sum(seqLengths)
print "tottalLen", totalLen
inputs = zeros((totalLen,1), "f")
offset = 0

#for filename in seqTags:
#    print "reading image file", filename
#    image = Image.open(filename).transpose(Image.FLIP_TOP_BOTTOM).transpose(Image.ROTATE_270)
#    for i in image.getdata():
#        inputs[offset][0]= (float(i)-inputMean)/inputStd
#        offset += 1
#
#create a new .nc file
file = netcdf_helpers.NetCDFFile(outputFilename, "w")

#create the dimensions
netcdf_helpers.createNcDim(file, "numSeqs", len(seqLengths))
netcdf_helpers.createNcDim(file,"numTimesteps", len(inputs))
netcdf_helpers.createNcDim(file, "inputPattSize", len(inputs[0]))
netcdf_helpers.createNcDim(file,"numDims", 2)
netcdf_helpers.createNcDim(file,"numLabels", len(labels))

#create the variables
netcdf_helpers.createNcStrings(file, "seqTags", seqTags, ("numSeqs", "maxSeqTagLength"), "sequence tags")
netcdf_helpers.createNcStrings(file, "labels", labels, ("numLabels", "maxLabelLength"), "labels")
netcdf_helpers.createNcStrings(file, "targetStrings", targetStrings, ("numSeqs", "maxTargetStringLength"), "target strings")
netcdf_helpers.createNcVar(file, "seqLengths", seqLengths, "i", ("numSeqs",), "sequence lengths")
netcdf_helpers.createNcVar(file, "seqDims", seqDims, "i", ("numSeqs","numDims"), "sequence dimensions")
netcdf_helpers.createNcVar(file, "inputs", inputs, "f", ("numTimesteps","inputPattSize"), "input patterns")

#write the data to disk
print "writing data to", outputFilename
json_data.close()

