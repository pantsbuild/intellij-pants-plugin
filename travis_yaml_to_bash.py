#!/usr/bin/env python

import yaml
import sys

'''the final output will look something like:
case "${CI_SHARD}" in
0) IJ_ULTIMATE=false
 ;;
1) IJ_ULTIMATE=true
 ;;
2) USE_PANTS_TO_COMPILE=false
 ;;
3) PANTS_SHA="release_0.0.69" TEST_SET=integration
 ;;
*)
  exit 1
'''

start = '''case "${CI_SHARD}" in'''
end = '''esac'''
default = '''\
*)
  exit 1'''

script='''\
echo $IJ_ULTIMATE
echo PANTS_SHA
./scripts/setup-ci-environment.sh
./scripts/run-tests-ci.sh
'''
components=[start]
with open(".travis.yml", 'r') as stream:
    yml = yaml.load(stream)
    for i, line in enumerate(yml['env']['matrix']):
        components.append("{}) {} \n ;;".format(i, line))
    components.append(default)
    components.append(end)
    components.append(script)
    final_output = '\n'.join(components)
    with open(sys.argv[1], 'w') as f:
        f.write(final_output)


