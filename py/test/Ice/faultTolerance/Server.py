# **********************************************************************
#
# Copyright (c) 2003-2004 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

import sys, os, Ice

Ice.loadSlice('Test.ice')
import Test

def usage(n):
    sys.stderr.write("Usage: " + n + " port\n")

class TestI(Test.TestIntf):
    def __init__(self, adapter):
        self._adapter = adapter

    def shutdown(self, current=None):
        self._adapter.getCommunicator().shutdown()

    def abort(self, current=None):
        print "aborting..."
        sys.exit(0)

    def idempotentAbort(self, current=None):
        sys.exit(0)

    def nonmutatingAbort(self, current=None):
        sys.exit(0)

    def pid(self, current=None):
        return os.getpid()

def run(args, communicator):
    port = 0
    for arg in args[1:]:
        if arg[0] == '-':
            print >> sys.stderr, args[0] + ": unknown option `" + arg + "'"
            usage(args[0])
            return False
        if port > 0:
            print >> sys.stderr, args[0] + ": only one port can be specified"
            usage(args[0])
            return False

        port = int(arg)

    if port <= 0:
        print >> sys.stderr, args[0] + ": no port specified"
        usage(args[0])
        return False

    endpts = "default -p " + str(port)
    communicator.getProperties().setProperty("TestAdapter.Endpoints", endpts)
    adapter = communicator.createObjectAdapter("TestAdapter")
    object = TestI(adapter)
    adapter.add(object, Ice.stringToIdentity("test"))
    adapter.activate()
    communicator.waitForShutdown()
    return True

try:
    #
    # In this test, we need a longer server idle time, otherwise
    # our test servers may time out before they are used in the
    # test.
    #
    properties = Ice.getDefaultProperties(sys.argv)
    properties.setProperty("Ice.ServerIdleTime", "120") # Two minutes.

    communicator = Ice.initialize(sys.argv)
    status = run(sys.argv, communicator)
except Ice.Exception, ex:
    print ex
    status = False

if communicator:
    try:
        communicator.destroy()
    except Ice.Exception, ex:
        print ex
        status = False

sys.exit(not status)
