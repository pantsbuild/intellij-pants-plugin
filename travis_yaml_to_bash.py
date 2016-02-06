#!/usr/bin/env python

import yaml
import os

shard_num = int(os.environ['CI_SHARD'])
with open(".travis.yml", 'r') as stream:
    yml = yaml.load(stream)
    try:
        print yml['env']['matrix'][shard_num]
    except IndexError:
        print "EXIT=0"
